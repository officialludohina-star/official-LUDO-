package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.voiceludo.app.net.BackendClient
import com.voiceludo.app.net.ServerMessage

// Asal HTML ke #ludoMatchingScreen jaisa hi — bet confirm hone ke baad "opponents
// dhoonda ja raha hai" wala screen. Ab yeh REAL bekend se real matchmaking karta
// hai — koi fixed 2.5-second fake delay ya fake bot player nahi. Jab tak bekend
// se dusra REAL player (usi bet/mode/players par) na mile, yahin "waiting" dikhata
// rehta hai; "matched" message aane par hi asal game screen khulta hai.
@Composable
fun LudoMatchingScreen(
    navController: NavController,
    mode: String,
    players: Int,
    magic: Boolean,
    betIndex: Int
) {
    val bet = BET_OPTIONS.getOrElse(betIndex) { BET_OPTIONS.first() }
    val totalPool = bet * players
    var waitingText by remember { mutableStateOf("Opponents dhoonde ja rahe hain...") }
    var foundCount by remember { mutableStateOf(1) }
    var errorText by remember { mutableStateOf<String?>(null) }

    DisposableEffect(mode, players, magic, betIndex) {
        val listener: (ServerMessage) -> Unit = { msg ->
            when (msg) {
                is ServerMessage.Waiting -> {
                    waitingText = msg.message
                    // "1/2 players — ..." jaisa message parse kar ke count nikal lete hain,
                    // taake neeche wale slots mein sirf abhi tak "mile hue" players hi glow karein
                    Regex("^(\\d+)/").find(msg.message)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        foundCount = it
                    }
                }
                is ServerMessage.Matched -> {
                    navController.navigate("ludo_game/$mode/$players/$magic/$betIndex") {
                        popUpTo("ludo_matching/$mode/$players/$magic/$betIndex") { inclusive = true }
                    }
                }
                is ServerMessage.Err -> {
                    errorText = msg.message
                }
                else -> {}
            }
        }
        BackendClient.addListener(listener)
        BackendClient.join(mode, bet, players, magic)
        onDispose { BackendClient.removeListener(listener) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1a2a))) {
        AsyncImage(
            model = MODE_SCREEN_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Matching/searching gif — jab tak asal opponent bekend se nahi mil jata
            // (ServerMessage.Matched aane tak) yeh chalti rehti hai.
            AsyncImage(
                model = MATCHING_SEARCH_GIF,
                contentDescription = "searching",
                modifier = Modifier.size(90.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                waitingText,
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(30.dp))

            // Entry coins + total pool
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .padding(top = 0.dp, start = 24.dp, end = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                AsyncImage(model = COIN_ICON, contentDescription = null, modifier = Modifier.size(40.dp))
                Column {
                    Text("Entry Coins $bet", color = Color(0xFFffd93b), fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Total Pool $totalPool", color = Color(0xFFffd93b), fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(30.dp))

            // Players — sirf abhi tak REAL match hue players hi solid dikhte hain,
            // baaki khali slots dhoondne wala spinner dikhate hain (koi fake face nahi)
            if (players == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 0 until 2) {
                            MatchPlayerSlot(label = if (i == 0) "Aap" else "Player", found = i < foundCount)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 2 until 4) {
                            MatchPlayerSlot(label = "Player", found = i < foundCount)
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    for (i in 0 until players) {
                        MatchPlayerSlot(label = if (i == 0) "Aap" else "Player", found = i < foundCount)
                    }
                }
            }

            errorText?.let { err ->
                Spacer(Modifier.height(30.dp))
                Text(
                    err, color = Color(0xFFff6b6b), fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(12.dp)
                )
            }
        }

        // Close button — matchmaking queue se nikal jao
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 12.dp)
                .size(width = 44.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFff7a2a), Color(0xFFc93a0a))))
                .clickable {
                    BackendClient.leaveRoom()
                    navController.popBackStack()
                },
            contentAlignment = Alignment.Center
        ) { Text("\u2715", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
    }
}

@Composable
private fun MatchPlayerSlot(label: String, found: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (found) {
                AsyncImage(
                    model = DEFAULT_AVATAR_IMG,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(34.dp),
                    color = Color(0xFFffd93b),
                    strokeWidth = 3.dp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF143c64).copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
