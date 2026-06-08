package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.datastore.SettingsDataStore
import com.example.data.local.entity.ProfileEntity
import com.example.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: BrowserRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    var hasHandledInitialNavigation = false

    val profiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeProfileId: StateFlow<Long?> = settingsDataStore.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun createProfile(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.addProfile(name, colorHex)
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun deleteProfile(id: Long) {
        viewModelScope.launch {
            repository.deleteProfile(id)
        }
    }

    fun setActiveProfile(id: Long?) {
        viewModelScope.launch {
            settingsDataStore.setActiveProfileId(id)
        }
    }
    
    companion object {
        fun provideFactory(repository: BrowserRepository, settingsDataStore: SettingsDataStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(repository, settingsDataStore) as T
            }
        }
    }
}
