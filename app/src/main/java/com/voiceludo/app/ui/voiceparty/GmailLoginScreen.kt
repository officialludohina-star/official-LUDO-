package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import com.voiceludo.app.net.BackendClient
import com.voiceludo.app.net.ServerMessage
import com.voiceludo.app.net.SessionStore
import com.voiceludo.app.ui.common.LoadingOverlay
import com.voiceludo.app.ui.ludo.NO_CONNECTION_ICON
import kotlinx.coroutines.delay

private const val LOGIN_BG = "file:///android_asset/img/file-0000000097f0820b81bc2995a995177d.png"
private const val EMAIL_ICON = "https://i.postimg.cc/T29TPStz/IMG-20260831-WA0012.jpg"
private const val LOGIN_BUTTON = "https://i.postimg.cc/PqgDL1c1/IMG-20260831-WA0014.jpg"

// Panel se test kiye gaye final numbers — ab permanent bake kar diye.
private const val EMAIL_ICON_WIDTH = 80
private const val EMAIL_ICON_HEIGHT = 40
private const val EMAIL_ICON_OFFSET_X = 0
private const val EMAIL_ICON_OFFSET_Y = 0
private const val EMAIL_INPUT_HEIGHT = 56
private const val EMAIL_INPUT_OFFSET_X = 0
private const val EMAIL_INPUT_OFFSET_Y = 0

// Ab REAL bekend (BackendClient.login) se login hota hai — koi local/fake
// account-check nahi. Bekend ka apna account (email + bcrypt password) hi
// asal source-of-truth hai, jo signup ke waqt SetPasswordScreen se bana tha.
//
// DEBUG BUILD: 10-second timeout add kiya hai (server se koi response na aaye
// to loading hamesha ke liye ghoomta nahi rahega) aur ek chhota neela debug
// text bhi dikhta hai jisse pata chalta hai server se asal mein kya mila —
// isse "stuck ho jata hai" wali problem diagnose karna aasan ho jayega.
@Composable
fun GmailLoginScreen(navController: NavController) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var debugText by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    // WebSocket bekend se connect hi na ho paye (net na ho ya server down) — is
    // waqt baar-baar screen par NO_CONNECTION_ICON dikhaya jata hai.
    var connectionLost by remember { mutableStateOf(false) }

    // Agar loading 10 second se zyada chale (slow network / server down) to
    // khud hi timeout error dikha do — hamesha ghoomta hua spinner nahi rehna chahiye.
    LaunchedEffect(loading) {
        if (loading) {
            delay(10000)
            if (loading) {
                loading = false
                errorText = "Timeout: 10 second mein server se koi response nahi aaya. Network check karein."
            }
        }
    }

    DisposableEffect(Unit) {
        val listener: (ServerMessage) -> Unit = { msg ->
            debugText = if (msg is ServerMessage.ConnectionClosed) {
                "Debug: connection closed -> reason: '${msg.reason}'"
            } else {
                "Debug: server se mila -> ${msg::class.simpleName}"
            }
            when (msg) {
                is ServerMessage.Auth -> {
                    loading = false
                    connectionLost = false
                    AccountStore.saveSession(context, "gmail", email)
                    SessionStore.save(context, msg.playerId, msg.authToken, msg.name, msg.avatar, msg.coins, msg.diamonds)
                    navController.navigate("vp_home")
                }
                is ServerMessage.Err -> {
                    loading = false
                    connectionLost = false
                    errorText = if (msg.message.isNotBlank()) msg.message else "Server ne error bheja (khali message)"
                }
                is ServerMessage.ConnectionClosed -> {
                    // Pehle yahan kuch nahi hota tha — agar WebSocket connect hi na ho pae
                    // (server down/unreachable), loading hamesha ke liye ghoomta rehta tha
                    // aur koi error kabhi nazar nahi aata tha. Ab connection fail hone par
                    // seedha error + NO_CONNECTION_ICON dikha dete hain.
                    connectionLost = true
                    if (loading) {
                        loading = false
                        errorText = "Server se connect nahi ho saka: ${msg.reason}"
                    }
                }
                is ServerMessage.ConnectionOpened -> connectionLost = false
                else -> {}
            }
        }
        BackendClient.addListener(listener)
        onDispose { BackendClient.removeListener(listener) }
    }

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

                RealInput(
                    pass, { pass = it; errorText = "" },
                    "Enter password", isPassword = true
                )

                if (errorText.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectionLost) {
                            AsyncImage(
                                model = NO_CONNECTION_ICON,
                                contentDescription = "no connection",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            errorText, color = Color(0xFFcc3333), fontSize = 11.sp,
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFfdecec), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        )
                    }
                }

                if (debugText.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        debugText, color = Color(0xFF1565C0), fontSize = 10.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(16.dp))

                val loginEnabled = pass.isNotBlank() && !loading
                if (loginEnabled) {
                    RemoteImageButton(
                        imageUrl = LOGIN_BUTTON,
                        contentDescription = "Login",
                        onClick = {
                            when {
                                email.isBlank() || pass.isBlank() ->
                                    errorText = "Please enter your email and password."
                                else -> {
                                    loading = true
                                    errorText = ""
                                    debugText = "Debug: connecting..."
                                    BackendClient.login(email, pass)
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
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Login", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                LinkRow(
                    leftText = "Forgot Password?",
                    rightText = "Sign Up",
                    onLeft = { navController.navigate("vp_forgot_password") },
                    onRight = { navController.navigate("vp_gmail_signup") }
                )
            }
        }

        // Jab tak bekend se jawab (Auth ya error) na aa jaye, poori screen block —
        // aage nahi badh sakte, dobara button bhi nahi daba sakte.
        if (loading) {
            LoadingOverlay("Login ho raha hai...")
        }
    }
}
