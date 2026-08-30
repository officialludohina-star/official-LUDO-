package com.voiceludo.app.ui.ludo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random

// pos: -1 = yard, 0..50 = ring (color ke apne start se relative), 51..55 = home-stretch,
// 56 = finish (ghar pahunch gaya)
class LudoGameState(val mode: LudoMode, val players: List<LudoColor>) {

    val tokens = mutableStateMapOf<LudoColor, MutableList<Int>>().apply {
        players.forEach { c -> put(c, mutableStateListOf(-1, -1, -1, -1)) }
    }

    var currentIdx = mutableStateOf(0)
    var dice = mutableStateOf(1)
    var diceRolled = mutableStateOf(false)
    // Asal HTML ke dice-roll-animation.gif jaisa hi rolling state — UI isay dekh kar
    // gif dikhata hai, phir 700ms baad asal number reveal hota hai
    var isRolling = mutableStateOf(false)
    var movable = mutableStateListOf<Int>()
    var gameOver = mutableStateOf(false)
    var winnerText = mutableStateOf("")
    var sixStreak = 0

    // 4-player mode mein rank-order track karta hai — 1st/2nd/3rd finish karne wale
    // yahan record hote hain; game tab hi khatam hota hai jab last banda bhi tay ho jaye.
    val finishOrder = mutableStateListOf<LudoColor>()
    val rankBadge = mutableStateMapOf<LudoColor, Int>() // color -> rank (1,2,3)

    val currentColor: LudoColor get() = players[currentIdx.value]

    private val arrowTails = arrowTailSet()

    fun rollDice() {
        if (gameOver.value) return
        dice.value = Random.nextInt(1, 7)
        diceRolled.value = true
        movable.clear()
        movable.addAll(computeMovable(currentColor, dice.value))
        if (movable.isEmpty()) {
            // koi chaal nahi — turn khud aage badha do
            advanceTurn(extra = dice.value == 6)
        }
    }

    fun computeMovable(color: LudoColor, dv: Int): List<Int> {
        val t = tokens.getValue(color)
        val result = mutableListOf<Int>()
        for (i in t.indices) {
            val p = t[i]
            if (p == -1) {
                if (dv == 6) result.add(i)
            } else if (p in 0..50) {
                if (p + dv <= 56) result.add(i)
            } else if (p in 51..55) {
                if (p + dv <= 56) result.add(i)
            }
        }
        return result
    }

    fun globalCellOf(color: LudoColor, pos: Int): Int =
        (COLOR_META.getValue(color).start + pos) % 52

    // Token move karta hai; capture/safe-cell/arrow/quick-block rules apply karta hai,
    // phir checkWin karta hai aur agla turn set karta hai.
    fun moveToken(tokenIdx: Int) {
        if (gameOver.value || !diceRolled.value) return
        val color = currentColor
        val t = tokens.getValue(color)
        val dv = dice.value
        var newPos = if (t[tokenIdx] == -1) 0 else t[tokenIdx] + dv
        if (newPos > 56) return

        var captured = false

        if (newPos in 0..50) {
            val g = globalCellOf(color, newPos)

            // Quick mode: color ka apna block cell aa jaye to seedha finish
            if (mode == LudoMode.QUICK && newPos == QUICK_BLOCK_REL) {
                newPos = 56
            }
            // Arrow mode: apni exit-arm ka tail cell -> seedha head cell tak chala jata hai
            else if (mode == LudoMode.ARROW && newPos == ARROW_TAIL_OFFSET) {
                newPos = ARROW_HEAD_OFFSET
            }

            // Capture check (safe cells par capture nahi hota)
            if (newPos in 0..50 && g !in SAFE_SET.map { it }) {
                for (oc in players) {
                    if (oc == color) continue
                    val ot = tokens.getValue(oc)
                    for (j in ot.indices) {
                        val op = ot[j]
                        if (op in 0..50 && globalCellOf(oc, op) == g) {
                            ot[j] = -1
                            captured = true
                        }
                    }
                }
            }
        }

        t[tokenIdx] = newPos
        movable.clear()
        diceRolled.value = false

        val wonThisColor = checkWin(color)
        if (gameOver.value) return

        val extra = dv == 6 || captured
        advanceTurn(extra)
    }

    // Diya gaya color jeet chuka ya nahi check karta hai. Quick mode: 1 token finish =
    // jeet (baaki tokens khud-b-khud yard mein wapis chale jate hain). Classic/Arrow/Master:
    // saare 4 tokens finish hone chahiye. 4-player mein rank track hota hai aur game tab tak
    // jaari rehta hai jab tak last player bhi decide na ho jaye.
    fun checkWin(color: LudoColor): Boolean {
        val t = tokens.getValue(color)
        val done = if (mode == LudoMode.QUICK) t.any { it == 56 } else t.all { it == 56 }
        if (!done) return false

        val already = finishOrder.contains(color)
        if (!already) finishOrder.add(color)

        if (!already && mode == LudoMode.QUICK) {
            for (i in t.indices) if (t[i] != 56) t[i] = -1
        }

        if (players.size == 4) {
            if (!already) rankBadge[color] = finishOrder.size
            if (finishOrder.size >= players.size - 1) {
                gameOver.value = true
                winnerText.value = "${finishOrder[0]} JEET GAYA! \uD83C\uDFC6"
            }
        } else {
            gameOver.value = true
            winnerText.value = "$color JEET GAYA! \uD83C\uDFC6"
        }
        return done
    }

    // Agla turn set karta hai — jo color pehle hi finish ho chuka hai (4P rank ke baad)
    // uska turn khud skip ho jata hai.
    fun advanceTurn(extra: Boolean) {
        diceRolled.value = false
        movable.clear()
        if (gameOver.value) return
        if (!extra) {
            var next = (currentIdx.value + 1) % players.size
            var guard = 0
            while (finishOrder.contains(players[next]) && guard < players.size) {
                next = (next + 1) % players.size
                guard++
            }
            currentIdx.value = next
            sixStreak = 0
        }
    }
}
