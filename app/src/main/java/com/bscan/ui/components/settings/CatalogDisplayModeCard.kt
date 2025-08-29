package com.bscan.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.screens.home.CatalogDisplayMode

/**
 * Card for catalog display preferences
 */
@Composable
fun CatalogDisplayModeCard(
    currentMode: CatalogDisplayMode,
    currentMaterialSettings: MaterialDisplaySettings,
    onModeChanged: (CatalogDisplayMode) -> Unit,
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
                text = "Catalog Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "How product information is displayed in the catalog browser",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Display mode options using consistent full-width style
            CatalogDisplayMode.entries.forEach { mode ->
                CatalogDisplayOption(
                    title = mode.displayName,
                    description = mode.description,
                    isSelected = currentMode == mode,
                    onClick = { onModeChanged(mode) },
                    displayMode = mode,
                    materialSettings = currentMaterialSettings
                )
            }
        }
    }
}

/**
 * Catalog display option with radio button and complete catalog item preview
 */
@Composable
private fun CatalogDisplayOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    displayMode: CatalogDisplayMode,
    materialSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Complete catalog item preview showing display mode effect
        CatalogPreviewCard(
            displayMode = displayMode,
            materialSettings = materialSettings,
            modifier = Modifier.fillMaxWidth()
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
            
            // Status indicators (same for both modes)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Has RFID mapping",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Thermoplastic",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CatalogDisplayModeCardPreview() {
    MaterialTheme {
        CatalogDisplayModeCard(
            currentMode = CatalogDisplayMode.COMPLETE_TITLE,
            currentMaterialSettings = MaterialDisplaySettings(
                showMaterialShapes = true,
                showFinishEffects = true,
                accelerometerEffects = false
            ),
            onModeChanged = { }
        )
    }
}


