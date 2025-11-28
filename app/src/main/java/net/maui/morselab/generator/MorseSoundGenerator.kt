package net.maui.morselab.generator

import net.maui.morselab.utils.MorseCodeMaps
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class MorseSoundGenerator {

    fun generate(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ): ByteArray {
        val dotDuration = morseTiming(wpm)
        val dashDuration = 3 * dotDuration
        val intraCharacterPause = createSilence(dotDuration * sampleRate / 1000)

        val farnsworthDotDuration = morseTiming(farnsworthWpm)
        val interCharacterPause = createSilence(3 * farnsworthDotDuration * sampleRate / 1000)
        val interWordPause = createSilence(7 * farnsworthDotDuration * sampleRate / 1000)

        val morseSoundList = mutableListOf<ByteArray>()

        // Add a small initial pause for clean playback from the start.
        morseSoundList.add(intraCharacterPause)

        text.uppercase().forEach { char ->
            when (char) {
                ' ' -> morseSoundList.add(interWordPause) // Space between words
                else -> {
                    val morseCode = MorseCodeMaps.asciiToMorse[char] ?: return@forEach
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

    private fun createSilence(numSamples: Int): ByteArray {
        val buffer = ByteArray(numSamples)
        // For unsigned 8-bit PCM, silence is at the midpoint (128).
        buffer.fill(128.toByte())
        return buffer
    }

    private fun generateSineWave(
        frequency: Int,
        durationMs: Int,
        sampleRate: Int,
        fadePercentage: Double = 0.1 // 10% fade-in and fade-out
    ): ByteArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val fadeSamples = (numSamples * fadePercentage).toInt()
        val sample = ByteArray(numSamples)

        for (i in sample.indices) {
            val angle = 2.0 * Math.PI * i / (sampleRate / frequency)
            var amplitude = sin(angle) * 127

            // Apply cosine-based fade-in and fade-out
            val envelope = when {
                i < fadeSamples -> (1 - cos(i * Math.PI / fadeSamples)) / 2
                i >= numSamples - fadeSamples -> (1 - cos(((numSamples - 1) - i) * Math.PI / fadeSamples)) / 2
                else -> 1.0
            }
            amplitude *= envelope

            // Shift the signed amplitude to unsigned 8-bit PCM range
            sample[i] = (amplitude + 128).toInt().toByte()
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
