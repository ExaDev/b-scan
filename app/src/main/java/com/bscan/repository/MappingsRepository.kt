package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bscan.model.FilamentMappings
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing updatable filament mappings.
 * These mappings are used by FilamentInterpreter to convert raw scan data 
 * into meaningful information at runtime.
 */
class MappingsRepository(private val context: Context) {
    
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
     * Get current filament mappings, loading from assets on first run
     */
    fun getCurrentMappings(): FilamentMappings {
        val mappingsJson = sharedPreferences.getString("mappings", null)
        
        return if (mappingsJson != null) {
            try {
                gson.fromJson(mappingsJson, FilamentMappings::class.java)
                    ?: loadFromAssetsOrDefault()
            } catch (e: JsonSyntaxException) {
                Log.w(TAG, "Corrupted mappings data, loading from assets", e)
                // If data is corrupted, try to load from assets
                loadFromAssetsOrDefault()
            }
        } else {
            // First run - load from bundled assets
            loadFromAssetsOrDefault()
        }
    }
    
    /**
     * Load mappings from bundled assets file, with fallback to defaults
     */
    private fun loadFromAssetsOrDefault(): FilamentMappings {
        return try {
            // Use the stored context
            val assetsInputStream = context.assets.open("filament_mappings.json")
            val jsonString = assetsInputStream.bufferedReader().use { it.readText() }
            
            // Parse the tools format JSON
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            
            val mappings = FilamentMappings(
                version = jsonObject.get("version")?.asInt ?: 1,
                lastUpdated = LocalDateTime.now(),
                colorMappings = parseMapFromJson(jsonObject.getAsJsonObject("colorMappings")),
                materialMappings = parseMapFromJson(jsonObject.getAsJsonObject("materialMappings")),
                brandMappings = parseMapFromJson(jsonObject.getAsJsonObject("brandMappings")),
                temperatureMappings = parseTemperatureMappings(jsonObject.getAsJsonObject("temperatureMappings"))
            )
            
            // Save to preferences for future use
            saveMappings(mappings)
            
            Log.i(TAG, "Loaded mappings from assets: version ${mappings.version}")
            mappings
            
        } catch (e: IOException) {
            Log.w(TAG, "Failed to load from assets, using defaults", e)
            FilamentMappings()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing assets mappings, using defaults", e)
            FilamentMappings()
        }
    }
    
    private fun parseMapFromJson(jsonObject: JsonObject?): Map<String, String> {
        if (jsonObject == null) return emptyMap()
        return jsonObject.entrySet().associate { (key, value) ->
            key to value.asString
        }
    }
    
    private fun parseTemperatureMappings(jsonObject: JsonObject?): Map<String, com.bscan.model.TemperatureRange> {
        if (jsonObject == null) return emptyMap()
        return jsonObject.entrySet().associate { (key, value) ->
            val tempObj = value.asJsonObject
            key to com.bscan.model.TemperatureRange(
                minNozzle = tempObj.get("minNozzle").asInt,
                maxNozzle = tempObj.get("maxNozzle").asInt,
                bed = tempObj.get("bed").asInt
            )
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
    
    companion object {
        private const val TAG = "MappingsRepository"
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