package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LudoModeSelectScreen(navController: NavController) {
    var mode by remember { mutableStateOf(LudoMode.CLASSIC) }
    var players by remember { mutableStateOf(4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1a1a3d))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text("LUDO", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { players = 2 }
            ) {
                AsyncImage(model = MODE_2P_ICON, contentDescription = "1 on 1", modifier = Modifier.size(56.dp))
                Text("1 ON 1", color = Color.White)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { players = 4 }
            ) {
                AsyncImage(model = MODE_4P_ICON, contentDescription = "4 players", modifier = Modifier.size(56.dp))
                Text("4 Players", color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))

        Text("Mode chunein", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LudoMode.values().forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { mode = m },
                    label = { Text(m.name) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Spacer(Modifier.height(40.dp))
        Button(onClick = {
            navController.navigate("ludo_game/${mode.name}/$players")
        }) {
            Text("Game Shuru Karein")
        }
    }
}
