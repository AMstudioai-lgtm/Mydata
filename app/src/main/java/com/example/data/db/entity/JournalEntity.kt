package com.example.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journals")
data class JournalEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    // Horodatage de dernière modification, sert de référence au conflit last-write-wins.
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    // Suppression logique : null = vivant. Requis pour propager les suppressions au cloud.
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    // Propriétaire cloud. Null = donnée créée hors-ligne, à annexer à la première connexion.
    @ColumnInfo(name = "user_id") val userId: String? = null
)
