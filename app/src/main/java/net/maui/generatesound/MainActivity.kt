package net.maui.generatesound

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var editTextText: EditText
    private lateinit var editTextFrequency: EditText
    private lateinit var editTextWPM: EditText
    private lateinit var editTextFarnsworthWPM: EditText
    private lateinit var buttonPlay: Button

    // Morse code map for letters, numbers, and some symbols
    val morseCodeMap = mapOf(
        'A' to ".-",    'B' to "-...",  'C' to "-.-.",  'D' to "-..",   'E' to ".",
        'F' to "..-.",  'G' to "--.",   'H' to "....",  'I' to "..",    'J' to ".---",
        'K' to "-.-",   'L' to ".-..",  'M' to "--",    'N' to "-.",    'O' to "---",
        'P' to ".--.",  'Q' to "--.-",  'R' to ".-.",   'S' to "...",   'T' to "-",
        'U' to "..-",   'V' to "...-",  'W' to ".--",   'X' to "-..-",  'Y' to "-.--",
        'Z' to "--..",
        '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
        '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.",  '(' to "-.--.",  ')' to "-.--.-", '&' to ".-...",  ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-",  '+' to ".-.-.",  '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editTextText = findViewById(R.id.editTextText)
        editTextFrequency = findViewById(R.id.editTextFrequency)
        editTextWPM = findViewById(R.id.editTextWPM)
        editTextFarnsworthWPM = findViewById(R.id.editTextFarnsworthWPM)
        buttonPlay = findViewById(R.id.buttonPlay)

        buttonPlay.setOnClickListener {
            val text = editTextText.text.toString()
            val frequency = editTextFrequency.text.toString().toIntOrNull() ?: 800
            val wpm = editTextWPM.text.toString().toIntOrNull() ?: 20
            val farnsworthWpm = editTextFarnsworthWPM.text.toString().toIntOrNull() ?: 10

            val morseCodeSound = encodeMorse(text, wpm, farnsworthWpm, frequency, 44100)
            playSound(morseCodeSound, 44100)
        }
    }

    private fun generateSineWave(frequency: Int, durationMs: Int, sampleRate: Int, fadeDurationMs: Int = 20): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val fadeSamples = (sampleRate * (fadeDurationMs / 1000.0)).toInt()
        val sample = ByteArray(numSamples)

        for (i in sample.indices) {
            val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
            var amplitude = sin(angle) * 127

            // Apply fade-in and fade-out
            if (i < fadeSamples) {
                amplitude *= i / fadeSamples.toDouble() // Fade-in
            } else if (i >= numSamples - fadeSamples) {
                amplitude *= (numSamples - i - 1) / fadeSamples.toDouble() // Fade-out
            }

            sample[i] = amplitude.toInt().toByte()
        }
        return sample
    }

    private fun concatenateSounds(vararg sounds: ByteArray): ByteArray {
        val totalLength = sounds.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (sound in sounds) {
            System.arraycopy(sound, 0, result, offset, sound.size)
            offset += sound.size
        }
        return result
    }

    private fun playSound(soundData: ByteArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                android.media.AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .build()
            )
            .setBufferSizeInBytes(soundData.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(soundData, 0, soundData.size)
        audioTrack.play()

        // Ensure the sound plays fully
        Thread.sleep((soundData.size * 1000L / sampleRate))
        audioTrack.release()
    }

    private fun morseTiming(wpm: Int): Int {
        return 1200 / wpm
    }

    private fun encodeMorse(text: String, wpm: Int, farnsworthWpm: Int, frequency: Int, sampleRate: Int): ByteArray {
        val dotDuration = morseTiming(wpm)
        val dashDuration = 3 * dotDuration
        val intraCharacterPause = ByteArray(dotDuration * sampleRate / 1000)

        val farnsworthDotDuration = morseTiming(farnsworthWpm)
        val interCharacterPause = ByteArray(3 * farnsworthDotDuration * sampleRate / 1000)
        val interWordPause = ByteArray(7 * farnsworthDotDuration * sampleRate / 1000)

        val morseSoundList = mutableListOf<ByteArray>()

        text.toUpperCase().forEach { char ->
            when (char) {
                ' ' -> morseSoundList.add(interWordPause) // Space between words
                else -> {
                    val morseCode = morseCodeMap[char] ?: return@forEach
                    morseCode.forEach { symbol ->
                        when (symbol) {
                            '.' -> morseSoundList.add(generateSineWave(frequency, dotDuration, sampleRate))
                            '-' -> morseSoundList.add(generateSineWave(frequency, dashDuration, sampleRate))
                        }
                        morseSoundList.add(intraCharacterPause) // Pause between dots and dashes within a character
                    }
                    morseSoundList.add(interCharacterPause) // Pause between characters
                }
            }
        }

        return concatenateSounds(*morseSoundList.toTypedArray())
    }
}
