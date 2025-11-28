package net.maui.morselab.generator

import org.junit.Test
import java.io.File

class MorseSoundGeneratorTest {

    @Test
    fun generateSosWavFileForAnalysis() {
        val generator = MorseSoundGenerator()
        val text = "SOS"
        val wpm = 20
        val farnsworthWpm = 20
        val frequency = 700
        val sampleRate = 44100

        val waveData = generator.generateWave(
            text,
            wpm,
            farnsworthWpm,
            frequency,
            sampleRate
        )

        val outputFile = File("build/sos_output.wav")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(waveData)

        println("WAV file generated for analysis at: ${outputFile.absolutePath}")
    }
}
