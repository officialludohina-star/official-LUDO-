package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

// Asal "#ludoModeScreen" ka hoobahoo design — tabs (1 ON 1 / 4 Players), Select Mode
// card (Classic/Arrow/Quick/Master), Select Amount card (bet +/-, total pool), Magic
// toggle, aur Start button. Rangeen gradients/icons HTML se seedhe copy kiye gaye hain.
@Composable
fun LudoModeSelectScreen(navController: NavController) {
    var mode by remember { mutableStateOf(LudoMode.CLASSIC) }
    var players by remember { mutableStateOf(2) }
    var betIndex by remember { mutableStateOf(0) }
    var magicOn by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val bet = BET_OPTIONS[betIndex]
    val totalPool = bet * players

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0a1a2a))) {
        AsyncImage(
            model = MODE_SCREEN_BG_IMG,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 20.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TABS: 1 ON 1 / 4 Players
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ModeTab(
                    iconUrl = MODE_2P_ICON, label = "1 ON 1",
                    active = players == 2, onClick = { players = 2 }
                )
                ModeTab(
                    iconUrl = MODE_4P_ICON, label = "4 Players",
                    active = players == 4, onClick = { players = 4 }
                )
            }

            // SELECT MODE CARD
            LudoCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Text("Select Mode", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .clickable { showHelp = true },
                        contentAlignment = Alignment.Center
                    ) { Text("?", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeButton("Classic", mode == LudoMode.CLASSIC, Modifier.weight(1f)) { mode = LudoMode.CLASSIC }
                    ModeButton("Arrow", mode == LudoMode.ARROW, Modifier.weight(1f), showHot = true) { mode = LudoMode.ARROW }
                    ModeButton("Quick", mode == LudoMode.QUICK, Modifier.weight(1f)) { mode = LudoMode.QUICK }
                    ModeButton("Master", mode == LudoMode.MASTER, Modifier.weight(1f)) { mode = LudoMode.MASTER }
                }
            }

            // SELECT AMOUNT CARD
            LudoCard {
                Text(
                    "Select Amount", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = if (betIndex <= 0) BET_MINUS_ICON_DISABLED else BET_MINUS_ICON,
                        contentDescription = "kam karo",
                        modifier = Modifier.size(40.dp).clickable {
                            if (betIndex > 0) betIndex--
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        AsyncImage(model = COIN_ICON, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(bet.toString(), color = Color(0xFFffe066), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    AsyncImage(
                        model = if (betIndex >= BET_OPTIONS.lastIndex) BET_PLUS_ICON_DISABLED else BET_PLUS_ICON,
                        contentDescription = "zyada karo",
                        modifier = Modifier.size(40.dp).clickable {
                            if (betIndex < BET_OPTIONS.lastIndex) betIndex++
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Total Pool: $totalPool", color = Color(0xFFcce6ff), fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                // 4-player mein 1st (60%) / 2nd (40%) prize split, rank badge icons ke sath —
                // asal HTML ke renderPoolAndPrize() se
                if (players == 4) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(model = RANK_1_ICON, contentDescription = "1st prize", modifier = Modifier.size(20.dp))
                            Text((totalPool * 0.6).let { Math.round(it) }.toString(), color = Color(0xFF7fffbf), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(model = RANK_2_ICON, contentDescription = "2nd prize", modifier = Modifier.size(20.dp))
                            Text((totalPool * 0.4).let { Math.round(it) }.toString(), color = Color(0xFFffe066), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // MAGIC TOGGLE CARD
            LudoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { magicOn = !magicOn },
                        contentAlignment = Alignment.Center
                    ) { if (magicOn) Text("\u2713", color = Color(0xFF7fffbf), fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(10.dp))
                    AsyncImage(model = GOLDEN_DICE_ICON, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Magic", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // START BUTTON
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFffec66), Color(0xFFffb300))))
                    .clickable { navController.navigate("ludo_game/${mode.name}/$players") }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Start", color = Color(0xFF7a4a00), fontWeight = FontWeight.Black, fontSize = 22.sp)
            }
        }

        // CLOSE BUTTON
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

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("OK") } },
            text = { Text("Classic: Normal\nArrow: Fast\nQuick: Short\nMaster: Pro") }
        )
    }
}

@Composable
private fun ModeTab(iconUrl: String, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) Brush.verticalGradient(listOf(Color(0xFF3ad4ff), Color(0xFF1a7aff)))
                else Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.35f)))
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
    ) {
        AsyncImage(model = iconUrl, contentDescription = label, modifier = Modifier.fillMaxWidth().height(56.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LudoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 18.dp, start = 14.dp, end = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1a4a7a), Color(0xFF0f3a5a))))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        content = content
    )
}

@Composable
private fun ModeButton(label: String, active: Boolean, modifier: Modifier = Modifier, showHot: Boolean = false, onClick: () -> Unit) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (active) Brush.verticalGradient(listOf(Color(0xFF4ac8ff), Color(0xFF1a8aff)))
                    else Brush.verticalGradient(listOf(Color(0xFF2a8aff), Color(0xFF1860c0)))
                )
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp)
        ) {
            if (active) {
                AsyncImage(model = TICK_ICON, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                color = if (active) Color(0xFFfffc00) else Color(0xFFcce6ff),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
        if (showHot) {
            AsyncImage(
                model = HOT_BADGE_ICON, contentDescription = "hot",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-6).dp)
                    .size(20.dp)
            )
        }
    }
}
