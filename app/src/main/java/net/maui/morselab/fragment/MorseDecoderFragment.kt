package net.maui.morselab.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.databinding.FragmentMorseDecoderBinding
import net.maui.morselab.decoder.MorseDecoder
import net.maui.morselab.recorder.AudioRecorder
import javax.inject.Inject

@AndroidEntryPoint
class MorseDecoderFragment @Inject constructor(): Fragment() {

    private var audioRecorder: AudioRecorder? = null

    private lateinit var morseDecoder: MorseDecoder
    private var isDecoding = false

    private var _binding: FragmentMorseDecoderBinding? = null
    private val binding get() = _binding!!

    // The ActivityResultLauncher for permission requests.
    @SuppressLint("MissingPermission")
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission was granted. We can now safely start the decoder.
                Toast.makeText(requireContext(), "Permission granted. Ready to start.", Toast.LENGTH_SHORT).show()
                // Call the new "safe" function here.
                safelyToggleDecoder()
            } else {
                // Permission was denied. Inform the user.
                Toast.makeText(requireContext(), "Permission denied. Cannot record audio.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 1. Initialize the MorseDecoder with a callback to update the UI
        morseDecoder = MorseDecoder(
            onDecoded = { decodedText ->
                // The decoder runs on a background thread.
                // We must update the UI on the main thread.
                activity?.runOnUiThread {
                    binding.textViewDecoded.append(decodedText)
                }
            }
            // You can configure other parameters like targetFreq here if needed
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMorseDecoderBinding.inflate(inflater, container, false)

        // The button click now triggers the permission check flow.
        binding.buttonStartStop.setOnClickListener {
            checkForPermissionAndStart()
        }

        updateUI()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // This function is the single "safe" entry point from the UI.
    @SuppressLint("MissingPermission")
    private fun checkForPermissionAndStart() {
        when {
            // Check if the permission is already granted.
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already available, call the new "safe" function.
                safelyToggleDecoder()
            }
            // Optional: Show a rationale if needed.
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                Toast.makeText(requireContext(), "Microphone access is required to decode Morse code.", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                // Directly ask for the permission.
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    @SuppressLint("MissingPermission")
    // This is our new "safe" function. It has no annotation and can be called freely
    // from within our permission-checking logic.
    private fun safelyToggleDecoder() {
        if (isDecoding) {
            stopDecoder()
        } else {
            // Because this function is only called after a successful permission check,
            // we can call startDecoder() without a lint warning.
            startDecoder()
        }
        isDecoding = !isDecoding
        updateUI()
    }

    @SuppressLint("MissingPermission")
    private fun startDecoder() {
        // Clear previous text and reset decoder state
        binding.textViewDecoded.text = ""
        morseDecoder.reset()
        audioRecorder = AudioRecorder(
            context = requireContext(),
            sampleRate = 16000,
            blockSize = 512,
            onBufferReady = morseDecoder::processBuffer
        ).also {
            try {
                // Now you just start the recorder
                it.start()
            } catch (e: SecurityException) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                // If start fails, clean up
                isDecoding = false
                audioRecorder = null
            }
        }
    }

    private fun stopDecoder() {
        // And stop the recorder
        audioRecorder?.stop()
        audioRecorder = null
    }

    private fun updateUI() {
        binding.buttonStartStop.text = if (isDecoding) "Stop Decoding" else "Start Decoding"
        binding.textViewDecoded.hint = if (isDecoding) "Listening for Morse code..." else "Press 'Start' to begin."
    }

    override fun onStop() {
        super.onStop()
        if (isDecoding) {
            stopDecoder()
            isDecoding = false
            updateUI()
        }
    }
}
