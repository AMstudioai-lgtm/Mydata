package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties WHERE journal_id = :journalId AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun watchByJournal(journalId: String): Flow<List<PropertyEntity>>

    // Même règle que les journaux : lignes du compte + orphelines en attente d'adoption.
    @Query("SELECT * FROM properties WHERE journal_id = :journalId AND (user_id IS :userId OR user_id IS NULL) AND deleted_at IS NULL ORDER BY sort_order ASC")
    fun watchByJournalForUser(journalId: String, userId: String?): Flow<List<PropertyEntity>>

    @Query("SELECT COUNT(*) FROM properties WHERE journal_id = :journalId AND deleted_at IS NULL")
    suspend fun countByJournal(journalId: String): Int

    @Query("UPDATE properties SET user_id = :userId WHERE user_id IS NULL AND deleted_at IS NULL")
    suspend fun adoptOrphans(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(property: PropertyEntity)

    @Query("SELECT * FROM properties WHERE id = :id")
    suspend fun getById(id: String): PropertyEntity?

    @Query("UPDATE properties SET name = :name, kind = :kind, options_json = :optionsJson, format = :format, updated_at = :updatedAt, user_id = :userId WHERE id = :id")
    suspend fun updateMeta(id: String, name: String, kind: String, optionsJson: String, format: String?, updatedAt: Long, userId: String?)

    @Query("UPDATE properties SET sort_order = :order, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int, updatedAt: Long)

    @Query("UPDATE properties SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long)

    // Purge en cascade logique : appelée à la purge du journal parent.
    @Query("DELETE FROM properties WHERE journal_id = :journalId")
    suspend fun purgeByJournal(journalId: String)

    // Lecture complète pour l'export de sauvegarde.
    @Query("SELECT * FROM properties")
    suspend fun getAllProperties(): List<PropertyEntity>

    @Query("SELECT * FROM properties WHERE user_id = :userId AND updated_at > :since ORDER BY updated_at ASC")
    suspend fun pendingPush(userId: String, since: Long): List<PropertyEntity>

    @Query("DELETE FROM properties WHERE deleted_at IS NOT NULL AND deleted_at < :olderThan AND (user_id IS NULL OR updated_at <= :pushCursor)")
    suspend fun purgeTombstones(olderThan: Long, pushCursor: Long): Int

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
