package com.irsyadlabs.espbridge

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.core.model.ConnectionMode
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.core.model.PhoneState
import com.irsyadlabs.espbridge.core.model.WeatherState
import com.irsyadlabs.espbridge.core.security.DeviceCredentialsManager
import com.irsyadlabs.espbridge.core.util.PermissionUtils
import com.irsyadlabs.espbridge.data.auth.AuthResult
import com.irsyadlabs.espbridge.data.local.LocalSettings
import com.irsyadlabs.espbridge.data.firmware.FirmwareUpdateState
import com.irsyadlabs.espbridge.service.DeviceConnectionService
import com.irsyadlabs.espbridge.transport.ble.DiscoveredBleDevice
import com.irsyadlabs.espbridge.transport.ble.BleProtocolStatus
import com.irsyadlabs.espbridge.transport.ble.ConnectedDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull


data class MainUiState(
    val initialized: Boolean = false,
    val signedIn: Boolean = false,
    val email: String? = null,
    val firebaseReady: Boolean = false,
    val settings: LocalSettings = LocalSettings(),
    val phoneState: PhoneState = PhoneState(),
    val bleState: ConnectionState = ConnectionState.DISCONNECTED,
    val bleDevices: List<DiscoveredBleDevice> = emptyList(),
    val bleRssi: Int? = null,
    val bleProtocolStatus: BleProtocolStatus = BleProtocolStatus(),
    val connectedDevice: ConnectedDeviceInfo? = null,
    val firmwareUpdate: FirmwareUpdateState = FirmwareUpdateState.Idle,
    val credentials: DeviceCredentialsManager.Credentials? = null,
    val busy: Boolean = false,
    val message: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as EspBridgeApp
    private val c = app.container

    private val signedIn = MutableStateFlow(c.auth.isFirebaseSignedIn())
    private val email = MutableStateFlow(c.auth.currentEmail())
    private val credentials = MutableStateFlow<DeviceCredentialsManager.Credentials?>(null)
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val initialized = MutableStateFlow(false)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<MainUiState> = combine(
        c.settings.settings.map { it as Any? },
        c.stateHub.state.map { it as Any? },
        c.ble.connectionState.map { it as Any? },
        c.ble.devices.map { it as Any? },
        c.ble.rssi.map { it as Any? },
        c.ble.protocolStatus.map { it as Any? },
        signedIn.map { it as Any? },
        email.map { it as Any? },
        credentials.map { it as Any? },
        busy.map { it as Any? },
        message.map { it as Any? },
        initialized.map { it as Any? },
        c.ble.deviceInfo.map { it as Any? },
        c.firmwareUpdates.state.map { it as Any? }
    ) { values ->
        val settings = values[0] as LocalSettings
        MainUiState(
            initialized = values[11] as Boolean,
            signedIn = (values[6] as Boolean) || settings.demoLoggedIn,
            email = (values[7] as String?) ?: settings.demoEmail,
            firebaseReady = c.auth.isFirebaseReady(),
            settings = settings,
            phoneState = values[1] as PhoneState,
            bleState = values[2] as ConnectionState,
            bleDevices = values[3] as List<DiscoveredBleDevice>,
            bleRssi = values[4] as Int?,
            bleProtocolStatus = values[5] as BleProtocolStatus,
            connectedDevice = values[12] as ConnectedDeviceInfo?,
            firmwareUpdate = values[13] as FirmwareUpdateState,
            credentials = values[8] as DeviceCredentialsManager.Credentials?,
            busy = values[9] as Boolean,
            message = values[10] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            credentials.value = c.credentials.getOrCreate()
            c.systemCollector.refresh()
            val initialSettings = c.settings.settings.first()
            if (initialSettings.weatherTemperature != null) {
                c.stateHub.updateWeather(
                    WeatherState(
                        temperatureC = initialSettings.weatherTemperature,
                        condition = initialSettings.weatherCondition ?: "unknown",
                        locationLabel = initialSettings.weatherLocation.orEmpty(),
                        updatedAt = initialSettings.weatherUpdatedAt,
                        isCached = true
                    )
                )
            }
            if (initialSettings.onboardingComplete && initialSettings.keepBackgroundConnection) {
                startBackgroundServiceIfEnabled()
            }
            initialized.value = true
        }
        viewModelScope.launch {
            c.ble.deviceInfo
                .filterNotNull()
                .distinctUntilChangedBy { "${it.id}:${it.firmwareVersion}:${it.board}" }
                .collect { c.firmwareUpdates.check(it) }
        }
    }

    fun login(emailValue: String, password: String) = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            when (val result = c.auth.login(emailValue.trim(), password)) {
                is AuthResult.Success -> {
                    signedIn.value = true
                    email.value = result.email
                    registerCloudDeviceInBackground()
                }
                is AuthResult.Error -> message.value = result.message
            }
        } finally {
            busy.value = false
        }
    }

    fun register(emailValue: String, password: String) = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            when (val result = c.auth.register(emailValue.trim(), password)) {
                is AuthResult.Success -> {
                    signedIn.value = true
                    email.value = result.email
                    registerCloudDeviceInBackground()
                }
                is AuthResult.Error -> message.value = result.message
            }
        } finally {
            busy.value = false
        }
    }

    fun googleDemoLogin() = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            val result = c.auth.demoGoogleLogin()
            if (result is AuthResult.Success) {
                signedIn.value = true
                email.value = result.email
                registerCloudDeviceInBackground()
            }
        } finally {
            busy.value = false
        }
    }

    fun firebaseGoogleToken(idToken: String) = viewModelScope.launch {
        busy.value = true
        message.value = null
        try {
            when (val result = c.auth.loginWithGoogleIdToken(idToken)) {
                is AuthResult.Success -> {
                    signedIn.value = true
                    email.value = result.email
                    registerCloudDeviceInBackground()
                }
                is AuthResult.Error -> message.value = result.message
            }
        } finally {
            busy.value = false
        }
    }

    fun logout() = viewModelScope.launch {
        // Send clear config command to ESP32 before disconnecting
        if (c.ble.isConnected()) {
            c.router.sendClearConfig()
            kotlinx.coroutines.delay(500) // Give time for BLE message to be sent
        }
        c.auth.logout()
        signedIn.value = false
        email.value = null
        c.ble.disconnect()
        getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
    }

    fun completeOnboarding() = viewModelScope.launch {
        c.settings.setOnboardingComplete(true)
        startBackgroundServiceIfEnabled()
        c.systemCollector.refresh()
        runCatching { c.locationCollector.refresh(true) }
    }

    fun setSourceEnabled(id: String, enabled: Boolean) = viewModelScope.launch {
        c.settings.setSourceEnabled(id, enabled)
    }

    fun setConnectionMode(mode: ConnectionMode) = viewModelScope.launch {
        c.settings.setConnectionMode(mode)
    }

    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch {
        c.settings.setAutoConnect(enabled)
        if (enabled) startBackgroundServiceIfEnabled()
    }

    fun setCloudSync(enabled: Boolean) = viewModelScope.launch {
        c.settings.setCloudSyncEnabled(enabled)
    }

    fun setAppTheme(theme: AppTheme) = viewModelScope.launch {
        c.settings.setAppTheme(theme)
    }

    fun setFirebaseSecret(secret: String) = viewModelScope.launch {
        c.settings.setFirebaseDatabaseSecret(secret)
    }

    fun sendWifiConfig(ssid: String, password: String) {
        c.router.sendWifiConfig(ssid, password)
    }

    fun sendSwitchMode(mode: String) {
        c.router.sendSwitchMode(mode)
    }

    fun setKeepBackground(enabled: Boolean) = viewModelScope.launch {
        c.settings.setKeepBackgroundConnection(enabled)
        if (enabled) startBackgroundServiceIfEnabled()
        else getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
    }

    fun scanBle() = c.ble.startScan()
    fun stopBleScan() = c.ble.stopScan()

    fun connect(device: DiscoveredBleDevice) = viewModelScope.launch {
        c.ble.connect(device.address)
        val verifiedDevice = withTimeoutOrNull(30_000L) {
            c.ble.deviceInfo.first { it != null }
        }
        if (verifiedDevice != null) {
            c.settings.setTrustedDevice(device.address, device.name)
            c.companionAssociation.startObservingPresence(device.address)
            startBackgroundServiceIfEnabled()
        } else {
            c.ble.disconnect(closeOnly = true)
            message.value = "Pairing ESP32 belum selesai. Coba hubungkan kembali."
        }
    }

    fun disconnect() = c.ble.disconnect()

    fun forgetDevice() = viewModelScope.launch {
        val address = uiState.value.settings.trustedDeviceAddress
        // Send clear config command to ESP32 before disconnecting
        if (c.ble.isConnected()) {
            c.router.sendClearConfig()
            kotlinx.coroutines.delay(500) // Give time for BLE message to be sent
        }
        c.ble.disconnect()
        if (address != null) c.companionAssociation.disassociate(address)
        c.settings.setTrustedDevice(null, null)
    }

    fun reconnect() {
        val address = uiState.value.settings.trustedDeviceAddress ?: return
        c.ble.reconnectTrusted(address)
    }

    fun refreshPhoneState() {
        c.systemCollector.refresh()
        viewModelScope.launch { runCatching { c.locationCollector.refresh(true) } }
    }

    fun regenerateCredentials() = viewModelScope.launch {
        credentials.value = c.credentials.regenerate()
    }

    fun checkFirmwareUpdate() = viewModelScope.launch {
        c.firmwareUpdates.check(c.ble.deviceInfo.value)
    }

    fun installFirmwareUpdate() = viewModelScope.launch {
        c.firmwareUpdates.install()
    }

    fun clearMessage() {
        message.value = null
    }

    fun showMessage(value: String) {
        message.value = value
    }

    private suspend fun registerCloudDeviceIfAvailable() {
        val creds = credentials.value ?: c.credentials.getOrCreate().also { credentials.value = it }
        if (c.cloud.available()) runCatching { c.cloud.registerDevice(creds.deviceId, "ESP Display") }
    }

    private fun registerCloudDeviceInBackground() {
        viewModelScope.launch { registerCloudDeviceIfAvailable() }
    }

    private fun startBackgroundServiceIfEnabled() {
        val context = getApplication<Application>()
        if (!PermissionUtils.bluetoothGranted(context)) return
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, DeviceConnectionService::class.java))
        }
    }
}
