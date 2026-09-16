package com.irsyadlabs.espbridge.ui.screens.mode

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.irsyadlabs.espbridge.ui.components.NeoBadge
import com.irsyadlabs.espbridge.ui.components.NeoButton
import com.irsyadlabs.espbridge.ui.components.NeoCard
import com.irsyadlabs.espbridge.ui.theme.NeoTokens

@Composable
fun ModeSelectionScreen(
    onSelectChronchi: () -> Unit,
    onSelectXiaozhi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoTokens.Cream)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        
        NeoBadge(text = "MULTI-ECOSYSTEM HUB", backgroundColor = NeoTokens.Yellow)
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "PILIH MODE APLIKASI",
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = NeoTokens.Black,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Silakan tentukan sistem yang ingin Anda hubungkan. Pilihan ini dapat diganti kapan saja melalui menu Profile.",
            fontSize = 14.sp,
            color = NeoTokens.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 28.dp)
        )

        // Card 1: Chronchi BLE
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoBadge(text = "ESP32 COMPANION", backgroundColor = NeoTokens.Cyan)
                    Icon(Icons.Rounded.Bluetooth, null, tint = NeoTokens.Cyan, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Chronchi BLE Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Jembatan Bluetooth Low Energy untuk perangkat keras ESP32. Sinkronisasi notifikasi telepon, cuaca, dan update firmware.",
                    fontSize = 13.sp,
                    color = NeoTokens.Muted
                )
                Spacer(Modifier.height(20.dp))
                NeoButton(
                    text = "MASUK KE CHRONCHI BLE",
                    onClick = onSelectChronchi,
                    color = NeoTokens.Cyan,
                    textColor = NeoTokens.Black
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Card 2: Xiaozhi AI
        NeoCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NeoTokens.White,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoBadge(text = "AI IOT PLATFORM", backgroundColor = NeoTokens.Mint)
                    Icon(Icons.Rounded.Psychology, null, tint = NeoTokens.Emerald, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Xiaozhi AI Mode",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = NeoTokens.Black
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Asisten suara AI cerdas dengan protokol WebSocket MCP Tools, Persona RAG otomatis (1-100%), Smart Home relay, dan riwayat dialog.",
                    fontSize = 13.sp,
                    color = NeoTokens.Muted
                )
                Spacer(Modifier.height(20.dp))
                NeoButton(
                    text = "MASUK KE XIAOZHI AI",
                    onClick = onSelectXiaozhi,
                    color = NeoTokens.Emerald,
                    textColor = NeoTokens.White
                )
            }
        }
        
        Spacer(Modifier.height(48.dp))
    }
}
