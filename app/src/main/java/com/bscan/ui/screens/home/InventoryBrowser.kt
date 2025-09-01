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
import com.bscan.model.graph.entities.PhysicalComponent
import com.bscan.model.graph.entities.InventoryItem
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphRepository
import com.bscan.ui.screens.DetailType
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
    allComponents: List<PhysicalComponent>,
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
    val graphRepository = remember { GraphRepository(context) }
    
    // Load inventory root entities (main trackable items from each subgraph)
    var inventoryItems by remember { mutableStateOf(listOf<Entity>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        try {
            inventoryItems = graphRepository.findInventoryRootEntities()
        } catch (e: Exception) {
            inventoryItems = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Inventory content
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (inventoryItems.isEmpty()) {
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
                        totalItems = inventoryItems.size,
                        allItems = inventoryItems.size,
                        onViewFullInventory = { 
                            // TODO: Navigate to full inventory screen
                        }
                    )
                }
                
                // Inventory items (up to 10 items in tab view)
                val itemsToShow = inventoryItems.take(10)
                items(itemsToShow, key = { it.id }) { inventoryItem ->
                    CompactInventoryCard(
                        inventoryItem = inventoryItem,
                        allComponents = allComponents,
                        onNavigateToDetails = onNavigateToDetails,
                        onDeleteComponent = { 
                            // TODO: Implement entity deletion
                        }
                    )
                }
                
                // Show "View All" if there are more items
                if (inventoryItems.size > 10) {
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
                                    text = "Showing 10 of ${inventoryItems.size} items",
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
    inventoryItem: Entity,
    allComponents: List<PhysicalComponent>,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onDeleteComponent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val entityType = inventoryItem.getProperty<String>("category") 
        ?: inventoryItem.getProperty<String>("virtualType")
        ?: "inventory-item"
    val label = inventoryItem.label
    val manufacturer = inventoryItem.getProperty<String>("manufacturer") ?: "Unknown"
    val currentQuantity = inventoryItem.getProperty<Double>("currentQuantity") ?: 0.0
    val quantityUnit = inventoryItem.getProperty<String>("quantityUnit") ?: "units"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            // Navigate to entity details - the "entity/" prefix will be handled by AppNavigation
            onNavigateToDetails?.invoke(DetailType.COMPONENT, "entity/${inventoryItem.id}")
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
                imageVector = getComponentIcon(entityType),
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
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entityType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = manufacturer,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Quantity if available
            if (currentQuantity > 0.0) {
                Text(
                    text = "${String.format("%.1f", currentQuantity)} $quantityUnit",
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
                createMockPhysicalComponent("1", "PLA Orange"),
                createMockPhysicalComponent("2", "PETG Black"),
                createMockPhysicalComponent("3", "ABS White")
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
            inventoryItem = createMockVirtualEntity("1", "PLA Orange"),
            allComponents = emptyList(),
            onNavigateToDetails = { _, _ -> },
            onDeleteComponent = { }
        )
    }
}

// Mock data for previews
private fun createMockPhysicalComponent(id: String, name: String): PhysicalComponent {
    return PhysicalComponent(
        id = "component_$id",
        label = name,
        properties = mutableMapOf<String, com.bscan.model.graph.PropertyValue>().apply {
            put("category", com.bscan.model.graph.PropertyValue.create("filament"))
            put("manufacturer", com.bscan.model.graph.PropertyValue.create("Bambu Lab"))
        }
    )
}

private fun createMockVirtualEntity(id: String, name: String): com.bscan.model.graph.entities.Virtual {
    return com.bscan.model.graph.entities.Virtual(
        id = "virtual_$id",
        virtualType = "filament_tray",
        label = name,
        properties = mutableMapOf<String, com.bscan.model.graph.PropertyValue>().apply {
            put("manufacturer", com.bscan.model.graph.PropertyValue.create("Bambu Lab"))
            put("currentQuantity", com.bscan.model.graph.PropertyValue.create(578.5))
            put("quantityUnit", com.bscan.model.graph.PropertyValue.create("grams"))
            put("filamentType", com.bscan.model.graph.PropertyValue.create("PLA"))
            put("colorName", com.bscan.model.graph.PropertyValue.create("Orange"))
        }
    )
}