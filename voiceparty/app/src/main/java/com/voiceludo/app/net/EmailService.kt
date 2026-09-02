package com.voiceludo.app.net

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// ============================================================================
// Yeh index.html (asal HTML reference) ke Gmail signup/forgot-password screen
// wale email.js scene ka hoobahoo Kotlin equivalent hai. HTML mein browser ka
// EmailJS JS SDK (emailjs.init + emailjs.send) use hota tha; Android WebView/
// browser JS SDK yahan available nahi hoti, isliye seedha EmailJS ka REST API
// (https://api.emailjs.com/api/v1.0/email/send) OkHttp se call kiya jata hai —
// wahi service/template/public-key jo index.html mein the, taake asal
// "officialludohina@gmail.com" wala OTP email hoobahoo yahan se bhi jaye.
// ============================================================================

private const val EMAILJS_SERVICE_ID = "service_1d2y28h"
private const val EMAILJS_TEMPLATE_ID = "template_diw9ywh"
private const val EMAILJS_PUBLIC_KEY = "1mgEysCMhFFaok_Fb"
private const val EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send"

object EmailService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val mainHandler = Handler(Looper.getMainLooper())

    // toEmail ko OTP bhejta hai. onResult hamesha main/UI thread par call hota hai.
    fun sendOtp(toEmail: String, otp: String, onResult: (success: Boolean) -> Unit) {
        val templateParams = JSONObject()
            .put("to_email", toEmail)
            .put("email", toEmail)
            .put("name", "Voice Party Ludo")
            .put("from_name", "Voice Party Ludo")
            .put("otp_code", otp)
            .put("message", otp)

        val body = JSONObject()
            .put("service_id", EMAILJS_SERVICE_ID)
            .put("template_id", EMAILJS_TEMPLATE_ID)
            .put("user_id", EMAILJS_PUBLIC_KEY)
            .put("template_params", templateParams)
            .toString()

        val request = Request.Builder()
            .url(EMAILJS_URL)
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("EmailService", "sendOtp network failure: ${e.message}")
                mainHandler.post { onResult(false) }
            }

            override fun onResponse(call: Call, response: Response) {
                val ok = response.isSuccessful
                if (!ok) {
                    // EmailJS ka asal error message (jaisay "API calls are disabled for
                    // non-browser applications") yahan Logcat mein dikhega — tag "EmailService"
                    // se filter kar ke dekhein.
                    val errBody = response.body?.string()
                    Log.e("EmailService", "sendOtp failed: HTTP ${response.code} — $errBody")
                }
                response.close()
                mainHandler.post { onResult(ok) }
            }
        })
    }
}
