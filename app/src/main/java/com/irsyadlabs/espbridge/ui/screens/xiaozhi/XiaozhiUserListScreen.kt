package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.data.xiaozhi.XiaozhiAdminUserItem
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun XiaozhiUserListScreen(
    users: List<XiaozhiAdminUserItem>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }

    val filterOptions = listOf("Semua", "Board Aktif", "MCP Terhubung", "Sedang Play", "Belum Aktif")

    val filteredUsers = users.filter { user ->
        val matchesSearch = searchQuery.isBlank() ||
                user.username.contains(searchQuery, ignoreCase = true) ||
                user.deviceMac.contains(searchQuery, ignoreCase = true) ||
                user.deviceName.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "Board Aktif" -> user.deviceMac.isNotBlank()
            "MCP Terhubung" -> user.mcpStatus.connected
            "Sedang Play" -> user.isPlaying
            "Belum Aktif" -> !user.mcpStatus.connected && user.deviceMac.isBlank()
            else -> true
        }

        matchesSearch && matchesFilter
    }

    val totalUsers = users.size
    val totalBoardConnected = users.count { it.deviceMac.isNotBlank() }
    val totalMcpConnected = users.count { it.mcpStatus.connected }
    val totalStreaming = users.count { it.isPlaying }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ── 1. Screen Title & Refresh ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                NeoBadge(
                    text = "ADMIN PANEL",
                    backgroundColor = NeoTokens.Yellow,
                    textColor = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Daftar Pengguna",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Text(
                    text = "Kelola pengguna, pantau MAC ID hardware & status MCP realtime.",
                    fontSize = 13.sp,
                    color = NeoTokens.Muted
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(44.dp)
                    .background(NeoTokens.White, RoundedCornerShape(10.dp))
                    .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(10.dp))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = NeoTokens.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh Users",
                        tint = NeoTokens.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── 2. Real-time Metric Badges (Horizontal Scroll) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminStatPill(
                icon = Icons.Rounded.People,
                label = "Total User",
                value = "$totalUsers",
                color = NeoTokens.Yellow
            )
            AdminStatPill(
                icon = Icons.Rounded.DeveloperBoard,
                label = "Board MAC",
                value = "$totalBoardConnected",
                color = NeoTokens.MintLight
            )
            AdminStatPill(
                icon = Icons.Rounded.Bolt,
                label = "MCP Aktif",
                value = "$totalMcpConnected",
                color = NeoTokens.Cyan.copy(alpha = 0.35f)
            )
            AdminStatPill(
                icon = Icons.Rounded.MusicNote,
                label = "Streaming",
                value = "$totalStreaming",
                color = NeoTokens.Pink
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── 3. Search Bar ──
        NeoTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Pencarian Pengguna",
            placeholder = "Cari nama pengguna atau MAC ID...",
            leadingIcon = Icons.Rounded.Search,
            trailingIcon = if (searchQuery.isNotBlank()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Clear, "Clear Search", tint = NeoTokens.Black)
                    }
                }
            } else null
        )

        Spacer(Modifier.height(10.dp))

        // ── 4. Filter Chips Row ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { filter ->
                val isSelected = selectedFilter == filter
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) NeoTokens.Yellow else NeoTokens.White,
                            RoundedCornerShape(NeoTokens.PillCorner)
                        )
                        .border(
                            1.5.dp,
                            NeoTokens.Black,
                            RoundedCornerShape(NeoTokens.PillCorner)
                        )
                        .clickable { selectedFilter = filter }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = NeoTokens.Black
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── 5. User List ──
        if (isLoading && users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = NeoTokens.Emerald
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Memuat daftar pengguna...",
                        fontWeight = FontWeight.Bold,
                        color = NeoTokens.Dark
                    )
                }
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    backgroundColor = NeoTokens.White
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonOff,
                            contentDescription = null,
                            tint = NeoTokens.Muted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tidak Ada Pengguna Ditemukan",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeoTokens.Black
                        )
                        Text(
                            text = "Coba ubah kata kunci pencarian atau filter status.",
                            fontSize = 12.sp,
                            color = NeoTokens.Muted
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredUsers, key = { it.id }) { user ->
                    AdminUserCard(user)
                }
            }
        }
    }
}

@Composable
private fun AdminStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color, RoundedCornerShape(10.dp))
            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeoTokens.Black,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = NeoTokens.Black
            )
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = NeoTokens.Dark
            )
        }
    }
}

@Composable
private fun AdminUserCard(user: XiaozhiAdminUserItem) {
    NeoCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NeoTokens.White,
        contentPadding = PaddingValues(14.dp)
    ) {
        Column {
            // User Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (user.role == "admin") NeoTokens.Yellow else NeoTokens.MintLight,
                            CircleShape
                        )
                        .border(1.5.dp, NeoTokens.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.username.take(1).uppercase().ifBlank { "U" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoTokens.Black
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.username,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeoTokens.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        NeoBadge(
                            text = user.role.uppercase(),
                            backgroundColor = if (user.role == "admin") NeoTokens.Yellow else NeoTokens.MintLight,
                            textColor = NeoTokens.Black
                        )
                    }

                    if (user.createdAt.isNotBlank()) {
                        Text(
                            text = "Terdaftar: ${user.createdAt.take(10)}",
                            fontSize = 11.sp,
                            color = NeoTokens.Muted
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Hardware Board Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (user.deviceMac.isNotBlank()) NeoTokens.MintLight else NeoTokens.Gray.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(1.2.dp, NeoTokens.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeveloperBoard,
                            contentDescription = "Board ESP32",
                            tint = if (user.deviceMac.isNotBlank()) NeoTokens.Emerald else NeoTokens.Dark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "HARDWARE BOARD ESP32",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Muted
                            )
                            Text(
                                text = if (user.deviceMac.isNotBlank()) user.deviceMac else "Belum Terhubung",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = if (user.deviceMac.isNotBlank()) NeoTokens.Black else NeoTokens.Dark
                            )
                        }
                    }

                    NeoBadge(
                        text = if (user.deviceMac.isNotBlank()) "AKTIF" else "BELUM ADA",
                        backgroundColor = if (user.deviceMac.isNotBlank()) NeoTokens.Emerald else NeoTokens.Yellow,
                        textColor = if (user.deviceMac.isNotBlank()) NeoTokens.White else NeoTokens.Black
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // MCP Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (user.mcpStatus.connected) NeoTokens.Emerald else NeoTokens.Muted,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (user.mcpStatus.connected) "MCP WebSocket Terhubung" else "MCP Belum Aktif",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (user.mcpStatus.connected) NeoTokens.Emerald else NeoTokens.Muted
                    )
                }

                if (user.deviceName.isNotBlank()) {
                    Text(
                        text = user.deviceName,
                        fontSize = 11.sp,
                        color = NeoTokens.Muted
                    )
                }
            }

            // Live YouTube Music Streaming Card
            if (user.isPlaying) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NeoTokens.Pink.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .border(1.2.dp, NeoTokens.Coral, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeoPulseIndicator(active = true)
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "Playing Music",
                            tint = NeoTokens.Coral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SEDANG STREAMING MUSIK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Coral
                            )
                            Text(
                                text = user.currentTrack.ifBlank { "YouTube Audio Stream" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeoTokens.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
