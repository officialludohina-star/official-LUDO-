package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.offset
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
    // Facebook icon ke live size/position controls
    var fbIconSize by remember { mutableStateOf(52f) }
    var fbOffsetX by remember { mutableStateOf(0f) }
    var fbOffsetY by remember { mutableStateOf(0f) }

    // Gmail icon ke live size/position controls
    var gmailIconSize by remember { mutableStateOf(52f) }
    var gmailOffsetX by remember { mutableStateOf(0f) }
    var gmailOffsetY by remember { mutableStateOf(0f) }

    var showPanel by remember { mutableStateOf(false) }

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
                bgColor = Color.Transparent,
                iconSize = fbIconSize.dp,
                iconOffsetX = fbOffsetX.dp,
                iconOffsetY = fbOffsetY.dp,
                onClick = { navController.navigate("vp_facebook_login") }
            )
            // Gmail signup button
            BigIconLoginButton(
                iconUrl = GMAIL_ICON,
                label = "Login with Gmail",
                bgColor = Color.Transparent,
                iconSize = gmailIconSize.dp,
                iconOffsetX = gmailOffsetX.dp,
                iconOffsetY = gmailOffsetY.dp,
                onClick = { navController.navigate("vp_gmail_login") }
            )
        }

        // Panel kholne/band karne ka chota gear button
        Button(
            onClick = { showPanel = !showPanel },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.45f)),
            contentPadding = PaddingValues(12.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 16.dp)
        ) {
            Text(text = "⚙", color = Color.White, fontSize = 18.sp)
        }

        // Icon size/position adjust karne wala panel
        if (showPanel) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text("Facebook icon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LabeledSlider("Size", fbIconSize, 20f, 100f) { fbIconSize = it }
                LabeledSlider("Left-Right", fbOffsetX, -100f, 100f) { fbOffsetX = it }
                LabeledSlider("Up-Down", fbOffsetY, -100f, 100f) { fbOffsetY = it }

                Spacer(Modifier.height(12.dp))

                Text("Gmail icon", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LabeledSlider("Size", gmailIconSize, 20f, 100f) { gmailIconSize = it }
                LabeledSlider("Left-Right", gmailOffsetX, -100f, 100f) { gmailOffsetX = it }
                LabeledSlider("Up-Down", gmailOffsetY, -100f, 100f) { gmailOffsetY = it }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    onChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Color.White, fontSize = 12.sp, modifier = Modifier.width(80.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BigIconLoginButton(
    iconUrl: String,
    label: String,
    bgColor: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    iconOffsetX: androidx.compose.ui.unit.Dp,
    iconOffsetY: androidx.compose.ui.unit.Dp,
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
            // Big icon — size aur position dono panel se adjustable
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(iconSize)
                    .offset(x = iconOffsetX, y = iconOffsetY)
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
