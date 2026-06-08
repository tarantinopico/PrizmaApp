package com.example.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.example.PrizmaApp

class HapticHelper(private val context: Context, private val enabled: Boolean, private val intensity: String) {
    
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private fun getAmplitude(base: Int): Int {
        return when (intensity) {
            "jemná" -> (base * 0.5f).toInt().coerceIn(1, 255)
            "silná" -> (base * 1.5f).toInt().coerceIn(1, 255)
            else -> base
        }
    }

    fun perform(type: HapticType) {
        if (!enabled || !vibrator.hasVibrator()) return
        
        try {
            val effect = when (type) {
                HapticType.LONG_PRESS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                    } else {
                        VibrationEffect.createOneShot(50, getAmplitude(255))
                    }
                }
                HapticType.SWITCH_PROFILE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 20, 50, 20),
                            intArrayOf(0, getAmplitude(150), 0, getAmplitude(200)),
                            -1
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        VibrationEffect.createWaveform(longArrayOf(0, 20, 50, 20), -1)
                    }
                }
                HapticType.TAB_ACTION -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                    } else {
                        VibrationEffect.createOneShot(20, getAmplitude(100))
                    }
                }
                HapticType.DOWNLOAD_COMPLETE -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 30, 80, 50),
                            intArrayOf(0, getAmplitude(255), 0, getAmplitude(150)),
                            -1
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        VibrationEffect.createWaveform(longArrayOf(0, 30, 80, 50), -1)
                    }
                }
                HapticType.DRAG_DROP -> {
                     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    } else {
                        VibrationEffect.createOneShot(10, getAmplitude(50))
                    }
                }
            }
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            // Ignore
        }
    }
}

enum class HapticType {
    LONG_PRESS,
    SWITCH_PROFILE,
    TAB_ACTION,
    DOWNLOAD_COMPLETE,
    DRAG_DROP
}

@Composable
fun rememberHapticHelper(): HapticHelper {
    val context = LocalContext.current
    val app = context.applicationContext as PrizmaApp
    val enabled by app.container.settingsDataStore.hapticsEnabled.collectAsState(initial = true)
    val intensity by app.container.settingsDataStore.hapticsIntensity.collectAsState(initial = "střední")

    return remember(enabled, intensity) {
        HapticHelper(context, enabled, intensity)
    }
}
