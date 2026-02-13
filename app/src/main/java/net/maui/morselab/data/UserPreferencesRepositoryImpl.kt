package net.maui.morselab.data

import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    private val TAG: String = "UserPreferencesRepo"
    
    // Repository-level scope for the StateFlow
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val userPreferences: StateFlow<UserPreferences> = dataStore.data
        .onEach { Log.i(TAG, "RECOGNIZED: New preferences from DataStore: $it") }
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading user preferences.", exception)
                emit(UserPreferences())
            } else {
                Log.e(TAG, "Other exception:", exception)
                throw exception
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferences()
        )

    override suspend fun updateFrequency(frequency: Int) {
        Log.i(TAG, "REQUEST: Update frequency to $frequency")
        dataStore.updateData { preferences ->
            preferences.copy(frequency = frequency)
        }
    }

    override suspend fun updateWpm(wpm: Int) {
        Log.i(TAG, "REQUEST: Update WPM to $wpm")
        dataStore.updateData { preferences ->
            preferences.copy(wpm = wpm)
        }
    }

    override suspend fun updateFarnsworthWpm(farnsworthWpm: Int) {
        Log.i(TAG, "REQUEST: Update Farnsworth WPM to $farnsworthWpm")
        dataStore.updateData { preferences ->
            preferences.copy(farnsworthWpm = farnsworthWpm)
        }
    }
}