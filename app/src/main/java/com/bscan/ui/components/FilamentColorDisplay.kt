package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.tooling.preview.Preview
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

@Preview(showBackground = true)
@Composable
private fun FilamentColorBoxPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilamentColorBox(
                    colorHex = "#FF0000",
                    filamentType = "PLA Basic",
                    size = 48.dp
                )
                FilamentColorBox(
                    colorHex = "#00FF00",
                    filamentType = "ABS Basic", 
                    size = 48.dp
                )
                FilamentColorBox(
                    colorHex = "#0000FF",
                    filamentType = "PETG Basic",
                    size = 48.dp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilamentColorBox(
                    colorHex = "#FFD700",
                    filamentType = "PLA Silk",
                    size = 48.dp
                )
                FilamentColorBox(
                    colorHex = "#800080",
                    filamentType = "PLA Matte",
                    size = 48.dp
                )
                FilamentColorBox(
                    colorHex = "#FFA50080",
                    filamentType = "PLA Translucent",
                    size = 48.dp
                )
            }
        }
    }
}