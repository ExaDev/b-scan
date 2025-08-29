package com.bscan.ui.components.visual

/**
 * Material types with their corresponding visual shapes
 */
enum class MaterialType {
    PLA,      // Circle
    ABS,      // Triangle  
    ASA,      // Inverted Triangle
    PETG,     // Hexagon
    TPU,      // Rounded Square
    PC,       // Octagon
    PA,       // Diamond/Rhombus
    PVA,      // Teardrop
    SUPPORT,  // Vertical Lines
    UNKNOWN   // Dodecagon (12-sided)
}

/**
 * Detects the base material type from filament type string
 */
fun detectMaterialType(filamentType: String): MaterialType {
    return when {
        filamentType.contains("Support", ignoreCase = true) -> MaterialType.SUPPORT
        filamentType.contains("PVA", ignoreCase = true) -> MaterialType.PVA
        filamentType.contains("PLA", ignoreCase = true) -> MaterialType.PLA
        filamentType.contains("ASA", ignoreCase = true) -> MaterialType.ASA
        filamentType.contains("ABS", ignoreCase = true) -> MaterialType.ABS
        filamentType.contains("PETG", ignoreCase = true) -> MaterialType.PETG
        filamentType.contains("TPU", ignoreCase = true) -> MaterialType.TPU
        filamentType.contains("PC", ignoreCase = true) -> MaterialType.PC
        filamentType.contains("PA", ignoreCase = true) || 
        filamentType.contains("Nylon", ignoreCase = true) -> MaterialType.PA
        else -> MaterialType.UNKNOWN
    }
}

/**
 * Gets the material abbreviation for text overlay display
 */
fun getMaterialAbbreviation(materialType: MaterialType): String {
    return when (materialType) {
        MaterialType.PLA -> "PLA"
        MaterialType.ABS -> "ABS"
        MaterialType.ASA -> "ASA"
        MaterialType.PETG -> "PETG"
        MaterialType.TPU -> "TPU"
        MaterialType.PC -> "PC"
        MaterialType.PA -> "PA"
        MaterialType.PVA -> "PVA"
        MaterialType.SUPPORT -> "SUP"
        MaterialType.UNKNOWN -> "?"
    }
}

/**
 * Extracts variant information from filament type string
 */
fun getVariantFromFilamentType(filamentType: String, showFullVariantNames: Boolean): String {
    // Common variant patterns
    val variants = listOf(
        "Basic", "Silk", "Matte", "Translucent", "Carbon Fiber", "CF", 
        "Support", "Water Soluble", "High Flow", "HF", "Tough", "Impact"
    )
    
    for (variant in variants) {
        if (filamentType.contains(variant, ignoreCase = true)) {
            return if (showFullVariantNames) {
                when (variant.uppercase()) {
                    "CF" -> "Carbon Fiber"
                    "HF" -> "High Flow"
                    else -> variant
                }
            } else {
                when (variant.uppercase()) {
                    "BASIC" -> "B"
                    "SILK" -> "S"
                    "MATTE" -> "M"
                    "TRANSLUCENT" -> "T"
                    "CARBON FIBER", "CF" -> "CF"
                    "SUPPORT" -> "SUP"
                    "HIGH FLOW", "HF" -> "HF"
                    "TOUGH" -> "TGH"
                    "IMPACT" -> "IMP"
                    else -> variant.take(3).uppercase()
                }
            }
        }
    }
    
    return ""
}