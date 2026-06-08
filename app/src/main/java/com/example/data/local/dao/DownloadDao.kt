package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE profileId = :profileId ORDER BY downloadedAt DESC")
    fun getDownloadsForProfile(profileId: Long): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE downloadManagerId = :downloadManagerId LIMIT 1")
    suspend fun getDownloadByManagerId(downloadManagerId: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity): Long

    @androidx.room.Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Long)
}
