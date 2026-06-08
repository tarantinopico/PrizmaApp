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
    private val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    private val HAPTICS_INTENSITY = stringPreferencesKey("haptics_intensity")
    private val AUTO_HIDE_PANEL = booleanPreferencesKey("auto_hide_panel")
    private val PANEL_STYLE = stringPreferencesKey("panel_style")

    val activeProfileId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[ACTIVE_PROFILE_ID]
    }
    
    val hapticsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[HAPTICS_ENABLED] ?: true
    }

    val hapticsIntensity: Flow<String> = context.dataStore.data.map {
        it[HAPTICS_INTENSITY] ?: "střední"
    }

    val autoHidePanel: Flow<Boolean> = context.dataStore.data.map {
        it[AUTO_HIDE_PANEL] ?: false
    }

    val panelStyle: Flow<String> = context.dataStore.data.map {
        it[PANEL_STYLE] ?: "floating"
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
    
    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HAPTICS_ENABLED] = enabled }
    }

    suspend fun setHapticsIntensity(intensity: String) {
        context.dataStore.edit { it[HAPTICS_INTENSITY] = intensity }
    }

    suspend fun setAutoHidePanel(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_HIDE_PANEL] = enabled }
    }

    suspend fun setPanelStyle(style: String) {
        context.dataStore.edit { it[PANEL_STYLE] = style }
    }
}
