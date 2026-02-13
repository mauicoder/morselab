package net.maui.morselab.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.maui.morselab.databinding.FragmentPlayTextBinding
import net.maui.morselab.viewmodel.PlayTextViewModel
import javax.inject.Inject

@AndroidEntryPoint
class PlayTextFragment @Inject constructor(): Fragment() {

    private val viewModel: PlayTextViewModel by activityViewModels()
    private var _binding: FragmentPlayTextBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayTextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manual two-way binding for textLiveData
        viewModel.textLiveData.observe(viewLifecycleOwner) { text ->
            if (binding.editText.text.toString() != text) {
                binding.editText.setText(text)
            }
        }

        binding.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.textLiveData.value = s.toString()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Observe isReady to update button states
        viewModel.isReady.observe(viewLifecycleOwner) { isReady ->
            binding.playButton.isEnabled = isReady
            binding.saveButton.isEnabled = isReady
            binding.shareButton.isEnabled = isReady
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userPreferences.collect { prefs ->
                    // Update any labels that show current settings
                }
            }
        }

        binding.playButton.setOnClickListener {
            viewModel.playMorseCallback()
        }

        binding.saveButton.setOnClickListener {
            viewModel.saveMorseAsWaveFile(requireContext())
        }

        binding.shareButton.setOnClickListener {
            viewModel.shareMorseAsWaveFile(requireActivity())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
