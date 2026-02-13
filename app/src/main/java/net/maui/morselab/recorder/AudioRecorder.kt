package net.maui.morselab.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import net.maui.morselab.utils.AudioUtils
import kotlin.concurrent.thread

/**
 * Handles the complexities of recording audio from the microphone.
 * It provides raw audio data buffers via a callback.
 */
class AudioRecorder(
    private val context: Context,
    private val sampleRate: Int,
    private val blockSize: Int,
    private val onBufferReady: (FloatArray, Int) -> Unit
) {
    private val audioBufSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(blockSize * 2)

    // Recorder is initialized lazily using the modern Builder (API 26+)
    private val recorder: AudioRecord by lazy {
        AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build()
            )
            .setBufferSizeInBytes(audioBufSize)
            .build()
    }

    @Volatile
    private var isRecording = false

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO permission not granted.")
        }

        if (isRecording) return

        isRecording = true
        recorder.startRecording()

        thread {
            val shortBuf = ShortArray(blockSize)
            val floatBuf = FloatArray(blockSize)

            while (isRecording) {
                val read = recorder.read(shortBuf, 0, blockSize)
                if (read > 0) {
                    AudioUtils.shortsToFloat(shortBuf, floatBuf, read)
                    // Pass the buffer to the consumer (our MorseDecoder)
                    onBufferReady(floatBuf, read)
                }
            }

            // Clean up when the loop finishes
            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.stop()
                recorder.release()
            }
        }
    }

    fun stop() {
        isRecording = false
    }
}
