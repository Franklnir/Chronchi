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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.irsyadlabs.espbridge.ui.screens.onboarding.PermissionOnboardingScreen
import com.irsyadlabs.espbridge.ui.screens.settings.SettingsScreen
import com.irsyadlabs.espbridge.ui.screens.wificonfig.WifiConfigScreen
import com.irsyadlabs.espbridge.ui.screens.setup.SetupScreen
import com.irsyadlabs.espbridge.ui.theme.PaperWhite

private const val ROUTE_SPLASH = "splash"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_REGISTER = "register"
private const val ROUTE_PERMISSIONS = "permissions"

@Composable
fun AppNavHost(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val mainRoutes = mainDestinations.map { it.route }.toSet()
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as EspBridgeApp

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

    Scaffold(
        containerColor = PaperWhite,
        bottomBar = {
            if (route in mainRoutes) {
                MainBottomBar(route) { target ->
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
                Box(Modifier.fillMaxSize().background(PaperWhite), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                LaunchedEffect(state.initialized) {
                    if (!state.initialized) return@LaunchedEffect
                    val target = when {
                        !state.signedIn -> ROUTE_LOGIN
                        !state.settings.onboardingComplete -> ROUTE_PERMISSIONS
                        else -> MainDestination.Home.route
                    }
                    navController.navigate(target) { popUpTo(ROUTE_SPLASH) { inclusive = true } }
                }
            }

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
                    }
                )
            }
        }
    }
}
