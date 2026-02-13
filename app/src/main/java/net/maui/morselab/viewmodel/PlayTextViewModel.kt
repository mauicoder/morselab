package net.maui.morselab.viewmodel

import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import net.maui.morselab.ShareFile
import net.maui.morselab.data.UserPreferences
import net.maui.morselab.data.UserPreferencesRepository
import net.maui.morselab.generator.MorseSoundGenerator
import javax.inject.Inject

@HiltViewModel
class PlayTextViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val SAMPLE_RATE = 44100
    private val TAG = "PlayTextViewModel"

    private var playJob: Job? = null
    private val morseSoundGenerator = MorseSoundGenerator()
    private val shareFile = ShareFile()

    val textLiveData: MutableLiveData<String> = MutableLiveData("Hello")

    val userPreferences: StateFlow<UserPreferences> = userPreferencesRepository.userPreferences

    val isReady: LiveData<Boolean> = userPreferences.map { true }.asLiveData()

    fun playMorseCallback() {
        if (playJob?.isActive == true) {
            Log.w(TAG, "Playback already in progress, ignoring request.")
            return
        }

        val prefs = userPreferences.value
        val text = textLiveData.value ?: return
        
        Log.i(TAG, "playMorseCallback: text=$text, prefs=$prefs")
        
        playJob = viewModelScope.launch(Dispatchers.Default) {
            Log.i(TAG, "Playback coroutine started.")
            try {
                val soundData = morseSoundGenerator.generate(
                    text,
                    prefs.wpm,
                    prefs.farnsworthWpm,
                    prefs.frequency,
                    SAMPLE_RATE
                )
                Log.i(TAG, "Sound generated, size=${soundData.size} bytes.")
                playSound(soundData, SAMPLE_RATE)
            } catch (e: Exception) {
                Log.e(TAG, "Error in playback coroutine", e)
            } finally {
                Log.i(TAG, "Playback coroutine finished.")
            }
        }
    }

    fun shareMorseAsWaveFile(activity: FragmentActivity) {
        val prefs = userPreferences.value
        val text = textLiveData.value ?: return

        val waveStream = morseSoundGenerator.generateWave(
            text,
            prefs.wpm,
            prefs.farnsworthWpm,
            prefs.frequency,
            SAMPLE_RATE
        )
        shareFile.shareAsFile(activity, waveStream)
    }

    fun saveMorseAsWaveFile(context: Context) {
        val prefs = userPreferences.value
        val text = textLiveData.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val waveStream = morseSoundGenerator.generateWave(
                text,
                prefs.wpm,
                prefs.farnsworthWpm,
                prefs.frequency,
                SAMPLE_RATE
            )

            val resolver = context.contentResolver
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, "morse_output_${System.currentTimeMillis()}.wav")
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Music/MorseLab")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(collection, contentValues)

            uri?.let {
                try {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(waveStream)
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "File saved to Music/MorseLab", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving file", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Error saving file", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun playSound(soundData: ByteArray, sampleRate: Int) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_8BIT
        )
        
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBufferSize, soundData.size))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack.play()
            var offset = 0
            while (offset < soundData.size) {
                val bytesToWrite = minOf(soundData.size - offset, minBufferSize)
                val result = audioTrack.write(soundData, offset, bytesToWrite)
                if (result <= 0) break
                offset += result
            }
            
            // Wait for the hardware to finish playing the buffered data
            val totalFrames = soundData.size // 1 byte per frame for 8-bit mono
            while (audioTrack.playbackHeadPosition < totalFrames) {
                delay(50)
            }
            Log.i(TAG, "Playback finished physically.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during AudioTrack playback", e)
        } finally {
            audioTrack.stop()
            audioTrack.release()
            Log.i(TAG, "AudioTrack released.")
        }
    }
}