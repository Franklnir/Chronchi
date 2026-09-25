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
import androidx.compose.ui.text.style.TextAlign
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
    onLinkGoogle: () -> Unit,
    onUnlinkGoogle: () -> Unit,
    onSwitchToChronchi: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var toolSearchQuery by remember { mutableStateOf("") }
    var selectedToolCategory by remember { mutableStateOf("all") }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showUnlinkGoogleConfirm by remember { mutableStateOf(false) }

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
                        .size(68.dp)
                        .background(NeoTokens.Yellow, CircleShape)
                        .border(NeoTokens.BorderWidth, NeoTokens.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.take(1).uppercase().ifBlank { "U" },
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.username.ifBlank { "Pengguna" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                    Spacer(Modifier.height(4.dp))
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
                                text = "Bergabung ${user.createdAt.take(10)}",
                                fontSize = 12.sp,
                                color = NeoTokens.Muted
                            )
                        }
                    }

                    if (!user.deviceMac.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DeveloperBoard,
                                contentDescription = null,
                                tint = NeoTokens.Emerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "MAC: ${user.deviceMac}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Dark
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
                            Text("🧠", fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Karakter & Persona AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Black
                            )
                        }
                        Text(
                            text = "RAG Vector Profiling Otomatis",
                            fontSize = 13.sp,
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
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Scan",
                            modifier = Modifier
                                .size(18.dp)
                                .then(if (isScanningPersona) Modifier.rotate(spinAngle) else Modifier)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isScanningPersona) "Scanning..." else "Scan Ulang",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(NeoTokens.MintLight, RoundedCornerShape(99.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        NeoPulseIndicator(active = true)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Deteksi Otomatis",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Dark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(NeoTokens.Gray.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "${persona.totalChatsAnalyzed} Obrolan Dianalisis",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Black
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Spektrum Kepribadian Card
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
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Black
                            )
                            NeoBadge(
                                text = persona.personality.primaryTrait,
                                backgroundColor = NeoTokens.Lavender
                            )
                        }

                        if (persona.personality.description.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = persona.personality.description,
                                fontSize = 13.sp,
                                color = NeoTokens.Dark,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        // Gauge Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🟣 Introvert (${persona.personality.introvertPercent}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Purple
                            )
                            Text(
                                text = "🟢 Extrovert (${persona.personality.extrovertPercent}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Emerald
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        // Gauge Split Track
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
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

                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Reflektif & Mandiri", fontSize = 11.sp, color = NeoTokens.Muted)
                            Text("Sosial & Terbuka", fontSize = 11.sp, color = NeoTokens.Muted)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Hobi & Minat Utama
                Text(
                    text = "🎯 Hobi & Minat Utama",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.height(16.dp))

                // Masalah & Tantangan Dihadapi
                Text(
                    text = "⚠️ Masalah & Tantangan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.height(16.dp))

                // Pola Rutinitas & Kegiatan
                Text(
                    text = "📅 Pola Rutinitas & Kegiatan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(8.dp))
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

                Spacer(Modifier.height(16.dp))

                // Kesukaan & Preferensi Interaksi
                Text(
                    text = "💬 Preferensi Interaksi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    persona.preferences.forEach { p ->
                        Box(
                            modifier = Modifier
                                .background(NeoTokens.MintLight, RoundedCornerShape(99.dp))
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${p.icon} ${p.name} ${p.percent}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Dark
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "💡 Terhubung ke Suara Xiaozhi: AI Xiaozhi di ESP32 Anda secara otomatis membaca data profil ini agar tanggapan suaranya selalu memahami kepribadian dan gaya bicara Anda.",
                        fontSize = 12.sp,
                        color = NeoTokens.Dark,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── 3. Akun Google Integration ──
        val isGoogleLinked = (!user.googleId.isNullOrBlank() && user.googleId != "null") || 
                             (!user.googleEmail.isNullOrBlank() && user.googleEmail != "null")
        val isRegisteredViaGoogle = user.registeredWithGoogle

        val displayEmail = user.googleEmail?.takeIf { it != "null" && it.isNotBlank() }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontSize = 22.sp, fontWeight = FontWeight.Black, color = NeoTokens.Blue)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tautan Akun Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Black
                            )
                            Text(
                                text = if (isGoogleLinked) (displayEmail ?: "Akun Google Terhubung") else "Belum ditautkan ke Google",
                                fontSize = 12.5.sp,
                                fontWeight = if (isGoogleLinked) FontWeight.Bold else FontWeight.Normal,
                                color = if (isGoogleLinked) NeoTokens.Dark else NeoTokens.Muted
                            )
                        }
                    }

                    if (isGoogleLinked) {
                        if (isRegisteredViaGoogle) {
                            NeoBadge(
                                text = "🔒 Akun Utama",
                                backgroundColor = Color(0xFFFEF3C7),
                                textColor = Color(0xFF92400E)
                            )
                        } else {
                            NeoBadge(
                                text = "✓ Tertaut",
                                backgroundColor = NeoTokens.MintLight,
                                textColor = Color(0xFF065F46)
                            )
                        }
                    } else {
                        NeoBadge(
                            text = "Tidak Tertaut",
                            backgroundColor = NeoTokens.Gray,
                            textColor = NeoTokens.Black
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Actions & Descriptions for Google Account
                if (isGoogleLinked) {
                    if (isRegisteredViaGoogle) {
                        // Registered with Google: Permanent, cannot unlink
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                .border(1.5.dp, Color(0xFFD97706), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Text("🔒", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Akun ini didaftarkan langsung menggunakan Akun Google, sehingga tautan Google ini berfungsi sebagai metode masuk utama dan tidak dapat dilepaskan demi keamanan akun Anda.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E),
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    } else {
                        // Linked via normal account: Can be unlinked
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { showUnlinkGoogleConfirm = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeoTokens.White,
                                    contentColor = NeoTokens.Coral
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Coral),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Putuskan Tautan", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Not linked: Provide link option
                    Button(
                        onClick = onLinkGoogle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeoTokens.White,
                            contentColor = NeoTokens.Blue
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, NeoTokens.Blue),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Tautkan dengan Google Sekarang", fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tautkan akun Google Anda untuk memungkinkan login cepat ('Masuk dengan Google') di masa mendatang tanpa perlu mengingat password.",
                        fontSize = 12.sp,
                        color = NeoTokens.Dark,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Kemampuan asisten suara Xiaozhi.",
                            fontSize = 13.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    NeoBadge(
                        text = "${data.totalTools} Tools",
                        backgroundColor = NeoTokens.Yellow
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Search Tools
                NeoTextField(
                    value = toolSearchQuery,
                    onValueChange = { toolSearchQuery = it },
                    placeholder = "Cari tools...",
                    label = "Pencarian Kemampuan"
                )

                Spacer(Modifier.height(10.dp))

                // Filter Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = listOf(
                        "all" to "Semua",
                        "core" to "Dasar",
                        "academic" to "Akademik",
                        "iot" to "Smart Home",
                        "productivity" to "Produktivitas",
                        "system" to "Sistem"
                    )
                    cats.forEach { (key, label) ->
                        val isSel = selectedToolCategory == key
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) NeoTokens.Yellow else NeoTokens.White,
                                    RoundedCornerShape(99.dp)
                                )
                                .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                                .clickable { selectedToolCategory = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Black
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                val filteredTools = data.toolsCatalog.filter { t ->
                    val matchCat = selectedToolCategory == "all" || t.category.equals(selectedToolCategory, ignoreCase = true)
                    val matchQ = toolSearchQuery.isBlank() ||
                        t.name.contains(toolSearchQuery, ignoreCase = true) ||
                        t.title.contains(toolSearchQuery, ignoreCase = true) ||
                        t.description.contains(toolSearchQuery, ignoreCase = true)
                    matchCat && matchQ
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTools.take(15).forEach { tool ->
                        ToolCatalogItemRow(tool = tool)
                    }
                    if (filteredTools.size > 15) {
                        Text(
                            text = "+ ${filteredTools.size - 15} tools lainnya aktif",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Muted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 5. Actions Footer ──
        NeoButton(
            text = "LOGOUT DARI XIAOZHI",
            onClick = { showLogoutConfirm = true },
            color = NeoTokens.Coral,
            textColor = NeoTokens.White
        )

        Spacer(Modifier.height(14.dp))

        // Switch to Chronchi BLE Mode
        NeoButton(
            text = "BERALIH KE CHRONCHI BLE",
            onClick = onSwitchToChronchi,
            color = NeoTokens.White,
            textColor = NeoTokens.Black
        )

        Spacer(Modifier.height(30.dp))
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Keluar dari Akun?", fontWeight = FontWeight.Black) },
            text = { Text("Anda perlu memasukkan username dan password kembali untuk mengakses Xiaozhi AI.") },
            confirmButton = {
                NeoButton(
                    text = "YA, KELUAR",
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
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

    // Unlink Google Confirmation Dialog
    if (showUnlinkGoogleConfirm) {
        AlertDialog(
            onDismissRequest = { showUnlinkGoogleConfirm = false },
            title = { Text("Putuskan Tautan Google?", fontWeight = FontWeight.Black) },
            text = { Text("Apakah Anda yakin ingin memutuskan tautan akun Google Anda dari akun ini?") },
            confirmButton = {
                NeoButton(
                    text = "PUTUSKAN",
                    onClick = {
                        showUnlinkGoogleConfirm = false
                        onUnlinkGoogle()
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { showUnlinkGoogleConfirm = false },
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
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Dark
                )
            }
            Text(
                text = "$percent%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (percent.coerceIn(0, 100) / 100f))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun ToolCatalogItemRow(tool: XiaozhiToolItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
            .border(1.dp, NeoTokens.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = tool.icon, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.title.ifBlank { tool.name },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeoTokens.Black
            )
            Text(
                text = tool.description,
                fontSize = 12.sp,
                color = NeoTokens.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (tool.enabled) NeoTokens.Emerald else NeoTokens.Coral, CircleShape)
                .border(1.dp, NeoTokens.Black, CircleShape)
        )
    }
}
