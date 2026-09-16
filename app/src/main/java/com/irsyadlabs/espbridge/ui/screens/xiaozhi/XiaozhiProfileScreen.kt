package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.data.xiaozhi.*
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun XiaozhiProfileScreen(
    profileData: XiaozhiProfileData?,
    isScanningPersona: Boolean = false,
    onScanPersona: () -> Unit,
    onSwitchToChronchi: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var toolSearchQuery by remember { mutableStateOf("") }
    var selectedToolCategory by remember { mutableStateOf("all") }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val data = profileData ?: XiaozhiProfileData()
    val persona = data.personaAnalysis
    val user = data.user

    // Scanning rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "scan_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── 1. Profile Header ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(NeoTokens.Yellow, CircleShape)
                        .border(NeoTokens.BorderWidth, NeoTokens.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.take(1).uppercase().ifBlank { "U" },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username.ifBlank { "Pengguna" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NeoBadge(
                            text = user.role.uppercase(),
                            backgroundColor = if (user.role == "admin") NeoTokens.Yellow else NeoTokens.MintLight
                        )
                        if (user.createdAt.isNotBlank()) {
                            Text(
                                text = "Sejak ${user.createdAt.take(10)}",
                                fontSize = 11.sp,
                                color = NeoTokens.Muted
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 2. AI Persona & Vector Profiling (RAG Vector) ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧠", fontSize = 16.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Karakter & Persona AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Black
                            )
                        }
                        Text(
                            text = "RAG Vector Profiling Otomatis",
                            fontSize = 11.sp,
                            color = NeoTokens.Muted
                        )
                    }

                    // Scan Ulang AI Button
                    Button(
                        onClick = onScanPersona,
                        enabled = !isScanningPersona,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeoTokens.Emerald,
                            contentColor = NeoTokens.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeoTokens.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Scan",
                            modifier = Modifier
                                .size(16.dp)
                                .then(if (isScanningPersona) Modifier.rotate(spinAngle) else Modifier)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isScanningPersona) "Scanning..." else "Scan Ulang",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(NeoTokens.MintLight, RoundedCornerShape(99.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        NeoPulseIndicator(active = true)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Deteksi Otomatis",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Dark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Gray.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${persona.totalChatsAnalyzed} Riwayat Dianalisis",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Black
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Spektrum Kepribadian Hero Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧬 Spektrum Kepribadian",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Black
                            )
                            NeoBadge(
                                text = persona.personality.primaryTrait,
                                backgroundColor = NeoTokens.Lavender
                            )
                        }

                        if (persona.personality.description.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = persona.personality.description,
                                fontSize = 11.sp,
                                color = NeoTokens.Dark,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Gauge Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🟣 Introvert (${persona.personality.introvertPercent}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Purple
                            )
                            Text(
                                text = "🟢 Extrovert (${persona.personality.extrovertPercent}%)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Emerald
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Gauge Split Track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                        ) {
                            val introWeight = persona.personality.introvertPercent.coerceIn(5, 95).toFloat()
                            val extroWeight = persona.personality.extrovertPercent.coerceIn(5, 95).toFloat()
                            Box(
                                modifier = Modifier
                                    .weight(introWeight)
                                    .fillMaxHeight()
                                    .background(NeoTokens.Purple)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(extroWeight)
                                    .fillMaxHeight()
                                    .background(NeoTokens.Emerald)
                            )
                        }

                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reflektif & Mandiri", fontSize = 9.sp, color = NeoTokens.Muted)
                            Text("Sosial & Terbuka", fontSize = 9.sp, color = NeoTokens.Muted)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Hobi & Minat Utama
                Text(
                    text = "🎯 Hobi & Minat Utama (1 - 100% Rasio)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    persona.hobbies.forEach { h ->
                        PersonaMetricRow(
                            icon = h.icon,
                            name = h.name,
                            percent = h.percent,
                            barColor = NeoTokens.Blue
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Masalah & Tantangan Dihadapi
                Text(
                    text = "⚠️ Masalah & Tantangan (Prioritas Solusi)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    persona.challenges.forEach { c ->
                        PersonaMetricRow(
                            icon = c.icon,
                            name = c.name,
                            percent = c.percent,
                            barColor = if (c.badge == "danger") NeoTokens.Coral else NeoTokens.Amber
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Pola Rutinitas & Kegiatan
                Text(
                    text = "📅 Pola Rutinitas & Kegiatan",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    persona.activities.forEach { a ->
                        PersonaMetricRow(
                            icon = a.icon,
                            name = a.name,
                            percent = a.percent,
                            barColor = NeoTokens.Emerald
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Kesukaan & Preferensi Interaksi
                Text(
                    text = "💬 Kesukaan & Preferensi Interaksi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    persona.preferences.forEach { p ->
                        Box(
                            modifier = Modifier
                                .background(NeoTokens.MintLight, RoundedCornerShape(99.dp))
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${p.icon} ${p.name} ${p.percent}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Dark
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Terhubung ke Suara Xiaozhi: AI Xiaozhi di ESP32 Anda secara otomatis membaca data profil ini agar tanggapan suaranya selalu memahami kepribadian, hobi, dan rutinitas Anda.",
                        fontSize = 11.sp,
                        color = NeoTokens.Dark,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 3. Akun Google Integration ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NeoTokens.Blue)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Akun Google",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = user.googleEmail ?: "Belum Tertaut",
                            fontSize = 11.sp,
                            color = NeoTokens.Muted
                        )
                    }
                }

                NeoBadge(
                    text = if (user.googleId != null) "✓ Terhubung" else "Tidak Terhubung",
                    backgroundColor = if (user.googleId != null) NeoTokens.MintLight else NeoTokens.Gray
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 4. Tools Cerdas Aktif (All 39 Tools Catalog) ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🔧 Tools Cerdas Aktif",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Kemampuan asisten suara Xiaozhi terhubung.",
                            fontSize = 11.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    NeoBadge(
                        text = "${data.totalTools} Tools",
                        backgroundColor = NeoTokens.Yellow
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Search Tools
                NeoTextField(
                    value = toolSearchQuery,
                    onValueChange = { toolSearchQuery = it },
                    placeholder = "Cari tool cerdas...",
                    label = "Pencarian Tool"
                )

                Spacer(Modifier.height(8.dp))

                // Category Filter Pills
                val categories = listOf(
                    "all" to "Semua",
                    "knowledge" to "Knowledge",
                    "iot" to "Smart Home",
                    "education" to "Edukasi",
                    "productivity" to "Produktivitas",
                    "media" to "Media",
                    "critical_thinking" to "Logika",
                    "spiritual" to "Spiritual",
                    "info" to "Cuaca"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        val isSel = selectedToolCategory == catKey
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) NeoTokens.Yellow else NeoTokens.White,
                                    RoundedCornerShape(99.dp)
                                )
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                                .clickable { selectedToolCategory = catKey }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = catLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Black
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Filtered Tools List
                val filteredTools = data.toolsCatalog.filter { tool ->
                    val matchCat = selectedToolCategory == "all" || tool.category.equals(selectedToolCategory, ignoreCase = true)
                    val matchQ = toolSearchQuery.isBlank() ||
                        tool.title.contains(toolSearchQuery, ignoreCase = true) ||
                        tool.description.contains(toolSearchQuery, ignoreCase = true) ||
                        tool.categoryLabel.contains(toolSearchQuery, ignoreCase = true)
                    matchCat && matchQ
                }

                if (filteredTools.isEmpty()) {
                    Text(
                        text = "Tidak ada tool yang cocok.",
                        fontSize = 12.sp,
                        color = NeoTokens.Muted,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredTools.forEach { tool ->
                            ToolItemRow(tool = tool)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 5. Status Koneksi MCP ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (data.mcpStatus.connected) NeoTokens.Emerald
                                else if (data.mcpStatus.tokenSaved) NeoTokens.Amber
                                else NeoTokens.Gray,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (data.mcpStatus.connected) "MCP Terhubung"
                            else if (data.mcpStatus.tokenSaved) "Menunggu Bridge"
                            else "Tidak Terhubung",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = if (data.mcpStatus.connected) "MCP terhubung ke XiaoZhi"
                            else if (data.mcpStatus.tokenSaved) "Token tersimpan, menunggu bridge"
                            else "Endpoint MCP belum disimpan",
                            fontSize = 11.sp,
                            color = NeoTokens.Muted
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 6. Bantuan WhatsApp ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Butuh Bantuan?",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Hubungi kami via WhatsApp untuk bantuan teknis.",
                    fontSize = 11.sp,
                    color = NeoTokens.Muted
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val url = "https://wa.me/6289531832365?text=Halo%2C%20saya%20butuh%20bantuan%20terkait%20Xiaozhi%20Indonesia"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, NeoTokens.Black),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text(
                        text = "💬 Chat WhatsApp",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 7. Ubah Mode Operasi ──
        NeoButton(
            text = "🔄 GANTI MODE KE CHRONCHI BLE",
            onClick = onSwitchToChronchi,
            color = NeoTokens.Lavender,
            textColor = NeoTokens.Black
        )

        Spacer(Modifier.height(10.dp))

        // ── 8. Logout ──
        NeoButton(
            text = "KELUAR DARI AKUN",
            onClick = { showLogoutConfirm = true },
            color = NeoTokens.Coral,
            textColor = NeoTokens.White
        )

        Spacer(Modifier.height(36.dp))
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Keluar dari Akun?", fontWeight = FontWeight.Black) },
            text = { Text("Anda perlu login kembali untuk mengakses Xiaozhi AI.") },
            confirmButton = {
                NeoButton(
                    text = "YA, KELUAR",
                    onClick = {
                        onLogout()
                        showLogoutConfirm = false
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { showLogoutConfirm = false },
                    color = NeoTokens.Gray,
                    textColor = NeoTokens.Black
                )
            },
            containerColor = NeoTokens.Cream,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun PersonaMetricRow(
    icon: String,
    name: String,
    percent: Int,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 13.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Black
                )
            }
            Text(
                text = "$percent%",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = barColor
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(NeoTokens.Gray.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent.coerceIn(0, 100) / 100f)
                    .background(barColor, RoundedCornerShape(99.dp))
            )
        }
    }
}

@Composable
private fun ToolItemRow(tool: XiaozhiToolItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeoTokens.Gray.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .border(1.dp, NeoTokens.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(NeoTokens.White, RoundedCornerShape(6.dp))
                .border(1.dp, NeoTokens.Black, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(tool.icon, fontSize = 18.sp)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tool.description,
                fontSize = 10.sp,
                color = NeoTokens.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NeoBadge(text = tool.categoryLabel, backgroundColor = NeoTokens.White)
                if (tool.enabled) {
                    Text("✓ Aktif", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeoTokens.Emerald)
                } else {
                    Text("✗ Nonaktif", fontSize = 10.sp, color = NeoTokens.Muted)
                }
            }
        }
    }
}
