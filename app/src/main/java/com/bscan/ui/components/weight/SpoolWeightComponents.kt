package com.bscan.ui.components.weight

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.logic.WeightCalculationService
import com.bscan.logic.WeightUnit

/**
 * Card component for managing spool weight settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolWeightSettingsCard(
    presets: List<SpoolWeightPreset>,
    configurations: List<SpoolConfiguration>,
    components: List<SpoolComponent>,
    onPresetSelected: (SpoolWeightPreset) -> Unit,
    onEditPreset: (SpoolWeightPreset) -> Unit,
    onResetPreset: (String) -> Unit,
    onCreateCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Spool Weight Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Configure dry spool weights and packaging to calculate actual filament mass",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Preset Templates Section
            Text(
                text = "Preset Templates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(presets) { preset ->
                    SpoolWeightPresetItem(
                        preset = preset,
                        configuration = configurations.find { it.id == preset.configurationId },
                        components = components,
                        onSelect = { onPresetSelected(preset) },
                        onEdit = { onEditPreset(preset) },
                        onReset = if (preset.isFactory && preset.isModified) {
                            { onResetPreset(preset.id) }
                        } else null
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Create Custom Button
            OutlinedButton(
                onClick = onCreateCustom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom Configuration")
            }
        }
    }
}

/**
 * Individual preset item with actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolWeightPresetItem(
    preset: SpoolWeightPreset,
    configuration: SpoolConfiguration?,
    components: List<SpoolComponent>,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onReset: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val calculationService = remember { WeightCalculationService() }
    val totalWeight = configuration?.let { 
        calculationService.calculateTotalSpoolWeight(it, components) 
    } ?: 0f
    
    Card(
        onClick = onSelect,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (preset.isModified) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (preset.isModified) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Modified",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Text(
                        text = "${String.format("%.0f", totalWeight)}g dry weight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Supports: ${preset.supportedCapacities.joinToString { "${it}kg" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Settings, contentDescription = "Edit")
                    }
                    
                    onReset?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset to default")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Weight input dialog for recording measurements
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightInputDialog(
    trayUid: String,
    availableConfigurations: List<SpoolConfiguration>,
    components: List<SpoolComponent>,
    preferredWeightUnit: WeightUnit,
    onMeasurementSaved: (FilamentWeightMeasurement) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var weightText by remember { mutableStateOf("") }
    var selectedConfigId by remember { mutableStateOf(availableConfigurations.firstOrNull()?.id ?: "") }
    var measurementType by remember { mutableStateOf(MeasurementType.FULL_WEIGHT) }
    var notes by remember { mutableStateOf("") }
    
    val calculationService = remember { WeightCalculationService() }
    
    // Calculate results in real-time
    val calculationResult by remember(weightText, selectedConfigId, measurementType) {
        derivedStateOf {
            val weight = weightText.toFloatOrNull()
            val config = availableConfigurations.find { it.id == selectedConfigId }
            
            if (weight != null && weight > 0 && config != null) {
                calculationService.calculateFilamentWeight(weight, config, components, measurementType)
            } else null
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Weight Measurement") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Weight input
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Measured Weight (${preferredWeightUnit.name.lowercase()})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Measurement type
                Text(
                    text = "Measurement Type",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = measurementType == MeasurementType.FULL_WEIGHT,
                            onClick = { measurementType = MeasurementType.FULL_WEIGHT }
                        )
                        Text("With filament (total weight)")
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = measurementType == MeasurementType.EMPTY_WEIGHT,
                            onClick = { measurementType = MeasurementType.EMPTY_WEIGHT }
                        )
                        Text("Empty spool only")
                    }
                }
                
                // Configuration selection
                if (availableConfigurations.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = false,
                        onExpandedChange = { }
                    ) {
                        OutlinedTextField(
                            value = availableConfigurations.find { it.id == selectedConfigId }?.name ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Spool Configuration") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        
                        ExposedDropdownMenu(
                            expanded = false,
                            onDismissRequest = { }
                        ) {
                            availableConfigurations.forEach { config ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(config.name)
                                            val weight = calculationService.calculateTotalSpoolWeight(config, components)
                                            Text(
                                                "${String.format("%.0f", weight)}g dry weight",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    },
                                    onClick = { selectedConfigId = config.id }
                                )
                            }
                        }
                    }
                }
                
                // Calculation result display
                calculationResult?.let { result ->
                    if (result.success) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "Calculated Results",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (measurementType == MeasurementType.FULL_WEIGHT) {
                                    Text(
                                        text = "Filament weight: ${calculationService.formatWeight(result.filamentWeightGrams, preferredWeightUnit)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                Text(
                                    text = "Spool weight: ${calculationService.formatWeight(result.spoolWeightGrams, preferredWeightUnit)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = result.errorMessage ?: "Calculation error",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
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
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    if (weight != null && weight > 0 && selectedConfigId.isNotEmpty()) {
                        val measurement = FilamentWeightMeasurement(
                            id = "measurement_${System.currentTimeMillis()}",
                            trayUid = trayUid,
                            measuredWeightGrams = weight,
                            spoolConfigurationId = selectedConfigId,
                            measurementType = measurementType,
                            measuredAt = java.time.LocalDateTime.now(),
                            notes = notes
                        )
                        onMeasurementSaved(measurement)
                    }
                },
                enabled = calculationResult?.success == true
            ) {
                Text("Save")
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
 * Display component for showing weight measurements
 */
@Composable
fun WeightMeasurementDisplay(
    measurement: FilamentWeightMeasurement,
    spoolConfiguration: SpoolConfiguration?,
    components: List<SpoolComponent>,
    weightUnit: WeightUnit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calculationService = remember { WeightCalculationService() }
    
    val calculationResult = remember(measurement, spoolConfiguration) {
        if (spoolConfiguration != null) {
            calculationService.calculateFilamentWeight(
                measurement.measuredWeightGrams,
                spoolConfiguration,
                components,
                measurement.measurementType
            )
        } else null
    }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weight Measurement",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        text = "Measured: ${calculationService.formatWeight(measurement.measuredWeightGrams, weightUnit)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    calculationResult?.let { result ->
                        if (result.success && measurement.measurementType == MeasurementType.FULL_WEIGHT) {
                            Text(
                                text = "Filament: ${calculationService.formatWeight(result.filamentWeightGrams, weightUnit)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Text(
                        text = "Config: ${spoolConfiguration?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (measurement.notes.isNotEmpty()) {
                        Text(
                            text = measurement.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}