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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

// Same jungle/moon background as rest of app
private const val LOGIN_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val FB_ICON   = "https://i.postimg.cc/zvYRnVKB/bind-f-1c2b455.png"
private const val GMAIL_ICON = "https://i.postimg.cc/9fzTV3C1/bind-e-e8c989f.png"

@Composable
fun VoicePartyMainScreen(navController: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1f15))) {
        AsyncImage(
            model = LOGIN_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Subtle dark overlay so buttons are readable
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Facebook login button
            BigIconLoginButton(
                iconUrl = FB_ICON,
                label = "Login with Facebook",
                bgColor = Color(0x993d5afe), // semi-transparent blue
                onClick = { navController.navigate("vp_facebook_login") }
            )
            // Gmail signup button
            BigIconLoginButton(
                iconUrl = GMAIL_ICON,
                label = "Login with Gmail",
                bgColor = Color(0x990ab85a), // semi-transparent green
                onClick = { navController.navigate("vp_gmail_login") }
            )
        }
    }
}

@Composable
private fun BigIconLoginButton(
    iconUrl: String,
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Big icon
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(52.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp
            )
        }
    }
}
