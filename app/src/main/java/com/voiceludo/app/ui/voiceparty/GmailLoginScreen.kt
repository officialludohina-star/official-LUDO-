package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

private const val LOGIN_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val EMAIL_ICON = "https://i.postimg.cc/GhVRgSb8/IMG-20260831-WA0012.jpg"

@Composable
fun GmailLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Jungle background — same as login screen
        AsyncImage(
            model = LOGIN_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            // Green "Login" header bar hata diya — sirf ek chhota close (X) button,
            // koi green background nahi, seedha jungle background par float karta hai.
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) { Text("\u2715", color = Color.White) }
            }

            LoginCard(showTopBorder = false) {
                // Email field — icon ab apne asal size mein, bina green badge ke.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = EMAIL_ICON,
                        contentDescription = "Email",
                        modifier = Modifier.size(40.dp).padding(end = 8.dp)
                    )
                    RealInput(
                        email, { email = it; errorText = "" }, "Enter email address",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Password field
                RealInput(
                    pass, { pass = it; errorText = "" },
                    "Enter password", isPassword = true
                )

                // Error
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

                // Login button — password khali ho to grey/disabled, kuch bhi type
                // hote hi green/active ho jata hai.
                val loginEnabled = pass.isNotBlank()
                Button(
                    onClick = {
                        when {
                            email.isBlank() || pass.isBlank() ->
                                errorText = "Please enter your email and password."
                            else -> when (val result = AccountStore.login(context, "gmail", email, pass)) {
                                is AccountStore.LoginResult.Success -> {
                                    AccountStore.saveSession(context, "gmail", email)
                                    navController.navigate("vp_home")
                                }
                                AccountStore.LoginResult.NoAccount ->
                                    errorText = "No account found with this email. Please sign up first."
                                AccountStore.LoginResult.WrongPassword ->
                                    errorText = "Incorrect password. Please try again."
                            }
                        }
                    },
                    enabled = loginEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0a7a42),
                        disabledContainerColor = Color(0xFFbdbdbd)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(4.dp))

                // Sign Up link
                LinkRow(
                    leftText = "Forgot Password?",
                    rightText = "Sign Up",
                    onLeft = { /* TODO: forgot password screen */ },
                    onRight = { navController.navigate("vp_gmail_signup") }
                )
            }
        }
    }
}
