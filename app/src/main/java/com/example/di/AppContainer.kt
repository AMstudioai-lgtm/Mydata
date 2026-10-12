package com.example.di

import android.content.Context
import com.example.BuildConfig
import com.example.data.auth.SupabaseAuthService
import com.example.data.db.AppDatabase
import com.example.data.repository.JournalRepository
import com.example.data.session.SessionProvider
import com.example.data.session.asSessionProvider
import com.example.data.images.ImageStore
import com.example.data.images.SupabaseStorageClient
import com.example.data.sync.SupabaseSyncClient
import com.example.data.sync.SyncPreferences
import com.example.data.sync.SyncRepository
import com.example.data.update.UpdateChecker
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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

    /**
     * Stockage des images de lignes, adossé aux mêmes identifiants Supabase que la
     * synchro. Nul si Supabase n'est pas configuré : la synchro ignore alors les images.
     *
     * Déclaré AVANT syncRepository : Kotlin initialise dans l'ordre de déclaration,
     * et syncRepository en a besoin dès sa construction.
     */
    private val storageClient: SupabaseStorageClient? =
        if (BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()) {
            SupabaseStorageClient(
                baseUrl = BuildConfig.SUPABASE_URL,
                apiKey = BuildConfig.SUPABASE_KEY,
                tokenProvider = { authService.validAccessToken() },
                onTokenExpired = { authService.refreshSession() }
            )
        } else {
            null
        }

    private val imageStore: ImageStore = ImageStore(appContext, storageClient)

    val syncRepository: SyncRepository? = syncClient?.let { client ->
        SyncRepository(
            db = database,
            client = client,
            prefs = SyncPreferences(appContext),
            sessionProvider = { authService.getCachedSession() },
            authService = authService,
            imageStore = imageStore
        )
    }

    /**
     * Préférences partagées avec la synchro (même fichier `myapp_sync_prefs`,
     * clés disjointes `last_update_*` / `auto_sync`) : un seul fichier à
     * sauvegarder, aucun conflit d'écriture.
     */
    private val syncPrefs = appContext.getSharedPreferences("myapp_sync_prefs", Context.MODE_PRIVATE)

    /**
     * Vérificateur de mises à jour GitHub. Les clients Supabase gardent leur
     * OkHttpClient privé : il n'y a pas de client partageable accessible, donc
     * un client dédié est construit ici avec des timeouts courts.
     */
    val updateChecker: UpdateChecker = UpdateChecker(
        prefs = syncPrefs,
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    )
}
