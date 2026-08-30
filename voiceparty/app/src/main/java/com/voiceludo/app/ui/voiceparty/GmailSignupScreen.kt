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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Same local jungle background as the login screen — loads instantly, no network needed
private const val SIGNUP_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"

@Composable
fun GmailSignupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var lastOtp by remember { mutableStateOf<String?>(null) }
    var secondsLeft by remember { mutableStateOf(0) }
    var errorText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Jungle background — same asset as login screen
        AsyncImage(
            model = SIGNUP_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeaderBar("Sign Up", Color(0xFF0a7a42)) { navController.popBackStack() }

            LoginCard {
                // Email field
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0a7a42), RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Text("\u2709\uFE0F", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    RealInput(
                        email, { email = it; errorText = "" }, "Enter email address",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // OTP row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RealInput(
                        code, { code = it; errorText = "" }, "Verification Code",
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorText = "Please enter your email address."
                            } else {
                                lastOtp = AccountStore.generateOtp()
                                otpSent = true
                                secondsLeft = 60
                                scope.launch {
                                    while (secondsLeft > 0) { delay(1000); secondsLeft-- }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd1d5db)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = secondsLeft == 0
                    ) {
                        Text(
                            if (secondsLeft > 0) "${secondsLeft}s" else "Obtain",
                            color = Color(0xFF222222), fontWeight = FontWeight.Bold, fontSize = 13.sp
                        )
                    }
                }

                if (otpSent) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "A verification code has been sent. Enter it above.",
                        color = Color(0xFF0a7a42), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                    // Demo only — remove when real email backend is connected
                    Text(
                        "Test code: ${lastOtp ?: "1234"} (or use 1234)",
                        color = Color(0xFF6b8f7a), fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                }

                // Error message
                if (errorText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorText,
                        color = Color(0xFFcc3333), fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFfdecec), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Confirm button
                Button(
                    onClick = {
                        when {
                            email.isBlank() ->
                                errorText = "Please enter your email address."
                            !AccountStore.verifyOtp(code) ->
                                errorText = "Incorrect verification code. Please try again."
                            AccountStore.accountExists(context, "gmail", email) ->
                                errorText = "An account with this email already exists. Please log in."
                            else ->
                                navController.navigate("vp_set_password/gmail/$email")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0a7a42)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
