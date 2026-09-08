package com.irsyadlabs.espbridge

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase
import com.irsyadlabs.espbridge.core.util.PermissionUtils
import com.irsyadlabs.espbridge.service.DeviceConnectionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EspBridgeApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Live state must never become a disk-backed offline event queue.
        // Keep RTDB persistence explicitly disabled even if SDK defaults change.
        if (BuildConfig.FIREBASE_CONFIG_PRESENT) {
            runCatching {
                FirebaseApp.initializeApp(this)
                FirebaseDatabase.getInstance().setPersistenceEnabled(false)
            }
        }
        container = AppContainer(this)
        appScope.launch {
            container.credentials.getOrCreate()
            val settings = container.settings.settings.first()
            if (settings.autoConnect && settings.keepBackgroundConnection &&
                settings.trustedDeviceAddress != null && PermissionUtils.bluetoothGranted(this@EspBridgeApp)
            ) {
                runCatching {
                    ContextCompat.startForegroundService(
                        this@EspBridgeApp,
                        Intent(this@EspBridgeApp, DeviceConnectionService::class.java)
                    )
                }
            }
        }
    }

    companion object {
        lateinit var instance: EspBridgeApp
            private set
    }
}
