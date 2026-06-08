package com.example.domain.repository

import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class BrowserRepository(
    private val profileDao: ProfileDao,
    private val tabDao: TabDao,
    private val tabGroupDao: TabGroupDao,
    private val bookmarkDao: BookmarkDao,
    private val historyDao: HistoryDao,
    private val downloadDao: DownloadDao
) {
    // Profiles
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    fun getProfileById(id: Long): Flow<ProfileEntity?> = profileDao.getProfileById(id)
    suspend fun addProfile(name: String, accentColorHex: String? = null) = profileDao.insertProfile(ProfileEntity(name = name, accentColorHex = accentColorHex))
    suspend fun updateProfile(profile: ProfileEntity) = profileDao.updateProfile(profile)
    suspend fun deleteProfile(id: Long) = profileDao.deleteProfileById(id)

    // Tab Groups
    fun getTabGroupsForProfile(profileId: Long): Flow<List<TabGroupEntity>> = tabGroupDao.getTabGroupsForProfile(profileId)
    suspend fun addTabGroup(profileId: Long, name: String, colorHex: String): Long = tabGroupDao.insertTabGroup(
        TabGroupEntity(profileId = profileId, name = name, colorHex = colorHex)
    )
    suspend fun updateTabGroup(tabGroup: TabGroupEntity) = tabGroupDao.updateTabGroup(tabGroup)
    suspend fun deleteTabGroup(id: Long) = tabGroupDao.deleteTabGroup(id)
    suspend fun deleteAllTabGroupsForProfile(profileId: Long) = tabGroupDao.deleteAllTabGroupsForProfile(profileId)

    // Tabs
    fun getTabsForProfile(profileId: Long): Flow<List<TabEntity>> = tabDao.getTabsForProfile(profileId)
    suspend fun addTab(profileId: Long, url: String, title: String) = tabDao.insertTab(
        TabEntity(profileId = profileId, url = url, title = title)
    )
    suspend fun updateTab(tab: TabEntity) = tabDao.updateTab(tab)
    suspend fun deleteTab(id: Long) = tabDao.deleteTab(id)
    suspend fun deleteAllTabsForProfile(profileId: Long) = tabDao.deleteAllTabsForProfile(profileId)

    // Bookmarks
    fun getBookmarksForProfile(profileId: Long): Flow<List<BookmarkEntity>> = bookmarkDao.getBookmarksForProfile(profileId)
    suspend fun addBookmark(profileId: Long, url: String, title: String) = bookmarkDao.insertBookmark(
        BookmarkEntity(profileId = profileId, url = url, title = title)
    )
    suspend fun deleteBookmark(id: Long) = bookmarkDao.deleteBookmark(id)

    // History
    fun getHistoryForProfile(profileId: Long): Flow<List<HistoryEntryEntity>> = historyDao.getHistoryForProfile(profileId)
    suspend fun addHistoryEntry(profileId: Long, url: String, title: String) = historyDao.insertHistoryEntry(
        HistoryEntryEntity(profileId = profileId, url = url, title = title)
    )
    suspend fun clearHistoryForProfile(profileId: Long) = historyDao.clearHistoryForProfile(profileId)

    // Profiles

    // Downloads
    fun getDownloadsForProfile(profileId: Long): Flow<List<DownloadEntity>> = downloadDao.getDownloadsForProfile(profileId)
    suspend fun getDownloadByManagerId(downloadManagerId: Long): DownloadEntity? = downloadDao.getDownloadByManagerId(downloadManagerId)
    suspend fun addDownload(download: DownloadEntity) = downloadDao.insertDownload(download)
    suspend fun updateDownload(download: DownloadEntity) = downloadDao.updateDownload(download)
    suspend fun deleteDownload(id: Long) = downloadDao.deleteDownload(id)
}
