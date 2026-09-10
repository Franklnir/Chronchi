package com.irsyadlabs.espbridge.ui.screens.wificonfig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.preview.previewUiState
import com.irsyadlabs.espbridge.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WifiConfigScreen(
    state: MainUiState,
    onSendWifiConfig: (String, String) -> Unit,
    onSwitchMode: (String) -> Unit,
    onScanWifi: () -> Unit,
    onSendFirebaseConfig: () -> Unit,
    onCheckFirebaseStatus: () -> Unit,
    onForgetFirebase: () -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var configSent by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("chronchi") }
    var isScanning by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val isConnected = state.bleState == ConnectionState.CONNECTED
    val device = state.connectedDevice
    val firebaseSaved = device?.firebaseSaved ?: false

    // Auto-stop scanning animation after some time if no result
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(10000)
            isScanning = false
        }
    }
    
    // Stop scanning when networks list changes
    LaunchedEffect(state.wifiNetworks) {
        isScanning = false
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Firebase Config?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Ini akan menghapus Secret API dan akses Cloud dari ESP32. Anda harus mengirim ulang konfigurasi agar Cloud Sync berfungsi kembali.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onForgetFirebase()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SketchRed)
                ) { Text("HAPUS SEKARANG", fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("BATAL", color = SketchMuted)
                }
            },
            shape = RoundedCornerShape(UiTokens.CardRadius),
            containerColor = Color.White
        )
    }

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            item {
                SectionHeader("WiFi Configuration", "Provisioning Chronchi device via BLE")
            }

            // Connection status & Current WiFi
            item {
                PlayfulCard {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(52.dp).background(
                                    if (isConnected) SketchTeal.copy(alpha = 0.1f) else SketchPeach.copy(alpha = 0.1f),
                                    CircleShape
                                ).border(
                                    1.2.dp,
                                    if (isConnected) SketchTeal else SketchPeach,
                                    CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isConnected) Icons.Rounded.BluetoothConnected else Icons.Rounded.BluetoothDisabled,
                                    null,
                                    tint = if (isConnected) SketchTeal else SketchPeach,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column(Modifier.padding(start = 16.dp).weight(1f)) {
                                Text(
                                    if (isConnected) "Device Connected" else "Device Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isConnected) SketchTeal else SketchPeach
                                )
                                if (isConnected && device != null) {
                                    Text(
                                        "Mode: ${device.mode.uppercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SketchMuted
                                    )
                                }
                            }
                        }
                        
                        if (isConnected && device?.wifiSsid != null) {
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = SketchBg,
                                shape = RoundedCornerShape(UiTokens.SmallRadius),
                                border = BorderStroke(1.dp, SketchBorder.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Wifi, null, tint = SketchTeal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Connected to: ", style = MaterialTheme.typography.bodyMedium, color = SketchMuted)
                                    Text(device.wifiSsid, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Firebase Config Status
                        if (isConnected) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                color = SketchBg,
                                shape = RoundedCornerShape(UiTokens.SmallRadius),
                                border = BorderStroke(1.dp, SketchBorder.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Firebase Control", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SketchTeal)
                                        Spacer(Modifier.weight(1f))
                                        StatusPill(
                                            if (firebaseSaved) "PROVISIONED" else "UNSET",
                                            if (firebaseSaved) StatusTone.GOOD else StatusTone.ERROR
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FirebaseMiniButton(
                                            onClick = onSendFirebaseConfig,
                                            icon = Icons.Rounded.CloudUpload,
                                            label = "SEND",
                                            color = if (firebaseSaved) SuccessGreen else SketchTeal,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FirebaseMiniButton(
                                            onClick = onCheckFirebaseStatus,
                                            icon = if (firebaseSaved) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                            label = "STATUS",
                                            color = if (firebaseSaved) SuccessGreen else SketchRed,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FirebaseMiniButton(
                                            onClick = { showClearConfirm = true },
                                            icon = Icons.Rounded.DeleteSweep,
                                            label = "CLEAR",
                                            color = SketchPeach,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WiFi Credentials Input
            item {
                PlayfulCard {
                    Column {
                        Text("WiFi Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = ssid,
                            onValueChange = { 
                                ssid = it
                                configSent = false 
                            },
                            label = { Text("SSID") },
                            placeholder = { Text("Select from list or enter name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = isConnected,
                            shape = RoundedCornerShape(UiTokens.SmallRadius),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SketchTeal,
                                unfocusedBorderColor = SketchBorder.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { 
                                password = it
                                configSent = false
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Enter password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = isConnected,
                            shape = RoundedCornerShape(UiTokens.SmallRadius),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = SketchTeal
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SketchTeal,
                                unfocusedBorderColor = SketchBorder.copy(alpha = 0.2f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        Spacer(Modifier.height(24.dp))

                        PrimaryActionButton(
                            text = if (configSent) "✓ PROVISIONED" else "CONNECT WIFI",
                            onClick = {
                                if (ssid.isNotBlank()) {
                                    onSendWifiConfig(ssid, password)
                                    configSent = true
                                }
                            },
                            enabled = isConnected && ssid.isNotBlank() && !configSent
                        )
                    }
                }
            }

            // WiFi Scan Section
            item {
                SectionHeader("Available Networks")
            }
            
            item {
                OutlinedButton(
                    onClick = {
                        isScanning = true
                        onScanWifi()
                    },
                    enabled = isConnected && !isScanning,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(UiTokens.SmallRadius),
                    border = BorderStroke(1.5.dp, SketchBorder)
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = SketchBorder)
                        Spacer(Modifier.width(12.dp))
                        Text("SCANNING...", fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                    } else {
                        Icon(Icons.Rounded.WifiFind, null, modifier = Modifier.size(20.dp), tint = SketchBorder)
                        Spacer(Modifier.width(10.dp))
                        Text("SCAN FOR NETWORKS", fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                    }
                }
            }
            
            if (state.wifiNetworks.isEmpty()) {
                item {
                    Text(
                        if (isScanning) "Searching for signals..." else "No networks discovered yet.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = SketchMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(state.wifiNetworks) { network ->
                    PlayfulCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(enabled = isConnected) {
                                ssid = network.ssid
                                configSent = false
                            },
                        innerPadding = PaddingValues(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(40.dp).background(SketchTeal.copy(alpha = 0.05f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (network.secure) Icons.Rounded.WifiLock else Icons.Rounded.Wifi,
                                    null,
                                    tint = SketchTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                                Text(network.ssid, fontWeight = FontWeight.Bold, color = SketchBorder)
                                Text("${network.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = SketchMuted)
                            }
                            if (ssid == network.ssid) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = SketchTeal, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // Switch Mode (Advanced)
            item { SectionHeader("Firmware Mode") }
            item {
                PlayfulCard {
                    Column {
                        Text("Protocol Switch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Spacer(Modifier.height(20.dp))
                        PrimaryActionButton(
                            text = "REBOOT TO ${selectedMode.uppercase()}",
                            onClick = { onSwitchMode(selectedMode) },
                            enabled = isConnected,
                            color = SketchPeach
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(UiTokens.BottomBarHeight)) }
        }
    }
}

@Composable
private fun FirebaseMiniButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        shape = RoundedCornerShape(UiTokens.SmallRadius),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = BorderStroke(1.2.dp, color.coerceAlpha(0.3f))
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}

private fun Color.coerceAlpha(alpha: Float): Color = this.copy(alpha = alpha)

@Preview(name = "WiFi Config", showBackground = true, showSystemUi = true)
@Composable
private fun WifiConfigScreenPreview() {
    ChronchiTheme {
        WifiConfigScreen(
            state = previewUiState,
            onSendWifiConfig = { _, _ -> },
            onSwitchMode = {},
            onScanWifi = {},
            onSendFirebaseConfig = {},
            onCheckFirebaseStatus = {},
            onForgetFirebase = {}
        )
    }
}
