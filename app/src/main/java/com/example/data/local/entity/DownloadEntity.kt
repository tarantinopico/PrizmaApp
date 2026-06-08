package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val url: String,
    val fileName: String,
    val mimeType: String = "",
    val status: String,
    val downloadManagerId: Long = -1L,
    val filePath: String? = null,
    val fileSize: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
)
