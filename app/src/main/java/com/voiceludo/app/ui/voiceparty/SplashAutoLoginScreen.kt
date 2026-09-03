package com.voiceludo.app.ui.voiceparty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.voiceludo.app.net.BackendClient
import com.voiceludo.app.net.ConnState
import com.voiceludo.app.net.SessionStore
import com.voiceludo.app.ui.common.LoadingOverlay

// ============================================================================
// App khulte hi agar pehle se saved session (auth_token) mil jaye, to yahan
// khud-b-khud (bina dobara email/password maange) server se re-authenticate
// ho kar seedha home par chala jata hai.
//
// Yeh usi bug ka fix hai: "1 dafa login karne ke baad, app band/on karne par
// dobara signup/login maanga jana" — pehle koi bhi saved session check hi
// nahi hoti thi, app hamesha login screen se hi shuru hoti thi. Ab startup par
// yahi screen check karti hai (MainActivity dekhein).
// ============================================================================
@Composable
fun SplashAutoLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val connState by BackendClient.state.collectAsState()
    var attempted by remember { mutableStateOf(false) }

    LaunchedEffect(connState) {
        if (connState == ConnState.CONNECTED && !attempted) {
            attempted = true
            val token = SessionStore.getToken(context)
            if (token == null) {
                navController.navigate("vp_main") { popUpTo("vp_splash") { inclusive = true } }
                return@LaunchedEffect
            }
            BackendClient.autoLogin(token) { success, _ ->
                if (success) {
                    navController.navigate("vp_home") {
                        popUpTo("vp_splash") { inclusive = true }
                    }
                } else {
                    // Saved token ab valid nahi (jaise server-side account/session
                    // reset ho chuka) — chup-chap fail hone ki bajaye seedha login
                    // screen par bhej dete hain.
                    SessionStore.clear(context)
                    navController.navigate("vp_main") {
                        popUpTo("vp_splash") { inclusive = true }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a2f1a))) {
        LoadingOverlay("Restoring your session...")
    }
}
