package net.maui.morselab.generator

import net.maui.morselab.utils.MorseCodeMaps
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class MorseSoundGenerator {

    /**
     * Generates a 16-bit PCM Mono audio stream for the given text.
     */
    fun generate(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ): ShortArray {
        val dotDuration = morseTiming(wpm)
        val dashDuration = 3 * dotDuration
        val intraCharacterPause = createSilence(dotDuration * sampleRate / 1000)

        val farnsworthDotDuration = morseTiming(farnsworthWpm)
        val interCharacterPause = createSilence(3 * farnsworthDotDuration * sampleRate / 1000)
        val interWordPause = createSilence(7 * farnsworthDotDuration * sampleRate / 1000)

        val morseSoundList = mutableListOf<ShortArray>()

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

    private fun createSilence(numSamples: Int): ShortArray {
        return ShortArray(numSamples) // Shorts initialized to 0 (silence in 16-bit PCM)
    }

    private fun generateSineWave(
        frequency: Int,
        durationMs: Int,
        sampleRate: Int,
        fadePercentage: Double = 0.1 // 10% fade-in and fade-out
    ): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val fadeSamples = (numSamples * fadePercentage).toInt()
        val sample = ShortArray(numSamples)

        for (i in sample.indices) {
            val angle = 2.0 * Math.PI * i * frequency / sampleRate
            var amplitude = sin(angle) * 32767.0

            // Apply cosine-based fade-in and fade-out for smoothing (Hanning envelope)
            val envelope = when {
                i < fadeSamples -> (1.0 - cos(i * Math.PI / fadeSamples)) / 2.0
                i >= numSamples - fadeSamples -> (1.0 - cos(((numSamples - 1) - i) * Math.PI / fadeSamples)) / 2.0
                else -> 1.0
            }
            amplitude *= envelope

            sample[i] = amplitude.toInt().toShort()
        }
        return sample
    }

    /**
     * Generates a complete .WAV file (header + 16-bit PCM data)
     */
    fun generateWave(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ): ByteArray {
        val morseCodeShorts = generate(text, wpm, farnsworthWpm, frequency, sampleRate)
        val dataSizeInBytes = morseCodeShorts.size * 2

        val byteBuffer = ByteBuffer.allocate(44 + dataSizeInBytes).order(ByteOrder.LITTLE_ENDIAN)
        
        // Add WAV Header
        createWavHeader(byteBuffer, dataSizeInBytes, sampleRate)
        
        // Add PCM Data
        for (s in morseCodeShorts) {
            byteBuffer.putShort(s)
        }

        return byteBuffer.array()
    }

    private fun createWavHeader(buffer: ByteBuffer, dataSize: Int, sampleRate: Int) {
        val totalDataLen = dataSize + 36
        val byteRate = sampleRate * 1 * 16 / 8 // sampleRate * numChannels * bitsPerSample/8

        buffer.put("RIFF".toByteArray())             // RIFF header
        buffer.putInt(totalDataLen)                  // Total size of the file minus 8 bytes
        buffer.put("WAVE".toByteArray())             // WAVE type
        buffer.put("fmt ".toByteArray())             // Format chunk marker
        buffer.putInt(16)                            // Length of format data
        buffer.putShort(1)                           // PCM format
        buffer.putShort(1)                           // Number of channels (1 for mono)
        buffer.putInt(sampleRate)                    // Sample rate
        buffer.putInt(byteRate)                      // Byte rate
        buffer.putShort(2)                           // Block align (numChannels * bitsPerSample/8)
        buffer.putShort(16)                          // Bits per sample (16 bits)

        buffer.put("data".toByteArray())             // Data chunk header
        buffer.putInt(dataSize)                      // Size of data section
    }

    private fun concatenateSounds(vararg sounds: ShortArray): ShortArray {
        val totalLength = sounds.sumOf { it.size }
        val result = ShortArray(totalLength)
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
