package com.example.data.update

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UpdateInfo(val version: String, val url: String)

class UpdateChecker(
    private val prefs: SharedPreferences,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val KEY_LAST_CHECK_AT = "last_update_check_at"
        private const val KEY_LAST_SEEN = "last_update_seen"
        // URL de la release vue, pour réévaluer sans appel réseau pendant le cache.
        private const val KEY_LAST_URL = "last_update_url"
        private const val CACHE_MS = 24L * 60L * 60L * 1000L
        private const val LATEST_URL =
            "https://api.github.com/repos/AMstudioai-lgtm/Mydata/releases/latest"
        private const val FALLBACK_URL =
            "https://github.com/AMstudioai-lgtm/Mydata/releases/latest"
    }

    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            // Cache 24 h : réévalue la version mémorisée sans appel réseau.
            val now = System.currentTimeMillis()
            val lastCheck = prefs.getLong(KEY_LAST_CHECK_AT, 0L)
            if (now - lastCheck < CACHE_MS && lastCheck <= now) {
                val seen = prefs.getString(KEY_LAST_SEEN, null).orEmpty()
                if (seen.isBlank()) return@withContext null
                val url = prefs.getString(KEY_LAST_URL, null).orEmpty().ifBlank { FALLBACK_URL }
                return@withContext if (isNewer(seen, currentVersion)) UpdateInfo(seen, url) else null
            }
            val request = Request.Builder()
                .url(LATEST_URL)
                .addHeader("Accept", "application/vnd.github+json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(body)
                // tag_name au format vX.Y.Z, sans le préfixe v pour la comparaison et l'affichage.
                val remote = json.optString("tag_name", "").trim().removePrefix("v").removePrefix("V")
                val url = json.optString("html_url", "").trim().ifBlank { FALLBACK_URL }
                if (remote.isBlank()) return@withContext null
                // Mémorise le résultat même sans mise à jour, pour ne pas spammer l'API.
                prefs.edit()
                    .putLong(KEY_LAST_CHECK_AT, now)
                    .putString(KEY_LAST_SEEN, remote)
                    .putString(KEY_LAST_URL, url)
                    .apply()
                return@withContext if (isNewer(remote, currentVersion)) UpdateInfo(remote, url) else null
            }
        } catch (_: Exception) {
            // Toute erreur (réseau, parse, timeout) : pas de mise à jour proposée.
            null
        }
    }

    // Compare deux versions partie par partie en numérique, donc 1.10 > 1.9.
    // Les segments absents valent 0, les segments non numériques aussi.
    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.trim().removePrefix("v").removePrefix("V").split(".")
        val c = current.trim().removePrefix("v").removePrefix("V").split(".")
        val size = maxOf(r.size, c.size)
        for (i in 0 until size) {
            val rv = r.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            val cv = c.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            if (rv != cv) return rv > cv
        }
        return false
    }
}
