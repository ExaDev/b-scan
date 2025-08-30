package com.bscan.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.screens.home.CatalogDisplayMode

/**
 * Card for material shape style settings
 */
@Composable
fun MaterialShapeStyleCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Material Shape Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose the shape style for material color boxes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Option 1: Material-based shapes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSettingsChanged(currentSettings.copy(showMaterialShapes = true)) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = currentSettings.showMaterialShapes,
                        onClick = { onSettingsChanged(currentSettings.copy(showMaterialShapes = true)) }
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Material-based shapes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentSettings.showMaterialShapes) FontWeight.Medium else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Different shapes for each material type (PLA=circle, PETG=octagon, etc.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MaterialShapesPreview()
            }

            // Option 2: Simple rounded rectangles
            MaterialDisplayOption(
                title = "Simple rounded rectangles",
                description = "Consistent rectangular shape for all materials",
                isSelected = !currentSettings.showMaterialShapes,
                onClick = { onSettingsChanged(currentSettings.copy(showMaterialShapes = false)) },
                previewSettings = currentSettings.copy(showMaterialShapes = false)
            )
        }
    }
}

/**
 * Card for material text overlay settings
 */
@Composable
fun MaterialTextOverlayCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Material Text Overlays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose what text information appears over material colors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            MaterialDisplayOption(
                title = "No text overlays",
                description = "Pure color display without text",
                isSelected = !currentSettings.showMaterialNameInShape && !currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = false,
                        showMaterialVariantInShape = false
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = false,
                    showMaterialVariantInShape = false
                )
            )
            
            MaterialDisplayOption(
                title = "Material name only",
                description = "Show material abbreviations (PLA, PETG, etc.)",
                isSelected = currentSettings.showMaterialNameInShape && !currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = true,
                        showMaterialVariantInShape = false
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = true,
                    showMaterialVariantInShape = false
                )
            )
            
            MaterialDisplayOption(
                title = "Material + variant",
                description = "Show both material and variant (PLA B, PETG S, etc.)",
                isSelected = currentSettings.showMaterialNameInShape && currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = true,
                        showMaterialVariantInShape = true
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = true,
                    showMaterialVariantInShape = true
                )
            )
            
            MaterialDisplayOption(
                title = "Variant only",
                description = "Show only variant information (Basic, Silk, Matte)",
                isSelected = !currentSettings.showMaterialNameInShape && currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = false,
                        showMaterialVariantInShape = true
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = false,
                    showMaterialVariantInShape = true
                )
            )
        }
    }
}

/**
 * Card for material variant name format settings
 */
@Composable
fun MaterialVariantNameCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val variantOptionsEnabled = currentSettings.showMaterialVariantInShape
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Icon Variant Name Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (variantOptionsEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            
            Text(
                text = if (variantOptionsEnabled) {
                    "Choose how variant names are displayed in material icons"
                } else {
                    "Enable variant text overlays above to use these options"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (variantOptionsEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
            
            MaterialDisplayOption(
                title = "Abbreviated in icons",
                description = "Short forms in shapes (B, S, M, CF, HF, etc.)",
                isSelected = !currentSettings.showFullVariantNamesInShape,
                onClick = { onSettingsChanged(currentSettings.copy(showFullVariantNamesInShape = false)) },
                previewSettings = currentSettings.copy(showFullVariantNamesInShape = false),
                enabled = variantOptionsEnabled
            )
            
            MaterialDisplayOption(
                title = "Full names in icons",
                description = "Complete names in shapes (Basic, Silk, Matte, etc.)",
                isSelected = currentSettings.showFullVariantNamesInShape,
                onClick = { onSettingsChanged(currentSettings.copy(showFullVariantNamesInShape = true)) },
                previewSettings = currentSettings.copy(showFullVariantNamesInShape = true),
                enabled = variantOptionsEnabled
            )
        }
    }
}

/**
 * Material display option with radio button and complete catalog item preview
 */
@Composable
private fun MaterialDisplayOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        
        // Complete catalog item preview
        CatalogPreviewCard(
            displayMode = CatalogDisplayMode.COMPLETE_TITLE,
            materialSettings = previewSettings,
            modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)
        )
    }
}

/**
 * Preview card showing how catalog items will look in each display mode
 */
@Composable
private fun CatalogPreviewCard(
    displayMode: CatalogDisplayMode,
    materialSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview using current material display settings
            FilamentColorBox(
                colorHex = "#00BCD4", // Cyan color
                filamentType = "PLA Basic",
                materialDisplaySettings = materialSettings,
                modifier = Modifier.size(40.dp)
            )
            
            // Product information based on display mode
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title changes based on display mode
                Text(
                    text = when (displayMode) {
                        CatalogDisplayMode.COMPLETE_TITLE -> "Basic Cyan PLA"
                        CatalogDisplayMode.COLOR_FOCUSED -> "Cyan"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Properties change based on display mode
                when (displayMode) {
                    CatalogDisplayMode.COMPLETE_TITLE -> {
                        Text(
                            text = "SKU: GFL00A00K0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CatalogDisplayMode.COLOR_FOCUSED -> {
                        Text(
                            text = "PLA Basic • SKU: GFL00A00K0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Temperature info (same for both modes)
                Text(
                    text = "210-230°C • Bed: 60°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MaterialShapesPreview(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilamentColorBox(
            colorHex = "#FF6B35",
            filamentType = "PLA",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#4A90E2",
            filamentType = "PETG",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#7B68EE",
            filamentType = "ABS",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#F44336",
            filamentType = "ASA",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#4CAF50",
            filamentType = "TPU",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialShapeStyleCardPreview() {
    MaterialTheme {
        MaterialShapeStyleCard(
            currentSettings = MaterialDisplaySettings(
                showMaterialShapes = true,
                showMaterialNameInShape = false,
                showMaterialVariantInShape = true,
                showFullVariantNamesInShape = false
            ),
            onSettingsChanged = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialTextOverlayCardPreview() {
    MaterialTheme {
        MaterialTextOverlayCard(
            currentSettings = MaterialDisplaySettings(
                showMaterialShapes = true,
                showMaterialNameInShape = true,
                showMaterialVariantInShape = false,
                showFullVariantNamesInShape = true
            ),
            onSettingsChanged = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialVariantNameCardPreview() {
    MaterialTheme {
        MaterialVariantNameCard(
            currentSettings = MaterialDisplaySettings(
                showMaterialShapes = true,
                showMaterialNameInShape = true,
                showMaterialVariantInShape = true,
                showFullVariantNamesInShape = false
            ),
            onSettingsChanged = { }
        )
    }
}



