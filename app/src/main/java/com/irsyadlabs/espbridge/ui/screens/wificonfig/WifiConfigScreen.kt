package com.irsyadlabs.espbridge.ui.screens.wificonfig

import android.annotation.SuppressLint
import android.content.Intent
import android.provider.Settings
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.ConnectionState
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.preview.previewUiState
import com.irsyadlabs.espbridge.ui.theme.*
import kotlinx.coroutines.delay

@SuppressLint("SetJavaScriptEnabled")
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
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("ble") } // "ble" or "portal"

    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var configSent by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf("chronchi") }
    var isScanning by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // In-App WebView state
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var webLoading by remember { mutableStateOf(false) }
    var webError by remember { mutableStateOf<String?>(null) }

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
        // Mode Switcher Tabs (BLE vs In-App Web Portal)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.White, RoundedCornerShape(UiTokens.CardRadius))
                .border(2.dp, SketchBorder, RoundedCornerShape(UiTokens.CardRadius))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (activeTab == "ble") SketchTeal else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = "ble" },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Bluetooth,
                        null,
                        tint = if (activeTab == "ble") Color.White else SketchBorder,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Koneksi BLE (Instan)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = if (activeTab == "ble") Color.White else SketchBorder
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (activeTab == "portal") SketchTeal else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeTab = "portal" },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Language,
                        null,
                        tint = if (activeTab == "portal") Color.White else SketchBorder,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Web Portal (192.168.4.1)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp,
                        color = if (activeTab == "portal") Color.White else SketchBorder
                    )
                }
            }
        }

        if (activeTab == "portal") {
            // ==========================================
            // TAB 2: IN-APP WEBVIEW PORTAL (192.168.4.1)
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SectionHeader("In-App Web Portal", "Konfigurasi langsung ESP32 tanpa keluar aplikasi")
                }

                item {
                    PlayfulCard {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusPill("HOTSPOT AP ESP32", StatusTone.GOOD)
                                Spacer(Modifier.width(8.dp))
                                Text("192.168.4.1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SketchTeal)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Sambungkan HP Anda ke Wi-Fi Hotspot Xiaozhi-XXXX, lalu atur Wi-Fi & layar ESP32 langsung di portal bawah ini:",
                                fontSize = 13.sp,
                                color = SketchMuted,
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                        } catch (e: Exception) {
                                            // Fallback
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SketchPeach),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, SketchBorder),
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Wifi, null, tint = SketchBorder, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Pilih Wi-Fi HP", color = SketchBorder, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        webError = null
                                        webLoading = true
                                        webViewRef?.loadUrl("http://192.168.4.1/")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SketchTeal),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.5.dp, SketchBorder),
                                    modifier = Modifier.weight(1f).height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Muat Ulang", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    PlayfulCard(modifier = Modifier.fillMaxWidth().height(520.dp), innerPadding = PaddingValues(0.dp)) {
                        Box(Modifier.fillMaxSize()) {
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

                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                webLoading = true
                                                webError = null
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                webLoading = false
                                            }

                                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                super.onReceivedError(view, request, error)
                                                if (request?.isForMainFrame == true) {
                                                    webLoading = false
                                                    webError = "Belum terhubung ke Hotspot ESP32. Pastikan HP tersambung ke Wi-Fi 'Xiaozhi-XXXX'."
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

                            if (webLoading) {
                                Box(
                                    Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = SketchTeal, strokeWidth = 3.dp)
                                        Spacer(Modifier.height(10.dp))
                                        Text("Memuat portal 192.168.4.1...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SketchBorder)
                                    }
                                }
                            }

                            if (webError != null) {
                                Box(
                                    Modifier.fillMaxSize().background(Color.White).padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Rounded.WifiOff, null, tint = SketchRed, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(12.dp))
                                        Text("Portal Belum Terdeteksi", fontWeight = FontWeight.Black, fontSize = 16.sp, color = SketchBorder)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            webError ?: "",
                                            fontSize = 12.5.sp,
                                            color = SketchMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                webError = null
                                                webLoading = true
                                                webViewRef?.loadUrl("http://192.168.4.1/")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SketchTeal),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.5.dp, SketchBorder)
                                        ) {
                                            Text("Coba Hubungkan Ulang", fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // TAB 1: NATIVE BLE CONFIGURATION
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
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
                                        Icons.Rounded.Wifi,
                                        null,
                                        tint = SketchTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Text(
                                        network.ssid,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = SketchBorder
                                    )
                                    Text(
                                        "Signal: ${network.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SketchMuted
                                    )
                                }
                                StatusPill(
                                    when {
                                        network.rssi > -60 -> "EXCELLENT"
                                        network.rssi > -75 -> "GOOD"
                                        else -> "WEAK"
                                    },
                                    when {
                                        network.rssi > -60 -> StatusTone.GOOD
                                        network.rssi > -75 -> StatusTone.WARN
                                        else -> StatusTone.ERROR
                                    }
                                )
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
    XichiTheme {
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
