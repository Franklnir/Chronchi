package com.irsyadlabs.espbridge.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppGoogleAuthDialog(
    authUrl: String,
    onDismiss: () -> Unit,
    onCallback: (Uri) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 14.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .background(NeoTokens.White, RoundedCornerShape(16.dp))
                    .border(2.5.dp, NeoTokens.Black, RoundedCornerShape(16.dp))
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoTokens.Cream, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .border(
                            androidx.compose.foundation.BorderStroke(1.dp, NeoTokens.Black.copy(alpha = 0.15f)),
                            RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(NeoTokens.White, CircleShape)
                                .border(1.5.dp, NeoTokens.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NeoTokens.Blue)
                        }
                        Column {
                            Text(
                                text = "Autentikasi Akun Google",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = NeoTokens.Black
                            )
                            Text(
                                text = "100% In-App (Tanpa Keluar Aplikasi)",
                                fontSize = 10.5.sp,
                                color = NeoTokens.Emerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { webViewRef?.reload() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, "Muat Ulang", tint = NeoTokens.Black, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Close, "Tutup", tint = NeoTokens.Coral, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = NeoTokens.Blue,
                        trackColor = NeoTokens.Cream
                    )
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false

                                // Custom User Agent to bypass Google OAuth disallowed_useragent restriction
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val url = request?.url ?: return false
                                        val urlStr = url.toString()

                                        if (url.scheme == "espbridge" && url.host == "oauth") {
                                            onCallback(url)
                                            return true
                                        }

                                        if (urlStr.startsWith("espbridge://")) {
                                            onCallback(url)
                                            return true
                                        }

                                        return false
                                    }

                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true

                                        if (url != null && (url.startsWith("espbridge://oauth/") || url.startsWith("espbridge://"))) {
                                            onCallback(Uri.parse(url))
                                        }
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }
                                }

                                webViewRef = this
                                loadUrl(authUrl)
                            }
                        },
                        update = { webViewRef = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
