package com.bscan.ui.components.component

import androidx.compose.foundation.layout.*
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
import com.bscan.logic.ComponentValidation
import com.bscan.model.PhysicalComponent
import com.bscan.model.PhysicalComponentType
import com.bscan.repository.InventoryRepository
import com.bscan.repository.PhysicalComponentRepository
import java.time.format.DateTimeFormatter

/**
 * Card displaying a single physical component with actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentListCard(
    component: PhysicalComponent,
    onEdit: (() -> Unit)? = null,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventoryRepository = remember { InventoryRepository(context) }
    var isInUse by remember { mutableStateOf(false) }
    
    LaunchedEffect(component.id) {
        // Check if component is in use
        isInUse = inventoryRepository.getInventoryItems().any { 
            component.id in it.components 
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (component.isUserDefined) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        if (!component.isUserDefined) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("Built-in", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                        
                        if (isInUse) {
                            Spacer(modifier = Modifier.width(8.dp))
                            AssistChip(
                                onClick = { },
                                label = { Text("In Use", style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "${component.massGrams}g • ${component.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    if (component.manufacturer != "Unknown" && component.manufacturer.isNotBlank()) {
                        Text(
                            text = component.manufacturer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (component.description.isNotBlank()) {
                        Text(
                            text = component.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    if (onEdit != null) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                    
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    
                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            enabled = !isInUse // Disable if component is in use
                        ) {
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = "Delete",
                                tint = if (isInUse) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating or editing a physical component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentEditDialog(
    component: PhysicalComponent?,
    onSave: (PhysicalComponent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { PhysicalComponentRepository(context) }
    val validation = remember { ComponentValidation(repository) }
    
    var name by remember { mutableStateOf(component?.name ?: "") }
    var type by remember { mutableStateOf(component?.type ?: PhysicalComponentType.BASE_SPOOL) }
    var massText by remember { mutableStateOf(component?.massGrams?.toString() ?: "") }
    var manufacturer by remember { mutableStateOf(component?.manufacturer ?: "") }
    var description by remember { mutableStateOf(component?.description ?: "") }
    
    // Validation states
    var nameError by remember { mutableStateOf<String?>(null) }
    var massError by remember { mutableStateOf<String?>(null) }
    
    // Validate inputs
    LaunchedEffect(name) {
        nameError = validation.validateName(name, component?.id)
    }
    
    LaunchedEffect(massText) {
        massError = validation.validateMass(massText, type)
    }
    
    val isValid = nameError == null && massError == null && name.isNotBlank() && massText.isNotBlank()
    val isCreate = component == null
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreate) "Create Component" else "Edit Component") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name *") },
                    isError = nameError != null,
                    supportingText = if (nameError != null) {
                        { Text(nameError!!) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = false,
                        onDismissRequest = { }
                    ) {
                        PhysicalComponentType.values().filter { it != PhysicalComponentType.FILAMENT }.forEach { componentType ->
                            DropdownMenuItem(
                                text = { Text(componentType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
                                onClick = { type = componentType }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = massText,
                    onValueChange = { massText = it },
                    label = { Text("Mass (grams) *") },
                    isError = massError != null,
                    supportingText = if (massError != null) {
                        { Text(massError!!) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    suffix = { Text("g") }
                )
                
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mass = massText.toFloat()
                    val newComponent = if (isCreate) {
                        PhysicalComponent(
                            id = "user_${System.currentTimeMillis()}",
                            name = name.trim(),
                            type = type,
                            massGrams = mass,
                            variableMass = false,
                            manufacturer = manufacturer.trim().ifBlank { "Custom" },
                            description = description.trim(),
                            isUserDefined = true
                        )
                    } else {
                        component!!.copy(
                            name = name.trim(),
                            type = type,
                            massGrams = mass,
                            manufacturer = manufacturer.trim().ifBlank { "Custom" },
                            description = description.trim()
                        )
                    }
                    onSave(newComponent)
                },
                enabled = isValid
            ) {
                Text(if (isCreate) "Create" else "Save")
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
 * Dialog for copying an existing component with pre-filled form
 */
@Composable
fun ComponentCopyDialog(
    sourceComponent: PhysicalComponent,
    onSave: (PhysicalComponent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Create a copy template with modified name
    val copyTemplate = sourceComponent.copy(
        id = "user_${System.currentTimeMillis()}", // Will be regenerated
        name = "${sourceComponent.name} (Copy)",
        isUserDefined = true
    )
    
    ComponentEditDialog(
        component = copyTemplate,
        onSave = onSave,
        onDismiss = onDismiss,
        modifier = modifier
    )
}

/**
 * Confirmation dialog for deleting a component
 */
@Composable
fun ComponentDeleteConfirmationDialog(
    component: PhysicalComponent,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventoryRepository = remember { InventoryRepository(context) }
    var isInUse by remember { mutableStateOf(false) }
    var usageDetails by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(component.id) {
        // Check if component is in use
        val inventoryItems = inventoryRepository.getInventoryItems()
        val itemsUsingComponent = inventoryItems.filter { component.id in it.components }
        isInUse = itemsUsingComponent.isNotEmpty()
        
        if (isInUse) {
            usageDetails = "Used in ${itemsUsingComponent.size} inventory item(s)"
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            ) 
        },
        title = { Text("Delete Component") },
        text = {
            Column {
                Text("Are you sure you want to delete \"${component.name}\"?")
                
                if (isInUse) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ Cannot delete: $usageDetails",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (!isInUse) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}