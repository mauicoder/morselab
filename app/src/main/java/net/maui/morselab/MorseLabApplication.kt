package net.maui.morselab

import android.app.Application
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MorseLabApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply Dynamic Colors (Material 3) to all activities if available (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}