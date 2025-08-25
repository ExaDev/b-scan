package com.bscan.ui.components.weight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.logic.WeightCalculationService
import com.bscan.logic.WeightUnit
import java.time.LocalDateTime

/**
 * Dialog for setting expected filament weight without scales by selecting components
 * and entering the expected filament weight to calculate total spool weight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpectedWeightInputDialog(
    trayUid: String,
    components: List<SpoolComponent>,
    preferredWeightUnit: WeightUnit,
    onWeightConfigured: (FilamentWeightMeasurement, String) -> Unit, // measurement + config ID
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expectedFilamentWeightText by remember { mutableStateOf("") }
    var hasCardboardCore by remember { mutableStateOf(true) }
    var hasReusableSpool by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    
    val calculationService = remember { WeightCalculationService() }
    
    // Find relevant components
    val cardboardCore = components.find { it.type == SpoolComponentType.CORE_RING }
    val reusableSpool = components.find { it.type == SpoolComponentType.BASE_SPOOL }
    
    // Calculate total weight and create configuration
    val calculationResult by remember(expectedFilamentWeightText, hasCardboardCore, hasReusableSpool) {
        derivedStateOf {
            val expectedWeight = expectedFilamentWeightText.toFloatOrNull()
            if (expectedWeight != null && expectedWeight > 0) {
                
                // Calculate spool weight from selected components
                var spoolWeight = 0f
                val selectedComponents = mutableListOf<String>()
                
                if (hasCardboardCore && cardboardCore != null) {
                    spoolWeight += cardboardCore.weightGrams
                    selectedComponents.add(cardboardCore.id)
                }
                
                if (hasReusableSpool && reusableSpool != null) {
                    spoolWeight += reusableSpool.weightGrams
                    selectedComponents.add(reusableSpool.id)
                }
                
                val totalWeight = expectedWeight + spoolWeight
                
                // Create a configuration for this combination
                val configName = when {
                    hasReusableSpool && hasCardboardCore -> "Bambu Spool"
                    hasCardboardCore -> "Bambu Refill"
                    else -> "Custom Configuration"
                }
                
                val configId = when {
                    hasReusableSpool && hasCardboardCore -> "bambu_spool_config"
                    hasCardboardCore -> "bambu_refill_config"
                    else -> "custom_${selectedComponents.joinToString("_")}"
                }
                
                ExpectedWeightResult(
                    totalWeight = totalWeight,
                    spoolWeight = spoolWeight,
                    expectedFilamentWeight = expectedWeight,
                    configurationName = configName,
                    configurationId = configId,
                    selectedComponents = selectedComponents,
                    isValid = true
                )
            } else {
                ExpectedWeightResult(
                    totalWeight = 0f,
                    spoolWeight = 0f,
                    expectedFilamentWeight = 0f,
                    configurationName = "",
                    configurationId = "",
                    selectedComponents = emptyList(),
                    isValid = false
                )
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Expected Weight") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set filament weight without scales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Expected filament weight input
                OutlinedTextField(
                    value = expectedFilamentWeightText,
                    onValueChange = { expectedFilamentWeightText = it },
                    label = { Text("Expected Filament Weight (${preferredWeightUnit.name.lowercase()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("e.g., 1000g for 1kg spool, 500g for 0.5kg spool")
                    }
                )
                
                // Component selection
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Spool Components",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Cardboard core checkbox
                        cardboardCore?.let { core ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = hasCardboardCore,
                                    onCheckedChange = { hasCardboardCore = it }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = core.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${core.weightGrams.toInt()}g - ${core.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Reusable spool checkbox
                        reusableSpool?.let { spool ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = hasReusableSpool,
                                    onCheckedChange = { hasReusableSpool = it }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = spool.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "${spool.weightGrams.toInt()}g - ${spool.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Calculation result display
                if (calculationResult.isValid) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Calculated Configuration",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "Type: ${calculationResult.configurationName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "Spool weight: ${calculationService.formatWeight(calculationResult.spoolWeight, preferredWeightUnit)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            Text(
                                text = "Total weight: ${calculationService.formatWeight(calculationResult.totalWeight, preferredWeightUnit)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    supportingText = {
                        Text("e.g., 'New spool', 'Estimated from packaging'")
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (calculationResult.isValid) {
                        // Create a synthetic measurement representing the full weight
                        val measurement = FilamentWeightMeasurement(
                            id = "expected_${System.currentTimeMillis()}",
                            trayUid = trayUid,
                            measuredWeightGrams = calculationResult.totalWeight,
                            spoolConfigurationId = calculationResult.configurationId,
                            measurementType = MeasurementType.FULL_WEIGHT,
                            measuredAt = LocalDateTime.now(),
                            notes = notes.ifEmpty { "Expected weight (no scale): ${calculationResult.expectedFilamentWeight}g filament + ${calculationResult.spoolWeight}g spool" }
                        )
                        
                        onWeightConfigured(measurement, calculationResult.configurationId)
                    }
                },
                enabled = calculationResult.isValid
            ) {
                Text("Set Weight")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Result of expected weight calculation
 */
private data class ExpectedWeightResult(
    val totalWeight: Float,
    val spoolWeight: Float,
    val expectedFilamentWeight: Float,
    val configurationName: String,
    val configurationId: String,
    val selectedComponents: List<String>,
    val isValid: Boolean
)