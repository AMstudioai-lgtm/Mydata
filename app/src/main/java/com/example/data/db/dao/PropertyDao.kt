package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.db.entity.PropertyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PropertyDao {
    @Query("SELECT * FROM properties WHERE journal_id = :journalId ORDER BY sort_order ASC")
    fun watchByJournal(journalId: String): Flow<List<PropertyEntity>>

    @Query("SELECT COUNT(*) FROM properties WHERE journal_id = :journalId")
    suspend fun countByJournal(journalId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(property: PropertyEntity)

    @Query("DELETE FROM properties WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE properties SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: String, order: Int)

    @Query("UPDATE properties SET name = :name, kind = :kind, options_json = :optionsJson WHERE id = :id")
    suspend fun updateProperty(id: String, name: String, kind: String, optionsJson: String)
}
