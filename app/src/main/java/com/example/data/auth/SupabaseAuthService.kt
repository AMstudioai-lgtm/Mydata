package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseAuthService(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "SupabaseAuthService"
        private const val PREFS_NAME = "myapp_auth_prefs"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"

        // Default credentials as specified in project specifications
        private const val DEFAULT_SUPABASE_URL = "https://hvxslvdqnwaafvdanasf.supabase.co"
        private const val DEFAULT_SUPABASE_KEY = "sb_publishable_wvvtlC4ym1GS8VvXhR2wWA_Pg5OLHy3"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val supabaseUrl: String
        get() {
            val configured = runCatching { BuildConfig.SUPABASE_URL }.getOrNull()
            return if (!configured.isNullOrBlank()) configured else DEFAULT_SUPABASE_URL
        }

    val supabaseKey: String
        get() {
            val configured = runCatching { BuildConfig.SUPABASE_KEY }.getOrNull()
            return if (!configured.isNullOrBlank()) configured else DEFAULT_SUPABASE_KEY
        }

    fun getCachedSession(): AuthSession? {
        val email = prefs.getString(KEY_USER_EMAIL, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: ""
        val token = prefs.getString(KEY_ACCESS_TOKEN, null)
        return AuthSession(userId = userId, email = email, accessToken = token, isOfflineMode = false)
    }

    private fun saveSession(session: AuthSession) {
        prefs.edit()
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_USER_EMAIL, session.email)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    suspend fun signInWithPassword(email: String, password: String):Result<AuthSession> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email et mot de passe requis"))
        }

        val url = "$supabaseUrl/auth/v1/token?grant_type=password"
        val payload = JSONObject().apply {
            put("email", trimmedEmail)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(bodyString, "Échec de connexion (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(bodyString)
            val accessToken = json.optString("access_token")
            val userObj = json.optJSONObject("user")
            val userId = userObj?.optString("id") ?: json.optString("id", "")
            val userEmail = userObj?.optString("email") ?: trimmedEmail

            val session = AuthSession(userId = userId, email = userEmail, accessToken = accessToken)
            saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            Result.failure(Exception("Erreur réseau: ${e.localizedMessage ?: "Vérifiez votre connexion"}"))
        }
    }

    suspend fun signUp(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Email et mot de passe requis"))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Le mot de passe doit comporter au moins 6 caractères"))
        }

        val url = "$supabaseUrl/auth/v1/signup"
        val payload = JSONObject().apply {
            put("email", trimmedEmail)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(bodyString, "Échec de création du compte (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(bodyString)
            val accessToken = if (json.has("access_token")) json.getString("access_token") else null
            val userObj = json.optJSONObject("user")
            val userId = userObj?.optString("id") ?: json.optString("id", "")
            val userEmail = userObj?.optString("email") ?: trimmedEmail

            val session = AuthSession(userId = userId, email = userEmail, accessToken = accessToken)
            saveSession(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            Result.failure(Exception("Erreur réseau: ${e.localizedMessage ?: "Vérifiez votre connexion"}"))
        }
    }

    private fun parseErrorMessage(jsonStr: String, fallback: String): String {
        return try {
            val json = JSONObject(jsonStr)
            when {
                json.has("error_description") -> json.getString("error_description")
                json.has("msg") -> json.getString("msg")
                json.has("message") -> json.getString("message")
                json.has("error") -> json.getString("error")
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
