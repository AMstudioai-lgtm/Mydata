package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.JournalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journals ORDER BY created_at ASC")
    fun watchAll(): Flow<List<JournalEntity>>

    @Query("SELECT COUNT(*) FROM journals")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(journal: JournalEntity)

    @Query("DELETE FROM journals WHERE id = :id")
    suspend fun deleteById(id: String)
}
