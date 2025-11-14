package net.maui.morselab.decoder

import androidx.compose.ui.text.toUpperCase
import net.maui.morselab.utils.AudioUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Locale
import java.util.Locale.getDefault
import kotlin.math.min

class MorseDecoder2Test {

    // --- Test Configuration ---
    // blockSize remains a static configuration for the decoder's processing loop.
    private val blockSize = 512
    // targetFreq is now handled automatically by the decoder's multiple Goertzel filters.

    private lateinit var decoder: MorseDecoder
    private var decodedString = ""

    // @Before setup is no longer needed as the decoder is created dynamically in the test.

    @Test
    fun `decoder correctly decodes HI SOS from wav file`() {
        // 1. Load WAV file and its metadata
        val wavFile = WavReader.readWavFile("hi-sos.wav")
        assertNotNull("Failed to read hi-sos.wav file from resources.", wavFile)
        wavFile!! // Use non-null assertion for convenience below

        // 2. Initialize the decoder dynamically with the sample rate from the file
        decodedString = ""
        decoder = MorseDecoder(
            onDecoded = { decoded ->
                decodedString += decoded
            },
            sampleRate = wavFile.sampleRate, // <-- DYNAMICALLY SET
            blockSize = blockSize
        )
        decoder.reset()

        // 3. Convert to FloatArray
        val audioFloatArray = AudioUtils.bytesToFloat(wavFile.audioData)

        // 4. Feed the audio data to the decoder
        feedDataToDecoder(decoder, audioFloatArray, wavFile.sampleRate)

        // 5. Assert the final decoded string is correct
        // Using trim() and replace() to make the assertion robust against extra whitespace.
        assertEquals("HI SOS", decodedString.trim())
    }

    @Test
    fun `decoder correctly decodes tapping CQ qrz 10956 3 from wav file`() {
        // 1. Load WAV file and its metadata
        val wavFile = WavReader.readWavFile("tapping_CQ_qrz_10956.3.wav")
        assertNotNull("Failed to read tapping_CQ_qrz_10956.3.wav file from resources.", wavFile)
        wavFile!! // Use non-null assertion for convenience below

        // 2. Initialize the decoder dynamically with the sample rate from the file
        decodedString = ""
        decoder = MorseDecoder(
            onDecoded = { decoded ->
                decodedString += decoded
            },
            sampleRate = wavFile.sampleRate, // <-- DYNAMICALLY SET
            blockSize = blockSize
        )
        decoder.reset()

        // 3. Convert to FloatArray
        val audioFloatArray = AudioUtils.bytesToFloat(wavFile.audioData)

        // 4. Feed the audio data to the decoder
        feedDataToDecoder(decoder, audioFloatArray, wavFile.sampleRate)

        // 5. Assert the final decoded string is correct
        // Using trim() and replace() to make the assertion robust against extra whitespace.
        assertEquals("tapping CQ qrz 10956.3".uppercase(getDefault()), decodedString.trim())
    }

    @Test
    fun `decoder correctly decodes ok 1056 from wav file`() {
        // 1. Load WAV file and its metadata
        val wavFile = WavReader.readWavFile("ok_1056.wav")
        assertNotNull("Failed to read ok_1056.wav file from resources.", wavFile)
        wavFile!! // Use non-null assertion for convenience below

        // 2. Initialize the decoder dynamically with the sample rate from the file
        decodedString = ""
        decoder = MorseDecoder(
            onDecoded = { decoded ->
                decodedString += decoded
            },
            sampleRate = wavFile.sampleRate, // <-- DYNAMICALLY SET
            blockSize = blockSize
        )
        decoder.reset()

        // 3. Convert to FloatArray
        val audioFloatArray = AudioUtils.bytesToFloat(wavFile.audioData)

        // 4. Feed the audio data to the decoder
        feedDataToDecoder(decoder, audioFloatArray, wavFile.sampleRate)

        // 5. Assert the final decoded string is correct
        // Using trim() and replace() to make the assertion robust against extra whitespace.
        assertEquals("OK 1056", decodedString.trim())
    }

    /**
     * Helper function to chunk a larger audio array and feed it to the decoder.
     * This version is fully deterministic and does not rely on Thread.sleep().
     */
    private fun feedDataToDecoder(decoder: MorseDecoder, audioData: FloatArray, sampleRate: Int) {
        var offset = 0
        while (offset < audioData.size) {
            val remaining = audioData.size - offset
            val numSamplesInBlock = min(blockSize, remaining)
            val block = audioData.copyOfRange(offset, offset + numSamplesInBlock)

            val finalBlock = if (block.size < blockSize) {
                block.copyOf(blockSize) // Pads with 0f by default
            } else {
                block
            }
            decoder.processBuffer(finalBlock, numSamplesInBlock)
            offset += numSamplesInBlock
        }

        // After all audio is processed, flush any remaining characters from the buffer.
        decoder.flush()
    }
}
