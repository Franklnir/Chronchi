package com.irsyadlabs.espbridge.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.ui.theme.*

sealed class MainDestination(val route: String, val label: String) {
    data object Home : MainDestination("home", "Home")
    data object Setup : MainDestination("setup", "Setup")
    data object WifiConfig : MainDestination("wifi_config", "WiFi")
    data object Settings : MainDestination("settings", "Settings")
}

val mainDestinations = listOf(
    MainDestination.Home,
    MainDestination.Setup,
    MainDestination.WifiConfig,
    MainDestination.Settings
)

sealed class XiaozhiDestination(val route: String, val label: String) {
    data object Dashboard : XiaozhiDestination("xiaozhi_dashboard", "Dashboard")
    data object ChatHistory : XiaozhiDestination("xiaozhi_chat", "Riwayat")
    data object Flasher : XiaozhiDestination("xiaozhi_flasher", "Flasher")
    data object UserList : XiaozhiDestination("xiaozhi_users", "Daftar User")
    data object Profile : XiaozhiDestination("xiaozhi_profile", "Profil")
}

val xiaozhiDestinations = listOf(
    XiaozhiDestination.Dashboard,
    XiaozhiDestination.ChatHistory,
    XiaozhiDestination.Flasher,
    XiaozhiDestination.UserList,
    XiaozhiDestination.Profile
)

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

@Composable
fun XiaozhiBottomBar(
    currentRoute: String?,
    isAdmin: Boolean = false,
    onNavigate: (String) -> Unit
) {
    val items = remember(isAdmin) {
        if (isAdmin) {
            listOf(
                XiaozhiDestination.Dashboard,
                XiaozhiDestination.ChatHistory,
                XiaozhiDestination.Flasher,
                XiaozhiDestination.UserList,
                XiaozhiDestination.Profile
            )
        } else {
            listOf(
                XiaozhiDestination.Dashboard,
                XiaozhiDestination.ChatHistory,
                XiaozhiDestination.Flasher,
                XiaozhiDestination.Profile
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeoTokens.Cream)
    ) {
        // Neo-Brutalist Border Top
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = NeoTokens.BorderWidth, color = NeoTokens.Black),
            containerColor = NeoTokens.White
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                val icon = when (item) {
                    XiaozhiDestination.Dashboard -> Icons.Rounded.Dashboard
                    XiaozhiDestination.ChatHistory -> Icons.Rounded.Forum
                    XiaozhiDestination.Flasher -> Icons.Rounded.Memory
                    XiaozhiDestination.UserList -> Icons.Rounded.People
                    XiaozhiDestination.Profile -> Icons.Rounded.Person
                    else -> Icons.Rounded.Circle
                }
                NavigationBarItem(
                    selected = selected,
                    onClick = { onNavigate(item.route) },
                    icon = {
                        Icon(
                            icon,
                            contentDescription = item.label,
                            tint = if (selected) NeoTokens.Black else NeoTokens.Muted
                        )
                    },
                    label = {
                        Text(
                            item.label,
                            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            color = if (selected) NeoTokens.Black else NeoTokens.Muted
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeoTokens.Black,
                        selectedTextColor = NeoTokens.Black,
                        indicatorColor = NeoTokens.Yellow,
                        unselectedIconColor = NeoTokens.Muted,
                        unselectedTextColor = NeoTokens.Muted
                    )
                )
            }
        }
    }
}
