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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import net.maui.morselab.ShareFile
import net.maui.morselab.data.UserPreferencesRepositoryImpl
import net.maui.morselab.generator.MorseSoundGenerator
import javax.inject.Inject

@HiltViewModel
class PlayTextViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepositoryImpl
) : ViewModel(
) {

    private val SAMPLE_RATE = 44100
    private val TAG = "PlayTextViewModel"

    private var playJob: Job? = null
    private val morseSoundGenerator = MorseSoundGenerator()
    private val shareFile = ShareFile()

    val textLiveData: MutableLiveData<String> = MutableLiveData("Hello")

    val frequencyFlow: LiveData<Int> = 
        userPreferencesRepository.getPreferencesFlow().map { it.frequency }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()
    val wpmFlow: LiveData<Int> = 
        userPreferencesRepository.getPreferencesFlow().map { it.wpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()

    val farnsworthWpmFlow: LiveData<Int> = 
        userPreferencesRepository.getPreferencesFlow().map { it.farnsworthWpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()

    private val _isReady = MediatorLiveData<Boolean>().apply {
        value = false
        val check = { _: Any? ->
            value = wpmFlow.value != null && frequencyFlow.value != null && farnsworthWpmFlow.value != null
        }
        addSource(wpmFlow, check)
        addSource(frequencyFlow, check)
        addSource(farnsworthWpmFlow, check)
    }
    val isReady: LiveData<Boolean> = _isReady


    fun playMorseCallback() {
        if (playJob?.isActive == true || isReady.value == false) return

        val text = textLiveData.value ?: return
        val wpm = wpmFlow.value ?: return
        val farnsworthWpm = farnsworthWpmFlow.value ?: return
        val frequency = frequencyFlow.value ?: return

        playJob = CoroutineScope(context = Dispatchers.Default).launch {
            playMorse(
                text,
                wpm,
                farnsworthWpm,
                frequency,
                SAMPLE_RATE
            )
        }
    }

    fun shareMorseAsWaveFile(activity: FragmentActivity) {
        if (isReady.value == false) return
        val text = textLiveData.value ?: return
        val wpm = wpmFlow.value ?: return
        val farnsworthWpm = farnsworthWpmFlow.value ?: return
        val frequency = frequencyFlow.value ?: return

        val waveStream = morseSoundGenerator.generateWave(
            text,
            wpm,
            farnsworthWpm,
            frequency,
            SAMPLE_RATE
        )
        shareFile.shareAsFile(activity, waveStream)
    }

    fun saveMorseAsWaveFile(context: Context) {
        if (isReady.value == false) return
        val text = textLiveData.value ?: return
        val wpm = wpmFlow.value ?: return
        val farnsworthWpm = farnsworthWpmFlow.value ?: return
        val frequency = frequencyFlow.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val waveStream = morseSoundGenerator.generateWave(
                text,
                wpm,
                farnsworthWpm,
                frequency,
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

    private fun playMorse(
        text: String,
        wpm: Int,
        farnsworthWpm: Int,
        frequency: Int,
        sampleRate: Int
    ) {

        Log.i(TAG, "playMorse( $text $wpm $farnsworthWpm $frequency $sampleRate)")
        val morseCodeSound = morseSoundGenerator.generate(
            text,
            wpm,
            farnsworthWpm,
            frequency,
            sampleRate
        )
        playSound(morseCodeSound, sampleRate)
    }

    private fun playSound(soundData: ByteArray, sampleRate: Int) {
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
            .setBufferSizeInBytes(soundData.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack) {
                // Not used
            }

            override fun onPeriodicNotification(track: AudioTrack) {
                if (track.playbackHeadPosition >= soundData.size) {
                    track.release()
                }
            }
        }, null)

        audioTrack.write(soundData, 0, soundData.size)
        audioTrack.play()
    }
}