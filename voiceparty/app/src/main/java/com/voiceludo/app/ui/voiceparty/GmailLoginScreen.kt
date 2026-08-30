package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Asal "gmailLogin" screen — #0a7a42 header, "✉️ Email" green-badge + input row,
// password field, gray "Login" button, Forgot Password/Sign Up links.
//
// Pehle "Login" hamesha seedha vp_home par chala jata tha (chahe account ho ya na ho),
// aur "Sign Up" ek khali TODO tha — ab dono AccountStore (dekho AccountStore.kt) se
// asal mein connected hain, jaisa HTML mein Firebase se tha.
@Composable
fun GmailLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0a2f1a)),
        verticalArrangement = Arrangement.Center
    ) {
        LoginHeaderBar("Login", Color(0xFF0a7a42)) { navController.popBackStack() }

        LoginCard {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0a7a42), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Text("\u2709\uFE0F Email", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                RealInput(
                    email, { email = it; errorText = "" }, "Enter email",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            RealInput(pass, { pass = it; errorText = "" }, "Enter password", isPassword = true)

            if (errorText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    errorText, color = Color(0xFFcc3333), fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFfdecec), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isBlank() || pass.isBlank()) {
                        errorText = "Email aur password enter karein"
                        return@Button
                    }
                    when (val result = AccountStore.login(context, "gmail", email, pass)) {
                        is AccountStore.LoginResult.Success -> navController.navigate("vp_home")
                        AccountStore.LoginResult.NoAccount ->
                            errorText = "\u274C Please check email - No account found! Sign Up karein"
                        AccountStore.LoginResult.WrongPassword ->
                            errorText = "Wrong Password"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFbdbdbd)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) { Text("Login", color = Color(0xFF222222), fontWeight = FontWeight.Bold) }

            LinkRow(
                leftText = "Forgot Password?",
                rightText = "Sign Up",
                onLeft = { /* TODO: forgotGmail screen abhi nahi bani */ },
                onRight = { navController.navigate("vp_gmail_signup") }
            )
        }
    }
}
