package com.bscan.ui.components.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import java.time.LocalDateTime

/**
 * Card displaying a child component in the inventory hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryChildComponentCard(
    component: Component,
    allComponents: List<Component>,
    depth: Int = 0,
    onDeleteComponent: (Component) -> Unit,
    modifier: Modifier = Modifier
) {
    val childComponents = allComponents.filter { it.parentComponentId == component.id }
    val indentSize = (depth * 16).dp
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = getCategoryColor(component.category, MaterialTheme.colorScheme)
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentSize)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Hierarchy indicator
                    if (depth > 0) {
                        Box(
                            modifier = Modifier
                                .size(2.dp, 20.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                    
                    // Component icon
                    Icon(
                        getComponentIcon(component.category),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Category chip
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    component.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            // Variable mass indicator
                            if (component.variableMass) {
                                Icon(
                                    Icons.AutoMirrored.Filled.TrendingDown,
                                    contentDescription = "Variable mass",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // Inferred mass indicator
                            if (component.inferredMass) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = "Inferred mass",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                
                // Mass and actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mass display
                    Column(horizontalAlignment = Alignment.End) {
                        component.massGrams?.let { mass ->
                            Text(
                                text = "${String.format("%.1f", mass)}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Show remaining percentage for variable mass
                            component.getRemainingPercentage()?.let { percentage ->
                                Text(
                                    text = "${(percentage * 100).toInt()}% remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        percentage < 0.05f -> MaterialTheme.colorScheme.error
                                        percentage < 0.20f -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        } ?: Text(
                            text = "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = { onDeleteComponent(component) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Remove component",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Show identifiers
            if (component.identifiers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(component.identifiers.take(3)) { identifier ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "${identifier.type.name}: ${formatComponentId(identifier.value)}",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Recursively display child components
            if (childComponents.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    childComponents.forEach { childComponent ->
                        InventoryChildComponentCard(
                            component = childComponent,
                            allComponents = allComponents,
                            depth = depth + 1,
                            onDeleteComponent = onDeleteComponent,
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}


