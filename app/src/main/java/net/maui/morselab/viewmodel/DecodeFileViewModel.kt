package net.maui.morselab.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.maui.morselab.decoder.MorseDecoder
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class DecodeFileViewModel @Inject constructor() : ViewModel() {

    private val _decodedText = MutableLiveData<String>()
    val decodedText: LiveData<String> = _decodedText

    private val _isDecoding = MutableLiveData<Boolean>()
    val isDecoding: LiveData<Boolean> = _isDecoding

    private var selectedFileUri: Uri? = null

    fun onFileSelected(uri: Uri) {
        selectedFileUri = uri
    }

    fun startDecoding(context: Context, playAudio: Boolean) {
        val uri = selectedFileUri ?: return
        _isDecoding.value = true
        _decodedText.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val decodedChars = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->

                    // Read and parse WAV header
                    val header = ByteArray(44)
                    val bytesReadHeader = inputStream.read(header, 0, 44)
                    if (bytesReadHeader < 44) throw IOException("Invalid WAV header")

                    val sampleRate = readLittleEndianInt(header, 24)
                    val numChannels = readLittleEndianShort(header, 22).toInt()
                    val bitsPerSample = readLittleEndianShort(header, 34).toInt()

                    Log.d("DecodeFile", "WAV Info: $sampleRate Hz, $bitsPerSample-bit, $numChannels-channel(s)")

                    if (numChannels != 1) {
                        _decodedText.postValue("Error: Only mono audio files are supported.")
                        return@launch
                    }

                    // Setup MorseDecoder
                    val decoder = MorseDecoder(
                        onDecoded = { char -> decodedChars.append(char) },
                        sampleRate = sampleRate
                    )

                    // Read and process audio data in chunks
                    val bufferSize = 1024 * 4
                    val byteBuffer = ByteArray(bufferSize)
                    var bytesRead: Int

                    while (inputStream.read(byteBuffer).also { bytesRead = it } != -1) {
                        val floatBuffer = convertToFloatArray(byteBuffer, bytesRead, bitsPerSample)
                        decoder.processBuffer(floatBuffer, floatBuffer.size)
                        _decodedText.postValue(decodedChars.toString()) // Update UI incrementally
                    }

                    decoder.flush()
                    _decodedText.postValue(decodedChars.toString()) // Post final result
                }
            } catch (e: Exception) {
                Log.e("DecodeFile", "Error decoding file", e)
                _decodedText.postValue("Error: ${e.message}")
            } finally {
                _isDecoding.postValue(false)
            }
        }
    }

    private fun readLittleEndianInt(data: ByteArray, offset: Int): Int {
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readLittleEndianShort(data: ByteArray, offset: Int): Short {
        return ((data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)).toShort()
    }

    private fun convertToFloatArray(byteBuffer: ByteArray, bytesRead: Int, bitsPerSample: Int): FloatArray {
        return when (bitsPerSample) {
            8 -> { // Unsigned 8-bit PCM
                val floatArray = FloatArray(bytesRead)
                for (i in 0 until bytesRead) {
                    floatArray[i] = ((byteBuffer[i].toInt() and 0xFF) - 128) / 128.0f
                }
                floatArray
            }
            16 -> { // Signed 16-bit PCM, little-endian
                val floatArray = FloatArray(bytesRead / 2)
                for (i in 0 until bytesRead step 2) {
                    val shortSample = ((byteBuffer[i].toInt() and 0xFF) or (byteBuffer[i + 1].toInt() shl 8)).toShort()
                    floatArray[i / 2] = shortSample / 32768.0f
                }
                floatArray
            }
            else -> throw IOException("Unsupported bit depth: $bitsPerSample")
        }
    }
}
