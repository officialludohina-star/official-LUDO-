package com.voiceludo.app.ui.ludo

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

// Asal HTML jaisa hi board — wohi Checkerboard-duel background image, aur usi ke
// upar har token apni asal PNG/WEBP image (piece-green/yellow/blue/red) ke sath
// theek 15x15 grid ke row/col hisaab se position hota hai.

// Home-yard covers — board artwork ke andar bake hue 4 tokens ko hide karte hain,
// asal HTML ke .home-cover left%/top% values se hoobahoo (35% x 35%, har color ka apna)
private data class HomeCover(val leftPct: Float, val topPct: Float, val color: Color)

private val HOME_COVERS = listOf(
    HomeCover(2.4f, 2.4f, Color(0xFF1C883C)),   // green
    HomeCover(62.6f, 2.4f, Color(0xFFC39615)),  // yellow
    HomeCover(62.6f, 62.6f, Color(0xFF1C6BBA)), // blue
    HomeCover(2.4f, 62.6f, Color(0xFFB33123))   // red
)

// Arrow mode ke 4 curved + 4 center-diagonal arrow overlays — asal HTML ke
// left%/top%/width%/rotate() values se hoobahoo liye gaye hain
private data class ArrowSpot(val leftPct: Float, val topPct: Float, val widthPct: Float, val rotateDeg: Float)

private const val ARROW_CURVED_ICON = "file:///android_asset/img/file-00000000dc8082118a379ac2ac711ac3.png"

private val CURVED_ARROW_SPOTS = listOf(
    ArrowSpot(39f, -1f, 14f, 0f),
    ArrowSpot(84f, 36f, 14f, 89f),
    ArrowSpot(47f, 79f, 15f, 180f),
    ArrowSpot(2f, 43f, 15f, 270f)
)
private val CENTER_ARROW_SPOTS = listOf(
    ArrowSpot(33f, 33f, 14f, 267f),
    ArrowSpot(53f, 33f, 14f, 4f),
    ArrowSpot(52f, 53f, 15f, 90f),
    ArrowSpot(32f, 52f, 15f, 180f)
)
// Quick/Master mode ke 4 block-cell icons
private val BLOCK_ICON_SPOTS = listOf(
    ArrowSpot(42.6f, -1f, 15f, 0f),
    ArrowSpot(3f, 39.6f, 14f, 271f),
    ArrowSpot(82.2f, 38.6f, 15f, 89f),
    ArrowSpot(42.6f, 79.2f, 15f, 0f)
)

@Composable
fun LudoBoardCanvas(state: LudoGameState, onTokenTap: (LudoColor, Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val boardSizeDp = maxWidth
        val cellDp: Dp = boardSizeDp / 15f
        // Asal HTML ke .game-token { width:6%; height:6% } se hoobahoo — cell ka 0.75x
        // nahi, seedha board ka 6% (thoda chota, cell ke andar theek fit hota hai)
        val tokenSizeDp: Dp = boardSizeDp * 0.06f

        AsyncImage(
            model = GAME_BOARD_IMG,
            contentDescription = "board",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

        // Board artwork ke andar bake hue home-yard tokens ko hide karte hain (z-index:1,
        // tokens se neeche) — asal HTML ke .home-cover se hoobahoo
        HOME_COVERS.forEach { hc ->
            val coverSize = boardSizeDp * 0.35f
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .offset(
                        x = boardSizeDp * (hc.leftPct / 100f),
                        y = boardSizeDp * (hc.topPct / 100f)
                    )
                    .background(hc.color, CircleShape)
            )
        }

        // Arrow mode: curved + center diagonal arrow overlays
        if (state.mode == LudoMode.ARROW) {
            (CURVED_ARROW_SPOTS + CENTER_ARROW_SPOTS).forEach { spot ->
                val iconSize = boardSizeDp * (spot.widthPct / 100f)
                AsyncImage(
                    model = if (spot in CURVED_ARROW_SPOTS) ARROW_CURVED_ICON else ARROW_CENTER_ICON,
                    contentDescription = "arrow",
                    modifier = Modifier
                        .size(iconSize)
                        .offset(
                            x = boardSizeDp * (spot.leftPct / 100f),
                            y = boardSizeDp * (spot.topPct / 100f)
                        )
                        .graphicsLayer(rotationZ = spot.rotateDeg)
                )
            }
        }

        // Quick/Master mode: block-cell icons
        if (state.mode == LudoMode.QUICK || state.mode == LudoMode.MASTER) {
            BLOCK_ICON_SPOTS.forEach { spot ->
                val iconSize = boardSizeDp * (spot.widthPct / 100f)
                AsyncImage(
                    model = QUICK_BLOCK_ICON,
                    contentDescription = "block",
                    modifier = Modifier
                        .size(iconSize)
                        .offset(
                            x = boardSizeDp * (spot.leftPct / 100f),
                            y = boardSizeDp * (spot.topPct / 100f)
                        )
                        .graphicsLayer(rotationZ = spot.rotateDeg)
                )
            }
        }

        state.players.forEach { color ->
            val list = state.tokens.getValue(color)
            list.forEachIndexed { i, pos ->
                val (rowF, colF) = when {
                    pos == -1 -> {
                        val (px, py) = COLOR_META.getValue(color).yard[i]
                        // yard % (0-100) ko 15x15 grid ke row/col-equivalent mein convert
                        (py / 100f * 15f) to (px / 100f * 15f)
                    }
                    pos in 0..50 -> {
                        val g = (COLOR_META.getValue(color).start + pos) % 52
                        RING[g].row.toFloat() to RING[g].col.toFloat()
                    }
                    pos in 51..55 -> {
                        val p = COLOR_META.getValue(color).stretch[pos - 51]
                        p.row.toFloat() to p.col.toFloat()
                    }
                    else -> 7f to 7f // finished -> center ghar
                }
                // Asal HTML ke .game-token.movable jaisa hi glow — jab yeh token abhi
                // chalne ke qabil hai (current player ki apni chaal)
                val isMovable = color == state.currentColor &&
                    state.currentIdx.value == 0 &&
                    i in state.movable

                // Asal HTML ke .token-flyer jaisi hi smooth 220ms slide — cell-se-cell
                // jump nahi, tokan clean glide karta hai
                val targetX = cellDp * colF + (cellDp - tokenSizeDp) / 2
                val targetY = cellDp * rowF + (cellDp - tokenSizeDp) / 2
                val animX by animateDpAsState(targetValue = targetX, animationSpec = tween(220), label = "tokenX")
                val animY by animateDpAsState(targetValue = targetY, animationSpec = tween(220), label = "tokenY")

                AsyncImage(
                    model = TOKEN_IMG[color],
                    contentDescription = "$color token $i",
                    modifier = Modifier
                        .size(tokenSizeDp)
                        .offset(x = animX, y = animY)
                        .then(
                            if (isMovable) {
                                Modifier
                                    .shadow(6.dp, CircleShape, clip = false, ambientColor = Color(0xFFFFEC00), spotColor = Color(0xFFFFEC00))
                                    .background(Color(0x33FFEC00), CircleShape)
                            } else Modifier
                        )
                        .clickable(enabled = isMovable) { onTokenTap(color, i) }
                )
            }
        }
    }
}
