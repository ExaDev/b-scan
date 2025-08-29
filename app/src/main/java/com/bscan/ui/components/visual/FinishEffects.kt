package com.bscan.ui.components.visual

/**
 * Material finish types that affect visual rendering
 */
enum class FilamentFinish {
    BASIC,
    SILK,
    MATTE,
    TRANSLUCENT
}

/**
 * Detects the finish type of filament based on color alpha channel and filament type
 */
fun detectFilamentFinish(colorHex: String, filamentType: String): FilamentFinish {
    // Parse alpha from hex colour
    val cleanHex = colorHex.removePrefix("#")
    val alpha = when (cleanHex.length) {
        8 -> {
            // Check if this is RGBA format (common from tag data)
            val alphaHex = cleanHex.substring(6, 8)
            alphaHex.toIntOrNull(16) ?: 255
        }
        else -> 255
    }
    
    // Check for translucent materials
    if (alpha < 255 || filamentType.contains("Translucent", ignoreCase = true)) {
        return FilamentFinish.TRANSLUCENT
    }
    
    // Otherwise check product line for finish type
    return when {
        filamentType.contains("Silk", ignoreCase = true) -> FilamentFinish.SILK
        filamentType.contains("Matte", ignoreCase = true) -> FilamentFinish.MATTE
        else -> FilamentFinish.BASIC
    }
}

/**
 * Determines reflection blur intensity based on material type and finish
 */
fun getReflectionBlurIntensity(materialType: MaterialType, finish: FilamentFinish, filamentType: String): Float {
    return when {
        // PETG HF gets same reflection as regular PLA (medium blur)
        materialType == MaterialType.PETG && filamentType.contains("HF", ignoreCase = true) -> 0.5f
        // Regular PETG gets strong reflection
        materialType == MaterialType.PETG -> 0.1f
        // Matte PLA gets very blurred reflection
        materialType == MaterialType.PLA && finish == FilamentFinish.MATTE -> 0.9f
        // Regular PLA gets medium blur
        materialType == MaterialType.PLA && finish == FilamentFinish.BASIC -> 0.5f
        // Other combinations get no reflection
        else -> -1f // Use -1 to indicate no reflection should be applied
    }
}