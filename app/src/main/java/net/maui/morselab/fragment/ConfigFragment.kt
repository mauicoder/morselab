package net.maui.morselab.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.databinding.FragmentConfigBinding
import net.maui.morselab.viewmodel.UserPreferencesViewModel
import javax.inject.Inject


@AndroidEntryPoint
class ConfigFragment @Inject constructor() : Fragment() {

    private val viewModel: UserPreferencesViewModel by activityViewModels()
    private var _binding: FragmentConfigBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.wpmFlow.observe(viewLifecycleOwner) { value ->
            Log.i("ConfigFragment", "wpm changed $value")
            binding.textViewWPM.text = value.toString()
        }
        viewModel.farnsworthWpmFlow.observe(viewLifecycleOwner) { value ->
            Log.i("ConfigFragment", "farnsworthWpmFlow changed $value")
            binding.textViewFarnsworthWPM.text = value.toString()
        }
        viewModel.frequencyFlow.observe(viewLifecycleOwner) { value ->
            Log.i("ConfigFragment", "frequencyFlow changed $value")
            binding.textViewFrequency.text = value.toString()
        }

        // Button bindings
        binding.buttonIncreaseWPM.setOnClickListener { viewModel.increaseWpm() }
        binding.buttonDecreaseWPM.setOnClickListener { viewModel.decreaseWpm() }
        binding.buttonIncreaseFrequency.setOnClickListener { viewModel.increaseFrequency() }
        binding.buttonDecreaseFrequency.setOnClickListener { viewModel.decreaseFrequency() }
        binding.buttonIncreaseFarnsworthWPM.setOnClickListener { viewModel.increaseFarnsworthWpm() }
        binding.buttonDecreaseFarnsworthWPM.setOnClickListener { viewModel.decreaseFarnsworthWpm() }
    }
}
