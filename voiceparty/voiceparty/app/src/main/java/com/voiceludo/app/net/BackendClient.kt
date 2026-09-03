package com.voiceludo.app.net

import android.content.Context
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ============================================================================
// Asal Go WebSocket backend (voiceparty-backend) se poora connection yahan se
// manage hota hai — signup, login, aur pehle se saved session se khud-b-khud
// dobara login (authToken), sab isi ek socket ke andar. BackendClient.state
// se hi "server se connection nahi hai" wala icon control hota hai, aur har
// login/signup call ka result callback isi se milta hai (loading popup control
// karne ke liye).
//
// Login/signup ke ilawa BackendClient.addListener() se koi bhi screen typed
// ServerMessage events (matchmaking, password-reset OTP, profile update wagera)
// bhi sun sakti hai — dekhein ServerMessage.kt.
//
// !!! ZAROORI: apna asal Railway deployment URL yahan neeche BASE_WS_URL mein
// dalein (jaise "wss://voiceparty-backend-production.up.railway.app/ws") !!!
// Avatar upload isi URL se HTTP(S) POST /avatar nikal kar bhejta hai — is liye
// bekend par yeh route bhi zaroor hona chahiye.
// ============================================================================

enum class ConnState { CONNECTING, CONNECTED, DISCONNECTED }

object BackendClient {

    // TODO: apna asal Railway backend ka WebSocket URL yahan daalein.
    var BASE_WS_URL = "wss://YOUR-BACKEND.up.railway.app/ws"

    private val httpClient = OkHttpClient.Builder()
        // Railway/proxy khali (idle) socket ko band na kar de is liye periodic ping.
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var socket: WebSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainScope = CoroutineScope(Dispatchers.Main)

    private val _state = MutableStateFlow(ConnState.DISCONNECTED)
    val state: StateFlow<ConnState> = _state

    // Ek waqt mein sirf ek "auth-type" request (signup/login/authToken) pending
    // hoti hai — is se response wapis isi callback ke zariye deliver hota hai.
    private var pendingAuthCallback: ((JSONObject?) -> Unit)? = null

    // Typed ServerMessage listeners — matchmaking/forgot-password/profile jaisi
    // screens inhi se subscribe karti hain (addListener/removeListener).
    // CopyOnWriteArrayList isliye taake screen apna listener onDispose mein
    // remove kare usi waqt jab handleMessage() list ko iterate kar raha ho, to
    // ConcurrentModificationException na aaye.
    private val listeners = CopyOnWriteArrayList<(ServerMessage) -> Unit>()

    private var appContext: Context? = null
    private var reconnectAttempt = 0

    // Apni khud ki profile — matchmaking screen mein "main" slot dikhane ke
    // liye (BackendClient.myName / myAvatar). SessionStore hi source-of-truth
    // hai, yeh sirf usi par ek chhota convenience wrapper hai.
    val myName: String get() = appContext?.let { SessionStore.getName(it) } ?: ""
    val myAvatar: String get() = appContext?.let { SessionStore.getAvatar(it) } ?: ""

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        connect()
    }

    fun connect() {
        val ctx = appContext ?: return
        if (_state.value == ConnState.CONNECTING) return
        _state.value = ConnState.CONNECTING

        val request = Request.Builder().url(BASE_WS_URL).build()
        socket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempt = 0
                _state.value = ConnState.CONNECTED
                broadcast(ServerMessage.ConnectionOpened)
                // Agar pehle se session save hai to khud hi (bina password ke) dobara
                // authenticate ho jate hain — isi se "1 dafa login karo, hamesha
                // logged-in raho" wala flow poora hota hai.
                SessionStore.getToken(ctx)?.let { token ->
                    // ZAROORI: bekend "authToken" naam ka koi message type nahi samajhta —
                    // sirf "loginWithToken" (dekhein hub.go ReadPump). Pehle yahan galat
                    // naam bheja ja raha tha, is liye reconnect ke baad server is client
                    // ko kabhi authenticate hi nahi karta tha — aur baad mein "join" bhejte
                    // waqt "pehle signup ya login karein" wala error aata tha, chahe user
                    // asal mein already logged-in ho.
                    sendRaw(JSONObject().put("type", "loginWithToken").put("auth_token", token))
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = ConnState.DISCONNECTED
                deliverToPending(null)
                broadcast(ServerMessage.ConnectionClosed(reason.ifBlank { "connection closed" }))
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("BackendClient", "WS failure: ${t.message}")
                _state.value = ConnState.DISCONNECTED
                deliverToPending(null)
                broadcast(ServerMessage.ConnectionClosed(t.message ?: "connection failed"))
                scheduleReconnect()
            }
        })
    }

    // Screens (jaise LudoMatchingScreen) connection lost hote hi baar-baar
    // isay call karti hain jab tak wapis CONNECTED na ho jaye — connect() khud
    // hi CONNECTING state mein dobara call hone se guard karta hai, isliye
    // baar-baar call karna safe hai (no-op agar pehle se try ho raha ho).
    fun reconnect() {
        if (_state.value != ConnState.CONNECTED) connect()
    }

    private fun scheduleReconnect() {
        reconnectAttempt++
        val delayMs = (1000L * reconnectAttempt).coerceAtMost(15_000L) // 1s,2s,3s... max 15s
        scope.launch {
            delay(delayMs)
            if (_state.value == ConnState.DISCONNECTED) connect()
        }
    }

    private fun sendRaw(obj: JSONObject) {
        socket?.send(obj.toString())
    }

    private fun deliverToPending(resp: JSONObject?) {
        val cb = pendingAuthCallback ?: return
        pendingAuthCallback = null
        mainScope.launch { cb(resp) } // Compose state hamesha main thread par update karte hain
    }

    private fun broadcast(msg: ServerMessage) {
        mainScope.launch {
            listeners.forEach { it(msg) }
        }
    }

    fun addListener(listener: (ServerMessage) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (ServerMessage) -> Unit) {
        listeners.remove(listener)
    }

    private fun handleMessage(text: String) {
        val obj = try { JSONObject(text) } catch (e: Exception) { return }
        when (val type = obj.optString("type")) {
            "auth" -> {
                deliverToPending(obj) // login/signup/autoLogin callback flow (purana)
                broadcast(
                    ServerMessage.Auth(
                        playerId = obj.optString("player_id"),
                        name = obj.optString("name"),
                        avatar = obj.optString("avatar"),
                        coins = obj.optLong("coins"),
                        diamonds = obj.optLong("diamonds")
                    )
                )
            }
            "error" -> {
                deliverToPending(obj) // sirf ek login/signup pending ho to isi ka jawab hai
                broadcast(ServerMessage.Err(obj.optString("message", "Kuch ghalat ho gaya, dobara try karein")))
            }
            "otpSent" -> broadcast(ServerMessage.OtpSent)
            "waiting" -> broadcast(
                ServerMessage.Waiting(
                    current = obj.optInt("current"),
                    needed = obj.optInt("needed")
                )
            )
            "matched" -> broadcast(parseMatched(obj))
            "forceLogout" -> appContext?.let { SessionStore.clear(it) }
            else -> {
                // Baaki/naye message types abhi tak yahan wire nahi hue — jab
                // zaroorat pare, yahan ek naya "when" branch + ServerMessage
                // case add kar dein.
                Log.d("BackendClient", "Unhandled message type: $type")
            }
        }
    }

    private fun parseMatched(obj: JSONObject): ServerMessage.Matched {
        val playersArr = obj.optJSONArray("players")
        val players = mutableListOf<String>()
        if (playersArr != null) {
            for (i in 0 until playersArr.length()) players.add(playersArr.optString(i))
        }
        val profilesObj = obj.optJSONObject("profiles")
        val profiles = mutableMapOf<String, PlayerProfile>()
        profilesObj?.keys()?.forEach { color ->
            val p = profilesObj.optJSONObject(color)
            if (p != null) {
                profiles[color] = PlayerProfile(name = p.optString("name"), avatar = p.optString("avatar"))
            }
        }
        return ServerMessage.Matched(
            roomId = obj.optString("room_id"),
            color = obj.optString("color"),
            players = players,
            profiles = profiles
        )
    }

    // ---- Public auth API — loading/timeout khud handle karte hain ----

    fun signup(email: String, password: String, onResult: (success: Boolean, message: String?) -> Unit) {
        authRequest(JSONObject().put("type", "signup").put("email", email).put("password", password), onResult)
    }

    fun login(email: String, password: String, onResult: (success: Boolean, message: String?) -> Unit) {
        authRequest(JSONObject().put("type", "login").put("email", email).put("password", password), onResult)
    }

    // App dobara khulne par saved token se khud login karta hai.
    fun autoLogin(onResult: (success: Boolean, message: String?) -> Unit) {
        val ctx = appContext ?: return onResult(false, "not initialized")
        val token = SessionStore.getToken(ctx) ?: return onResult(false, "no saved session")
        authRequest(JSONObject().put("type", "loginWithToken").put("auth_token", token), onResult)
    }

    private fun authRequest(payload: JSONObject, onResult: (Boolean, String?) -> Unit) {
        val ctx = appContext
        if (_state.value != ConnState.CONNECTED || ctx == null) {
            onResult(false, "Server se connection nahi hai — internet check karein")
            return
        }
        pendingAuthCallback = { resp ->
            if (resp == null) {
                onResult(false, "Connection toot gaya — dobara try karein")
            } else if (resp.optString("type") == "auth") {
                SessionStore.save(
                    ctx,
                    resp.optString("player_id"),
                    resp.optString("auth_token"),
                    resp.optString("name"),
                    resp.optString("avatar"),
                    resp.optLong("coins"),
                    resp.optLong("diamonds")
                )
                onResult(true, null)
            } else {
                onResult(false, resp.optString("message", "Kuch ghalat ho gaya, dobara try karein"))
            }
        }
        sendRaw(payload)

        // 10-second safety timeout — server response na de to loading hamesha ke
        // liye atki nahi rahegi.
        scope.launch {
            delay(10_000)
            if (pendingAuthCallback != null) {
                deliverToPending(null)
            }
        }
    }

    // ---- Forgot password — dono steps bekend par (server hi OTP banata/verify
    // karta hai). Jawab ServerMessage.OtpSent / ServerMessage.Auth / .Err ke
    // zariye listener par aata hai (deliverToPending yahan use nahi hota kyunke
    // yeh dono calls "fire and forget" hain, screen listener se hi sunti hai). ----

    // bekend type expected wapis: "otpSent" ya "error"
    fun requestPasswordReset(email: String) {
        sendRaw(JSONObject().put("type", "requestPasswordReset").put("email", email))
    }

    // bekend type expected wapis: "auth" (reset + turant login) ya "error"
    fun resetPassword(email: String, newPassword: String, code: String) {
        // ZAROORI: bekend field ka naam "otp" expect karta hai, "code" nahi
        // (ClientMsg.Otp, dekhein hub.go/handleResetPassword) — pehle galat
        // naam ki wajah se OTP hamesha khali/ghalat samjha jata tha aur
        // password reset kabhi kaamyab nahi hota tha.
        sendRaw(
            JSONObject()
                .put("type", "resetPassword")
                .put("email", email)
                .put("password", newPassword)
                .put("otp", code)
        )
    }

    // ---- Matchmaking — LudoMatchingScreen isi se poore protocol ko chalati
    // hai. Jawab (waiting/matched/error) listener par ServerMessage ke zariye
    // aata hai, koi callback nahi (kyunke ek se zyada message aa sakte hain). ----

    // bekend type expected wapis: "waiting" (bar bar) phir "matched", ya "error"
    fun join(mode: String, bet: Int, players: Int, magic: Boolean) {
        sendRaw(
            JSONObject()
                .put("type", "join")
                .put("mode", mode)
                .put("bet", bet)
                .put("players", players)
                .put("magic", magic)
        )
    }

    fun leaveRoom() {
        // ZAROORI: bekend "leaveRoom" nahi, sirf "leave" type samajhta hai
        // (hub.go ReadPump ka switch-case) — galat naam se yeh silently ignore
        // ho raha tha aur room/queue se kabhi nikalta hi nahi tha.
        sendRaw(JSONObject().put("type", "leave"))
    }

    // ---- Profile — ProfileEditScreen isi se naam/avatar bekend (aur is liye
    // baaki game-partners) ko sync karta hai. ----

    // bekend type expected wapis: "auth" (updated name/avatar ke sath) ya "error"
    fun updateProfile(name: String, avatarUrl: String) {
        sendRaw(JSONObject().put("type", "updateProfile").put("name", name).put("avatar", avatarUrl))
    }

    // Avatar photo websocket se nahi jati (bekend JSON text-frames expect karta
    // hai) — yeh alag se ek plain HTTPS multipart POST hai, isi liye callback
    // deta hai (ServerMessage listener ki jagah), taake caller ko turant hosted
    // URL mil jaye avatar preview update karne ke liye.
    //
    // !!! ZAROORI: bekend par isi BASE_WS_URL (wss:// -> https://) ke "/avatar"
    // route par ek multipart-form POST endpoint hona chahiye jo {"url": "..."}
    // wapis kare. !!!
    fun uploadAvatar(bytes: ByteArray, mime: String, onResult: (url: String?, error: String?) -> Unit) {
        val ctx = appContext
        val token = ctx?.let { SessionStore.getToken(it) }
        if (token == null) {
            onResult(null, "Login zaroori hai")
            return
        }
        val httpBase = BASE_WS_URL
            .replaceFirst("wss://", "https://")
            .replaceFirst("ws://", "http://")
            .substringBeforeLast("/ws")
        // ZAROORI: bekend (main.go avatarUploadHandler) multipart-form NAHI, seedha
        // RAW image bytes body mein expect karta hai, aur token Authorization header
        // se nahi balke "?token=" query param se leta hai. Pehle multipart + Bearer
        // header bheja ja raha tha jo bekend bilkul samajhta hi nahi — upload hamesha
        // 401/400 se fail hota tha.
        val request = Request.Builder()
            .url("$httpBase/avatar?token=$token")
            .post(bytes.toRequestBody(mime.toMediaTypeOrNull()))
            .build()

        scope.launch {
            try {
                httpClient.newCall(request).execute().use { resp ->
                    val respText = resp.body?.string()
                    if (resp.isSuccessful && respText != null) {
                        val url = try { JSONObject(respText).optString("url") } catch (e: Exception) { "" }
                        mainScope.launch {
                            if (url.isNotBlank()) onResult(url, null)
                            else onResult(null, "Upload ka jawab samajh nahi aya")
                        }
                    } else {
                        mainScope.launch { onResult(null, "Upload fail ho gaya (${resp.code})") }
                    }
                }
            } catch (e: Exception) {
                mainScope.launch { onResult(null, e.message ?: "Upload nahi ho saka, internet check karein") }
            }
        }
    }
}
