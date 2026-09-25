package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.provider.Settings
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.irsyadlabs.espbridge.data.xiaozhi.XiaozhiMcpStatus
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XiaozhiAuthScreen(
    operatingMode: String = "XIAOZHI_AI",
    isLoggedIn: Boolean = false,
    currentUsername: String? = null,
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegister: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onGoogleAuth: (isRegister: Boolean, onComplete: (Boolean, String?) -> Unit) -> Unit,
    onGoogleWebAuth: ((isRegister: Boolean) -> Unit)? = null,
    onSaveAndConnectMcp: (String, (XiaozhiMcpStatus) -> Unit, (Boolean) -> Unit) -> Unit,
    onAuthSuccessAndConnected: () -> Unit,
    onSwitchToChronchi: () -> Unit,
    onSwitchToXiaozhi: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isChronchiMode = operatingMode.equals("CHRONCHI_BLE", ignoreCase = true) || operatingMode.equals("CHRONCHI", ignoreCase = true)
    
    // Auth Tabs: "login", "register", "wifi_portal" (Offline Wi-Fi setup samping register)
    var currentTab by remember { mutableStateOf("login") }
    val isLoginTab = currentTab == "login"
    val isRegisterTab = currentTab == "register"
    val isWifiTab = currentTab == "wifi_portal"

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var busyActionText by remember { mutableStateOf("Memproses...") }

    // In-App WebView state for 192.168.4.1
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebLoading by remember { mutableStateOf(false) }
    var webErrorMsg by remember { mutableStateOf<String?>(null) }

    // MCP Gating & Direct Input state
    var showMcpInputSection by remember { mutableStateOf(false) }
    var mcpTokenInput by remember { mutableStateOf("") }
    var isConnectingMcp by remember { mutableStateOf(false) }
    var mcpStatusText by remember { mutableStateOf("") }
    var mcpConnectedSuccess by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        // Top Brand Banner
        NeoBadge(
            text = if (isChronchiMode) "CHRONCHI BLE & XIAOZHI AI" else "XICHI AI INDONESIA",
            backgroundColor = if (isChronchiMode) NeoTokens.Blue else NeoTokens.Emerald,
            textColor = NeoTokens.White
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = if (isLoggedIn) {
                "HUBUNGKAN MCP"
            } else if (isWifiTab) {
                "PORTAL WI-FI ESP32"
            } else if (isChronchiMode) {
                if (isLoginTab) "MASUK DENGAN AKUN XIAOZHI" else "DAFTAR AKUN XIAOZHI"
            } else {
                if (isLoginTab) "MASUK KE XIAOZHI AI" else "BUAT AKUN BARU"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = NeoTokens.Black,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (isLoggedIn) {
                "Halo, ${currentUsername ?: "Pengguna"}! Hubungkan WebSocket MCP xiaozhi.me untuk melanjutkan."
            } else if (isWifiTab) {
                "Konfigurasi Wi-Fi offline langsung ke perangkat ESP32 via hotspot 192.168.4.1"
            } else if (isChronchiMode) {
                "Satu akun terpadu untuk mengakses fitur Chronchi BLE dan Ekosistem Xiaozhi AI"
            } else {
                "Terhubung langsung dengan server website xiaozhiscig.biz.id"
            },
            fontSize = 13.5.sp,
            color = NeoTokens.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
        )

        // Loading Animation Card (when registering / logging in)
        AnimatedVisibility(
            visible = isBusy,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                backgroundColor = NeoTokens.MintLight,
                borderColor = NeoTokens.Black,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.85f,
                            targetValue = 1.35f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(900),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                                .background(NeoTokens.Emerald.copy(alpha = 0.25f), CircleShape)
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = NeoTokens.Black
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = busyActionText,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Mohon tunggu, menghubungkan ke server...",
                            fontSize = 12.sp,
                            color = NeoTokens.Dark
                        )
                    }
                }
            }
        }

        // ==========================================
        // KASUS 1: USER SUDAH LOGIN NAMUN BUTUH MCP
        // ==========================================
        if (isLoggedIn && !isChronchiMode) {
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NeoTokens.White,
                borderColor = NeoTokens.Black,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoBadge(
                            text = if (mcpConnectedSuccess) "MCP TERHUBUNG" else "WAJIB MCP UNTUK XIAOZHI",
                            backgroundColor = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Yellow,
                            textColor = if (mcpConnectedSuccess) NeoTokens.White else NeoTokens.Black
                        )
                        NeoPulseIndicator(active = isConnectingMcp || mcpConnectedSuccess)
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = "Hubungkan Endpoint MCP Xichi",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoTokens.Black
                    )

                    Text(
                        text = "Untuk mengakses Xiaozhi AI Dashboard & Chatbot, Anda wajib memiliki MCP endpoint aktif dari xiaozhi.me.",
                        fontSize = 13.sp,
                        color = NeoTokens.Dark,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    Button(
                        onClick = { uriHandler.openUri("https://xiaozhi.me") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeoTokens.White,
                            contentColor = NeoTokens.Black
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = NeoTokens.Emerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Buka xiaozhi.me ↗",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeoTokens.Black
                            )
                        }
                    }

                    NeoTextField(
                        value = mcpTokenInput,
                        onValueChange = { 
                            mcpTokenInput = it
                            mcpStatusText = ""
                        },
                        label = "WebSocket MCP Endpoint",
                        placeholder = "wss://api.xiaozhi.me/mcp/?token=...",
                        leadingIcon = Icons.Rounded.Key,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                    if (clip.isNotBlank()) mcpTokenInput = clip
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Tempel dari Clipboard",
                                    tint = NeoTokens.Black
                                )
                            }
                        }
                    )

                    if (mcpStatusText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isConnectingMcp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.5.dp,
                                    color = NeoTokens.Emerald
                                )
                            } else {
                                Icon(
                                    imageVector = if (mcpConnectedSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Coral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = mcpStatusText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Coral
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    NeoButton(
                        text = if (isConnectingMcp) "MENGHUBUNGKAN MCP..." else if (mcpConnectedSuccess) "MASUK KE DASHBOARD" else "⚡ HUBUNGKAN SEKARANG",
                        loading = isConnectingMcp,
                        enabled = !isConnectingMcp && (mcpTokenInput.isNotBlank() || mcpConnectedSuccess),
                        onClick = {
                            if (mcpConnectedSuccess) {
                                onAuthSuccessAndConnected()
                                return@NeoButton
                            }
                            val cleanEndpoint = mcpTokenInput.trim()
                            if (!cleanEndpoint.startsWith("wss://") && !cleanEndpoint.startsWith("ws://")) {
                                mcpStatusText = "Endpoint MCP tidak valid. Wajib diawali wss:// atau ws://"
                                return@NeoButton
                            }
                            isConnectingMcp = true
                            mcpStatusText = "Menyimpan endpoint & menghubungkan WebSocket..."
                            onSaveAndConnectMcp(cleanEndpoint, { status ->
                                mcpStatusText = status.statusText
                            }) { connected ->
                                isConnectingMcp = false
                                if (connected) {
                                    mcpConnectedSuccess = true
                                    mcpStatusText = "🎉 Berhasil Terhubung ke XiaoZhi! Mengalihkan..."
                                    onAuthSuccessAndConnected()
                                } else {
                                    mcpStatusText = "Koneksi belum berhasil. Periksa kembali endpoint WebSocket Anda."
                                }
                            }
                        },
                        color = NeoTokens.Emerald,
                        textColor = NeoTokens.White
                    )

                    Spacer(Modifier.height(14.dp))

                    // Beralih ke Mode Chronchi (Tanpa Perlu MCP)
                    OutlinedButton(
                        onClick = onSwitchToChronchi,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(NeoTokens.ButtonCorner),
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeoTokens.Blue),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeoTokens.Blue)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "⚡ Beralih ke Chronchi BLE (Tanpa MCP)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }

                    if (onLogout != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🚪 Logout / Ganti Akun",
                                color = NeoTokens.Coral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // KASUS 2: USER BELUM LOGIN (SEGMENTED SWITCHER 3 TAB: MASUK / DAFTAR / WI-FI)
            // =========================================================================

            // Segmented Switcher 3 Tab
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(NeoTokens.White, RoundedCornerShape(NeoTokens.ButtonCorner))
                    .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(NeoTokens.ButtonCorner))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Tab 1: MASUK
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isLoginTab) (if (isChronchiMode) NeoTokens.Blue else NeoTokens.Yellow) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isBusy) {
                                currentTab = "login"
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MASUK",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = if (isLoginTab && isChronchiMode) NeoTokens.White else NeoTokens.Black
                        )
                    }

                    // Tab 2: DAFTAR
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (isRegisterTab) (if (isChronchiMode) NeoTokens.Blue else NeoTokens.Yellow) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isBusy) {
                                currentTab = "register"
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DAFTAR",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = if (isRegisterTab && isChronchiMode) NeoTokens.White else NeoTokens.Black
                        )
                    }

                    // Tab 3: WI-FI ESP32 (Offline Web Portal 192.168.4.1)
                    Box(
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                            .background(
                                if (isWifiTab) NeoTokens.Emerald else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = !isBusy) {
                                currentTab = "wifi_portal"
                                errorMessage = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = if (isWifiTab) NeoTokens.White else NeoTokens.Emerald,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "WI-FI ESP32",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = if (isWifiTab) NeoTokens.White else NeoTokens.Black
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            if (isWifiTab) {
                // =========================================================================
                // SUB-KASUS A: OFFLINE WI-FI WEB PORTAL (MODERN & TOUCH-OPTIMIZED)
                // =========================================================================
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoTokens.White,
                    borderColor = NeoTokens.Black,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
                        // Header Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NeoBadge(
                                text = "100% OFFLINE (PORTAL AP)",
                                backgroundColor = NeoTokens.Emerald,
                                textColor = NeoTokens.White
                            )
                            NeoBadge(
                                text = "192.168.4.1",
                                backgroundColor = NeoTokens.MintLight,
                                textColor = Color(0xFF065F46)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Konfigurasi Wi-Fi & ESP32",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeoTokens.Black
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "Atur koneksi Wi-Fi perangkat keras ESP32 secara instan langsung dari ponsel Anda tanpa koneksi internet.",
                            fontSize = 12.5.sp,
                            color = NeoTokens.Muted,
                            lineHeight = 17.sp
                        )

                        Spacer(Modifier.height(12.dp))

                        // Step-by-Step Guidance Box
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeoTokens.Cream, RoundedCornerShape(10.dp))
                                .border(1.5.dp, NeoTokens.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Panduan Langkah Cepat:",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.5.sp,
                                color = NeoTokens.Black
                            )
                            Row(verticalAlignment = Alignment.Top) {
                                Text("1️⃣", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Nyalakan ESP32 hingga mode hotspot aktif (misal 'Xiaozhi-XXXX' atau 'ESP32-AP').",
                                    fontSize = 12.sp,
                                    color = NeoTokens.Dark,
                                    lineHeight = 16.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("2️⃣", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Sambungkan Wi-Fi ponsel Anda ke hotspot ESP32 tersebut.",
                                    fontSize = 12.sp,
                                    color = NeoTokens.Dark,
                                    lineHeight = 16.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("3️⃣", fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Jika ada notifikasi 'Wi-Fi tanpa internet', pilih 'Tetap Terhubung' dan matikan Data Seluler sementara.",
                                    fontSize = 12.sp,
                                    color = NeoTokens.Dark,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                    } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Yellow),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                modifier = Modifier.weight(1f).height(42.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Wifi, null, tint = NeoTokens.Black, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Setelan Wi-Fi HP", color = NeoTokens.Black, fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    webErrorMsg = null
                                    isWebLoading = true
                                    webViewRef?.loadUrl("http://192.168.4.1/")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Emerald),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                modifier = Modifier.weight(1f).height(42.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Refresh, null, tint = NeoTokens.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Muat Ulang", color = NeoTokens.White, fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://192.168.4.1/")).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.White),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                modifier = Modifier.weight(1f).height(42.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.OpenInNew, null, tint = NeoTokens.Black, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Di Browser", color = NeoTokens.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Modern Browser Mockup Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(620.dp)
                                .background(NeoTokens.White, RoundedCornerShape(14.dp))
                                .border(2.dp, NeoTokens.Black, RoundedCornerShape(14.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Address Bar Header
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .background(Color(0xFFF1F5F9))
                                        .border(
                                            androidx.compose.foundation.BorderStroke(1.dp, NeoTokens.Black.copy(alpha = 0.2f))
                                        )
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // macOS Window Dots
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(Modifier.size(10.dp).background(NeoTokens.Coral, CircleShape))
                                            Box(Modifier.size(10.dp).background(NeoTokens.Yellow, CircleShape))
                                            Box(Modifier.size(10.dp).background(NeoTokens.Emerald, CircleShape))
                                        }

                                        // URL Pill
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 10.dp)
                                                .height(28.dp)
                                                .background(NeoTokens.White, RoundedCornerShape(6.dp))
                                                .border(1.dp, NeoTokens.Black.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Lock, null, tint = NeoTokens.Emerald, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    text = "http://192.168.4.1/",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NeoTokens.Black
                                                )
                                            }
                                        }

                                        // Reload Icon
                                        IconButton(
                                            onClick = {
                                                webErrorMsg = null
                                                isWebLoading = true
                                                webViewRef?.loadUrl("http://192.168.4.1/")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Rounded.Refresh, null, tint = NeoTokens.Black, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                // WebView Container
                                Box(modifier = Modifier.fillMaxSize()) {
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

                                                // Enable smooth touch inside nested scroll
                                                setOnTouchListener { v, event ->
                                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                                    false
                                                }

                                                webViewClient = object : WebViewClient() {
                                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                        super.onPageStarted(view, url, favicon)
                                                        isWebLoading = true
                                                        webErrorMsg = null
                                                    }

                                                    override fun onPageFinished(view: WebView?, url: String?) {
                                                        super.onPageFinished(view, url)
                                                        isWebLoading = false
                                                    }

                                                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                        super.onReceivedError(view, request, error)
                                                        if (request?.isForMainFrame == true) {
                                                            isWebLoading = false
                                                            webErrorMsg = "Belum dapat terhubung ke 192.168.4.1"
                                                        }
                                                    }
                                                }
                                                webViewRef = this
                                                loadUrl("http://192.168.4.1/")
                                            }
                                        },
                                        update = { webViewRef = it },
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Loading Indicator Overlay
                                    if (isWebLoading) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(NeoTokens.White.copy(alpha = 0.88f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                CircularProgressIndicator(color = NeoTokens.Emerald, strokeWidth = 3.dp)
                                                Spacer(Modifier.height(10.dp))
                                                Text(
                                                    text = "Memuat portal web 192.168.4.1...",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = NeoTokens.Black
                                                )
                                            }
                                        }
                                    }

                                    // Error Message Overlay
                                    if (webErrorMsg != null) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(NeoTokens.White).padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                                            ) {
                                                Icon(Icons.Rounded.WifiOff, null, tint = NeoTokens.Coral, modifier = Modifier.size(48.dp))
                                                Spacer(Modifier.height(8.dp))
                                                Text(
                                                    text = "Portal ESP32 Belum Terdeteksi",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = NeoTokens.Black,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(NeoTokens.Cream, RoundedCornerShape(10.dp))
                                                        .border(1.dp, NeoTokens.Black.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "Langkah Perbaikan:",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.sp,
                                                        color = NeoTokens.Black
                                                    )
                                                    Text(
                                                        text = "1. Pastikan ponsel sudah terhubung ke hotspot Wi-Fi ESP32 ('Xiaozhi-XXXX').",
                                                        fontSize = 11.5.sp,
                                                        color = NeoTokens.Dark
                                                    )
                                                    Text(
                                                        text = "2. Jika muncul peringatan 'Wi-Fi tanpa internet', wajib pilih 'Tetap Terhubung' (Stay Connected).",
                                                        fontSize = 11.5.sp,
                                                        color = NeoTokens.Dark
                                                    )
                                                    Text(
                                                        text = "3. Matikan sementara Data Seluler (4G/5G) agar koneksi tidak dialihkan ke jaringan seluler.",
                                                        fontSize = 11.5.sp,
                                                        color = NeoTokens.Dark
                                                    )
                                                }
                                                Spacer(Modifier.height(14.dp))
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Yellow),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Setelan Wi-Fi", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeoTokens.Black)
                                                    }
                                                    Button(
                                                        onClick = {
                                                            webErrorMsg = null
                                                            isWebLoading = true
                                                            webViewRef?.loadUrl("http://192.168.4.1/")
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Emerald),
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Muat Ulang", fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeoTokens.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // =========================================================================
                // SUB-KASUS B: FORM LOGIN / DAFTAR AKUN
                // =========================================================================

                // Google Auth Button Card
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoTokens.White,
                    borderColor = NeoTokens.Black,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = {
                                isBusy = true
                                busyActionText = if (isLoginTab) "MENGHUBUNGKAN GOOGLE..." else "MENDAFTARKAN DENGAN GOOGLE..."
                                errorMessage = null
                                onGoogleAuth(!isLoginTab) { success, msg ->
                                    isBusy = false
                                    if (success) {
                                        if (isChronchiMode) {
                                            onAuthSuccessAndConnected()
                                        } else if (msg == "MCP_REQUIRED") {
                                            showMcpInputSection = true
                                            mcpStatusText = "Akun Google terhubung! Silakan masukkan endpoint MCP."
                                        } else {
                                            onAuthSuccessAndConnected()
                                        }
                                    } else {
                                        errorMessage = msg ?: "Autentikasi Google dibatalkan atau gagal."
                                    }
                                }
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeoTokens.White,
                                contentColor = NeoTokens.Black
                            ),
                            shape = RoundedCornerShape(NeoTokens.ButtonCorner),
                            border = androidx.compose.foundation.BorderStroke(2.dp, NeoTokens.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeoTokens.Blue
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (isLoginTab) "MASUK DENGAN GOOGLE" else "DAFTAR DENGAN GOOGLE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = NeoTokens.Black
                                )
                            }
                        }

                        if (onGoogleWebAuth != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = { onGoogleWebAuth(!isLoginTab) },
                                enabled = !isBusy
                            ) {
                                Text(
                                    text = "Atau buka Google Sign-In Alternatif (In-App) ↗",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoTokens.Blue
                                )
                            }
                        }

                        if (!isLoginTab) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "ℹ️ Pendaftaran melalui Google akan menautkan akun Google Anda secara otomatis.",
                                fontSize = 12.sp,
                                color = NeoTokens.Muted,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Divider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(NeoTokens.Gray))
                    Text(
                        text = if (isLoginTab) " ATAU USERNAME & PASSWORD " else " ATAU DAFTAR MANUAL ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoTokens.Muted
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(NeoTokens.Gray))
                }

                Spacer(Modifier.height(16.dp))

                // Main Auth Form
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoTokens.White,
                    borderColor = NeoTokens.Black,
                    contentPadding = PaddingValues(20.dp)
                ) {
                    Column {
                        NeoTextField(
                            value = username,
                            onValueChange = { 
                                username = it.trim()
                                errorMessage = null 
                            },
                            label = "Username",
                            placeholder = "Minimal 3 karakter alfanumerik",
                            leadingIcon = Icons.Rounded.Person
                        )

                        Spacer(Modifier.height(14.dp))

                        NeoTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                errorMessage = null 
                            },
                            label = "Password",
                            placeholder = "Minimal 6 karakter",
                            leadingIcon = Icons.Rounded.Lock,
                            isPassword = true
                        )

                        if (!isLoginTab) {
                            Spacer(Modifier.height(14.dp))
                            NeoTextField(
                                value = confirmPassword,
                                onValueChange = { 
                                confirmPassword = it
                                errorMessage = null 
                            },
                            label = "Ulangi Password",
                            placeholder = "Konfirmasi password sama",
                            leadingIcon = Icons.Rounded.Lock,
                            isPassword = true
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeoTokens.Coral.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .border(1.5.dp, NeoTokens.Coral, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = NeoTokens.Coral,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    NeoButton(
                        text = if (isLoginTab) "MASUK SEKARANG" else "DAFTAR SEKARANG",
                        loading = isBusy,
                        enabled = !isBusy && username.isNotBlank() && password.isNotBlank() &&
                            (isLoginTab || password == confirmPassword),
                        onClick = {
                            val cleanUser = username.trim()
                            val cleanPass = password.trim()

                            // Validasi keamanan input
                            if (cleanUser.length < 3) {
                                errorMessage = "Username minimal harus 3 karakter."
                                return@NeoButton
                            }
                            val usernameRegex = Regex("^[a-zA-Z0-9._-]+$")
                            if (!usernameRegex.matches(cleanUser)) {
                                errorMessage = "Username hanya boleh huruf, angka, titik, strip, atau underscore."
                                return@NeoButton
                            }
                            if (cleanPass.length < 6) {
                                errorMessage = "Password minimal harus 6 karakter demi keamanan."
                                return@NeoButton
                            }
                            if (!isLoginTab && cleanPass != confirmPassword.trim()) {
                                errorMessage = "Konfirmasi password tidak cocok."
                                return@NeoButton
                            }

                            isBusy = true
                            busyActionText = if (isLoginTab) "MEMPROSES MASUK..." else "MEMBUAT AKUN BARU..."
                            errorMessage = null

                            if (isLoginTab) {
                                onLogin(cleanUser, cleanPass) { success, msg ->
                                    isBusy = false
                                    if (success) {
                                        if (isChronchiMode) {
                                            onAuthSuccessAndConnected()
                                        } else if (msg == "MCP_REQUIRED") {
                                            showMcpInputSection = true
                                            mcpStatusText = "Sesi masuk, silakan hubungkan MCP WebSocket."
                                        } else {
                                            onAuthSuccessAndConnected()
                                        }
                                    } else {
                                        errorMessage = msg ?: "Login gagal. Periksa username dan password Anda."
                                    }
                                }
                            } else {
                                onRegister(cleanUser, cleanPass) { success, msg ->
                                    isBusy = false
                                    if (success) {
                                        if (isChronchiMode) {
                                            onAuthSuccessAndConnected()
                                        } else if (msg == "MCP_REQUIRED") {
                                            showMcpInputSection = true
                                            mcpStatusText = "Akun berhasil dibuat! Silakan hubungkan MCP endpoint."
                                        } else {
                                            onAuthSuccessAndConnected()
                                        }
                                    } else {
                                        errorMessage = msg ?: "Registrasi gagal. Coba username lain."
                                    }
                                }
                            }
                        },
                        color = if (isLoginTab) (if (isChronchiMode) NeoTokens.Blue else NeoTokens.Emerald) else NeoTokens.Coral,
                        textColor = NeoTokens.White
                    )

                    // Shortcut to offline WiFi setup inside form
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { currentTab = "wifi_portal" },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Wifi, null, tint = NeoTokens.Emerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Punya ESP32 baru? Setup Wi-Fi Offline di sini ↗",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Emerald
                            )
                        }
                    }

                    // Quick toggle to show MCP input if user wants to setup early (only in Xiaozhi mode)
                    if (!isChronchiMode && !showMcpInputSection) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(
                            onClick = { showMcpInputSection = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Link, null, tint = NeoTokens.Muted, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Punya endpoint MCP dari xiaozhi.me? Hubungkan di sini",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoTokens.Muted
                                )
                            }
                        }
                    }
                }
            }

            // Animated MCP Connection Section
            if (!isChronchiMode) {
                AnimatedVisibility(
                    visible = showMcpInputSection,
                    enter = expandVertically() + fadeIn()
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                        NeoCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = NeoTokens.MintLight,
                            borderColor = NeoTokens.Black,
                            contentPadding = PaddingValues(20.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NeoBadge(
                                        text = if (mcpConnectedSuccess) "MCP TERHUBUNG" else "KONEKSI MCP DIBUTUHKAN",
                                        backgroundColor = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Yellow,
                                        textColor = if (mcpConnectedSuccess) NeoTokens.White else NeoTokens.Black
                                    )
                                    NeoPulseIndicator(active = isConnectingMcp || mcpConnectedSuccess)
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    text = "Hubungkan Endpoint MCP Xichi",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = NeoTokens.Black
                                )

                                Text(
                                    text = "Silakan ambil MCP endpoint di xiaozhi.me lalu tempelkan di bawah ini:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoTokens.Dark,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                                )

                                Button(
                                    onClick = { uriHandler.openUri("https://xiaozhi.me") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeoTokens.White,
                                        contentColor = NeoTokens.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Black),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.padding(bottom = 14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.OpenInNew,
                                            contentDescription = null,
                                            tint = NeoTokens.Emerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Buka xiaozhi.me ↗",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = NeoTokens.Black
                                        )
                                    }
                                }

                                NeoTextField(
                                    value = mcpTokenInput,
                                    onValueChange = { 
                                        mcpTokenInput = it
                                        mcpStatusText = ""
                                    },
                                    label = "WebSocket MCP Endpoint",
                                    placeholder = "wss://api.xiaozhi.me/mcp/?token=...",
                                    leadingIcon = Icons.Rounded.Key,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                val clip = clipboardManager.getText()?.text?.trim().orEmpty()
                                                if (clip.isNotBlank()) mcpTokenInput = clip
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ContentPaste,
                                                contentDescription = "Tempel dari Clipboard",
                                                tint = NeoTokens.Black
                                            )
                                        }
                                    }
                                )

                                if (mcpStatusText.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isConnectingMcp) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.5.dp,
                                                color = NeoTokens.Emerald
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (mcpConnectedSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                                contentDescription = null,
                                                tint = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Coral,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = mcpStatusText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Coral
                                        )
                                    }
                                }

                                Spacer(Modifier.height(20.dp))

                                NeoButton(
                                    text = if (isConnectingMcp) "MENGHUBUNGKAN MCP..." else if (mcpConnectedSuccess) "MASUK KE DASHBOARD" else "⚡ HUBUNGKAN SEKARANG",
                                    loading = isConnectingMcp,
                                    enabled = !isConnectingMcp && (mcpTokenInput.isNotBlank() || mcpConnectedSuccess),
                                    onClick = {
                                        if (mcpConnectedSuccess) {
                                            onAuthSuccessAndConnected()
                                            return@NeoButton
                                        }
                                        val cleanEndpoint = mcpTokenInput.trim()
                                        if (!cleanEndpoint.startsWith("wss://") && !cleanEndpoint.startsWith("ws://")) {
                                            mcpStatusText = "Endpoint MCP tidak valid. Wajib diawali wss:// atau ws://"
                                            return@NeoButton
                                        }
                                        isConnectingMcp = true
                                        mcpStatusText = "Menyimpan endpoint & menghubungkan WebSocket..."
                                        onSaveAndConnectMcp(cleanEndpoint, { status ->
                                            mcpStatusText = status.statusText
                                        }) { connected ->
                                            isConnectingMcp = false
                                            if (connected) {
                                                mcpConnectedSuccess = true
                                                mcpStatusText = "🎉 Berhasil Terhubung ke XiaoZhi! Mengalihkan..."
                                                onAuthSuccessAndConnected()
                                            } else {
                                                mcpStatusText = "Koneksi belum berhasil. Periksa kembali endpoint WebSocket Anda."
                                            }
                                        }
                                    },
                                    color = NeoTokens.Emerald,
                                    textColor = NeoTokens.White
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(24.dp))

        // Switch Mode Footer Link
        if (isChronchiMode) {
            if (onSwitchToXiaozhi != null) {
                TextButton(onClick = onSwitchToXiaozhi) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SmartToy, null, tint = NeoTokens.Emerald, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Beralih ke Mode Xiaozhi AI (Web & Cloud)",
                            color = NeoTokens.Emerald,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            TextButton(onClick = onSwitchToChronchi) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bluetooth, null, tint = NeoTokens.Blue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Beralih ke Mode Chronchi BLE (ESP32)",
                        color = NeoTokens.Blue,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // App Version Footer
        Text(
            text = "Versi v1.4.0 (Build 7) • Chronchi & Xiaozhi AI",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = NeoTokens.Muted,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))
    }
}
