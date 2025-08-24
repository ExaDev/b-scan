package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.ui.components.MaterialDisplayMode

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
    
    // BLE Scales Preferences
    
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
        private const val PREFERRED_SCALE_ADDRESS_KEY = "preferred_scale_address"
        private const val PREFERRED_SCALE_NAME_KEY = "preferred_scale_name"
        private const val BLE_SCALES_ENABLED_KEY = "ble_scales_enabled"
        private const val BLE_SCALES_AUTO_CONNECT_KEY = "ble_scales_auto_connect"
    }
}