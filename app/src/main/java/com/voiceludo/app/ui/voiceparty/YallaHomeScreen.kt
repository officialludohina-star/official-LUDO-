package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Asal HTML ka "yallaHome" lobby screen (#051a0f background) — filhal simplified,
// yahan se Ludo game khola ja sakta hai. Store/Settings/Profile-edit jaisi baaki
// screens agle iteration mein add hongi.
@Composable
fun YallaHomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF051a0f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Voice Party Home", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { navController.navigate("ludo_mode_select") }) {
            Text("Ludo Khelein")
        }
    }
}
