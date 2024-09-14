package net.maui.morselab.data

interface UserPreferencesRepository {
    suspend fun updateFrequency(frequency: Int)

    suspend fun updateWpm(wpm: Int)

    suspend fun updateFarnsworthWpm(farnsworthWpm: Int)
}