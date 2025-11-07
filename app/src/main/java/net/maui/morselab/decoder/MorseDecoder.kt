package net.maui.morselab.decoder

import net.maui.morselab.utils.MorseCodeMaps
import java.util.logging.Logger
import kotlin.math.abs
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
        val median = magHistory.sorted().getOrElse(magHistory.size / 2) { 0.0 }
        val thresholdOn = median * 6.0 + 1e-9

        if (goertzel == null) {
            val mag800 = goertzel800.magnitudeSquared(audioBuffer)
            val mag700 = goertzel700.magnitudeSquared(audioBuffer)
            val mag600 = goertzel600.magnitudeSquared(audioBuffer)
            // Restore Frequency Scan Log
            logger.info("Frequency Scan: mag800=${mag800.toInt()}, mag700=${mag700.toInt()}, mag600=${mag600.toInt()}")
            mag = maxOf(mag800, mag700, mag600)

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

        if (mag < thresholdOn) {
            magHistory.addLast(mag)
            if (magHistory.size > histSize) magHistory.removeFirst()
        }

        val thresholdOff = median * 3.0 + 1e-9
        val newOn = if (onState) mag > thresholdOff else mag > thresholdOn

        if (newOn != onState) {
            val durationOfPreviousState = totalSamplesProcessed - lastStateChangeSamplePos
            // Restore State Change Log
            logger.info(
                "STATE CHANGE: ${if (onState) "TONE" else "SILENCE"} -> ${if (newOn) "TONE" else "SILENCE"}. " +
                        "Duration: $durationOfPreviousState"
            )
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
        logger.info("--- flush() called ---")
        val finalDuration = totalSamplesProcessed - lastStateChangeSamplePos
        if (finalDuration > sampleRate * 0.015) {
            if (onState) processTone(finalDuration)
            else processSilence(finalDuration)
        }
        logger.info("Flushing with final fake long silence.")
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
        // Restore Detailed Tone Log
        val timeInMs = (totalSamplesProcessed * 1000) / sampleRate
        logger.info(
            "TONE ended at sample $totalSamplesProcessed (${timeInMs}ms). " +
                    "Buffer: '${currentCharMorse}' (Symbol: $symbol, Duration: $durationInSamples, Unit: ${dotUnitSamples.roundToInt()})"
        )
    }

    private fun processSilence(durationInSamples: Long) {
        if (dotUnitSamples <= 0) return
        val silenceUnits = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)
        // Restore Detailed Silence Log
        val timeInMs = (totalSamplesProcessed * 1000) / sampleRate
        logger.info(
            "processSilence at sample $totalSamplesProcessed (${timeInMs}ms): " +
                    "Duration=$durationInSamples, dotUnit=${dotUnitSamples.roundToInt()}, Calculated Units=$silenceUnits"
        )

        if (silenceUnits >= 3) {
            logger.info("Silence detected as SEPARATOR (units >= 3).")
            if (currentCharMorse.isNotEmpty()) {
                val char = morseToAscii(currentCharMorse.toString())
                logger.info("End char: '${currentCharMorse}' -> '$char'")
                if (char.isNotEmpty()) {
                    onDecoded(char)
                    hasDecodedFirstChar = true
                }
                currentCharMorse.clear()
            }
            if (silenceUnits >= 7 && hasDecodedFirstChar) {
                logger.info("Firing WORD SPACE")
                onDecoded(" ")
            }
        } else {
            logger.info("Silence detected as INTRA-CHARACTER gap (units < 3). No action taken.")
        }
    }

    fun reset() {
        logger.info("--- Decoder Reset ---")
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
        if (recentDotDurations.size < 5) {
            //check if the current dotUnitSamples is way off from the provided dotDuration

            if(abs(dotUnitSamples - dotDuration) > (dotUnitSamples * 0.5)) {
                logger.info("updateUnitDuration: dotUnitSamples=${dotUnitSamples.roundToInt()}, way off, set to dotDuration=${dotDuration}")
                dotUnitSamples = dotDuration.toDouble()
            } else if(dotUnitSamples < 0.9 * dotDuration || dotUnitSamples > 1.1 * dotDuration) {
                logger.info("updateUnitDuration: dotUnitSamples=${dotUnitSamples.roundToInt()}, slightly off, adapting by 50% of the difference")
                // --- START OF COMPLETED CODE ---
                // Adapt to 50% of the difference
                dotUnitSamples = (dotUnitSamples + dotDuration.toDouble()) / 2.0
                // --- END OF COMPLETED CODE ---
            }
            return
        } // Need enough data for a stable calculation

        // --- THE FINAL AND UNBREAKABLE FIX ---
        // The new unit is the PURE MEDIAN of the SHORTEST HALF of the last known dots.
        // This aggressively resists upward drift from "long dots".
        val sortedDots = recentDotDurations.sorted()
        val shortestHalf = sortedDots.subList(0, sortedDots.size / 2 + 1)
        val medianDot = shortestHalf[shortestHalf.size / 2].toDouble()

        val oldUnit = dotUnitSamples
        if (medianDot > 0) {
            // Apply a gentle smoothing to this extremely stable median.
            dotUnitSamples = (dotUnitSamples * 0.3) + (medianDot * 0.7)
        }
        // Restore Detailed Timing Log
        logger.info("updateUnitDuration: oldUnit=${oldUnit.roundToInt()}, medianDot=${medianDot.roundToInt()}, newUnit=${dotUnitSamples.roundToInt()}")
        // --- END OF FIX ---
    }

    private fun morseToAscii(morse: String): String {
        return MorseCodeMaps.morseToAscii[morse] ?: ""
    }
}
