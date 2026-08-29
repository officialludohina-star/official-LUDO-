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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// Asal "facebookLogin" screen ka hoobahoo design — #1877f2 header/buttons.
@Composable
fun FacebookLoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0a2f1a))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF1877f2)).padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(32.dp))
            Text("Facebook Login", color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) { Text("\u2715", color = Color.White) }
        }

        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF1877f2)),
                contentAlignment = Alignment.Center
            ) { Text("f", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.height(8.dp))
            Text("Log in to Facebook", color = Color.White)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Mobile number or email") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pass, onValueChange = { pass = it },
                label = { Text("Facebook password") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("vp_home") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877f2)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Log In") }
        }
    }
}
