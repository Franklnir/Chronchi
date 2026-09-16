package com.irsyadlabs.espbridge.ui.screens.xiaozhi

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.data.xiaozhi.*
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun XiaozhiChatHistoryScreen(
    chatData: XiaozhiChatHistoryData,
    isLoading: Boolean = false,
    onSearch: (query: String, date: String) -> Unit,
    onClearHistory: () -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filteredMessages = remember(chatData.items, searchQuery, selectedDate) {
        chatData.items.filter { item ->
            val matchDate = selectedDate.isBlank() || item.createdAt.startsWith(selectedDate)
            val matchQuery = searchQuery.isBlank() ||
                (item.userMessage?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.xiaozhiAnswer?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.toolName?.contains(searchQuery, ignoreCase = true) == true)
            matchDate && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 16.dp)
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
                    text = "Riwayat Xiaozhi",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoTokens.Muted
                )
                Text(
                    text = "Riwayat Chat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Text(
                    text = "Percakapan terbaru dari MCP. Filter per hari atau cari kata kunci.",
                    fontSize = 12.sp,
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
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = NeoTokens.Black)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Status Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    if (chatData.mcpStatus.connected) NeoTokens.MintLight else NeoTokens.Gray.copy(alpha = 0.4f),
                    RoundedCornerShape(99.dp)
                )
                .border(2.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            NeoPulseIndicator(active = chatData.mcpStatus.connected)
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (chatData.mcpStatus.connected) "MCP Terhubung" else "MCP Belum Aktif",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeoTokens.Dark
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── 2. Stat Grid (4 Cards in 2x2) ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChatStatCard(
                title = "Total Chat",
                value = "${chatData.total}",
                bgColor = NeoTokens.White,
                modifier = Modifier.weight(1f)
            )
            ChatStatCard(
                title = "Ditampilkan",
                value = "${filteredMessages.size}",
                bgColor = NeoTokens.MintLight,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChatStatCard(
                title = "Hari Aktif",
                value = "${chatData.dateList.size}",
                bgColor = Color(0xFFFEF3C7),
                modifier = Modifier.weight(1f)
            )
            ChatStatCard(
                title = "Filter",
                value = if (selectedDate.isNotBlank()) formatRelDate(selectedDate) else "Semua",
                bgColor = NeoTokens.Lavender,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── 3. Search & Filter Bar ──
        NeoTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                onSearch(it, selectedDate)
            },
            placeholder = "Cari chat percakapan...",
            label = "Pencarian Chat"
        )

        Spacer(Modifier.height(8.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                NeoButton(
                    text = "SEMUA HARI",
                    onClick = {
                        selectedDate = ""
                        searchQuery = ""
                        onSearch("", "")
                    },
                    color = if (selectedDate.isBlank()) NeoTokens.Yellow else NeoTokens.White,
                    textColor = NeoTokens.Black,
                    modifier = Modifier.height(40.dp)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                NeoButton(
                    text = "HAPUS SEMUA",
                    onClick = { showClearConfirm = true },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White,
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 4. Date Filter Pills (Horizontal Scroll) ──
        if (chatData.dateList.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // All pill
                DateFilterChip(
                    label = "Semua (${chatData.total})",
                    isSelected = selectedDate.isBlank(),
                    onClick = {
                        selectedDate = ""
                        onSearch(searchQuery, "")
                    }
                )

                chatData.dateList.forEach { dg ->
                    val isSel = selectedDate == dg.date
                    DateFilterChip(
                        label = "${formatRelDate(dg.date)} (${dg.count})",
                        isSelected = isSel,
                        onClick = {
                            selectedDate = dg.date
                            onSearch(searchQuery, dg.date)
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // ── 5. Chat Thread LazyColumn ──
        if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp)
                    .background(NeoTokens.White, RoundedCornerShape(16.dp))
                    .border(NeoTokens.BorderWidth, NeoTokens.Black, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💬", fontSize = 36.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Belum ada riwayat chat.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoTokens.Dark
                    )
                    Text(
                        text = "Mulai mengobrol dengan Xiaozhi di ESP32 Anda.",
                        fontSize = 12.sp,
                        color = NeoTokens.Muted
                    )
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
                items(filteredMessages, key = { it.id ?: it.hashCode() }) { msg ->
                    ChatTurnItem(item = msg)
                }
            }
        }
    }

    // Clear History Confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Hapus Semua Riwayat?", fontWeight = FontWeight.Black) },
            text = { Text("Semua rekaman riwayat obrolan Xiaozhi akan dihapus secara permanen.") },
            confirmButton = {
                NeoButton(
                    text = "YA, HAPUS",
                    onClick = {
                        onClearHistory()
                        showClearConfirm = false
                    },
                    color = NeoTokens.Coral,
                    textColor = NeoTokens.White
                )
            },
            dismissButton = {
                NeoButton(
                    text = "BATAL",
                    onClick = { showClearConfirm = false },
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
private fun ChatStatCard(title: String, value: String, bgColor: Color, modifier: Modifier = Modifier) {
    NeoCard(
        modifier = modifier,
        backgroundColor = bgColor,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = NeoTokens.Muted
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = NeoTokens.Black,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DateFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) NeoTokens.Yellow else NeoTokens.White,
                RoundedCornerShape(99.dp)
            )
            .border(1.5.dp, NeoTokens.Black, RoundedCornerShape(99.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NeoTokens.Black
        )
    }
}

@Composable
private fun ChatTurnItem(item: XiaozhiChatMessage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User Message Bubble
        item.userMessage?.let { uMsg ->
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 24.dp),
                backgroundColor = Color(0xFFE0F2FE), // Soft sky-blue
                contentPadding = PaddingValues(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "User",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = NeoTokens.Blue
                        )
                        Text(
                            text = formatTime(item.createdAt),
                            fontSize = 10.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = uMsg,
                        fontSize = 13.sp,
                        color = NeoTokens.Black,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // AI Message Bubble
        val aiAnswer = item.xiaozhiAnswer ?: item.responsePayload
        if (!aiAnswer.isNullOrBlank()) {
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                backgroundColor = NeoTokens.White,
                contentPadding = PaddingValues(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🤖", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (item.toolName != null) "Xiaozhi AI (${item.toolName})" else "Xiaozhi AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoTokens.Emerald
                            )
                        }
                        Text(
                            text = formatTime(item.createdAt),
                            fontSize = 10.sp,
                            color = NeoTokens.Muted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = aiAnswer,
                        fontSize = 13.sp,
                        color = NeoTokens.Dark,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Content generated by AI",
                        fontSize = 9.sp,
                        color = NeoTokens.Muted,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } else if (item.toolName != null) {
            // Tool call record
            NeoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp),
                backgroundColor = NeoTokens.MintLight,
                contentPadding = PaddingValues(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tool Call: ${item.toolName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeoTokens.Dark
                    )
                    Text(
                        text = formatTime(item.createdAt),
                        fontSize = 10.sp,
                        color = NeoTokens.Muted
                    )
                }
            }
        }
    }
}

private fun formatTime(iso: String): String {
    if (iso.length < 16) return iso
    return iso.substring(11, 16)
}

private fun formatRelDate(dateStr: String): String {
    if (dateStr.length < 10) return dateStr
    return dateStr.substring(5) // e.g. "09-16"
}
