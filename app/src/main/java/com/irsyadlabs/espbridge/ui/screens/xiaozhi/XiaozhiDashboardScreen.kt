package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.data.xiaozhi.*
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun XiaozhiDashboardScreen(
    dashboardData: XiaozhiDashboardData,
    isLoading: Boolean = false,
    onRefresh: () -> Unit,
    onCreateMaterial: (title: String, category: String, content: String, keywords: String, apiUrl: String) -> Unit,
    onUpdateMaterial: (id: Int, title: String, category: String, content: String, keywords: String, apiUrl: String) -> Unit,
    onDeleteMaterial: (id: Int) -> Unit,
    onCreateCategory: (name: String) -> Unit,
    onDeleteCategory: (id: Int) -> Unit,
    onSaveMcpToken: (token: String) -> Unit,
    onReconnectMcp: () -> Unit,
    onDeleteMcpToken: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Filter states
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("Semua") }

    // Tambah Materi form state
    var showAddForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("") }
    var newContent by remember { mutableStateOf("") }
    var newKeywords by remember { mutableStateOf("") }
    var newIsLiveApi by remember { mutableStateOf(false) }
    var newApiUrl by remember { mutableStateOf("") }

    // Edit Material state
    var editingMaterial by remember { mutableStateOf<XiaozhiMaterial?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("") }
    var editContent by remember { mutableStateOf("") }
    var editKeywords by remember { mutableStateOf("") }
    var editApiUrl by remember { mutableStateOf("") }

    // MCP input state
    var mcpInput by remember { mutableStateOf(dashboardData.mcpStatus.tokenPreview) }
    var showDeleteMcpConfirm by remember { mutableStateOf(false) }

    // Kategori input state
    var newCategoryName by remember { mutableStateOf("") }

    // Material delete confirm
    var deletingMaterialId by remember { mutableStateOf<Int?>(null) }
    var deletingCategoryId by remember { mutableStateOf<Int?>(null) }

    // Default category if none selected or if previously selected category was deleted
    LaunchedEffect(dashboardData.categories) {
        if (dashboardData.categories.isNotEmpty()) {
            if (newCategory.isBlank() || dashboardData.categories.none { it.name.equals(newCategory, ignoreCase = true) }) {
                newCategory = dashboardData.categories.first().name
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── 1. Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Halo, ${dashboardData.user.username.ifBlank { "Pengguna" }}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Muted
                )
                Text(
                    text = "Dashboard Xiaozhi",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Text(
                    text = "Kelola materi, catatan, dan data API realtime untuk Xiaozhi.",
                    fontSize = 14.sp,
                    color = NeoTokens.Muted
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .background(NeoTokens.White, RoundedCornerShape(10.dp))
                    .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = "Refresh",
                    tint = NeoTokens.Black
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Knowledge Base Active Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(NeoTokens.MintLight, RoundedCornerShape(99.dp))
                .border(2.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            NeoPulseIndicator(active = true)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Knowledge base aktif",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeoTokens.Dark
            )
        }

        // ── Hardware Board Status (MAC ID) ──
        Spacer(Modifier.height(14.dp))
        val boardMac = dashboardData.user.deviceMac
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (!boardMac.isNullOrBlank()) NeoTokens.MintLight else NeoTokens.White,
            borderColor = NeoTokens.Black,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (!boardMac.isNullOrBlank()) NeoTokens.Emerald else NeoTokens.Gray, RoundedCornerShape(10.dp))
                        .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeveloperBoard,
                        contentDescription = "Board ESP32",
                        tint = if (!boardMac.isNullOrBlank()) NeoTokens.White else NeoTokens.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "HARDWARE BOARD ESP32",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Muted
                        )
                        Spacer(Modifier.width(6.dp))
                        if (!boardMac.isNullOrBlank()) {
                            NeoBadge(
                                text = "TERHUBUNG",
                                backgroundColor = NeoTokens.Emerald,
                                textColor = NeoTokens.White
                            )
                        } else {
                            NeoBadge(
                                text = "MENUNGGU BOARD",
                                backgroundColor = NeoTokens.Yellow,
                                textColor = NeoTokens.Black
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (!boardMac.isNullOrBlank()) "MAC ID: $boardMac" else "Belum Ada Board Terdeteksi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                    Text(
                        text = if (!boardMac.isNullOrBlank()) "Board ESP32 aktif & terdaftar di akun Anda." else "Nyalakan board ESP32 Anda agar terhubung secara otomatis.",
                        fontSize = 12.sp,
                        color = NeoTokens.Dark
                    )
                }
            }
        }

        // ── 2. MCP Disconnected Warning Banner ──
        if (!dashboardData.mcpStatus.connected && dashboardData.user.role != "admin") {
            Spacer(Modifier.height(16.dp))
            NeoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFEF3C7),
                contentPadding = PaddingValues(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(NeoTokens.Amber, CircleShape)
                            .padding(top = 4.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PERHATIAN: ENDPOINT MCP BELUM TERHUBUNG",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Asisten suara Xiaozhi belum dapat membaca materi atau mengontrol perangkat hingga endpoint MCP Anda terhubung.",
                            fontSize = 14.sp,
                            color = NeoTokens.Dark
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 3. Stat Grid (4 Cards in 2x2) ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Total Materi",
                value = "${dashboardData.stats.total}",
                bgColor = NeoTokens.White,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Tugas",
                value = "${dashboardData.stats.tugas}",
                bgColor = NeoTokens.MintLight,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Pengumuman",
                value = "${dashboardData.stats.pengumuman}",
                bgColor = Color(0xFFFEF3C7),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Materi",
                value = "${dashboardData.stats.materi}",
                bgColor = NeoTokens.Lavender,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── 4. Batas Akun (Quota Card) ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(14.dp)
        ) {
            Column {
                Text(
                    text = "Batas Akun",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Text(
                    text = "Kuota yang ditetapkan admin untuk akun ini.",
                    fontSize = 14.sp,
                    color = NeoTokens.Muted
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuotaChip(label = "Total Materi: ${dashboardData.quota.materials.label}")
                    QuotaChip(
                        label = if (dashboardData.quota.wordsPerMaterial.unlimited) "Kata / Materi: Tanpa batas"
                        else "Kata / Materi: ${dashboardData.quota.wordsPerMaterial.limit}"
                    )
                    QuotaChip(label = "API Realtime: ${dashboardData.quota.liveApis.label}")
                    QuotaChip(label = "Relay: ${dashboardData.quota.relayRooms.label}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 5. Koneksi XiaoZhi (MCP Card) ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (dashboardData.mcpStatus.connected) NeoTokens.MintLight else NeoTokens.White,
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
                            text = "Koneksi XiaoZhi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Endpoint MCP yang dipakai XiaoZhi.",
                            fontSize = 14.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    NeoBadge(
                        text = if (dashboardData.mcpStatus.connected) "Terhubung"
                        else if (dashboardData.mcpStatus.tokenSaved) "Menunggu"
                        else "Belum aktif",
                        backgroundColor = if (dashboardData.mcpStatus.connected) NeoTokens.Mint
                        else if (dashboardData.mcpStatus.tokenSaved) NeoTokens.Yellow
                        else NeoTokens.Gray
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Alert message
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (dashboardData.mcpStatus.connected) Color(0xFFE6F4EA)
                            else if (dashboardData.mcpStatus.tokenSaved) Color(0xFFFEF3C7)
                            else Color(0xFFF1F5F9),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.5.dp,
                            if (dashboardData.mcpStatus.connected) NeoTokens.Emerald
                            else if (dashboardData.mcpStatus.tokenSaved) NeoTokens.Amber
                            else NeoTokens.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (dashboardData.mcpStatus.tokenSaved) dashboardData.mcpStatus.statusText
                        else "Endpoint MCP belum tersimpan.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeoTokens.Black
                    )
                }

                Spacer(Modifier.height(8.dp))
                NeoProgressBar(
                    label = if (dashboardData.mcpStatus.connected) "Koneksi Aktif" else if (dashboardData.mcpStatus.tokenSaved) "Menghubungkan" else "Offline",
                    percentage = if (dashboardData.mcpStatus.connected) 100 else if (dashboardData.mcpStatus.tokenSaved) 60 else 0,
                    color = if (dashboardData.mcpStatus.connected) NeoTokens.Emerald else NeoTokens.Blue
                )

                Spacer(Modifier.height(12.dp))
                NeoTextField(
                    value = mcpInput,
                    onValueChange = { mcpInput = it },
                    label = "Endpoint MCP",
                    placeholder = "wss://api.xiaozhi.me/mcp/?token=..."
                )

                Spacer(Modifier.height(10.dp))
                NeoButton(
                    text = if (dashboardData.mcpStatus.tokenSaved) "UPDATE ENDPOINT" else "SIMPAN ENDPOINT",
                    onClick = { onSaveMcpToken(mcpInput) },
                    color = NeoTokens.Yellow,
                    textColor = NeoTokens.Black
                )

                if (dashboardData.mcpStatus.tokenSaved) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NeoButton(
                                text = "RECONNECT",
                                onClick = onReconnectMcp,
                                color = NeoTokens.Blue,
                                textColor = NeoTokens.White,
                                modifier = Modifier.height(44.dp)
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            NeoButton(
                                text = "HAPUS",
                                onClick = { showDeleteMcpConfirm = true },
                                color = NeoTokens.Coral,
                                textColor = NeoTokens.White,
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 6. Tambah Materi Form Card ──
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
                            text = "Tambah Materi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Manual atau hubungkan endpoint API realtime.",
                            fontSize = 14.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    IconButton(onClick = { showAddForm = !showAddForm }) {
                        Icon(
                            if (showAddForm) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Toggle Add Form"
                        )
                    }
                }

                AnimatedVisibility(visible = showAddForm) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        NeoTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = "Judul",
                            placeholder = "Contoh: Catatan Kuliah"
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Kategori",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeoTokens.Black
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            dashboardData.categories.forEach { cat ->
                                val isSelected = newCategory == cat.name
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) NeoTokens.Yellow else NeoTokens.White,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.5.dp,
                                            NeoTokens.Black,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { newCategory = cat.name }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoTokens.Black
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Toggle Live API
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { newIsLiveApi = !newIsLiveApi }
                        ) {
                            Checkbox(
                                checked = newIsLiveApi,
                                onCheckedChange = { newIsLiveApi = it }
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Sumber API Realtime (Live JSON)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Black
                            )
                        }

                        if (newIsLiveApi) {
                            Spacer(Modifier.height(6.dp))
                            NeoTextField(
                                value = newApiUrl,
                                onValueChange = { newApiUrl = it },
                                label = "Endpoint API URL",
                                placeholder = "https://api.example.com/data"
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        NeoTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            label = "Isi Data",
                            placeholder = "Tulis isi materi secara lengkap...",
                            modifier = Modifier.height(120.dp)
                        )

                        Spacer(Modifier.height(10.dp))
                        NeoTextField(
                            value = newKeywords,
                            onValueChange = { newKeywords = it },
                            label = "Kata Kunci",
                            placeholder = "Contoh: materi, iot, tugas"
                        )

                        Spacer(Modifier.height(14.dp))
                        NeoButton(
                            text = if (dashboardData.quota.materials.reached) "BATAS KUOTA TERCAPAI" else "SIMPAN MATERI",
                            onClick = {
                                if (newTitle.isNotBlank() && newContent.isNotBlank() && !dashboardData.quota.materials.reached) {
                                    onCreateMaterial(
                                        newTitle,
                                        newCategory.ifBlank { "Umum" },
                                        newContent,
                                        newKeywords,
                                        if (newIsLiveApi) newApiUrl else ""
                                    )
                                    newTitle = ""
                                    newContent = ""
                                    newKeywords = ""
                                    newApiUrl = ""
                                    newIsLiveApi = false
                                    showAddForm = false
                                }
                            },
                            color = if (dashboardData.quota.materials.reached) NeoTokens.Gray else NeoTokens.Emerald,
                            textColor = NeoTokens.White,
                            enabled = !dashboardData.quota.materials.reached && newTitle.isNotBlank() && newContent.isNotBlank()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 7. Database Materi Section ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Database Materi (${dashboardData.materials.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black
            )
        }

        Spacer(Modifier.height(8.dp))

        // Search bar
        NeoTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = "Cari data materi...",
            label = "Pencarian"
        )

        Spacer(Modifier.height(8.dp))

        // Category filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val allCats = listOf("Semua") + dashboardData.categories.map { it.name }
            allCats.forEach { cat ->
                val isSel = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (isSel) NeoTokens.Yellow else NeoTokens.White,
                            RoundedCornerShape(99.dp)
                        )
                        .border(
                            1.5.dp,
                            NeoTokens.Black,
                            RoundedCornerShape(99.dp)
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoTokens.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Materials List
        val filteredMaterials = dashboardData.materials.filter { m ->
            val matchCat = selectedCategoryFilter == "Semua" || m.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchQ = searchQuery.isBlank() ||
                m.title.contains(searchQuery, ignoreCase = true) ||
                m.content.contains(searchQuery, ignoreCase = true) ||
                m.keywords.contains(searchQuery, ignoreCase = true)
            matchCat && matchQ
        }

        if (filteredMaterials.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoTokens.White, RoundedCornerShape(12.dp))
                    .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada data materi yang sesuai.",
                    fontSize = 14.sp,
                    color = NeoTokens.Muted
                )
            }
        } else {
            filteredMaterials.forEach { mat ->
                MaterialCard(
                    material = mat,
                    onEdit = {
                        editingMaterial = mat
                        editTitle = mat.title
                        editCategory = mat.category
                        editContent = mat.content
                        editKeywords = mat.keywords
                        editApiUrl = mat.apiUrl
                    },
                    onDelete = { deletingMaterialId = mat.id }
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 8. Kategori Management Card ──
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column {
                Text(
                    text = "Kelola Kategori (${dashboardData.categories.size} item)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NeoTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = "Kategori Baru",
                            placeholder = "Nama kategori baru"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    NeoButton(
                        text = "TAMBAH",
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onCreateCategory(newCategoryName.trim())
                                newCategoryName = ""
                            }
                        },
                        color = NeoTokens.Yellow,
                        textColor = NeoTokens.Black,
                        modifier = Modifier.width(100.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Categories list with delete
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dashboardData.categories.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NeoTokens.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(1.dp, NeoTokens.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cat.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Black
                            )
                            if (!cat.name.trim().equals("data dari api", ignoreCase = true)) {
                                IconButton(
                                    onClick = { deletingCategoryId = cat.id },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = "Hapus Kategori",
                                        tint = NeoTokens.Coral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Permanent",
                                    fontSize = 14.sp,
                                    color = NeoTokens.Muted
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))
    }

    // ── Edit Material Dialog ──
    editingMaterial?.let { mat ->
        AlertDialog(
            onDismissRequest = { editingMaterial = null },
            title = {
                Text(
                    text = "Edit Materi",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = NeoTokens.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    NeoTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = "Judul"
                    )
                    Spacer(Modifier.height(8.dp))
                    NeoTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = "Kategori"
                    )
                    Spacer(Modifier.height(8.dp))
                    NeoTextField(
                        value = editApiUrl,
                        onValueChange = { editApiUrl = it },
                        label = "Endpoint API (Opsional)"
                    )
                    Spacer(Modifier.height(8.dp))
                    NeoTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = "Isi Data",
                        placeholder = "Tulis isi materi...",
                        modifier = Modifier.height(100.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    NeoTextField(
                        value = editKeywords,
                        onValueChange = { editKeywords = it },
                        label = "Kata Kunci"
                    )
                }
            },
            confirmButton = {
                NeoButton(
                    text = "SIMPAN",
                    onClick = {
                        onUpdateMaterial(
                            mat.id,
                            editTitle,
                            editCategory,
                            editContent,
                            editKeywords,
                            editApiUrl
                        )
                        editingMaterial = null
                    },
                    color = NeoTokens.Emerald,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { editingMaterial = null },
                    color = NeoTokens.Gray,
                    textColor = NeoTokens.Black
                )
            },
            containerColor = NeoTokens.Cream,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Delete Material Confirm ──
    deletingMaterialId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingMaterialId = null },
            title = { Text("Hapus Materi?", fontWeight = FontWeight.Black) },
            text = { Text("Materi ini akan dihapus permanen dari knowledge base Xiaozhi.") },
            confirmButton = {
                NeoButton(
                    text = "HAPUS",
                    onClick = {
                        onDeleteMaterial(id)
                        deletingMaterialId = null
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { deletingMaterialId = null },
                    color = NeoTokens.Gray,
                    textColor = NeoTokens.Black
                )
            }
        )
    }

    // ── Delete Category Confirm ──
    deletingCategoryId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingCategoryId = null },
            title = { Text("Hapus Kategori?", fontWeight = FontWeight.Black) },
            text = { Text("Kategori ini akan dihapus.") },
            confirmButton = {
                NeoButton(
                    text = "HAPUS",
                    onClick = {
                        onDeleteCategory(id)
                        deletingCategoryId = null
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { deletingCategoryId = null },
                    color = NeoTokens.Gray,
                    textColor = NeoTokens.Black
                )
            }
        )
    }

    // ── Delete MCP Confirm ──
    if (showDeleteMcpConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteMcpConfirm = false },
            title = { Text("Hapus Koneksi MCP?", fontWeight = FontWeight.Black) },
            text = { Text("Token MCP akan dihapus dan asisten suara Xiaozhi akan terputus.") },
            confirmButton = {
                NeoButton(
                    text = "YA, HAPUS",
                    onClick = {
                        onDeleteMcpToken()
                        showDeleteMcpConfirm = false
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { showDeleteMcpConfirm = false },
                    color = NeoTokens.Gray,
                    textColor = NeoTokens.Black
                )
            }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, bgColor: Color, modifier: Modifier = Modifier) {
    NeoCard(
        modifier = modifier,
        backgroundColor = bgColor,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeoTokens.Muted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black
            )
        }
    }
}

@Composable
private fun QuotaChip(label: String) {
    Box(
        modifier = Modifier
            .background(NeoTokens.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NeoTokens.Black
        )
    }
}

@Composable
private fun MaterialCard(
    material: XiaozhiMaterial,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoTokens.White,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NeoBadge(text = material.category, backgroundColor = NeoTokens.MintLight)
                    if (material.sourceType == "api_live") {
                        NeoBadge(text = "Realtime", backgroundColor = Color(0xFFE0F2FE))
                    }
                }
                Text(
                    text = "ID #${material.id}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Muted
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = material.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black
            )

            if (material.keywords.isNotBlank()) {
                Text(
                    text = "Kata Kunci: ${material.keywords}",
                    fontSize = 14.sp,
                    color = NeoTokens.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))

            if (material.sourceType == "api_live" && material.apiLabel.isNotBlank()) {
                Text(
                    text = "Endpoint: ${material.apiLabel}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Blue
                )
            }

            Text(
                text = material.content,
                fontSize = 14.sp,
                color = NeoTokens.Dark,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (material.content.length > 120) {
                Text(
                    text = if (isExpanded) "Tampilkan lebih sedikit" else "Baca selengkapnya...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Blue,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit", fontWeight = FontWeight.Bold, color = NeoTokens.Blue)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onDelete) {
                    Text("Hapus", fontWeight = FontWeight.Bold, color = NeoTokens.Coral)
                }
            }
        }
    }
}
