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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage

private const val LOGIN_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val EMAIL_ICON = "https://i.postimg.cc/T29TPStz/IMG-20260831-WA0012.jpg"
private const val LOGIN_BUTTON = "https://i.postimg.cc/PqgDL1c1/IMG-20260831-WA0014.jpg"

@Composable
fun GmailLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = LOGIN_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeaderBar("Login", onClose = { navController.popBackStack() })

            LoginCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = EMAIL_ICON,
                        contentDescription = "Email",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .width(112.dp)
                            .height(56.dp)
                    )
                    RealInput(
                        email, { email = it; errorText = "" }, "Enter email address",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                RealInput(
                    pass, { pass = it; errorText = "" },
                    "Enter password", isPassword = true
                )

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

                val loginEnabled = pass.isNotBlank()
                if (loginEnabled) {
                    RemoteImageButton(
                        imageUrl = LOGIN_BUTTON,
                        contentDescription = "Login",
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
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    )
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFFbdbdbd)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp)
                    ) { Text("Login", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                LinkRow(
                    leftText = "Forgot Password?",
                    rightText = "Sign Up",
                    onLeft = { },
                    onRight = { navController.navigate("vp_gmail_signup") }
                )
            }
        }
    }
}
