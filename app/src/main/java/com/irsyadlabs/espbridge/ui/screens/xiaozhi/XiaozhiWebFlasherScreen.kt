package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

private const val WEB_FLASHER_URL = "https://xiaozhiscig.biz.id/web-flasher"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XiaozhiWebFlasherScreen(
    currentUsername: String? = null,
    accessToken: String? = null,
    onClaimPresetCode: ((String, (Boolean, String?) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    // Amankan pengambilan Activity dari Context
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) break
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }

    // Jaga layar HP tetap menyala selama membuka layar flasher agar proses transfer serial tidak terputus
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var pageTitle by remember { mutableStateOf("Web Flasher ESP32") }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // File Chooser Handler untuk Upload File Firmware (.bin)
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fileUploadCallback?.onReceiveValue(arrayOf(uri))
        } else {
            fileUploadCallback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
    ) {
        //  TOP HEADER BAR (Neo-Brutalist Compact Navigation) 
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = NeoTokens.BorderWidth, color = NeoTokens.Black),
            color = NeoTokens.White
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (canGoBack) {
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = NeoTokens.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(NeoTokens.Yellow, RoundedCornerShape(6.dp))
                                    .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Memory,
                                    contentDescription = "Flasher",
                                    tint = NeoTokens.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "⚡ Web Flasher ESP32",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.5.sp,
                                color = NeoTokens.Black
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(NeoTokens.Emerald, RoundedCornerShape(3.dp))
                                )
                                Text(
                                    text = if (isLoading) "Memuat..." else "USB OTG Engine Aktif",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLoading) NeoTokens.Muted else NeoTokens.Emerald
                                )
                            }
                        }
                    }

                    // Action Buttons (Reload & Chrome OTG Intent)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Tombol Buka di Chrome USB OTG
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_FLASHER_URL)).apply {
                                        setPackage("com.android.chrome")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback ke browser default
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_FLASHER_URL)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeoTokens.Cream, RoundedCornerShape(6.dp))
                                .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                Icons.Rounded.OpenInBrowser,
                                contentDescription = "Buka di Chrome OTG",
                                tint = NeoTokens.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Tombol Refresh / Muat Ulang
                        IconButton(
                            onClick = {
                                loadError = null
                                webViewInstance?.reload()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(NeoTokens.Yellow, RoundedCornerShape(6.dp))
                                .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Muat Ulang",
                                tint = NeoTokens.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Progress Indicator Bar saat loading
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (loadingProgress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = NeoTokens.Emerald,
                        trackColor = NeoTokens.Cream
                    )
                }
            }
        }

        //  FULL-SCREEN WEBVIEW AREA 
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // Konfigurasi Cookie & Session Sync
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(this@apply, true)
                            if (!accessToken.isNullOrBlank()) {
                                setCookie(
                                    "https://xiaozhiscig.biz.id",
                                    "access_token=$accessToken; Path=/; Secure; SameSite=Lax"
                                )
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true

                                // Sinkronisasi localStorage token agar preset berlisensi otomatis terbuka
                                if (!accessToken.isNullOrBlank()) {
                                    val js = "try { localStorage.setItem('token', '$accessToken'); localStorage.setItem('username', '${currentUsername ?: ""}'); } catch(e){}"
                                    view?.evaluateJavascript(js, null)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = error?.description?.toString() ?: "Gagal memuat Web Flasher."
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                if (!title.isNullOrBlank()) pageTitle = title
                            }

                            // Tangani Upload File Firmware (.bin) langsung dari WebView
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileUploadCallback?.onReceiveValue(null)
                                fileUploadCallback = filePathCallback
                                try {
                                    filePickerLauncher.launch("*/*")
                                    return true
                                } catch (e: Exception) {
                                    fileUploadCallback?.onReceiveValue(null)
                                    fileUploadCallback = null
                                    return false
                                }
                            }

                            // Izinkan Request Perangkat WebUSB / WebSerial
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = settings.userAgentString + " MobileFlasherApp/1.4.5"
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        loadUrl(WEB_FLASHER_URL)
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Tampilan Error jika koneksi gagal
            if (loadError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NeoTokens.Cream)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "⚠️ Gagal Memuat Web Flasher",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = NeoTokens.Black
                            )
                            Text(
                                text = "Terjadi kendala saat menyambung ke server flasher:\n$loadError",
                                fontSize = 12.5.sp,
                                color = NeoTokens.Muted
                            )
                            Button(
                                onClick = {
                                    loadError = null
                                    webViewInstance?.loadUrl(WEB_FLASHER_URL)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Emerald),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Coba Lagi", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
