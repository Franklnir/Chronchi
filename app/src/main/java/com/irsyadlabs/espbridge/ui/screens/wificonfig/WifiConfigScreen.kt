package com.irsyadlabs.espbridge.ui.screens.wificonfig

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.*

@Composable
fun WifiConfigScreen(
    state: MainUiState,
    onSendWifiConfig: (String, String) -> Unit,
    onSwitchMode: (String) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var configSent by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("xiaozhi") }

    val isConnected = state.bleState == ConnectionState.CONNECTED
    val deviceMode = state.connectedDevice?.mode

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            item {
                SectionHeader("WiFi Configuration", "Konfigurasi WiFi untuk ESP32 via BLE")
            }

            // Connection status
            item {
                PlayfulCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(48.dp).background(
                                if (isConnected) SuccessGreen.copy(alpha = 0.1f) else SketchPeach.copy(alpha = 0.1f),
                                CircleShape
                            ).border(
                                1.2.dp,
                                if (isConnected) SuccessGreen else SketchPeach,
                                CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isConnected) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled,
                                null,
                                tint = if (isConnected) SuccessGreen else SketchPeach,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(
                                if (isConnected) "ESP32 Terhubung" else "ESP32 Terputus",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isConnected) SuccessGreen else SketchPeach
                            )
                            if (isConnected && deviceMode != null) {
                                Text(
                                    "Mode: ${deviceMode.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SketchMuted
                                )
                            }
                            if (!isConnected) {
                                Text(
                                    "Hubungkan BLE dulu untuk konfigurasi WiFi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SketchMuted
                                )
                            }
                        }
                    }
                }
            }

            // WiFi SSID & Password input
            item {
                PlayfulCard {
                    Column {
                        Text("WiFi Credentials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = ssid,
                            onValueChange = { ssid = it },
                            label = { Text("SSID (Nama WiFi)") },
                            placeholder = { Text("Masukkan nama WiFi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = isConnected,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SketchTeal,
                                unfocusedBorderColor = SketchBorder.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            placeholder = { Text("Masukkan password WiFi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = isConnected,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SketchTeal,
                                unfocusedBorderColor = SketchBorder.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        PrimaryActionButton(
                            text = if (configSent) "✓ TERKIRIM" else "KIRIM KE ESP32",
                            onClick = {
                                if (ssid.isNotBlank()) {
                                    onSendWifiConfig(ssid, password)
                                    configSent = true
                                }
                            },
                            enabled = isConnected && ssid.isNotBlank() && !configSent
                        )

                        if (configSent) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "✓ WiFi config terkirim! ESP32 sudah menyimpan ke NVS.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }

            // Switch Mode
            item {
                PlayfulCard {
                    Column {
                        Text("Alihkan Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Text(
                            "Pilih mode tujuan, lalu tekan tombol untuk restart ESP32 ke mode tersebut.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SketchMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("xiaozhi" to "Xiaozhi", "chronchi" to "Chronchi", "xichi" to "Xichi").forEach { (mode, label) ->
                                FilterChip(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.ExtraBold) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(UiTokens.SmallRadius),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SketchTeal,
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = SketchMuted
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selectedMode == mode,
                                        borderColor = SketchBorder.copy(alpha = 0.3f),
                                        selectedBorderColor = SketchBorder,
                                        borderWidth = 1.2.dp
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        OutlinedButton(
                            onClick = { onSwitchMode(selectedMode) },
                            enabled = isConnected,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(UiTokens.SmallRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SketchTeal),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, SketchTeal.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("ALIHKAN KE ${selectedMode.uppercase()}", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(UiTokens.BottomBarHeight)) }
        }
    }
}
