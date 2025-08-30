package com.bscan.nfc.handlers

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

class HapticFeedbackProvider(private val context: Context) {
    
    fun provideFeedback() {
        try {
            val vibrator = getVibrator()
            
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26+ - Use VibrationEffect
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    it.vibrate(effect)
                } else {
                    // API < 26 - Use legacy vibrate method
                    @Suppress("DEPRECATION")
                    it.vibrate(100)
                }
            }
        } catch (e: Exception) {
            // Haptic feedback failed, continue silently
        }
    }
    
    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ - Use VibratorManager
            val vibratorManager = context.getSystemService<VibratorManager>()
            vibratorManager?.defaultVibrator
        } else {
            // API < 31 - Use legacy Vibrator service
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
}