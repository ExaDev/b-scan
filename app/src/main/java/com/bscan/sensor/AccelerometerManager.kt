package com.bscan.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing tilt angles in degrees
 */
data class TiltAngles(
    val x: Float = 0f, // Roll: -90° (left) to +90° (right)
    val y: Float = 0f  // Pitch: -90° (forward) to +90° (backward)
)

/**
 * Manager for accelerometer sensor readings to provide device tilt information
 */
class AccelerometerManager(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private val _tiltAngles = MutableStateFlow(TiltAngles())
    val tiltAngles: StateFlow<TiltAngles> = _tiltAngles.asStateFlow()
    
    // Smoothing filter parameters
    private var filteredX = 0f
    private var filteredY = 0f
    private var filteredZ = 0f
    private val alpha = 0.8f // Low-pass filter coefficient
    
    private var isRegistered = false
    
    /**
     * Start listening to accelerometer sensor
     */
    fun startListening() {
        if (accelerometer != null && !isRegistered) {
            Log.i(TAG, "Starting accelerometer listening")
            logDevicePerformanceInfo()
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI // 60Hz refresh rate
            )
            isRegistered = true
        } else if (accelerometer == null) {
            Log.w(TAG, "Accelerometer sensor not available")
        } else {
            Log.d(TAG, "Accelerometer already registered")
        }
    }
    
    /**
     * Stop listening to accelerometer sensor
     */
    fun stopListening() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
        }
    }
    
    /**
     * Check if accelerometer sensor is available
     */
    fun isAvailable(): Boolean = accelerometer != null
    
    private var sampleCount = 0
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // Apply low-pass filter to smooth out noise
                filteredX = alpha * filteredX + (1 - alpha) * sensorEvent.values[0]
                filteredY = alpha * filteredY + (1 - alpha) * sensorEvent.values[1]
                filteredZ = alpha * filteredZ + (1 - alpha) * sensorEvent.values[2]
                
                // Calculate tilt angles using accelerometer data
                val tiltX = calculateTiltX(filteredX, filteredY, filteredZ)
                val tiltY = calculateTiltY(filteredX, filteredY, filteredZ)
                
                _tiltAngles.value = TiltAngles(tiltX, tiltY)
                
                // Debug logging every 60 samples (about once per second at 60Hz)
                sampleCount++
                if (sampleCount % 60 == 0) {
                    Log.d(TAG, "Tilt angles: X=${tiltX.toInt()}°, Y=${tiltY.toInt()}°")
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for accelerometer
    }
    
    /**
     * Calculate roll (X-axis tilt) in degrees
     */
    private fun calculateTiltX(x: Float, y: Float, z: Float): Float {
        val angle = Math.atan2(x.toDouble(), Math.sqrt((y * y + z * z).toDouble()))
        return Math.toDegrees(angle).toFloat().coerceIn(-90f, 90f)
    }
    
    /**
     * Calculate pitch (Y-axis tilt) in degrees
     */
    private fun calculateTiltY(x: Float, y: Float, z: Float): Float {
        val angle = Math.atan2(y.toDouble(), Math.sqrt((x * x + z * z).toDouble()))
        return Math.toDegrees(angle).toFloat().coerceIn(-90f, 90f)
    }
    
    /**
     * Log device performance information for debugging
     */
    private fun logDevicePerformanceInfo() {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = (runtime.maxMemory() / (1024 * 1024)).toInt()
        val usedMemoryMB = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()
        val freeMemoryMB = (runtime.freeMemory() / (1024 * 1024)).toInt()
        
        Log.i(TAG, "Performance info:")
        Log.i(TAG, "  Memory: ${usedMemoryMB}MB used, ${freeMemoryMB}MB free, ${maxMemoryMB}MB max")
        Log.i(TAG, "  CPU cores: ${runtime.availableProcessors()}")
        Log.i(TAG, "  Sensor: ${accelerometer?.name} (vendor: ${accelerometer?.vendor})")
        Log.i(TAG, "  Max delay: ${accelerometer?.maxDelay}μs, Min delay: ${accelerometer?.minDelay}μs")
    }
    
    companion object {
        private const val TAG = "AccelerometerManager"
    }
}