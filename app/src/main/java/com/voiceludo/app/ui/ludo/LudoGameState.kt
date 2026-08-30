package com.voiceludo.app.ui.ludo

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.delay
import kotlin.random.Random

// pos: -1 = yard, 0..50 = ring (color ke apne start se relative), 51..55 = home-stretch,
// 56 = finish (ghar pahunch gaya)

// Ek saved-roll ke chip-popup mein dikhne wala option: savedRolls array ke andar ka
// index (jise apply karte waqt splice karna hai) + wo dice value jo user ko dikhti hai.
data class RollOption(val rollIndex: Int, val value: Int)

// Jab kisi movable token par tap ho aur us token ke liye ek se zyada ALAG numbers
// (jaisay saved 6 aur saved 5 dono) legally chal saktay hon, tab yeh popup dikhta hai
// taake player khud chuney ke kaunsa number is token par apply karna hai.
data class RollChoice(val tokenIdx: Int, val options: List<RollOption>)

class LudoGameState(val mode: LudoMode, val players: List<LudoColor>, val magicOn: Boolean = false) {

    val tokens = mutableStateMapOf<LudoColor, MutableList<Int>>().apply {
        players.forEach { c -> put(c, mutableStateListOf(-1, -1, -1, -1)) }
    }

    var currentIdx = mutableStateOf(0)
    // Asal HTML jaisa hi — har player ka apna alag dice-box/number hota hai (gameDiceImg_${idx}),
    // isliye ye ab per-color map hai, ek shared value nahi. Warna jab bhi koi bhi roll karta,
    // saare players ke dice-box ek sath badal jate the (dono "roll ho rahe" jaise dikhte the).
    val diceByColor = mutableStateMapOf<LudoColor, Int>().apply { players.forEach { put(it, 1) } }
    // Chain mein kam se kam ek roll ho chuki hai aur uska number dikh raha hai (dice-box par)
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
    var killerFlashPos = mutableStateOf<Int?>(null)
    var killedFlashPos = mutableStateOf<Int?>(null)

    var gameOver = mutableStateOf(false)
    var winnerText = mutableStateOf("")

    // ---- Saved-rolls chain (asal HTML ke window.gameState.savedRolls jaisa hoobahoo) ----
    // Ek turn mein 6 aane par ya capture/arrow/reachedHome/magic-bonus ke baad number
    // "save" ho jata hai aur agli roll bhi isi chain mein jama hoti hai; player baad mein
    // in saare jama numbers ko kisi bhi apne token par (jahan legally chal sakein) apply
    // kar sakta hai. Yeh Classic/Quick/Master/Arrow — sab modes ka common turn-engine hai.
    var savedRolls = mutableStateListOf<Int>()
    // Popup jab ek token ke liye ek se zyada ALAG saved number legal hon
    var rollChoice = mutableStateOf<RollChoice?>(null)
    private var sixStreak = 0
    // Chain ke doraan kisi bhi move ne capture/arrow-jump/reachedHome/magic-bonus diya
    // ho to poori chain khatam hone par EXTRA TURN milta hai (currentIdx nahi badalta)
    private var chainCapture = false

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

    // Asal HTML ke window.weightedDiceRoll jaisa hi — 6 thora zyada aata hai (token
    // nikalna zaroori hai), 1 thora kam aata hai (mushkil). Weights total = 100.
    private fun weightedDiceRoll(): Int {
        val weights = mapOf(1 to 10, 2 to 16, 3 to 17, 4 to 17, 5 to 16, 6 to 24)
        var r = Random.nextDouble(0.0, 100.0)
        for (face in 1..6) {
            val w = weights.getValue(face)
            if (r < w) return face
            r -= w
        }
        return 6
    }

    // ---- Turn engine entry point — asal HTML ke window.rollDice jaisa hoobahoo ----
    // Sirf current player hi roll kar sakta hai, aur pehle se koi movable token pending
    // ho (chain ka move abhi baaki ho) to naya roll allow nahi.
    suspend fun rollDice() {
        if (gameOver.value) return
        if (movable.isNotEmpty()) return
        rollChoice.value = null

        isRolling.value = true
        delay(700)
        isRolling.value = false

        val color = currentColor
        val forcedSix = bonusSix[color] == true
        val result = if (forcedSix) 6 else weightedDiceRoll()
        if (forcedSix) bonusSix[color] = false
        diceByColor[color] = result
        diceRolled.value = true

        if (result == 6) {
            sixStreak++
            // Teesri lagataar chhakka: poori chain void, seedha turn skip (koi token nahi hilta)
            if (sixStreak >= 3) {
                sixStreak = 0
                savedRolls.clear()
                chainCapture = false
                delay(500)
                advanceTurn(false)
                return
            }
            // 6 sirf save hota hai — token bahar nikalne wala icon abhi nahi aata,
            // agli roll khud (tap ya bot khud-b-khud) karni hai (teesri baar tak).
            savedRolls.add(6)
            if (currentIdx.value != 0) {
                delay(600) // bot khud agli roll kar leta hai
                rollDice()
            }
            return
        }

        // Chhakka nahi aaya: chain yahin khatam hoti hai, ye number bhi save list mein jama ho jata hai
        sixStreak = 0
        savedRolls.add(result)
        delay(400) // pehle poora number thehar kar dikhay, uske baad hi movable icon aaye
        handleDiceResult()
    }

    // Ab tak jama saare savedRolls ke mutabiq kaunse tokens chal saktay hain, ye check
    // karta hai. Kuch bhi movable na ho to chain khud khatam ho kar turn aage badh jata
    // hai; warna human ka intezaar (tap) ya bot khud chain resolve kar leta hai.
    //
    // NOTE: agar sirf EK hi token movable hai aur uske liye bhi sirf EK hi number legal
    // hai (matlab koi real choice hi nahi hai — jaisay 6 se yard se nikalne ke baad
    // baaki bachi hui 3 sirf usi token par lag sakti hai), to human se dobara tap
    // maangna zaroori nahi — khud-b-khud chal jata hai (asal ludo games jaisa hi,
    // taake player ko na lagay ke "token aage nahi ja raha").
    private suspend fun handleDiceResult() {
        val color = currentColor
        val movableSet = mutableListOf<Int>()
        savedRolls.forEach { dv ->
            computeMovable(color, dv).forEach { ti -> if (ti !in movableSet) movableSet.add(ti) }
        }
        if (movableSet.isEmpty()) {
            savedRolls.clear()
            val extra = chainCapture
            chainCapture = false
            advanceTurn(extra)
            return
        }
        movable.clear()
        movable.addAll(movableSet)
        if (currentIdx.value != 0) {
            botResolveChain()
            return
        }
        if (movableSet.size == 1) {
            val onlyIdx = movableSet[0]
            val legalIdxs = legalRollsForToken(color, onlyIdx)
            val distinctValues = legalIdxs.map { savedRolls[it] }.distinct()
            if (distinctValues.size == 1) {
                delay(350) // thora pause taake player dekh le ke kaunsa token chalne wala hai
                applyRollToToken(onlyIdx, legalIdxs[0])
                return
            }
        }
        // warna: human ke tap ka intezaar — UI se tapToken()/chooseRoll() call hoga
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

    // Diya gaya token us waqt tak jama saved numbers mein se kaunse legally chal
    // sakta hai — savedRolls array ke andar ke indexes wapis karta hai.
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

    // ---- UI se call hota hai jab human (self) kisi movable token par tap kare ----
    suspend fun tapToken(tokenIdx: Int) {
        if (currentIdx.value != 0 || tokenIdx !in movable || isMoving.value) return
        val color = currentColor
        val legalIdxs = legalRollsForToken(color, tokenIdx)
        if (legalIdxs.isEmpty()) return
        val distinctValues = legalIdxs.map { savedRolls[it] }.distinct()
        if (distinctValues.size == 1) {
            // Sirf ek hi tarah ka number legal hai (chahe saved list mein ek se zyada
            // baar ho, jaisay do baar 6) — seedha chal do, popup ki zaroorat nahi
            applyRollToToken(tokenIdx, legalIdxs[0])
        } else {
            // Alag-alag numbers legal hain (jaisay 6 aur 5 dono) — token popup dikhao,
            // har distinct value ka ek hi option (duplicate values ke liye repeat nahi)
            val options = distinctValues.map { v -> RollOption(legalIdxs.first { savedRolls[it] == v }, v) }
            rollChoice.value = RollChoice(tokenIdx, options)
        }
    }

    // ---- UI se call hota hai jab human popup mein se koi ek number chune ----
    suspend fun chooseRoll(option: RollOption) {
        val choice = rollChoice.value ?: return
        rollChoice.value = null
        applyRollToToken(choice.tokenIdx, option.rollIndex)
    }

    // Ek saved number ko chosen token par apply karta hai, phir dekhta hai ke aur
    // numbers bache hain ya nahi — agar hain to unhe use karne ka mauqa deta hai,
    // warna turn khatam karta hai (ya extra turn deta hai agar chain mein capture/
    // arrow/reachedHome/magic-bonus mila ho).
    private suspend fun applyRollToToken(tokenIdx: Int, rollIdx: Int) {
        movable.clear()
        rollChoice.value = null
        val dv = savedRolls[rollIdx]
        savedRolls.removeAt(rollIdx)

        val extra = performMove(tokenIdx, dv)
        if (gameOver.value) return
        if (extra) chainCapture = true

        if (savedRolls.isNotEmpty()) {
            handleDiceResult()
        } else {
            val giveExtra = chainCapture
            chainCapture = false
            advanceTurn(giveExtra)
        }
    }

    // Bot apni saved-rolls chain khud hi ek ek karke use kar leta hai
    private suspend fun botResolveChain() {
        if (savedRolls.isEmpty()) {
            val extra = chainCapture
            chainCapture = false
            advanceTurn(extra)
            return
        }
        val color = currentColor
        var dv: Int? = null
        var ri = -1
        var cand: List<Int> = emptyList()
        for (i in savedRolls.indices) {
            val c = computeMovable(color, savedRolls[i])
            if (c.isNotEmpty()) {
                dv = savedRolls[i]; ri = i; cand = c
                break
            }
        }
        if (dv == null) {
            savedRolls.clear()
            val extra = chainCapture
            chainCapture = false
            advanceTurn(extra)
            return
        }
        movable.clear()
        val choice = botPickToken(color, cand, dv)
        savedRolls.removeAt(ri)

        val extra = performMove(choice, dv)
        if (gameOver.value) return
        if (extra) chainCapture = true
        botResolveChain()
    }

    // Bot kaunsa token chalaye — asal HTML ke window.botPickToken se hoobahoo: 6 aaye to
    // pehle yard se token nikalo; warna jo chaal kisi opponent ko capture kar sake wo chuno;
    // warna sabse aage nikla hua (sabse zyada advanced) token chalao.
    private fun botPickToken(color: LudoColor, movableIdxs: List<Int>, dv: Int): Int {
        val t = tokens.getValue(color)
        if (dv == 6) {
            val yardIdx = movableIdxs.firstOrNull { t[it] == -1 }
            if (yardIdx != null) return yardIdx
        }
        for (i in movableIdxs) {
            val posNow = t[i]
            if (posNow == -1) continue
            val newPos = posNow + dv
            if (newPos in 0..50) {
                val g = globalCellOf(color, newPos)
                if (g !in SAFE_SET) {
                    for (oc in players) {
                        if (oc == color) continue
                        val ot = tokens.getValue(oc)
                        if (ot.any { it in 0..50 && globalCellOf(oc, it) == g }) return i
                    }
                }
            }
        }
        return movableIdxs.maxByOrNull { t[it] } ?: movableIdxs.first()
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
            killerFlashPos.value = g
            killedFlashPos.value = g
        }
        return captured
    }

    // Token move karta hai — asal HTML jaisa hi ek-ek cell "slide" karke (step-by-step,
    // ~220ms/cell), taake real smooth motion dikhe (seedha purani se nai jagah jump/cut
    // nahi karta). Uske baad capture/safe-cell/arrow/quick-block rules apply karta hai,
    // phir checkWin karta hai. Wapis karta hai ke is move ne "extra" diya ya nahi
    // (capture/arrow-jump/reachedHome/magic-dice-bonus — asal HTML ke resolveMove ke
    // onDone(extraRoll) jaisa hoobahoo; sirf dv==6 hone se khud extra nahi milta, kyunke
    // 6 ka bonus pehle hi savedRolls-chain ke through mil chuka hota hai).
    private suspend fun performMove(tokenIdx: Int, dv: Int): Boolean {
        if (gameOver.value || isMoving.value) return false
        val color = currentColor
        val t = tokens.getValue(color)
        val wasInYard = t[tokenIdx] == -1
        val rawTarget = if (wasInYard) 0 else t[tokenIdx] + dv
        if (rawTarget > 56) return false

        isMoving.value = true

        // ---- Step 1: cell-by-cell slide (yard se nikalna = 1 hi step, seedha start par) ----
        val steps = if (wasInYard) 1 else dv
        repeat(steps) {
            t[tokenIdx] = if (t[tokenIdx] == -1) 0 else t[tokenIdx] + 1
            delay(230)
        }
        delay(240)

        var newPos = t[tokenIdx]
        var captured = false
        var arrowJumped = false
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
                    arrowJumped = true
                    delay(160) // asal HTML jaisa hi thora "poof" pause
                    newPos = if (isTail) newPos + (ARROW_HEAD_OFFSET - ARROW_TAIL_OFFSET) else ARROW_EDGE_ENTRY_REL
                    t[tokenIdx] = newPos
                    delay(160)
                }
            }

            // Main-move capture check — asal HTML ke finalizeAfterMove() jaisa hi: yeh
            // magic check se PEHLE hota hai, arrow-warp ke turant baad (jahan bhi token
            // abhi ruka hai, chahe wahi normal landing ho ya arrow ke baad wali warped cell)
            if (newPos in 0..50) {
                val g = globalCellOf(color, newPos)
                if (captureAtCell(color, g)) captured = true
            }

            // Magic mode: golden-dice cell -> agli roll seedha 6; rocket cell -> 1-15 ghar boost.
            // Rocket ki landing cell khud bhi Arrow-warp aur capture trigger kar sakti hai —
            // asal HTML ke checkMagicCellHit() mein rocket ke andar dobara arrow-check hota hai.
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

                    // Rocket landing par bhi arrow tail/edge ho sakta hai — usi tarah warp karo
                    if (mode == LudoMode.ARROW && newPos in 0..50) {
                        val g2 = globalCellOf(color, newPos)
                        val isTail2 = g2 in arrowTails
                        val isOwnEdge2 = g2 in ARROW_EDGE_SET && ARROW_EDGE_OWNER[g2] == color
                        if (isTail2 || isOwnEdge2) {
                            arrowJumped = true
                            delay(160)
                            newPos = if (isTail2) newPos + (ARROW_HEAD_OFFSET - ARROW_TAIL_OFFSET) else ARROW_EDGE_ENTRY_REL
                            t[tokenIdx] = newPos
                            delay(160)
                        }
                    }

                    // Rocket ki (arrow ke baad wali final) landing cell ka apna capture check
                    if (newPos in 0..50) {
                        val g3 = globalCellOf(color, newPos)
                        if (captureAtCell(color, g3)) captured = true
                    }
                }
            }
        }

        t[tokenIdx] = newPos
        isMoving.value = false

        val reachedHome = newPos == 56
        // Capture flash: 2 second ke baad icons clear kar do
        if (captured) {
            delay(2000)
            killerFlashPos.value = null
            killedFlashPos.value = null
        }
        checkWin(color)

        // NOTE (asal HTML se hoobahoo match): golden-dice cell par land hona khud
        // "extra turn" NAHI deta — sirf agli roll guaranteed 6 hoti hai (bonusSix[color]
        // upar set ho chuka). Asal HTML ke finalizeAfterMove() mein bhi checkMagicCellHit()
        // ka plain golden-dice-hit result hamesha {captured:false} deta hai, isliye
        // magicDiceBonus ko yahan extra-turn ke return mein shamil NAHI karna — warna
        // player ko turn khatam kiye bagair seedha ek aur move bhi mil jata (jo HTML
        // mein nahi hota). Sirf rocket-boost ki landing par hone wala capture/arrow-jump
        // hi extra turn deta hai (woh upar rocket-boost block mein "captured"/"arrowJumped"
        // mein khud shamil ho chuka hota hai).
        return captured || arrowJumped || reachedHome
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
    // uska turn khud skip ho jata hai. Asal HTML ke window.advanceTurn + maybeBotTurn
    // jaisa hoobahoo — bot ki agli roll yahin se khud chain hoti hai, taake extra-turn
    // (chainCapture) milne par bhi bot khud-b-khud agli roll kare.
    private suspend fun advanceTurn(extra: Boolean) {
        movable.clear()
        rollChoice.value = null
        diceRolled.value = false
        if (gameOver.value) return
        savedRolls.clear()
        chainCapture = false
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
        maybeBotTurn()
    }

    private suspend fun maybeBotTurn() {
        if (gameOver.value) return
        if (currentIdx.value == 0) return // self ka intezaar
        delay(600)
        rollDice()
    }
}
