package net.maui.morselab.fragment

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import net.maui.morselab.R
import net.maui.morselab.viewmodel.UserPreferencesViewModel

class ConfigFragment : Fragment() {

    companion object {
        fun newInstance() = ConfigFragment()
    }

    private val viewModel: UserPreferencesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_config, container, false)
    }
}