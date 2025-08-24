package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bscan.model.FilamentMappings
import com.bscan.model.RfidMappings
import com.bscan.model.RfidMapping
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
    
    // Cached RFID mappings to avoid repeated asset loading
    private var _rfidMappings: RfidMappings? = null
    
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
            // First run or no cached data - load from bundled assets
            val assetsMappings = loadFromAssetsOrDefault()
            // Save to preferences for future use
            if (assetsMappings.productCatalog.isNotEmpty()) {
                saveMappings(assetsMappings)
            }
            assetsMappings
        }
    }
    
    /**
     * Load mappings from bundled assets file, with fallback to defaults
     * Note: With exact-only RFID matching, we use minimal FilamentMappings
     */
    private fun loadFromAssetsOrDefault(): FilamentMappings {
        Log.i(TAG, "Using minimal FilamentMappings for exact-only RFID matching")
        return FilamentMappings.empty()
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
     * Find products by color and material type from loaded product catalog
     */
    fun findProductsByColor(hex: String, materialType: String? = null): List<com.bscan.model.ProductEntry> {
        return getCurrentMappings().findProductsByColor(hex, materialType)
    }
    
    /**
     * Get RFID mappings, loading from assets if needed
     */
    fun getRfidMappings(): RfidMappings {
        if (_rfidMappings == null) {
            _rfidMappings = loadRfidMappingsFromAssets()
        }
        return _rfidMappings!!
    }
    
    /**
     * Look up exact SKU by RFID material and variant IDs
     */
    fun getSkuByRfidCode(materialId: String, variantId: String): String? {
        val rfidCode = "$materialId:$variantId"
        return getRfidMappings().getSkuByRfidCode(rfidCode)
    }
    
    /**
     * Get complete RFID mapping by material and variant IDs
     */
    fun getRfidMappingByCode(materialId: String, variantId: String): RfidMapping? {
        val rfidCode = "$materialId:$variantId"
        return getRfidMappings().getMappingByRfidCode(rfidCode)
    }
    
    /**
     * Check if exact RFID mapping exists
     */
    fun hasExactRfidMapping(materialId: String, variantId: String): Boolean {
        return getRfidMappings().hasExactMapping(materialId, variantId)
    }
    
    /**
     * Load RFID mappings from assets
     */
    private fun loadRfidMappingsFromAssets(): RfidMappings {
        return try {
            val assetsInputStream = context.assets.open("rfid_to_sku_mappings.json")
            val jsonString = assetsInputStream.bufferedReader().use { it.readText() }
            
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val mappingsObject = jsonObject.getAsJsonObject("rfidMappings")
            
            val mappings = mutableMapOf<String, RfidMapping>()
            mappingsObject?.entrySet()?.forEach { (rfidCode, mappingElement) ->
                try {
                    val mapping = mappingElement.asJsonObject
                    mappings[rfidCode] = RfidMapping(
                        rfidCode = rfidCode,
                        sku = mapping.get("sku")?.asString ?: "",
                        material = mapping.get("material")?.asString ?: "",
                        color = mapping.get("color")?.asString ?: "",
                        hex = mapping.get("hex")?.asString,
                        sampleCount = mapping.get("sampleCount")?.asInt ?: 1
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing RFID mapping for $rfidCode: ${e.message}")
                }
            }
            
            RfidMappings(
                version = jsonObject.get("version")?.asInt ?: 1,
                description = jsonObject.get("description")?.asString ?: "RFID to SKU mappings",
                rfidMappings = mappings
            ).also {
                Log.i(TAG, "Loaded ${mappings.size} RFID mappings from assets")
            }
            
        } catch (e: IOException) {
            Log.w(TAG, "RFID mappings file not found in assets", e)
            RfidMappings.empty()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading RFID mappings from assets", e)
            RfidMappings.empty()
        }
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
     * Reset mappings by reloading from assets
     */
    fun resetToDefaults() {
        clearMappings()
        // This will trigger loading from assets again
        getCurrentMappings()
    }
    
    /**
     * Force reload from assets (useful for development)
     */
    fun reloadFromAssets(): FilamentMappings {
        val mappings = loadFromAssetsOrDefault()
        if (mappings.productCatalog.isNotEmpty()) {
            saveMappings(mappings)
        }
        return mappings
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
            productCatalogCount = mappings.productCatalog.size,
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
    val productCatalogCount: Int,
    val materialMappingCount: Int,
    val brandMappingCount: Int,
    val temperatureMappingCount: Int
)