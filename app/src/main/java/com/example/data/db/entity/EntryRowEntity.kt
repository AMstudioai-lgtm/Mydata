package com.example.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry_rows",
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
data class EntryRowEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "journal_id") val journalId: String,
    @ColumnInfo(name = "values_json") val valuesJson: String = "{}",
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
