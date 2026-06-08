package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TabGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabGroupDao {
    @Query("SELECT * FROM tab_groups WHERE profileId = :profileId")
    fun getTabGroupsForProfile(profileId: Long): Flow<List<TabGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabGroup(tabGroup: TabGroupEntity): Long

    @Update
    suspend fun updateTabGroup(tabGroup: TabGroupEntity)

    @Query("DELETE FROM tab_groups WHERE id = :id")
    suspend fun deleteTabGroup(id: Long)

    @Query("DELETE FROM tab_groups WHERE profileId = :profileId")
    suspend fun deleteAllTabGroupsForProfile(profileId: Long)
}
