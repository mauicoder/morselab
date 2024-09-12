package net.maui.morselab.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import net.maui.morselab.data.UserPreferencesRepository

class UserPreferencesViewModel(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val userPreferencesFlow = userPreferencesRepository.userPreferencesFlow

    private val frequencyFlow: Flow<Int> =
        userPreferencesRepository.userPreferencesFlow.map { it -> it.frequency }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    private val wpmFlow: Flow<Int> =
        userPreferencesRepository.userPreferencesFlow.map { it -> it.wpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )
    private val farnsworthWpmFlow: Flow<Int> =
        userPreferencesRepository.userPreferencesFlow.map { it -> it.farnsworthWpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            )

    /*

    buttonIncreaseFrequency.setOnClickListener {
        frequency += 50
        textViewFrequency.text = "Frequency (Hz): $frequency"
    }

    buttonDecreaseFrequency.setOnClickListener {
        frequency -= 50
        if (frequency < 100) frequency = 100
        textViewFrequency.text = "Frequency (Hz): $frequency"
    }

    buttonIncreaseWPM.setOnClickListener {
        wpm += 1
        updateWPMText()
    }

    buttonDecreaseWPM.setOnClickListener {
        wpm -= 1
        if (wpm < 5) wpm = 5
        updateWPMText()
        if (farnsworthWpm > wpm) {
            farnsworthWpm = wpm
            updateFarnsworthText()
        }
    }

    buttonIncreaseFarnsworthWPM.setOnClickListener {
        farnsworthWpm += 1
        updateFarnsworthText()

        if (farnsworthWpm > wpm) {
             wpm = farnsworthWpm
            updateWPMText()
        }
    }

    buttonDecreaseFarnsworthWPM.setOnClickListener {
        farnsworthWpm -= 1
        if (farnsworthWpm < 5) farnsworthWpm = 5
        updateFarnsworthText()

    }
    */

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