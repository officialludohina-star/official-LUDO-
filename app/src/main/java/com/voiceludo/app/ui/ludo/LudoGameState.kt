package com.voiceludo.app.ui.ludo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import kotlin.random.Random

// pos: -1 = yard, 0..50 = ring (color ke apne start se relative), 51..55 = home-stretch,
// 56 = finish (ghar pahunch gaya)
class LudoGameState(val mode: LudoMode, val players: List<LudoColor>, val magicOn: Boolean = false) {

    val tokens = mutableStateMapOf<LudoColor, MutableList<Int>>().apply {
        players.forEach { c -> put(c, mutableStateListOf(-1, -1, -1, -1)) }
    }

    var currentIdx = mutableStateOf(0)
    // Asal HTML jaisa hi — har player ka apna alag dice-box/number hota hai (gameDiceImg_${idx}),
    // isliye ye ab per-color map hai, ek shared value nahi. Warna jab bhi koi bhi roll karta,
    // saare players ke dice-box ek sath badal jate the (dono "roll ho rahe" jaise dikhte the).
    val diceByColor = mutableStateMapOf<LudoColor, Int>().apply { players.forEach { put(it, 1) } }
    var diceRolled = mutableStateOf(false)
    // Asal HTML ke dice-roll-animation.gif jaisa hi rolling state — UI isay dekh kar
    // gif dikhata hai, phir 700ms baad asal number reveal hota hai. Sirf current player
    // ka box hi rolling dikhata hai (isRolling ke sath currentColor bhi check hota hai).
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
    val dice: Int get() = diceByColor.getValue(currentColor)

    private val arrowTails = arrowTailSet()

    // ---- Magic mode: asal HTML ke window.MAGIC_CELLS jaisa — 52 ring cells mein se
    // 3 dice-bonus aur 3 rocket-bonus cells random chuni jati hain (arrow tail/head
    // cells ke upar kabhi nahi aatin). Golden-dice cell par land karne se agli roll
    // seedha 6 milti hai; rocket cell se token 1-15 ghar tak aage boost hota hai.
    val magicDiceCells = mutableStateListOf<Int>()
    val magicRocketCells = mutableStateListOf<Int>()
    private val bonusSix = mutableMapOf<LudoColor, Boolean>()

    init {
        if (magicOn) {
            val arrowRelated = arrowTails + players.map { arrowHeadFor(it) }.toSet()
            val pool = (0..51).filterNot { it in arrowRelated }.shuffled()
            magicDiceCells.addAll(pool.take(3))
            magicRocketCells.addAll(pool.drop(3).take(3))
        }
    }

    private fun relocateMagicCell(list: MutableList<Int>, usedIdx: Int) {
        val used = (magicDiceCells + magicRocketCells).toSet()
        val arrowRelated = arrowTails + players.map { arrowHeadFor(it) }.toSet()
        val pool = (0..51).filterNot { it in used || it in arrowRelated }
        if (pool.isEmpty()) return
        val newIdx = pool.random()
        val at = list.indexOf(usedIdx)
        if (at != -1) list[at] = newIdx
    }

    fun rollDice() {
        if (gameOver.value) return
        val forcedSix = bonusSix[currentColor] == true
        val roll = if (forcedSix) 6 else Random.nextInt(1, 7)
        if (forcedSix) bonusSix[currentColor] = false
        diceByColor[currentColor] = roll
        diceRolled.value = true
        movable.clear()
        movable.addAll(computeMovable(currentColor, roll))
        if (movable.isEmpty()) {
            // koi chaal nahi — turn khud aage badha do
            advanceTurn(extra = roll == 6)
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
        val dv = diceByColor.getValue(color)
        var newPos = if (t[tokenIdx] == -1) 0 else t[tokenIdx] + dv
        if (newPos > 56) return

        var captured = false
        var magicDiceBonus = false

        if (newPos in 0..50) {
            var g = globalCellOf(color, newPos)

            // Quick mode: color ka apna block cell aa jaye to seedha finish
            if (mode == LudoMode.QUICK && newPos == QUICK_BLOCK_REL) {
                newPos = 56
            }
            // Arrow mode: apni exit-arm ka tail cell -> seedha head cell tak chala jata hai
            else if (mode == LudoMode.ARROW && newPos == ARROW_TAIL_OFFSET) {
                newPos = ARROW_HEAD_OFFSET
            }

            // Magic mode: golden-dice cell -> agli roll seedha 6; rocket cell -> 1-15 ghar boost
            if (magicOn && newPos in 0..50) {
                g = globalCellOf(color, newPos)
                if (g in magicDiceCells) {
                    bonusSix[color] = true
                    magicDiceBonus = true
                    relocateMagicCell(magicDiceCells, g)
                } else if (g in magicRocketCells) {
                    val boost = Random.nextInt(1, 16) // 1 se 15 tak
                    val maxAdd = minOf(boost, 56 - newPos)
                    relocateMagicCell(magicRocketCells, g)
                    if (maxAdd > 0) newPos += maxAdd
                }
            }

            // Capture check (safe cells par capture nahi hota)
            g = globalCellOf(color, newPos)
            if (newPos in 0..50 && g !in SAFE_SET) {
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

        val extra = dv == 6 || captured || magicDiceBonus
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
