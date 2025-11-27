package net.maui.morselab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.databinding.ActivityMainBinding
import net.maui.morselab.fragment.ConfigFragment
import net.maui.morselab.fragment.MorseDecoderFragment
import net.maui.morselab.fragment.PlayTextFragment
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    @Inject
    lateinit var configFragment: ConfigFragment
    @Inject
    lateinit var playTextFragment: PlayTextFragment

    @Inject
    lateinit var decoderFragment: MorseDecoderFragment

    private fun replaceFragment(fragment: Fragment) : Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.flFragment, fragment).commit()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
              R.id.miPlay -> replaceFragment(playTextFragment)
              R.id.miDecode -> replaceFragment(decoderFragment)
              R.id.miConfig -> replaceFragment(configFragment)
                else -> false
            }
        }
        replaceFragment(playTextFragment) // place the home-fragment
    }

}
