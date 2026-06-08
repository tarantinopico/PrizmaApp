package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun getProfileById(id: Long): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)
}
