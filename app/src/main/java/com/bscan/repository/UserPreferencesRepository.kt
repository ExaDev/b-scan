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
    
    companion object {
        private const val MATERIAL_DISPLAY_MODE_KEY = "material_display_mode"
    }
}