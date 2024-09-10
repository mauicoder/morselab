package net.maui.morselab

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.io.PrintWriter

class ShareFile {

    private val FILE_PROVIDER_AUTHORITY = "net.maui.morselab.provider"
    private val FILE_PROVIDER_NAME = "shared_data.wav"

    fun shareAsFile(activity: FragmentActivity, waveStream: ByteArray) {
        val file = File(activity.cacheDir, FILE_PROVIDER_NAME)

        file.updateText(waveStream)

        val uri = FileProvider.getUriForFile(
            activity.applicationContext,
            FILE_PROVIDER_AUTHORITY,
            file
        )

        Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, uri)
        }.also { intent ->
            activity.startActivity(Intent.createChooser(intent, "Share Sound File"))
        }
    }

    private fun File.clearText() {
        PrintWriter(this).also {
            it.print("")
            it.close()
        }
    }

    private fun File.updateText(content: ByteArray) {
        clearText()
        appendBytes(content)
    }

}