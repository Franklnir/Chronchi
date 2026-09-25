package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

private const val WEB_FLASHER_URL = "https://xiaozhiscig.biz.id/web-flasher"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun XiaozhiWebFlasherScreen(
    currentUsername: String? = null,
    onClaimPresetCode: (String, (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Jaga layar HP tetap menyala selama membuka layar flasher
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var claimCodeInput by remember { mutableStateOf("") }
    var isClaiming by remember { mutableStateOf(false) }
    var claimMessage by remember { mutableStateOf<String?>(null) }
    var isClaimSuccess by remember { mutableStateOf(false) }

    var showGuideDialog by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isWebViewLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. HEADER KARTU NEO-BRUTALIST ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(NeoTokens.Yellow, RoundedCornerShape(8.dp))
                                .border(2.dp, NeoTokens.Black, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = "Flasher",
                                tint = NeoTokens.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Web Flasher ESP32",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = NeoTokens.Black
                            )
                            Text(
                                text = "Flash Firmware Resmi via USB OTG",
                                fontSize = 11.5.sp,
                                color = NeoTokens.Muted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Emerald, RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PORT OTG READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.White
                        )
                    }
                }
            }
        }

        // ── 2. PANDUAN CEPAT FLASHING ANDROID USB OTG ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📱 Panduan Flashing di HP Android:",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.5.sp,
                    color = NeoTokens.Black
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "1. Hubungkan board ESP32 ke HP menggunakan kabel data USB OTG.\n" +
                            "2. Klik tombol hijau di bawah untuk membuka Engine Flasher di Chrome USB OTG.\n" +
                            "3. Izinkan popup izin perangkat USB serial di browser Chrome.\n" +
                            "4. Pilih Preset Firmware dan klik Mulai Flash!",
                    fontSize = 12.sp,
                    color = NeoTokens.Black,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Aksi Utama: Buka Full Engine di Chrome USB OTG
                Button(
                    onClick = { launchChromeFlasher(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeoTokens.Emerald,
                        contentColor = NeoTokens.White
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Buka Engine Flasher (Chrome USB OTG)",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Buka Panduan Pinout & Bootloader Khusus esp32s3_cam
                OutlinedButton(
                    onClick = { showGuideDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = NeoTokens.Cyan,
                        contentColor = NeoTokens.Black
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "📖 Buku Panduan & PIN GPIO (esp32s3_cam)",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // ── 3. FORM KLAIM KODE LISENSI SEKALI PAKAI ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🎟️ Klaim Kode Lisensi Firmware Sekali Pakai:",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = NeoTokens.Black
                )
                Text(
                    text = "Masukkan kode lisensi pembelian kit ESP32 Anda untuk membuka akses preset komersial:",
                    fontSize = 11.5.sp,
                    color = NeoTokens.Muted
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = claimCodeInput,
                        onValueChange = { claimCodeInput = it.uppercase() },
                        placeholder = { Text("XZ-PRESET-XXXX-YYYY", fontSize = 11.5.sp, color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = NeoTokens.Cream,
                            unfocusedContainerColor = NeoTokens.Cream,
                            focusedBorderColor = NeoTokens.Black,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )

                    Button(
                        onClick = {
                            val code = claimCodeInput.trim()
                            if (code.isNotBlank()) {
                                isClaiming = true
                                claimMessage = null
                                onClaimPresetCode(code) { success, msg ->
                                    isClaiming = false
                                    isClaimSuccess = success
                                    claimMessage = msg
                                    if (success) {
                                        claimCodeInput = ""
                                        webViewInstance?.reload()
                                    }
                                }
                            }
                        },
                        enabled = claimCodeInput.isNotBlank() && !isClaiming,
                        modifier = Modifier
                            .height(52.dp)
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeoTokens.Yellow,
                            contentColor = NeoTokens.Black,
                            disabledContainerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isClaiming) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeoTokens.Black, strokeWidth = 2.dp)
                        } else {
                            Text("🔓 Buka", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }

                if (claimMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isClaimSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isClaimSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Text(
                            text = claimMessage.orEmpty(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isClaimSuccess) Color(0xFF15803D) else Color(0xFFB91C1C)
                        )
                    }
                }
            }
        }

        // ── 4. IN-APP WEBVIEW FLASHER PORTAL ──
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Bar status webview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoTokens.Black)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Web Portal Preview",
                        color = NeoTokens.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Reload",
                                tint = NeoTokens.Yellow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isWebViewLoading = false
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    cacheMode = WebSettings.LOAD_NO_CACHE
                                    userAgentString = settings.userAgentString + " MobileFlasherApp/1.4.1"
                                }
                                loadUrl(WEB_FLASHER_URL)
                                webViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isWebViewLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(NeoTokens.Cream.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeoTokens.Emerald, strokeWidth = 3.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Memuat Web Flasher...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoTokens.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── DIALOG: BUKU PANDUAN FLASHING & SKEMA PIN GPIO KHUSUS esp32s3_cam ──
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "📖 Panduan Flashing esp32s3_cam", fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Target Preset: ESP32-S3 N16R8 CAM (Offset 0x0)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.5.sp,
                        color = NeoTokens.Emerald
                    )
                    HorizontalDivider()

                    Text(
                        text = "🔧 Cara Masuk Mode Bootloader USB:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "1. Tahan tombol BOOT (GPIO 0).\n" +
                                "2. Tekan tombol RST / EN 1 kali lalu lepas.\n" +
                                "3. Lepaskan tombol BOOT.\n" +
                                "4. Perangkat kini berada di mode download flashing.",
                        fontSize = 11.5.sp,
                        color = NeoTokens.Black,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider()

                    Text(
                        text = "🔌 Skema PIN Layar SPI ST7789 240x280 (8-Pin):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "• SCL / SCK: GPIO 19\n" +
                                "• SDA / MOSI: GPIO 20\n" +
                                "• RES / RST: GPIO 21\n" +
                                "• DC: GPIO 47\n" +
                                "• CS: GPIO 45\n" +
                                "• BLK (Backlight): GPIO 38\n" +
                                "• VCC & GND: 3.3V & GND",
                        fontSize = 11.5.sp,
                        color = NeoTokens.Black,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider()

                    Text(
                        text = "🎤 Modul Audio & Kamera Bawaan:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "• Mic INMP441: BCLK=GPIO 4, WS=GPIO 5, DIN=GPIO 6\n" +
                                "• Speaker MAX98357A: BCLK=GPIO 15, LRC=GPIO 16, DOUT=GPIO 7\n" +
                                "• Kamera OV2640 / OV3660: Port DVP terpasang native.",
                        fontSize = 11.5.sp,
                        color = NeoTokens.Black,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Black)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = NeoTokens.White,
            shape = RoundedCornerShape(10.dp)
        )
    }
}

/**
 * Membuka engine flasher di peramban Google Chrome Android
 * untuk memastikan WebUSB dan Web Serial API dapat mengakses kabel USB OTG secara native.
 */
private fun launchChromeFlasher(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_FLASHER_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Coba arahkan ke package Google Chrome untuk jaminan WebUSB / WebSerial API
        setPackage("com.android.chrome")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback ke browser default jika Chrome belum terpasang
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(WEB_FLASHER_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallbackIntent)
    }
}
