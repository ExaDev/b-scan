package com.bscan.ui.components.inventory

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import com.bscan.ui.screens.DetailType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Card displaying an individual inventory item with expandable child components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemCard(
    inventoryItem: Component,
    allComponents: List<Component>,
    isExpanded: Boolean,
    onToggleExpanded: (String) -> Unit,
    onDeleteComponent: (Component) -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    val childComponents = allComponents.filter { it.parentComponentId == inventoryItem.id }
    val totalMass = calculateTotalMass(inventoryItem, allComponents)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                inventoryItem.getPrimaryTrackingIdentifier()?.let { identifier ->
                    onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, identifier.value)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(300)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with component info and expand/collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        getComponentIcon(inventoryItem.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = inventoryItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Show primary identifier
                        inventoryItem.getPrimaryTrackingIdentifier()?.let { identifier ->
                            Text(
                                text = "${identifier.type.name}: ${formatComponentId(identifier.value)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand/collapse button
                    if (childComponents.isNotEmpty()) {
                        IconButton(
                            onClick = { onToggleExpanded(inventoryItem.id) }
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = { onDeleteComponent(inventoryItem) },
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
            }
            
            // Component metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manufacturer: ${inventoryItem.manufacturer}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show tags
                    if (inventoryItem.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(inventoryItem.tags) { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            tag, 
                                            style = MaterialTheme.typography.labelSmall
                                        ) 
                                    },
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${childComponents.size} components",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Mass information
            if (totalMass != null) {
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
                        text = "${String.format("%.1f", totalMass)}g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Expanded child components
            if (isExpanded && childComponents.isNotEmpty()) {
                HorizontalDivider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Child Components",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    childComponents.forEach { childComponent ->
                        InventoryChildComponentCard(
                            component = childComponent,
                            allComponents = allComponents,
                            depth = 1,
                            onDeleteComponent = onDeleteComponent
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
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inventoryItem.lastUpdated.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Show description if available
                if (inventoryItem.description.isNotBlank()) {
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
                                imageVector = Icons.Default.Description,
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



