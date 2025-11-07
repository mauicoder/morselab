package net.maui.morselab.decoder
import kotlin.math.*

/** Simple Goertzel implementation for one frequency */
class Goertzel(val sampleRate: Double, val targetFreq: Double, val blockSize: Int) {
    private val k = (0.5 + (blockSize * targetFreq / sampleRate)).toInt()
    private val omega = 2.0 * Math.PI * k / blockSize
    private val coeff = 2.0 * cos(omega)
    private var sPrev = 0.0
    private var sPrev2 = 0.0

    fun reset() { sPrev = 0.0; sPrev2 = 0.0 }

    /** feed a block (FloatArray or DoubleArray) and return magnitude^2 */
    fun magnitudeSquared(samples: FloatArray): Double {
        sPrev = 0.0; sPrev2 = 0.0
        for (i in 0 until min(blockSize, samples.size)) {
            val s = samples[i].toDouble() + coeff * sPrev - sPrev2
            sPrev2 = sPrev
            sPrev = s
        }
        val real = sPrev - sPrev2 * cos(omega)
        val imag = sPrev2 * sin(omega)
        return real*real + imag*imag
    }
}