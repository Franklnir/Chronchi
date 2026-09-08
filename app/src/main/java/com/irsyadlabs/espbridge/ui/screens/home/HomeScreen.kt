package com.irsyadlabs.espbridge.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.*
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.preview.previewUiState
import com.irsyadlabs.espbridge.ui.theme.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: MainUiState,
    onToggleSource: (String, Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    LaunchedEffect(Unit) { onRefresh() }
    val isPreview = LocalInspectionMode.current
    val resources = LocalContext.current.resources
    var iconBitmaps by remember { mutableStateOf(AppIconCache.snapshot()) }
    LaunchedEffect(resources) {
        iconBitmaps = AppIconCache.preload(resources, SupportedSources.all.map(AppSource::id))
    }
    val now = remember(state.phoneState.phoneStatus.updatedAt) {
        if (isPreview) {
            LocalDateTime.of(2026, 8, 26, 18, 41)
        } else {
            LocalDateTime.now()
        }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID")) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val sourcesByCategory = remember {
        SourceCategory.entries.associateWith { category ->
            SupportedSources.all.filter { it.category == category }
        }
    }
    val selectedSourceIds = state.settings.selectedSourceIds

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            item(key = "home_header") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Chronchi",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Bridge Control Center",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                    // Illustrative Notification Dot
                    Box(
                        Modifier.size(48.dp).background(Color.White, CircleShape).border(1.5.dp, SketchBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Notifications, null, tint = SketchBorder, modifier = Modifier.size(24.dp))
                    }
                }
            }

            item(key = "phone_summary") {
                val theme = LocalAppTheme.current
                val summaryBg = if (theme == AppTheme.COMIC) ComicPink else SketchTeal

                PlayfulCard(
                    background = summaryBg,
                    modifier = Modifier.fillMaxWidth(),
                    innerPadding = PaddingValues(0.dp) // Summary doesn't need inner border padding
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(
                                    timeFormatter.format(now),
                                    color = Color.White,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    dateFormatter.format(now),
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(UiTokens.InnerRadius))
                                    .border(1.2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(UiTokens.InnerRadius))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                val weather = state.phoneState.weather
                                Text(
                                    if (weather.temperatureC != null) "☁ ${weather.temperatureC.toInt()}°C" else "☁ --°C",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusItem(Icons.Rounded.SignalCellularAlt, networkText(state), Color.White)
                            Box(Modifier.width(1.2.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                            StatusItem(
                                if (state.phoneState.phoneStatus.charging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryFull,
                                "${state.phoneState.phoneStatus.batteryLevel}%",
                                Color.White
                            )
                            Box(Modifier.width(1.2.dp).height(20.dp).background(Color.White.copy(alpha = 0.3f)))
                            StatusItem(
                                Icons.Rounded.Bluetooth,
                                if (state.bleState == ConnectionState.CONNECTED) "Connected" else "--",
                                Color.White
                            )
                        }
                    }
                }
            }

            SourceCategory.entries.forEach { category ->
                val sources = sourcesByCategory.getValue(category)
                if (sources.isNotEmpty()) {
                    item(key = "section_${category.name}") {
                        SectionHeader(title = category.title)
                    }
                    items(sources, key = { it.id }) { source ->
                        SourceListItem(
                            source = source,
                            iconBitmap = bundledAppIcon(source.id)?.let(iconBitmaps::get),
                            enabled = source.id in selectedSourceIds,
                            onToggle = onToggleSource
                        )
                    }
                }
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(UiTokens.BottomBarHeight)) }
        }
    }
}

@Composable
private fun SourceListItem(
    source: AppSource,
    iconBitmap: ImageBitmap?,
    enabled: Boolean,
    onToggle: (String, Boolean) -> Unit
) {
    val onCheckedChange = remember(source.id, onToggle) {
        { checked: Boolean -> onToggle(source.id, checked) }
    }
    
    PlayfulCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        background = if (enabled) Color.White else SketchBg,
        innerPadding = PaddingValues(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Avatar style icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(if (enabled) SketchTeal.copy(alpha = 0.1f) else Color.White, CircleShape)
                    .border(1.2.dp, SketchBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SourceIcon(source = source, bitmap = iconBitmap)
            }
            
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(
                    source.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    if (enabled) "Protocol Active" else "Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) SketchTeal else SketchMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SketchTeal,
                    uncheckedThumbColor = SketchMuted,
                    uncheckedTrackColor = SketchBg,
                    uncheckedBorderColor = SketchBorder.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun StatusItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(value, color = color, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
    }
}

private fun networkText(state: MainUiState): String {
    val p = state.phoneState.phoneStatus
    return when (p.networkTransport) {
        "wifi" -> "WiFi ${"▮".repeat(p.signalLevel.coerceIn(0, 4))}"
        "cellular" -> "${p.networkGeneration.ifBlank { "Cell" }} ${"▮".repeat(p.signalLevel.coerceIn(0, 4))}"
        else -> "OFF"
    }
}

@Preview(name = "Home", showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    ChronchiTheme {
        HomeScreen(
            state = previewUiState,
            onToggleSource = { _, _ -> },
            onRefresh = {}
        )
    }
}
