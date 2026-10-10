package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.auth.SupabaseAuthService
import com.example.data.db.AppDatabase
import com.example.data.repository.JournalRepository
import com.example.data.session.SessionProvider
import com.example.data.session.asSessionProvider
import com.example.data.sync.SupabaseSyncClient
import com.example.data.sync.SyncPreferences
import com.example.data.sync.SyncRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val authService: SupabaseAuthService = SupabaseAuthService(appContext)
    private val sessionProvider: SessionProvider = authService.asSessionProvider()

    val journalRepository: JournalRepository = JournalRepository(database, sessionProvider)

    /**
     * Les identifiants Supabase proviennent du BuildConfig, alimenté par le plugin `secrets`
     * depuis `.env` (local, gitignoré) avec `.env.example` comme valeurs de repli.
     * Aucune clé n'est écrite dans le code source.
     */
    private val syncClient: SupabaseSyncClient? =
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            SupabaseSyncClient(
                baseUrl = BuildConfig.SUPABASE_URL,
                apiKey = BuildConfig.SUPABASE_KEY,
                tokenProvider = { authService.validAccessToken() },
                onTokenExpired = { authService.refreshSession() }
            )
        } else {
            null
        }

    val syncRepository: SyncRepository? = syncClient?.let { client ->
        SyncRepository(
            db = database,
            client = client,
            prefs = SyncPreferences(appContext),
            sessionProvider = { authService.getCachedSession() },
            authService = authService
        )
    }
}
