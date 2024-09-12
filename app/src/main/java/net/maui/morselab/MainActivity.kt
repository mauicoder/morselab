package net.maui.morselab

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import androidx.fragment.app.Fragment
import net.maui.morselab.data.UserPreferencesSerializer
import net.maui.morselab.databinding.ActivityMainBinding
import net.maui.morselab.datastore.UserPreferences
import net.maui.morselab.fragment.ConfigFragment
import net.maui.morselab.fragment.PlayTextFragment

class MainActivity : AppCompatActivity() {

    private val DATA_STORE_FILE_NAME = "user_prefs.pb"
    private lateinit var binding: ActivityMainBinding

    private val Context.userPreferencesStore: DataStore<UserPreferences> by dataStore(
        fileName = DATA_STORE_FILE_NAME,
        serializer = UserPreferencesSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler(
            produceNewData = { UserPreferences.getDefaultInstance() }
        )
    )

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
