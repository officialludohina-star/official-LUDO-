package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

private const val SIGNUP_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val EMAIL_ICON = "https://i.postimg.cc/T29TPStz/IMG-20260831-WA0012.jpg"
private const val OBTAIN_BUTTON = "https://i.postimg.cc/dtgJcnTx/IMG-20260831-WA0016.jpg"
private const val CONFIRM_BUTTON = "https://i.postimg.cc/zf7mvk3P/IMG-20260831-WA0017.jpg"

// Panel se test kiye gaye final numbers — ab permanent bake kar diye.
private const val EMAIL_ICON_WIDTH = 80
private const val EMAIL_ICON_HEIGHT = 48
private const val EMAIL_ICON_OFFSET_X = -8
private const val EMAIL_ICON_OFFSET_Y = 0
private const val EMAIL_INPUT_HEIGHT = 56
private const val EMAIL_INPUT_OFFSET_X = -8
private const val EMAIL_INPUT_OFFSET_Y = 0
private const val OBTAIN_BTN_WIDTH = 84
private const val OBTAIN_BTN_HEIGHT = 44
private const val OBTAIN_BTN_OFFSET_X = 0
private const val OBTAIN_BTN_OFFSET_Y = 0
private const val CONFIRM_BTN_HEIGHT = 58
private const val CONFIRM_BTN_OFFSET_X = -4
private const val CONFIRM_BTN_OFFSET_Y = -4

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
        AsyncImage(
            model = SIGNUP_BG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.20f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            LoginHeaderBar("Sign Up", onClose = { navController.popBackStack() })

            LoginCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = EMAIL_ICON,
                        contentDescription = "Email",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .width(EMAIL_ICON_WIDTH.dp)
                            .height(EMAIL_ICON_HEIGHT.dp)
                            .offset(x = EMAIL_ICON_OFFSET_X.dp, y = EMAIL_ICON_OFFSET_Y.dp)
                    )
                    RealInput(
                        email, { email = it; errorText = "" }, "Enter email address",
                        modifier = Modifier
                            .weight(1f)
                            .height(EMAIL_INPUT_HEIGHT.dp)
                            .offset(x = EMAIL_INPUT_OFFSET_X.dp, y = EMAIL_INPUT_OFFSET_Y.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RealInput(
                        code, { code = it; errorText = "" }, "Verification Code",
                        modifier = Modifier.weight(1f)
                    )
                    if (secondsLeft == 0) {
                        RemoteImageButton(
                            imageUrl = OBTAIN_BUTTON,
                            contentDescription = "Obtain",
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
                            modifier = Modifier
                                .width(OBTAIN_BTN_WIDTH.dp)
                                .height(OBTAIN_BTN_HEIGHT.dp)
                                .offset(x = OBTAIN_BTN_OFFSET_X.dp, y = OBTAIN_BTN_OFFSET_Y.dp)
                        )
                    } else {
                        Text(
                            "${secondsLeft}s",
                            modifier = Modifier.width(OBTAIN_BTN_WIDTH.dp),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF6d756f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
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
                    Text(
                        "Test code: ${lastOtp ?: "1234"} (or use 1234)",
                        color = Color(0xFF6b8f7a), fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
                    )
                }

                if (errorText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        errorText,
                        color = Color(0xFFcc3333), fontSize = 11.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFfdecec), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                RemoteImageButton(
                    imageUrl = CONFIRM_BUTTON,
                    contentDescription = "Confirm",
                    onClick = {
                        when {
                            email.isBlank() -> errorText = "Please enter your email address."
                            !AccountStore.verifyOtp(code) -> errorText = "Incorrect verification code. Please try again."
                            AccountStore.accountExists(context, "gmail", email) -> errorText = "An account with this email already exists. Please log in."
                            else -> navController.navigate("vp_set_password/gmail/$email")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CONFIRM_BTN_HEIGHT.dp)
                        .offset(x = CONFIRM_BTN_OFFSET_X.dp, y = CONFIRM_BTN_OFFSET_Y.dp)
                )
            }
        }
    }
}
