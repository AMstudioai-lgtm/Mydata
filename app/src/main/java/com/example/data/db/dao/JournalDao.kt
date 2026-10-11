package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals WHERE deleted_at IS NULL ORDER BY created_at ASC")
    fun watchAll(): Flow<List<JournalEntity>>

    // Filtrage par compte : les orphelines (user_id NULL) restent visibles
    // pour être adoptées à la prochaine connexion (cf. adoptOrphans).
    // `IS` et non `=` car `= NULL` ne matche jamais en SQLite.
    @Query("SELECT * FROM journals WHERE (user_id IS :userId OR user_id IS NULL) AND deleted_at IS NULL ORDER BY created_at ASC")
    fun watchAllForUser(userId: String?): Flow<List<JournalEntity>>

    @Query("SELECT COUNT(*) FROM journals")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM journals WHERE user_id IS NULL AND deleted_at IS NULL")
    suspend fun countOrphans(): Int

    @Query("UPDATE journals SET user_id = :userId WHERE user_id IS NULL AND deleted_at IS NULL")
    suspend fun adoptOrphans(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(journal: JournalEntity)

    @Query("SELECT * FROM journals WHERE id = :id")
    suspend fun getById(id: String): JournalEntity?

    @Query("UPDATE journals SET name = :name, updated_at = :updatedAt, user_id = :userId WHERE id = :id")
    suspend fun updateMeta(id: String, name: String, updatedAt: Long, userId: String?)

    @Query("UPDATE journals SET deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :id AND deleted_at IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("SELECT * FROM journals WHERE user_id = :userId AND updated_at > :since ORDER BY updated_at ASC")
    suspend fun pendingPush(userId: String, since: Long): List<JournalEntity>

    /**
     * Purge des tombes anciennes. La condition sur updated_at garantit qu'une suppression
     * encore en attente d'envoi n'est jamais effacée avant d'avoir atteint le cloud.
     */
    @Query("DELETE FROM journals WHERE deleted_at IS NOT NULL AND deleted_at < :olderThan AND (user_id IS NULL OR updated_at <= :pushCursor)")
    suspend fun purgeTombstones(olderThan: Long, pushCursor: Long): Int

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun hardDeleteById(id: String)
}
