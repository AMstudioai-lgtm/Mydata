package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class JournalRepository(private val db: AppDatabase) {
    private val journalDao = db.journalDao()
    private val propertyDao = db.propertyDao()
    private val entryRowDao = db.entryRowDao()

    fun watchJournals(): Flow<List<JournalEntity>> = journalDao.watchAll()

    suspend fun createJournal(customName: String? = null): String {
        val count = journalDao.count() + 1
        val id = UUID.randomUUID().toString()
        val name = if (!customName.isNullOrBlank()) customName else "Database $count"
        journalDao.insert(JournalEntity(id = id, name = name))
        return id
    }

    suspend fun deleteJournal(id: String) {
        journalDao.deleteById(id)
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
        val jsonArray = JSONArray()
        options.forEach { jsonArray.put(it) }
        val optionsJson = jsonArray.toString()

        propertyDao.insert(
            PropertyEntity(
                id = UUID.randomUUID().toString(),
                journalId = journalId,
                name = name.trim(),
                kind = kind,
                optionsJson = optionsJson,
                sortOrder = order
            )
        )
    }

    suspend fun deleteProperty(propertyId: String) {
        propertyDao.deleteById(propertyId)
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
        val jsonArray = JSONArray()
        options.forEach { jsonArray.put(it) }
        val optionsJson = jsonArray.toString()
        propertyDao.updateProperty(propertyId, name.trim(), kind, optionsJson)
    }

    suspend fun reorderProperties(orderedPropertyIds: List<String>) {
        orderedPropertyIds.forEachIndexed { index, propId ->
            propertyDao.updateSortOrder(propId, index)
        }
    }

    fun watchRows(journalId: String): Flow<List<EntryRowEntity>> =
        entryRowDao.watchByJournal(journalId)

    suspend fun addRow(journalId: String): String {
        val id = UUID.randomUUID().toString()
        entryRowDao.insert(
            EntryRowEntity(
                id = id,
                journalId = journalId
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
        entryRowDao.updateValues(rowId, jsonObj.toString())
    }

    suspend fun deleteRow(rowId: String) {
        entryRowDao.deleteById(rowId)
    }
}
