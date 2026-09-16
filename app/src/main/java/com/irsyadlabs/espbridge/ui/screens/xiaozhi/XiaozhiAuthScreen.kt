package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // MCP Gating state
    var showMcpInputSection by remember { mutableStateOf(false) }
    var mcpTokenInput by remember { mutableStateOf("") }
    var isConnectingMcp by remember { mutableStateOf(false) }
    var mcpStatusText by remember { mutableStateOf("") }
    var mcpConnectedSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Top Brand Banner
        NeoBadge(text = "XIAOZHI AI INDONESIA", backgroundColor = NeoTokens.Emerald, textColor = NeoTokens.White)

        Spacer(Modifier.height(14.dp))

        Text(
            text = if (isLoginTab) "MASUK AKUN" else "BUAT AKUN BARU",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = NeoTokens.Black
        )

        Text(
            text = "Autentikasi terhubung langsung dengan server website xiaozhiscig.biz.id",
            fontSize = 13.sp,
            color = NeoTokens.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
        )

        // Segmented Switcher (Masuk / Daftar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
                        .clickable {
                            isLoginTab = true
                            errorMessage = null
                            showMcpInputSection = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("MASUK", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoTokens.Black)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (!isLoginTab) NeoTokens.Yellow else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            isLoginTab = false
                            errorMessage = null
                            showMcpInputSection = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("DAFTAR", fontWeight = FontWeight.Black, fontSize = 14.sp, color = NeoTokens.Black)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Form Card
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column {
                NeoTextField(
                    value = username,
                    onValueChange = {
                        username = it.lowercase().trim()
                        errorMessage = null
                    },
                    label = "Username",
                    placeholder = "misal: irsyadmiler",
                    leadingIcon = Icons.Rounded.Person
                )

                Spacer(Modifier.height(16.dp))

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
                    Spacer(Modifier.height(16.dp))
                    NeoTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = null
                        },
                        label = "Konfirmasi Password",
                        placeholder = "Ulangi password",
                        leadingIcon = Icons.Rounded.Lock,
                        isPassword = true
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = NeoTokens.Coral,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                if (!showMcpInputSection) {
                    NeoButton(
                        text = if (isLoginTab) "MASUK SEKARANG" else "DAFTAR SEKARANG",
                        loading = isBusy,
                        enabled = !isBusy && username.length >= 3 && password.length >= 6,
                        onClick = {
                            if (!isLoginTab && password != confirmPassword) {
                                errorMessage = "Password konfirmasi tidak cocok."
                                return@NeoButton
                            }
                            isBusy = true
                            errorMessage = null

                            if (isLoginTab) {
                                onLogin(username, password) { success, msg ->
                                    isBusy = false
                                    if (success) {
                                        // Checked by caller: if MCP already connected, onAuthSuccessAndConnected() is called.
                                        // If not connected, msg will instruct to show MCP section.
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
                                        // On register success: smoothly animate MCP input section below!
                                        showMcpInputSection = true
                                        mcpStatusText = "Akun berhasil dibuat! Silakan masukkan URL endpoint MCP."
                                    } else {
                                        errorMessage = msg ?: "Registrasi gagal. Coba username lain."
                                    }
                                }
                            }
                        },
                        color = if (isLoginTab) NeoTokens.Emerald else NeoTokens.Blue,
                        textColor = NeoTokens.White
                    )
                }
            }
        }

        // Animated MCP Connection Section (Gating)
        AnimatedVisibility(
            visible = showMcpInputSection,
            enter = expandVertically() + fadeIn()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
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

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Hubungkan Endpoint MCP Xiaozhi",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeoTokens.Black
                        )

                        Text(
                            text = "Aplikasi mewajibkan koneksi MCP aktif sebelum masuk ke Dashboard. Masukkan WebSocket endpoint Xiaozhi Anda di bawah ini:",
                            fontSize = 13.sp,
                            color = NeoTokens.Dark,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        NeoTextField(
                            value = mcpTokenInput,
                            onValueChange = { mcpTokenInput = it },
                            label = "WebSocket MCP Endpoint",
                            placeholder = "wss://api.xiaozhi.me/v1/mcp?token=...",
                            leadingIcon = Icons.Rounded.Key
                        )

                        if (mcpStatusText.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (mcpConnectedSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Dark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = mcpStatusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mcpConnectedSuccess) NeoTokens.Emerald else NeoTokens.Dark
                                )
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        NeoButton(
                            text = if (isConnectingMcp) "MENGHUBUNGKAN MCP..." else if (mcpConnectedSuccess) "MASUK KE DASHBOARD" else "HUBUNGKAN & MASUK",
                            loading = isConnectingMcp,
                            enabled = !isConnectingMcp && (mcpTokenInput.isNotBlank() || mcpConnectedSuccess),
                            onClick = {
                                if (mcpConnectedSuccess) {
                                    onAuthSuccessAndConnected()
                                    return@NeoButton
                                }
                                isConnectingMcp = true
                                mcpStatusText = "Menyimpan endpoint & mencoba bridge..."
                                onSaveAndConnectMcp(mcpTokenInput, { status ->
                                    mcpStatusText = status.statusText
                                }) { connected ->
                                    isConnectingMcp = false
                                    if (connected) {
                                        mcpConnectedSuccess = true
                                        mcpStatusText = "Koneksi MCP Berhasil Terhubung! Mengalihkan..."
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

        Spacer(Modifier.height(28.dp))

        // Switch Mode Link
        TextButton(onClick = onSwitchToChronchi) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.SwapHoriz, null, tint = NeoTokens.Muted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Beralih ke Mode Chronchi BLE",
                    color = NeoTokens.Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}
