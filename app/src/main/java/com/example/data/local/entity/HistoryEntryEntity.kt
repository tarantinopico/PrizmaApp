package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val url: String,
    val title: String,
    val visitedAt: Long = System.currentTimeMillis()
)
