package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.ui.components.MaterialDisplayMode
import com.bscan.logic.WeightUnit

/**
 * Repository for managing user preferences
 */
class UserPreferencesRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
    
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
    fun setMaterialDisplayMode(mode: MaterialDisplayMode) {
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
    fun setWeightUnit(unit: WeightUnit) {
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
    fun setWeightTolerance(tolerancePercent: Float) {
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
    fun setShowWeightSuggestions(show: Boolean) {
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
    fun setDefaultSpoolConfigurationId(configurationId: String?) {
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
    fun setPreferredScaleAddress(address: String?) {
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
    fun setPreferredScaleName(name: String?) {
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
    fun setBleScalesEnabled(enabled: Boolean) {
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
    fun setBleScalesAutoConnectEnabled(enabled: Boolean) {
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
        
        private const val DEFAULT_WEIGHT_TOLERANCE = 5f // 5% tolerance
    }
}