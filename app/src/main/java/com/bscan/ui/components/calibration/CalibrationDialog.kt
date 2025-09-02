package com.bscan.ui.components.calibration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.bscan.model.graph.entities.InventoryItem
import com.bscan.service.CalibrationService
import com.bscan.service.BulkItemType
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.launch

/**
 * Dialog for calibrating unit weights of bulk items
 * Supports the "box of screws" scenario perfectly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationDialog(
    inventoryItem: InventoryItem,
    onDismiss: () -> Unit,
    onCalibrationComplete: (success: Boolean, message: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // State for different calibration modes
    var calibrationMode by remember { mutableStateOf(CalibrationMode.COUNT_AND_WEIGH) }
    var isLoading by remember { mutableStateOf(false) }
    
    // State for count-and-weigh mode
    var totalWeight by remember { mutableStateOf("") }
    var itemCount by remember { mutableStateOf("") }
    var containerWeight by remember { mutableStateOf("") }
    
    // State for container learning mode
    var emptyContainerWeight by remember { mutableStateOf("") }
    
    // State for bulk item estimation mode
    var selectedBulkType by remember { mutableStateOf<BulkItemType?>(null) }
    
    // Current calibration status
    var currentStatus by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(inventoryItem) {
        currentStatus = inventoryItem.getCalibrationStatus()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth().heightIn(max = 600.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calibrate Unit Weight",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = inventoryItem.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                // Current status
                currentStatus?.let { status ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (inventoryItem.isProperlyCalibrated())
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (inventoryItem.isProperlyCalibrated()) 
                                    Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Mode selection
                Text(
                    text = "Calibration Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CalibrationMode.values().forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = calibrationMode == mode,
                                onClick = { calibrationMode = mode }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                Divider()
                
                // Mode-specific inputs
                when (calibrationMode) {
                    CalibrationMode.COUNT_AND_WEIGH -> {
                        CountAndWeighInputs(
                            totalWeight = totalWeight,
                            onTotalWeightChange = { totalWeight = it },
                            itemCount = itemCount,
                            onItemCountChange = { itemCount = it },
                            containerWeight = containerWeight,
                            onContainerWeightChange = { containerWeight = it }
                        )
                    }
                    
                    CalibrationMode.LEARN_CONTAINER -> {
                        LearnContainerInputs(
                            emptyContainerWeight = emptyContainerWeight,
                            onEmptyContainerWeightChange = { emptyContainerWeight = it },
                            canLearnContainer = inventoryItem.unitWeight != null
                        )
                    }
                    
                    CalibrationMode.ESTIMATE_FROM_TYPE -> {
                        EstimateFromTypeInputs(
                            selectedBulkType = selectedBulkType,
                            onBulkTypeSelected = { selectedBulkType = it }
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            performCalibration(
                                calibrationService = CalibrationService(GraphRepository(context)),
                                inventoryItem = inventoryItem,
                                mode = calibrationMode,
                                totalWeight = totalWeight,
                                itemCount = itemCount,
                                containerWeight = containerWeight,
                                emptyContainerWeight = emptyContainerWeight,
                                bulkType = selectedBulkType,
                                scope = scope,
                                onLoading = { isLoading = it },
                                onComplete = onCalibrationComplete
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && isInputValid(calibrationMode, totalWeight, itemCount, containerWeight, emptyContainerWeight, selectedBulkType)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Calibrate")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CountAndWeighInputs(
    totalWeight: String,
    onTotalWeightChange: (String) -> Unit,
    itemCount: String,
    onItemCountChange: (String) -> Unit,
    containerWeight: String,
    onContainerWeightChange: (String) -> Unit
) {
    Text(
        text = "Count and Weigh Method",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    
    Text(
        text = "Perfect for: \"I have 100 screws in a box weighing 247g total\"",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
    
    OutlinedTextField(
        value = totalWeight,
        onValueChange = onTotalWeightChange,
        label = { Text("Total Weight (grams)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    )
    
    OutlinedTextField(
        value = itemCount,
        onValueChange = onItemCountChange,
        label = { Text("Item Count") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
        modifier = Modifier.fillMaxWidth()
    )
    
    OutlinedTextField(
        value = containerWeight,
        onValueChange = onContainerWeightChange,
        label = { Text("Container Weight (optional)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
        supportingText = { Text("Weight of empty box/container") },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LearnContainerInputs(
    emptyContainerWeight: String,
    onEmptyContainerWeightChange: (String) -> Unit,
    canLearnContainer: Boolean
) {
    Text(
        text = "Learn Container Weight",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    
    if (!canLearnContainer) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                text = "Cannot learn container weight - no previous calibration found",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp)
            )
        }
    } else {
        Text(
            text = "Weigh the empty container to improve accuracy",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        
        OutlinedTextField(
            value = emptyContainerWeight,
            onValueChange = onEmptyContainerWeightChange,
            label = { Text("Empty Container Weight (grams)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstimateFromTypeInputs(
    selectedBulkType: BulkItemType?,
    onBulkTypeSelected: (BulkItemType?) -> Unit
) {
    Text(
        text = "Estimate from Item Type",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    
    Text(
        text = "Quick start with estimated weights (low accuracy)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    // Bulk type selection dropdown
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedBulkType?.name?.replace('_', ' ') ?: "",
            onValueChange = { },
            readOnly = true,
            label = { Text("Item Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            BulkItemType.values().forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name.replace('_', ' ')) },
                    onClick = {
                        onBulkTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun isInputValid(
    mode: CalibrationMode,
    totalWeight: String,
    itemCount: String,
    containerWeight: String,
    emptyContainerWeight: String,
    bulkType: BulkItemType?
): Boolean {
    return when (mode) {
        CalibrationMode.COUNT_AND_WEIGH -> {
            totalWeight.toFloatOrNull() != null && 
            itemCount.toIntOrNull() != null && 
            itemCount.toIntOrNull()!! > 0
        }
        CalibrationMode.LEARN_CONTAINER -> {
            emptyContainerWeight.toFloatOrNull() != null
        }
        CalibrationMode.ESTIMATE_FROM_TYPE -> {
            bulkType != null
        }
    }
}

private fun performCalibration(
    calibrationService: CalibrationService,
    inventoryItem: InventoryItem,
    mode: CalibrationMode,
    totalWeight: String,
    itemCount: String,
    containerWeight: String,
    emptyContainerWeight: String,
    bulkType: BulkItemType?,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onComplete: (Boolean, String) -> Unit
) {
    onLoading(true)
    
    scope.launch {
        try {
            val result = when (mode) {
                CalibrationMode.COUNT_AND_WEIGH -> {
                    calibrationService.calibrateFromCount(
                        inventoryItemId = inventoryItem.id,
                        totalWeight = totalWeight.toFloat(),
                        containerWeight = containerWeight.toFloatOrNull(),
                        knownQuantity = itemCount.toInt()
                    )
                }
                
                CalibrationMode.LEARN_CONTAINER -> {
                    calibrationService.learnContainerWeight(
                        inventoryItemId = inventoryItem.id,
                        emptyContainerWeight = emptyContainerWeight.toFloat()
                    )
                }
                
                CalibrationMode.ESTIMATE_FROM_TYPE -> {
                    calibrationService.estimateUnitWeight(
                        inventoryItemId = inventoryItem.id,
                        itemType = bulkType!!
                    )
                }
            }
            
            onLoading(false)
            
            if (result.success) {
                val unitWeight = result.unitWeight ?: 0f
                val confidence = result.confidence ?: 0f
                onComplete(true, "Calibration successful! Unit weight: ${unitWeight}g (${confidence}% confidence)")
            } else {
                onComplete(false, result.error ?: "Calibration failed")
            }
            
        } catch (e: Exception) {
            onLoading(false)
            onComplete(false, "Error: ${e.message}")
        }
    }
}

/**
 * Available calibration modes
 */
enum class CalibrationMode(
    val title: String,
    val description: String
) {
    COUNT_AND_WEIGH(
        "Count and Weigh",
        "Weigh container with known quantity (most accurate)"
    ),
    LEARN_CONTAINER(
        "Learn Container Weight", 
        "Improve accuracy by weighing empty container"
    ),
    ESTIMATE_FROM_TYPE(
        "Estimate from Type",
        "Quick start with typical weights (low accuracy)"
    )
}