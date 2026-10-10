package com.example.data.auth

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.session.SessionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Auth Supabase via l'API REST GoTrue. Aucune clé n'est codée en dur : elles viennent du
 * BuildConfig, alimenté par le plugin `secrets` (.env local, .env.example par défaut).
 */
class SupabaseAuthService(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) : SessionProvider {
    companion object {
        private const val TAG = "SupabaseAuthService"
        private const val PREFS_NAME = "myapp_auth_prefs"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_USER_EMAIL = "auth_user_email"
        private const val KEY_ACCESS_TOKEN = "auth_access_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_EXPIRES_AT = "auth_expires_at"

        // Marge avant expiration pour déclencher le rafraîchissement.
        private const val REFRESH_MARGIN_MS = 60_000L

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    private val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    private val supabaseKey: String get() = BuildConfig.SUPABASE_KEY

    override fun currentUserId(): String? = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    override fun currentEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

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

    private fun saveTokens(accessToken: String?, refreshToken: String?, expiresInSeconds: Long?) {
        val expiresAt = if (expiresInSeconds != null) {
            System.currentTimeMillis() + expiresInSeconds * 1000L
        } else {
            null
        }
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_EXPIRES_AT, expiresAt ?: 0L)
            .apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    /**
     * Jeton d'accès valide, rafraîchi si proche de l'expiration.
     * Retourne null si aucun jeton n'est disponible (mode hors-ligne ou session expirée).
     */
    suspend fun validAccessToken(): String? = withContext(Dispatchers.IO) {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val shouldRefresh = expiresAt > 0 && System.currentTimeMillis() >= expiresAt - REFRESH_MARGIN_MS
        if (!shouldRefresh) return@withContext token
        if (!refreshSession()) return@withContext null
        prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /** true si le jeton a pu être renouvelé ou était encore valide. */
    suspend fun refreshSession(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return@withContext false

        val payload = JSONObject().apply { put("refresh_token", refreshToken) }.toString()
        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/token?grant_type=refresh_token")
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.w(TAG, "Rafraîchissement refusé (${response.code})")
                // Session réellement morte : on repasse en déconnecté proprement.
                if (response.code == 400 || response.code == 401) clearSession()
                return@withContext false
            }
            val json = JSONObject(bodyString)
            saveTokens(
                accessToken = json.optString("access_token").takeIf { it.isNotBlank() },
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() } ?: refreshToken,
                expiresInSeconds = json.optLong("expires_in", 0L).takeIf { it > 0 }
            )
            true
        } catch (e: Exception) {
            Log.w(TAG, "Rafraîchissement impossible (réseau)", e)
            false
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<AuthSession> =
        withContext(Dispatchers.IO) {
            val trimmedEmail = email.trim()
            if (trimmedEmail.isBlank() || password.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Email et mot de passe requis"))
            }
            val payload = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            try {
                val response = okHttpClient.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyString, "Échec de connexion (${response.code})")
                    return@withContext Result.failure(Exception(errorMsg))
                }
                val json = JSONObject(bodyString)
                val userObj = json.optJSONObject("user")
                val session = AuthSession(
                    userId = userObj?.optString("id") ?: json.optString("id", ""),
                    email = userObj?.optString("email") ?: trimmedEmail,
                    accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
                )
                saveSession(session)
                saveTokens(
                    accessToken = session.accessToken,
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                    expiresInSeconds = json.optLong("expires_in", 0L).takeIf { it > 0 }
                )
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

        val payload = JSONObject().apply {
            put("email", trimmedEmail)
            put("password", password)
        }.toString()

        val request = Request.Builder()
            .url("$supabaseUrl/auth/v1/signup")
            .addHeader("apikey", supabaseKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(bodyString, "Échec de création du compte (${response.code})")
                return@withContext Result.failure(Exception(errorMsg))
            }
            val json = JSONObject(bodyString)
            val userObj = json.optJSONObject("user")
            val accessToken = json.optString("access_token").takeIf { it.isNotBlank() }
            val session = AuthSession(
                userId = userObj?.optString("id") ?: json.optString("id", ""),
                email = userObj?.optString("email") ?: trimmedEmail,
                accessToken = accessToken
            )
            saveSession(session)
            saveTokens(
                accessToken = accessToken,
                refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                expiresInSeconds = json.optLong("expires_in", 0L).takeIf { it > 0 }
            )
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
