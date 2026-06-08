package com.example.ui.viewmodels

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.local.datastore.SettingsDataStore
import com.example.domain.repository.BrowserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    private val repository: BrowserRepository,
    private val context: Context,
    val settingsDataStore: SettingsDataStore
) : ViewModel() {
    private val _currentProfileId = MutableStateFlow<Long?>(null)
    
    val currentTab = MutableStateFlow<TabEntity?>(null)

    val currentProfile: StateFlow<ProfileEntity?> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getProfileById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val tabs: StateFlow<List<TabEntity>> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getTabsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabGroups: StateFlow<List<TabGroupEntity>> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getTabGroupsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<BookmarkEntity>> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getBookmarksForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntryEntity>> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getHistoryForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadEntity>> = _currentProfileId.filterNotNull().flatMapLatest { id ->
        repository.getDownloadsForProfile(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tabThumbnails = mutableStateMapOf<Long, android.graphics.Bitmap>()
    val webViewStates = mutableMapOf<Long, android.os.Bundle>()

    fun initProfile(profileId: Long) {
        if (_currentProfileId.value == profileId) return
        _currentProfileId.value = profileId
        
        viewModelScope.launch {
            val existingTabs = repository.getTabsForProfile(profileId).firstOrNull() ?: emptyList()
            if (existingTabs.isEmpty()) {
                val newId = repository.addTab(profileId, NEW_TAB_URL, "Nová karta")
                currentTab.value = TabEntity(id = newId, profileId = profileId, url = NEW_TAB_URL, title = "Nová karta")
            } else {
                currentTab.value = existingTabs.first()
            }
        }
    }

    fun addTab(url: String, title: String) {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            val parsedUrl = parseUrl(url)
            val id = repository.addTab(pid, parsedUrl, title)
            val newTab = TabEntity(id = id, profileId = pid, url = parsedUrl, title = title)
            currentTab.value = newTab
        }
    }
    
    fun addTabToGroup(url: String, title: String, groupId: Long) {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            val parsedUrl = parseUrl(url)
            val id = repository.addTab(pid, parsedUrl, title)
            repository.updateTab(TabEntity(id = id, profileId = pid, url = parsedUrl, title = title, groupId = groupId))
        }
    }

    fun addTabToProfile(url: String, title: String, profileId: Long) {
        viewModelScope.launch {
            val parsedUrl = parseUrl(url)
            repository.addTab(profileId, parsedUrl, title)
        }
    }
    
    fun createTabGroup(name: String, colorHex: String, initialTabId: Long?) {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            val groupId = repository.addTabGroup(pid, name, colorHex)
            if (initialTabId != null) {
                assignTabToGroup(initialTabId, groupId)
            }
        }
    }

    fun assignTabToGroup(tabId: Long, groupId: Long?) {
        viewModelScope.launch {
            val tab = tabs.value.find { it.id == tabId } ?: return@launch
            repository.updateTab(tab.copy(groupId = groupId))
        }
    }

    fun closeGroup(groupId: Long) {
        viewModelScope.launch {
            val tabsInGroup = tabs.value.filter { it.groupId == groupId }
            tabsInGroup.forEach { repository.deleteTab(it.id) }
            repository.deleteTabGroup(groupId)
            
            if (tabsInGroup.any { it.id == currentTab.value?.id }) {
                val remainingTabs = tabs.value.filter { it.groupId != groupId }
                if (remainingTabs.isNotEmpty()) {
                    currentTab.value = remainingTabs.first()
                } else {
                    addTab(NEW_TAB_URL, "Nová karta")
                }
            }
        }
    }

    fun toggleGroupCollapse(groupId: Long) {
        viewModelScope.launch {
            val group = tabGroups.value.find { it.id == groupId } ?: return@launch
            repository.updateTabGroup(group.copy(isCollapsed = !group.isCollapsed))
        }
    }

    fun closeAllTabs() {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            repository.deleteAllTabsForProfile(pid)
            repository.deleteAllTabGroupsForProfile(pid)
            addTab(NEW_TAB_URL, "Nová karta")
        }
    }

    fun selectTab(tab: TabEntity) {
        currentTab.value = tab
    }
    
    fun closeTab(tabId: Long) {
        viewModelScope.launch {
            repository.deleteTab(tabId)
            val remainingTabs = tabs.value.filter { it.id != tabId }
            if (currentTab.value?.id == tabId) {
                if (remainingTabs.isNotEmpty()) {
                    currentTab.value = remainingTabs.first()
                } else {
                    // Create a new tab if all are closed
                    addTab(NEW_TAB_URL, "Nová karta")
                }
            }
        }
    }

    fun updateCurrentTabUrl(url: String, title: String) {
        val tab = currentTab.value ?: return
        if (tab.url == url && tab.title == title) return
        val updatedTab = tab.copy(url = url, title = title, lastAccessed = System.currentTimeMillis())
        currentTab.value = updatedTab
        viewModelScope.launch {
            repository.updateTab(updatedTab)
            repository.addHistoryEntry(updatedTab.profileId, url, title)
        }
    }

    fun parseUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return NEW_TAB_URL
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            android.util.Patterns.WEB_URL.matcher(trimmed).matches() -> "https://$trimmed"
            trimmed.startsWith("prizma://") -> trimmed
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(trimmed)}"
        }
    }

    fun toggleBookmark(url: String, title: String) {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            val existing = repository.getBookmarkByUrl(pid, url)
            if (existing != null) {
                repository.deleteBookmarkByUrl(pid, url)
            } else {
                repository.addBookmark(pid, url, title)
            }
        }
    }

    fun updateBookmarkTitle(id: Long, newTitle: String) {
        viewModelScope.launch {
            val bookmark = bookmarks.value.find { it.id == id } ?: return@launch
            repository.updateBookmark(bookmark.copy(title = newTitle))
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun addToHistory(url: String, title: String) {
        val pid = _currentProfileId.value ?: return
        viewModelScope.launch {
            repository.addHistoryEntry(pid, url, title)
        }
    }

    fun startDownload(url: String, userAgent: String?, contentDisposition: String?, mimeType: String?, contentLength: Long) {
        val pid = _currentProfileId.value ?: return
        
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            setTitle(fileName)
            setDescription("Stahování souboru...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        viewModelScope.launch {
            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
            val downloadEntity = DownloadEntity(
                profileId = pid,
                url = url,
                fileName = fileName,
                mimeType = mimeType ?: "",
                status = "probíhá",
                downloadManagerId = downloadId,
                fileSize = contentLength
            )
            repository.addDownload(downloadEntity)
        }
    }

    fun updateDownload(download: DownloadEntity) {
        viewModelScope.launch {
            repository.updateDownload(download)
        }
    }

    fun deleteDownload(downloadId: Long) {
        viewModelScope.launch {
            val d = downloads.value.find { it.id == downloadId }
            d?.let {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                if (it.downloadManagerId != -1L) {
                    downloadManager.remove(it.downloadManagerId)
                }
                repository.deleteDownload(downloadId)
            }
        }
    }

    companion object {
        const val NEW_TAB_URL = "prizma://newtab"
        fun provideFactory(repository: BrowserRepository, context: Context, settingsDataStore: SettingsDataStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BrowserViewModel(repository, context, settingsDataStore) as T
            }
        }
    }
}
