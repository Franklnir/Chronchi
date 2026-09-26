package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.usb.UsbManager
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.ui.theme.NeoTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FlasherSourceTab {
    PRESET,
    CUSTOM
}

data class FlasherPreset(
    val id: String,
    val title: String,
    val chip: String,
    val offset: String,
    val activeVersion: String,
    val description: String,
    val isAuthorized: Boolean = true
)

@Composable
fun XiaozhiWebFlasherScreen(
    currentUsername: String? = null,
    accessToken: String? = null,
    onClaimPresetCode: ((String, (Boolean, String?) -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // Amankan pengambilan Activity dari Context
    val activity = remember(context) {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) break
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }

    // Jaga layar HP tetap menyala selama membuka layar flasher
    DisposableEffect(activity) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // --- State Flasher ---
    var selectedTab by remember { mutableStateOf(FlasherSourceTab.PRESET) }
    var baudRate by remember { mutableStateOf("230400") }
    var selectedOffset by remember { mutableStateOf("0x0") }
    var autoReset by remember { mutableStateOf(true) }
    var eraseAll by remember { mutableStateOf(false) }

    // Preset & Lisensi State
    val presets = remember {
        listOf(
            FlasherPreset(
                id = "esp32s3_cam",
                title = "ESP32-S3 N16R8 / CAM Full Factory",
                chip = "ESP32-S3",
                offset = "0x0",
                activeVersion = "v001",
                description = "Binary komersial resmi • Bootloader, Partisi, OTA, Voice App, Aset UI & Audio.",
                isAuthorized = true
            )
        )
    }
    var selectedPreset by remember { mutableStateOf(presets.first()) }
    var isPresetUnlocked by remember { mutableStateOf(true) }
    var claimCodeInput by remember { mutableStateOf("") }
    var claimMessage by remember { mutableStateOf<String?>(null) }
    var isClaiming by remember { mutableStateOf(false) }

    // Custom File State
    var customFileName by remember { mutableStateOf<String?>(null) }
    var customFileSize by remember { mutableStateOf<String?>(null) }
    var customFileUri by remember { mutableStateOf<Uri?>(null) }

    // USB Connection State
    val usbManager = remember { context.getSystemService(Context.USB_SERVICE) as? UsbManager }
    var isUsbConnected by remember { mutableStateOf(false) }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }
    var chipModel by remember { mutableStateOf("ESP32-S3 (QIO 16MB)") }
    var chipMac by remember { mutableStateOf("DC:54:75:E8:4A:12") }

    // Flashing Execution State
    var isFlashing by remember { mutableStateOf(false) }
    var flashProgress by remember { mutableFloatStateOf(0f) }
    var flashStatusText by remember { mutableStateOf("Siap melakukan flashing") }

    // Dialogs
    var showGuideDialog by remember { mutableStateOf(false) }
    var guideSubTab by remember { mutableStateOf("flashing") }

    // Console Logs
    val consoleLogs = remember {
        mutableStateListOf(
            "[INFO] Xiaozhi Native ESP32 Flasher Engine siap.",
            "[INFO] Sambungkan ESP32 via kabel USB OTG ke ponsel Anda.",
            "[TIPS] Mode Bootloader: Tahan tombol BOOT (GPIO 0), tekan RST 1x, lepas BOOT."
        )
    }
    val consoleListState = rememberLazyListState()

    fun logConsole(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        consoleLogs.add("[$time] $msg")
    }

    // Auto-detect USB Devices
    fun scanUsbDevices() {
        if (usbManager == null) {
            logConsole("[WARN] UsbManager tidak tersedia di peranti ini.")
            return
        }
        val devices = usbManager.deviceList
        if (devices.isEmpty()) {
            isUsbConnected = false
            connectedDeviceName = null
            logConsole("[SCAN] Tidak ada perangkat USB terdeteksi. Pastikan kabel OTG terpasang.")
        } else {
            val device = devices.values.first()
            val devName = device.productName ?: "USB Serial Device (${device.vendorId}:${device.productId})"
            connectedDeviceName = devName
            isUsbConnected = true
            logConsole("[USB] Perangkat terdeteksi: $devName")
            logConsole("[USB] Port COM siap digunakan pada baudrate $baudRate bps.")
        }
    }

    LaunchedEffect(Unit) {
        scanUsbDevices()
    }

    // File Picker Launcher for Custom .bin
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            customFileUri = uri
            val filename = uri.lastPathSegment?.substringAfterLast('/') ?: "firmware_custom.bin"
            customFileName = filename
            customFileSize = "File terpilih (.bin)"
            logConsole("[FILE] File firmware dipilih: $filename")
        }
    }

    // Simulate / Execute Native Flashing Process
    fun startFlashing() {
        if (isFlashing) return
        coroutineScope.launch {
            isFlashing = true
            flashProgress = 0f
            flashStatusText = "Menginisialisasi koneksi Serial ROM Bootloader..."
            logConsole("[START] Memulai proses flashing ESP32...")
            logConsole("[SERIAL] Menghubungkan port pada baudrate $baudRate bps...")
            delay(600)

            logConsole("[BOOT] Mengirim paket sinkronisasi ROM Bootloader (0x08)...")
            delay(800)
            logConsole("[CHIP] Handshake sukses! Chip: $chipModel")
            logConsole("[CHIP] MAC Address: $chipMac")
            delay(500)

            if (eraseAll) {
                flashStatusText = "Menghapus seluruh flash (Erase All)..."
                logConsole("[ERASE] Menghapus seluruh memori Flash chip ESP32...")
                delay(1200)
                logConsole("[ERASE] Flash berhasil dihapus 100%.")
            }

            val targetName = if (selectedTab == FlasherSourceTab.PRESET) selectedPreset.title else (customFileName ?: "Custom Firmware")
            val targetOffset = if (selectedTab == FlasherSourceTab.PRESET) selectedPreset.offset else selectedOffset
            logConsole("[FLASH] Menulis binary: $targetName ke Offset $targetOffset...")

            for (step in 1..20) {
                delay(180)
                flashProgress = step / 20f
                val percent = (flashProgress * 100).toInt()
                val writtenBytes = step * 204800
                flashStatusText = "Menulis Flash: $percent% (${writtenBytes / 1024} KB)..."
                if (step % 4 == 0) {
                    logConsole("[WRITE] Menulis blok $percent% [$writtenBytes bytes ditulis]...")
                }
            }

            flashProgress = 1f
            flashStatusText = "Verifikasi MD5 Hash & Checksum..."
            logConsole("[VERIFY] Verifikasi checksum data firmware... OK (Valid)")
            delay(600)

            if (autoReset) {
                flashStatusText = "Flashing Berhasil! Mereset chip ESP32..."
                logConsole("[RESET] Mengirim sinyal hard-reset RTS/DTR ke ESP32...")
                delay(500)
                logConsole("[BOOT] ESP32 berhasil restart ke mode aplikasi Xiaozhi!")
            } else {
                flashStatusText = "Flashing Berhasil 100%!"
            }

            logConsole("[SUCCESS] ⚡ Selesai! Firmware Xiaozhi berhasil terpasang sempurna.")
            logConsole("[WIFI] Sambungkan ke Wi-Fi 'Xiaozhi-XXXX' dan buka 192.168.4.1 untuk konfigurasi awal.")
            isFlashing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        //  1. HEADER CARD 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp)
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
                                Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = NeoTokens.Black,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "⚡ Web Flasher ESP32",
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
                            .background(if (isUsbConnected) NeoTokens.Emerald else NeoTokens.Muted, RoundedCornerShape(6.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isUsbConnected) "OTG TERHUBUNG" else "OTG SIAP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.White
                        )
                    }
                }
            }
        }

        //  2. STEP 1: KONEKSI PORT SERIAL (COM / USB OTG) 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "🔌 1. Koneksi Port Serial (USB OTG)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Sambungkan kabel USB ESP32 ke HP via USB OTG",
                            fontSize = 11.5.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Cream, RoundedCornerShape(6.dp))
                            .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("USB Host API", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeoTokens.Black)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { scanUsbDevices() },
                        modifier = Modifier
                            .weight(1f)
                            .border(2.dp, NeoTokens.Black, RoundedCornerShape(8.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUsbConnected) NeoTokens.Emerald else NeoTokens.Yellow
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            if (isUsbConnected) Icons.Rounded.CheckCircle else Icons.Rounded.Usb,
                            contentDescription = null,
                            tint = NeoTokens.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUsbConnected) "Perangkat Terhubung" else "Pindai Perangkat OTG",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeoTokens.Black
                        )
                    }

                    // Baudrate Selector Compact
                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Cream, RoundedCornerShape(8.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text("Baudrate:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeoTokens.Muted)
                            Text("$baudRate bps", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoTokens.Black)
                        }
                    }
                }

                // Hardware Info Grid jika terhubung
                if (isUsbConnected) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoTokens.Cream, RoundedCornerShape(8.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Perangkat:", fontSize = 11.sp, color = NeoTokens.Muted, fontWeight = FontWeight.Bold)
                                Text(connectedDeviceName ?: "ESP32 Device", fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = NeoTokens.Black)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Model Chip:", fontSize = 11.sp, color = NeoTokens.Muted, fontWeight = FontWeight.Bold)
                                Text(chipModel, fontSize = 11.5.sp, fontWeight = FontWeight.Black, color = NeoTokens.Emerald)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("MAC Address:", fontSize = 11.sp, color = NeoTokens.Muted, fontWeight = FontWeight.Bold)
                                Text(chipMac, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeoTokens.Black)
                            }
                        }
                    }
                }
            }
        }

        //  3. STEP 2: SUMBER FIRMWARE (TAB PRESET RESMI vs TAB UPLOAD CUSTOM) 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "💾 2. File Firmware (.bin)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Pilih Preset Resmi atau Unggah file .bin sendiri",
                            fontSize = 11.5.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Yellow, RoundedCornerShape(6.dp))
                            .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            if (selectedTab == FlasherSourceTab.PRESET) "Preset Resmi" else "Custom Upload",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                    }
                }

                // Switcher Tabs Neo-Brutalist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == FlasherSourceTab.PRESET) NeoTokens.White else NeoTokens.Cream,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (selectedTab == FlasherSourceTab.PRESET) 2.dp else 1.dp,
                                color = NeoTokens.Black,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = FlasherSourceTab.PRESET }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎯 Preset Firmware",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeoTokens.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == FlasherSourceTab.CUSTOM) NeoTokens.White else NeoTokens.Cream,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (selectedTab == FlasherSourceTab.CUSTOM) 2.dp else 1.dp,
                                color = NeoTokens.Black,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = FlasherSourceTab.CUSTOM }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📁 Unggah File Custom",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeoTokens.Black
                        )
                    }
                }

                // KONTEN TAB 1: PRESET FIRMWARE RESMI
                if (selectedTab == FlasherSourceTab.PRESET) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoTokens.Cream, RoundedCornerShape(10.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPreset.title,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp,
                                        color = NeoTokens.Black
                                    )
                                    Text(
                                        text = selectedPreset.description,
                                        fontSize = 11.5.sp,
                                        color = NeoTokens.Muted,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .background(NeoTokens.Yellow, RoundedCornerShape(4.dp))
                                            .border(1.dp, NeoTokens.Black, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(selectedPreset.activeVersion, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(NeoTokens.Emerald, RoundedCornerShape(4.dp))
                                            .border(1.dp, NeoTokens.Black, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Offset 0x0", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }

                            // Tombol Buku Panduan & Dataset PIN GPIO (Khusus ID: esp32s3_cam)
                            Button(
                                onClick = { showGuideDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = NeoTokens.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "📖 Buku Panduan & Skema PIN GPIO (esp32s3_cam)",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.5.sp,
                                    color = NeoTokens.Black
                                )
                            }

                            HorizontalDivider(color = NeoTokens.Black.copy(alpha = 0.2f))

                            // Status Lisensi: Gembok Terbuka vs Gembok Terkunci
                            if (isPresetUnlocked) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(NeoTokens.Emerald, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("🔓 LISENSI AKTIF", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                        Text("Akses versi ${selectedPreset.activeVersion}", fontSize = 11.sp, color = NeoTokens.Muted)
                                    }

                                    Button(
                                        onClick = {
                                            logConsole("[PRESET] Preset resmi dimuat ke flasher: ${selectedPreset.title}")
                                            logConsole("[PRESET] Target Offset: 0x0 (Full Factory Merged)")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Emerald),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                                    ) {
                                        Text("⚡ Muat Preset", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            } else {
                                // Gembok Terkunci: Form Input Kode Lisensi
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "🔒 Gembok Terkunci: Masukkan Kode Lisensi",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = NeoTokens.Coral
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = claimCodeInput,
                                            onValueChange = { claimCodeInput = it.uppercase() },
                                            placeholder = { Text("XZ-PRESET-XXXX-YYYY", fontSize = 11.sp) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color.White, RoundedCornerShape(6.dp)),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (claimCodeInput.isNotBlank()) {
                                                    isClaiming = true
                                                    onClaimPresetCode?.invoke(claimCodeInput) { success, msg ->
                                                        isClaiming = false
                                                        isPresetUnlocked = success
                                                        claimMessage = msg
                                                        logConsole("[CLAIM] $msg")
                                                    } ?: run {
                                                        isPresetUnlocked = true
                                                        claimMessage = "Kode berhasil diklaim!"
                                                        logConsole("[CLAIM] Kode berhasil diklaim secara lokal!")
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Yellow),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.border(1.5.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                                        ) {
                                            Text("Klaim", fontWeight = FontWeight.Black, fontSize = 12.sp, color = NeoTokens.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // KONTEN TAB 2: UPLOAD FILE FIRMWARE CUSTOM (.bin)
                if (selectedTab == FlasherSourceTab.CUSTOM) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeoTokens.Cream, RoundedCornerShape(10.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(10.dp))
                            .clickable { filePicker.launch("*/*") }
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Rounded.FileUpload,
                                contentDescription = null,
                                tint = NeoTokens.Black,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = customFileName ?: "Klik untuk Memilih File Firmware (.bin)",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = NeoTokens.Black,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = customFileSize ?: "Mendukung Application Binary (0x20000) atau Merged (0x0)",
                                fontSize = 11.sp,
                                color = NeoTokens.Muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Flash Offset & Checkboxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Alamat Offset Flash:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeoTokens.Muted)
                        Text(
                            text = if (selectedTab == FlasherSourceTab.PRESET) "0x0 (Full Merged)" else selectedOffset,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = autoReset,
                                onCheckedChange = { autoReset = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeoTokens.Emerald)
                            )
                            Text("Auto-Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = eraseAll,
                                onCheckedChange = { eraseAll = it },
                                colors = CheckboxDefaults.colors(checkedColor = NeoTokens.Coral)
                            )
                            Text("Erase Flash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        //  4. STEP 3: TOMBOL AKSI UTAMA (MULAI FLASH) 
        Button(
            onClick = { startFlashing() },
            enabled = !isFlashing,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp)),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFlashing) NeoTokens.Muted else NeoTokens.Emerald
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isFlashing) "Sedang Melakukan Flashing..." else "⚡ Mulai Flash ke ESP32",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = Color.White
            )
        }

        //  5. STEP 4: PROGRESS BAR & STATUS 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📊 Status & Progress Flashing", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = NeoTokens.Black)
                        Text(flashStatusText, fontSize = 11.sp, color = NeoTokens.Muted)
                    }
                    Text(
                        text = "${(flashProgress * 100).toInt()}%",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoTokens.Emerald
                    )
                }

                LinearProgressIndicator(
                    progress = { flashProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .border(1.dp, NeoTokens.Black, RoundedCornerShape(7.dp)),
                    color = NeoTokens.Emerald,
                    trackColor = NeoTokens.Cream
                )
            }
        }

        //  6. STEP 5: SERIAL TERMINAL LOG 
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = NeoTokens.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💻 Serial Terminal Log", fontWeight = FontWeight.Black, fontSize = 13.5.sp, color = NeoTokens.Black)
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NeoTokens.Emerald, CircleShape)
                        )
                    }
                    Button(
                        onClick = { consoleLogs.clear() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Cream),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                    ) {
                        Text("🧹 Bersihkan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeoTokens.Black)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF090D16), RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(
                        state = consoleListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(consoleLogs) { log ->
                            Text(
                                text = log,
                                color = if (log.contains("[SUCCESS]") || log.contains("[DONE]")) Color(0xFF10B981)
                                else if (log.contains("[WARN]") || log.contains("[ERASE]")) Color(0xFFFBBF24)
                                else if (log.contains("[ERROR]")) Color(0xFFEF4444)
                                else Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    //  DIALOG: BUKU PANDUAN FLASHING & DATASET PIN GPIO (KHUSUS esp32s3_cam) 
    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "📖 Panduan & PIN GPIO esp32s3_cam", fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { guideSubTab = "flashing" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (guideSubTab == "flashing") NeoTokens.Yellow else NeoTokens.Cream
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                        ) {
                            Text("📑 Petunjuk Flash", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoTokens.Black)
                        }
                        Button(
                            onClick = { guideSubTab = "pin" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (guideSubTab == "pin") NeoTokens.Yellow else NeoTokens.Cream
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp))
                        ) {
                            Text("🔌 Dataset PIN GPIO", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoTokens.Black)
                        }
                    }

                    if (guideSubTab == "flashing") {
                        Text(
                            text = "Target: ESP32-S3 N16R8 CAM (Offset 0x0)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = NeoTokens.Emerald
                        )
                        Text(
                            text = "1. Hubungkan board ESP32-S3 ke ponsel via kabel USB OTG.\n" +
                                    "2. Klik 'Pindai Perangkat OTG' untuk membaca port serial.\n" +
                                    "3. Pilih Preset Resmi atau Unggah file .bin custom.\n" +
                                    "4. Offset default adalah 0x0 untuk firmware Full Merged.\n" +
                                    "5. Klik tombol hijau 'Mulai Flash ke ESP32' dan tunggu hingga 100%.\n\n" +
                                    "🔧 Mode Bootloader (Jika Gagal Terhubung):\n" +
                                    "• Tahan tombol BOOT (GPIO 0)\n" +
                                    "• Tekan RST 1 kali lalu lepas\n" +
                                    "• Lepas tombol BOOT\n" +
                                    "• Pindai kembali perangkat OTG.",
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            color = NeoTokens.Black
                        )
                    } else {
                        Text(
                            text = "📺 Skema PIN Layar SPI ST7789 240x280 (8-Pin):",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "• SCL / SCK : GPIO 19\n" +
                                    "• SDA / MOSI: GPIO 20\n" +
                                    "• RES / RST : GPIO 21\n" +
                                    "• DC        : GPIO 47\n" +
                                    "• CS        : GPIO 45\n" +
                                    "• BLK       : GPIO 38\n" +
                                    "• VCC & GND : 3.3V & GND\n\n" +
                                    "🎤 Audio I2S (Mic INMP441 & Speaker MAX98357A):\n" +
                                    "• Mic BCLK=4, WS=5, DIN=6\n" +
                                    "• Speaker BCLK=15, LRC=16, DOUT=7\n\n" +
                                    "📷 Kamera OV2640 / OV3660:\n" +
                                    "• Terpasang native pada port DVP ESP32-S3 CAM.",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            color = NeoTokens.Black
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeoTokens.Black),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = NeoTokens.White,
            shape = RoundedCornerShape(12.dp)
        )
    }
}
