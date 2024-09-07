package net.maui.generatesound

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private var SAMPLE_RATE = 44100
    private val FILE_PROVIDER_AUTHORITY = "net.maui.generatesound.provider"
    private val FILE_PROVIDER_NAME = "shared_data.wav"

    private lateinit var editTextText: EditText
    private lateinit var textViewFrequency: TextView
    private lateinit var textViewWPM: TextView
    private lateinit var textViewFarnsworthWPM: TextView

    private var frequency = 800
    private var wpm = 20
    private var farnsworthWpm = 20

    // Morse code map for letters, numbers, and some symbols
    private val morseCodeMap = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..",
        '1' to ".----", '2' to "..---", '3' to "...--", '4' to "....-", '5' to ".....",
        '6' to "-....", '7' to "--...", '8' to "---..", '9' to "----.", '0' to "-----",
        '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '\'' to ".----.", '!' to "-.-.--",
        '/' to "-..-.", '(' to "-.--.", ')' to "-.--.-", '&' to ".-...", ':' to "---...",
        ';' to "-.-.-.", '=' to "-...-", '+' to ".-.-.", '-' to "-....-", '_' to "..--.-",
        '"' to ".-..-.", '$' to "...-..-", '@' to ".--.-."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        val buttonPlay: Button = findViewById(R.id.buttonPlay)

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

        buttonPlay.setOnClickListener {
            val text = editTextText.text.toString()
            val morseCodeSound = encodeMorse(text, wpm, farnsworthWpm, frequency, SAMPLE_RATE)
            playSound(morseCodeSound, SAMPLE_RATE)
        }
    }

    private fun updateWPMText() {
        textViewWPM.text = "WPM: $wpm"
    }

    private fun updateFarnsworthText() {
        textViewFarnsworthWPM.text = "Farnsworth WPM: $farnsworthWpm"
    }

    private fun generateSineWave(
        frequency: Int,
        durationMs: Int,
        sampleRate: Int,
        fadeDurationMs: Int = 20
    ): ByteArray {
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

    private fun encodeMorse(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ): ByteArray {
        val dotDuration = morseTiming(wpm)
        val dashDuration = 3 * dotDuration
        val intraCharacterPause = ByteArray(dotDuration * sampleRate / 1000)

        val farnsworthDotDuration = morseTiming(farnsworthWpm)
        val interCharacterPause = ByteArray(3 * farnsworthDotDuration * sampleRate / 1000)
        val interWordPause = ByteArray(7 * farnsworthDotDuration * sampleRate / 1000)

        val morseSoundList = mutableListOf<ByteArray>()

        text.uppercase().forEach { char ->
            when (char) {
                ' ' -> morseSoundList.add(interWordPause) // Space between words
                else -> {
                    val morseCode = morseCodeMap[char] ?: return@forEach
                    morseCode.forEach { symbol ->
                        when (symbol) {
                            '.' -> morseSoundList.add(
                                generateSineWave(
                                    frequency,
                                    dotDuration,
                                    sampleRate
                                )
                            )

                            '-' -> morseSoundList.add(
                                generateSineWave(
                                    frequency,
                                    dashDuration,
                                    sampleRate
                                )
                            )
                        }
                        morseSoundList.add(intraCharacterPause) // Pause between dots and dashes within a character
                    }
                    morseSoundList.add(interCharacterPause) // Pause between characters
                }
            }
        }

        return concatenateSounds(*morseSoundList.toTypedArray())
    }

    private fun File.clearText() {
        PrintWriter(this).also {
            it.print("")
            it.close()
        }
    }

    private fun File.updateText(content: ByteArray) {
        clearText()
        appendBytes(content)
    }

    private fun shareFile(waveStream: ByteArray) {
        val application = this
        val file = File(application.cacheDir, FILE_PROVIDER_NAME)

        file.updateText(waveStream)

        val uri = FileProvider.getUriForFile(this, FILE_PROVIDER_AUTHORITY, file)

        Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
        }.also { intent ->
            startActivity(Intent.createChooser(intent, "Share Sound File"))
        }
    }

    private fun createWavHeader(dataSize: Int, sampleRate: Int): ByteArray {
        val totalDataLen = dataSize + 36
        val byteRate = sampleRate * 1 * 8 / 8 // sampleRate * numChannels * bitsPerSample/8

        val header = ByteArray(44)
        val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())             // RIFF header
        buffer.putInt(totalDataLen)                  // Total size of the file minus 8 bytes
        buffer.put("WAVE".toByteArray())             // WAVE type
        buffer.put("fmt ".toByteArray())             // Format chunk marker
        buffer.putInt(16)                            // Length of format data
        buffer.putShort(1)                           // PCM format
        buffer.putShort(1)                           // Number of channels (1 for mono)
        buffer.putInt(sampleRate)                    // Sample rate
        buffer.putInt(byteRate)                      // Byte rate
        buffer.putShort(1)                           // Block align (numChannels * bitsPerSample/8)
        buffer.putShort(8)                           // Bits per sample (8 bits)

        buffer.put("data".toByteArray())             // Data chunk header
        buffer.putInt(dataSize)                      // Size of data section

        return header
    }

    fun exportAsWave(view: View) {
        val text = editTextText.text.toString()
        val morseCodeSound = encodeMorse(text, wpm, farnsworthWpm, frequency, SAMPLE_RATE)

        val waveStream = createWavHeader(morseCodeSound.size, SAMPLE_RATE)

        shareFile(waveStream + morseCodeSound)
    }
}
