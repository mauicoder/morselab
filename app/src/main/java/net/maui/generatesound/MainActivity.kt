package net.maui.generatesound

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.sin


class MainActivity : AppCompatActivity() {

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

    fun generateSineWave(frequency: Int, durationMs: Int, sampleRate: Int, fadeDurationMs: Int = 20): ByteArray {
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

    fun concatenateSounds(vararg sounds: ByteArray): ByteArray {
        val totalLength = sounds.sumOf { it.size }
        val result = ByteArray(totalLength)
        var offset = 0
        for (sound in sounds) {
            System.arraycopy(sound, 0, result, offset, sound.size)
            offset += sound.size
        }
        return result
    }

    fun playSound(soundData: ByteArray, sampleRate: Int) {
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

    fun morseTiming(wpm: Int): Int {
        return 1200 / wpm
    }

    fun encodeMorse(text: String, wpm: Int, farnsworthWpm: Int, frequency: Int, sampleRate: Int): ByteArray {
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

    fun playA() {
        val sampleRate = 44100
        val wpm = 20 // Words per minute (Character speed)
        val farnsworthWpm = 10 // Farnsworth Words per minute (Overall speed)
        val frequency = 800 // Frequency of the tone in Hz

        // Example text to encode and play in Morse code
        val text = "HELLO WORLD"

        // Encode the text into Morse code sound using Farnsworth timing
        val morseCodeSound = encodeMorse(text, wpm, farnsworthWpm, frequency, sampleRate)

        // Play the concatenated sound
        playSound(morseCodeSound, sampleRate)
    }


    fun playClickCallBack(view: View) {
        // Use a new tread as this can take a while
        playA()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}