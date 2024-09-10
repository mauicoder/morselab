package net.maui.morselab

import androidx.lifecycle.ViewModel

class ConfigViewModel : ViewModel() {
    // TODO: Implement the ViewModel

    /*
        editTextText = findViewById(R.id.editTextText)
        textViewFrequency = findViewById(R.id.textViewFrequency)
        textViewWPM = findViewById(R.id.textViewWPM)
        textViewFarnsworthWPM = findViewById(R.id.textViewFarnsworthWPM)

        val buttonIncreaseFrequency: Button = findViewById(R.id.buttonIncreaseFrequency)
        val buttonDecreaseFrequency: Button = findViewById(R.id.buttonDecreaseFrequency)
        val buttonIncreaseWPM: Button = findViewById(R.id.buttonIncreaseWPM)
        val buttonDecreaseWPM: Button = findViewById(R.id.buttonDecreaseWPM)
        val buttonIncreaseFarnsworthWPM: Button = findViewById(R.id.buttonIncreaseFarnsworthWPM)
        val buttonDecreaseFarnsworthWPM: Button = findViewById(R.id.buttonDecreaseFarnsworthWPM)

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
}