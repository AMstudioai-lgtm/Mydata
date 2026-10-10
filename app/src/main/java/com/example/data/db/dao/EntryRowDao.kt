package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.EntryRowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRowDao {
    @Query("SELECT * FROM entry_rows WHERE journal_id = :journalId ORDER BY created_at ASC")
    fun watchByJournal(journalId: String): Flow<List<EntryRowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(row: EntryRowEntity)

    @Query("SELECT * FROM entry_rows WHERE id = :id")
    suspend fun getById(id: String): EntryRowEntity?

    @Query("UPDATE entry_rows SET values_json = :valuesJson WHERE id = :id")
    suspend fun updateValues(id: String, valuesJson: String)

    @Query("DELETE FROM entry_rows WHERE id = :id")
    suspend fun deleteById(id: String)
}
