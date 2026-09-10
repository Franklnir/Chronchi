package com.irsyadlabs.espbridge.ui.screens.onboarding

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.irsyadlabs.espbridge.core.util.PermissionUtils
import com.irsyadlabs.espbridge.ui.components.MainScreenColumn
import com.irsyadlabs.espbridge.ui.components.PlayfulCard
import com.irsyadlabs.espbridge.ui.components.PrimaryActionButton
import com.irsyadlabs.espbridge.ui.components.UiTokens
import com.irsyadlabs.espbridge.ui.theme.*

private data class PermissionStep(
    val title: String,
    val description: String,
    val bullets: List<String>,
    val optional: Boolean = false
)

@Composable
fun PermissionOnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var step by remember { mutableIntStateOf(0) }
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val steps = remember {
        listOf(
            PermissionStep(
                "Notification Access",
                "Forward notifications only from apps you enable to your device.",
                listOf("Secure transmission", "Messaging, Social, Finance & more", "Real-time protocol execution")
            ),
            PermissionStep(
                "Nearby Devices",
                "Scan, connect and automatically reconnect to your hardware over Bluetooth LE.",
                listOf("Works without internet", "Direct phone-to-hardware sync", "Privacy focused link")
            ),
            PermissionStep(
                "Location Services",
                "Used for hardware-level GPS, weather and local context features.",
                listOf("Latest state only", "No history storage", "Optional for bridge link"),
                optional = true
            ),
            PermissionStep(
                "Phone Status",
                "Allows cellular signal and battery status for the hardware OLED.",
                listOf("System health monitoring", "Real-time state broadcast"),
                optional = true
            ),
            PermissionStep(
                "Background Pulse",
                "Keep the secure bridge healthy even when the app is minimized.",
                listOf("Uninterrupted sync", "Auto handshake on return", "Optimized power usage"),
                optional = true
            )
        )
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshKey++
        if (step < steps.lastIndex) step++
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshKey++
        if (step < steps.lastIndex) step++
    }
    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshKey++
        if (step < steps.lastIndex) step++
    }

    // Force status checks to recompute after launchers return.
    @Suppress("UNUSED_VARIABLE") val ignored = refreshKey
    val current = steps[step]
    val granted = when (step) {
        0 -> PermissionUtils.notificationListenerEnabled(context)
        1 -> PermissionUtils.bluetoothGranted(context) && PermissionUtils.appNotificationGranted(context)
        2 -> PermissionUtils.locationGranted(context)
        3 -> PermissionUtils.phoneStateGranted(context)
        else -> false
    }

    MainScreenColumn(
        modifier = Modifier.padding(horizontal = UiTokens.HorizontalPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ACCESS PROTOCOL", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
                Text("${step + 1} / ${steps.size}", color = SketchTeal, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        when (step) {
                            0 -> SketchYellow
                            1 -> SketchTeal
                            2 -> SketchPeach
                            3 -> SketchYellow
                            else -> SketchTeal
                        },
                        RoundedCornerShape(UiTokens.CardRadius)
                    )
                    .border(1.5.dp, SketchBorder, RoundedCornerShape(UiTokens.CardRadius)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (step) {
                    0 -> Icons.Rounded.Notifications
                    1 -> Icons.Rounded.Bluetooth
                    2 -> Icons.Rounded.LocationOn
                    3 -> Icons.Rounded.PhoneAndroid
                    else -> Icons.Rounded.BatterySaver
                }
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(52.dp))
            }
            
            Spacer(Modifier.height(32.dp))
            Text(current.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = SketchBorder)
            Text(
                current.description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                color = SketchMuted,
                textAlign = TextAlign.Center
            )
            
            PlayfulCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    current.bullets.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = SketchBorder) }
                    Text(
                        if (granted) "HANDSHAKE READY" else "AWAITING AUTHORIZATION",
                        color = if (granted) SketchTeal else SketchRed,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            PrimaryActionButton(
                text = when {
                    granted && step == steps.lastIndex -> "INITIALIZE HUB"
                    granted -> "PROCEED"
                    step == 0 -> "ENABLE NOTIFICATIONS"
                    step == 1 -> "ALLOW BLUETOOTH"
                    step == 2 -> "ALLOW LOCATION"
                    step == 3 -> "ALLOW STATUS"
                    else -> "OPEN BACKGROUND SYNC"
                },
                onClick = {
                    if (granted) {
                        if (step == steps.lastIndex) onComplete() else step++
                        return@PrimaryActionButton
                    }
                    when (step) {
                        0 -> context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        1 -> {
                            val permissions = buildList {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    add(Manifest.permission.BLUETOOTH_SCAN)
                                    add(Manifest.permission.BLUETOOTH_CONNECT)
                                } else {
                                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            bluetoothLauncher.launch(permissions)
                        }
                        2 -> locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        3 -> phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                        4 -> {
                            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
                            onComplete()
                        }
                    }
                }
            )
            if (current.optional) {
                TextButton(onClick = { if (step == steps.lastIndex) onComplete() else step++ }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Skip for now", color = SketchMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Preview(name = "Permission Setup", showBackground = true, showSystemUi = true)
@Composable
private fun PermissionOnboardingScreenPreview() {
    ChronchiTheme {
        PermissionOnboardingScreen(onComplete = {})
    }
}
