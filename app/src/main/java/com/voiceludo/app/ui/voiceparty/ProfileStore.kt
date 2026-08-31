package com.voiceludo.app.ui.voiceparty

import android.content.Context
import android.content.SharedPreferences

// ============================================================================
// Asal HTML mein profile ka data `window.currentUserData` (JS object, Firestore
// se sync) mein rehta tha — name, bio, gender, flag, dp (photo) sab isi object
// ke fields thay (editName/editBio/editGender/editFlag/uploadAvatarFile sab isi
// ko update karte thay, phir Firestore mein save).
//
// Yahan Firebase nahi hai, isliye yeh data seedha on-device SharedPreferences
// mein rakhte hain — AccountStore (login/coins) se alag, kyunke profile Guest
// mode mein bhi editable hona chahiye (HTML jaisa hi).
// ============================================================================

private const val PROFILE_PREFS = "voiceludo_profile"

data class UserProfile(
    val name: String = "Guest_00000000",
    val bio: String = "",
    val gender: String = "male",              // "male" | "female"
    val flagName: String = "Pakistan",
    val flagIcon: String = "\uD83C\uDDF5\uD83C\uDDF0", // 🇵🇰
    // Khali = default user-icon.png. Warna ya to internal file ka absolute path
    // (device gallery se choose ki gayi photo) ya preset gallery ka https url.
    val avatarUri: String = ""
)

object ProfileStore {

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE)

    fun get(context: Context): UserProfile {
        val p = prefs(context)
        return UserProfile(
            name = p.getString("name", null) ?: "Guest_00000000",
            bio = p.getString("bio", null) ?: "",
            gender = p.getString("gender", null) ?: "male",
            flagName = p.getString("flagName", null) ?: "Pakistan",
            flagIcon = p.getString("flagIcon", null) ?: "\uD83C\uDDF5\uD83C\uDDF0",
            avatarUri = p.getString("avatarUri", null) ?: ""
        )
    }

    fun saveName(context: Context, name: String) {
        // HTML jaisa hi: max 16 chars, khali ho to Guest_ fallback
        val t = name.trim().let { if (it.isEmpty()) "Guest_00000000" else it }.take(16)
        prefs(context).edit().putString("name", t).apply()
    }

    fun saveBio(context: Context, bio: String) {
        prefs(context).edit().putString("bio", bio.take(70)).apply()
    }

    fun saveGender(context: Context, gender: String) {
        prefs(context).edit().putString("gender", gender).apply()
    }

    fun saveFlag(context: Context, flagName: String, flagIcon: String) {
        prefs(context).edit()
            .putString("flagName", flagName)
            .putString("flagIcon", flagIcon)
            .apply()
    }

    fun saveAvatar(context: Context, avatarUri: String) {
        prefs(context).edit().putString("avatarUri", avatarUri).apply()
    }
}
