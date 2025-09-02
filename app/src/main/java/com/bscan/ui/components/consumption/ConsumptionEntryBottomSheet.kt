package com.bscan.ui.components.consumption

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.graph.entities.InventoryItem
import com.bscan.service.*

/**
 * Bottom sheet for quick consumption entry supporting both individual and composite measurement modes.
 * 
 * PLACEHOLDER IMPLEMENTATION - Simplified for testing purposes.
 * Full implementation requires proper BottomSheet components from Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsumptionEntryBottomSheet(
    availableEntities: List<InventoryItem>,
    unitConversionService: UnitConversionService,
    onIndividualConsumption: (InventoryItem, Float, String?, String?) -> Unit,
    onCompositeConsumption: (String, Float, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // PLACEHOLDER: Simple Card layout for testing
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Consumption Entry",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            // Entity info
            Text(
                text = "Available Entities: ${availableEntities.size}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "Individual consumables: ${availableEntities.count { it.isConsumable }}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Composite entities: ${availableEntities.count { !it.isConsumable }}",
                style = MaterialTheme.typography.bodySmall
            )
            
            // Sample buttons for testing
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        availableEntities.firstOrNull()?.let { entity ->
                            onIndividualConsumption(entity, 100f, "grams", "Test consumption")
                        }
                    },
                    enabled = availableEntities.isNotEmpty()
                ) {
                    Text("Test Individual")
                }
                
                Button(
                    onClick = { 
                        onCompositeConsumption("test_composite", 950f, "Test composite consumption")
                    }
                ) {
                    Text("Test Composite")
                }
            }
        }
    }
}

/**
 * Entry modes for consumption tracking
 */
enum class ConsumptionEntryMode {
    INDIVIDUAL,  // Direct consumption of individual entities
    COMPOSITE    // Composite measurement with distribution
}