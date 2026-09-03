package com.voiceludo.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.voiceludo.app.ui.ludo.LudoModeSelectScreen
import com.voiceludo.app.ui.ludo.LudoGameScreen
import com.voiceludo.app.ui.ludo.LudoMatchingScreen
import com.voiceludo.app.ui.voiceparty.VoicePartyMainScreen
import com.voiceludo.app.ui.voiceparty.MobileLoginScreen
import com.voiceludo.app.ui.voiceparty.FacebookLoginScreen
import com.voiceludo.app.ui.voiceparty.GmailLoginScreen
import com.voiceludo.app.ui.voiceparty.GmailSignupScreen
import com.voiceludo.app.ui.voiceparty.SetPasswordScreen
import com.voiceludo.app.ui.voiceparty.SplashAutoLoginScreen
import com.voiceludo.app.ui.voiceparty.YallaHomeScreen
import com.voiceludo.app.net.BackendClient

// Poori app ka navigation graph — Voice Party (login/home) aur Ludo (mode-select/game)
// dono yahan se navigate hote hain, sab kuch native Kotlin/Compose mein.
class MainActivity : ComponentActivity() {
    // Poori app ko full-screen/immersive banata hai — status bar (battery, network,
    // clock waghera) aur navigation bar dono hide ho jate hain, taake mobile par game
    // bilkul edge-to-edge, "app jaisa" nazar aaye, browser/system chrome jaisa nahi.
    // User swipe kar ke bars ko thori der ke liye wapis la sakta hai (transient), is
    // liye onWindowFocusChanged mein dobara hide kar dete hain taake woh sticky rahe.
    private fun applyImmersiveFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Notch/camera-cutout wale phones par, immersive mode ke bawajood, us
        // cutout ke peechay content draw nahi hota jab tak yeh explicitly allow
        // na kiya jaye — warna wahan hamesha ek black patti dikhti rehti hai
        // (chahe status bar hidden ho). Yeh line poori screen tak content ko
        // extend karti hai, cutout ke peechay bhi.
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    if (Build.VERSION.SDK_INT >= 30) {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    } else {
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveFullScreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveFullScreen()

        // ZAROORI: yeh call kahin bhi nahi ho rahi thi — is ke bagair BackendClient
        // ka appContext hamesha null rehta, connect() kuch karta hi nahi (khali
        // return), aur poori app "Server se connection nahi hai" state mein
        // hamesha atki rehti — chahe internet theek ho ya na ho. Yahin se WebSocket
        // connection asal mein shuru hoti hai.
        BackendClient.init(applicationContext)

        // Dice-roll GIF (aur koi bhi doosri animated GIF) ko sahi se animate karne ke liye
        // Coil ke default ImageLoader mein GIF decoder register karna zaroori hai — warna
        // AsyncImage GIF ka sirf pehla frame static dikhata hai.
        val gifImageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
        Coil.setImageLoader(gifImageLoader)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "vp_splash") {
                        composable("vp_splash") { SplashAutoLoginScreen(navController) }
                        composable("vp_main") { VoicePartyMainScreen(navController) }
                        composable("vp_mobile_login") { MobileLoginScreen(navController) }
                        composable("vp_facebook_login") { FacebookLoginScreen(navController) }
                        composable("vp_gmail_login") { GmailLoginScreen(navController) }
                        composable("vp_gmail_signup") { GmailSignupScreen(navController) }
                        composable(
                            "vp_set_password/{method}/{contact}",
                            arguments = listOf(
                                navArgument("method") { type = NavType.StringType },
                                navArgument("contact") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val method = backStackEntry.arguments?.getString("method") ?: "gmail"
                            val contact = backStackEntry.arguments?.getString("contact") ?: ""
                            SetPasswordScreen(navController, method = method, contact = contact)
                        }
                        composable("vp_home") { YallaHomeScreen(navController) }
                        composable("ludo_mode_select") { LudoModeSelectScreen(navController) }
                        composable(
                            "ludo_matching/{mode}/{players}/{magic}/{betIndex}",
                            arguments = listOf(
                                navArgument("mode") { type = NavType.StringType },
                                navArgument("players") { type = NavType.IntType },
                                navArgument("magic") { type = NavType.BoolType },
                                navArgument("betIndex") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "classic"
                            val players = backStackEntry.arguments?.getInt("players") ?: 2
                            val magic = backStackEntry.arguments?.getBoolean("magic") ?: false
                            val betIndex = backStackEntry.arguments?.getInt("betIndex") ?: 0
                            LudoMatchingScreen(navController, mode = mode, players = players, magic = magic, betIndex = betIndex)
                        }
                        composable(
                            "ludo_game/{mode}/{players}/{magic}/{betIndex}",
                            arguments = listOf(
                                navArgument("mode") { type = NavType.StringType },
                                navArgument("players") { type = NavType.IntType },
                                navArgument("magic") { type = NavType.BoolType },
                                navArgument("betIndex") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "classic"
                            val players = backStackEntry.arguments?.getInt("players") ?: 4
                            val magic = backStackEntry.arguments?.getBoolean("magic") ?: false
                            val betIndex = backStackEntry.arguments?.getInt("betIndex") ?: 0
                            LudoGameScreen(navController, mode = mode, players = players, magic = magic, betIndex = betIndex)
                        }
                    }
                }
            }
        }
    }
}
