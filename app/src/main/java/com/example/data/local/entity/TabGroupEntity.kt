package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tab_groups")
data class TabGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val name: String,
    val colorHex: String = "#7C5CFF",
    val isCollapsed: Boolean = false
)
