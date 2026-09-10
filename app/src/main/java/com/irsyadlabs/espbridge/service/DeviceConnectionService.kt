package com.irsyadlabs.espbridge.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.irsyadlabs.espbridge.EspBridgeApp
import com.irsyadlabs.espbridge.MainActivity
import com.irsyadlabs.espbridge.R
import com.irsyadlabs.espbridge.core.model.BleDeliveryState
import com.irsyadlabs.espbridge.core.model.ChronchiLiveStatusFormatter
import com.irsyadlabs.espbridge.core.model.ConnectionMode
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.core.model.OledPreviewSelector
import com.irsyadlabs.espbridge.core.model.PhoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DeviceConnectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null
    private var previewExpiryJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            connectionNotification(PhoneState(), ConnectionState.DISCONNECTED)
        )

        val container = EspBridgeApp.instance.container
        scope.launch {
            container.ble.connectionState.collectLatest { state ->
                if (state == ConnectionState.CONNECTED) {
                    container.systemCollector.refresh()
                    container.router.fullSync(container.stateHub.state.value)
                    // Firebase config is now manual - user sends via button
                    // container.router.sendFirebaseConfig()
                }
            }
        }
        scope.launch {
            combine(container.stateHub.state, container.ble.connectionState) { phoneState, connection ->
                phoneState to connection
            }.collectLatest { (phoneState, connection) ->
                updateLiveNotification(phoneState, connection)
            }
        }

        reconnectJob = scope.launch {
            var cloudWasAvailable = false
            var reconnectDelayMs = 2_000L
            while (isActive) {
                val settings = container.settings.settings.first()
                if (settings.autoConnect && settings.trustedDeviceAddress != null &&
                    container.ble.bluetoothEnabled() && !container.ble.isConnected() &&
                    !container.ble.isFirmwareUpdateInProgress()
                ) {
                    val currentState = container.ble.connectionState.value
                    if (currentState == ConnectionState.DISCONNECTED || currentState == ConnectionState.ERROR) {
                        container.ble.reconnectTrusted(settings.trustedDeviceAddress)
                        // Exponential backoff for retries: 2s, 4s, 8s... up to 30s
                        reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
                    }
                } else if (container.ble.isConnected()) {
                    reconnectDelayMs = 2_000L // Reset delay when connected
                }

                container.systemCollector.refresh()
                val cloudAvailable = settings.cloudSyncEnabled &&
                    settings.connectionMode != ConnectionMode.BLUETOOTH_ONLY &&
                    container.cloud.available()
                if (cloudAvailable && !cloudWasAvailable) {
                    container.router.fullSync(container.stateHub.state.value)
                }
                cloudWasAvailable = cloudAvailable
                if (settings.selectedSourceIds.contains("location")) {
                    runCatching {
                        container.locationCollector.refresh(settings.selectedSourceIds.contains("weather"))
                    }
                }
                delay(if (container.ble.isConnected()) 60_000L else reconnectDelayMs)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        reconnectJob?.cancel()
        previewExpiryJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.connection_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Tampilan Chronchi langsung dan koneksi otomatis ESP32"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun updateLiveNotification(phoneState: PhoneState, connection: ConnectionState) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            connectionNotification(phoneState, connection)
        )
        previewExpiryJob?.cancel()
        val event = phoneState.liveData.event ?: return
        if (phoneState.navigation.active) return
        val remaining = OledPreviewSelector.timeoutFor(event.category) -
            (System.currentTimeMillis() - event.timestamp).coerceAtLeast(0L)
        if (remaining <= 0L || remaining == Long.MAX_VALUE) return
        previewExpiryJob = scope.launch {
            delay(remaining)
            val container = EspBridgeApp.instance.container
            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID,
                connectionNotification(container.stateHub.state.value, container.ble.connectionState.value)
            )
        }
    }

    private fun connectionNotification(
        phoneState: PhoneState,
        connection: ConnectionState
    ): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val live = ChronchiLiveStatusFormatter.format(phoneState)
        val connectionText = when (connection) {
            ConnectionState.CONNECTED -> "BLE tersambung"
            ConnectionState.CONNECTING, ConnectionState.DISCOVERING, ConnectionState.SYNCING -> "Menghubungkan BLE"
            ConnectionState.SCANNING -> "Mencari Chronchi otomatis"
            ConnectionState.ERROR -> "BLE mencoba kembali"
            else -> "Menunggu Chronchi"
        }
        val deliveryText = when {
            phoneState.navigation.active -> deliveryText(phoneState.navigation.bleDelivery)
            phoneState.liveData.event != null -> deliveryText(phoneState.liveData.bleDelivery)
            else -> ""
        }
        val expanded = buildList {
            add(live.primary)
            live.secondary.takeIf(String::isNotBlank)?.let(::add)
            live.footer.takeIf(String::isNotBlank)?.let(::add)
            add(listOf(connectionText, deliveryText).filter(String::isNotBlank).joinToString(" • "))
        }.joinToString("\n")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(live.title)
            .setContentText(live.primary.ifBlank { connectionText })
            .setStyle(NotificationCompat.BigTextStyle().bigText(expanded))
            .setSubText(connectionText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun deliveryText(value: BleDeliveryState): String = when (value) {
        BleDeliveryState.SENT -> "ACK Chronchi"
        BleDeliveryState.SENDING -> "Mengirim ke Chronchi"
        BleDeliveryState.PREVIEW_ONLY -> "Menunggu sinkronisasi"
    }

    companion object {
        const val CHANNEL_ID = "espbridge_connection"
        const val NOTIFICATION_ID = 4101
    }
}
