package com.bscan.ui.screens.spool

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
import androidx.compose.ui.unit.dp
import com.bscan.model.Component
import com.bscan.model.IdentifierType
import com.bscan.model.IdentifierPurpose
import com.bscan.ui.components.ColorPreviewCard
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

@Composable
fun FilamentReelStatisticsCard(
    filamentComponents: List<Component>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Collection Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val totalFilamentComponents = filamentComponents.size
            val uniqueTypes = filamentComponents.map { 
                it.metadata["filamentType"] ?: it.category 
            }.toSet().size
            val uniqueManufacturers = filamentComponents.map { it.manufacturer }.toSet().size
            val variableMassComponents = filamentComponents.count { it.variableMass }
            
            val statistics = listOf(
                "Components" to totalFilamentComponents.toString(),
                "Types" to uniqueTypes.toString(),
                "Manufacturers" to uniqueManufacturers.toString(),
                "Variable Mass" to variableMassComponents.toString()
            )
            
            StatisticGrid(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Most recently updated component info
            filamentComponents.maxByOrNull { it.lastUpdated }?.let { mostRecent ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Most recent update: ${mostRecent.lastUpdated.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun FilamentReelFilterSection(
    selectedFilter: String,
    onFilterChanged: (String) -> Unit,
    selectedTypeFilter: String,
    onTypeFilterChanged: (String) -> Unit,
    availableTypes: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Success rate filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Successful Only", "High Success Rate").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter) }
                )
            }
        }
        
        // Type filter chips
        if (availableTypes.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(availableTypes) { type ->
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { onTypeFilterChanged(type) },
                        label = { Text(type) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentReelCard(
    component: Component,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { 
            onClick?.invoke(component.getPrimaryTrackingIdentifier()?.value ?: component.id) 
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview with name
            ColorPreviewCard(
                colorHex = component.metadata["colorHex"] ?: "#808080",
                colorName = component.metadata["colorName"] ?: "Unknown Color",
                filamentType = component.metadata["filamentType"] ?: component.category,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Filament type information
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Filament Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = component.metadata["detailedFilamentType"]?.ifEmpty { 
                            component.metadata["filamentType"] ?: component.category 
                        } ?: (component.metadata["filamentType"] ?: component.category),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    val detailedType = component.metadata["detailedFilamentType"]
                    val basicType = component.metadata["filamentType"] ?: component.category
                    if (!detailedType.isNullOrEmpty() && detailedType != basicType) {
                        Text(
                            text = basicType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Component information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Mass information
                Column {
                    Text(
                        text = "Mass Information",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    component.massGrams?.let { mass ->
                        Text(
                            text = "${mass.toInt()}g current",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        component.fullMassGrams?.let { fullMass ->
                            val percentage = ((mass / fullMass) * 100).toInt()
                            Text(
                                text = "$percentage% remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    percentage >= 80 -> MaterialTheme.colorScheme.primary
                                    percentage >= 30 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    } ?: run {
                        Text(
                            text = if (component.variableMass) "Variable mass" else "Fixed component",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (component.inferredMass) "Mass inferred" else "Mass unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Last updated
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = component.lastUpdated.format(DateTimeFormatter.ofPattern("MMM dd")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = component.lastUpdated.format(DateTimeFormatter.ofPattern("yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Identifier information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Primary Identifier",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val primaryId = component.getPrimaryTrackingIdentifier() 
                        ?: component.identifiers.firstOrNull()
                    Text(
                        text = primaryId?.value ?: component.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    primaryId?.let { id ->
                        Text(
                            text = "${id.type.name.replace("_", " ").lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status indicator based on component properties
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                ) {
                    when {
                        component.isNearlyEmpty -> Icon(
                            Icons.Default.Error,
                            contentDescription = "Nearly Empty",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        component.isRunningLow -> Icon(
                            Icons.Default.Warning,
                            contentDescription = "Running Low",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        component.hasUniqueIdentifier() -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Identified Component",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            Icons.Default.Help,
                            contentDescription = "Unknown Component",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilamentReelListEmptyState(
    hasFilamentComponents: Boolean,
    currentFilter: String,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, icon) = if (hasFilamentComponents) {
        when (currentFilter) {
            "Running Low" -> Triple(
                "No components running low", 
                "Components with less than 20% remaining will appear here",
                Icons.Default.FilterList
            )
            "Nearly Empty" -> Triple(
                "No nearly empty components",
                "Components with less than 5% remaining will appear here",
                Icons.Default.FilterList
            )
            else -> Triple(
                "No filament components match your filters",
                "Try adjusting your filters", 
                Icons.Default.FilterList
            )
        }
    } else {
        Triple(
            "No filament components in your collection yet",
            "Add components through scanning or manual entry",
            Icons.Default.Inventory
        )
    }
    
    EmptyStateView(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}