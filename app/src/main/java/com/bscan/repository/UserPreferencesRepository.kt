package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.ui.components.MaterialDisplayMode
import com.bscan.logic.WeightUnit
import com.bscan.utils.DeviceCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for managing user preferences
 */
class UserPreferencesRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    
    private val deviceCapabilities = DeviceCapabilities(context)
    
    /**
     * Get the current material display mode preference
     */
    fun getMaterialDisplayMode(): MaterialDisplayMode {
        val modeName = sharedPreferences.getString(MATERIAL_DISPLAY_MODE_KEY, MaterialDisplayMode.SHAPES.name)
        return try {
            MaterialDisplayMode.valueOf(modeName ?: MaterialDisplayMode.SHAPES.name)
        } catch (e: IllegalArgumentException) {
            MaterialDisplayMode.SHAPES // Default fallback
        }
    }
    
    /**
     * Set the material display mode preference
     */
    suspend fun setMaterialDisplayMode(mode: MaterialDisplayMode) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(MATERIAL_DISPLAY_MODE_KEY, mode.name)
            .apply()
    }
    
    // === Weight Management Preferences ===
    
    /**
     * Get the preferred weight unit for display
     */
    fun getWeightUnit(): WeightUnit {
        val unitName = sharedPreferences.getString(WEIGHT_UNIT_KEY, WeightUnit.GRAMS.name)
        return try {
            WeightUnit.valueOf(unitName ?: WeightUnit.GRAMS.name)
        } catch (e: IllegalArgumentException) {
            WeightUnit.GRAMS // Default fallback
        }
    }
    
    /**
     * Set the preferred weight unit
     */
    suspend fun setWeightUnit(unit: WeightUnit) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(WEIGHT_UNIT_KEY, unit.name)
            .apply()
    }
    
    /**
     * Get the weight measurement tolerance percentage
     */
    fun getWeightTolerance(): Float {
        return sharedPreferences.getFloat(WEIGHT_TOLERANCE_KEY, DEFAULT_WEIGHT_TOLERANCE)
    }
    
    /**
     * Set the weight measurement tolerance percentage
     */
    suspend fun setWeightTolerance(tolerancePercent: Float) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putFloat(WEIGHT_TOLERANCE_KEY, tolerancePercent.coerceIn(0f, 50f))
            .apply()
    }
    
    /**
     * Get whether to show weight suggestions automatically
     */
    fun getShowWeightSuggestions(): Boolean {
        return sharedPreferences.getBoolean(SHOW_WEIGHT_SUGGESTIONS_KEY, true)
    }
    
    /**
     * Set whether to show weight suggestions automatically
     */
    suspend fun setShowWeightSuggestions(show: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean(SHOW_WEIGHT_SUGGESTIONS_KEY, show)
            .apply()
    }
    
    /**
     * Get the default spool configuration ID for new measurements
     */
    fun getDefaultSpoolConfigurationId(): String? {
        return sharedPreferences.getString(DEFAULT_SPOOL_CONFIG_KEY, null)
    }
    
    /**
     * Set the default spool configuration ID for new measurements
     */
    suspend fun setDefaultSpoolConfigurationId(configurationId: String?) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(DEFAULT_SPOOL_CONFIG_KEY, configurationId)
            .apply()
    }
    
    // === BLE Scales Preferences ===
    
    /**
     * Get the preferred BLE scale device address
     */
    fun getPreferredScaleAddress(): String? {
        return sharedPreferences.getString(PREFERRED_SCALE_ADDRESS_KEY, null)
    }
    
    /**
     * Set the preferred BLE scale device address
     */
    suspend fun setPreferredScaleAddress(address: String?) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(PREFERRED_SCALE_ADDRESS_KEY, address)
            .apply()
    }
    
    /**
     * Get the preferred BLE scale device name for display
     */
    fun getPreferredScaleName(): String? {
        return sharedPreferences.getString(PREFERRED_SCALE_NAME_KEY, null)
    }
    
    /**
     * Set the preferred BLE scale device name for display
     */
    suspend fun setPreferredScaleName(name: String?) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putString(PREFERRED_SCALE_NAME_KEY, name)
            .apply()
    }
    
    /**
     * Check if BLE scales are enabled
     */
    fun isBleScalesEnabled(): Boolean {
        return sharedPreferences.getBoolean(BLE_SCALES_ENABLED_KEY, false)
    }
    
    /**
     * Set BLE scales enabled state
     */
    suspend fun setBleScalesEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean(BLE_SCALES_ENABLED_KEY, enabled)
            .apply()
    }
    
    /**
     * Check if auto-connect to BLE scales is enabled
     */
    fun isBleScalesAutoConnectEnabled(): Boolean {
        return sharedPreferences.getBoolean(BLE_SCALES_AUTO_CONNECT_KEY, true)
    }
    
    /**
     * Set BLE scales auto-connect preference
     */
    suspend fun setBleScalesAutoConnectEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean(BLE_SCALES_AUTO_CONNECT_KEY, enabled)
            .apply()
    }
    
    /**
     * Clear BLE scales configuration (disconnect)
     */
    fun clearBleScalesConfiguration() {
        sharedPreferences.edit()
            .remove(PREFERRED_SCALE_ADDRESS_KEY)
            .remove(PREFERRED_SCALE_NAME_KEY)
            .putBoolean(BLE_SCALES_ENABLED_KEY, false)
            .apply()
    }
    
    // === Visual Effects Preferences ===
    
    /**
     * Check if accelerometer-based effects are enabled
     * Uses smart defaults based on device performance
     */
    fun isAccelerometerEffectsEnabled(): Boolean {
        // If user has never set this preference, use smart default
        if (!sharedPreferences.contains(ACCELEROMETER_EFFECTS_KEY)) {
            val defaultValue = getAccelerometerEffectsDefault()
            // Cache the smart default so it's consistent
            sharedPreferences.edit()
                .putBoolean(ACCELEROMETER_EFFECTS_KEY, defaultValue)
                .putBoolean(ACCELEROMETER_EFFECTS_AUTO_SET_KEY, true)
                .apply()
            return defaultValue
        }
        
        return sharedPreferences.getBoolean(ACCELEROMETER_EFFECTS_KEY, getAccelerometerEffectsDefault())
    }
    
    /**
     * Set accelerometer effects enabled state (marks as user-set)
     */
    suspend fun setAccelerometerEffectsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putBoolean(ACCELEROMETER_EFFECTS_KEY, enabled)
            .putBoolean(ACCELEROMETER_EFFECTS_AUTO_SET_KEY, false) // User manually changed it
            .apply()
    }
    
    /**
     * Check if accelerometer effects were automatically disabled due to performance
     */
    fun wasAccelerometerEffectsAutoDisabled(): Boolean {
        return sharedPreferences.getBoolean(ACCELEROMETER_EFFECTS_AUTO_SET_KEY, false) &&
                !isAccelerometerEffectsEnabled()
    }
    
    /**
     * Get device performance info for accelerometer effects
     */
    fun getAccelerometerEffectsDeviceInfo(): String {
        return if (deviceCapabilities.shouldDisableAccelerometerEffects()) {
            deviceCapabilities.getDisableReason()
        } else {
            "Device supports motion effects"
        }
    }
    
    /**
     * Get motion sensitivity setting (0.1 = subtle, 1.0 = maximum)
     */
    fun getMotionSensitivity(): Float {
        return sharedPreferences.getFloat(MOTION_SENSITIVITY_KEY, DEFAULT_MOTION_SENSITIVITY)
    }
    
    /**
     * Set motion sensitivity setting
     */
    suspend fun setMotionSensitivity(sensitivity: Float) = withContext(Dispatchers.IO) {
        val clampedSensitivity = sensitivity.coerceIn(MIN_MOTION_SENSITIVITY, MAX_MOTION_SENSITIVITY)
        sharedPreferences.edit()
            .putFloat(MOTION_SENSITIVITY_KEY, clampedSensitivity)
            .apply()
    }
    
    /**
     * Get the smart default for accelerometer effects based on device performance
     */
    private fun getAccelerometerEffectsDefault(): Boolean {
        return !deviceCapabilities.shouldDisableAccelerometerEffects()
    }
    
    companion object {
        private const val MATERIAL_DISPLAY_MODE_KEY = "material_display_mode"
        
        // Weight management keys
        private const val WEIGHT_UNIT_KEY = "weight_unit"
        private const val WEIGHT_TOLERANCE_KEY = "weight_tolerance"
        private const val SHOW_WEIGHT_SUGGESTIONS_KEY = "show_weight_suggestions"
        private const val DEFAULT_SPOOL_CONFIG_KEY = "default_spool_configuration"
        
        // BLE scales keys
        private const val PREFERRED_SCALE_ADDRESS_KEY = "preferred_scale_address"
        private const val PREFERRED_SCALE_NAME_KEY = "preferred_scale_name"
        private const val BLE_SCALES_ENABLED_KEY = "ble_scales_enabled"
        private const val BLE_SCALES_AUTO_CONNECT_KEY = "ble_scales_auto_connect"
        
        // Visual effects keys
        private const val ACCELEROMETER_EFFECTS_KEY = "accelerometer_effects_enabled"
        private const val ACCELEROMETER_EFFECTS_AUTO_SET_KEY = "accelerometer_effects_auto_set"
        private const val MOTION_SENSITIVITY_KEY = "motion_sensitivity"
        
        private const val DEFAULT_WEIGHT_TOLERANCE = 5f // 5% tolerance
        
        // Motion sensitivity constants
        private const val DEFAULT_MOTION_SENSITIVITY = 0.7f // Balanced default
        private const val MIN_MOTION_SENSITIVITY = 0.1f     // Very subtle
        private const val MAX_MOTION_SENSITIVITY = 1.0f     // Maximum movement
    }
}