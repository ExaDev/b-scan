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
    val colorMappings: Map<String, String> = defaultColorMappings,
    val materialMappings: Map<String, String> = defaultMaterialMappings,
    val brandMappings: Map<String, String> = defaultBrandMappings,
    val temperatureMappings: Map<String, TemperatureRange> = defaultTemperatureMappings
) {
    companion object {
        val defaultColorMappings = mapOf(
            "#000000" to "Black",
            "#FFFFFF" to "White", 
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            "#FFFF00" to "Yellow",
            "#FF00FF" to "Magenta",
            "#00FFFF" to "Cyan",
            "#FFA500" to "Orange",
            "#800080" to "Purple",
            "#FFC0CB" to "Pink",
            "#A52A2A" to "Brown",
            "#808080" to "Grey",
            "#C0C0C0" to "Silver",
            "#FFD700" to "Gold",
            "#008000" to "Dark Green",
            "#000080" to "Navy",
            "#800000" to "Maroon",
            "#008080" to "Teal",
            "#808000" to "Olive"
        )
        
        val defaultMaterialMappings = mapOf(
            "PLA" to "PLA (Polylactic Acid)",
            "ABS" to "ABS (Acrylonitrile Butadiene Styrene)",
            "PETG" to "PETG (Polyethylene Terephthalate Glycol)",
            "TPU" to "TPU (Thermoplastic Polyurethane)",
            "WOOD" to "Wood-filled PLA",
            "CARBON" to "Carbon Fiber",
            "METAL" to "Metal-filled",
            "SILK" to "Silk PLA",
            "MATTE" to "Matte PLA",
            "BASIC" to "Basic PLA"
        )
        
        val defaultBrandMappings = mapOf(
            "BAMBU" to "Bambu Lab",
            "BL" to "Bambu Lab",
            "GENERIC" to "Generic"
        )
        
        val defaultTemperatureMappings = mapOf(
            "PLA" to TemperatureRange(190, 220, 60),
            "ABS" to TemperatureRange(220, 260, 80),
            "PETG" to TemperatureRange(220, 250, 70),
            "TPU" to TemperatureRange(200, 230, 60)
        )
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