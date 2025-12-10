package net.maui.morselab.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DecodeFileViewModel @Inject constructor() : ViewModel() {

    private val _decodedText = MutableLiveData<String>()
    val decodedText: LiveData<String> = _decodedText

    private val _isDecoding = MutableLiveData<Boolean>()
    val isDecoding: LiveData<Boolean> = _isDecoding

    fun onFileSelected(uri: Uri) {
        // Handle the file URI, perhaps read file metadata or prepare for decoding
    }

    fun startDecoding(context: Context, playAudio: Boolean) {
        // Implement your decoding logic here
        _isDecoding.value = true
        _decodedText.value = "Starting the decoding process..." // Example text

        // After decoding is finished:
        // _isDecoding.value = false
        // _decodedText.value = "Your decoded text here"
    }
}
