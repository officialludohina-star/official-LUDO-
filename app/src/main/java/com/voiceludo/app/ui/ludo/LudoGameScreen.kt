package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun LudoGameScreen(navController: NavController, mode: String, players: Int) {
    val ludoMode = LudoMode.valueOf(mode)
    val colorList = if (players == 4) PLAYER_COLORS_4P else PLAYER_COLORS_2P
    val state = remember { LudoGameState(ludoMode, colorList) }

    // Bot turns: player index 0 hamesha "aap" hain, baaki players auto-play karte hain
    LaunchedEffect(state.currentIdx.value, state.gameOver.value) {
        if (state.gameOver.value) return@LaunchedEffect
        if (state.currentIdx.value != 0) {
            delay(600)
            state.rollDice()
            delay(500)
            if (state.movable.isNotEmpty()) {
                state.moveToken(state.movable.first())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Asal HTML jaisa hi game-bg background image
        AsyncImage(
            model = GAME_BG_IMG,
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            var showSettings by remember { mutableStateOf(false) }
            var soundOn by remember { mutableStateOf(true) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AsyncImage(
                        model = SETTINGS_ICON, contentDescription = "settings",
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { showSettings = !showSettings }
                    )
                    AsyncImage(
                        model = BET_INFO_ICON, contentDescription = "bet info",
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.25f))
                    )
                }
            }

            if (showSettings) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = SOUND_ICON, contentDescription = "sound",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable { soundOn = !soundOn }
                        )
                        Text(if (soundOn) "On" else "Off", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(
                            model = EXIT_ICON, contentDescription = "exit",
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable { navController.popBackStack() }
                        )
                        Text("Exit", color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                colorList.forEach { c -> PlayerBadge(c, state) }
            }

            Spacer(Modifier.height(12.dp))
            LudoBoardCanvas(state) { _, _ -> }
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = DICE_FACE_IMG[state.dice.value],
                    contentDescription = "dice",
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { state.rollDice() },
                    enabled = !state.gameOver.value && state.currentIdx.value == 0 && !state.diceRolled.value
                ) { Text("Roll") }
            }

            Spacer(Modifier.height(12.dp))
            if (state.currentIdx.value == 0 && state.movable.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.movable.forEach { idx ->
                        Button(onClick = { state.moveToken(idx) }) { Text("Token ${idx + 1}") }
                    }
                }
            }

            if (state.gameOver.value) {
                Spacer(Modifier.height(16.dp))
                Text(state.winnerText.value, color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
                Button(onClick = { navController.popBackStack() }) { Text("Wapis Mode Select") }
            }
        }
    }
}

@Composable
private fun PlayerBadge(color: LudoColor, state: LudoGameState) {
    val rank = state.rankBadge[color]
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            AsyncImage(
                model = DEFAULT_AVATAR_IMG,
                contentDescription = color.name,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ludoColorOf(color))
            )
            if (rank != null) {
                AsyncImage(
                    model = RANK_BADGE_IMG[rank],
                    contentDescription = "rank $rank",
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-8).dp)
                )
            }
        }
        Text(color.name, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
