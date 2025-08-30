package com.bscan.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.model.Component
import com.bscan.repository.ComponentRepository
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.InventoryViewModel
import com.bscan.ui.screens.InventoryViewMode
import com.bscan.ui.components.inventory.InventoryItemCard
import com.bscan.ui.components.inventory.InventoryCompactListView
import com.bscan.ui.components.inventory.InventoryEmptyState
import java.time.LocalDateTime

/**
 * Inventory browser that fits within the home screen tab structure
 * Shows a compact view of inventory items similar to other browser tabs
 */
@Composable
fun InventoryBrowser(
    allComponents: List<Component>,
    lazyListState: LazyListState = rememberLazyListState(),
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: androidx.compose.ui.unit.Dp = 100.dp,
    fullPromptHeightDp: androidx.compose.ui.unit.Dp = 200.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventoryViewModel = remember { InventoryViewModel(context) }
    val uiState by inventoryViewModel.uiState.collectAsStateWithLifecycle()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Inventory content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.filteredComponents.isEmpty()) {
            // Empty state
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    InventoryEmptyState()
                }
            }
        } else {
            // Inventory items list (compact view for tab)
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary header
                item {
                    InventorySummaryCard(
                        totalItems = uiState.filteredComponents.size,
                        allItems = uiState.inventoryItems.size,
                        onViewFullInventory = { 
                            // TODO: Navigate to full inventory screen
                        }
                    )
                }
                
                // Inventory items (up to 10 items in tab view)
                val itemsToShow = uiState.filteredComponents.take(10)
                items(itemsToShow, key = { it.id }) { inventoryItem ->
                    CompactInventoryCard(
                        inventoryItem = inventoryItem,
                        allComponents = uiState.allComponents,
                        onNavigateToDetails = onNavigateToDetails,
                        onDeleteComponent = { 
                            inventoryViewModel.deleteComponent(it)
                        }
                    )
                }
                
                // Show "View All" if there are more items
                if (uiState.filteredComponents.size > 10) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Showing 10 of ${uiState.filteredComponents.size} items",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                TextButton(
                                    onClick = { 
                                        // TODO: Navigate to full inventory screen
                                    }
                                ) {
                                    Text(
                                        text = "View All",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary card showing inventory statistics
 */
@Composable
private fun InventorySummaryCard(
    totalItems: Int,
    allItems: Int,
    onViewFullInventory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inventory Overview",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$totalItems items • $allItems total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onViewFullInventory) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Open full inventory",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Compact inventory item card for tab view
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactInventoryCard(
    inventoryItem: Component,
    allComponents: List<Component>,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onDeleteComponent: ((Component) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val childCount = allComponents.count { it.parentComponentId == inventoryItem.id }
    val primaryIdentifier = inventoryItem.getPrimaryTrackingIdentifier()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            primaryIdentifier?.let { identifier ->
                onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, identifier.value)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Component icon
            Icon(
                imageVector = getComponentIcon(inventoryItem.category),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            // Component info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = inventoryItem.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = inventoryItem.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = inventoryItem.manufacturer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (childCount > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "$childCount",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            // Mass if available
            inventoryItem.massGrams?.let { mass ->
                Text(
                    text = "${String.format("%.1f", mass)}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Navigation icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Helper function for component icons (from InventoryUtilities.kt)
private fun getComponentIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "filament", "filament-tray" -> Icons.Default.ViewInAr
        "rfid-tag" -> Icons.Default.Nfc
        "core", "spool" -> Icons.Default.Circle
        "nozzle" -> Icons.Default.Circle
        "hotend" -> Icons.Default.Thermostat
        "build-plate" -> Icons.Default.GridOn
        "printer" -> Icons.Default.Print
        "tool" -> Icons.Default.Build
        "consumable" -> Icons.Default.Inventory
        else -> Icons.Default.Category
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryBrowserPreview() {
    MaterialTheme {
        InventoryBrowser(
            allComponents = listOf(
                createMockInventoryItem("1", "PLA Orange"),
                createMockInventoryItem("2", "PETG Black"),
                createMockInventoryItem("3", "ABS White")
            ),
            onNavigateToDetails = { _, _ -> },
            onSimulateScan = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CompactInventoryCardPreview() {
    MaterialTheme {
        CompactInventoryCard(
            inventoryItem = createMockInventoryItem("1", "PLA Orange"),
            allComponents = emptyList(),
            onNavigateToDetails = { _, _ -> },
            onDeleteComponent = { }
        )
    }
}

// Mock data for previews
private fun createMockInventoryItem(id: String, name: String): Component {
    return Component(
        id = "inventory_$id",
        identifiers = listOf(
            com.bscan.model.ComponentIdentifier(
                type = com.bscan.model.IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = com.bscan.model.IdentifierPurpose.TRACKING
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