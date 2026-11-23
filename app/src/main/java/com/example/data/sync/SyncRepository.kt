package com.example.data.sync

import com.example.data.auth.AuthSession
import com.example.data.db.AppDatabase
import com.example.domain.logic.ConflictResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Délai avant suppression définitive des lignes supprimées logiquement. */
private const val TOMBSTONE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000

data class SyncStatus(
    val isSyncing: Boolean = false,
    val phase: String = "",
    val lastSyncAt: Long = 0L,
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val error: String? = null
)

sealed interface SyncOutcome {
    data class Success(val pushed: Int, val pulled: Int) : SyncOutcome
    data class Skipped(val reason: String) : SyncOutcome
    data class Failure(val message: String) : SyncOutcome
}

/**
 * Moteur de synchronisation last-write-wins entre Room (source de vérité locale)
 * et Firebase Firestore (`/users/{userId}/journals`, `properties`, `entry_rows`).
 */
class SyncRepository(
    private val db: AppDatabase,
    private val client: FirestoreSyncClient,
    private val prefs: SyncPreferences,
    private val sessionProvider: () -> AuthSession?
) {
    private val _status = MutableStateFlow(SyncStatus(lastSyncAt = prefs.lastSyncAt()))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val journalDao = db.journalDao()
    private val propertyDao = db.propertyDao()
    private val entryRowDao = db.entryRowDao()

    val isOnlineSyncPossible: Boolean
        get() = sessionProvider()?.userId?.isNotBlank() == true

    /** Synchronisation complète. Ne propage jamais une exception à l'appelant. */
    suspend fun syncNow(): SyncOutcome = withContext(Dispatchers.IO) {
        val session = sessionProvider()
        val userId = session?.userId?.takeIf { it.isNotBlank() }
            ?: return@withContext SyncOutcome.Skipped("Connectez-vous avec Google pour synchroniser")

        _status.value = _status.value.copy(isSyncing = true, error = null, phase = "Vérification")

        try {
            var totalPushed = 0
            var totalPulled = 0

            adoptLocalRows(userId)

            // 1. Journals
            _status.value = _status.value.copy(phase = "Réception — tables")
            totalPulled += pullJournals(userId)
            _status.value = _status.value.copy(phase = "Envoi — tables")
            totalPushed += pushJournals(userId)

            // 2. Properties
            _status.value = _status.value.copy(phase = "Réception — colonnes")
            totalPulled += pullProperties(userId)
            _status.value = _status.value.copy(phase = "Envoi — colonnes")
            totalPushed += pushProperties(userId)

            // 3. Entry rows
            _status.value = _status.value.copy(phase = "Réception — lignes")
            totalPulled += pullEntryRows(userId)
            _status.value = _status.value.copy(phase = "Envoi — lignes")
            totalPushed += pushEntryRows(userId)

            purgeOldTombstones(userId)

            val now = System.currentTimeMillis()
            prefs.rememberSyncAt(now, userId)
            _status.value = SyncStatus(
                isSyncing = false,
                phase = "",
                lastSyncAt = now,
                pushedCount = totalPushed,
                pulledCount = totalPulled,
                error = null
            )
            SyncOutcome.Success(totalPushed, totalPulled)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "Erreur de synchronisation Firestore"
            _status.value = _status.value.copy(isSyncing = false, phase = "", error = message)
            SyncOutcome.Failure(message)
        }
    }

    private suspend fun adoptLocalRows(userId: String) {
        if (journalDao.countOrphans() == 0) return
        _status.value = _status.value.copy(phase = "Reprise des données locales")
        journalDao.adoptOrphans(userId)
        propertyDao.adoptOrphans(userId)
        entryRowDao.adoptOrphans(userId)
    }

    private suspend fun pullJournals(userId: String): Int {
        val cursor = prefs.pullCursor(userId, "journals")
        val pullResult = client.pullJournals(userId, cursor).getOrThrow()
        var applied = 0
        for (doc in pullResult.items) {
            if (doc.id.isBlank()) continue
            val local = journalDao.getById(doc.id)
            if (local != null && !ConflictResolver.remoteWins(local.updatedAt, doc.clientUpdatedAt)) {
                continue
            }
            journalDao.upsert(FirestoreSyncMapper.journalFromDoc(doc))
            applied++
        }
        prefs.rememberPull(userId, "journals", pullResult.maxClientUpdatedAt)
        return applied
    }

    private suspend fun pushJournals(userId: String): Int {
        val cursor = prefs.pushCursor(userId, "journals")
        val pending = journalDao.pendingPush(userId, cursor)
        if (pending.isEmpty()) return 0
        val maxPushed = client.pushJournals(userId, pending).getOrThrow()
        prefs.rememberPush(userId, "journals", maxPushed)
        return pending.size
    }

    private suspend fun pullProperties(userId: String): Int {
        val cursor = prefs.pullCursor(userId, "properties")
        val pullResult = client.pullProperties(userId, cursor).getOrThrow()
        var applied = 0
        for (doc in pullResult.items) {
            if (doc.id.isBlank()) continue
            // Évite une violation de clé étrangère si le journal parent n'existe pas localement.
            if (journalDao.getById(doc.journalId) == null) continue
            val local = propertyDao.getById(doc.id)
            if (local != null && !ConflictResolver.remoteWins(local.updatedAt, doc.clientUpdatedAt)) {
                continue
            }
            propertyDao.upsert(FirestoreSyncMapper.propertyFromDoc(doc))
            applied++
        }
        prefs.rememberPull(userId, "properties", pullResult.maxClientUpdatedAt)
        return applied
    }

    private suspend fun pushProperties(userId: String): Int {
        val cursor = prefs.pushCursor(userId, "properties")
        val pending = propertyDao.pendingPush(userId, cursor)
        if (pending.isEmpty()) return 0
        val maxPushed = client.pushProperties(userId, pending).getOrThrow()
        prefs.rememberPush(userId, "properties", maxPushed)
        return pending.size
    }

    private suspend fun pullEntryRows(userId: String): Int {
        val cursor = prefs.pullCursor(userId, "entry_rows")
        val pullResult = client.pullEntryRows(userId, cursor).getOrThrow()
        var applied = 0
        for (doc in pullResult.items) {
            if (doc.id.isBlank()) continue
            if (journalDao.getById(doc.journalId) == null) continue
            val existing = entryRowDao.getById(doc.id)
            if (existing != null && !ConflictResolver.remoteWins(existing.updatedAt, doc.clientUpdatedAt)) {
                continue
            }
            entryRowDao.upsert(FirestoreSyncMapper.entryRowFromDoc(doc, existing))
            applied++
        }
        prefs.rememberPull(userId, "entry_rows", pullResult.maxClientUpdatedAt)
        return applied
    }

    private suspend fun pushEntryRows(userId: String): Int {
        val cursor = prefs.pushCursor(userId, "entry_rows")
        val pending = entryRowDao.pendingPush(userId, cursor)
        if (pending.isEmpty()) return 0
        val maxPushed = client.pushEntryRows(userId, pending).getOrThrow()
        prefs.rememberPush(userId, "entry_rows", maxPushed)
        return pending.size
    }

    private suspend fun purgeOldTombstones(userId: String) {
        val threshold = System.currentTimeMillis() - TOMBSTONE_RETENTION_MS
        journalDao.purgeTombstones(threshold, prefs.pushCursor(userId, "journals"))
        propertyDao.purgeTombstones(threshold, prefs.pushCursor(userId, "properties"))
        entryRowDao.purgeTombstones(threshold, prefs.pushCursor(userId, "entry_rows"))
    }

    fun clearError() {
        _status.value = _status.value.copy(error = null)
    }
}
