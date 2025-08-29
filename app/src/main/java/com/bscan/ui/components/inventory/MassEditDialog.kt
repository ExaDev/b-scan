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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bscan.logic.WeightUnit
import com.bscan.model.Component
import java.text.DecimalFormat

@Composable
fun MassEditDialog(
    component: Component? = null, // null for total mass editing
    currentTotalMass: Float?,
    allComponents: List<Component>,
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
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val parsedMass = massInput.toFloatOrNull()
    val parsedFullMass = fullMassInput.toFloatOrNull()
    
    // Validation
    LaunchedEffect(massInput, fullMassInput) {
        errorMessage = when {
            massInput.isBlank() -> "Mass is required"
            parsedMass == null -> "Enter a valid number"
            parsedMass <= 0 -> "Mass must be greater than 0"
            !isEditingTotal && component?.variableMass == true && fullMassInput.isNotBlank() -> {
                when {
                    parsedFullMass == null -> "Enter a valid full mass"
                    parsedFullMass <= 0 -> "Full mass must be greater than 0"
                    parsedMass > parsedFullMass -> "Current mass cannot exceed full mass"
                    else -> null
                }
            }
            else -> null
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Component info (if editing specific component)
                if (!isEditingTotal) {
                    // component is guaranteed to be non-null when !isEditingTotal
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = component.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Category: ${component.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (component.variableMass) {
                                Text(
                                    text = "Variable mass component",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
                
                // Current mass input
                OutlinedTextField(
                    value = massInput,
                    onValueChange = { massInput = it },
                    label = { 
                        Text(
                            if (isEditingTotal) "Total Mass" else "Current Mass"
                        )
                    },
                    placeholder = { Text("Enter mass in grams") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Mass in grams")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Full mass input (for variable mass components only)
                if (!isEditingTotal && component?.variableMass == true) {
                    OutlinedTextField(
                        value = fullMassInput,
                        onValueChange = { fullMassInput = it },
                        label = { Text("Full Mass (optional)") },
                        placeholder = { Text("Enter original full mass") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Original/maximum mass when new") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Mass conversion hints
                if (parsedMass != null && parsedMass > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Converted values:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = buildString {
                                    append("${formatMass(parsedMass, WeightUnit.KILOGRAMS)} • ")
                                    append("${formatMass(parsedMass, WeightUnit.OUNCES)} • ")
                                    append(formatMass(parsedMass, WeightUnit.POUNDS))
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (parsedMass != null && errorMessage == null) {
                                val result = EditMassResult(
                                    componentId = component?.id,
                                    newMass = parsedMass,
                                    newFullMass = if (!isEditingTotal && component?.variableMass == true) parsedFullMass else null,
                                    isEditingTotal = isEditingTotal
                                )
                                onConfirm(result)
                            }
                        },
                        enabled = parsedMass != null && parsedMass > 0 && errorMessage == null
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Result of mass editing operation
 */
data class EditMassResult(
    val componentId: String?, // null if editing total
    val newMass: Float,
    val newFullMass: Float? = null, // for variable mass components
    val isEditingTotal: Boolean
)

// Helper function
private fun formatMass(massGrams: Float, unit: WeightUnit): String {
    val formatter = DecimalFormat("0.##")
    
    return when (unit) {
        WeightUnit.GRAMS -> "${formatter.format(massGrams)}g"
        WeightUnit.KILOGRAMS -> "${formatter.format(massGrams / 1000f)}kg"
        WeightUnit.OUNCES -> "${formatter.format(massGrams * 0.035274f)}oz"
        WeightUnit.POUNDS -> "${formatter.format(massGrams * 0.00220462f)}lbs"
    }
}

@Preview(showBackground = true)
@Composable
fun MassEditDialogPreview() {
    MaterialTheme {
        val mockComponent = Component(
            id = "comp_1",
            name = "PLA Filament - Blue",
            category = "filament",
            massGrams = 850.0f,
            manufacturer = "Bambu Lab",
            variableMass = true,
            fullMassGrams = 1000.0f
        )
        
        MassEditDialog(
            component = mockComponent,
            currentTotalMass = 1200.0f,
            allComponents = listOf(mockComponent),
            preferredWeightUnit = WeightUnit.GRAMS,
            onConfirm = { },
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MassEditDialogTotalMassPreview() {
    MaterialTheme {
        val mockComponents = listOf(
            Component(
                id = "comp_1",
                name = "PLA Filament",
                category = "filament",
                massGrams = 800.0f
            ),
            Component(
                id = "comp_2",
                name = "Plastic Spool",
                category = "spool", 
                massGrams = 250.0f
            )
        )
        
        MassEditDialog(
            component = null, // Editing total mass
            currentTotalMass = 1050.0f,
            allComponents = mockComponents,
            preferredWeightUnit = WeightUnit.GRAMS,
            onConfirm = { },
            onDismiss = { }
        )
    }
}