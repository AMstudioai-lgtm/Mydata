package com.example.data.images

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
 * Accès REST au Storage Supabase (bucket `row-images`).
 *
 * Même modèle que [com.example.data.sync.SupabaseSyncClient] : en-têtes `apikey` +
 * `Authorization: Bearer`, relance unique sur 401 après [onTokenExpired].
 */
class SupabaseStorageClient(
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
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Téléverse [bytes] vers `$bucket/$path`, en écrasant l'objet existant (`x-upsert: true`).
     * Lève [StorageException] en cas d'échec.
     */
    suspend fun upload(bucket: String, path: String, bytes: ByteArray, contentType: String) =
        withContext(Dispatchers.IO) {
            val mediaType = try {
                contentType.toMediaType()
            } catch (_: IllegalArgumentException) {
                throw StorageException("Type de contenu invalide : $contentType")
            }
            val url = "${baseUrl.trimEnd('/')}/storage/v1/object/$bucket/$path"
            executeWithRetry(url) { builder ->
                builder.post(bytes.toRequestBody(mediaType)).header("x-upsert", "true")
            }.use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    throw StorageException("Téléversement impossible (${response.code}) ${body.take(200)}")
                }
            }
        }

    /**
     * Télécharge l'objet `$bucket/$path` et retourne ses octets.
     * Lève [StorageException] en cas d'échec.
     */
    suspend fun download(bucket: String, path: String): ByteArray = withContext(Dispatchers.IO) {
        val url = "${baseUrl.trimEnd('/')}/storage/v1/object/$bucket/$path"
        executeWithRetry(url) { builder -> builder.get() }.use { response ->
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                throw StorageException("Téléchargement impossible (${response.code}) ${body.take(200)}")
            }
            response.body?.bytes()
                ?: throw StorageException("Téléchargement impossible : réponse vide")
        }
    }

    /**
     * Supprime les objets listés dans [paths] (chemins relatifs au bucket).
     * Sans effet si la liste est vide. Lève [StorageException] en cas d'échec.
     */
    suspend fun remove(bucket: String, paths: List<String>) = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) return@withContext
        val url = "${baseUrl.trimEnd('/')}/storage/v1/object/$bucket"
        val body = JSONObject().put("prefixes", JSONArray(paths)).toString()
        executeWithRetry(url) { builder -> builder.delete(body.toRequestBody(JSON)) }.use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string().orEmpty()
                throw StorageException("Suppression impossible (${response.code}) ${error.take(200)}")
            }
        }
    }

    private suspend fun executeWithRetry(
        url: String,
        build: (Request.Builder) -> Request.Builder
    ): okhttp3.Response {
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
        return httpClient.newCall(authorized).execute()
    }
}

/** Échec d'une opération sur le bucket (upload, download, suppression). */
class StorageException(message: String) : Exception(message)
