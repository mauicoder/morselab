package net.maui.morselab.data

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    val frequency: Int = 700,
    val wpm: Int = 20,
    val farnsworthWpm: Int = 20
)