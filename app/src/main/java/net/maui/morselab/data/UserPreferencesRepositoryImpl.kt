package net.maui.morselab.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import net.maui.morselab.datastore.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val userPreferencesStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    private val TAG: String = "UserPreferencesRepository"

    fun getPreferencesFlow() : Flow<UserPreferences> {
        return userPreferencesStore.data
    }
/*
    fun getData() = userPreferencesStore.data.catch { exception ->
        // dataStore.data throws an IOException when an error is encountered when reading data
        if (exception is IOException) {
            Log.e(TAG, "Error reading sort order preferences.", exception)
            emit(UserPreferences.getDefaultInstance())
        } else {
            throw exception
        }
    }*/

    override suspend fun updateFrequency(frequency: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setFrequency(frequency).build()
        }
    }
    override suspend fun updateWpm(wpm: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setWpm(wpm).build()
        }
    }
    override suspend fun updateFarnsworthWpm(farnsworthWpm: Int) {
        userPreferencesStore.updateData { preferences ->
            preferences.toBuilder().setFarnsworthWpm(farnsworthWpm).build()
        }
    }
}