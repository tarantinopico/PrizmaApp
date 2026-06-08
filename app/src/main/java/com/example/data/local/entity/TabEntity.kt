package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val groupId: Long? = null,
    val url: String,
    val title: String,
    val lastAccessed: Long = System.currentTimeMillis()
)
