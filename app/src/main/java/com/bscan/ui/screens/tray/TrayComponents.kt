package com.bscan.ui.screens.tray

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.repository.InventoryData
import com.bscan.repository.InventoryStatistics
import com.bscan.ui.components.ColorPreviewDot
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

// Utility functions for inventory UID display
fun formatInventoryId(inventoryUid: String): String {
    // Inventory UID is already in hex format from BambuTagDecoder
    // Git-style: use last 8 characters of hex string
    return if (inventoryUid.length > 8) {
        inventoryUid.takeLast(8)
    } else {
        inventoryUid
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStatisticsCard(
    statistics: InventoryStatistics,
    modifier: Modifier = Modifier
) {
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
                    imageVector = Icons.Default.Info,
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
                "Total Items" to statistics.totalInventoryItems.toString(),
                "Unique Tags" to statistics.totalUniqueTags.toString(),
                "Total Scans" to statistics.totalScans.toString()
            )
            
            StatisticGrid(
                statistics = mainStats,
                modifier = Modifier.fillMaxWidth()
            )
            
            val secondaryStats = listOf(
                "Avg Tags/Item" to "%.1f".format(statistics.averageTagsPerItem),
                "Avg Scans/Item" to "%.1f".format(statistics.averageScansPerItem)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                secondaryStats.forEach { (label, value) ->
                    StatisticDisplay(
                        label = label,
                        value = value
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Most active item
            statistics.mostActiveItem?.let { item ->
                HorizontalDivider()
                Text(
                    text = "Most Active: ${formatInventoryId(item.inventoryUid)} (${item.totalScans} scans)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Item with most tags
            statistics.itemWithMostTags?.let { item ->
                Text(
                    text = "Most Tags: ${formatInventoryId(item.inventoryUid)} (${item.uniqueTagCount} unique tags)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryCard(
    inventoryData: InventoryData,
    onDeleteInventory: (InventoryData) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with inventory UID and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inventory UID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatInventoryId(inventoryData.inventoryUid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Full: ${inventoryData.inventoryUid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { onDeleteInventory(inventoryData) },
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
            
            // Statistics row
            val statistics = listOf(
                "Unique Tags" to inventoryData.uniqueTagCount.toString(),
                "Total Scans" to inventoryData.totalScans.toString(),
                "Filament Types" to inventoryData.filamentTypes.size.toString()
            )
            
            StatisticGrid(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Filament types and colors
            if (inventoryData.filamentTypes.isNotEmpty()) {
                Text(
                    text = "Filament Types:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = inventoryData.filamentTypes.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Colors preview
            if (inventoryData.colorNames.isNotEmpty()) {
                Text(
                    text = "Colors:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show color dots for unique filaments
                    inventoryData.tagEntries.values.take(8).forEach { tagEntry ->
                        ColorPreviewDot(
                            colorHex = tagEntry.filamentInfo.colorHex,
                            size = 24.dp
                        )
                    }
                    if (inventoryData.tagEntries.size > 8) {
                        Text(
                            text = "+${inventoryData.tagEntries.size - 8}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        text = "First Seen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inventoryData.firstSeen.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inventoryData.lastUpdated.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.Storage,
        title = "No Inventory Items",
        subtitle = "Scan some filament tags to start tracking inventory",
        modifier = modifier
    )
}