package com.bscan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Granular settings for how materials are visually displayed in FilamentColorBox
 */
data class MaterialDisplaySettings(
    val showMaterialShapes: Boolean = true,
    val showMaterialNameInShape: Boolean = false,
    val showMaterialVariantInShape: Boolean = false,
    val showFullVariantNamesInShape: Boolean = false
) {
    companion object {
        /**
         * Default settings that maintain backward compatibility
         */
        val DEFAULT = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = false,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Legacy MaterialDisplayMode.SHAPES equivalent
         */
        val SHAPES_ONLY = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = false,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Legacy MaterialDisplayMode.TEXT_LABELS equivalent
         */
        val TEXT_LABELS = MaterialDisplaySettings(
            showMaterialShapes = false,
            showMaterialNameInShape = true,
            showMaterialVariantInShape = false,
            showFullVariantNamesInShape = false
        )
        
        /**
         * Maximum information display
         */
        val FULL_INFO = MaterialDisplaySettings(
            showMaterialShapes = true,
            showMaterialNameInShape = true,
            showMaterialVariantInShape = true,
            showFullVariantNamesInShape = true
        )
    }
}

// Preview components to demonstrate different MaterialDisplaySettings
@Composable
private fun MockFilamentColorBox(
    settings: MaterialDisplaySettings,
    modifier: Modifier = Modifier
) {
    val materialColor = Color(0xFF2196F3) // Blue
    val materialName = "PLA"
    val variantName = "Basic"
    val fullVariantName = "Basic Matte"
    
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(if (settings.showMaterialShapes) CircleShape else RoundedCornerShape(8.dp))
            .background(materialColor)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                if (settings.showMaterialShapes) CircleShape else RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (settings.showMaterialNameInShape || settings.showMaterialVariantInShape) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (settings.showMaterialNameInShape) {
                    Text(
                        text = materialName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
                if (settings.showMaterialVariantInShape) {
                    Text(
                        text = if (settings.showFullVariantNamesInShape) fullVariantName else variantName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Preview Functions
@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsDefaultPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Default Settings (Shapes Only)",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    MockFilamentColorBox(
                        settings = MaterialDisplaySettings.DEFAULT
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsShapesOnlyPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Shapes Only",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    MockFilamentColorBox(
                        settings = MaterialDisplaySettings.SHAPES_ONLY
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsTextLabelsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Text Labels",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    MockFilamentColorBox(
                        settings = MaterialDisplaySettings.TEXT_LABELS
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsFullInfoPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Full Information",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    MockFilamentColorBox(
                        settings = MaterialDisplaySettings.FULL_INFO
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsCustomPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Custom Configuration",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    MockFilamentColorBox(
                        settings = MaterialDisplaySettings(
                            showMaterialShapes = true,
                            showMaterialNameInShape = true,
                            showMaterialVariantInShape = false,
                            showFullVariantNamesInShape = false
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MaterialDisplaySettingsComparisonPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "MaterialDisplaySettings Comparison",
                style = MaterialTheme.typography.titleLarge
            )
            
            // Shapes Only
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Shapes Only",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        MockFilamentColorBox(settings = MaterialDisplaySettings.SHAPES_ONLY)
                    }
                }
            }
            
            // Text Labels
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Text Labels",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        MockFilamentColorBox(settings = MaterialDisplaySettings.TEXT_LABELS)
                    }
                }
            }
            
            // Full Info
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Full Information",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        MockFilamentColorBox(settings = MaterialDisplaySettings.FULL_INFO)
                    }
                }
            }
        }
    }
}

