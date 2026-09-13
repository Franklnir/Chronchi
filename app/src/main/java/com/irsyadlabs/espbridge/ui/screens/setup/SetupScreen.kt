package com.irsyadlabs.espbridge.ui.screens.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.*
import com.irsyadlabs.espbridge.transport.ble.DiscoveredBleDevice
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.preview.previewUiState
import com.irsyadlabs.espbridge.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

@Composable
fun SetupScreen(
    state: MainUiState,
    onMode: (ConnectionMode) -> Unit,
    onCompanionPair: () -> Unit,
    onScan: () -> Unit,
    onConnect: (DiscoveredBleDevice) -> Unit,
    onSendWifiConfig: (String, String) -> Unit,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    onRegenerateCredentials: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var revealSecret by remember { mutableStateOf(false) }
    var showScanner by remember { mutableStateOf(false) }
    val connected = state.bleState == ConnectionState.CONNECTED

    if (showScanner) {
        BarcodeScannerDialog(
            onResult = { result ->
                showScanner = false
                // Parse WiFi QR: WIFI:T:WPA;S:ssid;P:password;;
                if (result.startsWith("WIFI:", ignoreCase = true)) {
                    val ssid = result.substringAfter("S:").substringBefore(";")
                    val password = result.substringAfter("P:").substringBefore(";")
                    if (connected) {
                        onSendWifiConfig(ssid, password)
                    } else {
                        clipboard.setText(AnnotatedString("WiFi: $ssid / $password"))
                    }
                } else {
                    clipboard.setText(AnnotatedString(result))
                }
            },
            onDismiss = { showScanner = false }
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
                SectionHeader("Hardware Setup", "Device Provisioning & Pairing")
            }

            item {
                PlayfulCard(background = if (connected) Color(0xFFF1F9F8) else SketchSurface) {
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(56.dp).background(if (connected) Color.White else SketchTeal.copy(alpha = 0.1f), CircleShape).border(1.2.dp, SketchTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Router, null, tint = SketchTeal, modifier = Modifier.size(28.dp))
                            }
                            Column(Modifier.padding(start = 20.dp).weight(1f)) {
                                Text(
                                    state.settings.trustedDeviceName ?: "No Hardware Paired",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SketchBorder
                                )
                                Text(
                                    state.settings.trustedDeviceAddress ?: "Protocol standby",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SketchMuted
                                )
                            }
                            StatusPill(
                                if (connected) "Linked" else if (state.bleState == ConnectionState.ERROR) "Error" else state.bleState.name.lowercase().capitalize(),
                                when {
                                    connected -> StatusTone.GOOD
                                    state.bleState == ConnectionState.ERROR -> StatusTone.ERROR
                                    else -> StatusTone.WARN
                                }
                            )
                        }
                        
                        if (state.bleProtocolStatus.lastError != null) {
                            Text(
                                "STATUS: ${state.bleProtocolStatus.lastError}",
                                modifier = Modifier.padding(top = 12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SketchRed
                            )
                        }
                        
                        if (state.bleRssi != null) {
                            Text(
                                "LINK STRENGTH: ${state.bleRssi} dBm",
                                modifier = Modifier.padding(top = 20.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = SketchTeal
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (state.settings.trustedDeviceAddress != null && !connected) {
                                PrimaryActionButton(
                                    text = "RECONNECT",
                                    onClick = onReconnect,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (connected) {
                                OutlinedButton(
                                    onClick = onDisconnect,
                                    modifier = Modifier.weight(1f).height(UiTokens.PrimaryButtonHeight),
                                    shape = RoundedCornerShape(UiTokens.SmallRadius),
                                    border = BorderStroke(1.5.dp, SketchBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SketchBorder)
                                ) { Text("DISCONNECT", fontWeight = FontWeight.ExtraBold) }
                            }
                            if (state.settings.trustedDeviceAddress != null) {
                                TextButton(onClick = onForget) { Text("FORGET", color = BrandRed, fontWeight = FontWeight.ExtraBold) }
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Transmission") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeChip("Automatic", ConnectionMode.AUTOMATIC, state.settings.connectionMode, Modifier.weight(1f), onMode)
                    ModeChip("BLE Only", ConnectionMode.BLUETOOTH_ONLY, state.settings.connectionMode, Modifier.weight(1f), onMode)
                }
            }

            item { SectionHeader("Discovery") }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryActionButton(
                            text = "SCAN QR CONFIG",
                            onClick = { showScanner = true },
                            modifier = Modifier.weight(1f),
                            color = SketchPeach
                        )
                        PrimaryActionButton(
                            text = "COMPANION",
                            onClick = onCompanionPair,
                            modifier = Modifier.weight(1f),
                            color = SketchYellow
                        )
                    }
                    OutlinedButton(
                        onClick = onScan,
                        modifier = Modifier.fillMaxWidth().height(UiTokens.PrimaryButtonHeight),
                        shape = RoundedCornerShape(UiTokens.SmallRadius),
                        border = BorderStroke(1.5.dp, SketchBorder)
                    ) {
                        Icon(Icons.Rounded.Search, null, modifier = Modifier.size(20.dp), tint = SketchBorder)
                        Spacer(Modifier.width(10.dp))
                        Text(if (state.bleState == ConnectionState.SCANNING) "SCANNING..." else "DIRECT BLE SCAN", fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                    }
                }
            }
            
            items(state.bleDevices) { device ->
                PlayfulCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).background(SketchTeal.copy(alpha = 0.05f), CircleShape).border(1.2.dp, SketchTeal.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Bluetooth, null, tint = SketchTeal, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                            Text(device.name, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                            Text(device.address, style = MaterialTheme.typography.bodySmall, color = SketchMuted)
                        }
                        val isConnecting = state.bleState == ConnectionState.CONNECTING || state.bleState == ConnectionState.DISCOVERING
                        TextButton(
                            onClick = { onConnect(device) },
                            enabled = !isConnecting && !connected
                        ) { 
                            Text(
                                if (isConnecting) "..." else "PAIR", 
                                color = if (!isConnecting && !connected) SketchTeal else SketchMuted, 
                                fontWeight = FontWeight.ExtraBold
                            ) 
                        }
                    }
                }
            }

            item { SectionHeader("Stream Monitor") }
            item {
                LiveDataMonitorCard(liveData = state.phoneState.liveData)
            }
            
            item { SectionHeader("Visual Emulator") }
            item { OledPreviewCard(phoneState = state.phoneState) }

            item { SectionHeader("Security Keys") }
            item {
                PlayfulCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val credentials = state.credentials
                        CredentialRow("DEVICE ID", credentials?.deviceId.orEmpty()) { 
                            clipboard.setText(AnnotatedString(credentials?.deviceId.orEmpty())) 
                        }
                        CredentialRow("ACCESS KEY", credentials?.accessKey.orEmpty()) { 
                            clipboard.setText(AnnotatedString(credentials?.accessKey.orEmpty())) 
                        }
                        
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { revealSecret = !revealSecret }) {
                                Text(if (revealSecret) "HIDE SECRET" else "SHOW SECRET", fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = onRegenerateCredentials) {
                                Icon(Icons.Rounded.Refresh, null, tint = SketchTeal)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(UiTokens.BottomBarHeight)) }
        }
    }
}

@Composable
private fun LiveDataMonitorCard(liveData: LiveDataState) {
    val event = liveData.event
    PlayfulCard(background = SketchSurface) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("PROTOCOL MONITOR", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                StatusPill(if (event == null) "STANDBY" else "CAPTURED", if (event == null) StatusTone.INFO else StatusTone.GOOD)
            }
            
            Spacer(Modifier.height(20.dp))
            
            if (event == null) {
                Text("Awaiting device communication...", color = SketchMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp))
            } else {
                MonitorField("APP", event.sourceApp)
                MonitorField("TIMESTAMP", formatCapturedTime(event.timestamp))
                
                Spacer(Modifier.height(20.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SketchBg, RoundedCornerShape(UiTokens.InnerRadius))
                        .border(1.2.dp, SketchBorder.copy(alpha = 0.1f), RoundedCornerShape(UiTokens.InnerRadius))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("ESP PACKET", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = SketchTeal)
                        Spacer(Modifier.height(8.dp))
                        event.espDisplayLines().forEach { line ->
                            Text(line, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = SketchBorder, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OledPreviewCard(phoneState: PhoneState) {
    val currentEvent = phoneState.liveData.event
    var nowMillis by remember(currentEvent?.timestamp) { mutableStateOf(System.currentTimeMillis()) }

    val preview = OledPreviewSelector.select(phoneState, nowMillis)
    
    PlayfulCard(background = SketchBorder) {
        Column {
            Text("HARDWARE OLED EMULATOR", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
                    .background(Color.Black, RoundedCornerShape(UiTokens.InnerRadius))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(UiTokens.InnerRadius))
                    .padding(12.dp)
            ) {
                OledCanvas(preview)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Simulated 128x64 display output",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun OledCanvas(preview: OledPreviewState) {
    when (preview.template) {
        OledTemplate.HOME -> OledHome(preview)
        OledTemplate.MESSAGE -> OledMessage(preview)
        OledTemplate.PROFESSIONAL -> OledProfessional(preview)
        OledTemplate.PAYMENT -> OledPayment(preview)
        OledTemplate.ORDER -> OledOrder(preview)
        OledTemplate.NAVIGATION -> OledNavigation(preview)
        OledTemplate.SYSTEM -> OledSystem(preview)
    }
}

@Composable
private fun OledHome(preview: OledPreviewState) {
    val home = preview.home
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(home.networkType ?: "Offline", color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text("${home.batteryLevel}%", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Text(
            home.time,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
        Text(
            home.date,
            modifier = Modifier.fillMaxWidth(),
            color = Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable private fun OledMessage(p: OledPreviewState) { OledPlaceholder("Message") }
@Composable private fun OledProfessional(p: OledPreviewState) { OledPlaceholder("Email/Pro") }
@Composable private fun OledPayment(p: OledPreviewState) { OledPlaceholder("Payment") }
@Composable private fun OledOrder(p: OledPreviewState) { OledPlaceholder("Order") }
@Composable private fun OledNavigation(p: OledPreviewState) { OledPlaceholder("Navigation") }
@Composable private fun OledSystem(p: OledPreviewState) { OledPlaceholder("System") }

@Composable
private fun OledPlaceholder(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(label.uppercase(), color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun MonitorField(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SketchMuted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(value, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
    }
}

@Composable
private fun ModeChip(label: String, mode: ConnectionMode, selected: ConnectionMode, modifier: Modifier, onMode: (ConnectionMode) -> Unit) {
    FilterChip(
        selected = mode == selected,
        onClick = { onMode(mode) },
        label = { Text(label, modifier = Modifier.padding(horizontal = 4.dp)) },
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.SmallRadius),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SketchTeal,
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = SketchMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = mode == selected,
            borderColor = SketchBorder,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.5.dp
        )
    )
}

@Composable
private fun CredentialRow(label: String, value: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = SketchMuted, fontWeight = FontWeight.Bold)
            Text(value, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SketchBorder)
        }
        IconButton(onClick = onCopy) { Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(20.dp), tint = SketchTeal) }
    }
}

private fun String.capitalize() = replaceFirstChar { it.uppercase() }

private fun formatCapturedTime(timestamp: Long): String = runCatching {
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}.getOrDefault("--:--:--")

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SetupScreenPreview() {
    ChronchiTheme {
        SetupScreen(
            state = previewUiState,
            onMode = {}, onCompanionPair = {}, onScan = {}, onConnect = {},
            onSendWifiConfig = { _, _ -> },
            onReconnect = {}, onDisconnect = {}, onForget = {}, onRegenerateCredentials = {}
        )
    }
}
