package net.maui.morselab.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A singleton utility object for handling common audio data conversions.
 * Provides consistent, overloaded methods for both creating new arrays (convenience)
 * and filling existing ones (performance).
 */
object AudioUtils {

    // --- ByteArray to FloatArray Conversions ---

    /**
     * Converts a ByteArray of 16-bit PCM audio into a new, normalized FloatArray.
     * This version allocates a new FloatArray.
     *
     * @param byteArray The raw byte data (16-bit PCM).
     * @return A new FloatArray where each sample is in the range [-1.0, 1.0].
     */
    fun bytesToFloat(byteArray: ByteArray): FloatArray {
        val floatArray = FloatArray(byteArray.size / 2)
        bytesToFloat(byteArray, floatArray)
        return floatArray
    }

    /**
     * Converts a ByteArray of 16-bit PCM audio into a provided destination FloatArray.
     * This high-performance version avoids allocation. The destination array must have enough capacity.
     *
     * @param source The raw byte data (16-bit PCM).
     * @param destination The FloatArray to write the converted samples into.
     */
    fun bytesToFloat(source: ByteArray, destination: FloatArray) {
        // Wrap the byte array in a buffer and set it to use little-endian byte order.
        val buffer = ByteBuffer.wrap(source).order(ByteOrder.LITTLE_ENDIAN)

        for (i in destination.indices) {
            // Stop if the source buffer has been exhausted
            if (!buffer.hasRemaining()) break

            // Get the next 16-bit short from the buffer.
            val shortSample = buffer.getShort()
            // Convert the short to a float in the range [-1.0, 1.0] and store it.
            destination[i] = shortSample / 32768.0f
        }
    }


    // --- ShortArray to FloatArray Conversions ---

    /**
     * Converts a ShortArray of 16-bit PCM audio into a new, normalized FloatArray.
     * This version allocates a new FloatArray.
     *
     * @param source The ShortArray containing the raw audio samples.
     * @return A new FloatArray where each sample is in the range [-1.0, 1.0].
     */
    fun shortsToFloat(source: ShortArray): FloatArray {
        val floatArray = FloatArray(source.size)
        shortsToFloat(source, floatArray, source.size)
        return floatArray
    }

    /**
     * Converts a ShortArray of 16-bit PCM audio samples into a destination FloatArray.
     * This high-performance version avoids allocation.
     *
     * @param source The ShortArray containing the raw audio samples.
     * @param destination The FloatArray to write the converted samples into. Must be at least as large as `readSize`.
     * @param readSize The number of samples from the source to convert.
     */
    fun shortsToFloat(source: ShortArray, destination: FloatArray, readSize: Int) {
        for (i in 0 until readSize) {
            // Ensure we don't go out of bounds on either array
            if (i >= source.size || i >= destination.size) break
            destination[i] = source[i] / 32768.0f
        }
    }
}
