package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// Asal HTML ke #ludoGameScreen jaisa hi — sab kuch (top icons, settings panel, board,
// har player ka profile+dice) HTML ke % left/top/right values ke mutabiq bilkul
// wahi jagah absolute-position hota hai, taake layout hoobahoo match kare.

private enum class CornerPos { POS_TL, POS_TR, POS_BL, POS_BR }

@Composable
fun LudoGameScreen(navController: NavController, mode: String, players: Int, magic: Boolean = false) {
    val ludoMode = LudoMode.valueOf(mode)
    val colorList = if (players == 4) PLAYER_COLORS_4P else PLAYER_COLORS_2P
    val state = remember { LudoGameState(ludoMode, colorList, magic) }

    // Bot turns ab alag se manage nahi karne parte — state.rollDice()/advanceTurn khud
    // hi (asal HTML ke maybeBotTurn jaisa) bot ki agli roll/chain automatically chala
    // dete hain, chahe extra-turn (capture/6) kitni hi baar mile.

    var showSettings by remember { mutableStateOf(false) }
    var soundOn by remember { mutableStateOf(true) }

    // Asal HTML: window.COLOR_TO_POS — 4P mein har color apne board-quadrant ke
    // corner par hi apna profile+dice dikhata hai; 2P mein self hamesha bottom-left,
    // doosra player top-right (fixed positions)
    val colorToPos = mapOf(
        LudoColor.GREEN to CornerPos.POS_TL,
        LudoColor.YELLOW to CornerPos.POS_TR,
        LudoColor.BLUE to CornerPos.POS_BR,
        LudoColor.RED to CornerPos.POS_BL
    )

    // #ludoGameScreen: position:fixed; inset:0; background game-bg.webp cover
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        AsyncImage(
            model = GAME_BG_IMG,
            contentDescription = "background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // .game-board-wrap: position:absolute; top:50%; left:50%; translate(-50%,-50%);
        // width:100vw; height:auto (square board image)
        val boardScope = rememberCoroutineScope()
        Box(modifier = Modifier.align(Alignment.Center).fillMaxWidth().aspectRatio(1f)) {
            LudoBoardCanvas(state) { _, idx ->
                if (state.currentIdx.value == 0 && !state.isMoving.value) {
                    boardScope.launch { state.tapToken(idx) }
                }
            }
        }

        // .game-top-icons-wrap: position:absolute; top:8px; left:0; width:100%; height:44px
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = 8.dp)
                .fillMaxWidth()
                .height(44.dp)
        ) {
            AsyncImage(
                model = TOP_BAR_BG_IMG,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 0.dp))
            )
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = SETTINGS_ICON, contentDescription = "settings",
                    modifier = Modifier.size(40.dp).clickable { showSettings = !showSettings }
                )
                AsyncImage(
                    model = BET_INFO_ICON, contentDescription = "bet info",
                    modifier = Modifier.size(44.dp).clickable { /* bet info popup */ }
                )
            }
        }

        // .game-settings-panel: position:absolute; top:56px; left:10px
        if (showSettings) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 10.dp, y = 56.dp)
                    .background(Color(0xF20F1E37), RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsOption(
                    icon = if (soundOn) SOUND_ICON else SOUND_OFF_ICON,
                    label = if (soundOn) "On" else "Off",
                    onClick = { soundOn = !soundOn }
                )
                SettingsOption(
                    icon = EXIT_ICON,
                    label = "Exit",
                    onClick = { navController.popBackStack() }
                )
            }
        }

        // .game-players-wrap: position:absolute; inset:0 — har player ka apna
        // profile + dice box, HTML ke pos-tl/pos-tr/pos-bl/pos-br corners ke mutabiq
        val positions: List<CornerPos> = if (players == 4) {
            colorList.map { colorToPos.getValue(it) }
        } else {
            listOf(CornerPos.POS_BL, CornerPos.POS_TR)
        }

        colorList.forEachIndexed { i, color ->
            val pos = positions[i]
            val isSelf = i == 0
            val name = if (isSelf) "Player" else "Player ${i + 1}"

            val spec = when (pos) {
                CornerPos.POS_BL -> PosSpec(1f, 79f, -8f, -10f, false)
                CornerPos.POS_TL -> PosSpec(1f, 10f, -6f, 12f, false)
                CornerPos.POS_TR -> PosSpec(1f, 10f, 2f, 16f, true)
                CornerPos.POS_BR -> PosSpec(1f, 79f, 6f, -14f, true)
            }

            val xOffset = screenW * (spec.baseX / 100f) + spec.translateX.dp
            val yOffset = screenH * (spec.baseY / 100f) + spec.translateY.dp
            val cornerAlign = if (spec.alignEnd) Alignment.TopEnd else Alignment.TopStart
            val xForOffset = if (spec.alignEnd) -xOffset else xOffset

            Row(
                modifier = Modifier
                    .align(cornerAlign)
                    .offset(x = xForOffset, y = yOffset),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val profile = @Composable {
                    PlayerProfileBox(name = name, color = color, rank = state.rankBadge[color])
                }
                val dice = @Composable {
                    val scope = rememberCoroutineScope()
                    Box {
                        PlayerDiceBox(
                            // Har player apna alag stored dice-value dikhata hai (HTML jaisa) —
                            // rolling-gif sirf usi color ke box mein chalti hai jiski abhi turn hai.
                            diceValue = state.diceByColor[color] ?: 1,
                            rolling = state.isRolling.value && color == state.currentColor,
                            isClickable = isSelf,
                            // rollDice() khud rolling-animation, chain (6/capture/etc) sab
                            // sambhalta hai — bas movable khali honi chahiye (koi move pending na ho)
                            enabled = isSelf && !state.gameOver.value && state.currentIdx.value == 0 &&
                                state.movable.isEmpty() && !state.isRolling.value && !state.isMoving.value,
                            onClick = { scope.launch { state.rollDice() } }
                        )
                        // Asal HTML ke gameDiceNum_${idx} badge jaisa — is turn mein ab tak
                        // jama hue saved-rolls numbers ("6+4" jaisay) dice-box ke upar dikhata hai
                        if (color == state.currentColor && state.savedRolls.isNotEmpty()) {
                            Text(
                                state.savedRolls.joinToString("+"),
                                color = Color(0xFF5A3100),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-8).dp)
                                    .background(
                                        Brush.verticalGradient(listOf(Color(0xFFFFEC66), Color(0xFFFFB300))),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                // Player 3 (pos-tr) aur Player 4 (pos-br) ke liye dice pehle, profile baad me
                if (pos == CornerPos.POS_TR || pos == CornerPos.POS_BR) {
                    dice(); profile()
                } else {
                    profile(); dice()
                }
            }
        }

        // Movable tokens ke liye instruction text (self ka turn ho aur chaal available ho)
        if (state.currentIdx.value == 0 && state.movable.isNotEmpty() && !state.gameOver.value) {
            Text(
                "Chalne wala token tap karein",
                color = Color.White,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        // Asal HTML ke #rollChoicePopup jaisa — jab ek movable token ke liye ek se
        // zyada ALAG saved number (jaisay 6 aur 4 dono) legal hon, player yahan se
        // chuney ke kaunsa number is token par apply karna hai.
        state.rollChoice.value?.let { choice ->
            val popupScope = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
                    .background(Color(0xF20A1423), RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFFFCC33), RoundedCornerShape(10.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                choice.options.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.verticalGradient(listOf(Color(0xFFFFEC66), Color(0xFFFFB300))))
                            .clickable { popupScope.launch { state.chooseRoll(opt) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${opt.value}", color = Color(0xFF5A3100), fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                }
            }
        }

        if (state.gameOver.value) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(state.winnerText.value, color = Color.Yellow, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { navController.popBackStack() }) { Text("Wapis Mode Select") }
            }
        }
    }
}

private data class PosSpec(val baseX: Float, val baseY: Float, val translateX: Float, val translateY: Float, val alignEnd: Boolean)

@Composable
private fun SettingsOption(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(model = icon, contentDescription = label, modifier = Modifier.size(22.dp))
        }
        Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 3.dp))
    }
}

// .game-info-box: width:66px — profile pic (44dp circle) + name label
@Composable
private fun PlayerProfileBox(name: String, color: LudoColor, rank: Int?) {
    Column(
        modifier = Modifier.width(66.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            AsyncImage(
                model = DEFAULT_AVATAR_IMG,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(ludoColorOf(color))
            )
            if (rank != null) {
                AsyncImage(
                    model = RANK_BADGE_IMG[rank],
                    contentDescription = "rank $rank",
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-6).dp)
                )
            }
        }
        Text(
            name,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp).width(66.dp)
        )
    }
}

// .game-dice-box: 50x50, rgba white 0.12 bg, 10dp rounded corners
@Composable
private fun PlayerDiceBox(diceValue: Int, rolling: Boolean, isClickable: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .then(if (isClickable) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        // Asal HTML ke ".game-dice-box img.rolling { transform:scale(1.5); }" jaisa hi —
        // rolling ke dauran dice image 1.5x badi dikhti hai
        AsyncImage(
            model = if (rolling) DICE_ROLL_GIF else DICE_FACE_IMG[diceValue],
            contentDescription = "dice",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = if (rolling) 1.5f else 1f, scaleY = if (rolling) 1.5f else 1f)
        )
    }
}
