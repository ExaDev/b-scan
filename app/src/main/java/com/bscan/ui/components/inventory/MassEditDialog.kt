package com.bscan.ui.components.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bscan.logic.WeightUnit
import com.bscan.model.PhysicalComponent
import java.text.DecimalFormat

@Composable
fun MassEditDialog(
    component: PhysicalComponent? = null, // null for total mass editing
    currentTotalMass: Float?,
    allComponents: List<PhysicalComponent>,
    preferredWeightUnit: WeightUnit,
    onConfirm: (EditMassResult) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditingTotal = component == null
    val title = if (isEditingTotal) "Edit Total Mass" else "Edit ${component?.name} Mass"
    
    var massInput by remember { 
        mutableStateOf(
            if (isEditingTotal) {
                currentTotalMass?.toString() ?: ""
            } else {
                component?.massGrams?.toString() ?: ""
            }
        )
    }
    
    var fullMassInput by remember {
        mutableStateOf(
            if (!isEditingTotal && component?.variableMass == true) {
                component.fullMassGrams?.toString() ?: ""
            } else {
                ""
            }
        )
    }
    
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Calculate what the other values would be
    val previewCalculation = remember(massInput, fullMassInput, allComponents) {
        val newMass = massInput.toFloatOrNull()
        if (newMass == null || newMass < 0) {
            null
        } else {
            calculateMassPreview(
                newMass = newMass,
                newFullMass = if (!isEditingTotal && component?.variableMass == true) 
                    fullMassInput.toFloatOrNull() else null,
                isEditingTotal = isEditingTotal,
                targetComponent = component,
                allComponents = allComponents,
                currentTotalMass = currentTotalMass
            )
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (!isEditingTotal && component != null) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Mass input
                OutlinedTextField(
                    value = massInput,
                    onValueChange = { 
                        massInput = it
                        isError = false
                        errorMessage = ""
                    },
                    label = { 
                        Text(
                            if (isEditingTotal) "Total Mass" 
                            else if (component?.variableMass == true) "Current Mass"
                            else "Mass"
                        )
                    },
                    suffix = { 
                        Text(
                            text = when (preferredWeightUnit) {
                                WeightUnit.GRAMS -> "g"
                                WeightUnit.KILOGRAMS -> "kg"
                                WeightUnit.POUNDS -> "lb"
                                WeightUnit.OUNCES -> "oz"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(errorMessage) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Full mass input for variable components
                if (!isEditingTotal && component?.variableMass == true) {
                    OutlinedTextField(
                        value = fullMassInput,
                        onValueChange = { fullMassInput = it },
                        label = { Text("Full Mass (Optional)") },
                        suffix = { 
                            Text(
                                text = when (preferredWeightUnit) {
                                    WeightUnit.GRAMS -> "g"
                                    WeightUnit.KILOGRAMS -> "kg"
                                    WeightUnit.POUNDS -> "lb"
                                    WeightUnit.OUNCES -> "oz"
                                }
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        placeholder = { Text("Leave blank to keep current") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Preview calculations
                previewCalculation?.let { preview ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Preview:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (isEditingTotal) {
                                Text(
                                    text = "Variable components will be updated proportionally",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                preview.updatedComponents.filter { it.variableMass }.forEach { updatedComponent ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${updatedComponent.name}:",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = formatWeight(updatedComponent.massGrams, preferredWeightUnit),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "New total mass: ${formatWeight(preview.newTotalMass, preferredWeightUnit)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                
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
                            val newMass = massInput.toFloatOrNull()
                            val newFullMass = if (!isEditingTotal && component?.variableMass == true) 
                                fullMassInput.toFloatOrNull() else null
                            
                            when {
                                newMass == null -> {
                                    isError = true
                                    errorMessage = "Please enter a valid mass"
                                }
                                newMass < 0 -> {
                                    isError = true
                                    errorMessage = "Mass cannot be negative"
                                }
                                !isEditingTotal && component?.variableMass == true && 
                                newFullMass != null && newMass > newFullMass -> {
                                    isError = true
                                    errorMessage = "Current mass cannot exceed full mass"
                                }
                                else -> {
                                    onConfirm(
                                        EditMassResult(
                                            isEditingTotal = isEditingTotal,
                                            targetComponent = component,
                                            newMass = newMass,
                                            newFullMass = newFullMass,
                                            previewCalculation = previewCalculation
                                        )
                                    )
                                }
                            }
                        },
                        enabled = previewCalculation != null
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

data class EditMassResult(
    val isEditingTotal: Boolean,
    val targetComponent: PhysicalComponent?,
    val newMass: Float,
    val newFullMass: Float?,
    val previewCalculation: MassPreviewResult?
)

data class MassPreviewResult(
    val newTotalMass: Float,
    val updatedComponents: List<PhysicalComponent>
)

private fun calculateMassPreview(
    newMass: Float,
    newFullMass: Float?,
    isEditingTotal: Boolean,
    targetComponent: PhysicalComponent?,
    allComponents: List<PhysicalComponent>,
    currentTotalMass: Float?
): MassPreviewResult? {
    return try {
        if (isEditingTotal) {
            // User is editing total mass - distribute to variable components
            val fixedMass = allComponents.filter { !it.variableMass }
                .sumOf { it.massGrams.toDouble() }.toFloat()
            val availableForVariable = newMass - fixedMass
            
            if (availableForVariable < 0) return null
            
            val variableComponents = allComponents.filter { it.variableMass }
            if (variableComponents.isEmpty()) {
                MassPreviewResult(
                    newTotalMass = newMass,
                    updatedComponents = allComponents
                )
            } else {
                val currentVariableMass = variableComponents.sumOf { it.massGrams.toDouble() }.toFloat()
                val ratio = if (currentVariableMass > 0) availableForVariable / currentVariableMass else 1f
                
                val updatedComponents = allComponents.map { component ->
                    if (component.variableMass) {
                        component.withUpdatedMass(component.massGrams * ratio)
                    } else {
                        component
                    }
                }
                
                MassPreviewResult(
                    newTotalMass = newMass,
                    updatedComponents = updatedComponents
                )
            }
        } else {
            // User is editing individual component mass
            if (targetComponent == null) return null
            
            val updatedComponents = allComponents.map { component ->
                if (component.id == targetComponent.id) {
                    var updated = component.withUpdatedMass(newMass)
                    if (newFullMass != null && component.variableMass) {
                        updated = updated.withUpdatedFullMass(newFullMass)
                    }
                    updated
                } else {
                    component
                }
            }
            
            val newTotalMass = updatedComponents.sumOf { it.massGrams.toDouble() }.toFloat()
            
            MassPreviewResult(
                newTotalMass = newTotalMass,
                updatedComponents = updatedComponents
            )
        }
    } catch (e: Exception) {
        null
    }
}

private fun formatWeight(weightGrams: Float, unit: WeightUnit): String {
    return com.bscan.logic.MassCalculationService().formatWeight(weightGrams, unit)
}