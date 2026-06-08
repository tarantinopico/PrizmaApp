package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs WHERE profileId = :profileId ORDER BY lastAccessed DESC")
    fun getTabsForProfile(profileId: Long): Flow<List<TabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity): Long

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTab(id: Long)

    @Query("DELETE FROM tabs WHERE profileId = :profileId")
    suspend fun deleteAllTabsForProfile(profileId: Long)
}
