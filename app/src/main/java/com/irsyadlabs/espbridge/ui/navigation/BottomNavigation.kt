package com.irsyadlabs.espbridge.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.irsyadlabs.espbridge.ui.theme.BrandBlue
import com.irsyadlabs.espbridge.ui.theme.InkBlack
import com.irsyadlabs.espbridge.ui.theme.PaperWhite
import com.irsyadlabs.espbridge.ui.theme.SoftBlue

sealed class MainDestination(val route: String, val label: String) {
    data object Home : MainDestination("home", "Home")
    data object Setup : MainDestination("setup", "Setup")
    data object WifiConfig : MainDestination("wifi_config", "WiFi")
    data object Settings : MainDestination("settings", "Settings")
}

val mainDestinations = listOf(MainDestination.Home, MainDestination.Setup, MainDestination.WifiConfig, MainDestination.Settings)

@Composable
fun MainBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = PaperWhite) {
        mainDestinations.forEach { item ->
            val selected = currentRoute == item.route
            val icon = when (item) {
                MainDestination.Home -> Icons.Rounded.Home
                MainDestination.Setup -> Icons.Rounded.Tune
                MainDestination.WifiConfig -> Icons.Rounded.Wifi
                MainDestination.Settings -> Icons.Rounded.Settings
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandBlue,
                    selectedTextColor = InkBlack,
                    indicatorColor = SoftBlue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
