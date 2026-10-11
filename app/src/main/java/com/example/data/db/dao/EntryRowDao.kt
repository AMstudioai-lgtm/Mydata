package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.EntryRowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRowDao {
    @Query("SELECT * FROM entry_rows WHERE journal_id = :journalId AND deleted_at IS NULL ORDER BY created_at ASC")
    fun watchByJournal(journalId: String): Flow<List<EntryRowEntity>>

    // Même règle que les journaux : lignes du compte + orphelines en attente d'adoption.
    @Query("SELECT * FROM entry_rows WHERE journal_id = :journalId AND (user_id IS :userId OR user_id IS NULL) AND deleted_at IS NULL ORDER BY created_at ASC")
    fun watchByJournalForUser(journalId: String, userId: String?): Flow<List<EntryRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: EntryRowEntity)

    @Query("UPDATE entry_rows SET user_id = :userId WHERE user_id IS NULL AND deleted_at IS NULL")
    suspend fun adoptOrphans(userId: String): Int

    @Query("SELECT * FROM entry_rows WHERE id = :id")
    suspend fun getById(id: String): EntryRowEntity?

    @Query("UPDATE entry_rows SET values_json = :valuesJson, updated_at = :updatedAt, user_id = :userId WHERE id = :id")
    suspend fun updateValues(id: String, valuesJson: String, updatedAt: Long, userId: String?)

    @Query("UPDATE entry_rows SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("SELECT * FROM entry_rows WHERE user_id = :userId AND updated_at > :since ORDER BY updated_at ASC")
    suspend fun pendingPush(userId: String, since: Long): List<EntryRowEntity>

    @Query("DELETE FROM entry_rows WHERE deleted_at IS NOT NULL AND deleted_at < :olderThan AND (user_id IS NULL OR updated_at <= :pushCursor)")
    suspend fun purgeTombstones(olderThan: Long, pushCursor: Long): Int

    @Query("DELETE FROM entry_rows WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
