package com.irsyadlabs.espbridge.utils

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.IOException

class EspUsbSerialHelper(private val context: Context) : SerialInputOutputManager.Listener {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private var ioManager: SerialInputOutputManager? = null

    private val _serialDataFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val serialDataFlow = _serialDataFlow.asSharedFlow()

    private val ACTION_USB_PERMISSION = "com.irsyadlabs.espbridge.USB_PERMISSION"
    
    var onPermissionGranted: ((Boolean) -> Unit)? = null

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            Log.d("EspUsbSerialHelper", "Permission granted for device $device")
                            onPermissionGranted?.invoke(true)
                        }
                    } else {
                        Log.d("EspUsbSerialHelper", "Permission denied for device $device")
                        onPermissionGranted?.invoke(false)
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    fun getAvailableDevices(): List<UsbDevice> {
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return availableDrivers.map { it.device }
    }

    fun connect(device: UsbDevice, baudRate: Int): Boolean {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return false
        val port = driver.ports.firstOrNull() ?: return false
        
        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val permissionIntent = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_USB_PERMISSION), flags
            )
            usbManager.requestPermission(device, permissionIntent)
            return false // Permission requested, wait for broadcast
        }

        return try {
            val connection = usbManager.openDevice(device) ?: return false
            port.open(connection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = true
            port.rts = true

            serialPort = port
            ioManager = SerialInputOutputManager(port, this)
            ioManager?.start()
            Log.i("EspUsbSerialHelper", "Connected to ${device.productName} at $baudRate bps")
            _serialDataFlow.tryEmit("[SYSTEM] Berhasil terhubung ke ${device.productName} (${baudRate} bps)\n")
            true
        } catch (e: Exception) {
            Log.e("EspUsbSerialHelper", "Connection failed", e)
            _serialDataFlow.tryEmit("[ERROR] Gagal terhubung: ${e.message}\n")
            false
        }
    }

    fun disconnect() {
        ioManager?.stop()
        ioManager = null
        try {
            serialPort?.close()
        } catch (e: IOException) {}
        serialPort = null
    }

    fun cleanup() {
        disconnect()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {}
    }

    override fun onNewData(data: ByteArray?) {
        data?.let {
            val text = String(it, Charsets.UTF_8)
            _serialDataFlow.tryEmit(text)
        }
    }

    override fun onRunError(e: Exception?) {
        Log.e("EspUsbSerialHelper", "Runner error", e)
        _serialDataFlow.tryEmit("[ERROR] Koneksi serial terputus.\n")
        disconnect()
    }
}
