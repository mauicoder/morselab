package net.maui.morselab

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import net.maui.morselab.databinding.ActivityMainBinding
import net.maui.morselab.fragments.ConfigFragment
import net.maui.morselab.fragments.PlayTextFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    private fun replaceFragment(fragment: Fragment) : Boolean {
        supportFragmentManager.beginTransaction().replace(R.id.flFragment, fragment).commit()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val playTextFragment = PlayTextFragment()
        val configFragment = ConfigFragment()

        binding.bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
              R.id.miPlay -> replaceFragment(playTextFragment)
              R.id.miConfig -> replaceFragment(configFragment)
                else -> false
            }
        }
        replaceFragment(playTextFragment) // place the home-fragment
    }

}
