package net.maui.morselab.decoder

import net.maui.morselab.utils.MorseCodeMaps
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Decodes a stream of audio data into Morse code text in real-time.
 * It fires individual characters and spaces as they are recognized.
 * This implementation is fully decoupled from wall-clock time and phone performance,
 * basing all timing on the number of samples processed.
 */
class MorseDecoder(
    private val onDecoded: (String) -> Unit,
    private val targetFreq: Double = 800.0,
    private val sampleRate: Int = 16000,
    private val blockSize: Int = 512,
) {
    private val logger = Logger.getLogger(MorseDecoder::class.java.name)

    private val goertzel = Goertzel(sampleRate, targetFreq, blockSize)

    // --- State variables ---
    private val magHistory = ArrayDeque<Double>()
    private val histSize = 50
    private var onState = false

    // --- Sample-based timing ---
    private var totalSamplesProcessed: Long = 0
    private var lastStateChangeSamplePos: Long = 0
    private var dotUnitSamples: Double = sampleRate * 0.080 // Default 80ms dot at current sample rate

    // --- Internal buffer for the current character ---
    private val currentCharMorse = StringBuilder()

    /**
     * Processes a new buffer of audio data.
     * @param audioBuffer The audio data chunk.
     * @param numSamples The number of actual samples in this buffer.
     */
    fun processBuffer(audioBuffer: FloatArray, numSamples: Int) {
        val mag = goertzel.magnitudeSquared(audioBuffer)

        magHistory.addLast(mag)
        if (magHistory.size > histSize) magHistory.removeFirst()
        val median = magHistory.sorted().getOrElse(magHistory.size / 2) { 0.0 }

        val thresholdOn = median * 6.0 + 1e-9
        val thresholdOff = median * 3.0 + 1e-9

        val newOn = if (onState) mag > thresholdOff else mag > thresholdOn

        if (newOn != onState) {
            val durInSamples = totalSamplesProcessed - lastStateChangeSamplePos

            // Only process events with a meaningful duration
            if (durInSamples > sampleRate * 0.010) { // Ignore fleeting changes (< 10ms worth of samples)
                // --- START OF FIX ---
                if (onState) {
                    // A TONE just ended. Its duration is durInSamples. Add it to the buffer.
                    addToneToBuffer(durInSamples)
                    updateUnitDuration(durInSamples)
                    logger.info("add Tone: '${currentCharMorse}' Updated unit duration to ${dotUnitSamples}")
                } else {
                    // A SILENCE just ended. Its duration is durInSamples. Process it.
                    processSilence(durInSamples)
                }
                // --- END OF FIX ---
            }
            // Update the state for the next cycle
            lastStateChangeSamplePos = totalSamplesProcessed
            onState = newOn
        }

        // Advance the total sample count AFTER processing the state change
        totalSamplesProcessed += numSamples
    }

    // --- START OF FIX: NEW FLUSH METHOD ---
    /**
     * Processes any remaining characters in the buffer.
     * Call this when the audio stream ends.
     */
    fun flush() {
        // A long fake silence duration to process the final character
        val finalSilence = (dotUnitSamples * 4).toLong()
        processSilence(finalSilence)
    }
    // --- END OF FIX ---


    /** Appends a dot or dash to the internal buffer based on the tone's duration. */
    private fun addToneToBuffer(durationInSamples: Long) {
        if (dotUnitSamples <= 0) return
        val units = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)
        currentCharMorse.append(if (units < 2) '.' else '-')
    }

    /**
     * Analyzes a silence. If it's a character or word gap, it fires the decoded
     * character/space and clears the internal buffer.
     */
    private fun processSilence(durationInSamples: Long) {
        // A silence of any duration could mean the end of a character, but we only
        // act on it if there's something in the buffer.
        if (currentCharMorse.isEmpty()) {
            // If the buffer is empty, this silence might be a word gap.
            if (dotUnitSamples > 0) {
                val silenceUnits = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)
                if (silenceUnits >= 7) {
                    logger.info("End word (no char): firing space")
                    onDecoded(" ")
                }
            }
            return
        }

        // There's a character in the buffer. Let's see if this silence is long enough to flush it.
        if (dotUnitSamples <= 0) return
        val silenceUnits = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)

        // A gap of 3+ units means the character we were building is now complete.
        if (silenceUnits >= 3) {
            logger.info("End char: '${currentCharMorse}'")
            val char = morseToAscii(currentCharMorse.toString())
            if (char.isNotEmpty()) {
                onDecoded(char)
            }
            currentCharMorse.clear()


            // If the gap was also a word separator, fire a space.
            if (silenceUnits >= 7) {
                logger.info("End word: firing space")
                onDecoded(" ")
            }
        }
        // If the silence was short (intra-character gap), we do nothing.
        // This correctly allows the next tone to be appended to the current character buffer.
    }


    /** Resets the decoder's internal state. */
    fun reset() {
        magHistory.clear()
        onState = false
        totalSamplesProcessed = 0
        lastStateChangeSamplePos = 0
        dotUnitSamples = sampleRate * 0.080 // Reset to default
        currentCharMorse.clear()
    }

    /** Dynamically updates the dot unit duration based on the median of recent tone durations. */
    private fun updateUnitDuration(newToneDuration: Long) {
        // This is a simplified approach. For more stability, you could
        // maintain a list of recent tone durations and take the median.
        // For now, a simple weighted average can work well.
        val newUnit = newToneDuration.toDouble().coerceAtMost(dotUnitSamples * 2) // Cap to avoid dashes skewing the unit
        dotUnitSamples = (dotUnitSamples * 0.7) + (newUnit * 0.3)
    }

    private fun morseToAscii(morse: String): String {
        return MorseCodeMaps.morseToAscii[morse] ?: "" // Return "?" for unknown symbols if desired
    }
}
