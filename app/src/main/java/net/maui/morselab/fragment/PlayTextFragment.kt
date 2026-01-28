package net.maui.morselab.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import net.maui.morselab.R
import net.maui.morselab.databinding.FragmentPlayTextBinding
import net.maui.morselab.viewmodel.PlayTextViewModel

@AndroidEntryPoint
class PlayTextFragment @Inject constructor(): Fragment() {

    private val viewModel: PlayTextViewModel by viewModels()
    private lateinit var binding: FragmentPlayTextBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_play_text, container, false)
        binding.viewmodel = viewModel
        binding.lifecycleOwner = this

        binding.saveButton.setOnClickListener {
            viewModel.saveMorseAsWaveFile(requireContext())
        }

        binding.shareButton.setOnClickListener {
            viewModel.shareMorseAsWaveFile(requireActivity())
        }

        return binding.root
    }
}