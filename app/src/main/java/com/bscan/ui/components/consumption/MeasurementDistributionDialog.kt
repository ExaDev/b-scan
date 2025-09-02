package com.bscan.ui.components.consumption

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bscan.model.graph.entities.DistributionMethod
import com.bscan.model.graph.entities.InventoryItem
import com.bscan.service.ConsumptionCalculation
import com.bscan.service.UnitConversionService
import com.bscan.service.WeightUnit
import kotlin.math.abs

/**
 * Dialog for entering composite measurements and distributing consumption across entities.
 * Supports bill-splitting-style distribution with user control and AI suggestions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementDistributionDialog(
    consumptionCalculation: ConsumptionCalculation,
    consumableEntities: List<InventoryItem>,
    unitConversionService: UnitConversionService,
    onConfirm: (Map<String, Float>, DistributionMethod, String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMethod by remember { mutableStateOf(DistributionMethod.PROPORTIONAL) }
    var userDistributions by remember { 
        mutableStateOf(
            consumableEntities.associate { it.id to 0f }
        )
    }
    var notes by remember { mutableStateOf("") }
    var isDistributionValid by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Calculate automatic distributions
    val automaticDistributions = remember(selectedMethod, consumableEntities, consumptionCalculation) {
        calculateDistributions(selectedMethod, consumableEntities, consumptionCalculation.totalConsumption)
    }
    
    // Update user distributions when method changes (except for USER_SPECIFIED)
    LaunchedEffect(selectedMethod) {
        if (selectedMethod != DistributionMethod.USER_SPECIFIED) {
            userDistributions = automaticDistributions
        }
    }
    
    // Validate distribution
    LaunchedEffect(userDistributions, consumptionCalculation.totalConsumption) {
        val totalDistributed = userDistributions.values.sum()
        val error = abs(totalDistributed - consumptionCalculation.totalConsumption)
        isDistributionValid = error <= 0.01f && userDistributions.values.all { it >= 0f }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Distribute Consumption",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${consumptionCalculation.totalConsumption}g to distribute",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                item {
                    // Measurement summary
                    ConsumptionSummaryCard(
                        calculation = consumptionCalculation,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    // Distribution method selector
                    DistributionMethodSelector(
                        selectedMethod = selectedMethod,
                        onMethodSelected = { selectedMethod = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                items(consumableEntities) { entity ->
                    ConsumableDistributionItem(
                        entity = entity,
                        currentDistribution = userDistributions[entity.id] ?: 0f,
                        totalConsumption = consumptionCalculation.totalConsumption,
                        automaticDistribution = automaticDistributions[entity.id] ?: 0f,
                        isUserEditable = selectedMethod == DistributionMethod.USER_SPECIFIED,
                        onDistributionChanged = { newValue ->
                            userDistributions = userDistributions.toMutableMap().apply {
                                put(entity.id, newValue)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    // Distribution validation
                    DistributionValidationCard(
                        totalDistributed = userDistributions.values.sum(),
                        targetTotal = consumptionCalculation.totalConsumption,
                        isValid = isDistributionValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    // Notes input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("Add notes about this measurement...") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                item {
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = {
                                val finalDistributions = if (selectedMethod == DistributionMethod.USER_SPECIFIED) {
                                    userDistributions
                                } else {
                                    automaticDistributions
                                }
                                onConfirm(finalDistributions, selectedMethod, notes.ifBlank { null })
                            },
                            enabled = isDistributionValid
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Distribution")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumptionSummaryCard(
    calculation: ConsumptionCalculation,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Measured Weight",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${calculation.measuredWeight}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Fixed Components",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${calculation.totalFixedMass}g",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total Consumption",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${calculation.totalConsumption}g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DistributionMethodSelector(
    selectedMethod: DistributionMethod,
    onMethodSelected: (DistributionMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Distribution Method",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DistributionMethod.values().forEach { method ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMethod == method,
                        onClick = { onMethodSelected(method) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = getMethodDisplayName(method),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = getMethodDescription(method),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsumableDistributionItem(
    entity: InventoryItem,
    currentDistribution: Float,
    totalConsumption: Float,
    automaticDistribution: Float,
    isUserEditable: Boolean,
    onDistributionChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingValue by remember { mutableStateOf(currentDistribution.toString()) }
    
    // Update editing value when distribution changes externally
    LaunchedEffect(currentDistribution) {
        if (!isUserEditable) {
            editingValue = currentDistribution.toString()
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(300))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entity.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Current: ${entity.currentQuantity}g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (isUserEditable) {
                    OutlinedTextField(
                        value = editingValue,
                        onValueChange = { newValue ->
                            editingValue = newValue
                            newValue.toFloatOrNull()?.let { onDistributionChanged(it) }
                        },
                        label = { Text("Amount (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(120.dp),
                        isError = editingValue.toFloatOrNull()?.let { it < 0 } ?: true
                    )
                } else {
                    Text(
                        text = "${currentDistribution}g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Distribution percentage and slider
            val percentage = if (totalConsumption > 0) (currentDistribution / totalConsumption) * 100 else 0f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${percentage.toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp)
                )
                
                LinearProgressIndicator(
                    progress = percentage / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                
                if (isUserEditable) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editable",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionValidationCard(
    totalDistributed: Float,
    targetTotal: Float,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    val error = totalDistributed - targetTotal
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isValid) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            
            Column {
                Text(
                    text = if (isValid) "Distribution Valid" else "Distribution Error",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when {
                        isValid -> "Total distributed: ${totalDistributed}g"
                        error > 0 -> "Over-distributed by ${error}g"
                        else -> "Under-distributed by ${abs(error)}g"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun calculateDistributions(
    method: DistributionMethod,
    entities: List<InventoryItem>,
    totalConsumption: Float
): Map<String, Float> {
    return when (method) {
        DistributionMethod.PROPORTIONAL -> {
            val totalQuantity = entities.sumOf { it.currentQuantity.toDouble() }.toFloat()
            if (totalQuantity > 0) {
                entities.associate { entity ->
                    val proportion = entity.currentQuantity / totalQuantity
                    entity.id to (totalConsumption * proportion)
                }
            } else {
                // Fall back to equal split if no quantities
                val splitAmount = totalConsumption / entities.size
                entities.associate { it.id to splitAmount }
            }
        }
        DistributionMethod.EQUAL_SPLIT -> {
            val splitAmount = totalConsumption / entities.size
            entities.associate { it.id to splitAmount }
        }
        else -> {
            // For other methods, default to proportional
            calculateDistributions(DistributionMethod.PROPORTIONAL, entities, totalConsumption)
        }
    }
}

private fun getMethodDisplayName(method: DistributionMethod): String {
    return when (method) {
        DistributionMethod.PROPORTIONAL -> "Proportional"
        DistributionMethod.EQUAL_SPLIT -> "Equal Split"
        DistributionMethod.USER_SPECIFIED -> "Manual"
        DistributionMethod.WEIGHTED -> "Smart Weighted"
        DistributionMethod.INFERRED -> "AI Suggested"
    }
}

private fun getMethodDescription(method: DistributionMethod): String {
    return when (method) {
        DistributionMethod.PROPORTIONAL -> "Based on current quantities"
        DistributionMethod.EQUAL_SPLIT -> "Split equally between all items"
        DistributionMethod.USER_SPECIFIED -> "You specify each amount"
        DistributionMethod.WEIGHTED -> "Based on usage patterns"
        DistributionMethod.INFERRED -> "AI-powered suggestion"
    }
}