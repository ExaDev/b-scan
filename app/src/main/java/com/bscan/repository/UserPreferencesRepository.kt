package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.ui.components.MaterialDisplayMode

/**
 * Temporary stub for UserPreferencesRepository to fix compilation errors
 * TODO: Implement proper user preferences management
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    
    fun getMaterialDisplayMode(): MaterialDisplayMode {
        val mode = sharedPreferences.getString("material_display_mode", "SHAPES")
        return try {
            MaterialDisplayMode.valueOf(mode ?: "SHAPES")
        } catch (e: IllegalArgumentException) {
            MaterialDisplayMode.SHAPES
        }
    }
    
    fun setMaterialDisplayMode(mode: MaterialDisplayMode) {
        sharedPreferences.edit()
            .putString("material_display_mode", mode.name)
            .apply()
    }
    
    fun isAccelerometerEffectsEnabled(): Boolean {
        return sharedPreferences.getBoolean("accelerometer_effects", true)
    }
    
    fun setAccelerometerEffectsEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("accelerometer_effects", enabled)
            .apply()
    }
    
    fun getMotionSensitivity(): Float {
        return sharedPreferences.getFloat("motion_sensitivity", 0.5f)
    }
    
    fun setMotionSensitivity(sensitivity: Float) {
        sharedPreferences.edit()
            .putFloat("motion_sensitivity", sensitivity)
            .apply()
    }
    
    // BLE scales support stubs
    fun isBleScalesEnabled(): Boolean {
        return sharedPreferences.getBoolean("ble_scales_enabled", false)
    }
    
    fun setBleScalesEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("ble_scales_enabled", enabled)
            .apply()
    }
    
    fun getPreferredScaleName(): String? {
        return sharedPreferences.getString("preferred_scale_name", null)
    }
    
    fun setPreferredScaleName(name: String?) {
        sharedPreferences.edit()
            .putString("preferred_scale_name", name)
            .apply()
    }
    
    fun isBleScalesAutoConnectEnabled(): Boolean {
        return sharedPreferences.getBoolean("ble_scales_auto_connect", true)
    }
    
    fun setBleScalesAutoConnectEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("ble_scales_auto_connect", enabled)
            .apply()
    }
    
    fun getPreferredScaleAddress(): String? {
        return sharedPreferences.getString("preferred_scale_address", null)
    }
    
    fun setPreferredScaleAddress(address: String?) {
        sharedPreferences.edit()
            .putString("preferred_scale_address", address)
            .apply()
    }
    
    /**
     * Clear all BLE scale configuration settings
     */
    fun clearBleScalesConfiguration() {
        sharedPreferences.edit()
            .remove("ble_scales_enabled")
            .remove("preferred_scale_name")
            .remove("preferred_scale_address")
            .remove("ble_scales_auto_connect")
            .apply()
    }
    
    /**
     * Check if accelerometer effects were auto-disabled due to performance constraints
     */
    fun wasAccelerometerEffectsAutoDisabled(): Boolean {
        return sharedPreferences.getBoolean("accelerometer_effects_auto_disabled", false)
    }
    
    /**
     * Set the auto-disabled flag for accelerometer effects
     */
    fun setAccelerometerEffectsAutoDisabled(autoDisabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean("accelerometer_effects_auto_disabled", autoDisabled)
            .apply()
    }
    
    /**
     * Get device information related to accelerometer effects capability
     */
    fun getAccelerometerEffectsDeviceInfo(): String {
        return sharedPreferences.getString("accelerometer_effects_device_info", "Device performance optimised")
            ?: "Device performance optimised"
    }
    
    /**
     * Set device information related to accelerometer effects capability
     */
    fun setAccelerometerEffectsDeviceInfo(deviceInfo: String) {
        sharedPreferences.edit()
            .putString("accelerometer_effects_device_info", deviceInfo)
            .apply()
    }
}