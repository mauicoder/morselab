package net.maui.morselab.decoder

import net.maui.morselab.utils.MorseCodeMaps
import java.util.Locale
import java.util.logging.Logger

class MorseDecoder(
    private val onDecoded: (String) -> Unit,
    private val sampleRate: Int = 16000,
    private val blockSize: Int = 512,
) {
    private val logger = Logger.getLogger(MorseDecoder::class.java.name)
    private var goertzel: Goertzel? = null

    private val goertzel800: Goertzel = Goertzel(sampleRate.toDouble(), 800.0, blockSize)
    private val goertzel700: Goertzel = Goertzel(sampleRate.toDouble(), 700.0, blockSize)
    private val goertzel600: Goertzel = Goertzel(sampleRate.toDouble(), 600.0, blockSize)

    private val magHistory = ArrayDeque<Double>()
    private val histSize = 50
    private var onState = false

    private var totalSamplesProcessed: Long = 0
    private var lastStateChangeSamplePos: Long = 0
    private var dotUnitSamples: Double = sampleRate * 0.060 // Default 20 WPM
    private var silenceUnitSamples: Double = sampleRate * 0.060

    private val currentCharMorse = StringBuilder()
    private var hasDecodedFirstChar = false
    private var pendingToneDuration: Long = 0

    init {
        logger.info("Initializing MorseDecoder: sampleRate=$sampleRate, blockSize=$blockSize")
    }

    fun processBuffer(audioBuffer: FloatArray, numSamples: Int) {
        totalSamplesProcessed += numSamples

        val mag: Double
        val sortedHist = magHistory.sorted()
        val median = if (sortedHist.isNotEmpty()) sortedHist[sortedHist.size / 2] else 0.0
        val thresholdOn = median * 6.0 + 1e-9

        if (goertzel == null) {
            val mag800 = goertzel800.magnitudeSquared(audioBuffer)
            val mag700 = goertzel700.magnitudeSquared(audioBuffer)
            val mag600 = goertzel600.magnitudeSquared(audioBuffer)
            mag = maxOf(mag800, mag700, mag600)

            if (mag > thresholdOn) {
                goertzel = when (mag) {
                    mag800 -> goertzel800
                    mag700 -> goertzel700
                    else -> goertzel600
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
            val eventTime = totalSamplesProcessed - numSamples
            var duration = (eventTime - lastStateChangeSamplePos).coerceAtLeast(0L)
            if (duration > blockSize) duration -= (blockSize / 2)

            if (onState) { // TONE -> SILENCE
                pendingToneDuration += duration
            } else { // SILENCE -> TONE
                if (duration >= (sampleRate * 0.005).toLong()) {
                    if (pendingToneDuration > 0) processTone(pendingToneDuration)
                    pendingToneDuration = 0
                    processSilence(duration)
                }
            }
            lastStateChangeSamplePos = eventTime
            onState = newOn
        }
    }

    fun flush() {
        var finalDuration = (totalSamplesProcessed - lastStateChangeSamplePos).coerceAtLeast(0L)
        if (finalDuration > blockSize) finalDuration -= (blockSize / 2)

        if (onState) {
            pendingToneDuration += finalDuration
            if (pendingToneDuration > 0) processTone(pendingToneDuration)
        } else {
            if (pendingToneDuration > 0) processTone(pendingToneDuration)
            processSilence(finalDuration)
        }
        processSilence((dotUnitSamples * 7).toLong())
    }

    private fun processTone(duration: Long) {
        if (dotUnitSamples <= 0) return
        val units = duration.toDouble() / dotUnitSamples
        
        // Thresholds: 1.4 for the very first symbol, 2.0 otherwise.
        val threshold = if (!hasDecodedFirstChar && currentCharMorse.isEmpty()) 1.4 else 2.0
        val symbol = if (units < threshold) '.' else '-'
        
        logger.info(String.format(Locale.US, "Tone: dur=%d, unit=%.1f, units=%.2f, sym=%c", 
            duration, dotUnitSamples, units, symbol))

        currentCharMorse.append(symbol)
        updateUnitDuration(duration, symbol == '.')
    }

    private fun processSilence(duration: Long) {
        if (silenceUnitSamples <= 0 || duration <= 0) return
        val units = duration.toDouble() / silenceUnitSamples
        
        logger.info(String.format(Locale.US, "Silence: dur=%d, unit=%.1f, units=%.2f", 
            duration, silenceUnitSamples, units))

        // Lower threshold for character/word gaps to handle tight timing
        if (units >= 2.2) {
            if (currentCharMorse.isNotEmpty()) {
                val char = morseToAscii(currentCharMorse.toString())
                if (char.isNotEmpty()) {
                    onDecoded(char)
                    hasDecodedFirstChar = true
                }
                currentCharMorse.clear()
            }
            if (units >= 5.5 && hasDecodedFirstChar) {
                onDecoded(" ")
            }
        } else if (units < 1.8) {
            updateSilenceUnit(duration)
        }
    }

    private fun updateUnitDuration(duration: Long, isDot: Boolean) {
        val evidence = if (isDot) duration.toDouble() else duration / 3.0
        
        if (!hasDecodedFirstChar && currentCharMorse.length == 1) {
            // Initial snap to the first tone's unit
            dotUnitSamples = evidence
            silenceUnitSamples = evidence
        } else {
            dotUnitSamples = (dotUnitSamples * 0.85) + (evidence * 0.15)
            // Gently pull silence unit towards tone unit to maintain sync
            silenceUnitSamples = (silenceUnitSamples * 0.95) + (dotUnitSamples * 0.05)
        }
    }

    private fun updateSilenceUnit(duration: Long) {
        val evidence = duration.toDouble()
        silenceUnitSamples = (silenceUnitSamples * 0.85) + (evidence * 0.15)
    }

    private fun morseToAscii(morse: String): String = MorseCodeMaps.morseToAscii[morse] ?: ""

    fun reset() {
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
    }
}
