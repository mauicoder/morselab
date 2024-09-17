package net.maui.morselab.viewmodel

import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import net.maui.morselab.data.UserPreferencesRepositoryImpl
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel
    @Inject constructor(private val userPreferencesRepository: UserPreferencesRepositoryImpl
) : ViewModel() {

    private val userPreferencesFlow = userPreferencesRepository.getPreferencesFlow()

    val frequencyFlow: LiveData<Int> =
        userPreferencesFlow.map { it -> it.frequency }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()
    val wpmFlow: LiveData<Int> =
        userPreferencesFlow.map { it -> it.wpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()
    val farnsworthWpmFlow: LiveData<Int> =
        userPreferencesFlow.map { it -> it.farnsworthWpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()

    fun increaseFrequency() {
        var newFreq = frequencyFlow.value!! + 50
        if (newFreq > 1000)
            newFreq = 1000
        updateFrequency(newFreq)
        Log.i("Model", "New frequency value $newFreq")
    }

    fun decreaseFrequency() {
        var newFreq = frequencyFlow.value!! - 50
        if (newFreq < 100)
           newFreq = 1000
        updateFrequency(newFreq)
        Log.i("Model", "New frequency value $newFreq")
    }

    fun increaseWpm() {
        val newValue = wpmFlow.value!! + 1
        updateWpm(newValue)
        Log.i("Model", "New Wpm value $newValue")
    }

    fun decreaseWpm() {
        var newValue = wpmFlow.value!! - 1
        if( newValue < 5) {
           newValue = 5
        }
        updateWpm(newValue)
        Log.i("Model", "New Wpm value $newValue")

        if (farnsworthWpmFlow.value!! > newValue) {
            updateFarnsworthWpm(newValue)
            Log.i("Model", "New farnsworthWpm value $newValue")

        }

    }
    fun increaseFarnsworthWpm() {
        val newValue = farnsworthWpmFlow.value!! + 1
        updateFarnsworthWpm(newValue)
        if (newValue > wpmFlow.value!!) {
            updateWpm(newValue)
            Log.i("Model", "New Wpm value $newValue")
        }
        Log.i("Model", "New farnsworthWpm value $newValue")

    }

    fun decreaseFarnsworthWpm() {
        var newValue = farnsworthWpmFlow.value!! - 1
        if (newValue < 5 )
            newValue = 5
        updateFarnsworthWpm(newValue)
        Log.i("Model", "New farnsworthWpm value $newValue")
    }


    private fun updateFrequency(frequency: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateFrequency(frequency)

        }
    }
    private fun updateWpm(wpm: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateWpm(wpm)
        }
    }
    private fun updateFarnsworthWpm(farnsworthWpm: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateFarnsworthWpm(farnsworthWpm)
        }
    }

}