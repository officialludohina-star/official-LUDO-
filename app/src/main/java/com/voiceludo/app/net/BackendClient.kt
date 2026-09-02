package com.voiceludo.app.net

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

// ============================================================================
// Asal bekend (README.md ke mutabiq): "signup, login, aur game sab kuch ek hi
// WebSocket connection ke andar hota hai". Yeh client bhi bilkul waisa hi hai —
// ek hi persistent /ws connection app ke poore session mein (login se le kar
// game khatam hone tak) khula rehta hai. Koi bhi fake/local player/bot ab
// istemal nahi hota — matchmaking, dice, moves sab is server se aate hain.
// ============================================================================

private const val WS_URL = "wss://voice-party-bekend-sarver-production.up.railway.app/ws"
// Avatar upload REST endpoint isi bekend host par hai, sirf wss:// ki jagah
// https:// aur path /avatar (WS_URL se hi derive kar lete hain taake host
// do jagah maintain na karna pare).
private val HTTP_BASE = WS_URL.removeSuffix("/ws").replaceFirst("wss://", "https://").replaceFirst("ws://", "http://")

// Bekend ke "profiles" map / "opponentProfile" event / "auth" response mein aane
// wala har player ka naam + avatar (avatar hamesha ek hosted URL hoti hai, kabhi
// base64 nahi — README ke mutabiq).
data class Profile(val name: String, val avatar: String)

data class GameEvent(
    val type: String,
    val color: String? = null,
    val value: Int = 0,
    val tokenIndex: Int = 0,
    val from: Int = 0,
    val to: Int = 0,
    val captured: List<String> = emptyList(),
    val arrowJumped: Boolean = false,
    val magicBonus: Boolean = false,
    val reachedHome: Boolean = false,
    val movable: List<Int> = emptyList(),
    val winner: String? = null,
    val finishOrder: List<String> = emptyList(),
    val message: String? = null
)

data class GameSnapshot(
    val mode: String,
    val players: List<String>,
    val tokens: Map<String, List<Int>>,
    val currentColor: String,
    val diceByColor: Map<String, Int>,
    val savedRolls: List<Int>,
    val movable: List<Int>,
    val gameOver: Boolean,
    val winner: String?,
    val finishOrder: List<String>,
    val rankBadge: Map<String, Int>,
    val magicOn: Boolean,
    val magicDiceCells: List<Int>,
    val magicRocketCells: List<Int>,
    // Har color ke liye agli extra-roll ki diamond-cost (2,4,8,10,16,24, phir har
    // baar double) — 0 ka matlab hai us player ke liye is game mein 1000-diamond
    // cap tak pahunch kar lock ho chuka.
    val extraRollNextCost: Map<String, Long>
)

sealed class ServerMessage {
    data class Auth(
        val playerId: String,
        val authToken: String,
        val coins: Long,
        val diamonds: Long,
        val name: String,
        val avatar: String
    ) : ServerMessage()
    data class Err(val message: String) : ServerMessage()
    data class Waiting(val message: String) : ServerMessage()
    data class Matched(
        val roomId: String,
        val color: String,
        val players: List<String>,
        val mode: String,
        val bet: Int,
        val coins: Long,
        val state: GameSnapshot,
        // color -> {name, avatar} har player ka, is se opponent ki profile
        // (naam + DP) game screen par dikhai ja sakti hai.
        val profiles: Map<String, Profile>
    ) : ServerMessage()
    data class Events(val events: List<GameEvent>, val state: GameSnapshot) : ServerMessage()
    // Net drop ke baad wapis connect hone par (BackendClient khud "resume" bhej
    // deta hai agar hum kisi active game mein thay) — poora room/game state
    // Matched jaisa hi wapis milta hai, koi navigation/re-match nahi hota.
    data class Resumed(
        val roomId: String, val color: String, val players: List<String>, val mode: String,
        val bet: Int, val coins: Long, val diamonds: Long, val state: GameSnapshot,
        val profiles: Map<String, Profile>
    ) : ServerMessage()
    // Opponent ka connection toot gaya — bekend seconds batata hai (reconnect
    // grace window) jitni der tak wo wapis aa sakta hai bina haarne ke.
    data class OpponentDisconnected(val color: String, val seconds: Int) : ServerMessage()
    data class OpponentReconnected(val color: String) : ServerMessage()
    data class Wallet(val color: String?, val coins: Long?, val diamonds: Long?, val message: String?) : ServerMessage()
    data class OpponentLeft(val color: String) : ServerMessage()
    // Kisi opponent ne mid-game apna naam/avatar badla (updateProfile bheja) —
    // isay real-time update karne ke liye.
    data class OpponentProfile(val color: String, val name: String, val avatar: String) : ServerMessage()
    // Har turn (roll ya pending-move) shuru hote hi bekend yeh bhejta hai — us
    // player ki profile par 12-second countdown ring dikhane ke liye.
    data class TurnTimer(val color: String, val seconds: Int) : ServerMessage()
    // Yeh account kisi doosre phone/device par login/signup hua — bekend ne is
    // (purane) connection ko turant band kar diya hai, 1 ID = 1 device rule ke tehat.
    data class ForceLogout(val message: String) : ServerMessage()
    object ConnectionOpened : ServerMessage()
    data class ConnectionClosed(val reason: String) : ServerMessage()
}

typealias ServerListener = (ServerMessage) -> Unit

object BackendClient {

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private val listeners = CopyOnWriteArrayList<ServerListener>()
    // Signup/login ke baad connect hone se pehle bheji gayi cheezein isi queue
    // mein rehti hain, socket open hote hi flush ho jati hain.
    private val pending = mutableListOf<JSONObject>()

    // Session — jab tak app khuli hai / socket connected hai, yehi asal account hai.
    var playerId: String? = null; private set
    var authToken: String? = null; private set
    var coins: Long = 0; private set
    var diamonds: Long = 0; private set
    // Server-truth apna naam/avatar (auth response se, aur updateProfile bheje
    // jaane ke baad hum khud optimistically yahan update kar dete hain).
    var myName: String = ""; private set
    var myAvatar: String = ""; private set

    // Matching screen match milte hi yahan store kar deti hai, Game screen isay
    // consume kar leti hai — is se navigation route args mein room/color/state
    // jaisi cheezein ghusane ki zaroorat nahi parti.
    private var lastMatch: ServerMessage.Matched? = null
    fun consumeLastMatch(): ServerMessage.Matched? {
        val m = lastMatch
        lastMatch = null
        return m
    }

    // inGame — jab tak yeh true hai, agar socket kabhi bhi (net jaane se) toot
    // kar wapis khule to onOpen khud "resume" bhej deta hai (naya login/signup
    // nahi maangta). "matched"/"resumed" par true hota hai, leaveRoom() ya
    // forceLogout par false.
    @Volatile private var inGame = false

    fun addListener(l: ServerListener) { listeners.add(l) }
    fun removeListener(l: ServerListener) { listeners.remove(l) }

    // OkHttp ke WebSocketListener callbacks (onOpen/onMessage/onClosed/onFailure)
    // hamesha OkHttp ke apne background thread par chalte hain, UI/main thread
    // par nahi. Agar listeners (jaise GmailLoginScreen) yahan se seedha Compose
    // state badalne ya navController.navigate() call karne ki koshish karte, to
    // "must be called on main thread" exception aata — jo OkHttp turant
    // connection failure samajh kar socket band kar deta ("ConnectionClosed"
    // dikhta, chahe server ne bilkul sahi jawab diya ho). Ab yahan hi ek dafa
    // main thread par switch kar dete hain, taake sab listeners hamesha safe
    // rahen aur unhe khud is baat ka khayal na rakhna pade.
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun notify(msg: ServerMessage) {
        mainHandler.post {
            listeners.forEach { it(msg) }
        }
    }

    fun ensureConnected() {
        if (socket != null) return
        val request = Request.Builder().url(WS_URL).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Agar hum kisi active game ke beech mein thay (net drop se pehle),
                // to sab se pehle purana session "resume" kar lete hain — koi
                // dobara login/signup nahi maangta, seedha wahi room/state wapis
                // mil jati hai. Yeh signup/login se bhi pehle jata hai.
                val token = authToken
                if (inGame && token != null) {
                    webSocket.send(JSONObject().put("type", "resume").put("auth_token", token).toString())
                }
                synchronized(pending) {
                    pending.forEach { webSocket.send(it.toString()) }
                    pending.clear()
                }
                notify(ServerMessage.ConnectionOpened)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parse(text)?.let { notify(it) }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BackendClient", "ws failure: ${t.message}")
                socket = null
                notify(ServerMessage.ConnectionClosed(t.message ?: "connection failed"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                notify(ServerMessage.ConnectionClosed(reason))
            }
        })
    }

    private fun send(obj: JSONObject) {
        ensureConnected()
        val ws = socket
        if (ws == null) {
            synchronized(pending) { pending.add(obj) }
        } else {
            ws.send(obj.toString())
        }
    }

    fun signup(email: String, password: String) {
        send(JSONObject().put("type", "signup").put("email", email).put("password", password))
    }

    fun login(email: String, password: String) {
        send(JSONObject().put("type", "login").put("email", email).put("password", password))
    }

    fun join(mode: String, bet: Int, players: Int, magic: Boolean) {
        send(
            JSONObject()
                .put("type", "join")
                .put("mode", mode)
                .put("bet", bet)
                .put("players", players)
                .put("magic", magic)
        )
    }

    fun roll() {
        send(JSONObject().put("type", "roll"))
    }

    fun move(token: Int, value: Int = 0) {
        send(JSONObject().put("type", "move").put("token", token).put("value", value))
    }

    fun buyExtraRoll() {
        send(JSONObject().put("type", "buyExtraRoll"))
    }

    fun leaveRoom() {
        inGame = false
        send(JSONObject().put("type", "leave"))
    }

    // reconnect() — user "Connect" button tap kare (network wapis check karne
    // ke liye) ya app khud dobara koshish karna chahe. Purana dead socket hata
    // kar naya connection kholta hai — onOpen() khud "resume" bhej dega
    // (inGame abhi tak true hai to).
    fun reconnect() {
        socket?.cancel()
        socket = null
        ensureConnected()
    }

    // Naam/avatar badalne ke liye — avatar hamesha pehle uploadAvatar() se mile
    // hosted URL ke sath bhejna hai, kabhi bhi local file path/base64 nahi.
    // Optimistically apni local copy (myName/myAvatar) bhi turant update kar
    // dete hain, taake apni hi screen par turant naya naam/photo dikhe.
    fun updateProfile(name: String, avatar: String) {
        myName = name
        myAvatar = avatar
        send(JSONObject().put("type", "updateProfile").put("name", name).put("avatar", avatar))
    }

    // Profile photo upload — README ke mutabiq POST /avatar?token=<auth_token>,
    // body = raw image bytes, Content-Type: image/jpeg|png|webp, max 3MB.
    // Response: {"url": "https://.../avatars/xxx.jpg"} — yehi URL phir
    // updateProfile() mein bheja jata hai. Kabhi bhi base64/data-URI nahi bhejte.
    // Callback hamesha main thread par aata hai.
    fun uploadAvatar(bytes: ByteArray, mimeType: String, onResult: (url: String?, error: String?) -> Unit) {
        val token = authToken
        if (token == null) {
            mainHandler.post { onResult(null, "login zaroori hai") }
            return
        }
        val url = "$HTTP_BASE/avatar?token=$token"
        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val request = Request.Builder().url(url).post(body).header("Content-Type", mimeType).build()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                mainHandler.post { onResult(null, e.message ?: "upload fail ho gaya") }
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                response.use { resp ->
                    val text = resp.body?.string()
                    if (!resp.isSuccessful || text == null) {
                        mainHandler.post { onResult(null, text ?: "upload fail ho gaya (${resp.code})") }
                        return
                    }
                    val resultUrl = try { JSONObject(text).optString("url").ifBlank { null } } catch (e: Exception) { null }
                    mainHandler.post {
                        if (resultUrl != null) onResult(resultUrl, null) else onResult(null, "invalid response")
                    }
                }
            }
        })
    }

    private fun jsonArrayToIntList(a: JSONArray?): List<Int> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.getInt(it) }
    }

    private fun jsonArrayToStringList(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return (0 until a.length()).map { a.getString(it) }
    }

    private fun parseSnapshot(obj: JSONObject?): GameSnapshot? {
        obj ?: return null
        val tokensObj = obj.optJSONObject("tokens")
        val tokens = mutableMapOf<String, List<Int>>()
        tokensObj?.keys()?.forEach { k -> tokens[k] = jsonArrayToIntList(tokensObj.optJSONArray(k)) }
        val diceObj = obj.optJSONObject("diceByColor")
        val dice = mutableMapOf<String, Int>()
        diceObj?.keys()?.forEach { k -> dice[k] = diceObj.optInt(k, 1) }
        val rankObj = obj.optJSONObject("rankBadge")
        val rank = mutableMapOf<String, Int>()
        rankObj?.keys()?.forEach { k -> rank[k] = rankObj.optInt(k) }
        val extraCostObj = obj.optJSONObject("extraRollNextCost")
        val extraCost = mutableMapOf<String, Long>()
        extraCostObj?.keys()?.forEach { k -> extraCost[k] = extraCostObj.optLong(k) }
        val winner = obj.optString("winner")
        return GameSnapshot(
            mode = obj.optString("mode"),
            players = jsonArrayToStringList(obj.optJSONArray("players")),
            tokens = tokens,
            currentColor = obj.optString("currentColor"),
            diceByColor = dice,
            savedRolls = jsonArrayToIntList(obj.optJSONArray("savedRolls")),
            movable = jsonArrayToIntList(obj.optJSONArray("movable")),
            gameOver = obj.optBoolean("gameOver"),
            winner = winner.ifBlank { null },
            finishOrder = jsonArrayToStringList(obj.optJSONArray("finishOrder")),
            rankBadge = rank,
            magicOn = obj.optBoolean("magicOn"),
            magicDiceCells = jsonArrayToIntList(obj.optJSONArray("magicDiceCells")),
            magicRocketCells = jsonArrayToIntList(obj.optJSONArray("magicRocketCells")),
            extraRollNextCost = extraCost
        )
    }

    private fun parseProfiles(obj: JSONObject?): Map<String, Profile> {
        if (obj == null) return emptyMap()
        val out = mutableMapOf<String, Profile>()
        obj.keys().forEach { color ->
            val p = obj.optJSONObject(color) ?: return@forEach
            out[color] = Profile(name = p.optString("name"), avatar = p.optString("avatar"))
        }
        return out
    }

    private fun parseEvents(arr: JSONArray?): List<GameEvent> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            val color = e.optString("color")
            val winner = e.optString("winner")
            val message = e.optString("message")
            GameEvent(
                type = e.optString("type"),
                color = color.ifBlank { null },
                value = e.optInt("value"),
                tokenIndex = e.optInt("tokenIndex"),
                from = e.optInt("from", -1),
                to = e.optInt("to", -1),
                captured = jsonArrayToStringList(e.optJSONArray("captured")),
                arrowJumped = e.optBoolean("arrowJumped"),
                magicBonus = e.optBoolean("magicBonus"),
                reachedHome = e.optBoolean("reachedHome"),
                movable = jsonArrayToIntList(e.optJSONArray("movable")),
                winner = winner.ifBlank { null },
                finishOrder = jsonArrayToStringList(e.optJSONArray("finishOrder")),
                message = message.ifBlank { null }
            )
        }
    }

    private fun parse(text: String): ServerMessage? {
        return try {
            val obj = JSONObject(text)
            when (obj.optString("type")) {
                "auth" -> {
                    playerId = obj.optString("player_id")
                    authToken = obj.optString("auth_token")
                    coins = obj.optLong("coins")
                    diamonds = obj.optLong("diamonds")
                    myName = obj.optString("name")
                    myAvatar = obj.optString("avatar")
                    ServerMessage.Auth(playerId!!, authToken!!, coins, diamonds, myName, myAvatar)
                }
                "error" -> ServerMessage.Err(obj.optString("message"))
                "waiting" -> ServerMessage.Waiting(obj.optString("message"))
                "matched" -> {
                    if (obj.has("coins")) coins = obj.optLong("coins")
                    val snap = parseSnapshot(obj.optJSONObject("state")) ?: return null
                    inGame = true
                    val m = ServerMessage.Matched(
                        roomId = obj.optString("room_id"),
                        color = obj.optString("color"),
                        players = jsonArrayToStringList(obj.optJSONArray("players")),
                        mode = obj.optString("mode"),
                        bet = obj.optInt("bet"),
                        coins = coins,
                        state = snap,
                        profiles = parseProfiles(obj.optJSONObject("profiles"))
                    )
                    lastMatch = m
                    m
                }
                "resumed" -> {
                    if (obj.has("coins")) coins = obj.optLong("coins")
                    if (obj.has("diamonds")) diamonds = obj.optLong("diamonds")
                    val snap = parseSnapshot(obj.optJSONObject("state")) ?: return null
                    inGame = true
                    ServerMessage.Resumed(
                        roomId = obj.optString("room_id"),
                        color = obj.optString("color"),
                        players = jsonArrayToStringList(obj.optJSONArray("players")),
                        mode = obj.optString("mode"),
                        bet = obj.optInt("bet"),
                        coins = coins,
                        diamonds = diamonds,
                        state = snap,
                        profiles = parseProfiles(obj.optJSONObject("profiles"))
                    )
                }
                "opponentDisconnected" -> ServerMessage.OpponentDisconnected(obj.optString("color"), obj.optInt("seconds", 60))
                "opponentReconnected" -> ServerMessage.OpponentReconnected(obj.optString("color"))
                "events" -> {
                    val snap = parseSnapshot(obj.optJSONObject("state")) ?: return null
                    ServerMessage.Events(events = parseEvents(obj.optJSONArray("events")), state = snap)
                }
                "wallet" -> {
                    if (obj.has("coins")) coins = obj.optLong("coins")
                    if (obj.has("diamonds")) diamonds = obj.optLong("diamonds")
                    val color = obj.optString("color")
                    val message = obj.optString("message")
                    ServerMessage.Wallet(
                        color = color.ifBlank { null },
                        coins = if (obj.has("coins")) obj.optLong("coins") else null,
                        diamonds = if (obj.has("diamonds")) obj.optLong("diamonds") else null,
                        message = message.ifBlank { null }
                    )
                }
                "opponentLeft" -> ServerMessage.OpponentLeft(obj.optString("color"))
                "opponentProfile" -> ServerMessage.OpponentProfile(
                    color = obj.optString("color"),
                    name = obj.optString("name"),
                    avatar = obj.optString("avatar")
                )
                "turnTimer" -> ServerMessage.TurnTimer(obj.optString("color"), obj.optInt("seconds", 12))
                "forceLogout" -> {
                    // Server ne yeh (purana) connection band kar diya — apni local
                    // session state bhi turant clear kar dete hain taake app dobara
                    // isi purani auth se kuch bhejne ki koshish na kare.
                    playerId = null
                    authToken = null
                    inGame = false
                    ServerMessage.ForceLogout(obj.optString("message"))
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("BackendClient", "parse error for '$text': ${e.message}")
            null
        }
    }
}
