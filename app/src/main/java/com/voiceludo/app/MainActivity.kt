package com.voiceludo.app

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
import com.voiceludo.app.ui.ludo.LudoModeSelectScreen
import com.voiceludo.app.ui.ludo.LudoGameScreen
import com.voiceludo.app.ui.voiceparty.VoicePartyMainScreen
import com.voiceludo.app.ui.voiceparty.MobileLoginScreen
import com.voiceludo.app.ui.voiceparty.YallaHomeScreen

// Poori app ka navigation graph — Voice Party (login/home) aur Ludo (mode-select/game)
// dono yahan se navigate hote hain, sab kuch native Kotlin/Compose mein.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "vp_main") {
                        composable("vp_main") { VoicePartyMainScreen(navController) }
                        composable("vp_mobile_login") { MobileLoginScreen(navController) }
                        composable("vp_home") { YallaHomeScreen(navController) }
                        composable("ludo_mode_select") { LudoModeSelectScreen(navController) }
                        composable("ludo_game/{mode}/{players}") { backStackEntry ->
                            val mode = backStackEntry.arguments?.getString("mode") ?: "classic"
                            val players = backStackEntry.arguments?.getString("players")?.toIntOrNull() ?: 4
                            LudoGameScreen(navController, mode = mode, players = players)
                        }
                    }
                }
            }
        }
    }
}
