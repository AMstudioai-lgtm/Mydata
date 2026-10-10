package com.example.data.sync

import com.example.data.db.entity.EntryRowEntity
import com.example.data.db.entity.JournalEntity
import com.example.data.db.entity.PropertyEntity
import org.json.JSONObject

/**
 * Correspondance champ Kotlin <-> colonne PostgREST.
 * Le nom des colonnes est imposé par le SQL de `supabase/schema.sql`, il doit rester identique.
 */

fun JournalEntity.toJson(now: () -> Long): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("created_at", createdAt)
    put("updated_at", if (updatedAt > 0) updatedAt else now())
    put("deleted_at", deletedAt ?: JSONObject.NULL)
    put("user_id", userId ?: JSONObject.NULL)
}

fun PropertyEntity.toJson(now: () -> Long): JSONObject = JSONObject().apply {
    put("id", id)
    put("journal_id", journalId)
    put("name", name)
    put("kind", kind)
    put("options_json", optionsJson)
    put("sort_order", sortOrder)
    put("updated_at", if (updatedAt > 0) updatedAt else now())
    put("deleted_at", deletedAt ?: JSONObject.NULL)
    put("user_id", userId ?: JSONObject.NULL)
}

fun EntryRowEntity.toJson(now: () -> Long): JSONObject = JSONObject().apply {
    put("id", id)
    put("journal_id", journalId)
    put("values_json", valuesJson)
    put("created_at", createdAt)
    put("updated_at", if (updatedAt > 0) updatedAt else now())
    put("deleted_at", deletedAt ?: JSONObject.NULL)
    put("user_id", userId ?: JSONObject.NULL)
}

/** Désérialisation d'une ligne PostgREST. `fromJson` est exposé en fonction libre car une
 * extension sur le Companion d'une autre classe n'est pas résolue depuis ce module. */
fun journalFromJson(row: JSONObject): JournalEntity = JournalEntity(
    id = row.getString("id"),
    name = row.getString("name"),
    createdAt = row.optLong("created_at", 0L),
    updatedAt = row.optLong("updated_at", 0L),
    deletedAt = row.optLong("deleted_at").takeIf { !row.isNull("deleted_at") && it > 0 },
    userId = row.optString("user_id").takeIf { !row.isNull("user_id") && it.isNotBlank() }
)

fun propertyFromJson(row: JSONObject): PropertyEntity = PropertyEntity(
    id = row.getString("id"),
    journalId = row.getString("journal_id"),
    name = row.getString("name"),
    kind = row.getString("kind"),
    optionsJson = row.optString("options_json", "[]"),
    sortOrder = row.optInt("sort_order", 0),
    updatedAt = row.optLong("updated_at", 0L),
    deletedAt = row.optLong("deleted_at").takeIf { !row.isNull("deleted_at") && it > 0 },
    userId = row.optString("user_id").takeIf { !row.isNull("user_id") && it.isNotBlank() }
)

fun entryRowFromJson(row: JSONObject): EntryRowEntity = EntryRowEntity(
    id = row.getString("id"),
    journalId = row.getString("journal_id"),
    valuesJson = row.optString("values_json", "{}"),
    createdAt = row.optLong("created_at", 0L),
    updatedAt = row.optLong("updated_at", 0L),
    deletedAt = row.optLong("deleted_at").takeIf { !row.isNull("deleted_at") && it > 0 },
    userId = row.optString("user_id").takeIf { !row.isNull("user_id") && it.isNotBlank() }
)
