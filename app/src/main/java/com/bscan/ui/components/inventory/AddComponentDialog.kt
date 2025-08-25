package com.bscan.ui.components.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bscan.logic.WeightUnit
import com.bscan.model.Component

@Composable
fun AddComponentDialog(
    availableComponents: List<Component>,
    currentComponentIds: List<String>,
    preferredWeightUnit: WeightUnit,
    onAddComponent: (Component) -> Unit,
    onDismiss: () -> Unit
) {
    // Filter out components that are already added
    val selectableComponents = availableComponents.filter { component ->
        component.id !in currentComponentIds
    }
    
    var selectedComponent by remember { mutableStateOf<Component?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Get unique categories for filtering
    val categories = selectableComponents.map { it.category }.distinct().sorted()
    
    // Filter components based on search and category
    val filteredComponents = selectableComponents.filter { component ->
        val matchesSearch = searchQuery.isBlank() || 
                           component.name.contains(searchQuery, ignoreCase = true) ||
                           component.manufacturer.contains(searchQuery, ignoreCase = true) ||
                           component.category.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == null || component.category == selectedCategory
        matchesSearch && matchesCategory
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = "Add Component",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search components") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Category filters
                if (categories.isNotEmpty()) {
                    Text(
                        text = "Filter by category:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        item {
                            FilterChip(
                                onClick = { selectedCategory = null },
                                label = { Text("All") },
                                selected = selectedCategory == null
                            )
                        }
                        items(categories) { category ->
                            FilterChip(
                                onClick = { selectedCategory = if (selectedCategory == category) null else category },
                                label = { Text(category) },
                                selected = selectedCategory == category
                            )
                        }
                    }
                }
                
                // Components list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredComponents) { component ->
                        ComponentSelectionCard(
                            component = component,
                            isSelected = selectedComponent == component,
                            preferredWeightUnit = preferredWeightUnit,
                            onSelect = { selectedComponent = component },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    if (filteredComponents.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "No components found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isNotBlank() || selectedCategory != null) {
                                    Text(
                                        text = "Try adjusting your search or filters",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            selectedComponent?.let(onAddComponent)
                            onDismiss()
                        },
                        enabled = selectedComponent != null
                    ) {
                        Text("Add Component")
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentSelectionCard(
    component: Component,
    isSelected: Boolean,
    preferredWeightUnit: WeightUnit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category chip
                    AssistChip(
                        onClick = { },
                        label = { Text(component.category, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    
                    // Mass display
                    Text(
                        text = formatMass(component.massGrams, preferredWeightUnit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Manufacturer
                if (component.manufacturer.isNotBlank()) {
                    Text(
                        text = component.manufacturer,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Description
                if (component.description.isNotBlank()) {
                    Text(
                        text = component.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            
            // Variable mass indicator
            if (component.variableMass) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = "Variable mass",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatMass(massGrams: Float?, preferredUnit: WeightUnit): String {
    if (massGrams == null) return "Unknown mass"
    
    return when (preferredUnit) {
        WeightUnit.GRAMS -> "${String.format("%.1f", massGrams)}g"
        WeightUnit.KILOGRAMS -> "${String.format("%.3f", massGrams / 1000f)}kg"
        WeightUnit.OUNCES -> "${String.format("%.2f", massGrams * 0.035274f)}oz"
        WeightUnit.POUNDS -> "${String.format("%.3f", massGrams * 0.00220462f)}lbs"
    }
}