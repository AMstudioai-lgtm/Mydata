package com.example.data.sync

import com.example.data.auth.AuthSession
import com.example.data.auth.SupabaseAuthService
import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Tables synchronisées, dans l'ordre imposé par les clés étrangères. */
private val TABLES_IN_DEPENDENCY_ORDER = listOf("journals", "properties", "entry_rows")

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
 * Moteur de synchronisation last-write-wins entre Room (source de vérité locale) et PostgREST.
 *
 * Règles :
 * - pull puis push, table par table, dans l'ordre des dépendances ;
 * - une ligne distante n'écrase la locale que si son `updated_at` est strictement plus récent ;
 * - les suppressions sont propagées par suppressions logiques (tombstones) ;
 * - les curseurs n'avancent qu'après application, donc une reprise est toujours sans perte.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val client: SupabaseSyncClient,
    private val prefs: SyncPreferences,
    private val sessionProvider: () -> AuthSession?,
    private val authService: SupabaseAuthService
) {
    private val _status = MutableStateFlow(SyncStatus(lastSyncAt = prefs.lastSyncAt()))
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val journalDao = db.journalDao()
    private val propertyDao = db.propertyDao()
    private val entryRowDao = db.entryRowDao()

    val isOnlineSyncPossible: Boolean
        get() = sessionProvider()?.accessToken != null

    /** Synchronisation complète. Ne propage jamais une exception à l'appelant. */
    suspend fun syncNow(): SyncOutcome = withContext(Dispatchers.IO) {
        val session = sessionProvider()
        val userId = session?.userId?.takeIf { it.isNotBlank() }
            ?: return@withContext SyncOutcome.Skipped("Aucune session active")

        _status.value = _status.value.copy(isSyncing = true, error = null, phase = "Vérification")

        try {
            // Un jeton proche de l'expiration est renouvelé avant tout appel : sans cela la
            // première synchro après une heure d'utilisation échouerait en 401.
            authService.refreshSession()

            var totalPushed = 0
            var totalPulled = 0

            adoptLocalRows(userId)

            for (table in TABLES_IN_DEPENDENCY_ORDER) {
                _status.value = _status.value.copy(phase = "Réception — $table")
                val pulled = pullTable(table, userId)
                totalPulled += pulled

                _status.value = _status.value.copy(phase = "Envoi — $table")
                val pushed = pushTable(table, userId)
                totalPushed += pushed
            }

            purgeOldTombstones()

            val now = System.currentTimeMillis()
            prefs.rememberSyncAt(now)
            _status.value = SyncStatus(
                isSyncing = false,
                phase = "",
                lastSyncAt = now,
                pushedCount = totalPushed,
                pulledCount = totalPulled,
                error = null
            )
            SyncOutcome.Success(totalPushed, totalPulled)
        } catch (e: Exception) {
            val message = e.message ?: "Erreur de synchronisation"
            _status.value = _status.value.copy(isSyncing = false, phase = "", error = message)
            SyncOutcome.Failure(message)
        }
    }

    /**
     * Annexion des données créées hors-ligne au compte qui vient de se connecter.
     * Les identifiants étant des UUID v4, une collision avec une ligne distante est impossible.
     */
    private suspend fun adoptLocalRows(userId: String) {
        if (journalDao.countOrphans() == 0) return
        _status.value = _status.value.copy(phase = "Reprise des données locales")
        journalDao.adoptOrphans(userId)
        propertyDao.adoptOrphans(userId)
        entryRowDao.adoptOrphans(userId)
    }

    private suspend fun pullTable(table: String, userId: String): Int {
        val cursor = prefs.pullCursor(table)
        val result = client.pull(table, userId, cursor)
        var applied = 0
        val maxRemote = result.maxUpdatedAt

        result.rows.forEach { row ->
            applied += when (table) {
                "journals" -> applyJournal(row)
                "properties" -> applyProperty(row)
                else -> applyEntryRow(row)
            }
        }
        prefs.rememberPull(table, maxRemote)
        return applied
    }

    private suspend fun pushTable(table: String, userId: String): Int {
        val cursor = prefs.pushCursor(table)
        val pending: List<JSONObject> = when (table) {
            "journals" -> journalDao.pendingPush(userId, cursor).map { it.toJson(client::now) }
            "properties" -> propertyDao.pendingPush(userId, cursor).map { it.toJson(client::now) }
            else -> entryRowDao.pendingPush(userId, cursor).map { it.toJson(client::now) }
        }
        if (pending.isEmpty()) return 0

        client.push(table, pending)
        val maxPushed = pending.maxOf { it.optLong("updated_at") }
        prefs.rememberPush(table, maxPushed)
        return pending.size
    }

    private suspend fun applyJournal(row: JSONObject): Int {
        val local = journalDao.getById(row.getString("id"))
        // Une ligne distante plus ancienne que la locale perd : la décision est prise par
        // comparaison stricte, jamais par écrasement systématique.
        if (local != null && !ConflictResolver.remoteWins(local.updatedAt, row.optLong("updated_at"))) return 0
        journalDao.upsert(JournalEntity.fromJson(row))
        return 1
    }

    private suspend fun applyProperty(row: JSONObject): Int {
        val local = propertyDao.getById(row.getString("id"))
        if (local != null && !ConflictResolver.remoteWins(local.updatedAt, row.optLong("updated_at"))) return 0
        propertyDao.upsert(PropertyEntity.fromJson(row))
        return 1
    }

    private suspend fun applyEntryRow(row: JSONObject): Int {
        val existing = entryRowDao.getById(row.getString("id"))
        if (existing != null && !ConflictResolver.remoteWins(existing.updatedAt, row.optLong("updated_at"))) return 0
        entryRowDao.upsert(EntryRowEntity.fromJson(row))
        return 1
    }

    /** Supprime définitivement les tombes anciennes, une fois propagées avec certitude. */
    private suspend fun purgeOldTombstones() {
        val threshold = System.currentTimeMillis() - TOMBSTONE_RETENTION_MS
        // Une tombe déjà poussée a un updated_at inférieur ou égal au curseur d'envoi.
        journalDao.purgeTombstones(threshold, prefs.pushCursor("journals"))
        propertyDao.purgeTombstones(threshold, prefs.pushCursor("properties"))
        entryRowDao.purgeTombstones(threshold, prefs.pushCursor("entry_rows"))
    }

    fun clearError() {
        _status.value = _status.value.copy(error = null)
    }
}
