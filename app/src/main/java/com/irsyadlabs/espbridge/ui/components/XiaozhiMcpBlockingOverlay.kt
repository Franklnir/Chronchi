package com.irsyadlabs.espbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

/**
 * Neo-Brutalist Overlay pemblokir akses Dashboard, Profile, dan Chat
 * ketika akun pengguna belum mengkoneksikan WebSocket MCP Xiaozhi.
 * Memungkinkan pengguna untuk:
 * 1. Menghubungkan MCP langsung dengan memasukkan token wss://
 * 2. Mengakses menu Web Flasher (bebas tanpa butuh MCP)
 * 3. Beralih ke mode Chronchi BLE
 * 4. Logout / Ganti Akun
 */
@Composable
fun XiaozhiMcpBlockingOverlay(
    modifier: Modifier = Modifier,
    onSaveAndConnectMcp: (String) -> Unit,
    onNavigateToFlasher: () -> Unit,
    onSwitchToChronchi: () -> Unit,
    onLogout: () -> Unit
) {
    var mcpTokenInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .border(width = NeoTokens.BorderWidth, color = NeoTokens.Black, shape = RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(NeoTokens.Coral, shape = RoundedCornerShape(10.dp))
                        .border(width = 2.dp, color = NeoTokens.Black, shape = RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "MCP Locked",
                        tint = NeoTokens.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Koneksi MCP Diperlukan",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeoTokens.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Fitur ini membutuhkan koneksi WebSocket MCP aktif ke Server Xiaozhi AI. Masukkan token MCP Anda di bawah atau pilih opsi alternatif:",
                    fontSize = 12.5.sp,
                    color = NeoTokens.Muted,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Input Box Token MCP
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoTokens.Cream, shape = RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoTokens.Black, shape = RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "🔑 Masukkan Token WebSocket MCP:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoTokens.Black
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = mcpTokenInput,
                        onValueChange = { mcpTokenInput = it },
                        placeholder = {
                            Text("wss://xiaozhiscig.biz.id/mcp/...", fontSize = 12.sp, color = Color.Gray)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeoTokens.White,
                            unfocusedContainerColor = NeoTokens.White,
                            focusedBorderColor = NeoTokens.Black,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val token = mcpTokenInput.trim()
                            if (token.isNotBlank()) {
                                isConnecting = true
                                onSaveAndConnectMcp(token)
                            }
                        },
                        enabled = mcpTokenInput.isNotBlank() && !isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeoTokens.Yellow,
                            contentColor = NeoTokens.Black,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = NeoTokens.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menghubungkan...", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                        } else {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hubungkan MCP Sekarang", fontWeight = FontWeight.Black, fontSize = 12.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider Or
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NeoTokens.Black.copy(alpha = 0.2f))
                    Text(
                        text = " ATAU PILIH OPSI LAIN ",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NeoTokens.Muted,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = NeoTokens.Black.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Menu Flasher (Bisa diakses tanpa MCP)
                Button(
                    onClick = onNavigateToFlasher,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeoTokens.Emerald,
                        contentColor = NeoTokens.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Menu Web Flasher (Tanpa MCP)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Beralih ke Mode Chronchi BLE
                OutlinedButton(
                    onClick = onSwitchToChronchi,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeoTokens.Cyan,
                        contentColor = NeoTokens.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.Watch, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Beralih ke Mode Chronchi BLE", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Logout / Ganti Akun
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeoTokens.White,
                        contentColor = NeoTokens.Coral
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout / Ganti Akun", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
