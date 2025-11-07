package net.maui.morselab.decoder

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

//1. Create a data class to hold the WAV file's properties and data
data class WavFile(    val audioData: ByteArray,
                       val sampleRate: Int,
                       val numChannels: Int,
                       val bitsPerSample: Int
) {
    // Override equals and hashCode for easier testing, especially for the ByteArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WavFile
        if (!audioData.contentEquals(other.audioData)) return false
        if (sampleRate != other.sampleRate) return false
        if (numChannels != other.numChannels) return false
        if (bitsPerSample != other.bitsPerSample) return false
        return true
    }

    override fun hashCode(): Int {
        var result = audioData.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + numChannels
        result = 31 * result + bitsPerSample
        return result
    }
}


/**
 * A simple utility to read audio data and header information from a WAV file stream.
 * Note: This is a minimal implementation for testing. It expects a standard 16-bit PCM WAV file.
 */
object WavReader {
    fun readWavFile(resourceName: String): WavFile? {
        val stream: InputStream? = WavReader::class.java.getResourceAsStream("/$resourceName")

        return stream?.use { inputStream ->
            // Use the recommended 'readNBytes' method instead of the deprecated 'readBytes'.
            val headerBytes = inputStream.readNBytes(44)
            if (headerBytes.size < 44) {
                println("Error: Incomplete WAV header.")
                return@use null
            }

            // Wrap the header in a ByteBuffer to easily read multi-byte values.
            // WAV files use Little Endian byte order.
            val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)

            // 2. Parse the required fields from the header
            val numChannels = headerBuffer.getShort(22).toInt()
            val sampleRate = headerBuffer.getInt(24)
            val bitsPerSample = headerBuffer.getShort(34).toInt()

            // The rest of the stream is the raw audio data
            val audioData = inputStream.readBytes() // readBytes() with no arguments is not deprecated

            WavFile(
                audioData = audioData,
                sampleRate = sampleRate,
                numChannels = numChannels,
                bitsPerSample = bitsPerSample
            )
        }
    }
}
