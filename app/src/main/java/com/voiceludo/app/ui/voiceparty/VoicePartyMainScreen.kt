package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// Login background — wahi jungle/moon wala jo asal app mein hai
private const val LOGIN_BG_IMG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"

// Custom login button icons (user ke diye gaye URLs)
private const val FB_ICON_URL  = "https://i.postimg.cc/zvYRnVKB/bind-f-1c2b455.png"
private const val GMAIL_ICON_URL = "https://i.postimg.cc/9fzTV3C1/bind-e-e8c989f.png"

@Composable
fun VoicePartyMainScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1f15))) {
        // Jungle/moon background
        AsyncImage(
            model = LOGIN_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Facebook icon button
            IconLoginButton(
                iconUrl = FB_ICON_URL,
                gradient = Brush.verticalGradient(listOf(Color(0xFF7aa6ff), Color(0xFF3d5afe))),
                onClick = { navController.navigate("vp_facebook_login") }
            )

            // Gmail/signup icon button (Number wala hataya — sirf 2 buttons)
            IconLoginButton(
                iconUrl = GMAIL_ICON_URL,
                gradient = Brush.verticalGradient(listOf(Color(0xFF4cff9f), Color(0xFF0ab85a))),
                onClick = { navController.navigate("vp_gmail_signup") }
            )
        }
    }
}

// Sirf icon wala button — koi text nahi, sirf image center mein
@Composable
private fun IconLoginButton(
    iconUrl: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(),
        modifier = Modifier
            .width(220.dp)
            .height(64.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
