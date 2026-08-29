package com.voiceludo.app.ui.ludo

// ==== Ludo board ka poora data model — HTML/JS wale asal game se hi hoobahoo liya gaya
// hai (RING coordinates, har color ka start/stretch/yard, safe cells, arrow cells waghera)
// taake board ka layout aur rules bilkul pehle jaisay hi rahein, sirf ab yeh Kotlin mein hai.

enum class LudoColor { RED, GREEN, YELLOW, BLUE }

data class RC(val row: Int, val col: Int)

// 15x15 grid ka bahar wala ring — 52 cells, jahan se sab tokens guzarte hain
val RING: List<RC> = listOf(
    RC(6,1), RC(6,2), RC(6,3), RC(6,4), RC(6,5),
    RC(5,6), RC(4,6), RC(3,6), RC(2,6), RC(1,6), RC(0,6),
    RC(0,7),
    RC(0,8), RC(1,8), RC(2,8), RC(3,8), RC(4,8), RC(5,8),
    RC(6,9), RC(6,10), RC(6,11), RC(6,12), RC(6,13), RC(6,14),
    RC(7,14),
    RC(8,14), RC(8,13), RC(8,12), RC(8,11), RC(8,10), RC(8,9),
    RC(9,8), RC(10,8), RC(11,8), RC(12,8), RC(13,8), RC(14,8),
    RC(14,7),
    RC(14,6), RC(13,6), RC(12,6), RC(11,6), RC(10,6), RC(9,6),
    RC(8,5), RC(8,4), RC(8,3), RC(8,2), RC(8,1), RC(8,0),
    RC(7,0),
    RC(6,0)
)

data class ColorMeta(
    val start: Int,
    val stretch: List<RC>,
    // yard token centers as % of board width/height (0..100), matches HTML layout
    val yard: List<Pair<Float, Float>>
)

val COLOR_META: Map<LudoColor, ColorMeta> = mapOf(
    LudoColor.GREEN to ColorMeta(
        start = 0,
        stretch = listOf(RC(7,1), RC(7,2), RC(7,3), RC(7,4), RC(7,5), RC(7,6)),
        yard = listOf(10.6f to 10.6f, 21.2f to 10.6f, 10.6f to 21.2f, 21.2f to 21.2f)
    ),
    LudoColor.YELLOW to ColorMeta(
        start = 13,
        stretch = listOf(RC(1,7), RC(2,7), RC(3,7), RC(4,7), RC(5,7), RC(6,7)),
        yard = listOf(70.8f to 10.6f, 81.4f to 10.6f, 70.8f to 21.2f, 81.4f to 21.2f)
    ),
    LudoColor.BLUE to ColorMeta(
        start = 26,
        stretch = listOf(RC(7,13), RC(7,12), RC(7,11), RC(7,10), RC(7,9), RC(7,8)),
        yard = listOf(70.8f to 70.8f, 81.4f to 70.8f, 70.8f to 81.4f, 81.4f to 81.4f)
    ),
    LudoColor.RED to ColorMeta(
        start = 39,
        stretch = listOf(RC(13,7), RC(12,7), RC(11,7), RC(10,7), RC(9,7), RC(8,7)),
        yard = listOf(10.6f to 70.8f, 21.2f to 70.8f, 10.6f to 81.4f, 21.2f to 81.4f)
    )
)

// Har color ka apna start + start se 8 aage wala star/safe square
val SAFE_SET: Set<Int> = setOf(0, 8, 13, 21, 26, 34, 39, 47)

val PLAYER_COLORS_2P = listOf(LudoColor.RED, LudoColor.YELLOW)
val PLAYER_COLORS_4P = listOf(LudoColor.RED, LudoColor.GREEN, LudoColor.YELLOW, LudoColor.BLUE)

// Arrow mode: har color ka apna exit-arm tail(ghar #5)/head(ghar #6)
const val ARROW_TAIL_OFFSET = 4
const val ARROW_HEAD_OFFSET = 5

fun arrowTailSet(): Set<Int> = COLOR_META.values.map { (it.start + ARROW_TAIL_OFFSET) % 52 }.toSet()
fun arrowHeadFor(color: LudoColor): Int = (COLOR_META.getValue(color).start + ARROW_HEAD_OFFSET) % 52

// Quick mode: har color ka apna block cell (relative offset), jahan se sirf usi color ka
// token guzar sakta hai — waha aa kar token seedha finish (56) ho jata hai.
const val QUICK_BLOCK_REL = 46

fun quickBlockCellFor(color: LudoColor): Int = (COLOR_META.getValue(color).start + QUICK_BLOCK_REL) % 52

enum class LudoMode { CLASSIC, ARROW, QUICK, MASTER }

// ==== Asal HTML mein jo image URLs use hoti thin, wahi yahan bhi — taake board,
// tokens, dice, rank badges sab bilkul waisay hi dikhein.
const val GAME_BG_IMG = "https://i.postimg.cc/qBFGBCmF/game-bg.webp"
const val GAME_BOARD_IMG = "https://i.postimg.cc/jjWBTmd2/Checkerboard-duel7bdc5231556b.png"

val TOKEN_IMG: Map<LudoColor, String> = mapOf(
    LudoColor.GREEN to "https://i.postimg.cc/sXXpsV5G/piece-green.webp",
    LudoColor.YELLOW to "https://i.postimg.cc/nVNwR5tV/piece-yellow.webp",
    LudoColor.BLUE to "https://i.postimg.cc/d0J2NR10/piece-bule.webp",
    LudoColor.RED to "https://i.postimg.cc/9z3TdZKM/piece-red.webp"
)

val DICE_FACE_IMG: Map<Int, String> = mapOf(
    1 to "https://i.postimg.cc/W3vGnbXh/1.png",
    2 to "https://i.postimg.cc/Hx9QPDNn/2.png",
    3 to "https://i.postimg.cc/kGjWqN9h/3.png",
    4 to "https://i.postimg.cc/T2QVVxmV/4.png",
    5 to "https://i.postimg.cc/tCThDct8/5.png",
    6 to "https://i.postimg.cc/fTrYPv8S/6.png"
)

// 4-player mein 1st/2nd/3rd finish karne walon ki profile par yehi rank badge lagta hai
val RANK_BADGE_IMG: Map<Int, String> = mapOf(
    1 to "https://i.postimg.cc/mkrk5s7b/room-icon-rank-crown.webp",
    2 to "https://i.postimg.cc/mrRbn4Gc/1000101592-removebg-preview.png",
    3 to "https://i.postimg.cc/3wJwRhbG/file-00000000229c81fd83b90d22a29f7bba.png"
)

const val DEFAULT_AVATAR_IMG = "https://i.postimg.cc/MK9xMWRc/user-icon.png"
const val MODE_2P_ICON = "https://i.postimg.cc/QdzJnZpV/game-online-2p-uncheck.png"
const val MODE_4P_ICON = "https://i.postimg.cc/FzhyJrQy/game-online-4p-uncheck.png"

// Top bar + settings panel ke icons (asal HTML se hoobahoo)
const val SETTINGS_ICON = "https://i.postimg.cc/y60TSpCD/system.webp"
const val BET_INFO_ICON = "https://i.postimg.cc/9Qm12dSP/file-000000000d188208a6a8a0921cdc4517.png"
const val SOUND_ICON = "https://i.postimg.cc/mr5tk4rG/room-ic-input-sound.webp"
const val EXIT_ICON = "https://i.postimg.cc/5NSyvLtn/btn-associate-exit.webp"

fun ludoColorOf(c: LudoColor): androidx.compose.ui.graphics.Color = when (c) {
    LudoColor.RED -> androidx.compose.ui.graphics.Color(0xFFE53935)
    LudoColor.GREEN -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
    LudoColor.YELLOW -> androidx.compose.ui.graphics.Color(0xFFF9A825)
    LudoColor.BLUE -> androidx.compose.ui.graphics.Color(0xFF1E88E5)
}
