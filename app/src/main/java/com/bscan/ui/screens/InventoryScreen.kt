package com.bscan.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bscan.MainViewModel
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import com.bscan.repository.MergedManufacturer
import com.bscan.ui.components.ColorPreviewDot
import com.bscan.ui.components.common.ConfirmationDialog
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Load inventory items and manufacturers using UnifiedDataAccess
    var inventoryItems by remember { mutableStateOf<Map<String, InventoryItem>>(emptyMap()) }
    var manufacturers by remember { mutableStateOf<Map<String, MergedManufacturer>>(emptyMap()) }
    var components by remember { mutableStateOf<Map<String, PhysicalComponent>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Show delete confirmation dialog
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    
    // Load data
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val unifiedDataAccess = viewModel.getUnifiedDataAccess()
                val loadedInventoryItems = unifiedDataAccess.getInventoryItems()
                val loadedManufacturers = unifiedDataAccess.getAllManufacturers()
                val loadedComponents = unifiedDataAccess.getComponents()
                
                withContext(Dispatchers.Main) {
                    inventoryItems = loadedInventoryItems
                    manufacturers = loadedManufacturers
                    components = loadedComponents
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }
    
    // Delete confirmation dialog
    itemToDelete?.let { item ->
        ConfirmationDialog(
            title = "Remove Inventory Item",
            message = "Remove inventory item ${formatTrayId(item.trayUid)} and all its data? This cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val unifiedDataAccess = viewModel.getUnifiedDataAccess()
                        // Remove inventory item and its components
                        item.components.forEach { componentId ->
                            components[componentId]?.let { component ->
                                viewModel.getUserDataRepository().removeComponent(componentId)
                            }
                        }
                        viewModel.getUserDataRepository().removeInventoryItem(item.trayUid)
                        
                        // Refresh data
                        val updatedInventoryItems = unifiedDataAccess.getInventoryItems()
                        val updatedComponents = unifiedDataAccess.getComponents()
                        
                        withContext(Dispatchers.Main) {
                            inventoryItems = updatedInventoryItems
                            components = updatedComponents
                        }
                    } catch (e: Exception) {
                        // Handle error silently for now
                    }
                }
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null },
            isDestructive = true
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        
        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (inventoryItems.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                InventoryEmptyState()
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics summary
                item {
                    InventoryStatisticsCard(
                        inventoryItems = inventoryItems,
                        manufacturers = manufacturers,
                        components = components
                    )
                }
                
                // Individual inventory item cards
                items(inventoryItems.values.toList()) { item ->
                    InventoryItemCard(
                        inventoryItem = item,
                        components = components,
                        manufacturers = manufacturers,
                        onDeleteItem = { itemToDelete = item },
                        onNavigateToDetails = onNavigateToDetails
                    )
                }
            }
        }
    }
}

// Utility functions for formatting
fun formatTrayId(trayUid: String): String {
    return if (trayUid.length > 8) {
        trayUid.takeLast(8)
    } else {
        trayUid
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStatisticsCard(
    inventoryItems: Map<String, InventoryItem>,
    manufacturers: Map<String, MergedManufacturer>,
    components: Map<String, PhysicalComponent>,
    modifier: Modifier = Modifier
) {
    val totalItems = inventoryItems.size
    val totalComponents = components.size
    val uniqueManufacturers = manufacturers.size
    val filamentComponents = components.values.count { it.type == PhysicalComponentType.FILAMENT }
    val spoolComponents = components.values.count { it.type == PhysicalComponentType.BASE_SPOOL }
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Inventory Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            val mainStats = listOf(
                "Total Items" to totalItems.toString(),
                "Components" to totalComponents.toString(),
                "Manufacturers" to uniqueManufacturers.toString()
            )
            
            StatisticGrid(
                statistics = mainStats,
                modifier = Modifier.fillMaxWidth()
            )
            
            val componentStats = listOf(
                "Filament" to filamentComponents.toString(),
                "Spools" to spoolComponents.toString()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                componentStats.forEach { (label, value) ->
                    StatisticDisplay(
                        label = label,
                        value = value
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Show manufacturer breakdown
            if (manufacturers.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Manufacturers: ${manufacturers.keys.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    inventoryItem: InventoryItem,
    components: Map<String, PhysicalComponent>,
    manufacturers: Map<String, MergedManufacturer>,
    onDeleteItem: (InventoryItem) -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    val itemComponents = inventoryItem.components.mapNotNull { components[it] }
    val filamentComponent = itemComponents.firstOrNull { it.type == PhysicalComponentType.FILAMENT }
    val manufacturerName = filamentComponent?.let { 
        manufacturers[it.manufacturer]?.displayName ?: it.manufacturer 
    } ?: "Unknown"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, inventoryItem.trayUid)
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with tray UID and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventory Item",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTrayId(inventoryItem.trayUid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Full: ${inventoryItem.trayUid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { onDeleteItem(inventoryItem) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove inventory item"
                    )
                }
            }
            
            // Manufacturer and basic stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manufacturer",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = manufacturerName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${itemComponents.size} components",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Component breakdown
            if (itemComponents.isNotEmpty()) {
                Text(
                    text = "Components:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(itemComponents) { component ->
                        ComponentChip(component = component)
                    }
                }
            }
            
            // Mass information
            inventoryItem.totalMeasuredMass?.let { totalMass ->
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Mass:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${totalMass}g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Latest measurement
            inventoryItem.latestMeasurement?.let { measurement ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Latest Measurement:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${measurement.measuredMassGrams}g",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Timestamps
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inventoryItem.lastUpdated.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Show notes if available
                if (inventoryItem.notes.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Note,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentChip(
    component: PhysicalComponent,
    modifier: Modifier = Modifier
) {
    val icon = when (component.type) {
        PhysicalComponentType.FILAMENT -> Icons.Default.LinearScale
        PhysicalComponentType.BASE_SPOOL -> Icons.Default.Album
        PhysicalComponentType.CORE_RING -> Icons.Default.DonutLarge
        PhysicalComponentType.ADAPTER -> Icons.Default.Extension
        PhysicalComponentType.PACKAGING -> Icons.Default.Inventory2
    }
    
    val containerColor = when (component.type) {
        PhysicalComponentType.FILAMENT -> MaterialTheme.colorScheme.primaryContainer
        PhysicalComponentType.BASE_SPOOL -> MaterialTheme.colorScheme.secondaryContainer
        PhysicalComponentType.CORE_RING -> MaterialTheme.colorScheme.tertiaryContainer
        PhysicalComponentType.ADAPTER -> MaterialTheme.colorScheme.errorContainer
        PhysicalComponentType.PACKAGING -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (component.type) {
        PhysicalComponentType.FILAMENT -> MaterialTheme.colorScheme.onPrimaryContainer
        PhysicalComponentType.BASE_SPOOL -> MaterialTheme.colorScheme.onSecondaryContainer
        PhysicalComponentType.CORE_RING -> MaterialTheme.colorScheme.onTertiaryContainer
        PhysicalComponentType.ADAPTER -> MaterialTheme.colorScheme.onErrorContainer
        PhysicalComponentType.PACKAGING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = component.name,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
            if (component.variableMass) {
                Text(
                    text = "${component.massGrams.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun InventoryEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.Inventory,
        title = "No Inventory Items",
        subtitle = "Scan some filament tags to start building your inventory",
        modifier = modifier
    )
}

