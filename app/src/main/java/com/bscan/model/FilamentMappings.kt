package com.bscan.model

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
    val productCatalog: List<ProductEntry> = emptyList() // SKU-specific product entries with embedded color information
) {
    
    /**
     * Find products matching the given hex color and material type
     */
    fun findProductsByColor(hex: String, materialType: String? = null): List<ProductEntry> {
        return productCatalog.filter { product ->
            product.matchesColorAndMaterial(hex, materialType)
        }
    }
    
    /**
     * Get color name from product catalog with fallback to basic color name if not found
     */
    fun getColorName(hex: String, materialType: String? = null): String {
        // First try to find in product catalog
        val matchingProducts = findProductsByColor(hex, materialType)
        if (matchingProducts.isNotEmpty()) {
            return matchingProducts.first().getDisplayColorName()
        }
        
        // Fallback to basic color name based on hex value patterns
        return hex.removePrefix("#").let { hexCode ->
            when {
                hexCode.equals("000000", ignoreCase = true) -> "Black"
                hexCode.equals("FFFFFF", ignoreCase = true) -> "White"
                hexCode.startsWith("FF", ignoreCase = true) && hexCode.endsWith("0000", ignoreCase = true) -> "Red"
                hexCode.startsWith("00FF", ignoreCase = true) -> "Green"
                hexCode.endsWith("FF", ignoreCase = true) && hexCode.startsWith("0000", ignoreCase = true) -> "Blue"
                else -> "Unknown Color (#$hexCode)"
            }
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
            return productCatalog.isNotEmpty() || 
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