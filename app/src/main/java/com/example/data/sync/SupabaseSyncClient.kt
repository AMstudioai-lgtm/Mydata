package com.example.data.sync

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Accès REST aux tables cloud (PostgREST).
 *
 * Les horodatages sont générés à partir de l'horloge de l'appareil, corrigée par l'en-tête
 * `Date` des réponses. Sans cette correction, un appareil dont l'horloge dérive ferait
 * gagner ses modifications à tort lors d'un conflit last-write-wins.
 */
class SupabaseSyncClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val tokenProvider: suspend () -> String?,
    private val onTokenExpired: suspend () -> Boolean,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "SupabaseSyncClient"
        private val JSON = "application/json; charset=utf-8".toMediaType()

        fun nowWithOffset(offsetMs: Long): Long = System.currentTimeMillis() + offsetMs
    }

    /** Décalage entre l'horloge de l'appareil et celle du serveur, en millisecondes. */
    @Volatile
    private var clockOffsetMs: Long = 0L

    /** Horodatage cohérent avec le serveur, utilisé pour les champs updated_at. */
    fun now(): Long = nowWithOffset(clockOffsetMs)

    /**
     * Lit les lignes modifiées strictement après [since].
     * Retourne les lignes et l'horodatage maximum observé (0 si aucune ligne).
     */
    suspend fun pull(table: String, userId: String, since: Long): PullResult = withContext(Dispatchers.IO) {
        val cursor = if (since > 0) since + 1 else 0L
        val url = "$baseUrl/rest/v1/$table?select=*&user_id=eq.$userId&updated_at=gt.$cursor&order=updated_at.asc&limit=1000"
        executeWithRetry(url) { request -> request.get() }.use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw SyncException("Lecture $table impossible (${response.code}) ${body.take(200)}")
            }
            val rows = JSONArray(body).let { array ->
                (0 until array.length()).map { array.getJSONObject(it) }
            }
            PullResult(
                rows = rows,
                maxUpdatedAt = rows.maxOfOrNull { it.optLong("updated_at") } ?: 0L
            )
        }
    }

    /**
     * Envoie les lignes en une seule requête d'upsert.
     * `resolution=merge-duplicates` fusionne sur la clé primaire, donc met à jour les lignes
     * existantes et insère les nouvelles.
     */
    suspend fun push(table: String, rows: List<JSONObject>) = withContext(Dispatchers.IO) {
        if (rows.isEmpty()) return@withContext
        val body = JSONArray().apply { rows.forEach { put(it) } }.toString()
        val request = Request.Builder()
            .url("$baseUrl/rest/v1/$table")
            .addHeader("apikey", apiKey)
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .post(body.toRequestBody(JSON))
            .build()
        execute(request)
    }

    private suspend fun executeWithRetry(url: String, build: (Request.Builder) -> Request.Builder): okhttp3.Response {
        var response = execute(build(Request.Builder().url(url)).build())
        if (response.code == 401 && onTokenExpired()) {
            response.close()
            response = execute(build(Request.Builder().url(url)).build())
        }
        return response
    }

    private suspend fun execute(request: Request): okhttp3.Response {
        val token = tokenProvider()
        val authorized = request.newBuilder()
            .header("apikey", apiKey)
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        val response = httpClient.newCall(authorized).execute()
        readClockOffset(response)
        return response
    }

    /** Aligne l'horloge locale sur celle du serveur, sans dérive persistante. */
    private fun readClockOffset(response: okhttp3.Response) {
        val dateHeader = response.header("Date") ?: return
        val serverTime = runCatching {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.parse(dateHeader)?.time
        }.getOrNull() ?: return
        clockOffsetMs = serverTime - System.currentTimeMillis()
    }
}

class SyncException(message: String) : Exception(message)

data class PullResult(val rows: List<JSONObject>, val maxUpdatedAt: Long)
