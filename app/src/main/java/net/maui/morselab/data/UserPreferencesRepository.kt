package net.maui.morselab.data

import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import net.maui.morselab.datastore.UserPreferences
import java.io.IOException

class UserPreferencesRepository(
    private val userPreferencesStore: DataStore<UserPreferences>
) {

    private val TAG: String = "UserPreferencesRepository"

    val userPreferencesFlow: Flow<UserPreferences> = userPreferencesStore.data


    fun getData() = userPreferencesStore.data.catch { exception ->
        // dataStore.data throws an IOException when an error is encountered when reading data
        if (exception is IOException) {
            Log.e(TAG, "Error reading sort order preferences.", exception)
            emit(UserPreferences.getDefaultInstance())
        } else {
            throw exception
        }
    }

    suspend fun updateFrequency(frequency: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setFrequency(frequency).build()
        }
    }
    suspend fun updateWpm(wpm: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setWpm(wpm).build()
        }
    }
    suspend fun updateFarnsworthWpm(farnsworthWpm: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setFarnsworthWpm(farnsworthWpm).build()
        }
    }
}