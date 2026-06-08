package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.HistoryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries WHERE profileId = :profileId ORDER BY visitedAt DESC")
    fun getHistoryForProfile(profileId: Long): Flow<List<HistoryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity)

    @Query("DELETE FROM history_entries WHERE profileId = :profileId")
    suspend fun clearHistoryForProfile(profileId: Long)
}
