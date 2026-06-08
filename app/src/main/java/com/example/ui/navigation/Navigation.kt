package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ProfileSelection

@Serializable
data class Browser(val profileId: Long)

@Serializable
data class TabSwitcher(val profileId: Long)

@Serializable
data class Downloads(val profileId: Long)

@Serializable
data class Settings(val profileId: Long)
