package com.irsyadlabs.espbridge.receiver

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.irsyadlabs.espbridge.EspBridgeApp
import com.irsyadlabs.espbridge.service.DeviceConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BluetoothStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        if (state != BluetoothAdapter.STATE_ON) return

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = EspBridgeApp.instance.container.settings.settings.first()
                if (settings.autoConnect && settings.keepBackgroundConnection && settings.trustedDeviceAddress != null) {
                    runCatching { ContextCompat.startForegroundService(context, Intent(context, DeviceConnectionService::class.java)) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
