package com.irsyadlabs.espbridge.transport.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import com.irsyadlabs.espbridge.core.model.AndroidFirmwareUpdatePolicy
import com.irsyadlabs.espbridge.core.model.ConnectionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

data class DiscoveredBleDevice(
    val address: String,
    val name: String,
    val rssi: Int
)

data class OutboundBleMessage(
    val type: PacketType,
    val json: String,
    val onComplete: ((Boolean) -> Unit)? = null
)

data class BleProtocolStatus(
    val ready: Boolean = false,
    val lastAcknowledgedSequence: Int? = null,
    val lastError: String? = null
)

data class ConnectedDeviceInfo(
    val id: String,
    val name: String,
    val firmwareVersion: String,
    val board: String,
    val otaAvailable: Boolean,
    val maximumImageSize: Long,
    val secureLinkRequired: Boolean,
    val runningSlot: String,
    val updateStrategy: String = "none",
    val role: String = "main",
    val mode: String = "chronchi",  // "chronchi" or "xichi"
    val wifiSsid: String? = null,
    val firebaseSaved: Boolean = false
)

data class DiscoveredWifiNetwork(
    val ssid: String,
    val rssi: Int,
    val secure: Boolean
)

private class WriteBatch(
    val sequence: Int,
    val type: PacketType,
    val waitsForFirmwareAck: Boolean,
    private val completion: ((Boolean) -> Unit)?
) {
    private var finished = false

    @Synchronized
    fun finish(success: Boolean) {
        if (finished) return
        finished = true
        completion?.invoke(success)
    }
}

private data class PendingFrame(
    val bytes: ByteArray,
    val batch: WriteBatch,
    val isLast: Boolean
)

class BleConnectionManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val manager = context.getSystemService(BluetoothManager::class.java)
    private val adapter: BluetoothAdapter? get() = manager?.adapter

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _devices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredBleDevice>> = _devices.asStateFlow()

    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()

    private val _protocolStatus = MutableStateFlow(BleProtocolStatus())
    val protocolStatus: StateFlow<BleProtocolStatus> = _protocolStatus.asStateFlow()

    private val _deviceInfo = MutableStateFlow<ConnectedDeviceInfo?>(null)
    val deviceInfo: StateFlow<ConnectedDeviceInfo?> = _deviceInfo.asStateFlow()

    private val _wifiNetworks = MutableStateFlow<List<DiscoveredWifiNetwork>>(emptyList())
    val wifiNetworks: StateFlow<List<DiscoveredWifiNetwork>> = _wifiNetworks.asStateFlow()

    private val discovered = ConcurrentHashMap<String, DiscoveredBleDevice>()
    private val awaitingAcks = ConcurrentHashMap<Int, WriteBatch>()
    private val incomingDecoder = BlePacketCodec.Decoder()
    private val queueLock = Any()
    private val handshakeLock = Any()

    @Volatile private var gatt: BluetoothGatt? = null
    @Volatile private var writeCharacteristic: BluetoothGattCharacteristic? = null
    @Volatile private var negotiatedMtu = MINIMUM_ATT_MTU
    @Volatile private var descriptorReady = false
    @Volatile private var readyReceived = false
    @Volatile private var serviceDiscoveryStarted = false
    @Volatile private var connectionGeneration = 0
    @Volatile private var firmwareUpdateInProgress = false
    @Volatile private var trustedScanAddress: String? = null

    private var pendingFrames = ArrayDeque<PendingFrame>()
    private var activeFrame: PendingFrame? = null
    private var writing = false

    fun bluetoothEnabled(): Boolean = adapter?.isEnabled == true

    private fun hasScanPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED

    private fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapterRef = adapter
        if (adapterRef == null || !adapterRef.isEnabled) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Bluetooth belum aktif")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        val scanner = adapterRef.bluetoothLeScanner
        if (scanner == null) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Bluetooth LE Scanner tidak tersedia")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        if (!hasScanPermission()) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Izin scan Bluetooth tidak diberikan")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        
        // Find if we have a trusted device in settings to enable auto-pair during scan
        val trustedAddress = runCatching {
            scope.launch {
                val settings = com.irsyadlabs.espbridge.EspBridgeApp.instance.container.settings.settings.first()
                trustedScanAddress = settings.trustedDeviceAddress
            }
        }

        discovered.clear()
        _devices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING

        // Reverting to single specific filter to avoid duplicates
        val filters = listOf(
            ScanFilter.Builder().setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID)).build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        runCatching {
            scanner.startScan(filters, settings, scanCallback)
        }.onFailure {
            _protocolStatus.value = BleProtocolStatus(lastError = "Gagal memulai scan: ${it.message}")
            _connectionState.value = ConnectionState.ERROR
        }

        scope.launch {
            delay(BleConstants.SCAN_PERIOD_MS)
            if (_connectionState.value == ConnectionState.SCANNING) {
                stopScan()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!hasScanPermission()) return
        runCatching { adapter?.bluetoothLeScanner?.stopScan(scanCallback) }
        trustedScanAddress = null
        if (_connectionState.value == ConnectionState.SCANNING) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /** Finds only the previously trusted ESP32 and connects as soon as it advertises. */
    @SuppressLint("MissingPermission")
    fun reconnectTrusted(address: String) {
        if (firmwareUpdateInProgress || isConnected() ||
            _connectionState.value in setOf(
                ConnectionState.CONNECTING,
                ConnectionState.DISCOVERING,
                ConnectionState.SYNCING,
                ConnectionState.SCANNING
            )
        ) return
        if (!hasScanPermission()) {
            reconnect(address)
            return
        }
        disconnect(closeOnly = true)
        val scanner = adapter?.bluetoothLeScanner ?: return
        trustedScanAddress = address
        _connectionState.value = ConnectionState.SCANNING
        val filters = listOf(
            ScanFilter.Builder()
                .setDeviceAddress(address)
                .build()
        )
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(filters, settings, scanCallback)
        scope.launch {
            delay(BleConstants.TRUSTED_SCAN_PERIOD_MS)
            if (trustedScanAddress.equals(address, ignoreCase = true) &&
                _connectionState.value == ConnectionState.SCANNING
            ) {
                stopScan()
                // Some phones suppress filtered BLE scan results while the display is off.
                // A direct cached-GATT attempt keeps reconnection automatic without a UI scan.
                reconnect(address)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val displayName = runCatching { device.name }.getOrNull()
                ?: result.scanRecord?.deviceName
                ?: "ESP Device"
            val item = DiscoveredBleDevice(device.address, displayName, result.rssi)
            
            // Ensure no duplicates by using the map properly
            discovered[item.address] = item
            _devices.value = discovered.values.toList().sortedByDescending { it.rssi }
            
            val trusted = trustedScanAddress
            if (trusted != null && trusted.equals(item.address, ignoreCase = true)) {
                stopScan()
                connect(item.address)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _protocolStatus.value = BleProtocolStatus(lastError = "BLE scan failed ($errorCode)")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String, isAutoConnect: Boolean = false) {
        if (!hasConnectPermission()) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Izin koneksi Bluetooth tidak diberikan")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        val adapterRef = adapter
        if (adapterRef == null || !adapterRef.isEnabled) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Bluetooth tidak aktif")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        stopScan()
        val device = runCatching { adapterRef.getRemoteDevice(address) }.getOrNull()
        if (device == null) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Alamat perangkat tidak valid")
            _connectionState.value = ConnectionState.ERROR
            return
        }
        disconnect(closeOnly = true)
        resetProtocolState()
        _connectionState.value = ConnectionState.CONNECTING
        
        // Use autoConnect = true for background/reconnect attempts to let Android OS handle the poll
        gatt = device.connectGatt(context, isAutoConnect, gattCallback, BluetoothDevice.TRANSPORT_LE)
        val generation = connectionGeneration
        scope.launch {
            // Longer timeout for auto-connect since it depends on OS scheduling
            val timeout = if (isAutoConnect) 60_000L else CONNECTION_TIMEOUT_MS
            delay(timeout)
            if (generation == connectionGeneration &&
                (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.DISCOVERING)
            ) {
                if (!isAutoConnect) { // Don't show error for background auto-poll
                    disconnect(closeOnly = true)
                    _protocolStatus.value = BleProtocolStatus(lastError = "Koneksi ke ESP32 timeout")
                    _connectionState.value = ConnectionState.ERROR
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun reconnect(address: String) = connect(address, isAutoConnect = true)

    @SuppressLint("MissingPermission")
    fun disconnect(closeOnly: Boolean = false) {
        connectionGeneration += 1
        val current = gatt
        if (current != null && hasConnectPermission()) {
            if (!closeOnly) runCatching { current.disconnect() }
            runCatching { current.close() }
        }
        gatt = null
        writeCharacteristic = null
        failPendingWrites()
        resetProtocolState()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED
    fun isFirmwareUpdateInProgress(): Boolean = firmwareUpdateInProgress

    fun send(type: PacketType, json: String, onComplete: ((Boolean) -> Unit)? = null): Boolean =
        sendBatch(listOf(OutboundBleMessage(type, json, onComplete)))

    /** Queues a complete logical transaction atomically so sync markers cannot be interleaved. */
    @SuppressLint("MissingPermission")
    fun sendBatch(messages: List<OutboundBleMessage>): Boolean {
        if (messages.isEmpty() || !hasConnectPermission() || !isConnected()) return false
        val characteristic = writeCharacteristic ?: return false
        val prepared = runCatching {
            messages.map { message ->
                val encoded = BlePacketCodec.encodePacket(message.type, message.json, negotiatedMtu)
                val batch = WriteBatch(
                    encoded.sequence,
                    message.type,
                    waitsForFirmwareAck = message.onComplete != null,
                    completion = message.onComplete
                )
                batch to encoded.frames
            }
        }.getOrElse {
            messages.forEach { it.onComplete?.invoke(false) }
            _protocolStatus.value = _protocolStatus.value.copy(lastError = it.message)
            return false
        }

        prepared.forEach { (batch, _) ->
            if (batch.waitsForFirmwareAck) {
                awaitingAcks.put(batch.sequence, batch)?.finish(false)
                scope.launch {
                    delay(ACK_TIMEOUT_MS)
                    if (awaitingAcks.remove(batch.sequence, batch)) batch.finish(false)
                }
            }
        }

        synchronized(queueLock) {
            prepared.forEach { (batch, frames) ->
                frames.forEachIndexed { index, frame ->
                    pendingFrames.addLast(PendingFrame(frame, batch, index == frames.lastIndex))
                }
            }
        }
        val started = writeNext(characteristic)
        if (!started) prepared.forEach { (batch, _) -> failBatch(batch) }
        return started
    }

    /** Sends one binary frame and completes on the GATT write response, not a firmware ACK. */
    @SuppressLint("MissingPermission")
    fun sendBinary(
        type: PacketType,
        payload: ByteArray,
        onComplete: (Boolean) -> Unit
    ): Boolean {
        if (!hasConnectPermission() || !isConnected()) return false
        val characteristic = writeCharacteristic ?: return false
        val encoded = runCatching {
            BlePacketCodec.encodeBinaryPacket(type, payload, negotiatedMtu)
        }.getOrElse {
            _protocolStatus.value = _protocolStatus.value.copy(lastError = it.message)
            onComplete(false)
            return false
        }
        val batch = WriteBatch(
            encoded.sequence,
            type,
            waitsForFirmwareAck = false,
            completion = onComplete
        )
        synchronized(queueLock) {
            pendingFrames.addLast(PendingFrame(encoded.frames.single(), batch, true))
        }
        val started = writeNext(characteristic)
        if (!started) failBatch(batch)
        return started
    }

    /** Sends binary control metadata and completes only after a protocol ACK. */
    @SuppressLint("MissingPermission")
    private fun sendBinaryAwaitingAck(
        type: PacketType,
        payload: ByteArray,
        onComplete: (Boolean) -> Unit
    ): Boolean {
        if (!hasConnectPermission() || !isConnected()) return false
        val characteristic = writeCharacteristic ?: return false
        val encoded = runCatching {
            BlePacketCodec.encodeBinaryPacket(type, payload, negotiatedMtu)
        }.getOrElse {
            _protocolStatus.value = _protocolStatus.value.copy(lastError = it.message)
            onComplete(false)
            return false
        }
        val batch = WriteBatch(
            encoded.sequence,
            type,
            waitsForFirmwareAck = true,
            completion = onComplete
        )
        awaitingAcks.put(encoded.sequence, batch)?.finish(false)
        scope.launch {
            delay(ACK_TIMEOUT_MS)
            if (awaitingAcks.remove(encoded.sequence, batch)) batch.finish(false)
        }
        synchronized(queueLock) {
            pendingFrames.addLast(PendingFrame(encoded.frames.single(), batch, true))
        }
        val started = writeNext(characteristic)
        if (!started) failBatch(batch)
        return started
    }

    suspend fun transferFirmware(
        image: ByteArray,
        version: String,
        sha256: String,
        board: String,
        deviceSignature: String,
        onProgress: (Float) -> Unit
    ) {
        check(!firmwareUpdateInProgress) { "Update firmware lain masih berjalan" }
        val connectedInfo = deviceInfo.value ?: error("Informasi perangkat belum tersedia")
        val access = AndroidFirmwareUpdatePolicy.evaluate(
            connectedInfo.board,
            connectedInfo.otaAvailable
        )
        require(access.allowed) { access.reason }
        firmwareUpdateInProgress = true
        try {
            var info = connectedInfo
            require(info.otaAvailable) { "Perangkat tidak menyediakan updater firmware" }
            require(info.board == board) { "Firmware tidak cocok untuk board ${info.board}" }
            require(info.secureLinkRequired) { "Update ditolak karena koneksi BLE tidak aman" }
            require(info.updateStrategy == "recovery") {
                "Strategi update perangkat tidak didukung (${info.updateStrategy})"
            }
            val address = gatt?.device?.address ?: error("Alamat ESP32 tidak tersedia")
            if (info.role != "recovery") {
                if (!awaitJsonAck(PacketType.OTA_ENTER_RECOVERY, "{}")) {
                    error("ESP32 gagal masuk ke recovery updater")
                }
                info = awaitRecoveryReconnect(address, board)
            }
            require(info.role == "recovery") { "ESP32 tidak berada pada recovery updater" }
            require(image.isNotEmpty() && image.size.toLong() <= info.maximumImageSize) {
                "Ukuran firmware melebihi slot utama perangkat"
            }

            val metadata = encodeSignedOtaMetadata(
                imageSize = image.size,
                sha256 = sha256,
                board = board,
                version = version,
                deviceSignature = deviceSignature
            )
            if (!awaitBinaryAck(PacketType.OTA_BEGIN, metadata)) {
                error("Recovery menolak metadata atau tanda tangan firmware")
            }
            val chunkSize = (negotiatedMtu - 3 - BlePacketCodec.HEADER_SIZE - 4)
                .coerceAtLeast(8)
            var offset = 0
            while (offset < image.size) {
                val count = minOf(chunkSize, image.size - offset)
                val payload = ByteBuffer.allocate(4 + count)
                    .order(ByteOrder.BIG_ENDIAN)
                    .putInt(offset)
                    .put(image, offset, count)
                    .array()
                if (!awaitBinaryWrite(PacketType.OTA_CHUNK, payload)) {
                    error("Transfer firmware terputus pada byte $offset")
                }
                offset += count
                onProgress(offset.toFloat() / image.size.toFloat())
            }
            if (!awaitJsonAck(PacketType.OTA_END, "{}")) {
                error("ESP32 gagal memverifikasi firmware")
            }
        } catch (error: Throwable) {
            send(PacketType.OTA_ABORT, "{}")
            throw error
        } finally {
            firmwareUpdateInProgress = false
        }
    }

    private suspend fun awaitRecoveryReconnect(
        address: String,
        board: String
    ): ConnectedDeviceInfo = withTimeout(RECOVERY_RECONNECT_TIMEOUT_MS) {
        delay(1_200)
        var attempt = 0
        while (true) {
            val current = deviceInfo.value
            if (isConnected() && current?.role == "recovery" && current.board == board) {
                return@withTimeout current
            }
            if (_connectionState.value !in setOf(
                    ConnectionState.CONNECTING,
                    ConnectionState.DISCOVERING,
                    ConnectionState.SYNCING
                )
            ) {
                reconnect(address)
                attempt += 1
            }
            val recovered = runCatching {
                withTimeout(RECOVERY_ATTEMPT_TIMEOUT_MS) {
                    deviceInfo.first { it?.role == "recovery" && it.board == board }
                }
            }.getOrNull()
            if (recovered != null) return@withTimeout recovered
            disconnect(closeOnly = true)
            delay((attempt * 500L).coerceAtMost(2_000L))
        }
        @Suppress("UNREACHABLE_CODE")
        error("Recovery updater tidak ditemukan")
    }

    private fun encodeSignedOtaMetadata(
        imageSize: Int,
        sha256: String,
        board: String,
        version: String,
        deviceSignature: String
    ): ByteArray {
        require(sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 firmware tidak valid" }
        val boardBytes = board.toByteArray(Charsets.UTF_8)
        val versionBytes = version.toByteArray(Charsets.UTF_8)
        val signatureBytes = Base64.getMimeDecoder().decode(deviceSignature)
        require(boardBytes.size in 1..63) { "Nama board terlalu panjang" }
        require(versionBytes.size in 1..31) { "Versi firmware terlalu panjang" }
        require(signatureBytes.size in 64..80) { "Tanda tangan perangkat tidak valid" }
        val digest = ByteArray(32) { index ->
            sha256.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
        return ByteBuffer.allocate(
            4 + digest.size + 1 + boardBytes.size + 1 + versionBytes.size + 1 + signatureBytes.size
        ).order(ByteOrder.BIG_ENDIAN)
            .putInt(imageSize)
            .put(digest)
            .put(boardBytes.size.toByte())
            .put(boardBytes)
            .put(versionBytes.size.toByte())
            .put(versionBytes)
            .put(signatureBytes.size.toByte())
            .put(signatureBytes)
            .array()
    }

    private suspend fun awaitJsonAck(type: PacketType, json: String): Boolean {
        val result = CompletableDeferred<Boolean>()
        if (!send(type, json) { success -> result.complete(success) }) {
            result.complete(false)
        }
        return result.await()
    }

    private suspend fun awaitBinaryWrite(type: PacketType, payload: ByteArray): Boolean {
        val result = CompletableDeferred<Boolean>()
        if (!sendBinary(type, payload) { success -> result.complete(success) }) {
            result.complete(false)
        }
        return result.await()
    }

    private suspend fun awaitBinaryAck(type: PacketType, payload: ByteArray): Boolean {
        val result = CompletableDeferred<Boolean>()
        if (!sendBinaryAwaitingAck(type, payload) { success -> result.complete(success) }) {
            result.complete(false)
        }
        return result.await()
    }

    @SuppressLint("MissingPermission")
    private fun writeNext(characteristic: BluetoothGattCharacteristic): Boolean {
        val frame = synchronized(queueLock) {
            if (writing) return true
            val next = pendingFrames.removeFirstOrNull() ?: return false
            writing = true
            activeFrame = next
            next
        }
        val currentGatt = gatt
        if (currentGatt == null) {
            synchronized(queueLock) {
                writing = false
                activeFrame = null
            }
            failBatch(frame.batch)
            return false
        }
        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currentGatt.writeCharacteristic(
                characteristic,
                frame.bytes,
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
        } else {
            @Suppress("DEPRECATION")
            characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            @Suppress("DEPRECATION")
            characteristic.value = frame.bytes
            @Suppress("DEPRECATION")
            currentGatt.writeCharacteristic(characteristic)
        }
        if (!started) {
            synchronized(queueLock) {
                writing = false
                activeFrame = null
            }
            failBatch(frame.batch)
            writeCharacteristic?.let { writeNext(it) }
        }
        return started
    }

    private fun failBatch(batch: WriteBatch) {
        synchronized(queueLock) {
            pendingFrames.removeAll { it.batch === batch }
        }
        awaitingAcks.remove(batch.sequence, batch)
        batch.finish(false)
    }

    private fun failPendingWrites() {
        val abandoned = synchronized(queueLock) {
            val batches = pendingFrames.map { it.batch }.toMutableSet()
            activeFrame?.batch?.let(batches::add)
            batches += awaitingAcks.values
            pendingFrames.clear()
            activeFrame = null
            writing = false
            batches
        }
        awaitingAcks.clear()
        abandoned.forEach { it.finish(false) }
    }

    private fun resetProtocolState() {
        incomingDecoder.reset()
        negotiatedMtu = MINIMUM_ATT_MTU
        descriptorReady = false
        readyReceived = false
        serviceDiscoveryStarted = false
        _protocolStatus.value = BleProtocolStatus()
        _deviceInfo.value = null
    }

    @SuppressLint("MissingPermission")
    private fun discoverServicesOnce(currentGatt: BluetoothGatt) {
        val shouldStart = synchronized(handshakeLock) {
            if (serviceDiscoveryStarted) false else {
                serviceDiscoveryStarted = true
                true
            }
        }
        if (shouldStart && !currentGatt.discoverServices()) {
            _protocolStatus.value = BleProtocolStatus(lastError = "Unable to start GATT discovery")
            _connectionState.value = ConnectionState.ERROR
        }
    }

    private fun beginReadyTimeout(generation: Int) {
        scope.launch {
            delay(READY_TIMEOUT_MS)
            if (
                generation == connectionGeneration &&
                descriptorReady &&
                !readyReceived &&
                _connectionState.value == ConnectionState.SYNCING
            ) {
                _protocolStatus.value = BleProtocolStatus(
                    ready = false,
                    lastError = "ESP32 did not complete the ESPBridge V1 handshake"
                )
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    private fun handleIncoming(value: ByteArray) {
        when (val decoded = incomingDecoder.feed(value)) {
            BleDecodeResult.Incomplete -> Unit
            is BleDecodeResult.Error -> {
                _protocolStatus.value = _protocolStatus.value.copy(lastError = decoded.reason)
            }
            is BleDecodeResult.Complete -> {
                val packet = decoded.packet
                if (packet.type == PacketType.DEVICE_STATUS) {
                    val json = runCatching { JSONObject(packet.jsonPayload) }.getOrElse {
                        _protocolStatus.value = _protocolStatus.value.copy(
                            lastError = "Invalid ESP32 device information"
                        )
                        return
                    }
                    _deviceInfo.value = ConnectedDeviceInfo(
                        id = json.optString("id"),
                        name = json.optString("name"),
                        firmwareVersion = json.optString("fw", "unknown"),
                        board = json.optString("board"),
                        otaAvailable = json.optBoolean("ota", false),
                        maximumImageSize = json.optLong("max", 0L),
                        secureLinkRequired = json.optBoolean("secure", false),
                        runningSlot = json.optString("slot", "unknown"),
                        updateStrategy = json.optString(
                            "update",
                            if (json.optBoolean("ota", false)) "ab" else "none"
                        ),
                        role = json.optString(
                            "role",
                            if (json.optString("slot") == "recovery") "recovery" else "main"
                        ),
                        mode = json.optString("mode", "chronchi"),
                        wifiSsid = json.optString("ssid").takeIf { it.isNotBlank() },
                        firebaseSaved = json.optBoolean("fs", false) || json.optBoolean("firebase", false)
                    )
                    return
                }
                if (packet.type == PacketType.FIREBASE_STATUS) {
                    val json = runCatching { JSONObject(packet.jsonPayload) }.getOrNull() ?: return
                    val saved = json.optBoolean("saved", false)
                    _deviceInfo.value = _deviceInfo.value?.copy(firebaseSaved = saved)
                    return
                }
                if (packet.type == PacketType.WIFI_LIST) {
                    val json = runCatching { JSONObject(packet.jsonPayload) }.getOrNull() ?: return
                    val listArr = json.optJSONArray("networks") ?: return
                    val networks = mutableListOf<DiscoveredWifiNetwork>()
                    for (i in 0 until listArr.length()) {
                        val obj = listArr.getJSONObject(i)
                        networks.add(
                            DiscoveredWifiNetwork(
                                ssid = obj.optString("ssid"),
                                rssi = obj.optInt("rssi"),
                                secure = obj.optBoolean("auth", true)
                            )
                        )
                    }
                    _wifiNetworks.value = networks.sortedByDescending { it.rssi }
                    return
                }
                if (packet.type != PacketType.ACK) return
                val json = runCatching { JSONObject(packet.jsonPayload) }.getOrElse {
                    _protocolStatus.value = _protocolStatus.value.copy(lastError = "Invalid ESP32 ACK JSON")
                    return
                }
                if (json.optBoolean("ready", false) || json.optBoolean("r", false)) {
                    readyReceived = true
                    _protocolStatus.value = BleProtocolStatus(ready = true)
                    if (descriptorReady && _connectionState.value in setOf(
                            ConnectionState.DISCOVERING,
                            ConnectionState.SYNCING
                        )
                    ) {
                        _connectionState.value = ConnectionState.CONNECTED
                        requestDeviceInfo()
                    }
                    return
                }

                val sequence = json.optInt("sequence", packet.sequence).coerceIn(0, 255)
                val ok = json.optBoolean("ok", false)
                val error = json.optString("error").takeIf(String::isNotBlank)
                val batch = awaitingAcks.remove(sequence)
                if (ok) {
                    batch?.finish(true)
                    _protocolStatus.value = BleProtocolStatus(
                        ready = readyReceived,
                        lastAcknowledgedSequence = sequence
                    )
                } else {
                    if (batch != null) failBatch(batch)
                    _protocolStatus.value = BleProtocolStatus(
                        ready = readyReceived,
                        lastAcknowledgedSequence = sequence,
                        lastError = error ?: "ESP32 rejected BLE packet"
                    )
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _protocolStatus.value = BleProtocolStatus(lastError = "GATT connection failed ($status)")
                _connectionState.value = ConnectionState.ERROR
                failPendingWrites()
                runCatching { gatt.close() }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionGeneration += 1
                    val generation = connectionGeneration
                    _connectionState.value = ConnectionState.DISCOVERING
                    if (hasConnectPermission()) {
                        @SuppressLint("MissingPermission")
                        runCatching { gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH) }
                        
                        // Small delay before service discovery helps ESP32 stability
                        scope.launch {
                            delay(600) 
                            if (generation == connectionGeneration) {
                                @SuppressLint("MissingPermission")
                                val discoveryStarted = gatt.discoverServices()
                                if (!discoveryStarted) {
                                    _protocolStatus.value = BleProtocolStatus(lastError = "Gagal memulai discovery servis")
                                    _connectionState.value = ConnectionState.ERROR
                                }
                            }
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionGeneration += 1
                    _connectionState.value = ConnectionState.DISCONNECTED
                    writeCharacteristic = null
                    failPendingWrites()
                    resetProtocolState()
                    runCatching { gatt.close() }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else MINIMUM_ATT_MTU
            if (hasConnectPermission()) discoverServicesOnce(gatt)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _protocolStatus.value = BleProtocolStatus(lastError = "GATT service discovery failed ($status)")
                _connectionState.value = ConnectionState.ERROR
                return
            }
            val service: BluetoothGattService? = gatt.getService(BleConstants.SERVICE_UUID)
            val phoneToEsp = service?.getCharacteristic(BleConstants.PHONE_TO_ESP_UUID)
            val espToPhone = service?.getCharacteristic(BleConstants.ESP_TO_PHONE_UUID)
            val descriptor = espToPhone?.getDescriptor(BleConstants.CCC_UUID)
            if (service == null || phoneToEsp == null || espToPhone == null || descriptor == null) {
                _protocolStatus.value = BleProtocolStatus(lastError = "ESPBridge V1 GATT service is incomplete")
                _connectionState.value = ConnectionState.ERROR
                return
            }
            writeCharacteristic = phoneToEsp
            if (!configureIncomingNotifications(gatt, espToPhone, descriptor)) {
                _protocolStatus.value = BleProtocolStatus(lastError = "Unable to enable ESP32 acknowledgements")
                _connectionState.value = ConnectionState.ERROR
            }
        }

        @SuppressLint("MissingPermission")
        private fun configureIncomingNotifications(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            descriptor: BluetoothGattDescriptor
        ): Boolean {
            if (!hasConnectPermission() || !gatt.setCharacteristicNotification(characteristic, true)) return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(
                    descriptor,
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                ) == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid != BleConstants.CCC_UUID) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _protocolStatus.value = BleProtocolStatus(lastError = "ESP32 ACK subscription failed ($status)")
                _connectionState.value = ConnectionState.ERROR
                return
            }
            descriptorReady = true
            if (readyReceived) {
                _connectionState.value = ConnectionState.CONNECTED
                requestDeviceInfo()
            } else {
                _connectionState.value = ConnectionState.SYNCING
                beginReadyTimeout(connectionGeneration)
            }
            if (hasConnectPermission()) runCatching { gatt.readRemoteRssi() }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            val completed = synchronized(queueLock) {
                val value = activeFrame
                activeFrame = null
                writing = false
                value
            }
            if (completed != null) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (completed.isLast && !completed.batch.waitsForFirmwareAck) {
                        completed.batch.finish(true)
                    }
                } else {
                    failBatch(completed.batch)
                }
            }
            writeCharacteristic?.let { writeNext(it) }
        }

        @Deprecated("Deprecated in Android 13")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value?.clone() ?: return
            handleIncoming(value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncoming(value)
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) _rssi.value = rssi
        }
    }

    companion object {
        private const val MINIMUM_ATT_MTU = 23
        private const val MTU_TIMEOUT_MS = 2_000L
        private const val READY_TIMEOUT_MS = 4_000L
        private const val ACK_TIMEOUT_MS = 15_000L
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val RECOVERY_RECONNECT_TIMEOUT_MS = 60_000L
        private const val RECOVERY_ATTEMPT_TIMEOUT_MS = 7_000L
    }

    private fun requestDeviceInfo() {
        scope.launch {
            delay(100)
            if (isConnected()) send(PacketType.DEVICE_STATUS, "{}")
        }
    }
}
