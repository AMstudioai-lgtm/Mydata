package com.example.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "properties",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntity::class,
            parentColumns = ["id"],
            childColumns = ["journal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journal_id")]
)
data class PropertyEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "journal_id") val journalId: String,
    val name: String,
    val kind: String, // "text" | "number" | "date" | "select"
    // Sous-format d'affichage, uniquement pour kind == "number". null vaut "plain".
    // Voir com.example.data.resolvePropertyFormat pour la règle de validation.
    val format: String? = null,
    @ColumnInfo(name = "options_json") val optionsJson: String = "[]",
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "user_id") val userId: String? = null
)
