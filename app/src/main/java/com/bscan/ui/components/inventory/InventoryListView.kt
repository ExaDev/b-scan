package com.bscan.ui.components.inventory

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Compact list view for inventory items - shows minimal information in dense format
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryCompactListView(
    inventoryItems: List<Component>,
    allComponents: List<Component>,
    selectedComponents: Set<String> = emptySet(),
    isBulkSelectionMode: Boolean = false,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onToggleSelection: ((String) -> Unit)? = null,
    onDeleteComponent: ((Component) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(inventoryItems, key = { it.id }) { inventoryItem ->
            CompactInventoryItemRow(
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
 * Individual compact row for an inventory item
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CompactInventoryItemRow(
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
            .height(72.dp)
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
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selection checkbox (if in bulk mode)
            if (isBulkSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection?.invoke(inventoryItem.id) },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Component icon
            Icon(
                imageVector = getComponentIcon(inventoryItem.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            // Main content area
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Name and identifier
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inventoryItem.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Child count badge
                    if (childCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = childCount.toString(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Second row: Category, manufacturer, mass
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category and manufacturer
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = inventoryItem.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = inventoryItem.manufacturer,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Mass information
                    totalMass?.let { mass ->
                        Text(
                            text = "${String.format("%.1f", mass)}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Delete button (only show when not in bulk mode or not selected)
                if (!isBulkSelectionMode || !isSelected) {
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
                
                // Navigation indicator
                if (!isBulkSelectionMode) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Table-style view for inventory items - shows data in columns
 */
@Composable
fun InventoryTableView(
    inventoryItems: List<Component>,
    allComponents: List<Component>,
    selectedComponents: Set<String> = emptySet(),
    isBulkSelectionMode: Boolean = false,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onToggleSelection: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Table header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isBulkSelectionMode) {
                    Spacer(modifier = Modifier.width(32.dp))
                }
                
                Text(
                    text = "NAME",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.3f)
                )
                
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.2f)
                )
                
                Text(
                    text = "MANUFACTURER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.2f)
                )
                
                Text(
                    text = "CHILDREN",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
                
                Text(
                    text = "MASS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.15f)
                )
            }
        }
        
        // Table rows
        LazyColumn(
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(inventoryItems, key = { it.id }) { inventoryItem ->
                TableInventoryItemRow(
                    inventoryItem = inventoryItem,
                    allComponents = allComponents,
                    isSelected = inventoryItem.id in selectedComponents,
                    isBulkSelectionMode = isBulkSelectionMode,
                    onNavigateToDetails = onNavigateToDetails,
                    onToggleSelection = onToggleSelection
                )
            }
        }
    }
}

/**
 * Individual table row for an inventory item
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TableInventoryItemRow(
    inventoryItem: Component,
    allComponents: List<Component>,
    isSelected: Boolean = false,
    isBulkSelectionMode: Boolean = false,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onToggleSelection: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val childCount = allComponents.count { it.parentComponentId == inventoryItem.id }
    val totalMass = calculateTotalMass(inventoryItem, allComponents)
    val primaryIdentifier = inventoryItem.getPrimaryTrackingIdentifier()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = {
                    if (isBulkSelectionMode) {
                        onToggleSelection?.invoke(inventoryItem.id)
                    } else {
                        primaryIdentifier?.let { identifier ->
                            onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, identifier.value)
                        }
                    }
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox
            if (isBulkSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection?.invoke(inventoryItem.id) },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // Name with icon
            Row(
                modifier = Modifier.weight(0.3f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = getComponentIcon(inventoryItem.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                
                Text(
                    text = inventoryItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Category
            Text(
                text = inventoryItem.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.2f)
            )
            
            // Manufacturer
            Text(
                text = inventoryItem.manufacturer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.2f)
            )
            
            // Child count
            Text(
                text = if (childCount > 0) childCount.toString() else "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(0.15f)
            )
            
            // Mass
            Text(
                text = totalMass?.let { "${String.format("%.1f", it)}g" } ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = if (totalMass != null) FontWeight.Medium else FontWeight.Normal,
                modifier = Modifier.weight(0.15f)
            )
        }
    }
}

// Preview composables
@Preview(showBackground = true)
@Composable
private fun InventoryCompactListViewPreview() {
    MaterialTheme {
        InventoryCompactListView(
            inventoryItems = listOf(createMockInventoryItem()),
            allComponents = createMockAllComponents(),
            onNavigateToDetails = { _, _ -> },
            onToggleSelection = { },
            onDeleteComponent = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryTableViewPreview() {
    MaterialTheme {
        InventoryTableView(
            inventoryItems = listOf(createMockInventoryItem(), createMockInventoryItem().copy(id = "2", name = "ABS Strong Filament")),
            allComponents = createMockAllComponents(),
            onNavigateToDetails = { _, _ -> },
            onToggleSelection = { }
        )
    }
}

// Mock data functions (reuse from InventoryItemCard.kt)
private fun createMockInventoryItem(): Component {
    return Component(
        id = "inventory_001",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = "PLA Basic Filament Tray",
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
    val parentComponent = createMockInventoryItem()
    return listOf(
        parentComponent,
        Component(
            id = "child_001",
            name = "RFID Tag",
            category = "rfid-tag",
            parentComponentId = "inventory_001",
            manufacturer = "Bambu Lab",
            massGrams = 2.0f
        )
    )
}