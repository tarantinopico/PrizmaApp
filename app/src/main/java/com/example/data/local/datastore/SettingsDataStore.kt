package com.example.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prizma_settings")

class SettingsDataStore(private val context: Context) {
    private val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")

    val activeProfileId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_PROFILE_ID]
    }

    suspend fun setActiveProfileId(id: Long?) {
        context.dataStore.edit { preferences ->
            if (id == null) {
                preferences.remove(ACTIVE_PROFILE_ID)
            } else {
                preferences[ACTIVE_PROFILE_ID] = id
            }
        }
    }
}
