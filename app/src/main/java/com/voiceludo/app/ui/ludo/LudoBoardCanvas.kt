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
//
// NOTE (HTML se hoobahoo match): asal HTML mein board artwork ke 15x15 playing-grid
// ke chaaron taraf 1% ka margin hai (window.BOARD_INSET_X/Y = 1), yani grid poore
// board ke 100% mein nahi balke beech wale 98% mein fit hoti hai (window.BOARD_SPAN_X/Y
// = 98, LUDO_CELL_X/Y = 98/15). Isi liye neeche bhi wahi inset + cell-size formula
// use ho raha hai — warna tokens board artwork ki asal cell-lines se thora hat kar
// (edge ki taraf drift karke) dikhtay.
private const val BOARD_INSET_PCT = 1f
private const val BOARD_SPAN_PCT = 100f - (BOARD_INSET_PCT * 2f)
private const val CELL_PCT = BOARD_SPAN_PCT / 15f // ek cell, board ka % (HTML ke LUDO_CELL_X/Y jaisa)

// Yard/base mein khare token ki size — cell-size ka 115%, HTML ke YARD_TOKEN_SIZE=115 se hoobahoo
private const val YARD_TOKEN_SIZE_PCT = 115f
// Yard slot ka flat +dx/+dy adjustment (board %), HTML ke YARD_SOCKET_ADJUST.slots se hoobahoo
// (chaaron slots ka adjustment barabar hai: dx=3.0, dy=3.0)
private const val YARD_ADJUST = 3f

// Path (ring/stretch) par jitne tokens ek hi cell mein stacked hon, unke hisaab se size aur
// offset — HTML ke window.STACK_CONFIG se hoobahoo (1 token=100% size center mein, 2=88% thora
// upar-neeche, waghera, taake ek cell mein kai tokens ek dusray ko poora chupayen nahi).
private data class StackCfg(val sizePct: Float, val offsets: List<Pair<Float, Float>>)

private val STACK_CONFIG: Map<Int, StackCfg> = mapOf(
    1 to StackCfg(100f, listOf(50f to 50f)),
    2 to StackCfg(88f, listOf(40f to 40f, 60f to 60f)),
    3 to StackCfg(80f, listOf(36f to 36f, 50f to 50f, 64f to 64f)),
    4 to StackCfg(74f, listOf(34f to 34f, 46f to 46f, 58f to 58f, 70f to 70f))
)

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

// Ek path token (ring ya stretch par) ka poora record — render se pehle group/stack
// karne ke liye zaroori sari info isi mein.
private data class PathToken(
    val color: LudoColor,
    val idx: Int,
    val row: Int,
    val col: Int,
    val cellKey: String
)

@Composable
fun LudoBoardCanvas(state: LudoGameState, onTokenTap: (LudoColor, Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        val boardSizeDp = maxWidth
        // Board ka 1 cell — HTML jaisa hi 1% inset ke sath (poore board ka 15waan hissa nahi)
        val cellDp: Dp = boardSizeDp * (CELL_PCT / 100f)

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

        // Magic mode: golden-dice aur rocket bonus cells (asal HTML ke #magicIconsLayer se
        // hoobahoo — .magic-icon { width:6%; height:6% }, cell ke exact center par)
        if (state.magicOn) {
            fun magicCenterOffsets(g: Int, iconSizeDp: Dp): Pair<Dp, Dp> {
                val rc = RING[g]
                val cellLeftDp = boardSizeDp * (BOARD_INSET_PCT / 100f) + cellDp * rc.col
                val cellTopDp = boardSizeDp * (BOARD_INSET_PCT / 100f) + cellDp * rc.row
                val centerX = cellLeftDp + cellDp / 2
                val centerY = cellTopDp + cellDp / 2
                return (centerX - iconSizeDp / 2) to (centerY - iconSizeDp / 2)
            }
            val magicIconSizeDp = boardSizeDp * 0.06f
            state.magicDiceCells.forEach { g ->
                val (x, y) = magicCenterOffsets(g, magicIconSizeDp)
                AsyncImage(
                    model = MAGIC_DICE_ICON,
                    contentDescription = "magic dice",
                    modifier = Modifier.size(magicIconSizeDp).offset(x = x, y = y)
                )
            }
            state.magicRocketCells.forEach { g ->
                val (x, y) = magicCenterOffsets(g, magicIconSizeDp)
                AsyncImage(
                    model = MAGIC_ROCKET_ICON,
                    contentDescription = "magic rocket",
                    modifier = Modifier.size(magicIconSizeDp).offset(x = x, y = y)
                )
            }
        }

        // ---- Pehla pass: sab tokens ko yard vs path mein baant kar, path walon ko
        // cell-key (HTML ke 'r'+g / color+'s'+pos jaisa) se group kar lete hain, taake
        // ek hi cell mein kitne tokens hain uske hisaab se STACK_CONFIG lagay (HTML jaisa).
        val pathGroups = LinkedHashMap<String, MutableList<PathToken>>()

        state.players.forEach { color ->
            state.tokens.getValue(color).forEachIndexed { i, pos ->
                if (pos in 0..56) {
                    val (rc, key) = when {
                        pos in 0..50 -> {
                            val g = (COLOR_META.getValue(color).start + pos) % 52
                            RING[g] to "r$g"
                        }
                        else -> {
                            // 51..55 = stretch cells, 56 = finish -> HTML jaisa stretch[5] par hi rehta hai
                            val stretchIdx = (pos - 51).coerceIn(0, 5)
                            COLOR_META.getValue(color).stretch[stretchIdx] to "${color}s$pos"
                        }
                    }
                    pathGroups.getOrPut(key) { mutableListOf() }.add(PathToken(color, i, rc.row, rc.col, key))
                }
            }
        }

        // ---- Path/stretch tokens: HTML jaisa hi per-cell wrapper + stack offsets
        pathGroups.values.forEach { group ->
            val total = group.size.coerceAtMost(4)
            val cfg = STACK_CONFIG.getValue(total)
            val first = group.first()
            // Cell box ka top-left corner (HTML ke wrap.style.left/top jaisa)
            val cellLeftDp = boardSizeDp * (BOARD_INSET_PCT / 100f) + cellDp * first.col
            val cellTopDp = boardSizeDp * (BOARD_INSET_PCT / 100f) + cellDp * first.row

            group.forEachIndexed { n, t ->
                val off = cfg.offsets[n % cfg.offsets.size]
                val tokenSizeDp = cellDp * (cfg.sizePct / 100f)
                val centerX = cellLeftDp + cellDp * (off.first / 100f)
                val centerY = cellTopDp + cellDp * (off.second / 100f)
                val targetX = centerX - tokenSizeDp / 2
                val targetY = centerY - tokenSizeDp / 2

                val isMovable = t.color == state.currentColor &&
                    state.currentIdx.value == 0 &&
                    t.idx in state.movable

                val animX by animateDpAsState(targetValue = targetX, animationSpec = tween(220), label = "tokenX")
                val animY by animateDpAsState(targetValue = targetY, animationSpec = tween(220), label = "tokenY")

                AsyncImage(
                    model = TOKEN_IMG[t.color],
                    contentDescription = "${t.color} token ${t.idx}",
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
                        .clickable(enabled = isMovable) { onTokenTap(t.color, t.idx) }
                )
            }
        }

        // ---- Yard (base) tokens: HTML jaisa hi cell-size ka 115%, yard-socket +3%/+3% adjustment ke sath
        state.players.forEach { color ->
            val list = state.tokens.getValue(color)
            list.forEachIndexed { i, pos ->
                if (pos != -1) return@forEachIndexed
                val (yx, yy) = COLOR_META.getValue(color).yard[i]
                val leftPct = BOARD_INSET_PCT + (yx / 100f) * BOARD_SPAN_PCT + YARD_ADJUST
                val topPct = BOARD_INSET_PCT + (yy / 100f) * BOARD_SPAN_PCT + YARD_ADJUST
                val tokenSizeDp = cellDp * (YARD_TOKEN_SIZE_PCT / 100f)
                val centerX = boardSizeDp * (leftPct / 100f)
                val centerY = boardSizeDp * (topPct / 100f)
                val targetX = centerX - tokenSizeDp / 2
                val targetY = centerY - tokenSizeDp / 2

                val isMovable = color == state.currentColor &&
                    state.currentIdx.value == 0 &&
                    i in state.movable

                val animX by animateDpAsState(targetValue = targetX, animationSpec = tween(220), label = "yardTokenX")
                val animY by animateDpAsState(targetValue = targetY, animationSpec = tween(220), label = "yardTokenY")

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
