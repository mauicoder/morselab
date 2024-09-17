package net.maui.morselab.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.R
import net.maui.morselab.databinding.FragmentPlayTextBinding
import net.maui.morselab.viewmodel.PlayTextViewModel
import javax.inject.Inject


@AndroidEntryPoint
class PlayTextFragment @Inject constructor(): Fragment() {

    private val viewModel: PlayTextViewModel by viewModels()
    private var _binding: FragmentPlayTextBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate<FragmentPlayTextBinding>(
            inflater, R.layout.fragment_play_text, container, false);
        val binding : FragmentPlayTextBinding = _binding!!

        binding.buttonPlay.setOnClickListener {viewModel.playMorseCallback()
        }
        binding.buttonExport.setOnClickListener { viewModel.exportAsWave(activity = requireActivity()) }

        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.wpmFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int){
                Log.i("ConfigFragment", "wpm changed $value")
            }
        })
        viewModel.farnsworthWpmFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int){
                Log.i("ConfigFragment", "farnsworthWpmFlow changed $value")
            }
        })
        viewModel.frequencyFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int){
                Log.i("ConfigFragment", "frequencyFlow changed $value")
            }
        })
        binding.executePendingBindings()
        return binding.root
    }

}