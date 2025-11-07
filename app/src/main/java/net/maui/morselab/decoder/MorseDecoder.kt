package net.maui.morselab.decoder

import net.maui.morselab.utils.MorseCodeMaps
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.roundToInt

class MorseDecoder(
    private val onDecoded: (String) -> Unit,
    private val sampleRate: Int = 16000,
    private val blockSize: Int = 512,
) {
    private val logger = Logger.getLogger(MorseDecoder::class.java.name)
    private var goertzel: Goertzel? = null
    private var filtersInitialized = false

    // Pool of filters to select from
    private val goertzel800: Goertzel
    private val goertzel700: Goertzel
    private val goertzel600: Goertzel

    private val magHistory = ArrayDeque<Double>()
    private val histSize = 50
    private var onState = false

    private var totalSamplesProcessed: Long = 0
    private var lastStateChangeSamplePos: Long = 0
    private var dotUnitSamples: Double = sampleRate * 0.080 // Default 80ms dot

    private val currentCharMorse = StringBuilder()
    private val recentDotDurations =
        ArrayDeque<Long>() // CRITICAL: Only dot durations are stored here
    private val TONE_HISTORY_SIZE = 10
    private var hasDecodedFirstChar = false

    init {
        logger.info("Initializing Goertzel filters...")
        logger.info("sampleRate: $sampleRate, blockSize: $blockSize")
        goertzel800 = Goertzel(sampleRate.toDouble(), 800.0, blockSize)
        goertzel700 = Goertzel(sampleRate.toDouble(), 700.0, blockSize)
        goertzel600 = Goertzel(sampleRate.toDouble(), 600.0, blockSize)
        filtersInitialized = true
    }

    fun processBuffer(audioBuffer: FloatArray, numSamples: Int) {
        val mag: Double
        // Calculate the threshold based on the median of the noise floor history.
        val median = magHistory.sorted().getOrElse(magHistory.size / 2) { 0.0 }
        val thresholdOn = median * 6.0 + 1e-9

        if (goertzel == null) {
            val mag800 = goertzel800.magnitudeSquared(audioBuffer)
            val mag700 = goertzel700.magnitudeSquared(audioBuffer)
            val mag600 = goertzel600.magnitudeSquared(audioBuffer)

            mag = maxOf(mag800, mag700, mag600)

            // Attempt to lock only when a confident tone is detected using the adaptive threshold
            if (mag > thresholdOn) {
                if (mag == mag800) {
                    goertzel = goertzel800
                    logger.info(">>> Goertzel filter locked to 800 Hz <<<")
                } else if (mag == mag700) {
                    goertzel = goertzel700
                    logger.info(">>> Goertzel filter locked to 700 Hz <<<")
                } else {
                    goertzel = goertzel600
                    logger.info(">>> Goertzel filter locked to 600 Hz <<<")
                }
            }
        } else {
            mag = goertzel!!.magnitudeSquared(audioBuffer)
        }

        // ONLY update the noise floor history if the current signal is considered silence.
        if (mag < thresholdOn) {
            magHistory.addLast(mag)
            if (magHistory.size > histSize) magHistory.removeFirst()
        }

        val thresholdOff = median * 3.0 + 1e-9
        val newOn = if (onState) mag > thresholdOff else mag > thresholdOn

        if (newOn != onState) {
            val durationOfPreviousState = totalSamplesProcessed - lastStateChangeSamplePos
            if (durationOfPreviousState > sampleRate * 0.015) { // Denoise
                if (onState) { // Previous state was TONE
                    processTone(durationOfPreviousState)
                } else { // Previous state was SILENCE
                    processSilence(durationOfPreviousState)
                }
            }
            lastStateChangeSamplePos = totalSamplesProcessed
        }
        onState = newOn

        totalSamplesProcessed += numSamples
    }

    fun flush() {
        val finalDuration = totalSamplesProcessed - lastStateChangeSamplePos
        if (finalDuration > sampleRate * 0.015) {
            if (onState) processTone(finalDuration)
            else processSilence(finalDuration)
        }
        processSilence((dotUnitSamples * 7).toLong())
    }

    private fun processTone(durationInSamples: Long) {
        if (dotUnitSamples <= 0) return
        val units = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)
        val symbol = if (units < 2) '.' else '-'
        currentCharMorse.append(symbol)

        if (symbol == '.') {
            updateUnitDuration(durationInSamples)
        }
    }

    private fun processSilence(durationInSamples: Long) {
        if (dotUnitSamples <= 0) return
        val silenceUnits = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)

        if (silenceUnits >= 3) {
            if (currentCharMorse.isNotEmpty()) {
                val char = morseToAscii(currentCharMorse.toString())
                if (char.isNotEmpty()) {
                    onDecoded(char)
                    hasDecodedFirstChar = true
                }
                currentCharMorse.clear()
            }
            if (silenceUnits >= 7 && hasDecodedFirstChar) {
                onDecoded(" ")
            }
        }
    }

    fun reset() {
        magHistory.clear()
        onState = false
        totalSamplesProcessed = 0
        lastStateChangeSamplePos = 0
        dotUnitSamples = sampleRate * 0.080
        currentCharMorse.clear()
        recentDotDurations.clear()
        goertzel = null
        hasDecodedFirstChar = false
    }

    private fun updateUnitDuration(dotDuration: Long) {
        recentDotDurations.addLast(dotDuration)
        if (recentDotDurations.size > TONE_HISTORY_SIZE) {
            recentDotDurations.removeFirst()
        }
        if (recentDotDurations.isEmpty()) return

        // --- THE FINAL AND UNBREAKABLE FIX ---
        // The most reliable "dot" is the SHORTEST valid tone we've seen recently.
        // This is extremely resistant to being skewed by longer dots or dashes.
        val shortestDot = recentDotDurations.minOrNull()?.toDouble() ?: dotUnitSamples

        val oldUnit = dotUnitSamples
        if (shortestDot > 0) {
            // We still smooth gently, but we smooth towards the most stable possible value.
            dotUnitSamples = (dotUnitSamples * 0.7) + (shortestDot * 0.3)
        }
        // --- END OF FIX ---
    }

    private fun morseToAscii(morse: String): String {
        return MorseCodeMaps.morseToAscii[morse] ?: ""
    }
}
