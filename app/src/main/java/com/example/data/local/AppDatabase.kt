package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*

@Database(
    entities = [
        ProfileEntity::class,
        TabGroupEntity::class,
        TabEntity::class,
        BookmarkEntity::class,
        HistoryEntryEntity::class,
        DownloadEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun tabGroupDao(): TabGroupDao
    abstract fun tabDao(): TabDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
}
