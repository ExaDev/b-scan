package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.FilamentMappings
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing updatable filament mappings.
 * These mappings are used by FilamentInterpreter to convert raw scan data 
 * into meaningful information at runtime.
 */
class MappingsRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("filament_mappings", Context.MODE_PRIVATE)
    
    // Custom LocalDateTime adapter for Gson
    private val localDateTimeAdapter = object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
            } catch (e: Exception) {
                LocalDateTime.now() // Fallback to current time
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .create()
    
    /**
     * Get current filament mappings, or default if none exist
     */
    fun getCurrentMappings(): FilamentMappings {
        val mappingsJson = sharedPreferences.getString("mappings", null)
        
        return if (mappingsJson != null) {
            try {
                gson.fromJson(mappingsJson, FilamentMappings::class.java)
                    ?: FilamentMappings() // Fallback to default
            } catch (e: JsonSyntaxException) {
                // If data is corrupted, return defaults and clear storage
                clearMappings()
                FilamentMappings()
            }
        } else {
            FilamentMappings() // Use default mappings
        }
    }
    
    /**
     * Save updated mappings
     */
    fun saveMappings(mappings: FilamentMappings) {
        val updatedMappings = mappings.copy(
            lastUpdated = LocalDateTime.now(),
            version = mappings.version + 1
        )
        
        val mappingsJson = gson.toJson(updatedMappings)
        sharedPreferences.edit()
            .putString("mappings", mappingsJson)
            .apply()
    }
    
    /**
     * Update color mappings only
     */
    fun updateColorMappings(colorMappings: Map<String, String>) {
        val current = getCurrentMappings()
        val updated = current.copy(colorMappings = colorMappings)
        saveMappings(updated)
    }
    
    /**
     * Add new color mapping
     */
    fun addColorMapping(hex: String, name: String) {
        val current = getCurrentMappings()
        val updatedColors = current.colorMappings.toMutableMap()
        updatedColors[hex.uppercase()] = name
        updateColorMappings(updatedColors)
    }
    
    /**
     * Update material mappings only
     */
    fun updateMaterialMappings(materialMappings: Map<String, String>) {
        val current = getCurrentMappings()
        val updated = current.copy(materialMappings = materialMappings)
        saveMappings(updated)
    }
    
    /**
     * Add new material mapping
     */
    fun addMaterialMapping(code: String, name: String) {
        val current = getCurrentMappings()
        val updatedMaterials = current.materialMappings.toMutableMap()
        updatedMaterials[code.uppercase()] = name
        updateMaterialMappings(updatedMaterials)
    }
    
    /**
     * Reset mappings to defaults
     */
    fun resetToDefaults() {
        saveMappings(FilamentMappings())
    }
    
    /**
     * Clear all mappings
     */
    fun clearMappings() {
        sharedPreferences.edit()
            .remove("mappings")
            .apply()
    }
    
    /**
     * Get mapping version for cache invalidation
     */
    fun getMappingsVersion(): Int {
        return getCurrentMappings().version
    }
    
    /**
     * Check if mappings have been updated since given version
     */
    fun hasUpdatedMappings(sinceVersion: Int): Boolean {
        return getCurrentMappings().version > sinceVersion
    }
    
    /**
     * Export mappings as JSON string for backup/sharing
     */
    fun exportMappings(): String {
        return gson.toJson(getCurrentMappings())
    }
    
    /**
     * Import mappings from JSON string
     */
    fun importMappings(json: String): Boolean {
        return try {
            val mappings = gson.fromJson(json, FilamentMappings::class.java)
            if (mappings != null) {
                saveMappings(mappings)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get statistics about current mappings
     */
    fun getMappingsStats(): MappingsStats {
        val mappings = getCurrentMappings()
        return MappingsStats(
            version = mappings.version,
            lastUpdated = mappings.lastUpdated,
            colorMappingCount = mappings.colorMappings.size,
            materialMappingCount = mappings.materialMappings.size,
            brandMappingCount = mappings.brandMappings.size,
            temperatureMappingCount = mappings.temperatureMappings.size
        )
    }
}

/**
 * Statistics about current mappings
 */
data class MappingsStats(
    val version: Int,
    val lastUpdated: LocalDateTime,
    val colorMappingCount: Int,
    val materialMappingCount: Int,
    val brandMappingCount: Int,
    val temperatureMappingCount: Int
)