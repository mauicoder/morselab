package net.maui.morselab.data

import kotlinx.coroutines.flow.StateFlow

interface UserPreferencesRepository {
    val userPreferences: StateFlow<UserPreferences>

    suspend fun updateFrequency(frequency: Int)
    suspend fun updateWpm(wpm: Int)
    suspend fun updateFarnsworthWpm(farnsworthWpm: Int)
}