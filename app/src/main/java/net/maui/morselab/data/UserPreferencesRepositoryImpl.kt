package net.maui.morselab.data

import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.maui.morselab.datastore.UserPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    private val userPreferencesStore: DataStore<UserPreferences>
) : UserPreferencesRepository {

    private val TAG: String = "UserPreferencesRepository"

    fun getPreferencesFlow(): Flow<UserPreferences> {
        return getData()
    }

    private fun getData() = userPreferencesStore.data.catch { exception ->
        // dataStore.data throws an IOException when an error is encountered when reading data
        if (exception is IOException) {
            Log.e(TAG, "Error reading sort order preferences.", exception)
            emit(defaultPreferences())
        } else {
            Log.e(TAG, "Other exception:", exception)
            emit(defaultPreferences())
        }
    }.map {
        if (it.frequency != 0 && it.wpm != 0 && it.farnsworthWpm != 0)
            it
        else {
            val builder = UserPreferences.newBuilder()
            if (it.frequency == 0) builder.setFrequency(700) else builder.setFrequency(it.frequency)
            if (it.wpm == 0) builder.setWpm(20) else builder.setWpm(it.wpm)
            if (it.farnsworthWpm == 0) builder.setFarnsworthWpm(20) else builder.setFarnsworthWpm(it.farnsworthWpm)
            builder.build()
        }
    }

    private fun defaultPreferences(): UserPreferences =
        UserPreferences.newBuilder().setFrequency(800).setWpm(20).setFarnsworthWpm(20).build()

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