package net.maui.morselab.decoder

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A simple utility to read audio data from a WAV file stream.
 * Note: This is a minimal implementation for testing purposes. It expects
 * a standard 16-bit PCM mono WAV file.
 */
object WavReader {
    fun readAudioData(resourceName: String): ByteArray? {
        val stream: InputStream? = WavReader::class.java.getResourceAsStream("/$resourceName")

        return stream?.use {
            // Read the WAV header (44 bytes for a standard PCM file)
            val content = it.readBytes()
            //val header = it.readBytes(44)
            val header = content.copyOfRange(0, 44)

            // Parse the WAV header
            if (header.size < 44) {
                println("Error: Incomplete WAV header.")
                return@use null
            }

            // You could parse the header here to verify sample rate, channels, etc.
            // For this test, we assume it's correct.

            // The rest of the stream is the raw audio data
            content.copyOfRange(44, content.size)
        }
    }
}
