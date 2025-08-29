package com.bscan.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.sensor.AccelerometerManager
import com.bscan.sensor.TiltAngles
import com.bscan.utils.DeviceCapabilities

/**
 * Remembers accelerometer state that provides real-time tilt information
 * 
 * @param enabled Whether accelerometer effects are enabled by user preference
 * @return TiltAngles containing X and Y tilt values in degrees
 */
@Composable
fun rememberAccelerometerState(enabled: Boolean): TiltAngles {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Check device capabilities to prevent enabling on low-performance devices
    val deviceCapabilities = remember { DeviceCapabilities(context) }
    val deviceSupportsEffects = remember { !deviceCapabilities.shouldDisableAccelerometerEffects() }
    
    // Create and remember the accelerometer manager
    val accelerometerManager = remember {
        AccelerometerManager(context)
    }
    
    // Only enable if both user preference and device capabilities allow it
    val shouldEnable = enabled && deviceSupportsEffects
    
    // Collect tilt angles from the manager's StateFlow
    val tiltAngles by accelerometerManager.tiltAngles.collectAsStateWithLifecycle()
    
    // Handle lifecycle events to start/stop sensor listening
    DisposableEffect(lifecycleOwner, shouldEnable) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (shouldEnable && accelerometerManager.isAvailable()) {
                        accelerometerManager.startListening()
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    accelerometerManager.stopListening()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            accelerometerManager.stopListening()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Return static angles if disabled, device incapable, or sensor not available
    return if (shouldEnable && accelerometerManager.isAvailable()) {
        tiltAngles
    } else {
        TiltAngles(0f, 0f)
    }
}

/**
 * Converts tilt angles to position offsets for visual effects
 * 
 * @param tiltAngles Current device tilt angles
 * @param sensitivity Multiplier for tilt sensitivity (0.1 to 1.0)
 * @return Pair of X and Y position offsets (-1.0 to 1.0)
 */
fun tiltToPositionOffset(tiltAngles: TiltAngles, sensitivity: Float): Pair<Float, Float> {
    // Map tilt angles (-90° to +90°) to position offsets (-1.0 to 1.0)
    // Apply sensitivity to reduce movement range
    val x = (tiltAngles.x / 90f) * sensitivity
    val y = -(tiltAngles.y / 90f) * sensitivity // Negative Y for natural feel
    
    return Pair(
        x.coerceIn(-1f, 1f),
        y.coerceIn(-1f, 1f)
    )
}