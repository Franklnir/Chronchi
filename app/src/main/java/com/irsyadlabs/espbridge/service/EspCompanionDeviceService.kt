package com.irsyadlabs.espbridge.service

import android.companion.CompanionDeviceService
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Companion-device presence hook. Android invokes this only after the app has an OS-level
 * companion association. The current MVP also stores the trusted BLE address so it can
 * auto-reconnect on devices where companion presence observation is not configured yet.
 */
@RequiresApi(Build.VERSION_CODES.S)
class EspCompanionDeviceService : CompanionDeviceService() {
    @Deprecated("Deprecated by newer Android companion-device APIs")
    override fun onDeviceAppeared(address: String) {
        ContextCompat.startForegroundService(this, Intent(this, DeviceConnectionService::class.java))
    }

    @Deprecated("Deprecated by newer Android companion-device APIs")
    override fun onDeviceDisappeared(address: String) {
        // Do not forget the device. The connection service will wait and reconnect later.
    }
}
