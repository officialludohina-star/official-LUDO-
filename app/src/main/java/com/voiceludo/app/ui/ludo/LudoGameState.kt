package com.voiceludo.app.ui.ludo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
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
    // Token move ke doraan true — asal HTML jaisa hi (koi ek waqt mein sirf ek hi token
    // "slide" karta hai). Isay UI mein tapping disable karne ke liye use karo, taake move
    // ke beech mein doosra tap na ho jaye.
    var isMoving = mutableStateOf(false)
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

    // Ek cell par capture-check karta hai (asal HTML ke captureAtCell jaisa hoobahoo):
    // safe cells (aur arrow-tail/head/edge spots) par kill nahi hota; aur agar kisi
    // opponent color ke 2+ tokens ek hi cell par "block" bana kar khare hon to woh
    // color kabhi capture nahi hoti — sirf akeli (1) token wali opponent color capture hoti hai.
    private fun captureAtCell(color: LudoColor, g: Int): Boolean {
        val isArrowSpot = mode == LudoMode.ARROW && (g in arrowTails || g in ARROW_EDGE_SET ||
            players.any { arrowHeadFor(it) == g })
        if (g in SAFE_SET && !isArrowSpot) return false

        val byColor = LinkedHashMap<LudoColor, MutableList<Int>>()
        for (oc in players) {
            if (oc == color) continue
            val ot = tokens.getValue(oc)
            for (j in ot.indices) {
                val op = ot[j]
                if (op in 0..50 && globalCellOf(oc, op) == g) {
                    byColor.getOrPut(oc) { mutableListOf() }.add(j)
                }
            }
        }
        var captured = false
        byColor.forEach { (oc, idxs) ->
            if (idxs.size >= 2) return@forEach // block — kabhi kill nahi hoti
            val ot = tokens.getValue(oc)
            idxs.forEach { j -> ot[j] = -1; captured = true }
        }
        return captured
    }

    // Token move karta hai — asal HTML jaisa hi ek-ek cell "slide" karke (step-by-step,
    // ~220ms/cell), taake real smooth motion dikhe (seedha purani se nai jagah jump/cut
    // nahi karta). Uske baad capture/safe-cell/arrow/quick-block rules apply karta hai,
    // phir checkWin karta hai aur agla turn set karta hai.
    suspend fun moveToken(tokenIdx: Int) {
        if (gameOver.value || !diceRolled.value || isMoving.value) return
        val color = currentColor
        val t = tokens.getValue(color)
        val dv = diceByColor.getValue(color)
        val wasInYard = t[tokenIdx] == -1
        val rawTarget = if (wasInYard) 0 else t[tokenIdx] + dv
        if (rawTarget > 56) return

        isMoving.value = true
        movable.clear()

        // ---- Step 1: cell-by-cell slide (yard se nikalna = 1 hi step, seedha start par) ----
        // Asal HTML ke stepOnce() jaisa hi: har cell 230ms mein slide hoti hai, aur
        // aakhri step ke baad capture/arrow checks se pehle 240ms ka thehrao hota hai.
        val steps = if (wasInYard) 1 else dv
        repeat(steps) {
            t[tokenIdx] = if (t[tokenIdx] == -1) 0 else t[tokenIdx] + 1
            delay(230)
        }
        delay(240)

        var newPos = t[tokenIdx]
        var captured = false
        var magicDiceBonus = false

        if (newPos in 0..50) {
            // Quick mode: color ka apna block cell aa jaye to seedha finish
            if (mode == LudoMode.QUICK && newPos == QUICK_BLOCK_REL) {
                newPos = 56
                t[tokenIdx] = newPos
            }
            // Arrow mode: apni exit-arm ka tail cell ya apna edge cell -> turant warp
            else if (mode == LudoMode.ARROW) {
                val g0 = globalCellOf(color, newPos)
                val isTail = g0 in arrowTails
                val isOwnEdge = g0 in ARROW_EDGE_SET && ARROW_EDGE_OWNER[g0] == color
                if (isTail || isOwnEdge) {
                    delay(160) // asal HTML jaisa hi thora "poof" pause
                    newPos = if (isTail) newPos + (ARROW_HEAD_OFFSET - ARROW_TAIL_OFFSET) else ARROW_EDGE_ENTRY_REL
                    t[tokenIdx] = newPos
                    delay(160)
                }
            }

            // Magic mode: golden-dice cell -> agli roll seedha 6; rocket cell -> 1-15 ghar boost
            if (magicOn && newPos in 0..50) {
                val g = globalCellOf(color, newPos)
                if (g in magicDiceCells) {
                    bonusSix[color] = true
                    magicDiceBonus = true
                    relocateMagicCell(magicDiceCells, g)
                } else if (g in magicRocketCells) {
                    val boost = Random.nextInt(1, 16) // 1 se 15 tak
                    val maxAdd = minOf(boost, 56 - newPos)
                    relocateMagicCell(magicRocketCells, g)
                    repeat(maxAdd) {
                        newPos += 1
                        t[tokenIdx] = newPos
                        delay(90)
                    }
                }
            }

            // Capture check (safe cells par capture nahi hota, block wali color kabhi nahi)
            if (newPos in 0..50) {
                val g = globalCellOf(color, newPos)
                if (captureAtCell(color, g)) captured = true
            }
        }

        t[tokenIdx] = newPos
        movable.clear()
        diceRolled.value = false
        isMoving.value = false

        checkWin(color)
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
