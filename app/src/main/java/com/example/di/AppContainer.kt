package com.example.di

import android.content.Context
import com.example.R
import com.example.data.auth.FirebaseAuthService
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.images.ImageStore
import com.example.data.repository.JournalRepository
import com.example.data.session.SessionProvider
import com.example.data.session.asSessionProvider
import com.example.data.sync.FirestoreSyncClient
import com.example.data.sync.SyncPreferences
import com.example.data.sync.SyncRepository
import com.example.data.update.UpdateChecker
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val authService: FirebaseAuthService = FirebaseAuthService()
    private val sessionProvider: SessionProvider = authService.asSessionProvider()

    val journalRepository: JournalRepository = JournalRepository(database, sessionProvider)

    /**
     * Instance Firestore nommée provisionnée pour ce projet AI Studio
     * (identifiant issu de R.string.firestore_database_id, jamais la base par défaut).
     */
    private val firestore: FirebaseFirestore by lazy {
        val databaseId = appContext.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val syncClient: FirestoreSyncClient by lazy {
        FirestoreSyncClient(firestore = firestore)
    }

    private val imageStore: ImageStore = ImageStore(appContext)

    val backupManager: BackupManager = BackupManager(
        context = appContext,
        db = database,
        sessionProvider = sessionProvider,
        imageStore = imageStore
    )

    val syncRepository: SyncRepository by lazy {
        SyncRepository(
            db = database,
            client = syncClient,
            prefs = SyncPreferences(appContext),
            sessionProvider = { authService.getCachedSession() }
        )
    }

    private val syncPrefs = appContext.getSharedPreferences("myapp_sync_prefs", Context.MODE_PRIVATE)

    val updateChecker: UpdateChecker = UpdateChecker(
        prefs = syncPrefs,
        client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    )
}
