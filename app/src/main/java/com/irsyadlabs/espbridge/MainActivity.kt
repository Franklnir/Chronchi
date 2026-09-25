package com.irsyadlabs.espbridge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.irsyadlabs.espbridge.ui.navigation.AppNavHost
import com.irsyadlabs.espbridge.ui.theme.XichiTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            XichiTheme(theme = state.settings.appTheme) {
                AppNavHost(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "espbridge" && uri.host == "oauth") {
            viewModel.handleOAuthCallback(uri)
        }
    }
}
