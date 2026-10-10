package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
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

    fun watchJournals(): Flow<List<JournalEntity>> = journalDao.watchAll()

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

    fun watchProps(journalId: String): Flow<List<PropertyEntity>> =
        propertyDao.watchByJournal(journalId)

    suspend fun addProperty(
        journalId: String,
        name: String,
        kind: String,
        options: List<String> = emptyList()
    ) {
        require(name.isNotBlank()) { "Nom de propriété requis" }
        require(kind in setOf("text", "number", "date", "select")) { "Type invalide: $kind" }
        if (kind == "select") {
            require(options.isNotEmpty()) { "Options requises pour le type sélection" }
        }
        val order = propertyDao.countByJournal(journalId)
        val optionsJson = JSONArray().apply { options.forEach { put(it) } }.toString()
        val now = System.currentTimeMillis()

        propertyDao.upsert(
            PropertyEntity(
                id = UUID.randomUUID().toString(),
                journalId = journalId,
                name = name.trim(),
                kind = kind,
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
        options: List<String> = emptyList()
    ) {
        require(name.isNotBlank()) { "Nom de propriété requis" }
        require(kind in setOf("text", "number", "date", "select")) { "Type invalide: $kind" }
        if (kind == "select") {
            require(options.isNotEmpty()) { "Options requises pour le type sélection" }
        }
        val optionsJson = JSONArray().apply { options.forEach { put(it) } }.toString()
        propertyDao.updateMeta(
            id = propertyId,
            name = name.trim(),
            kind = kind,
            optionsJson = optionsJson,
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

    fun watchRows(journalId: String): Flow<List<EntryRowEntity>> =
        entryRowDao.watchByJournal(journalId)

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
