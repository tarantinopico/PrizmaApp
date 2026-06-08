package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.AppDatabase
import com.example.data.local.datastore.SettingsDataStore
import com.example.domain.repository.BrowserRepository

interface AppContainer {
    val browserRepository: BrowserRepository
    val settingsDataStore: SettingsDataStore
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "prizma_database")
            .fallbackToDestructiveMigration()
            .build()
    }
    
    override val browserRepository: BrowserRepository by lazy {
        BrowserRepository(
            profileDao = database.profileDao(),
            tabDao = database.tabDao(),
            tabGroupDao = database.tabGroupDao(),
            bookmarkDao = database.bookmarkDao(),
            historyDao = database.historyDao(),
            downloadDao = database.downloadDao()
        )
    }

    override val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }
}
