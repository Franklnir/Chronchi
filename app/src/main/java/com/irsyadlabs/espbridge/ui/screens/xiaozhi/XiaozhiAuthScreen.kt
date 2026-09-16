package com.irsyadlabs.espbridge.ui.screens.xiaozhi

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.data.xiaozhi.XiaozhiMcpStatus
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun XiaozhiAuthScreen(
    onLogin: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onRegister: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onGoogleAuth: (isRegister: Boolean, onComplete: (Boolean, String?) -> Unit) -> Unit,
    onSaveAndConnectMcp: (String, (XiaozhiMcpStatus) -> Unit, (Boolean) -> Unit) -> Unit,
    onAuthSuccessAndConnected: () -> Unit,
    onSwitchToChronchi: () -> Unit
) {
    var isLoginTab by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }
    var busyActionText by remember { mutableStateOf("Memproses...") }

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
        Spacer(Modifier.height(36.dp))

        // Top Brand Banner
        NeoBadge(text = "XICHI AI INDONESIA", backgroundColor = NeoTokens.Emerald, textColor = NeoTokens.White)

        Spacer(Modifier.height(14.dp))

        Text(
            text = if (isLoginTab) "MASUK AKUN" else "BUAT AKUN BARU",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = NeoTokens.Black
        )

        Text(
            text = "Terhubung langsung dengan server website xiaozhiscig.biz.id",
            fontSize = 14.sp,
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

        // Segmented Switcher (Masuk / Daftar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(NeoTokens.White, RoundedCornerShape(NeoTokens.ButtonCorner))
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(NeoTokens.ButtonCorner))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isLoginTab) NeoTokens.Yellow else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isBusy) {
                            isLoginTab = true
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("MASUK", fontWeight = FontWeight.Black, fontSize = 15.sp, color = NeoTokens.Black)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!isLoginTab) NeoTokens.Yellow else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !isBusy) {
                            isLoginTab = false
                            errorMessage = null
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DAFTAR", fontWeight = FontWeight.Black, fontSize = 15.sp, color = NeoTokens.Black)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

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
                                if (msg == "MCP_REQUIRED") {
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

                if (!isLoginTab) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ℹ️ Pendaftaran melalui Google akan menautkan akun Google Anda secara permanen.",
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
                    onValueChange = { username = it; errorMessage = null },
                    label = "Username",
                    placeholder = "Masukkan username Anda",
                    leadingIcon = Icons.Rounded.Person
                )

                Spacer(Modifier.height(14.dp))

                // Password with eye icon visibility toggle built-in
                NeoTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    label = "Password",
                    placeholder = "Minimal 4 karakter",
                    leadingIcon = Icons.Rounded.Lock,
                    isPassword = true
                )

                if (!isLoginTab) {
                    Spacer(Modifier.height(14.dp))
                    NeoTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMessage = null },
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
                        if (!isLoginTab && password != confirmPassword) {
                            errorMessage = "Konfirmasi password tidak cocok."
                            return@NeoButton
                        }
                        isBusy = true
                        busyActionText = if (isLoginTab) "MEMPROSES MASUK..." else "MEMBUAT AKUN BARU..."
                        errorMessage = null
                        if (isLoginTab) {
                            onLogin(username, password) { success, msg ->
                                isBusy = false
                                if (success) {
                                    if (msg == "MCP_REQUIRED") {
                                        showMcpInputSection = true
                                        mcpStatusText = "Sesi masuk, silakan hubungkan MCP WebSocket."
                                    } else {
                                        onAuthSuccessAndConnected()
                                    }
                                } else {
                                    errorMessage = msg ?: "Login gagal. Periksa username dan password."
                                }
                            }
                        } else {
                            onRegister(username, password) { success, msg ->
                                isBusy = false
                                if (success) {
                                    if (msg == "MCP_REQUIRED") {
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
                    color = if (isLoginTab) NeoTokens.Emerald else NeoTokens.Blue,
                    textColor = NeoTokens.White
                )

                // Quick toggle to show MCP input if user wants to setup early
                if (!showMcpInputSection) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(
                        onClick = { showMcpInputSection = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Link, null, tint = NeoTokens.Muted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Punya endpoint MCP dari xiaozhi.me? Hubungkan di sini",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Muted
                            )
                        }
                    }
                }
            }
        }

        // Animated MCP Connection Section (Gating & Direct Input)
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

                        // Clear instruction with explicit mention of xiaozhi.me
                        Text(
                            text = "Silakan ambil MCP endpoint di xiaozhi.me lalu tempelkan di bawah ini:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Dark,
                            modifier = Modifier.padding(top = 6.dp, bottom = 10.dp)
                        )

                        // Direct button to open xiaozhi.me
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

                        // Input field with paste button
                        NeoTextField(
                            value = mcpTokenInput,
                            onValueChange = { mcpTokenInput = it },
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
                                        tint = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Dark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = mcpStatusText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Dark
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
                                isConnectingMcp = true
                                mcpStatusText = "Menyimpan endpoint & menghubungkan WebSocket..."
                                onSaveAndConnectMcp(mcpTokenInput, { status ->
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

        Spacer(Modifier.height(24.dp))

        // Switch Mode Link
        TextButton(onClick = onSwitchToChronchi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapHoriz, null, tint = NeoTokens.Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Beralih ke Mode Chronchi BLE",
                    color = NeoTokens.Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}
