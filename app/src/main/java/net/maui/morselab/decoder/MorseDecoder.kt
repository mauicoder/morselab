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

    // Pool of filters to select from
    private val goertzel800: Goertzel
    private val goertzel700: Goertzel
    private val goertzel600: Goertzel

    private val magHistory = ArrayDeque<Double>()
    private val histSize = 50
    private var onState = false

    private var totalSamplesProcessed: Long = 0
    private var lastStateChangeSamplePos: Long = 0
    private var dotUnitSamples: Double = sampleRate * 0.060 // Default 60ms dot (20 WPM)
    private var silenceUnitSamples: Double = sampleRate * 0.060 // Default 60ms silence unit

    private val currentCharMorse = StringBuilder()
    private var hasDecodedFirstChar = false

    // State for robust tone/silence processing
    private var pendingToneDuration: Long = 0
    private val recentDotDurations = ArrayDeque<Long>()
    private val TONE_HISTORY_SIZE = 10


    init {
        logger.info("Initializing Goertzel filters...")
        logger.info("sampleRate: $sampleRate, blockSize: $blockSize")
        goertzel800 = Goertzel(sampleRate.toDouble(), 800.0, blockSize)
        goertzel700 = Goertzel(sampleRate.toDouble(), 700.0, blockSize)
        goertzel600 = Goertzel(sampleRate.toDouble(), 600.0, blockSize)
    }

    fun processBuffer(audioBuffer: FloatArray, numSamples: Int) {
        totalSamplesProcessed += numSamples

        val mag: Double
        val median = magHistory.sorted().getOrElse(magHistory.size / 2) { 0.0 }
        val thresholdOn = median * 6.0 + 1e-9

        if (goertzel == null) {
            val mag800 = goertzel800.magnitudeSquared(audioBuffer)
            val mag700 = goertzel700.magnitudeSquared(audioBuffer)
            val mag600 = goertzel600.magnitudeSquared(audioBuffer)
            mag = maxOf(mag800, mag700, mag600)

            if (mag > thresholdOn) {
                if (mag == mag800) goertzel = goertzel800
                else if (mag == mag700) goertzel = goertzel700
                else goertzel = goertzel600
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
            val eventTime = totalSamplesProcessed - numSamples
            var duration = (eventTime - lastStateChangeSamplePos).coerceAtLeast(0L)
            if (duration > blockSize) { // Only compensate for latency on longer signals
                duration -= (blockSize / 2)
            }

            logger.info("STATE CHANGE: ${if (onState) "TONE" else "SILENCE"} -> ${if (newOn) "TONE" else "SILENCE"}. Duration: $duration")

            if (onState) { // TONE -> SILENCE transition
                if (duration > 0) {
                    pendingToneDuration += duration
                    logger.info("TONE part ended. Added $duration. Pending duration: $pendingToneDuration")
                }
            } else { // SILENCE -> TONE transition
                val silenceDuration = duration
                val noiseThreshold = (sampleRate * 0.005).toLong() // 5ms threshold

                if (silenceDuration >= noiseThreshold) {
                    if (pendingToneDuration > 0) {
                        processTone(pendingToneDuration)
                    }
                    pendingToneDuration = 0
                    processSilence(silenceDuration)
                } else {
                    if (pendingToneDuration > 0) {
                        logger.info("Noise blip of $silenceDuration samples detected. Ignoring.")
                    }
                }
            }
            lastStateChangeSamplePos = eventTime
        }
        onState = newOn
    }

    fun flush() {
        logger.info("--- flush() called ---")
        var finalDuration = (totalSamplesProcessed - lastStateChangeSamplePos).coerceAtLeast(0L)
        if (finalDuration > blockSize) {
            finalDuration -= (blockSize / 2)
        }

        if (onState) { // Ends on a tone
            pendingToneDuration += finalDuration
            if (pendingToneDuration > 0) {
                processTone(pendingToneDuration)
            }
        } else { // Ends on a silence
            if (pendingToneDuration > 0) {
                processTone(pendingToneDuration)
            }
            processSilence(finalDuration)
        }

        // Force the end of the last character
        processSilence((dotUnitSamples * 7).toLong())
    }


    private fun processTone(durationInSamples: Long) {
        if (dotUnitSamples <= 0) return
        val units = (durationInSamples / dotUnitSamples).roundToInt().coerceAtLeast(1)
        var symbol = if (units < 2) '.' else '-'
        logger.info("processTone: Final duration=$durationInSamples, dotUnitSamples=$dotUnitSamples, units=$units, symbol=$symbol")

        if (!hasDecodedFirstChar && currentCharMorse.isEmpty() && symbol == '.' && durationInSamples > dotUnitSamples * 1.1) {
            logger.info("the symbol has been overwritten to a dash by the initial tone heuristic")
            symbol = '-'
        }

        currentCharMorse.append(symbol)
        updateUnitDuration(durationInSamples, symbol == '.')
    }

    private fun processSilence(durationInSamples: Long) {
        if (silenceUnitSamples <= 0 || durationInSamples <= 0) return
        val silenceUnits = (durationInSamples / silenceUnitSamples).roundToInt().coerceAtLeast(1)
        logger.info("processSilence: Duration=$durationInSamples, silenceUnit=${silenceUnitSamples.roundToInt()}, Calculated Units=$silenceUnits")

        if (silenceUnits >= 3) {
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
            logger.info("Silence detected as INTRA-CHARACTER gap (units < 3).")
            updateSilenceUnit(durationInSamples)
        }
    }

    fun reset() {
        logger.info("--- Decoder Reset ---")
        magHistory.clear()
        onState = false
        totalSamplesProcessed = 0
        lastStateChangeSamplePos = 0
        dotUnitSamples = sampleRate * 0.060
        silenceUnitSamples = sampleRate * 0.060
        currentCharMorse.clear()
        goertzel = null
        hasDecodedFirstChar = false
        pendingToneDuration = 0
        recentDotDurations.clear()
    }
    private fun updateSilenceUnit(durationInSamples: Long) {
        val evidenceDuration = durationInSamples.toDouble()
        if (evidenceDuration <= 0) return

        val oldUnit = silenceUnitSamples
        // Use a simple smoothing average for silence
        silenceUnitSamples = (silenceUnitSamples * 0.7) + (evidenceDuration * 0.3)
        logger.info("updateSilenceUnit: old=${oldUnit.roundToInt()}, evidence=${evidenceDuration.roundToInt()}, new=${silenceUnitSamples.roundToInt()}")
    }

    private fun updateUnitDuration(durationInSamples: Long, isDot: Boolean) {
        if (isDot) {
            recentDotDurations.addLast(durationInSamples)
            if (recentDotDurations.size > TONE_HISTORY_SIZE) {
                recentDotDurations.removeFirst()
            }

            if (recentDotDurations.size < 5) {
                val evidenceDuration = durationInSamples.toDouble()
                if (abs(dotUnitSamples - evidenceDuration) > (dotUnitSamples * 0.5)) {
                    dotUnitSamples = evidenceDuration
                    silenceUnitSamples = evidenceDuration // Keep units in sync on snap
                } else {
                    dotUnitSamples = (dotUnitSamples * 0.7) + (evidenceDuration * 0.3)
                }
            } else {
                val sortedDots = recentDotDurations.sorted()
                val shortestHalf = sortedDots.subList(0, sortedDots.size / 2 + 1)
                val medianDot = shortestHalf[shortestHalf.size / 2].toDouble()

                val oldUnit = dotUnitSamples
                if (medianDot > 0) {
                    dotUnitSamples = (dotUnitSamples * 0.3) + (medianDot * 0.7)
                }
                logger.info("updateUnitDuration(dot): oldUnit=${oldUnit.roundToInt()}, medianDot=${medianDot.roundToInt()}, newUnit=${dotUnitSamples.roundToInt()}")
            }
        } else { // isDash
            val evidenceDuration = durationInSamples / 3.0
            if (evidenceDuration <= 0) return

            val oldUnit = dotUnitSamples
            if (abs(dotUnitSamples - evidenceDuration) > (dotUnitSamples * 0.5)) {
                logger.info("updateUnitDuration(dash): Snapping dotUnitSamples. old=${oldUnit.roundToInt()}, evidence=${evidenceDuration.roundToInt()}")
                dotUnitSamples = evidenceDuration
                silenceUnitSamples = evidenceDuration // Keep units in sync on snap
            } else {
                dotUnitSamples = (dotUnitSamples * 0.7) + (evidenceDuration * 0.3)
                logger.info("updateUnitDuration(dash): Smoothing dotUnitSamples. old=${oldUnit.roundToInt()}, evidence=${evidenceDuration.roundToInt()}, new=${dotUnitSamples.roundToInt()}")
            }
        }
    }

    private fun morseToAscii(morse: String): String {
        return MorseCodeMaps.morseToAscii[morse] ?: ""
    }
}
