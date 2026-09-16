package com.irsyadlabs.espbridge.ui.navigation

import android.app.Activity
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
import com.irsyadlabs.espbridge.ui.screens.auth.LoginScreen
import com.irsyadlabs.espbridge.ui.screens.auth.RegisterScreen
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

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        }.getOrNull()
        val token = account?.idToken
        if (token != null) viewModel.firebaseGoogleToken(token)
        else viewModel.showMessage("Google Sign-In dibatalkan atau ID token tidak tersedia.")
    }

    val beginGoogleSignIn: () -> Unit = {
        if (!state.firebaseReady) {
            viewModel.googleDemoLogin()
        } else if (activity != null) {
            val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (id == 0) {
                viewModel.showMessage("Firebase Google OAuth client belum tersedia. Periksa google-services.json.")
            } else {
                val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(id))
                    .requestEmail()
                    .build()
                val client = GoogleSignIn.getClient(activity, options)
                googleLauncher.launch(client.signInIntent)
            }
        }
    }

    val xiaozhiWebClientId = "3260223826-k8qrmthkeegt36pvnbac3oqurnmcmnvq.apps.googleusercontent.com"
    var pendingXiaozhiGoogleAction by remember { mutableStateOf("login") }

    val xiaozhiGoogleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = runCatching {
            GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
        }.getOrNull()
        val token = account?.idToken
        val action = pendingXiaozhiGoogleAction
        if (token != null) {
            viewModel.xiaozhiGoogleAuth(token, action) { success, msg ->
                viewModel.showMessage(msg ?: if (success) "Autentikasi Google berhasil!" else "Autentikasi Google gagal.")
                if (success && (action == "login" || action == "register")) {
                    navController.navigate(XiaozhiDestination.Dashboard.route) {
                        popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                    }
                }
            }
        } else {
            viewModel.showMessage("Google Sign-In dibatalkan atau token tidak tersedia.")
        }
    }

    val launchXiaozhiGoogle: (String) -> Unit = { action ->
        pendingXiaozhiGoogleAction = action
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
                XiaozhiBottomBar(route) { target ->
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
                        !state.signedIn -> ROUTE_LOGIN
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
                            !state.signedIn -> ROUTE_LOGIN
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
                    onLogin = viewModel::xiaozhiLogin,
                    onRegister = viewModel::xiaozhiRegister,
                    onGoogleAuth = { isRegister -> launchXiaozhiGoogle(if (isRegister) "register" else "login") },
                    onSaveAndConnectMcp = viewModel::xiaozhiSaveAndConnectMcp,
                    onAuthSuccessAndConnected = {
                        navController.navigate(XiaozhiDestination.Dashboard.route) {
                            popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                        }
                    },
                    onSwitchToChronchi = {
                        viewModel.setOperatingMode("CHRONCHI_BLE")
                        navController.navigate(MainDestination.Home.route) {
                            popUpTo(ROUTE_XIAOZHI_AUTH) { inclusive = true }
                        }
                    }
                )
            }

            composable(XiaozhiDestination.Dashboard.route) {
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

            composable(XiaozhiDestination.ChatHistory.route) {
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

            composable(XiaozhiDestination.Profile.route) {
                val isScanning by viewModel.xiaozhiScanningPersona.collectAsState()
                LaunchedEffect(Unit) {
                    viewModel.loadXiaozhiProfile()
                }
                XiaozhiProfileScreen(
                    profileData = state.xiaozhiProfile,
                    isScanningPersona = isScanning,
                    onScanPersona = { viewModel.xiaozhiScanPersona { _, _ -> } },
                    onLinkGoogle = { launchXiaozhiGoogle("link") },
                    onUnlinkGoogle = {
                        viewModel.xiaozhiUnlinkGoogle { success, msg ->
                            viewModel.showMessage(msg ?: if (success) "Tautan Google berhasil dilepas." else "Gagal melepas tautan Google.")
                        }
                    },
                    onSwitchToChronchi = {
                        viewModel.setOperatingMode("CHRONCHI_BLE")
                        navController.navigate(MainDestination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onLogout = {
                        viewModel.xiaozhiLogout()
                        navController.navigate(ROUTE_XIAOZHI_AUTH) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onRefresh = { viewModel.loadXiaozhiProfile() }
                )
            }

            // ── Chronchi BLE Routes ──
            composable(ROUTE_LOGIN) {
                LaunchedEffect(state.signedIn) {
                    if (state.signedIn) {
                        val target = if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        navController.navigate(target) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                    }
                }
                LoginScreen(
                    busy = state.busy,
                    firebaseReady = state.firebaseReady,
                    message = state.message,
                    onLogin = viewModel::login,
                    onGoogle = beginGoogleSignIn,
                    onRegister = { viewModel.clearMessage(); navController.navigate(ROUTE_REGISTER) }
                )
            }

            composable(ROUTE_REGISTER) {
                LaunchedEffect(state.signedIn) {
                    if (state.signedIn) {
                        val target = if (state.settings.onboardingComplete) MainDestination.Home.route else ROUTE_PERMISSIONS
                        navController.navigate(target) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                    }
                }
                RegisterScreen(
                    busy = state.busy,
                    message = state.message,
                    onRegister = viewModel::register,
                    onGoogle = beginGoogleSignIn,
                    onBack = { navController.popBackStack() }
                )
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
                        viewModel.logout()
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
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
    }
}
