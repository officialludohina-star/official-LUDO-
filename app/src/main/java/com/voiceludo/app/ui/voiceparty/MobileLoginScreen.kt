package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Asal "mobileLogin" screen ka header (#0a7a42, gol close button) + phone/password form.
// NOTE: asal file mein 200+ countries ki poori dropdown list thi — yahan filhal
// Pakistan default rakha hai, baaki countries agle iteration mein add hongi.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileLoginScreen(navController: NavController) {
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0a2f1a))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0a7a42))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(32.dp))
            Text("Login", color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { navController.popBackStack() }
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text("\uD83C\uDDF5\uD83C\uDDF0 +92 Pakistan", color = Color.White)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { navController.navigate("vp_home") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue") }
        }
    }
}
