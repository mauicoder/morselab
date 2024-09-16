package net.maui.morselab.viewmodel

import android.media.AudioFormat
import android.media.AudioTrack
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
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
class PlayTextViewModel @Inject constructor(private val userPreferencesRepository: UserPreferencesRepositoryImpl
) : ViewModel(
) {

    private val SAMPLE_RATE = 44100

    var text = "Hello World!"
    val frequencyFlow: LiveData<Int> =
        userPreferencesRepository.getPreferencesFlow().map { it -> it.frequency }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()
    val wpmFlow: LiveData<Int> =
        userPreferencesRepository.getPreferencesFlow().map { it -> it.wpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()
    val farnsworthWpmFlow: LiveData<Int> =
        userPreferencesRepository.getPreferencesFlow().map { it -> it.farnsworthWpm }
            .shareIn( // Only collect from the booksRepository when the UI is visible
                viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                replay = 1
            ).asLiveData()

    private var playJob: Job? = null
    private val morseSoundGenerator = MorseSoundGenerator()
    private val shareFile = ShareFile()


    fun playMorseCallback() {
        if (playJob != null && playJob!!.isActive)
            return //ignore as a playJob is already running
        playJob = CoroutineScope(context = Dispatchers.Default).launch {
            playMorse(text, wpmFlow.value!!, farnsworthWpmFlow.value!!, frequencyFlow.value!!, SAMPLE_RATE)
        }
    }

    fun exportAsWave(activity: FragmentActivity) {
        val waveStream = morseSoundGenerator.generateWave(text, wpmFlow.value!!, farnsworthWpmFlow.value!!, frequencyFlow.value!!, SAMPLE_RATE)

        shareFile.shareAsFile(activity, waveStream)
    }

    private fun playMorse(text: String,
                          wpm: Int,
                          farnsworthWpm: Int,
                          frequency: Int,
                          sampleRate: Int) {
        val morseCodeSound = morseSoundGenerator.generate(text, wpmFlow.value!!, farnsworthWpmFlow.value!!, frequencyFlow.value!!, sampleRate)
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
                android.media.AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_8BIT)
                    .build()
            )
            .setBufferSizeInBytes(soundData.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(soundData, 0, soundData.size)
        audioTrack.play()

        // Ensure the sound plays fully
        Thread.sleep((soundData.size * 1000L / sampleRate))
        audioTrack.release()
    }
}