package net.maui.morselab.fragments

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.maui.morselab.PlayTextViewModel
import net.maui.morselab.R
import net.maui.morselab.ShareFile
import net.maui.morselab.databinding.FragmentPlayTextBinding
import net.maui.morselab.generator.MorseSoundGenerator


class PlayTextFragment : Fragment() {

    private val morseSoundGenerator = MorseSoundGenerator()
    private val shareFile = ShareFile()

    private val SAMPLE_RATE = 44100


    private var playJob: Job? = null
    private var frequency = 800
    private var wpm = 20
    private var farnsworthWpm = 20


    private val viewModel: PlayTextViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentPlayTextBinding>(
            inflater, R.layout.fragment_play_text, container, false);

        binding.buttonPlay.setOnClickListener { playMorseCallback(it) }
        binding.buttonExport.setOnClickListener { exportAsWave(it) }

        return binding.root
    }


    private fun playMorse() {
        val text = viewModel.text//editTextText.text.toString()
        val morseCodeSound = morseSoundGenerator.generate(text, wpm, farnsworthWpm, frequency, SAMPLE_RATE)
        playSound(morseCodeSound, SAMPLE_RATE)
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


    //Button  callback
    private fun playMorseCallback(view: View) {
        if (playJob != null && playJob!!.isActive)
            return //ignore as a playJob is already running
        playJob = CoroutineScope(context = Dispatchers.Default).launch {
            playMorse()
        }
    }

    // Button  callback
    private fun exportAsWave(view: View) {
        val text = viewModel.text

        val waveStream = morseSoundGenerator.generateWave(text, wpm, farnsworthWpm, frequency, SAMPLE_RATE)

        shareFile.shareAsFile(requireActivity(), waveStream)
    }

}