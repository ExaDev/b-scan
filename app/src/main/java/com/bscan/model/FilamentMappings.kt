package com.bscan.model

import com.bscan.model.graph.entities.StockDefinition
import java.time.LocalDateTime

/**
 * Updatable mappings for interpreting raw scan data into meaningful filament information.
 * These mappings can be updated over time to improve the interpretation of existing scans
 * without needing to rescan tags.
 */
data class FilamentMappings(
    val version: Int = 1,
    val lastUpdated: LocalDateTime = LocalDateTime.now(),
    val materialMappings: Map<String, String> = emptyMap(),
    val brandMappings: Map<String, String> = emptyMap(),
    val temperatureMappings: Map<String, TemperatureRange> = emptyMap(),
    val stockDefinitions: List<StockDefinition> = emptyList() // SKU-specific stock definitions with embedded color information
) {
    
    /**
     * Find stock definitions matching the given hex color and material type
     */
    fun findStockDefinitionsByColor(hex: String, materialType: String? = null): List<StockDefinition> {
        return stockDefinitions.filter { stockDef ->
            val stockColorHex = stockDef.getProperty<String>("colorHex")
            val stockMaterialType = stockDef.getProperty<String>("materialType")
            
            val hexMatches = stockColorHex?.equals(hex, ignoreCase = true) ?: false
            val materialMatches = materialType?.let { 
                stockMaterialType?.equals(it, ignoreCase = true) ?: false
            } ?: true
            
            hexMatches && materialMatches
        }
    }
    
    
    /**
     * Get material name with fallback to code if not found
     */
    fun getMaterialName(code: String): String {
        return materialMappings[code.uppercase()] ?: code.replace("_", " ").lowercase()
            .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
    
    /**
     * Get brand name with fallback to code if not found
     */
    fun getBrandName(code: String): String {
        return brandMappings[code.uppercase()] ?: code
    }
    
    /**
     * Get temperature range with fallback to PLA defaults if not found
     */
    fun getTemperatureRange(materialCode: String): TemperatureRange {
        return temperatureMappings[materialCode.uppercase()] ?: TemperatureRange(190, 220, 60)
    }
    companion object {
        /**
         * Create empty mappings - data will be loaded from JSON assets at runtime
         */
        fun empty(): FilamentMappings = FilamentMappings()
        
        /**
         * Check if mappings are populated (not empty)
         */
        fun FilamentMappings.isPopulated(): Boolean {
            return stockDefinitions.isNotEmpty() || 
                   materialMappings.isNotEmpty() || 
                   brandMappings.isNotEmpty() || 
                   temperatureMappings.isNotEmpty()
        }
    }
}

/**
 * Temperature range recommendations for different materials
 */
data class TemperatureRange(
    val minNozzle: Int,
    val maxNozzle: Int, 
    val bed: Int
)

/**
 * Color interpretation result with confidence level
 */
data class ColorInterpretation(
    val name: String,
    val hex: String,
    val confidence: Float, // 0.0 to 1.0
    val source: ColorSource
)

enum class ColorSource {
    EXACT_MATCH,    // Direct hex match in mappings
    CLOSE_MATCH,    // Close colour match using colour distance
    HSV_ANALYSIS,   // Analysis of HSV values
    FALLBACK        // Generic fallback name
}

/**
 * Material interpretation result
 */
data class MaterialInterpretation(
    val name: String,
    val category: String,
    val confidence: Float,
    val properties: MaterialProperties
)

data class MaterialProperties(
    val flexibilty: Float = 0.5f,       // 0 = rigid, 1 = flexible
    val strength: Float = 0.5f,         // 0 = brittle, 1 = strong
    val ease_of_printing: Float = 0.5f, // 0 = difficult, 1 = easy
    val supports_needed: Boolean = false
)