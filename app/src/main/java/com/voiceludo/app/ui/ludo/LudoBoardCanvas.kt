package com.voiceludo.app.ui.ludo

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

// Asal HTML jaisa hi board — wohi Checkerboard-duel background image, aur usi ke
// upar har token apni asal PNG/WEBP image (piece-green/yellow/blue/red) ke sath
// theek 15x15 grid ke row/col hisaab se position hota hai.
@Composable
fun LudoBoardCanvas(state: LudoGameState, onTokenTap: (LudoColor, Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val boardSizeDp = maxWidth
        val cellDp: Dp = boardSizeDp / 15f
        val tokenSizeDp: Dp = cellDp * 0.75f

        AsyncImage(
            model = GAME_BOARD_IMG,
            contentDescription = "board",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f)
        )

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
                AsyncImage(
                    model = TOKEN_IMG[color],
                    contentDescription = "$color token $i",
                    modifier = Modifier
                        .size(tokenSizeDp)
                        .offset(
                            x = cellDp * colF + (cellDp - tokenSizeDp) / 2,
                            y = cellDp * rowF + (cellDp - tokenSizeDp) / 2
                        )
                )
            }
        }
    }
}
