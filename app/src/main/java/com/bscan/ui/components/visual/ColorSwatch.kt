package com.bscan.ui.components.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Basic color swatch component that displays a solid color
 * Use this for simple color display without material-specific effects
 */
@Composable
fun ColorSwatch(
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    showTransparencyPattern: Boolean = true
) {
    val color = parseColorWithAlpha(colorHex)
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
    ) {
        // Show transparency pattern if color has alpha and option is enabled
        if (showTransparencyPattern && color.alpha < 1f) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                drawCheckerboard(
                    size = this.size,
                    checkSize = with(density) { 4.dp.toPx() }
                )
            }
        } else {
            // Solid background for opaque colours
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface, shape)
            )
        }
        
        // Main colour layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color, shape)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorSwatchPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorSwatch(
                colorHex = "#FF6B35"
            )
            ColorSwatch(
                colorHex = "#4A90E2"
            )
            ColorSwatch(
                colorHex = "#7B68EE"
            )
            ColorSwatch(
                colorHex = "#50FF6B35", // Semi-transparent
                showTransparencyPattern = true
            )
        }
    }
}



