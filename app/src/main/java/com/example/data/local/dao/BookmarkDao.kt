package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId ORDER BY createdAt DESC")
    fun getBookmarksForProfile(profileId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE profileId = :profileId AND url = :url LIMIT 1")
    suspend fun getBookmarkByUrl(profileId: Long, url: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: Long)

    @Query("DELETE FROM bookmarks WHERE profileId = :profileId AND url = :url")
    suspend fun deleteBookmarkByUrl(profileId: Long, url: String)
}
