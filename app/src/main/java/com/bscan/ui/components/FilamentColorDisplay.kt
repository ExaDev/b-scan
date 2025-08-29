package com.bscan.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bscan.ui.components.visual.*

/**
 * Enhanced filament color display box with finish-specific visual effects
 * This is now a wrapper around MaterialDisplayBox for backward compatibility
 */
@Composable
fun FilamentColorBox(
    colorHex: String,
    filamentType: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape? = null, // Allow override, but default to material-based shape
    displayMode: MaterialDisplayMode? = null, // Allow override of display mode (deprecated)
    materialDisplaySettings: MaterialDisplaySettings? = null // New granular settings
) {
    MaterialDisplayBox(
        colorHex = colorHex,
        materialType = filamentType,
        modifier = modifier,
        size = size,
        shape = shape,
        displayMode = displayMode,
        materialDisplaySettings = materialDisplaySettings,
        category = "filament" // Maintain filament-specific behavior
    )
}

// All drawing functions have been moved to VisualEffects.kt