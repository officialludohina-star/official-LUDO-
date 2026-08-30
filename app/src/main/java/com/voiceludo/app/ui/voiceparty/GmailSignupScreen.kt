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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Signup background — user ka diya hua jungle/moon background
private const val SIGNUP_BG = "https://i.postimg.cc/wxDN9QYp/bghome-1295ddb.png"

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

    // Full-screen box with jungle background
    Box(modifier = Modifier.fillMaxSize()) {
        // Jungle/moon background
        AsyncImage(
            model = SIGNUP_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Dark overlay taake form readable rahe
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeaderBar("Sign Up", Color(0xFF0a7a42)) { navController.popBackStack() }

            LoginCard {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RealInput(code, { code = it; errorText = "" }, "Verification Code", modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (email.isBlank()) {
                                errorText = "Email enter karein"
                            } else {
                                lastOtp = AccountStore.generateOtp()
                                otpSent = true
                                secondsLeft = 60
                                scope.launch {
                                    while (secondsLeft > 0) {
                                        delay(1000)
                                        secondsLeft--
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFd1d5db)),
                        shape = RoundedCornerShape(12.dp),
                        enabled = secondsLeft == 0
                    ) { Text("Obtain", color = Color(0xFF222222), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }

                if (otpSent) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (secondsLeft > 0) "Resend in ${secondsLeft}s" else "Resend available",
                        color = Color(0xFF0a7a42), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        "Demo OTP: ${lastOtp ?: "1234"} (ya 1234 bhi chalega)",
                        color = Color(0xFF6b8f7a), fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

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

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        when {
                            email.isBlank() -> errorText = "Email enter karein"
                            !AccountStore.verifyOtp(code) -> errorText = "Wrong OTP, 1234 try karein"
                            AccountStore.accountExists(context, "gmail", email) ->
                                errorText = "Is email se account pehle se bana hua hai — Login karein"
                            else -> navController.navigate("vp_set_password/gmail/$email")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0a7a42)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text("Confirm", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
