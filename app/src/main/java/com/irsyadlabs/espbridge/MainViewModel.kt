package com.irsyadlabs.espbridge

import android.app.Application
import android.net.Uri
import com.irsyadlabs.espbridge.data.xiaozhi.XiaozhiAdminUserItem
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
import com.irsyadlabs.espbridge.data.xiaozhi.*
import com.irsyadlabs.espbridge.service.DeviceConnectionService
import com.irsyadlabs.espbridge.transport.ble.DiscoveredBleDevice
import com.irsyadlabs.espbridge.transport.ble.BleProtocolStatus
import com.irsyadlabs.espbridge.transport.ble.ConnectedDeviceInfo
import com.irsyadlabs.espbridge.transport.ble.DiscoveredWifiNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val wifiNetworks: List<DiscoveredWifiNetwork> = emptyList(),
    val firmwareUpdate: FirmwareUpdateState = FirmwareUpdateState.Idle,
    val credentials: DeviceCredentialsManager.Credentials? = null,
    val busy: Boolean = false,
    val message: String? = null,
    // Xiaozhi AI state
    val xiaozhiMcpStatus: XiaozhiMcpStatus = XiaozhiMcpStatus(false, false, "Memuat status MCP"),
    val xiaozhiRelays: List<SmartHomeRelay> = emptyList(),
    val xiaozhiChatMessages: List<XiaozhiChatMessage> = emptyList(),
    val xiaozhiProfile: XiaozhiProfileData? = null,
    val xiaozhiChatLoading: Boolean = false
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

    // Xiaozhi state flows
    private val xiaozhiMcpStatus = MutableStateFlow(XiaozhiMcpStatus(false, false, "Memuat status MCP"))
    private val xiaozhiRelays = MutableStateFlow<List<SmartHomeRelay>>(emptyList())
    private val xiaozhiChatMessages = MutableStateFlow<List<XiaozhiChatMessage>>(emptyList())
    private val xiaozhiProfile = MutableStateFlow<XiaozhiProfileData?>(null)
    private val xiaozhiChatLoading = MutableStateFlow(false)

    private val _xiaozhiDashboardData = MutableStateFlow(XiaozhiDashboardData())
    val xiaozhiDashboardData: StateFlow<XiaozhiDashboardData> = _xiaozhiDashboardData

    private val _xiaozhiChatHistory = MutableStateFlow(XiaozhiChatHistoryData())
    val xiaozhiChatHistory: StateFlow<XiaozhiChatHistoryData> = _xiaozhiChatHistory

    private val _xiaozhiDashboardLoading = MutableStateFlow(false)
    val xiaozhiDashboardLoading: StateFlow<Boolean> = _xiaozhiDashboardLoading

    private val _xiaozhiScanningPersona = MutableStateFlow(false)
    val xiaozhiScanningPersona: StateFlow<Boolean> = _xiaozhiScanningPersona

    private val _xiaozhiAdminUsers = MutableStateFlow<List<XiaozhiAdminUserItem>>(emptyList())
    val xiaozhiAdminUsers: StateFlow<List<XiaozhiAdminUserItem>> = _xiaozhiAdminUsers

    private val _xiaozhiAdminUsersLoading = MutableStateFlow(false)
    val xiaozhiAdminUsersLoading: StateFlow<Boolean> = _xiaozhiAdminUsersLoading

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            combine(
                c.settings.settings,
                c.stateHub.state,
                c.ble.connectionState,
                c.ble.devices,
                c.ble.rssi
            ) { s, ps, cs, dev, rssi ->
                @Suppress("UNCHECKED_CAST")
                listOf(s, ps, cs, dev, rssi) as List<Any?>
            },
            combine(
                c.ble.protocolStatus,
                signedIn,
                email,
                credentials
            ) { proto, si, em, cred ->
                @Suppress("UNCHECKED_CAST")
                listOf(proto, si, em, cred) as List<Any?>
            }
        ) { a, b -> a + b },
        combine(
            combine(
                busy,
                message,
                initialized,
                c.ble.deviceInfo,
                c.ble.wifiNetworks
            ) { b, msg, init, dInfo, wifi ->
                @Suppress("UNCHECKED_CAST")
                listOf(b, msg, init, dInfo, wifi) as List<Any?>
            },
            combine(
                c.firmwareUpdates.state,
                xiaozhiMcpStatus,
                xiaozhiRelays,
                xiaozhiChatMessages
            ) { fw, mcp, rel, chat ->
                @Suppress("UNCHECKED_CAST")
                listOf(fw, mcp, rel, chat) as List<Any?>
            }
        ) { a, b -> a + b },
        combine(
            xiaozhiProfile,
            xiaozhiChatLoading
        ) { prof, chatLoad ->
            listOf(prof, chatLoad)
        }
    ) { part1, part2, part3 ->
        val settings = part1[0] as LocalSettings
        MainUiState(
            settings = settings,
            phoneState = part1[1] as PhoneState,
            bleState = part1[2] as ConnectionState,
            bleDevices = part1[3] as List<DiscoveredBleDevice>,
            bleRssi = part1[4] as Int?,
            bleProtocolStatus = part1[5] as BleProtocolStatus,
            signedIn = (part1[6] as Boolean) || settings.demoLoggedIn || !settings.xiaozhiAccessToken.isNullOrBlank(),
            email = (part1[7] as String?) ?: settings.demoEmail ?: settings.xiaozhiUsername ?: (part3[0] as XiaozhiProfileData?)?.user?.username,
            credentials = part1[8] as DeviceCredentialsManager.Credentials?,
            busy = part2[0] as Boolean,
            message = part2[1] as String?,
            initialized = part2[2] as Boolean,
            connectedDevice = part2[3] as ConnectedDeviceInfo?,
            wifiNetworks = part2[4] as List<DiscoveredWifiNetwork>,
            firmwareUpdate = part2[5] as FirmwareUpdateState,
            xiaozhiMcpStatus = part2[6] as XiaozhiMcpStatus,
            xiaozhiRelays = part2[7] as List<SmartHomeRelay>,
            xiaozhiChatMessages = part2[8] as List<XiaozhiChatMessage>,
            xiaozhiProfile = part3[0] as XiaozhiProfileData?,
            xiaozhiChatLoading = part3[1] as Boolean,
            firebaseReady = c.auth.isFirebaseReady()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    init {
        viewModelScope.launch {
            credentials.value = c.credentials.getOrCreate()
            c.systemCollector.refresh()
            val initialSettings = c.settings.settings.first()
            if (!initialSettings.xiaozhiAccessToken.isNullOrBlank()) {
                signedIn.value = true
                if (!initialSettings.xiaozhiUsername.isNullOrBlank()) {
                    email.value = initialSettings.xiaozhiUsername
                }
            }
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
            // If Xiaozhi mode is already active, refresh dashboard in background
            if (initialSettings.operatingMode == "XIAOZHI_AI" && initialSettings.xiaozhiAccessToken != null) {
                refreshXiaozhiDashboard()
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

    // Operating Mode Management
    fun setOperatingMode(mode: String) = viewModelScope.launch {
        c.settings.setOperatingMode(mode)
        if (mode == "XIAOZHI_AI") {
            refreshXiaozhiDashboard()
        }
    }

private suspend fun syncFirebaseSession(username: String, pass: String) {
        if (!c.auth.isFirebaseReady()) return
        val fbEmail = if (username.contains('@')) username.trim() else "${username.lowercase().trim()}@xiaozhi.biz.id"
        val loginRes = c.auth.login(fbEmail, pass)
        if (loginRes is AuthResult.Error) {
            c.auth.register(fbEmail, pass)
        }
    }

    private suspend fun syncFirebaseFromOAuth(username: String, emailParam: String?) {
        if (!c.auth.isFirebaseReady()) return
        val fbEmail = if (!emailParam.isNullOrBlank() && emailParam.contains('@')) emailParam.trim() else "${username.lowercase().trim()}@xiaozhi.biz.id"
        val defPass = "Xz@${username.take(10)}#2026!"
        val loginRes = c.auth.login(fbEmail, defPass)
        if (loginRes is AuthResult.Error) {
            c.auth.register(fbEmail, defPass)
        }
    }

    // Xiaozhi AI Methods
    fun xiaozhiLogin(username: String, password: String, onResult: (Boolean, String?) -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            val res = c.xiaozhi.login(username, password)
            if (res.success && res.accessToken != null) {
                signedIn.value = true
                email.value = username
                syncFirebaseSession(username, password)
                val mcp = c.xiaozhi.getMcpStatus()
                xiaozhiMcpStatus.value = mcp
                val isAdmin = res.user?.role?.equals("admin", ignoreCase = true) == true
                if (mcp.connected || isAdmin) {
                    onResult(true, null)
                    refreshXiaozhiDashboard()
                } else {
                    onResult(true, "MCP_REQUIRED")
                }
            } else {
                onResult(false, res.message.ifBlank { "Login gagal. Periksa username dan password." })
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Gagal terhubung ke server.")
        } finally {
            busy.value = false
        }
    }

    fun xiaozhiRegister(username: String, password: String, onResult: (Boolean, String?) -> Unit) = viewModelScope.launch {
        busy.value = true
        try {
            val res = c.xiaozhi.register(username, password)
            if (res.success && res.accessToken != null) {
                signedIn.value = true
                email.value = username
                syncFirebaseSession(username, password)
                val mcp = c.xiaozhi.getMcpStatus()
                xiaozhiMcpStatus.value = mcp
                loadXiaozhiProfile()
                if (mcp.connected) {
                    refreshXiaozhiDashboard()
                    onResult(true, null)
                } else {
                    onResult(true, "MCP_REQUIRED")
                }
            } else {
                onResult(false, res.message.ifBlank { "Registrasi gagal." })
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Gagal terhubung ke server.")
        } finally {
            busy.value = false
        }
    }

    fun xiaozhiGoogleAuth(
        idToken: String,
        action: String = "login",
        onResult: (Boolean, String?) -> Unit
    ) = viewModelScope.launch {
        busy.value = true
        try {
            val res = c.xiaozhi.googleAuth(idToken, action)
            if (res.success) {
                if (action != "link") {
                    signedIn.value = true
                    val userEmail = res.user?.googleEmail ?: res.user?.username ?: "Google User"
                    email.value = userEmail
                    if (c.auth.isFirebaseReady()) {
                        c.auth.loginWithGoogleIdToken(idToken)
                    }
                    val mcp = c.xiaozhi.getMcpStatus()
                    xiaozhiMcpStatus.value = mcp
                    loadXiaozhiProfile()
                    val isAdmin = res.user?.role?.equals("admin", ignoreCase = true) == true
                    if (mcp.connected || isAdmin) {
                        refreshXiaozhiDashboard()
                        onResult(true, null)
                    } else {
                        onResult(true, "MCP_REQUIRED")
                    }
                } else {
                    loadXiaozhiProfile()
                    onResult(true, res.message.ifBlank { "Autentikasi Google berhasil." })
                }
            } else {
                onResult(false, res.message.ifBlank { "Autentikasi Google gagal." })
            }
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Gagal terhubung ke server Google Auth.")
        } finally {
            busy.value = false
        }
    }

    fun xiaozhiUnlinkGoogle(
        onResult: (Boolean, String?) -> Unit
    ) = viewModelScope.launch {
        busy.value = true
        try {
            c.xiaozhi.unlinkGoogle().fold(
                onSuccess = {
                    loadXiaozhiProfile()
                    onResult(true, "Tautan Google berhasil dilepas.")
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage ?: "Gagal melepas tautan Google.")
                }
            )
        } catch (e: Exception) {
            onResult(false, e.localizedMessage ?: "Terjadi kesalahan saat melepas tautan Google.")
        } finally {
            busy.value = false
        }
    }

    fun xiaozhiSaveAndConnectMcp(
        mcpToken: String,
        onUpdate: (XiaozhiMcpStatus) -> Unit,
        onDone: (Boolean) -> Unit
    ) = viewModelScope.launch {
        val saveRes = c.xiaozhi.saveMcpToken(mcpToken)
        if (saveRes.isFailure) {
            onDone(false)
            return@launch
        }
        val connected = c.xiaozhi.pollMcpConnection { status ->
            xiaozhiMcpStatus.value = status
            onUpdate(status)
        }
        if (connected) {
            refreshXiaozhiDashboard()
        }
        onDone(connected)
    }

    fun refreshXiaozhiDashboard() = viewModelScope.launch {
        _xiaozhiDashboardLoading.value = true
        try {
            c.xiaozhi.getDashboard().onSuccess { data ->
                _xiaozhiDashboardData.value = data
                xiaozhiMcpStatus.value = data.mcpStatus
            }
            c.xiaozhi.getSmartHomeRelays().onSuccess { relays ->
                xiaozhiRelays.value = relays
            }
        } catch (_: Exception) {} finally {
            _xiaozhiDashboardLoading.value = false
        }
    }

    fun xiaozhiCreateMaterial(
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.createMaterial(title, category, content, keywords, apiUrl).fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiUpdateMaterial(
        id: Int,
        title: String,
        category: String,
        content: String,
        keywords: String = "",
        apiUrl: String = "",
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.updateMaterial(id, title, category, content, keywords, apiUrl).fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiDeleteMaterial(
        id: Int,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.deleteMaterial(id).fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiCreateCategory(
        name: String,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.createCategory(name).fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiDeleteCategory(
        catId: Int,
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.deleteCategory(catId).fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiReconnectMcp(
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.reconnectMcp().fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun xiaozhiDeleteMcpToken(
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.deleteMcpToken().fold(
            onSuccess = {
                refreshXiaozhiDashboard()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun loadXiaozhiChatHistory(query: String = "", date: String = "") = viewModelScope.launch {
        xiaozhiChatLoading.value = true
        try {
            c.xiaozhi.getChatHistory(query, 100, date).onSuccess { data ->
                _xiaozhiChatHistory.value = data
                xiaozhiChatMessages.value = data.items
            }
        } finally {
            xiaozhiChatLoading.value = false
        }
    }

    fun xiaozhiClearChatHistory(
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        c.xiaozhi.clearChatHistory().fold(
            onSuccess = {
                loadXiaozhiChatHistory()
                onResult(true, null)
            },
            onFailure = { onResult(false, it.localizedMessage) }
        )
    }

    fun loadXiaozhiProfile() = viewModelScope.launch {
        c.xiaozhi.getProfileData().onSuccess { profile ->
            xiaozhiProfile.value = profile
        }
    }

    fun xiaozhiScanPersona(
        onResult: (Boolean, String?) -> Unit = { _, _ -> }
    ) = viewModelScope.launch {
        _xiaozhiScanningPersona.value = true
        try {
            c.xiaozhi.scanPersona().fold(
                onSuccess = { persona ->
                    val current = xiaozhiProfile.value
                    if (current != null) {
                        xiaozhiProfile.value = current.copy(personaAnalysis = persona)
                    }
                    onResult(true, null)
                },
                onFailure = { onResult(false, it.localizedMessage) }
            )
        } finally {
            _xiaozhiScanningPersona.value = false
        }
    }

    fun xiaozhiToggleRelay(channel: Int, state: Boolean) = viewModelScope.launch {
        c.xiaozhi.setRelay(channel, state).onSuccess {
            val current = xiaozhiRelays.value.map {
                if (it.channel == channel) it.copy(state = state) else it
            }
            xiaozhiRelays.value = current
        }
    }


    fun loadXiaozhiAdminUsers() = viewModelScope.launch {
        _xiaozhiAdminUsersLoading.value = true
        try {
            val res = c.xiaozhi.getAdminUsers()
            res.onSuccess { users ->
                _xiaozhiAdminUsers.value = users
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Gagal memuat daftar pengguna.")
            }
        } catch (e: Exception) {
            showMessage("Error memuat user: ${e.localizedMessage}")
        } finally {
            _xiaozhiAdminUsersLoading.value = false
        }
    }

    fun handleOAuthCallback(uri: Uri) = viewModelScope.launch {
        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            showMessage("Autentikasi Google: $error")
            return@launch
        }
        val action = uri.getQueryParameter("action")
        if (action == "link") {
            val msg = uri.getQueryParameter("msg") ?: "Akun Google berhasil ditautkan!"
            loadXiaozhiProfile()
            showMessage(msg)
            return@launch
        }
        val accessToken = uri.getQueryParameter("access_token")
        val refreshToken = uri.getQueryParameter("refresh_token") ?: ""
        val username = uri.getQueryParameter("username") ?: ""
        val role = uri.getQueryParameter("role") ?: "user"
        val userId = uri.getQueryParameter("user_id")?.toIntOrNull() ?: 1

        if (!accessToken.isNullOrBlank()) {
            busy.value = true
            try {
                setOperatingMode("XIAOZHI_AI")
                val mcp = c.xiaozhi.saveDirectSession(accessToken, refreshToken, username, userId, role)
                signedIn.value = true
                email.value = username
                syncFirebaseFromOAuth(username, uri.getQueryParameter("email"))
                xiaozhiMcpStatus.value = mcp
                loadXiaozhiProfile()
                refreshXiaozhiDashboard()
                showMessage("Selamat datang di Xichi, @$username!")
            } catch (e: Exception) {
                showMessage("Gagal memproses login Google: ${e.localizedMessage}")
            } finally {
                busy.value = false
            }
        }
    }

    fun performCompleteLogout(onComplete: (() -> Unit)? = null) = viewModelScope.launch {
        busy.value = true
        try {
            // 1. Send clear config to BLE if connected, then disconnect
            if (c.ble.isConnected()) {
                runCatching { c.router.sendClearConfig() }
                kotlinx.coroutines.delay(300)
                runCatching { c.ble.disconnect() }
            }
            // 2. Stop device connection foreground service
            runCatching {
                getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
            }
            // 3. Clear Xiaozhi session from repository & DataStore
            runCatching { c.xiaozhi.logout() }
            runCatching {
                c.settings.clearXiaozhiSession()
                c.settings.clearLocalSession()
            }
            // 4. Sign out Firebase & local auth
            runCatching { c.auth.logout() }
            // 5. Reset all in-memory flows
            signedIn.value = false
            email.value = null
            xiaozhiProfile.value = null
            xiaozhiChatMessages.value = emptyList()
            _xiaozhiAdminUsers.value = emptyList()
            _xiaozhiDashboardData.value = XiaozhiDashboardData()
            _xiaozhiChatHistory.value = XiaozhiChatHistoryData()
            xiaozhiMcpStatus.value = XiaozhiMcpStatus(connected = false, statusText = "Belum login")
            message.value = "Anda telah berhasil logout sepenuhnya."
            onComplete?.invoke()
        } catch (e: Exception) {
            message.value = "Logout error: ${e.localizedMessage}"
            onComplete?.invoke()
        } finally {
            busy.value = false
        }
    }

    fun logout() = performCompleteLogout()
    fun xiaozhiLogout() = performCompleteLogout()

    // Unified Chronchi BLE / Xiaozhi Auth Methods
    fun login(emailValue: String, password: String) = viewModelScope.launch {
        xiaozhiLogin(emailValue.trim(), password) { success, msg ->
            if (success) {
                signedIn.value = true
                email.value = emailValue.trim()
                registerCloudDeviceInBackground()
            } else {
                message.value = msg ?: "Login gagal. Periksa username dan password."
            }
        }
    }

    fun register(emailValue: String, password: String) = viewModelScope.launch {
        xiaozhiRegister(emailValue.trim(), password) { success, msg ->
            if (success) {
                signedIn.value = true
                email.value = emailValue.trim()
                registerCloudDeviceInBackground()
            } else {
                message.value = msg ?: "Registrasi gagal."
            }
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

    fun forgetFirebase() = viewModelScope.launch {
        if (c.ble.isConnected()) {
            c.router.sendClearConfig()
        }
    }

    fun sendFirebaseConfig() = viewModelScope.launch {
        val creds = credentials.value ?: return@launch
        c.router.sendFirebaseConfig()
    }

    fun scanWifi() = viewModelScope.launch {
        c.router.sendWifiScan()
    }

    fun checkFirebaseStatus() = viewModelScope.launch {
        c.router.sendFirebaseStatusRequest()
    }

    fun scanBle() = viewModelScope.launch {
        c.ble.startScan()
    }

    fun connect(device: DiscoveredBleDevice) = viewModelScope.launch {
        c.settings.setTrustedDevice(device.address, device.name)
        c.ble.connect(device.address)
        startBackgroundServiceIfEnabled()
    }

    fun reconnect() = viewModelScope.launch {
        val s = c.settings.settings.first()
        val address = s.trustedDeviceAddress ?: return@launch
        c.ble.reconnect(address)
        startBackgroundServiceIfEnabled()
    }

    fun disconnect() = viewModelScope.launch {
        c.ble.disconnect()
        getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
    }

    fun forgetDevice() = viewModelScope.launch {
        c.settings.setTrustedDevice(null, null)
        c.ble.disconnect()
        getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
    }

    fun completeOnboarding() = viewModelScope.launch {
        c.settings.setOnboardingComplete(true)
        startBackgroundServiceIfEnabled()
    }

    fun setSourceEnabled(sourceId: String, enabled: Boolean) = viewModelScope.launch {
        c.settings.setSourceEnabled(sourceId, enabled)
    }

    fun setConnectionMode(mode: ConnectionMode) = viewModelScope.launch {
        c.settings.setConnectionMode(mode)
    }

    fun setAutoConnect(enabled: Boolean) = viewModelScope.launch {
        c.settings.setAutoConnect(enabled)
    }

    fun setCloudSync(enabled: Boolean) = viewModelScope.launch {
        c.settings.setCloudSyncEnabled(enabled)
    }

    fun setKeepBackground(enabled: Boolean) = viewModelScope.launch {
        c.settings.setKeepBackgroundConnection(enabled)
        if (enabled) startBackgroundServiceIfEnabled()
        else getApplication<Application>().stopService(Intent(getApplication(), DeviceConnectionService::class.java))
    }

    fun setAppTheme(theme: AppTheme) = viewModelScope.launch {
        c.settings.setAppTheme(theme)
    }

    fun refreshPhoneState() = viewModelScope.launch {
        c.systemCollector.refresh()
    }

    fun regenerateCredentials() = viewModelScope.launch {
        credentials.value = c.credentials.regenerate()
    }

    fun checkFirmwareUpdate() = viewModelScope.launch {
        val info = uiState.value.connectedDevice ?: return@launch
        c.firmwareUpdates.check(info)
    }

    fun installFirmwareUpdate() = viewModelScope.launch {
        c.firmwareUpdates.install()
    }

    fun showMessage(msg: String) {
        message.value = msg
    }

    fun clearMessage() {
        message.value = null
    }

    fun sendSwitchMode(mode: String) = viewModelScope.launch {
        c.router.sendSwitchMode(mode)
    }

    fun sendWifiConfig(ssid: String, pass: String) = viewModelScope.launch {
        c.router.sendWifiConfig(ssid, pass)
    }

    private fun registerCloudDeviceInBackground() = viewModelScope.launch {
        val creds = credentials.value ?: return@launch
        val uid = c.auth.currentUid() ?: return@launch
        c.cloud.registerDevice(uid, creds.deviceId)
    }

    private fun startBackgroundServiceIfEnabled() {
        if (!PermissionUtils.bluetoothGranted(getApplication())) return
        val intent = Intent(getApplication(), DeviceConnectionService::class.java)
        runCatching { ContextCompat.startForegroundService(getApplication(), intent) }
    }
}
