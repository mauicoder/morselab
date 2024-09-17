package net.maui.morselab.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.R
import net.maui.morselab.databinding.FragmentConfigBinding
import net.maui.morselab.viewmodel.UserPreferencesViewModel
import javax.inject.Inject


@AndroidEntryPoint
class ConfigFragment @Inject constructor() : Fragment() {

    private val viewModel: UserPreferencesViewModel by activityViewModels()
    private var _binding: FragmentConfigBinding? = null


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
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_config, container, false)
        val binding = _binding!!
        binding.viewmodel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        viewModel.wpmFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int) {
                Log.i("ConfigFragment", "wpm changed $value")
            }
        })
        viewModel.farnsworthWpmFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int) {
                Log.i("ConfigFragment", "farnsworthWpmFlow changed $value")
            }
        })
        viewModel.frequencyFlow.observe(viewLifecycleOwner, Observer<Int> {
            @Override
            fun onChanged(value: Int) {
                Log.i("ConfigFragment", "frequencyFlow changed $value")
            }
        })

        // Button bindings
        binding.buttonIncreaseWPM.setOnClickListener { viewModel.increaseWpm() }
        binding.buttonDecreaseWPM.setOnClickListener { viewModel.decreaseWpm() }
        binding.buttonIncreaseFrequency.setOnClickListener { viewModel.increaseFrequency() }
        binding.buttonDecreaseFrequency.setOnClickListener { viewModel.decreaseFrequency() }
        binding.buttonIncreaseFarnsworthWPM.setOnClickListener { viewModel.increaseFarnsworthWpm() }
        binding.buttonDecreaseFarnsworthWPM.setOnClickListener { viewModel.decreaseFarnsworthWpm() }


        binding.executePendingBindings()
        return binding.root
    }
}