package com.bscan.ui.components.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.ui.components.common.StatisticGrid
import java.time.LocalDateTime

/**
 * Statistics card displaying inventory metrics and component breakdowns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryStatisticsCard(
    allComponents: List<Component>,
    inventoryItems: List<Component>,
    modifier: Modifier = Modifier
) {
    val totalInventoryItems = inventoryItems.size
    val totalComponents = allComponents.size
    val uniqueManufacturers = allComponents.map { it.manufacturer }.distinct().size
    
    // Count by categories
    val categoryStats = allComponents.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5) // Show top 5 categories
    
    // Count by tags
    val tagStats = allComponents.flatMap { it.tags }
        .groupBy { it }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(3) // Show top 3 tags
    
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
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Inventory Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            val mainStats = listOf(
                "Inventory Items" to totalInventoryItems.toString(),
                "Total Components" to totalComponents.toString(),
                "Manufacturers" to uniqueManufacturers.toString()
            )
            
            StatisticGrid(
                statistics = mainStats,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category breakdown
            if (categoryStats.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryStats) { (category, count) ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        getComponentIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "$category ($count)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            // Tag breakdown
            if (tagStats.isNotEmpty()) {
                Text(
                    text = "Popular Tags: ${tagStats.joinToString(", ") { "${it.first} (${it.second})" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryStatisticsCardPreview() {
    MaterialTheme {
        // Create mock components
        val mockComponents = listOf(
            Component(
                id = "comp1",
                name = "PLA Red Filament",
                category = "filament",
                tags = listOf("thermoplastic", "bambu"),
                manufacturer = "Bambu Lab"
            ),
            Component(
                id = "comp2", 
                name = "PETG Blue Filament",
                category = "filament",
                tags = listOf("thermoplastic", "transparent"),
                manufacturer = "Bambu Lab"
            ),
            Component(
                id = "comp3",
                name = "Tool A",
                category = "tool",
                tags = listOf("hardware"),
                manufacturer = "Generic"
            )
        )
        
        InventoryStatisticsCard(
            allComponents = mockComponents,
            inventoryItems = mockComponents.take(2),
            modifier = Modifier.padding(16.dp)
        )
    }
}



