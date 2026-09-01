package com.voiceludo.app.ui.ludo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import com.voiceludo.app.net.BackendClient
import com.voiceludo.app.net.GameEvent
import com.voiceludo.app.net.GameSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// pos: -1 = yard, 0..50 = ring (color ke apne start se relative), 51..55 = home-stretch,
// 56 = finish (ghar pahunch gaya)
//
// ============================================================================
// Yeh ab poori tarah BEKEND-DRIVEN hai — koi local bot/RNG/fake-player logic
// nahi bacha. Dice roll, token move, capture — sab kuch asal WebSocket bekend
// (BackendClient) par chalta hai; yeh class sirf us server-truth ko Compose UI
// ke liye animate/display karti hai (LudoGameScreen/LudoBoardCanvas isi ko
// dekh kar render karte hain, pehle jaisa hi).
// ============================================================================

// Ek saved-roll ke chip-popup mein dikhne wala option: savedRolls array ke andar ka
// index (jise apply karte waqt splice karna hai) + wo dice value jo user ko dikhti hai.
data class RollOption(val rollIndex: Int, val value: Int)

// Jab kisi movable token par tap ho aur us token ke liye ek se zyada ALAG numbers
// (jaisay saved 6 aur saved 5 dono) legally chal saktay hon, tab yeh popup dikhta hai
// taake player khud chuney ke kaunsa number is token par apply karna hai.
data class RollChoice(val tokenIdx: Int, val options: List<RollOption>)

class LudoGameState(
    val mode: LudoMode,
    val players: List<LudoColor>,
    val magicOn: Boolean,
    // Is device par jo actual insaan khel raha hai uska color — bekend ke "matched"
    // message se milta hai. Sirf isi color ke tokens/dice is device se control hote hain.
    val myColor: LudoColor
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    val tokens = mutableStateMapOf<LudoColor, MutableList<Int>>().apply {
        players.forEach { c -> put(c, mutableStateListOf(-1, -1, -1, -1)) }
    }

    var currentIdx = mutableStateOf(0)
    val diceByColor = mutableStateMapOf<LudoColor, Int>().apply { players.forEach { put(it, 1) } }
    var diceRolled = mutableStateOf(false)
    var isRolling = mutableStateOf(false)
    var isMoving = mutableStateOf(false)
    var movable = mutableStateListOf<Int>()
    var killerFlashPos = mutableStateOf<Int?>(null)
    var killedFlashPos = mutableStateOf<Int?>(null)

    var gameOver = mutableStateOf(false)
    var winnerText = mutableStateOf("")

    var savedRolls = mutableStateListOf<Int>()
    var rollChoice = mutableStateOf<RollChoice?>(null)

    val finishOrder = mutableStateListOf<LudoColor>()
    val rankBadge = mutableStateMapOf<LudoColor, Int>() // color -> rank (1,2,3)

    val currentColor: LudoColor get() = players[currentIdx.value]
    val dice: Int get() = diceByColor.getValue(currentColor)

    val magicDiceCells = mutableStateListOf<Int>()
    val magicRocketCells = mutableStateListOf<Int>()

    // ---- Server se aane wale "events"/"matched" messages yahan process hote hain ----

    // Naya match milte hi (koi dice roll hone se pehle) initial state seedha, bina
    // animation ke, apply karta hai.
    fun applyInitialSnapshot(snap: GameSnapshot) {
        syncFromSnapshot(snap)
    }

    // Server ke "events" message par LudoGameScreen isay call karta hai — events ko
    // ek ek karke animate karte hue play karta hai, phir final snapshot se sync kar
    // ke pakka karta hai ke state hoobahoo server jaisi hi hai.
    fun onServerEvents(events: List<GameEvent>, finalSnapshot: GameSnapshot) {
        scope.launch { playEvents(events, finalSnapshot) }
    }

    private suspend fun playEvents(events: List<GameEvent>, finalSnapshot: GameSnapshot) {
        for (e in events) {
            when (e.type) {
                "dice" -> {
                    val c = e.color?.let { runCatching { LudoColor.valueOf(it) }.getOrNull() } ?: continue
                    isRolling.value = true
                    delay(700)
                    isRolling.value = false
                    diceByColor[c] = e.value
                    diceRolled.value = true
                    delay(300)
                }
                "rollAgain" -> {
                    // Informational — savedRolls chain snapshot se hi sync ho jayegi
                }
                "awaitMove" -> {
                    movable.clear()
                    movable.addAll(e.movable)
                }
                "move" -> {
                    val c = e.color?.let { runCatching { LudoColor.valueOf(it) }.getOrNull() } ?: continue
                    animateMove(c, e)
                }
                "turn" -> {
                    val c = e.color?.let { runCatching { LudoColor.valueOf(it) }.getOrNull() }
                    if (c != null) {
                        currentIdx.value = players.indexOf(c).coerceAtLeast(0)
                        movable.clear()
                        rollChoice.value = null
                    }
                }
                "gameOver" -> {
                    gameOver.value = true
                    val winnerColor = e.winner?.let { runCatching { LudoColor.valueOf(it) }.getOrNull() }
                    winnerText.value = if (winnerColor != null) "$winnerColor JEET GAYA! \uD83C\uDFC6" else "Game khatam"
                }
            }
        }
        // Safety sync — chahe koi field upar miss ho gayi ho, yahan hamesha
        // server ki asal truth se poori tarah match ho jati hai.
        syncFromSnapshot(finalSnapshot)
    }

    private suspend fun animateMove(color: LudoColor, e: GameEvent) {
        val t = tokens.getValue(color)
        if (e.tokenIndex !in t.indices) return
        isMoving.value = true

        if (e.from == -1) {
            t[e.tokenIndex] = 0
            delay(230)
        } else {
            var pos = e.from
            while (pos < e.to) {
                pos += 1
                t[e.tokenIndex] = pos
                delay(150)
            }
        }
        t[e.tokenIndex] = e.to
        isMoving.value = false

        if (e.captured.isNotEmpty()) {
            val g = globalCellOf(color, e.to)
            killerFlashPos.value = g
            killedFlashPos.value = g
            delay(1200)
            killerFlashPos.value = null
            killedFlashPos.value = null
        } else if (e.arrowJumped) {
            delay(160)
        }
    }

    // Server ki Snapshot se poori local state (tokens/dice/turn/movable/saved-rolls/
    // gameOver/finish-order/rank) ko sach maan kar update karta hai.
    private fun syncFromSnapshot(snap: GameSnapshot) {
        players.forEach { c ->
            val serverPos = snap.tokens[c.name] ?: return@forEach
            val local = tokens.getValue(c)
            for (i in local.indices) {
                val newVal = serverPos.getOrElse(i) { local[i] }
                if (local[i] != newVal) local[i] = newVal
            }
        }
        players.forEach { c ->
            snap.diceByColor[c.name]?.let { diceByColor[c] = it }
        }
        val newIdx = players.indexOf(
            runCatching { LudoColor.valueOf(snap.currentColor) }.getOrNull() ?: currentColor
        )
        if (newIdx >= 0) currentIdx.value = newIdx

        savedRolls.clear()
        savedRolls.addAll(snap.savedRolls)
        movable.clear()
        movable.addAll(snap.movable)

        gameOver.value = snap.gameOver
        if (snap.gameOver && snap.winner != null && winnerText.value.isEmpty()) {
            winnerText.value = "${snap.winner} JEET GAYA! \uD83C\uDFC6"
        }

        finishOrder.clear()
        finishOrder.addAll(snap.finishOrder.mapNotNull { runCatching { LudoColor.valueOf(it) }.getOrNull() })
        rankBadge.clear()
        snap.rankBadge.forEach { (k, v) ->
            runCatching { LudoColor.valueOf(k) }.getOrNull()?.let { rankBadge[it] = v }
        }

        magicDiceCells.clear(); magicDiceCells.addAll(snap.magicDiceCells)
        magicRocketCells.clear(); magicRocketCells.addAll(snap.magicRocketCells)
    }

    // ---- UI se call hone wale actions — sab kuch seedha bekend ko bhej dete hain ----

    fun rollDice() {
        if (gameOver.value || movable.isNotEmpty() || currentColor != myColor) return
        BackendClient.roll()
    }

    // ---- UI se call hota hai jab self kisi movable token par tap kare ----
    fun tapToken(tokenIdx: Int) {
        if (currentColor != myColor || tokenIdx !in movable || isMoving.value) return
        val legalIdxs = legalRollsForToken(currentColor, tokenIdx)
        if (legalIdxs.isEmpty()) return
        val distinctValues = legalIdxs.map { savedRolls[it] }.distinct()
        if (distinctValues.size == 1) {
            BackendClient.move(tokenIdx, distinctValues[0])
        } else {
            val options = distinctValues.map { v -> RollOption(legalIdxs.first { savedRolls[it] == v }, v) }
            rollChoice.value = RollChoice(tokenIdx, options)
        }
    }

    // ---- UI se call hota hai jab self popup mein se koi ek number chune ----
    fun chooseRoll(option: RollOption) {
        val choice = rollChoice.value ?: return
        rollChoice.value = null
        BackendClient.move(choice.tokenIdx, option.value)
    }

    private fun legalRollsForToken(color: LudoColor, tokenIdx: Int): List<Int> {
        val pos = tokens.getValue(color)[tokenIdx]
        val legal = mutableListOf<Int>()
        savedRolls.forEachIndexed { i, dv ->
            if (pos == -1) {
                if (dv == 6) legal.add(i)
            } else if (pos + dv <= 56) {
                legal.add(i)
            }
        }
        return legal
    }

    fun globalCellOf(color: LudoColor, pos: Int): Int =
        (COLOR_META.getValue(color).start + pos) % 52
}
