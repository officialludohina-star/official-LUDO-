package com.voiceludo.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
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
import com.voiceludo.app.ui.voiceparty.VoicePartyMainScreen
import com.voiceludo.app.ui.voiceparty.MobileLoginScreen
import com.voiceludo.app.ui.voiceparty.FacebookLoginScreen
import com.voiceludo.app.ui.voiceparty.GmailLoginScreen
import com.voiceludo.app.ui.voiceparty.GmailSignupScreen
import com.voiceludo.app.ui.voiceparty.SetPasswordScreen
import com.voiceludo.app.ui.voiceparty.YallaHomeScreen

// Poori app ka navigation graph — Voice Party (login/home) aur Ludo (mode-select/game)
// dono yahan se navigate hote hain, sab kuch native Kotlin/Compose mein.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    NavHost(navController = navController, startDestination = "vp_main") {
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
                        composable("ludo_game/{mode}/{players}/{magic}") { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "classic"
                            val players = backStackEntry.arguments?.getString("players")?.toIntOrNull() ?: 4
                            val magic = backStackEntry.arguments?.getString("magic")?.toBooleanStrictOrNull() ?: false
                            LudoGameScreen(navController, mode = mode, players = players, magic = magic)
                        }
                    }
                }
            }
        }
    }
}
