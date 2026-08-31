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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Asal HTML ke "setPass" screen jaisa hi — naya password set karke account create karta
// hai (yahan local, on-device AccountStore mein — dekho AccountStore.kt ke comments).
@Composable
fun SetPasswordScreen(navController: NavController, method: String, contact: String) {
    val context = LocalContext.current
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var createdId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = "file:///android_asset/img/jungle_bg.jpg",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeaderBar("Set Password", Color(0xFF0a7a42)) { navController.popBackStack() }

            LoginCard {
                if (createdId != null) {
                    Text(
                        "\uD83C\uDF89 Account Created!",
                        color = Color(0xFF0a7a42), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Aapki Ludo ID: $createdId",
                        color = Color(0xFF222222), fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val loginRoute = if (method == "mobile") "vp_mobile_login" else "vp_gmail_login"
                            navController.popBackStack(loginRoute, inclusive = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0a7a42)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text("Login karein", color = Color.White, fontWeight = FontWeight.Bold) }
                } else {
                    RealInput(newPass, { newPass = it; errorText = "" }, "Enter new password", isPassword = true)
                    Spacer(Modifier.height(12.dp))
                    RealInput(confirmPass, { confirmPass = it; errorText = "" }, "Confirm password", isPassword = true)

                    if (errorText.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorText, color = Color(0xFFcc3333), fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFfdecec), RoundedCornerShape(8.dp)).padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            when {
                                newPass != confirmPass -> errorText = "Password match nahi ho raha"
                                newPass.length < 4 -> errorText = "Password kam se kam 4 characters ka ho"
                                else -> {
                                    val account = AccountStore.createAccount(context, method, contact, newPass)
                                    createdId = account.idNumber
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0a7a42)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text("Set Password", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
