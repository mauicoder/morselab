package net.maui.morselab.decoder

import net.maui.morselab.utils.AudioUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.math.min

class MorseDecoder2Test {

    // --- Test Configuration ---
    private val sampleRate = 16000
    private val blockSize = 512
    private val targetFreq = 800.0

    private lateinit var decoder: MorseDecoder
    private var decodedString = ""

    @Before
    fun setup() {
        decodedString = ""
        decoder = MorseDecoder(
            onDecoded = { decoded ->
                decodedString += decoded
            },
            targetFreq = targetFreq,
            sampleRate = sampleRate,
            blockSize = blockSize
        )
        // It's crucial to reset the decoder before each test run.
        decoder.reset()
    }

    @Test
    fun `decoder correctly decodes HI SOS from wav file`() {
        // 1. Load WAV file
        val audioByteArray = WavReader.readAudioData("hi-sos.wav")
        assertNotNull("Failed to read hi-sos.wav file from resources.", audioByteArray)

        // 2. Convert to FloatArray
        val audioFloatArray = AudioUtils.bytesToFloat(audioByteArray!!)

        // 3. Feed the audio data to the decoder using the sample-based method
        feedDataToDecoder(decoder, audioFloatArray)

        // 4. Assert the final decoded string is correct
        assertEquals("HI SOS", decodedString)
    }

    /**
     * Helper function to chunk a larger audio array and feed it to the decoder.
     * This version is fully deterministic and does not rely on Thread.sleep().
     */
    private fun feedDataToDecoder(decoder: MorseDecoder, audioData: FloatArray) {
        var offset = 0
        while (offset < audioData.size) {
            val remaining = audioData.size - offset

            // This is the number of actual audio samples we're processing in this iteration.
            val numSamplesInBlock = min(blockSize, remaining)

            val block = audioData.copyOfRange(offset, offset + numSamplesInBlock)

            // The 'finalBlock' is always padded to the full blockSize for the Goertzel algorithm,
            // but the decoder now knows how many "real" samples are in it.
            val finalBlock = if (block.size < blockSize) {
                block.copyOf(blockSize) // Pads with 0f by default
            } else {
                block
            }

            // Call the new, robust processBuffer method
            decoder.processBuffer(finalBlock, numSamplesInBlock)

            offset += numSamplesInBlock
        }

        // After the last audio block, we must call processBuffer one last time
        // with a silent block to trigger the timeout logic for the final word.
        // We tell it we're processing a full block's worth of "silence samples".
        decoder.processBuffer(FloatArray(blockSize), blockSize)
    }
}
