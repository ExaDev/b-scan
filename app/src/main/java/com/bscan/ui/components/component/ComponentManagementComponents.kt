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
import com.bscan.model.Component
import com.bscan.repository.ComponentRepository
import java.time.format.DateTimeFormatter

/**
 * Card displaying a single hierarchical component with actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentListCard(
    component: Component,
    onEdit: (() -> Unit)? = null,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val componentRepository = remember { ComponentRepository(context) }
    var isInUse by remember { mutableStateOf(false) }
    var childCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(component.id) {
        // Check if component has children (is a parent/composite component)
        val children = componentRepository.getChildComponents(component.id)
        childCount = children.size
        
        // Check if component is in use as a child of another component
        val allComponents = componentRepository.getComponents()
        isInUse = allComponents.any { comp ->
            component.id in comp.childComponents
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (component.isInventoryItem) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Category badge
                        AssistChip(
                            onClick = { },
                            label = { Text(component.category) },
                            leadingIcon = {
                                Icon(
                                    getCategoryIcon(component.category),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        
                        // Inventory item indicator
                        if (component.isInventoryItem) {
                            AssistChip(
                                onClick = { },
                                label = { Text("Inventory") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Inventory,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                        
                        // Parent component indicator
                        if (childCount > 0) {
                            AssistChip(
                                onClick = { },
                                label = { Text("$childCount parts") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AccountTree,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy component")
                    }
                    
                    onEdit?.let {
                        IconButton(onClick = it) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit component")
                        }
                    }
                    
                    onDelete?.let {
                        IconButton(
                            onClick = it,
                            enabled = !isInUse
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete component",
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Component details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Mass: ${formatMass(component.massGrams)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (component.manufacturer.isNotBlank()) {
                        Text(
                            text = "Manufacturer: ${component.manufacturer}",
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
                
                // Variable mass indicator
                if (component.variableMass) {
                    Column {
                        Icon(
                            Icons.Default.TrendingDown,
                            contentDescription = "Variable mass",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Variable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Tags row
            if (component.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    component.tags.take(3).forEach { tag ->
                        FilterChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                            selected = false
                        )
                    }
                    if (component.tags.size > 3) {
                        Text(
                            text = "+${component.tags.size - 3} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // In-use warning
            if (isInUse) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "In use by other components",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/**
 * Dialog for editing component details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentEditDialog(
    component: Component?,
    onSave: (Component) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val componentRepository = remember { ComponentRepository(context) }
    val validation = remember { ComponentValidation(componentRepository) }
    
    var name by remember { mutableStateOf(component?.name ?: "") }
    var category by remember { mutableStateOf(component?.category ?: "general") }
    var massText by remember { mutableStateOf(component?.massGrams?.toString() ?: "") }
    var manufacturer by remember { mutableStateOf(component?.manufacturer ?: "") }
    var description by remember { mutableStateOf(component?.description ?: "") }
    var variableMass by remember { mutableStateOf(component?.variableMass ?: false) }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    var massError by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (component == null) "Add Component" else "Edit Component") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = validation.validateName(it, component?.id)
                    },
                    label = { Text("Component Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Category field
                OutlinedTextField(
                    value = category,
                    onValueChange = { 
                        category = it
                        categoryError = validation.validateCategory(it)
                    },
                    label = { Text("Category") },
                    isError = categoryError != null,
                    supportingText = categoryError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Mass field
                OutlinedTextField(
                    value = massText,
                    onValueChange = { 
                        massText = it
                        massError = validation.validateMass(it, category)
                    },
                    label = { Text("Mass (grams)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = massError != null,
                    supportingText = massError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Variable mass checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = variableMass,
                        onCheckedChange = { variableMass = it }
                    )
                    Text("Variable mass (changes over time)")
                }
                
                // Manufacturer field
                OutlinedTextField(
                    value = manufacturer,
                    onValueChange = { manufacturer = it },
                    label = { Text("Manufacturer (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val mass = massText.toFloatOrNull()
                    if (mass != null && nameError == null && categoryError == null && massError == null) {
                        val newComponent = component?.copy(
                            name = name.trim(),
                            category = category.trim(),
                            massGrams = mass,
                            manufacturer = manufacturer.trim(),
                            description = description.trim(),
                            variableMass = variableMass,
                            fullMassGrams = if (variableMass) mass else null
                        ) ?: Component(
                            id = "component_${System.currentTimeMillis()}",
                            name = name.trim(),
                            category = category.trim(),
                            massGrams = mass,
                            manufacturer = manufacturer.trim(),
                            description = description.trim(),
                            variableMass = variableMass,
                            fullMassGrams = if (variableMass) mass else null
                        )
                        onSave(newComponent)
                    }
                },
                enabled = name.isNotBlank() && 
                         category.isNotBlank() && 
                         massText.isNotBlank() && 
                         nameError == null && 
                         categoryError == null && 
                         massError == null
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
 * Component selection dialog for adding to inventory items
 */
@Composable
fun ComponentSelectionDialog(
    availableComponents: List<Component>,
    selectedComponents: List<String>,
    onSelectionChanged: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSelection by remember { mutableStateOf(selectedComponents) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Components") },
        text = {
            Column {
                Text(
                    text = "Select components to add:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                availableComponents.forEach { component ->
                    val isSelected = component.id in currentSelection
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                currentSelection = if (checked) {
                                    currentSelection + component.id
                                } else {
                                    currentSelection - component.id
                                }
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = component.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "${component.category} • ${formatMass(component.massGrams)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelectionChanged(currentSelection)
                    onDismiss()
                }
            ) {
                Text("Add Selected")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun getCategoryIcon(category: String) = when (category.lowercase()) {
    "filament" -> Icons.Default.Polymer
    "spool" -> Icons.Default.Circle
    "core" -> Icons.Default.CircleOutlined
    "adapter" -> Icons.Default.Transform
    "packaging" -> Icons.Default.LocalShipping
    "rfid-tag" -> Icons.Default.Sensors
    "filament-tray" -> Icons.Default.Inventory
    else -> Icons.Default.Category
}

private fun formatMass(massGrams: Float?): String {
    return if (massGrams == null) {
        "Unknown"
    } else if (massGrams >= 1000f) {
        "${String.format("%.1f", massGrams / 1000f)}kg"
    } else {
        "${String.format("%.1f", massGrams)}g"
    }
}