package com.bscan.ui.components.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.logic.WeightUnit
import com.bscan.model.*
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryComponentsCard(
    inventoryComponent: Component,
    childComponents: List<Component>,
    preferredWeightUnit: WeightUnit,
    onEditComponentMass: (Component) -> Unit,
    onAddComponent: () -> Unit,
    onRemoveComponent: (Component) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Components (${childComponents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                FilledTonalButton(
                    onClick = onAddComponent,
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            if (childComponents.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No components yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add components to track their individual masses and properties",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Total mass summary
                val totalMass = childComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Mass",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatMass(totalMass, preferredWeightUnit),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Components list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(childComponents) { component ->
                        ComponentItemCard(
                            component = component,
                            preferredWeightUnit = preferredWeightUnit,
                            onEditMass = { onEditComponentMass(component) },
                            onRemove = { onRemoveComponent(component) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentItemCard(
    component: Component,
    preferredWeightUnit: WeightUnit,
    onEditMass: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Component icon based on category
            Icon(
                getCategoryIcon(component.category),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
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
                    // Category
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                component.category,
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        },
                        modifier = Modifier.height(20.dp)
                    )
                    
                    // Variable mass indicator
                    if (component.variableMass) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = "Variable mass",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                
                // Mass display
                Text(
                    text = formatMass(component.massGrams, preferredWeightUnit),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Full mass for variable components
                if (component.variableMass && component.fullMassGrams != null && component.fullMassGrams > 0f) {
                    val percentage = ((component.massGrams ?: 0f) / component.fullMassGrams * 100).toInt()
                    Text(
                        text = "Full mass: ${formatMass(component.fullMassGrams, preferredWeightUnit)} ($percentage% remaining)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (component.variableMass) {
                    IconButton(
                        onClick = onEditMass,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit mass",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onRemove,
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
    }
}

// Helper functions
private fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "filament" -> Icons.Default.Polymer
    "spool" -> Icons.Default.Circle
    "core" -> Icons.Outlined.Circle
    "adapter" -> Icons.Default.Transform
    "packaging" -> Icons.Default.LocalShipping
    "rfid-tag" -> Icons.Default.Sensors
    "filament-tray" -> Icons.Default.Inventory
    else -> Icons.Default.Category
}

private fun formatMass(massGrams: Float?, preferredUnit: WeightUnit): String {
    if (massGrams == null) return "Unknown"
    
    return when (preferredUnit) {
        WeightUnit.GRAMS -> "${String.format("%.1f", massGrams)}g"
        WeightUnit.KILOGRAMS -> "${String.format("%.3f", massGrams / 1000f)}kg"
        WeightUnit.OUNCES -> "${String.format("%.2f", massGrams * 0.035274f)}oz"
        WeightUnit.POUNDS -> "${String.format("%.3f", massGrams * 0.00220462f)}lbs"
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryComponentsCardEmptyPreview() {
    MaterialTheme {
        InventoryComponentsCard(
            inventoryComponent = createMockInventoryComponent(),
            childComponents = emptyList(),
            preferredWeightUnit = WeightUnit.GRAMS,
            onEditComponentMass = { },
            onAddComponent = { },
            onRemoveComponent = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryComponentsCardWithComponentsPreview() {
    MaterialTheme {
        InventoryComponentsCard(
            inventoryComponent = createMockInventoryComponent(),
            childComponents = createMockChildComponentList(),
            preferredWeightUnit = WeightUnit.GRAMS,
            onEditComponentMass = { },
            onAddComponent = { },
            onRemoveComponent = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentItemCardPreview() {
    MaterialTheme {
        ComponentItemCard(
            component = createMockFilamentComponent(),
            preferredWeightUnit = WeightUnit.GRAMS,
            onEditMass = { },
            onRemove = { }
        )
    }
}

// Mock data for preview
private fun createMockInventoryComponent(): Component {
    return Component(
        id = "inventory_main",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = "PLA Basic Filament Tray",
        category = "filament-tray",
        tags = listOf("PLA", "Orange", "1.75mm"),
        manufacturer = "Bambu Lab",
        description = "Complete filament tray system"
    )
}

private fun createMockChildComponentList(): List<Component> {
    return listOf(
        Component(
            id = "component_filament",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = "01008023456789AB",
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "PLA Filament",
            category = "filament",
            tags = listOf("PLA", "Orange"),
            parentComponentId = "inventory_main",
            massGrams = 165.5f,
            fullMassGrams = 330.0f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Remaining filament material"
        ),
        Component(
            id = "component_spool",
            name = "Empty Spool",
            category = "spool",
            parentComponentId = "inventory_main",
            massGrams = 245.0f,
            manufacturer = "Bambu Lab",
            description = "Reusable spool"
        ),
        Component(
            id = "component_rfid1",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = "A1B2C3D4",
                    purpose = IdentifierPurpose.AUTHENTICATION
                )
            ),
            name = "RFID Tag #1",
            category = "rfid-tag",
            parentComponentId = "inventory_main",
            massGrams = 2.1f,
            manufacturer = "Bambu Lab",
            description = "Primary authentication tag"
        ),
        Component(
            id = "component_rfid2",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = "E5F6G7H8",
                    purpose = IdentifierPurpose.AUTHENTICATION
                )
            ),
            name = "RFID Tag #2",
            category = "rfid-tag", 
            parentComponentId = "inventory_main",
            massGrams = 2.3f,
            manufacturer = "Bambu Lab",
            description = "Secondary authentication tag"
        )
    )
}

private fun createMockFilamentComponent(): Component {
    return Component(
        id = "component_filament_preview",
        identifiers = listOf(
            ComponentIdentifier(
                type = IdentifierType.CONSUMABLE_UNIT,
                value = "01008023456789AB",
                purpose = IdentifierPurpose.TRACKING
            )
        ),
        name = "PLA Filament",
        category = "filament",
        tags = listOf("PLA", "Orange", "Running Low"),
        parentComponentId = "parent_tray",
        massGrams = 48.2f,  // Low remaining mass
        fullMassGrams = 330.0f,  // Original full mass  
        variableMass = true,
        manufacturer = "Bambu Lab",
        description = "Premium PLA filament material",
        lastUpdated = LocalDateTime.now().minusHours(2)
    )
}