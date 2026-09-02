package com.voiceludo.app.net

// ============================================================================
// UI screens (LudoMatchingScreen, ForgotPasswordScreen, ProfileEditScreen...)
// bekend se aane wala raw JSON khud parse nahi karte — is ki jagah wo
// BackendClient.addListener() se subscribe karte hain aur isi typed sealed
// class ke `when` branches se handle karte hain. BackendClient.handleMessage()
// hi asal JSON ko in cases mein convert karta hai.
//
// !!! ZAROORI: yeh sirf APP-SIDE contract hai. Aapke asal Go WebSocket bekend
// ko bhi inhi message "type" values ke sath jawab dena hoga (neeche har case
// ke comment mein likha hai bekend se kaunsa "type" expect ho raha hai) —
// warna matchmaking/forgot-password/profile-update UI mein kuch nahi hoga
// (na error, na success), bas hamesha "waiting/loading" hi dikhta rahega.
// ============================================================================

// Matchmaking mein ek opponent ki bas UI ke liye zaroori info — poora account
// nahi, sirf naam + avatar (jo MatchPlayerSlot dikhata hai).
data class PlayerProfile(val name: String, val avatar: String)

sealed class ServerMessage {

    // bekend type: "auth" — login / signup / autoLogin / resetPassword, sab
    // isi ek message se success hote hain (session BackendClient khud save
    // kar chuka hota hai is tak pohanchne se pehle).
    data class Auth(
        val playerId: String,
        val name: String,
        val avatar: String,
        val coins: Long,
        val diamonds: Long
    ) : ServerMessage()

    // bekend type: "error" — koi bhi request (login/signup/join/reset/update)
    // fail ho jaye to yehi ek message wapis aata hai.
    data class Err(val message: String) : ServerMessage()

    // bekend type: "otpSent" — requestPasswordReset() ke jawab mein, jab OTP
    // email par bhej diya gaya ho.
    object OtpSent : ServerMessage()

    // bekend type: "waiting" — matchmaking queue mein hain, poore players
    // (players count) abhi tak nahi mile.
    data class Waiting(val current: Int, val needed: Int) : ServerMessage()

    // bekend type: "matched" — poora match mil gaya, game shuru hone ke liye
    // tayyar. `color` yeh batata hai ke MAIN (is device wala player) kaunsa
    // token color hai; `players` sabki color-order hai; `profiles` har color
    // ka naam+avatar.
    data class Matched(
        val roomId: String,
        val color: String,
        val players: List<String>,
        val profiles: Map<String, PlayerProfile>
    ) : ServerMessage()

    // Yeh do koi bekend "type" field se nahi aate — khud WebSocket ke
    // onOpen/onClosed/onFailure se yahan broadcast hote hain, taake
    // matchmaking/forgot-password jaisi screens bhi (jo sirf listener use
    // karti hain, ConnectionStatusOverlay ki tarah state collect nahi kartin)
    // connection drop/restore par apna UI theek kar sakein.
    object ConnectionOpened : ServerMessage()
    data class ConnectionClosed(val reason: String) : ServerMessage()
}
