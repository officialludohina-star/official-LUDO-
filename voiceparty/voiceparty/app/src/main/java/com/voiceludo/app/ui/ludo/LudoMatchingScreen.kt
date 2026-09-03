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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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
    // Match mil jaane par turant navigate nahi karte — pehle 2 second ke liye
    // sabke asal naam/DP dikhate hain (neeche slots mein), phir game screen
    // khulti hai. Jab tak yeh null hai, opponent slots loading-gif dikhate hain.
    var matchedMsg by remember { mutableStateOf<ServerMessage.Matched?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    // Agar matchmaking ke doraan hi bekend se connection toot jaye (net drop)
    // to yeh true ho jata hai — NO_CONNECTION_ICON dikhane ke liye. Server khud
    // reconnect hote hi (BackendClient.reconnect()) join wapis nahi bhejta —
    // isliye connection wapis aate hi hum khud dobara "join" bhej dete hain
    // taake queue mein wapis shamil ho jayen.
    var connectionLost by remember { mutableStateOf(false) }

    LaunchedEffect(connectionLost) {
        if (connectionLost) {
            while (connectionLost) {
                BackendClient.reconnect()
                kotlinx.coroutines.delay(4000)
            }
        }
    }

    // Match milte hi (matchedMsg set hote hi) 2 second ruk kar phir game screen
    // par navigate karte hain — is dauran user sab players ke asal naam/DP
    // dekh sakta hai.
    LaunchedEffect(matchedMsg) {
        val m = matchedMsg ?: return@LaunchedEffect
        kotlinx.coroutines.delay(2000)
        navController.navigate("ludo_game/$mode/$players/$magic/$betIndex") {
            popUpTo("ludo_matching/$mode/$players/$magic/$betIndex") { inclusive = true }
        }
    }

    DisposableEffect(mode, players, magic, betIndex) {
        val listener: (ServerMessage) -> Unit = { msg ->
            when (msg) {
                is ServerMessage.Waiting -> {
                    // Ab yahan koi UI text nahi dikhate — sirf loading-gif hi
                    // kaafi hai, "1/2 players..." jaisa raw message ab nahi
                    // dikhaya jata.
                }
                is ServerMessage.Matched -> {
                    matchedMsg = msg
                }
                is ServerMessage.Err -> {
                    errorText = msg.message
                }
                is ServerMessage.ConnectionClosed -> {
                    connectionLost = true
                }
                is ServerMessage.ConnectionOpened -> {
                    if (connectionLost) {
                        // net wapis aa gaya — matchmaking queue mein dobara shamil ho jate hain
                        connectionLost = false
                        BackendClient.join(mode, bet, players, magic)
                    }
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
            // Upar koi bara "searching" gif/text nahi dikhate — searching-animation
            // ab seedha "Player" (opponent) slot ke andar dikhti hai (neeche
            // MatchPlayerSlot mein), taake ek hi cheez do jagah repeat na ho.
            // Sirf connection-drop ek alag/zaroori state hai isliye usay yahan
            // top par chhota banner ki tarah dikhate hain.
            if (connectionLost) {
                Icon(
                    Icons.Filled.WifiOff,
                    contentDescription = "no connection",
                    tint = Color(0xFFff6b6b),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Connection nahi hai — reconnect ki koshish ja rahi hai...",
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(20.dp))
            }
            Spacer(Modifier.height(10.dp))

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

            // Players — apna slot hamesha khud ki asal profile (naam+DP) dikhata
            // hai; baaki slots jab tak match nahi milta loading-gif dikhate hain,
            // match milte hi (matchedMsg) unki bhi asal naam+DP nazar aati hai.
            // (matchedMsg.players ki order mein "main" kahin bhi ho sakta hoon,
            // isliye apna color explicitly nikaal kar baaki sabko "opponents" list
            // banate hain — sirf index se maan lena galat hota.)
            val opponents = matchedMsg?.let { m -> m.players.filter { it != m.color }.mapNotNull(m.profiles::get) } ?: emptyList()
            if (players == 4) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 0 until 2) {
                            val opp = opponents.getOrNull(i - 1)
                            MatchPlayerSlot(
                                name = if (i == 0) BackendClient.myName.ifBlank { "Aap" } else (opp?.name ?: "Player"),
                                avatarUrl = if (i == 0) BackendClient.myAvatar else opp?.avatar,
                                isSearching = i != 0 && matchedMsg == null
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (i in 2 until 4) {
                            val opp = opponents.getOrNull(i - 1)
                            MatchPlayerSlot(
                                name = opp?.name ?: "Player",
                                avatarUrl = opp?.avatar,
                                isSearching = matchedMsg == null
                            )
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    for (i in 0 until players) {
                        val opp = opponents.getOrNull(i - 1)
                        MatchPlayerSlot(
                            name = if (i == 0) BackendClient.myName.ifBlank { "Aap" } else (opp?.name ?: "Player"),
                            avatarUrl = if (i == 0) BackendClient.myAvatar else opp?.avatar,
                            isSearching = i != 0 && matchedMsg == null
                        )
                        // "VS" sirf dono avatars ke theek beech mein (1v1 look) — waqfa
                        // (gap) horizontalArrangement.spacedBy se pehle hi mil jata hai.
                        if (players == 2 && i == 0) {
                            AsyncImage(model = VS_ICON_IMG, contentDescription = "VS", modifier = Modifier.size(40.dp))
                        }
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
private fun MatchPlayerSlot(name: String, avatarUrl: String?, isSearching: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            if (isSearching) {
                // Koi aur player dhoondte hue is "Player" slot ke andar hi ghoomta
                // hua loader dikhta hai — silhouette ke upar overlay ki tarah,
                // taake pata chale ke yahan abhi koi opponent dhoonda ja raha hai.
                AsyncImage(
                    model = DEFAULT_AVATAR_IMG,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.White.copy(alpha = 0.15f))
                )
                val infiniteTransition = rememberInfiniteTransition(label = "searching")
                val angle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                    label = "searchingAngle"
                )
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize().rotate(angle),
                    color = Color(0xFFffd93b),
                    strokeWidth = 3.dp
                )
            } else {
                AsyncImage(
                    model = avatarUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_AVATAR_IMG,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF143c64).copy(alpha = 0.75f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
