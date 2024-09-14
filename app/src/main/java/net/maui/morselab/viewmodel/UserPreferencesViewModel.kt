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

    fun increaseFrequency(view: View) {
        val newFreq = frequencyFlow.value!! + 50
        updateFrequency(newFreq)
        Log.i("Model", "New frequency value $newFreq")
    }

    fun decreaseFrequency(view: View) {
        val newFreq = frequencyFlow.value!! - 50
        updateFrequency(newFreq)
    }
    fun increaseWpm(view: View) {
        val newValue = wpmFlow.value!! + 1
        updateWpm(newValue)
        Log.i("Model", "New Wpm value $newValue")
    }

    fun decreaseWpm(view: View) {
        val newValue = wpmFlow.value!! - 1
        updateWpm(newValue)
    }
    fun increaseFarnsworthWpm(view: View) {
        val newValue = farnsworthWpmFlow.value!! + 1
        updateFarnsworthWpm(newValue)
        Log.i("Model", "New farnsworthWpm value $newValue")

    }

    fun decreaseFarnsworthWpm(view: View) {
        val newValue = wpmFlow.value!! - 1
        updateFarnsworthWpm(newValue)
        Log.i("Model", "New farnsworthWpm value $newValue")
    }

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