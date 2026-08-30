package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// Asal HTML ka "main" login screen — Facebook / Mobile / Gmail buttons, wahi gradients
// aur colors jo index.html mein the (#3d5afe, #ffb300, #0ab85a). Background wahi
// jungle/lake image hai jo asal HTML mein ".bg" aur ".yalla-bg" dono jagah use hoti hai
// (yaani home/lobby screen wala hi background) — taake login se home tak look consistent rahe.
private const val LOGIN_BG_IMG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"

@Composable
fun VoicePartyMainScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1f15))) {
        AsyncImage(
            model = LOGIN_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Halka dark overlay — asal HTML jaisa hi buttons background par saaf dikhein
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

        // Buttons screen ke bilkul center mein — asal HTML jaisa "margin-top:280px" wala
        // upar/neeche khali space nahi, seedha center-aligned column
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginButton(
                text = "Login with Facebook",
                icon = "f",
                gradient = Brush.verticalGradient(listOf(Color(0xFF7aa6ff), Color(0xFF3d5afe))),
                textColor = Color.White
            ) { navController.navigate("vp_facebook_login") }

            LoginButton(
                text = "Login with Mobile",
                icon = "\uD83D\uDCF1",
                gradient = Brush.verticalGradient(listOf(Color(0xFFffe65a), Color(0xFFffb300))),
                textColor = Color(0xFF3a2500)
            ) { navController.navigate("vp_mobile_login") }

            LoginButton(
                text = "Login with Gmail",
                icon = "\u2709\uFE0F",
                gradient = Brush.verticalGradient(listOf(Color(0xFF4cff9f), Color(0xFF0ab85a))),
                textColor = Color.White
            ) { navController.navigate("vp_gmail_login") }
        }
    }
}

@Composable
private fun LoginButton(
    text: String,
    icon: String,
    gradient: Brush,
    textColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, color = textColor, modifier = Modifier.padding(end = 8.dp))
                Text(text, color = textColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
