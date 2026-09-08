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
    onLogout: () -> Unit
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
                            Icon(Icons.Rounded.AccountCircle, null, tint = SketchTeal, modifier = Modifier.size(32.dp))
                        }
                        Column(Modifier.padding(start = 20.dp).weight(1f)) {
                            Text(state.email ?: "Authorized User", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                            Text(
                                if (state.firebaseReady) "Cloud Sync Protocol" else "Local Identity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SketchMuted
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Access & Health") }
            item {
                PlayfulCard(innerPadding = PaddingValues(0.dp)) {
                    Column {
                        PermissionRow(Icons.Rounded.Notifications, "Notifications", PermissionUtils.notificationListenerEnabled(context)) {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = SketchBorder.copy(alpha = 0.1f))
                        PermissionRow(Icons.Rounded.Bluetooth, "Nearby Devices", PermissionUtils.bluetoothGranted(context)) {
                            openAppSettings(context)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = SketchBorder.copy(alpha = 0.1f))
                        PermissionRow(Icons.Rounded.LocationOn, "Location Service", PermissionUtils.locationGranted(context)) {
                            openAppSettings(context)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = SketchBorder.copy(alpha = 0.1f))
                        PermissionRow(Icons.Rounded.PhoneAndroid, "Phone Status", PermissionUtils.phoneStateGranted(context)) {
                            openAppSettings(context)
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 20.dp), color = SketchBorder.copy(alpha = 0.1f))
                        PermissionRow(Icons.Rounded.BatterySaver, "Background Sync", PermissionUtils.backgroundSyncGranted(context)) {
                            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                        }
                    }
                }
            }

            item { SectionHeader("Interface Preferences") }
            item {
                PlayfulCard {
                    Column {
                        Text("Active Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppTheme.entries.forEach { theme ->
                                ThemeChip(
                                    label = theme.label,
                                    selected = state.settings.appTheme == theme,
                                    onClick = { onAppTheme(theme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        ToggleRow(
                            title = "Cloud State Sync",
                            subtitle = "Sync latest status to secure cloud API.",
                            checked = state.settings.cloudSyncEnabled,
                            onChecked = onCloudSync
                        )
                    }
                }
            }

            item { SectionHeader("Xichi Mode — Firebase") }
            item {
                PlayfulCard {
                    Column {
                        Text("Notification Reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                        Text(
                            "Xichi mode membaca notifikasi HP dari Firebase secara otomatis. Secret per-device di-generate otomatis oleh app dan dikirim ke ESP32 via BLE saat mode Xichi aktif. Tidak perlu input manual.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SketchMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item { SectionHeader("Background Service") }
            item {
                PlayfulCard {
                    Column {
                        ToggleRow("Auto Handshake", "Reconnect when device is detected.", state.settings.autoConnect, onAutoConnect)
                        Spacer(Modifier.height(16.dp))
                        ToggleRow("Background Sync", "Forward data when app is closed.", state.settings.keepBackgroundConnection, onBackground)
                    }
                }
            }

            item { SectionHeader("Firmware Control") }
            item {
                FirmwareUpdateCard(
                    state = state,
                    onCheck = onCheckFirmware,
                    onInstall = onInstallFirmware
                )
            }

            item {
                PlayfulCard(background = SketchPink.copy(alpha = 0.05f)) {
                    Column {
                        Text("Identity Authorization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = BrandRed)
                        Text(
                            "Terminating the session will halt device synchronization.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SketchMuted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(UiTokens.SmallRadius),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRed),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandRed.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Rounded.Logout, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("LOGOUT CHRONCHI", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(UiTokens.BottomBarHeight)) }
        }
    }
}

@Composable
private fun FirmwareUpdateCard(
    state: MainUiState,
    onCheck: () -> Unit,
    onInstall: () -> Unit
) {
    val update = state.firmwareUpdate
    val device = state.connectedDevice
    
    PlayfulCard(background = SketchSurface) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(SketchTeal.copy(alpha = 0.1f), CircleShape).border(1.2.dp, SketchTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.SystemUpdate, null, tint = SketchTeal, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.padding(start = 16.dp).weight(1f)) {
                    Text("Firmware Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    if (device != null) {
                        Text("v${device.firmwareVersion} detected", style = MaterialTheme.typography.bodySmall, color = SketchMuted)
                    }
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            if (update is FirmwareUpdateState.Available) {
                PrimaryActionButton(
                    text = "INSTALL V${update.manifest.version}",
                    onClick = onInstall
                )
            } else {
                OutlinedButton(
                    onClick = onCheck,
                    enabled = device != null && update !is FirmwareUpdateState.Checking,
                    modifier = Modifier.fillMaxWidth().height(UiTokens.PrimaryButtonHeight),
                    shape = RoundedCornerShape(UiTokens.SmallRadius),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SketchBorder)
                ) {
                    Text("CHECK PROTOCOL UPDATES", fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, granted: Boolean, onFix: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (granted) SuccessGreen else SketchPeach, modifier = Modifier.size(24.dp))
        Text(label, modifier = Modifier.weight(1f).padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyLarge, color = SketchBorder, fontWeight = FontWeight.Bold)
        if (granted) {
            Icon(Icons.Rounded.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
        } else {
            TextButton(onClick = onFix, contentPadding = PaddingValues(0.dp)) {
                Text("ENABLE", color = SketchPeach, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.ExtraBold) },
        modifier = modifier,
        shape = RoundedCornerShape(UiTokens.SmallRadius),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SketchTeal,
            selectedLabelColor = Color.White,
            containerColor = Color.White,
            labelColor = SketchMuted
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = SketchBorder.copy(alpha = 0.3f),
            selectedBorderColor = SketchBorder,
            borderWidth = 1.2.dp
        )
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = SketchMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
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

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    context.startActivity(intent)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    ChronchiTheme {
        SettingsScreen(
            state = MainUiState(
                email = "user@example.com",
                firebaseReady = true,
                settings = LocalSettings(autoConnect = true, keepBackgroundConnection = true)
            ),
            onAutoConnect = {},
            onCloudSync = {},
            onBackground = {},
            onCheckFirmware = {},
            onInstallFirmware = {},
            onAppTheme = {},
            onLogout = {}
        )
    }
}
