package net.maui.morselab.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.maui.morselab.data.UserPreferences
import net.maui.morselab.data.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class UserPreferencesViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Use the StateFlow directly from the repository
    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences

    val frequencyFlow: LiveData<Int> = userPreferences.map { it.frequency }.asLiveData()
    val wpmFlow: LiveData<Int> = userPreferences.map { it.wpm }.asLiveData()
    val farnsworthWpmFlow: LiveData<Int> = userPreferences.map { it.farnsworthWpm }.asLiveData()

    fun increaseFrequency() {
        val current = userPreferences.value.frequency
        val newValue = (current + 50).coerceAtMost(1000)
        updateFrequency(newValue)
    }

    fun decreaseFrequency() {
        val current = userPreferences.value.frequency
        val newValue = (current - 50).coerceAtLeast(100)
        updateFrequency(newValue)
    }

    fun increaseWpm() {
        val current = userPreferences.value.wpm
        updateWpm(current + 1)
    }

    fun decreaseWpm() {
        val current = userPreferences.value.wpm
        val newValue = (current - 1).coerceAtLeast(5)
        updateWpm(newValue)
        
        if (userPreferences.value.farnsworthWpm > newValue) {
            updateFarnsworthWpm(newValue)
        }
    }

    fun increaseFarnsworthWpm() {
        val newValue = userPreferences.value.farnsworthWpm + 1
        updateFarnsworthWpm(newValue)
        if (newValue > userPreferences.value.wpm) {
            updateWpm(newValue)
        }
    }

    fun decreaseFarnsworthWpm() {
        val newValue = (userPreferences.value.farnsworthWpm - 1).coerceAtLeast(5)
        updateFarnsworthWpm(newValue)
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