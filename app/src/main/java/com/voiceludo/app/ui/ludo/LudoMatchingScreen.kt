package com.voiceludo.app.ui.ludo

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Asal HTML ke #ludoMatchingScreen jaisa hi — bet confirm hone ke baad "opponents
// dhoonda ja raha hai" wala screen. User ki di hui match.gif isi jagah chalti hai
// (mode icon ki jagah), players ki avatar circles pulse karti hain "searching"
// jaisa. 2.5 second baad khud hi asal game screen par navigate ho jata hai.
private const val MATCH_GIF = "https://i.postimg.cc/wvc7cYNC/match.gif"

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

    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate("ludo_game/$mode/$players/$magic") {
            popUpTo("ludo_matching/$mode/$players/$magic/$betIndex") { inclusive = true }
        }
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
            Text(
                "Opponents dhoonde ja rahe hain...",
                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
            )
            Spacer(Modifier.height(10.dp))

            // Matching GIF
            AsyncImage(
                model = MATCH_GIF,
                contentDescription = "matching",
                modifier = Modifier.size(190.dp)
            )

            // Entry coins + total pool
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .padding(top = 26.dp, start = 24.dp, end = 24.dp)
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

            Spacer(Modifier.height(26.dp))

            // Players — apna avatar solid, baqi "searching" (pulse)
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(horizontal = 14.dp)
            ) {
                for (i in 0 until players) {
                    MatchPlayerSlot(label = if (i == 0) "Aap" else "Player", isSearching = i != 0, small = players == 4)
                }
            }
        }

        // Close button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 12.dp)
                .size(width = 44.dp, height = 36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFFff7a2a), Color(0xFFc93a0a))))
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) { Text("\u2715", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp) }
    }
}

@Composable
private fun MatchPlayerSlot(label: String, isSearching: Boolean, small: Boolean) {
    val size = if (small) 76.dp else 96.dp
    val infinite = rememberInfiniteTransition(label = "match_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    if (isSearching) {
                        scaleX = pulse; scaleY = pulse; alpha = pulse
                    }
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF3d7fe0), Color(0xFF123a72)))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!isSearching) {
                Text("\uD83D\uDC64", fontSize = if (small) 26.sp else 34.sp)
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
