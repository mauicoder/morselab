package net.maui.morselab

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import net.maui.morselab.viewmodel.DecodeFileViewModel

@AndroidEntryPoint
class DecodeFileActivity : AppCompatActivity() {

    private val viewModel: DecodeFileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decode_file)

        val decodedTextView: TextView = findViewById(R.id.decodedTextView)
        val decodeButton: Button = findViewById(R.id.decodeButton)

        // Observe the LiveData from the ViewModel
        viewModel.decodedText.observe(this) {
            decodedTextView.text = it
        }

        viewModel.isDecoding.observe(this) {
            decodeButton.isEnabled = !it
            decodeButton.text = if(it) "Decoding..." else "Decode File"
        }

        // Get the URI from the intent that started this activity
        intent?.data?.let {
            viewModel.onFileSelected(it)
        }

        decodeButton.setOnClickListener {
            viewModel.startDecoding(applicationContext, playAudio = false)
        }
    }
}
