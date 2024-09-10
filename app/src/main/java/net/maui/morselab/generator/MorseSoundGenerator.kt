package net.maui.morselab.generator

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class MorseSoundGenerator {

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

    fun generate(
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

    fun generateWave(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ): ByteArray {
        val morseCodeSound = generate(text, wpm, farnsworthWpm, frequency, sampleRate)

        val waveStream = createWavHeader(morseCodeSound.size, sampleRate)
        return waveStream + morseCodeSound
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

    private fun morseTiming(wpm: Int): Int {
        return 1200 / wpm
    }
}