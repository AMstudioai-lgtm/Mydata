package com.example.data.sync

import androidx.annotation.Keep
import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import com.example.data.images.rowImagesFromJson
import com.example.data.images.writeImagesToValuesJson
import com.example.domain.logic.resolvePropertyFormat
import com.example.domain.model.IMAGES_KEY
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

private val ALLOWED_KINDS = setOf("text", "number", "date", "select")

@Keep
@IgnoreExtraProperties
data class FirestoreJournalDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val clientCreatedAt: Long = 0L,
    val clientUpdatedAt: Long = 0L,
    val deletedAt: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestorePropertyDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val journalId: String = "",
    val name: String = "",
    val kind: String = "text",
    val format: String? = null,
    val options: List<String> = emptyList(),
    val sortOrder: Int = 0,
    val clientUpdatedAt: Long = 0L,
    val deletedAt: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreEntryRowDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val journalId: String = "",
    val values: Map<String, Any> = emptyMap(),
    val clientCreatedAt: Long = 0L,
    val clientUpdatedAt: Long = 0L,
    val deletedAt: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object FirestoreSyncMapper {

    fun journalToCreateMap(entity: JournalEntity, userId: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "name" to sanitizeLabel(entity.name, "Sans titre"),
            "clientCreatedAt" to entity.createdAt.coerceAtLeast(0L),
            "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "deletedAt" to entity.deletedAt?.takeIf { it > 0L }
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    fun journalToUpdateMap(entity: JournalEntity): Map<String, Any> = mapOf(
        "name" to sanitizeLabel(entity.name, "Sans titre"),
        "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp(),
        "deletedAt" to (entity.deletedAt?.takeIf { it > 0L } ?: FieldValue.delete())
    )

    fun journalFromDoc(doc: FirestoreJournalDoc): JournalEntity = JournalEntity(
        id = doc.id,
        name = doc.name.ifBlank { "Sans titre" },
        createdAt = doc.clientCreatedAt.takeIf { it > 0L }
            ?: doc.createdAt?.toDate()?.time
            ?: System.currentTimeMillis(),
        updatedAt = doc.clientUpdatedAt.takeIf { it > 0L }
            ?: doc.updatedAt?.toDate()?.time
            ?: System.currentTimeMillis(),
        deletedAt = doc.deletedAt?.takeIf { it > 0L },
        userId = doc.userId.ifBlank { null }
    )

    fun propertyToCreateMap(entity: PropertyEntity, userId: String): Map<String, Any> {
        val safeKind = entity.kind.takeIf { it in ALLOWED_KINDS } ?: "text"
        val safeFormat = safeResolveFormat(safeKind, entity.format)
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "journalId" to entity.journalId,
            "name" to sanitizeLabel(entity.name, "Colonne"),
            "kind" to safeKind,
            "format" to safeFormat,
            "options" to parseOptionsJson(entity.optionsJson).take(200),
            "sortOrder" to entity.sortOrder.coerceIn(0, 10000),
            "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "deletedAt" to entity.deletedAt?.takeIf { it > 0L }
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    fun propertyToUpdateMap(entity: PropertyEntity): Map<String, Any> {
        val safeKind = entity.kind.takeIf { it in ALLOWED_KINDS } ?: "text"
        val safeFormat = safeResolveFormat(safeKind, entity.format)
        return mapOf(
            "name" to sanitizeLabel(entity.name, "Colonne"),
            "kind" to safeKind,
            "format" to (safeFormat ?: FieldValue.delete()),
            "options" to parseOptionsJson(entity.optionsJson).take(200),
            "sortOrder" to entity.sortOrder.coerceIn(0, 10000),
            "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp(),
            "deletedAt" to (entity.deletedAt?.takeIf { it > 0L } ?: FieldValue.delete())
        )
    }

    fun propertyFromDoc(doc: FirestorePropertyDoc): PropertyEntity {
        val safeKind = doc.kind.takeIf { it in ALLOWED_KINDS } ?: "text"
        val safeFormat = safeResolveFormat(safeKind, doc.format)
        val optionsJson = JSONArray().apply { doc.options.forEach { put(it) } }.toString()
        return PropertyEntity(
            id = doc.id,
            journalId = doc.journalId,
            name = doc.name.ifBlank { "Colonne" },
            kind = safeKind,
            format = safeFormat,
            optionsJson = optionsJson,
            sortOrder = doc.sortOrder.coerceAtLeast(0),
            updatedAt = doc.clientUpdatedAt.takeIf { it > 0L }
                ?: doc.updatedAt?.toDate()?.time
                ?: System.currentTimeMillis(),
            deletedAt = doc.deletedAt?.takeIf { it > 0L },
            userId = doc.userId.ifBlank { null }
        )
    }

    fun entryRowToCreateMap(entity: EntryRowEntity, userId: String): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "journalId" to entity.journalId,
            "values" to valuesJsonToFirestoreMap(entity.valuesJson),
            "clientCreatedAt" to entity.createdAt.coerceAtLeast(0L),
            "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "deletedAt" to entity.deletedAt?.takeIf { it > 0L }
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    fun entryRowToUpdateMap(entity: EntryRowEntity): Map<String, Any> = mapOf(
        "values" to valuesJsonToFirestoreMap(entity.valuesJson),
        "clientUpdatedAt" to entity.updatedAt.coerceAtLeast(0L),
        "updatedAt" to FieldValue.serverTimestamp(),
        "deletedAt" to (entity.deletedAt?.takeIf { it > 0L } ?: FieldValue.delete())
    )

    fun entryRowFromDoc(
        doc: FirestoreEntryRowDoc,
        existingLocal: EntryRowEntity? = null
    ): EntryRowEntity {
        var valuesJson = firestoreMapToValuesJson(doc.values)
        // Préserve les photos locales de l'appareil si la synchronisation cloud ne transporte pas de binaires.
        if (existingLocal != null) {
            val localImages = rowImagesFromJson(existingLocal.valuesJson)
            val remoteImages = rowImagesFromJson(valuesJson)
            if (localImages.isNotEmpty() && remoteImages.none { !it.uri.isNullOrBlank() }) {
                valuesJson = writeImagesToValuesJson(valuesJson, localImages)
            }
        }
        return EntryRowEntity(
            id = doc.id,
            journalId = doc.journalId,
            valuesJson = valuesJson,
            createdAt = doc.clientCreatedAt.takeIf { it > 0L }
                ?: doc.createdAt?.toDate()?.time
                ?: System.currentTimeMillis(),
            updatedAt = doc.clientUpdatedAt.takeIf { it > 0L }
                ?: doc.updatedAt?.toDate()?.time
                ?: System.currentTimeMillis(),
            deletedAt = doc.deletedAt?.takeIf { it > 0L },
            userId = doc.userId.ifBlank { null }
        )
    }

    fun parseOptionsJson(optionsJson: String): List<String> = try {
        val arr = JSONArray(optionsJson)
        buildList {
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "").trim()
                if (item.isNotEmpty()) add(item.take(200))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun valuesJsonToFirestoreMap(valuesJson: String): Map<String, Any> {
        val obj = try {
            JSONObject(valuesJson)
        } catch (_: Exception) {
            return emptyMap()
        }
        val result = LinkedHashMap<String, Any>()
        val keys = obj.keys()
        while (keys.hasNext() && result.size < 200) {
            val key = keys.next()
            val safeKey = encodeFirestoreMapKey(key)
            if (key == IMAGES_KEY) {
                // Les photos binaires restent gérées localement (ZIP backup) : on ne pousse
                // pas d'URIs locales propres à un seul téléphone, uniquement les légendes/métadonnées.
                val images = rowImagesFromJson(valuesJson)
                if (images.isNotEmpty()) {
                    result[safeKey] = images.take(20).map { img ->
                        buildMap<String, Any> {
                            put("caption", img.caption.take(300))
                            if (!img.remote.isNullOrBlank()) {
                                put("remote", img.remote.take(500))
                            }
                        }
                    }
                }
                continue
            }
            val value = obj.opt(key)
            val converted = convertJsonValue(value)
            if (converted != null) {
                result[safeKey] = converted
            }
        }
        return result
    }

    fun firestoreMapToValuesJson(map: Map<String, Any>): String {
        val obj = JSONObject()
        for ((key, value) in map) {
            obj.put(decodeFirestoreMapKey(key), toJsonValue(value))
        }
        return obj.toString()
    }

    /**
     * Firestore réserve les noms de champs correspondant au motif `__.*__` (ex. `__images__`, `__notes__`).
     * On les traduit en clés sûres côté Firestore et on les restaure à l'identique côté Room.
     */
    private fun encodeFirestoreMapKey(key: String): String =
        if (key.length >= 4 && key.startsWith("__") && key.endsWith("__")) {
            "mydata_reserved_" + key.substring(2, key.length - 2)
        } else {
            key
        }

    private fun decodeFirestoreMapKey(key: String): String =
        if (key.startsWith("mydata_reserved_") && key.length > "mydata_reserved_".length) {
            "__" + key.removePrefix("mydata_reserved_") + "__"
        } else {
            key
        }

    private fun convertJsonValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is Boolean -> value
        is Number -> {
            val d = value.toDouble()
            if (d.isFinite()) {
                if (value is Int || value is Long) value.toLong() else d
            } else {
                null
            }
        }
        is String -> value.take(4000)
        is JSONArray -> {
            buildList {
                for (i in 0 until minOf(value.length(), 100)) {
                    convertJsonValue(value.opt(i))?.let { add(it) }
                }
            }
        }
        is JSONObject -> {
            buildMap<String, Any> {
                val it = value.keys()
                while (it.hasNext() && size < 50) {
                    val k = it.next()
                    convertJsonValue(value.opt(k))?.let { v -> put(k, v) }
                }
            }
        }
        else -> value.toString().take(4000)
    }

    private fun toJsonValue(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply {
            value.forEach { (k, v) ->
                if (k is String) put(k, toJsonValue(v))
            }
        }
        is List<*> -> JSONArray().apply {
            value.forEach { item -> put(toJsonValue(item)) }
        }
        is Boolean, is Number, is String -> value
        else -> value.toString()
    }

    private fun sanitizeLabel(raw: String, fallback: String): String =
        raw.trim().ifBlank { fallback }.take(200)

    private fun safeResolveFormat(kind: String, format: String?): String? = try {
        resolvePropertyFormat(kind, format)
    } catch (_: Exception) {
        if (kind == "number") "plain" else null
    }
}
