package com.irsyadlabs.espbridge.ui.navigation

import com.irsyadlabs.espbridge.ui.components.InAppGoogleAuthDialog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.irsyadlabs.espbridge.EspBridgeApp
import com.irsyadlabs.espbridge.MainViewModel
import com.irsyadlabs.espbridge.ui.screens.home.HomeScreen
import com.irsyadlabs.espbridge.ui.screens.mode.ModeSelectionScreen
import com.irsyadlabs.espbridge.ui.screens.onboarding.PermissionOnboardingScreen
import com.irsyadlabs.espbridge.ui.screens.settings.SettingsScreen
import com.irsyadlabs.espbridge.ui.screens.setup.SetupScreen
import com.irsyadlabs.espbridge.ui.screens.wificonfig.WifiConfigScreen
import com.irsyadlabs.espbridge.ui.screens.xiaozhi.XiaozhiAuthScreen
import com.irsyadlabs.espbridge.ui.screens.xiaozhi.XiaozhiChatHistoryScreen
import com.irsyadlabs.espbridge.ui.screens.xiaozhi.XiaozhiDashboardScreen
import com.irsyadlabs.espbridge.ui.screens.xiaozhi.XiaozhiProfileScreen
import com.irsyadlabs.espbridge.ui.screens.xiaozhi.XiaozhiUserListScreen
import com.irsyadlabs.espbridge.ui.theme.NeoTokens
import com.irsyadlabs.espbridge.ui.theme.PaperWhite

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_MODE_SELECT = "mode_select"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_PERMISSIONS = "permissions"
private const val ROUTE_XIAOZHI_AUTH = "xiaozhi_auth"

@Composable
fun AppNavHost(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val mainRoutes = mainDestinations.map { it.route }.toSet()
    val xiaozhiRoutes = xiaozhiDestinations.map { it.route }.toSet()
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as EspBridgeApp

    val isXiaozhiMode = state.settings.operatingMode == "XIAOZHI_AI"

    val companionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val device = app.container.companionAssociation.extractDevice(result.data)
        if (device != null) {
            viewModel.connect(device)
        } else if (result.resultCode != Activity.RESULT_OK) {
            viewModel.showMessage("Companion pairing dibatalkan.")
        }
    }

    val beginCompanionPairing = {
        app.container.companionAssociation.requestAssociation(
            onChooser = { sender -> companionLauncher.launch(IntentSenderRequest.Builder(sender).build()) },
            onFailure = viewModel::showMessage
        )
    }

    // Google Sign-In Client ID untuk Xiaozhi AI & Backend Terpadu
    val xiaozhiWebClientId = "1073158241145-68rt5j4ekpji59f7ot1k91jq3qquv5l4.apps.googleusercontent.com"
    var pendingXiaozhiGoogleAction by remember { mutableStateOf("login") }
    var pendingXiaozhiGoogleCallback by remember { mutableStateOf<((Boolean, String?) -> Unit)?>(null) }
    var inAppGoogleAuthUrl by remember { mutableStateOf<String?>(null) }

    val xiaozhiGoogleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        var apiExceptionCode: Int? = null
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        } catch (e: ApiException) {
            apiExceptionCode = e.statusCode
            null
        } catch (e: Exception) {
            null
        }
        val token = account?.idToken
        val action = pendingXiaozhiGoogleAction
        val cb = pendingXiaozhiGoogleCallback
        if (token != null) {
            viewModel.xiaozhiGoogleAuth(token, action) { success, msg ->
                cb?.invoke(success, msg)
                if (success) {
                    if (action == "link") {
                        viewModel.showMessage(msg ?: "Akun Google berhasil ditautkan!")
                    } else if (state.settings.operatingMode == "CHRONCHI_BLE") {
                        viewModel.showMessage("Selamat datang di Chronchi!")
                        val target = if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        navController.navigate(target) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    } else if (msg != "MCP_REQUIRED") {
                        viewModel.showMessage("Selamat datang di Xichi!")
                        navController.navigate(XiaozhiDestination.Dashboard.route) {
                            popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                        }
                    }
                } else {
                    viewModel.showMessage(msg ?: "Autentikasi Google gagal.")
                }
            }
        } else if (apiExceptionCode != null && apiExceptionCode != 12501) {
            // ApiException (10 = DEVELOPER_ERROR / SHA-1 mismatch, etc.) -> Seamless 100% In-App Web OAuth!
            viewModel.showMessage("Membuka login Google di dalam aplikasi...")
            val webUri = if (action == "link") {
                val tok = state.settings.xiaozhiAccessToken ?: ""
                "https://xiaozhiscig.biz.id/api/auth/google/link?token=${Uri.encode(tok)}&source=mobile_app"
            } else {
                "https://xiaozhiscig.biz.id/api/auth/google/login?intent=$action&source=mobile_app"
            }
            inAppGoogleAuthUrl = webUri
        } else {
            cb?.invoke(false, "Google Sign-In dibatalkan.")
            viewModel.showMessage("Google Sign-In dibatalkan.")
        }
    }

    val launchXiaozhiGoogle: (String, ((Boolean, String?) -> Unit)?) -> Unit = { action, onComplete ->
        pendingXiaozhiGoogleAction = action
        pendingXiaozhiGoogleCallback = onComplete
        if (activity != null) {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(xiaozhiWebClientId)
                .requestEmail()
                .build()
            val client = GoogleSignIn.getClient(activity, options)
            client.signOut().addOnCompleteListener {
                xiaozhiGoogleLauncher.launch(client.signInIntent)
            }
        } else {
            onComplete?.invoke(false, "Activity tidak tersedia untuk Google Sign-In.")
            viewModel.showMessage("Activity tidak tersedia untuk Google Sign-In.")
        }
    }

    Scaffold(
        containerColor = if (isXiaozhiMode) NeoTokens.Cream else PaperWhite,
        bottomBar = {
            if (route in mainRoutes) {
                MainBottomBar(route) { target ->
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            } else if (route in xiaozhiRoutes) {
                val isAdmin = state.settings.xiaozhiRole.equals("admin", ignoreCase = true) || state.settings.xiaozhiUsername.equals("admin", ignoreCase = true)
                XiaozhiBottomBar(route, isAdmin = isAdmin) { target ->
                    navController.navigate(target) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(ROUTE_SPLASH) {
                Box(Modifier.fillMaxSize().background(NeoTokens.Cream), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeoTokens.Emerald)
                }
                LaunchedEffect(state.initialized) {
                    if (!state.initialized) return@LaunchedEffect
                    val mode = state.settings.operatingMode
                    val target = when {
                        mode.isBlank() -> ROUTE_MODE_SELECT
                        mode == "XIAOZHI_AI" -> {
                            val hasToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                            val mcpOk = state.settings.xiaozhiMcpConnected
                            if (hasToken && mcpOk) XiaozhiDestination.Dashboard.route else ROUTE_XIAOZHI_AUTH
                        }
                        !state.signedIn -> ROUTE_XIAOZHI_AUTH
                        !state.settings.onboardingComplete -> ROUTE_PERMISSIONS
                        else -> MainDestination.Home.route
                    }
                    navController.navigate(target) { popUpTo(ROUTE_SPLASH) { inclusive = true } }
                }
            }

            composable(ROUTE_MODE_SELECT) {
                ModeSelectionScreen(
                    onSelectChronchi = {
                        viewModel.setOperatingMode("CHRONCHI_BLE")
                        val target = when {
                            !state.signedIn -> ROUTE_XIAOZHI_AUTH
                            !state.settings.onboardingComplete -> ROUTE_PERMISSIONS
                            else -> MainDestination.Home.route
                        }
                        navController.navigate(target) { popUpTo(ROUTE_MODE_SELECT) { inclusive = true } }
                    },
                    onSelectXiaozhi = {
                        viewModel.setOperatingMode("XIAOZHI_AI")
                        val hasToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                        val mcpOk = state.settings.xiaozhiMcpConnected
                        val target = if (hasToken && mcpOk) XiaozhiDestination.Dashboard.route else ROUTE_XIAOZHI_AUTH
                        navController.navigate(target) { popUpTo(ROUTE_MODE_SELECT) { inclusive = true } }
                    }
                )
            }

            // ── Xiaozhi AI Routes ──
            composable(ROUTE_XIAOZHI_AUTH) {
                XiaozhiAuthScreen(
                    operatingMode = "XIAOZHI_AI",
                    isLoggedIn = !state.settings.xiaozhiAccessToken.isNullOrBlank(),
                    currentUsername = state.settings.xiaozhiUsername ?: state.email,
                    onLogin = viewModel::xiaozhiLogin,
                    onRegister = viewModel::xiaozhiRegister,
                    onGoogleAuth = { isRegister, onComplete ->
                        launchXiaozhiGoogle(if (isRegister) "register" else "login") { success, msg ->
                            onComplete(success, msg)
                            if (success) {
                                val target = if (state.settings.operatingMode == "CHRONCHI_BLE") {
                                    if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                                } else {
                                    XiaozhiDestination.Dashboard.route
                                }
                                navController.navigate(target) {
                                    popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                                }
                            }
                        }
                    },
                    onGoogleWebAuth = { isRegister ->
                        val action = if (isRegister) "register" else "login"
                        pendingXiaozhiGoogleAction = action
                        inAppGoogleAuthUrl = "https://xiaozhiscig.biz.id/api/auth/google/login?intent=$action&source=mobile_app"
                    },
                    onSaveAndConnectMcp = viewModel::xiaozhiSaveAndConnectMcp,
                    onAuthSuccessAndConnected = {
                        val target = if (state.settings.operatingMode == "CHRONCHI_BLE") {
                            if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        } else {
                            XiaozhiDestination.Dashboard.route
                        }
                        navController.navigate(target) {
                            popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                        }
                    },
                    onSwitchToChronchi = {
                        viewModel.setOperatingMode("CHRONCHI_BLE")
                        val target = if (state.signedIn) {
                            if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        } else {
                            ROUTE_XIAOZHI_AUTH
                        }
                        navController.navigate(target) {
                            popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                        }
                    },
                    onSwitchToXiaozhi = null,
                    onLogout = {
                        if (activity != null) {
                            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(activity, options).signOut()
                        }
                        viewModel.performCompleteLogout()
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard Xiaozhi AI: Wajib Login & Wajib Terkoneksi MCP
            composable(XiaozhiDestination.Dashboard.route) {
                val hasXiaozhiToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                val isMcpConnected = state.settings.xiaozhiMcpConnected

                LaunchedEffect(hasXiaozhiToken, isMcpConnected) {
                    if (!hasXiaozhiToken || !isMcpConnected) {
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(XiaozhiDestination.Dashboard.route) { inclusive = true }
                        }
                    }
                }

                if (!hasXiaozhiToken || !isMcpConnected) {
                    Box(Modifier.fillMaxSize().background(NeoTokens.Cream), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeoTokens.Emerald)
                    }
                    return@composable
                }

                val dashboardData by viewModel.xiaozhiDashboardData.collectAsState()
                val isDashLoading by viewModel.xiaozhiDashboardLoading.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.refreshXiaozhiDashboard()
                }
                XiaozhiDashboardScreen(
                    dashboardData = dashboardData,
                    isLoading = isDashLoading,
                    onRefresh = viewModel::refreshXiaozhiDashboard,
                    onCreateMaterial = { t, cat, cont, kw, api ->
                        viewModel.xiaozhiCreateMaterial(t, cat, cont, kw, api)
                    },
                    onUpdateMaterial = { id, t, cat, cont, kw, api ->
                        viewModel.xiaozhiUpdateMaterial(id, t, cat, cont, kw, api)
                    },
                    onDeleteMaterial = { id ->
                        viewModel.xiaozhiDeleteMaterial(id)
                    },
                    onCreateCategory = { name ->
                        viewModel.xiaozhiCreateCategory(name)
                    },
                    onDeleteCategory = { id ->
                        viewModel.xiaozhiDeleteCategory(id)
                    },
                    onSaveMcpToken = { token ->
                        viewModel.xiaozhiSaveAndConnectMcp(token, {}, {})
                    },
                    onReconnectMcp = {
                        viewModel.xiaozhiReconnectMcp { _, _ -> }
                    },
                    onDeleteMcpToken = {
                        viewModel.xiaozhiDeleteMcpToken { _, _ -> }
                    },
                    onNavigateToProfile = { navController.navigate(XiaozhiDestination.Profile.route) }
                )
            }

            // Chat History: Wajib Login & Wajib Terkoneksi MCP
            composable(XiaozhiDestination.ChatHistory.route) {
                val hasXiaozhiToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                val isMcpConnected = state.settings.xiaozhiMcpConnected

                LaunchedEffect(hasXiaozhiToken, isMcpConnected) {
                    if (!hasXiaozhiToken || !isMcpConnected) {
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(XiaozhiDestination.ChatHistory.route) { inclusive = true }
                        }
                    }
                }

                if (!hasXiaozhiToken || !isMcpConnected) {
                    Box(Modifier.fillMaxSize().background(NeoTokens.Cream), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeoTokens.Emerald)
                    }
                    return@composable
                }

                val chatHistory by viewModel.xiaozhiChatHistory.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.loadXiaozhiChatHistory()
                }
                XiaozhiChatHistoryScreen(
                    chatData = chatHistory,
                    isLoading = state.xiaozhiChatLoading,
                    onSearch = { q, d -> viewModel.loadXiaozhiChatHistory(q, d) },
                    onRefresh = { viewModel.loadXiaozhiChatHistory() }
                )
            }

            // User List (Admin): Wajib Login & Wajib Terkoneksi MCP
            composable(XiaozhiDestination.UserList.route) {
                val hasXiaozhiToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                val isMcpConnected = state.settings.xiaozhiMcpConnected

                LaunchedEffect(hasXiaozhiToken, isMcpConnected) {
                    if (!hasXiaozhiToken || !isMcpConnected) {
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(XiaozhiDestination.UserList.route) { inclusive = true }
                        }
                    }
                }

                if (!hasXiaozhiToken || !isMcpConnected) {
                    Box(Modifier.fillMaxSize().background(NeoTokens.Cream), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeoTokens.Emerald)
                    }
                    return@composable
                }

                val adminUsers by viewModel.xiaozhiAdminUsers.collectAsState()
                val isUsersLoading by viewModel.xiaozhiAdminUsersLoading.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.loadXiaozhiAdminUsers()
                }
                XiaozhiUserListScreen(
                    users = adminUsers,
                    isLoading = isUsersLoading,
                    onRefresh = viewModel::loadXiaozhiAdminUsers
                )
            }

            // Profile Xiaozhi: Wajib Login
            composable(XiaozhiDestination.Profile.route) {
                val hasXiaozhiToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()

                LaunchedEffect(hasXiaozhiToken) {
                    if (!hasXiaozhiToken) {
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(XiaozhiDestination.Profile.route) { inclusive = true }
                        }
                    }
                }

                if (!hasXiaozhiToken) {
                    Box(Modifier.fillMaxSize().background(NeoTokens.Cream), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeoTokens.Emerald)
                    }
                    return@composable
                }

                val isScanning by viewModel.xiaozhiScanningPersona.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.loadXiaozhiProfile()
                }
                XiaozhiProfileScreen(
                    profileData = state.xiaozhiProfile,
                    isScanningPersona = isScanning,
                    onScanPersona = { viewModel.xiaozhiScanPersona { _, _ -> } },
                    onLinkGoogle = { launchXiaozhiGoogle("link", null) },
                    onUnlinkGoogle = {
                        viewModel.xiaozhiUnlinkGoogle { success, msg ->
                            viewModel.showMessage(msg ?: if (success) "Tautan Google berhasil dilepas." else "Gagal melepas tautan Google.")
                        }
                    },
                    onSwitchToChronchi = {
                        viewModel.setOperatingMode("CHRONCHI_BLE")
                        val target = if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onLogout = {
                        if (activity != null) {
                            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(activity, options).signOut()
                        }
                        viewModel.performCompleteLogout()
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onRefresh = { viewModel.loadXiaozhiProfile() }
                )
            }

            // ── Chronchi BLE Routes (Unifikasi Akun Terpadu Xiaozhi AI & Firebase) ──
            composable(ROUTE_LOGIN) {
                LaunchedEffect(Unit) {
                    navController.navigate(ROUTE_XIAOZHI_AUTH) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            }

            composable(ROUTE_REGISTER) {
                LaunchedEffect(Unit) {
                    navController.navigate(ROUTE_XIAOZHI_AUTH) {
                        popUpTo(ROUTE_REGISTER) { inclusive = true }
                    }
                }
            }

            composable(ROUTE_PERMISSIONS) {
                PermissionOnboardingScreen {
                    viewModel.completeOnboarding()
                    navController.navigate(MainDestination.Home.route) {
                        popUpTo(ROUTE_PERMISSIONS) { inclusive = true }
                    }
                }
            }

            composable(MainDestination.Home.route) {
                HomeScreen(state, viewModel::setSourceEnabled, viewModel::refreshPhoneState)
            }

            composable(MainDestination.Setup.route) {
                SetupScreen(
                    state = state,
                    onMode = viewModel::setConnectionMode,
                    onCompanionPair = beginCompanionPairing,
                    onScan = viewModel::scanBle,
                    onConnect = viewModel::connect,
                    onSendWifiConfig = { ssid, password -> viewModel.sendWifiConfig(ssid, password) },
                    onReconnect = viewModel::reconnect,
                    onDisconnect = viewModel::disconnect,
                    onForget = viewModel::forgetDevice,
                    onRegenerateCredentials = viewModel::regenerateCredentials
                )
            }

            composable(MainDestination.WifiConfig.route) {
                WifiConfigScreen(
                    state = state,
                    onSendWifiConfig = { ssid, password -> viewModel.sendWifiConfig(ssid, password) },
                    onSwitchMode = { mode -> viewModel.sendSwitchMode(mode) },
                    onScanWifi = { viewModel.scanWifi() },
                    onSendFirebaseConfig = { viewModel.sendFirebaseConfig() },
                    onCheckFirebaseStatus = { viewModel.checkFirebaseStatus() },
                    onForgetFirebase = { viewModel.forgetFirebase() }
                )
            }

            composable(MainDestination.Settings.route) {
                SettingsScreen(
                    state = state,
                    onAutoConnect = viewModel::setAutoConnect,
                    onCloudSync = viewModel::setCloudSync,
                    onBackground = viewModel::setKeepBackground,
                    onAppTheme = viewModel::setAppTheme,
                    onCheckFirmware = viewModel::checkFirmwareUpdate,
                    onInstallFirmware = viewModel::installFirmwareUpdate,
                    onLogout = {
                        if (activity != null) {
                            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(activity, options).signOut()
                        }
                        viewModel.performCompleteLogout()
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchToXiaozhi = {
                        viewModel.setOperatingMode("XIAOZHI_AI")
                        val hasToken = !state.settings.xiaozhiAccessToken.isNullOrBlank()
                        val mcpOk = state.settings.xiaozhiMcpConnected
                        val target = if (hasToken && mcpOk) XiaozhiDestination.Dashboard.route else ROUTE_XIAOZHI_AUTH
                        navController.navigate(target) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
         }
        // ── 100% In-App Google OAuth Dialog (Tanpa Browser Luar) ────────────────
        if (inAppGoogleAuthUrl != null) {
            InAppGoogleAuthDialog(
                authUrl = inAppGoogleAuthUrl!!,
                onDismiss = {
                    inAppGoogleAuthUrl = null
                    pendingXiaozhiGoogleCallback?.invoke(false, "Google Sign-In dibatalkan.")
                },
                onCallback = { uri ->
                    inAppGoogleAuthUrl = null
                    viewModel.handleOAuthCallback(uri)
                    val action = uri.getQueryParameter("action") ?: pendingXiaozhiGoogleAction
                    val err = uri.getQueryParameter("error")
                    if (err.isNullOrBlank()) {
                        pendingXiaozhiGoogleCallback?.invoke(true, null)
                        if (action != "link") {
                            if (state.settings.operatingMode == "CHRONCHI_BLE") {
                                val target = if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                                navController.navigate(target) {
                                    popUpTo(ROUTE_LOGIN) { inclusive = true }
                                }
                            } else {
                                navController.navigate(XiaozhiDestination.Dashboard.route) {
                                    popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        pendingXiaozhiGoogleCallback?.invoke(false, err)
                    }
                }
            )
        }
    }
}
