package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.resolvePropertyFormat
import com.example.data.session.SessionProvider
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JournalRepository(
    private val db: AppDatabase,
    private val session: SessionProvider
) {
    private val journalDao = db.journalDao()
    private val propertyDao = db.propertyDao()
    private val entryRowDao = db.entryRowDao()

    fun watchJournals(userId: String?): Flow<List<JournalEntity>> =
        journalDao.watchAllForUser(userId)

    // Rattache les lignes orphelines (créées hors-ligne, user_id NULL) au compte connecté.
    suspend fun adoptOrphans(userId: String) {
        journalDao.adoptOrphans(userId)
        propertyDao.adoptOrphans(userId)
        entryRowDao.adoptOrphans(userId)
    }

    suspend fun createJournal(customName: String? = null): String {
        val count = journalDao.count() + 1
        val id = UUID.randomUUID().toString()
        val name = if (!customName.isNullOrBlank()) customName else "Database $count"
        val now = System.currentTimeMillis()
        journalDao.upsert(
            JournalEntity(
                id = id,
                name = name,
                createdAt = now,
                updatedAt = now,
                userId = session.currentUserId()
            )
        )
        return id
    }

    /**
     * Suppression logique : la ligne reste en base avec deleted_at renseigné pour que la
     * suppression puisse être propagée au cloud. Le GC supprime les tombes après 30 jours.
     */
    suspend fun deleteJournal(id: String) {
        val now = System.currentTimeMillis()
        journalDao.softDelete(id, now)
    }

    fun watchProps(journalId: String, userId: String?): Flow<List<PropertyEntity>> =
        propertyDao.watchByJournalForUser(journalId, userId)

    suspend fun addProperty(
        journalId: String,
        name: String,
        kind: String,
        options: List<String> = emptyList(),
        format: String? = null
    ) {
        require(name.isNotBlank()) { "Nom de propriété requis" }
        require(kind in setOf("text", "number", "date", "select")) { "Type invalide: $kind" }
        if (kind == "select") {
            require(options.isNotEmpty()) { "Options requises pour le type sélection" }
        }
        // Refuse un format porté par un autre kind, et comble l'absence de format sur number.
        val resolvedFormat = resolvePropertyFormat(kind, format)
        val order = propertyDao.countByJournal(journalId)
        val optionsJson = JSONArray().apply { options.forEach { put(it) } }.toString()
        val now = System.currentTimeMillis()

        propertyDao.upsert(
            PropertyEntity(
                id = UUID.randomUUID().toString(),
                journalId = journalId,
                name = name.trim(),
                kind = kind,
                format = resolvedFormat,
                optionsJson = optionsJson,
                sortOrder = order,
                updatedAt = now,
                userId = session.currentUserId()
            )
        )
    }

    suspend fun deleteProperty(propertyId: String) {
        propertyDao.softDelete(propertyId, System.currentTimeMillis())
    }

    suspend fun updateProperty(
        propertyId: String,
        name: String,
        kind: String,
        options: List<String> = emptyList(),
        format: String? = null
    ) {
        require(name.isNotBlank()) { "Nom de propriété requis" }
        require(kind in setOf("text", "number", "date", "select")) { "Type invalide: $kind" }
        if (kind == "select") {
            require(options.isNotEmpty()) { "Options requises pour le type sélection" }
        }
        // Même règle qu'à la création : un passage de number à un autre kind remet format à NULL.
        val resolvedFormat = resolvePropertyFormat(kind, format)
        val optionsJson = JSONArray().apply { options.forEach { put(it) } }.toString()
        propertyDao.updateMeta(
            id = propertyId,
            name = name.trim(),
            kind = kind,
            optionsJson = optionsJson,
            format = resolvedFormat,
            updatedAt = System.currentTimeMillis(),
            userId = session.currentUserId()
        )
    }

    suspend fun renameJournal(journalId: String, name: String) {
        journalDao.updateMeta(
            id = journalId,
            name = name,
            updatedAt = System.currentTimeMillis(),
            userId = session.currentUserId()
        )
    }

    suspend fun reorderProperties(orderedPropertyIds: List<String>) {
        val now = System.currentTimeMillis()
        orderedPropertyIds.forEachIndexed { index, propId ->
            propertyDao.updateSortOrder(propId, index, now)
        }
    }

    fun watchRows(journalId: String, userId: String?): Flow<List<EntryRowEntity>> =
        entryRowDao.watchByJournalForUser(journalId, userId)

    suspend fun addRow(journalId: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        entryRowDao.upsert(
            EntryRowEntity(
                id = id,
                journalId = journalId,
                createdAt = now,
                updatedAt = now,
                userId = session.currentUserId()
            )
        )
        return id
    }

    suspend fun updateCell(rowId: String, propId: String, value: Any?) {
        val row = entryRowDao.getById(rowId) ?: return
        val jsonObj = try {
            JSONObject(row.valuesJson)
        } catch (_: Exception) {
            JSONObject()
        }

        if (value == null) {
            jsonObj.remove(propId)
        } else {
            jsonObj.put(propId, value)
        }
        entryRowDao.updateValues(
            id = rowId,
            valuesJson = jsonObj.toString(),
            updatedAt = System.currentTimeMillis(),
            // Conserve le propriétaire déjà connu, sinon annexe la ligne au compte connecté.
            userId = row.userId ?: session.currentUserId()
        )
    }

    suspend fun deleteRow(rowId: String) {
        entryRowDao.softDelete(rowId, System.currentTimeMillis())
    }
}
