package com.irsyadlabs.espbridge.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.MainUiState
import com.irsyadlabs.espbridge.core.model.AndroidFirmwareUpdatePolicy
import com.irsyadlabs.espbridge.core.model.AppTheme
import com.irsyadlabs.espbridge.core.util.PermissionUtils
import com.irsyadlabs.espbridge.data.local.LocalSettings
import com.irsyadlabs.espbridge.data.firmware.FirmwareUpdateState
import com.irsyadlabs.espbridge.ui.components.*
import com.irsyadlabs.espbridge.ui.theme.*

@Composable
fun SettingsScreen(
    state: MainUiState,
    onAutoConnect: (Boolean) -> Unit,
    onCloudSync: (Boolean) -> Unit,
    onBackground: (Boolean) -> Unit,
    onAppTheme: (AppTheme) -> Unit,
    onCheckFirmware: () -> Unit,
    onInstallFirmware: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToXiaozhi: () -> Unit = {}
) {
    val context = LocalContext.current
    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(UiTokens.SectionSpacing)
        ) {
            item {
                SectionHeader("Settings", "Account & Device Authorization")
            }

            // Neo-Brutal Mode Switcher to Xiaozhi AI
            item {
                NeoCard(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = NeoTokens.MintLight,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MODE EKOSISTEM",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = NeoTokens.Black
                            )
                            NeoBadge(text = "CHRONCHI BLE", backgroundColor = NeoTokens.Cyan)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Beralih ke Xiaozhi AI untuk menggunakan asisten suara AI, Smart Home, dan analisis Persona RAG.",
                            fontSize = 12.sp,
                            color = NeoTokens.Dark
                        )
                        Spacer(Modifier.height(12.dp))
                        NeoButton(
                            text = "🔄 BERALIH KE MODE XIAOZHI AI",
                            onClick = onSwitchToXiaozhi,
                            color = NeoTokens.Emerald,
                            textColor = NeoTokens.White,
                            modifier = Modifier.height(44.dp)
                        )
                    }
                }
            }

            item {
                PlayfulCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(60.dp).background(SketchTeal.copy(alpha = 0.1f), CircleShape).border(1.2.dp, SketchBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Person, null, tint = SketchTeal, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(state.email ?: "Authorized Operator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                            Text(
                                if (state.signedIn) "Active Session" else "Local Session",
                                style = MaterialTheme.typography.bodySmall,
                                color = SketchMuted
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Rounded.Logout, "Logout", tint = SketchRed)
                        }
                    }
                }
            }

            item {
                SectionHeader("Bridge Configuration", "Automation and synchronization")
            }

            item {
                PlayfulCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ToggleRow(
                            title = "Auto Connect",
                            subtitle = "Connect to device when in proximity",
                            checked = state.settings.autoConnect,
                            onCheckedChange = onAutoConnect
                        )
                        HorizontalDivider(color = SketchBorder.copy(alpha = 0.1f))
                        ToggleRow(
                            title = "Background Bridge",
                            subtitle = "Maintain connection via foreground service",
                            checked = state.settings.keepBackgroundConnection,
                            onCheckedChange = onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = SketchBorder)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SketchMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SketchTeal,
                uncheckedThumbColor = SketchMuted,
                uncheckedTrackColor = SketchBg
            )
        )
    }
}
