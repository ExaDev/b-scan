package com.bscan.ui.components.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import java.time.LocalDateTime

/**
 * Gallery-style grid view for inventory items - shows visual cards with icons
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryGalleryView(
    inventoryItems: List<Component>,
    allComponents: List<Component>,
    selectedComponents: Set<String> = emptySet(),
    isBulkSelectionMode: Boolean = false,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onToggleSelection: ((String) -> Unit)? = null,
    onDeleteComponent: ((Component) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(inventoryItems, key = { it.id }) { inventoryItem ->
            GalleryInventoryItemCard(
                inventoryItem = inventoryItem,
                allComponents = allComponents,
                isSelected = inventoryItem.id in selectedComponents,
                isBulkSelectionMode = isBulkSelectionMode,
                onNavigateToDetails = onNavigateToDetails,
                onToggleSelection = onToggleSelection,
                onDeleteComponent = onDeleteComponent
            )
        }
    }
}

/**
 * Individual gallery card for an inventory item
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun GalleryInventoryItemCard(
    inventoryItem: Component,
    allComponents: List<Component>,
    isSelected: Boolean = false,
    isBulkSelectionMode: Boolean = false,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onToggleSelection: ((String) -> Unit)? = null,
    onDeleteComponent: ((Component) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val childCount = allComponents.count { it.parentComponentId == inventoryItem.id }
    val totalMass = calculateTotalMass(inventoryItem, allComponents)
    val primaryIdentifier = inventoryItem.getPrimaryTrackingIdentifier()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f) // Slightly taller than wide for better text display
            .combinedClickable(
                onClick = {
                    if (isBulkSelectionMode) {
                        onToggleSelection?.invoke(inventoryItem.id)
                    } else {
                        primaryIdentifier?.let { identifier ->
                            onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, identifier.value)
                        }
                    }
                },
                onLongClick = {
                    if (!isBulkSelectionMode) {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggleSelection?.invoke(inventoryItem.id)
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selection checkbox (if in bulk mode)
            if (isBulkSelectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection?.invoke(inventoryItem.id) },
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(28.dp)) // Balance spacing when no checkbox
            }
            
            // Large component icon
            Icon(
                imageVector = getComponentIcon(inventoryItem.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            
            // Component name
            Text(
                text = inventoryItem.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category
            Text(
                text = inventoryItem.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Manufacturer
            Text(
                text = inventoryItem.manufacturer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Bottom info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Child count badge
                if (childCount > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "$childCount",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                // Mass information
                totalMass?.let { mass ->
                    Text(
                        text = "${String.format("%.0f", mass)}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                } ?: Spacer(modifier = Modifier.width(1.dp))
            }
            
            // Action buttons (only show delete when not in bulk mode or not selected)
            if (!isBulkSelectionMode || !isSelected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onDeleteComponent?.invoke(inventoryItem) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete component",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun InventoryGalleryViewPreview() {
    MaterialTheme {
        InventoryGalleryView(
            inventoryItems = listOf(
                createMockGalleryItem("1", "PLA Basic Orange"),
                createMockGalleryItem("2", "PETG Strong Black"),
                createMockGalleryItem("3", "ABS Tough White")
            ),
            allComponents = createMockAllComponents(),
            onNavigateToDetails = { _, _ -> },
            onToggleSelection = { },
            onDeleteComponent = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GalleryInventoryItemCardPreview() {
    MaterialTheme {
        GalleryInventoryItemCard(
            inventoryItem = createMockGalleryItem("1", "PLA Basic Orange"),
            allComponents = createMockAllComponents(),
            onNavigateToDetails = { _, _ -> },
            onToggleSelection = { },
            onDeleteComponent = { }
        )
    }
}

// Mock data functions for previews
private fun createMockGalleryItem(id: String, name: String): Component {
    return Component(
        id = "gallery_$id",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = name,
        category = "filament-tray",
        tags = listOf("PLA", "Orange", "1.75mm"),
        parentComponentId = null,
        massGrams = 578.5f,
        manufacturer = "Bambu Lab",
        description = "Premium PLA filament",
        lastUpdated = LocalDateTime.now().minusHours(2)
    )
}

private fun createMockAllComponents(): List<Component> {
    val parentComponent = createMockGalleryItem("1", "PLA Basic Orange")
    return listOf(
        parentComponent,
        Component(
            id = "child_001",
            name = "RFID Tag",
            category = "rfid-tag",
            parentComponentId = "gallery_1",
            manufacturer = "Bambu Lab",
            massGrams = 2.0f
        )
    )
}