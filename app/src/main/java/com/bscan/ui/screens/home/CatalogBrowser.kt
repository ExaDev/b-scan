package com.bscan.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.bscan.repository.UserDataRepository
import com.bscan.repository.GraphRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.ui.components.list.OverscrollListWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.bscan.ScanState
import com.bscan.model.*
import com.bscan.model.graph.entities.PhysicalComponent
import com.bscan.model.graph.entities.StockDefinition
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.home.GroupHeader

/**
 * Multi-manufacturer product catalog browser
 * Displays products from all manufacturers with filtering and grouping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogBrowser(
    allComponents: List<PhysicalComponent>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    val userDataRepository = remember { UserDataRepository(context) }
    
    // Get user preferences for catalog display mode and material display settings
    val userData by remember { derivedStateOf { userDataRepository.getUserData() } }
    val catalogDisplayMode = userData?.preferences?.catalogDisplayMode ?: CatalogDisplayMode.COMPLETE_TITLE
    val materialDisplaySettings = userData?.preferences?.materialDisplaySettings ?: MaterialDisplaySettings.DEFAULT
    
    // Get all StockDefinition entities from GraphRepository
    var allStockItems by remember { mutableStateOf<List<StockDefinitionWithManufacturer>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val stockDefinitions = graphRepository.getEntitiesByType("stock_definition")
        android.util.Log.d("CatalogBrowser", "Found ${stockDefinitions.size} stock definition entities")
        
        allStockItems = stockDefinitions.mapNotNull { entity ->
            if (entity is StockDefinition) {
                val manufacturerId = entity.getProperty<String>("manufacturerId") ?: entity.getProperty<String>("catalogSource") ?: "unknown"
                val manufacturerName = when (manufacturerId.lowercase()) {
                    "bambu" -> "Bambu Lab"
                    else -> manufacturerId.replaceFirstChar { it.uppercase() }
                }
                
                StockDefinitionWithManufacturer(
                    stockDefinition = entity,
                    manufacturerId = manufacturerId,
                    manufacturerName = manufacturerName,
                    hasRfidMapping = entity.getProperty<String>("rfidMappingKey") != null,
                    temperatureProfile = null // We'll simplify this for now
                )
            } else null
        }.sortedWith(
            compareBy<StockDefinitionWithManufacturer> { it.manufacturerName }
                .thenBy { it.stockDefinition.getProperty<String>("materialType") }
                .thenBy { it.stockDefinition.getProperty<String>("displayName") }
        )
    }
    
    // Group stock items based on groupByOption
    val filteredGroupedAndSortedStockItems = remember(allStockItems, groupByOption) {
        when (groupByOption) {
            GroupByOption.NONE -> allStockItems.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> allStockItems.groupBy { 
                it.stockDefinition.getProperty<String>("colorName") ?: "Unknown Color"
            }.toList()
            GroupByOption.BASE_MATERIAL -> allStockItems.groupBy { 
                val materialType = it.stockDefinition.getProperty<String>("materialType") ?: "Unknown"
                materialType.split(" ").firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> allStockItems.groupBy { 
                val materialType = it.stockDefinition.getProperty<String>("materialType") ?: "Unknown"
                val parts = materialType.split(" ")
                if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Flatten the grouped stock items into a single list with headers and items
            filteredGroupedAndSortedStockItems.forEach { (groupKey, groupStockItems) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupStockItems.size)
                    }
                }
                
                // Add each stock item as individual items
                groupStockItems.forEach { stockItemInfo ->
                    val sku = stockItemInfo.stockDefinition.getProperty<String>("sku")
                    // Use entity ID as fallback to ensure unique keys even when SKU is null
                    val uniqueKey = if (sku != null) {
                        "${stockItemInfo.manufacturerId}_${sku}"
                    } else {
                        "${stockItemInfo.manufacturerId}_${stockItemInfo.stockDefinition.id}"
                    }
                    item(key = uniqueKey) {
                        StockDefinitionCard(
                            stockItemInfo = stockItemInfo,
                            catalogDisplayMode = catalogDisplayMode,
                            materialDisplaySettings = materialDisplaySettings,
                            onClick = {
                                if (sku != null) {
                                    onNavigateToDetails?.invoke(
                                        DetailType.SKU, 
                                        "${stockItemInfo.manufacturerId}:${sku}"
                                    )
                                } else {
                                    // Fallback to entity ID if no SKU
                                    onNavigateToDetails?.invoke(
                                        DetailType.COMPONENT, 
                                        stockItemInfo.stockDefinition.id
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stock definition information combined with manufacturer details
 */
data class StockDefinitionWithManufacturer(
    val stockDefinition: StockDefinition,
    val manufacturerId: String,
    val manufacturerName: String,
    val hasRfidMapping: Boolean,
    val temperatureProfile: TemperatureProfile?
)






/**
 * Individual stock definition card
 */
@Composable
fun StockDefinitionCard(
    stockItemInfo: StockDefinitionWithManufacturer,
    catalogDisplayMode: CatalogDisplayMode,
    materialDisplaySettings: MaterialDisplaySettings,
    onClick: () -> Unit
) {
    val stockDefinition = stockItemInfo.stockDefinition
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            FilamentColorBox(
                colorHex = stockDefinition.getProperty<String>("colorHex") ?: "#808080",
                filamentType = stockDefinition.getProperty<String>("materialType") ?: "Unknown",
                materialDisplaySettings = materialDisplaySettings,
                modifier = Modifier.size(40.dp)
            )
            
            // Stock definition information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title based on catalog display mode
                Text(
                    text = when (catalogDisplayMode) {
                        CatalogDisplayMode.COMPLETE_TITLE -> 
                            stockDefinition.getProperty<String>("displayName") ?: stockDefinition.label
                        CatalogDisplayMode.COLOR_FOCUSED -> 
                            stockDefinition.getProperty<String>("colorName") ?: "Unknown Color"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Properties based on display mode
                when (catalogDisplayMode) {
                    CatalogDisplayMode.COMPLETE_TITLE -> {
                        stockDefinition.getProperty<String>("sku")?.let { sku ->
                            Text(
                                text = "SKU: $sku",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    CatalogDisplayMode.COLOR_FOCUSED -> {
                        val materialType = stockDefinition.getProperty<String>("materialType")
                        val sku = stockDefinition.getProperty<String>("sku")
                        val subtitleText = if (materialType != null && sku != null) {
                            "$materialType • SKU: $sku"
                        } else if (sku != null) {
                            "SKU: $sku"
                        } else {
                            "No SKU"
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Temperature info if available
                stockItemInfo.temperatureProfile?.let { profile ->
                    Text(
                        text = "${profile.minNozzle}-${profile.maxNozzle}°C • Bed: ${profile.bed}°C",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Status indicators
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // RFID mapping status
                Icon(
                    imageVector = if (stockItemInfo.hasRfidMapping) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.Error
                    },
                    contentDescription = if (stockItemInfo.hasRfidMapping) {
                        "Has RFID mapping"
                    } else {
                        "No RFID mapping"
                    },
                    tint = if (stockItemInfo.hasRfidMapping) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                // Material category badge
                stockDefinition.getProperty<Boolean>("consumable")?.let { isConsumable ->
                    Badge(
                        containerColor = if (isConsumable) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        }
                    ) {
                        Text(
                            text = if (isConsumable) "CONSUMABLE" else "REUSABLE",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

