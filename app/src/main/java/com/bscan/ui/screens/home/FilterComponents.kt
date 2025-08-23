package com.bscan.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    filterState: FilterState,
    availableFilamentTypes: Set<String>,
    availableColors: Set<String>,
    availableBaseMaterials: Set<String>,
    availableMaterialSeries: Set<String>,
    onFilterStateChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filament types filter
                if (availableFilamentTypes.isNotEmpty()) {
                    Text(
                        text = "Filament Types",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    availableFilamentTypes.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = filterState.filamentTypes.contains(type),
                                onCheckedChange = { checked ->
                                    val newTypes = if (checked) {
                                        filterState.filamentTypes + type
                                    } else {
                                        filterState.filamentTypes - type
                                    }
                                    onFilterStateChange(filterState.copy(filamentTypes = newTypes))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = type)
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // Colors filter
                if (availableColors.isNotEmpty()) {
                    Text(
                        text = "Colors",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    availableColors.forEach { color ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = filterState.colors.contains(color),
                                onCheckedChange = { checked ->
                                    val newColors = if (checked) {
                                        filterState.colors + color
                                    } else {
                                        filterState.colors - color
                                    }
                                    onFilterStateChange(filterState.copy(colors = newColors))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = color)
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // Base Materials filter
                if (availableBaseMaterials.isNotEmpty()) {
                    Text(
                        text = "Base Materials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    availableBaseMaterials.forEach { material ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = filterState.baseMaterials.contains(material),
                                onCheckedChange = { checked ->
                                    val newMaterials = if (checked) {
                                        filterState.baseMaterials + material
                                    } else {
                                        filterState.baseMaterials - material
                                    }
                                    onFilterStateChange(filterState.copy(baseMaterials = newMaterials))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = material)
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // Material Series filter
                if (availableMaterialSeries.isNotEmpty()) {
                    Text(
                        text = "Material Series",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    availableMaterialSeries.forEach { series ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = filterState.materialSeries.contains(series),
                                onCheckedChange = { checked ->
                                    val newSeries = if (checked) {
                                        filterState.materialSeries + series
                                    } else {
                                        filterState.materialSeries - series
                                    }
                                    onFilterStateChange(filterState.copy(materialSeries = newSeries))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = series)
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // Success rate filter
                Text(
                    text = "Minimum Success Rate: ${(filterState.minSuccessRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = filterState.minSuccessRate,
                    onValueChange = { onFilterStateChange(filterState.copy(minSuccessRate = it)) },
                    valueRange = 0f..1f,
                    steps = 9 // 0%, 10%, 20%, ..., 100%
                )
                
                HorizontalDivider()
                
                // Result type filters
                Text(
                    text = "Result Types",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterState.showSuccessOnly,
                        onCheckedChange = { 
                            onFilterStateChange(
                                filterState.copy(
                                    showSuccessOnly = it,
                                    showFailuresOnly = if (it) false else filterState.showFailuresOnly
                                )
                            ) 
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Success Only")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterState.showFailuresOnly,
                        onCheckedChange = { 
                            onFilterStateChange(
                                filterState.copy(
                                    showFailuresOnly = it,
                                    showSuccessOnly = if (it) false else filterState.showSuccessOnly
                                )
                            ) 
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Failures Only")
                }
                
                HorizontalDivider()
                
                // Date range filter
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                val dateRangeOptions = listOf(
                    null to "All Time",
                    1 to "Last Day",
                    7 to "Last Week", 
                    30 to "Last Month",
                    90 to "Last 3 Months"
                )
                
                dateRangeOptions.forEach { (days, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = filterState.dateRangeDays == days,
                            onClick = { onFilterStateChange(filterState.copy(dateRangeDays = days)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onFilterStateChange(FilterState()) // Reset filters
                    onDismiss()
                }
            ) {
                Text("Clear All")
            }
        }
    )
}

@Composable
fun FilterChips(
    filterState: FilterState,
    onFilterStateChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    // Active filter chips
    if (filterState.filamentTypes.isNotEmpty() || 
        filterState.colors.isNotEmpty() ||
        filterState.baseMaterials.isNotEmpty() ||
        filterState.materialSeries.isNotEmpty() ||
        filterState.minSuccessRate > 0f || 
        filterState.showSuccessOnly || 
        filterState.showFailuresOnly || 
        filterState.dateRangeDays != null) {
        
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterState.filamentTypes.toList()) { type ->
                InputChip(
                    onClick = { 
                        val newTypes = filterState.filamentTypes - type
                        onFilterStateChange(filterState.copy(filamentTypes = newTypes))
                    },
                    label = { Text(type) },
                    selected = false,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            items(filterState.colors.toList()) { color ->
                InputChip(
                    onClick = { 
                        val newColors = filterState.colors - color
                        onFilterStateChange(filterState.copy(colors = newColors))
                    },
                    label = { Text(color) },
                    selected = false,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove color filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            items(filterState.baseMaterials.toList()) { material ->
                InputChip(
                    onClick = { 
                        val newMaterials = filterState.baseMaterials - material
                        onFilterStateChange(filterState.copy(baseMaterials = newMaterials))
                    },
                    label = { Text(material) },
                    selected = false,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove base material filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            items(filterState.materialSeries.toList()) { series ->
                InputChip(
                    onClick = { 
                        val newSeries = filterState.materialSeries - series
                        onFilterStateChange(filterState.copy(materialSeries = newSeries))
                    },
                    label = { Text(series) },
                    selected = false,
                    trailingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove material series filter",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            if (filterState.minSuccessRate > 0f) {
                item {
                    InputChip(
                        onClick = { 
                            onFilterStateChange(filterState.copy(minSuccessRate = 0f))
                        },
                        label = { Text("Success ≥ ${(filterState.minSuccessRate * 100).toInt()}%") },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            if (filterState.showSuccessOnly) {
                item {
                    InputChip(
                        onClick = { 
                            onFilterStateChange(filterState.copy(showSuccessOnly = false))
                        },
                        label = { Text("Success Only") },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            if (filterState.showFailuresOnly) {
                item {
                    InputChip(
                        onClick = { 
                            onFilterStateChange(filterState.copy(showFailuresOnly = false))
                        },
                        label = { Text("Failures Only") },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            
            filterState.dateRangeDays?.let { days ->
                item {
                    val label = when (days) {
                        1 -> "Last Day"
                        7 -> "Last Week"
                        30 -> "Last Month"
                        90 -> "Last 3 Months"
                        else -> "Last $days days"
                    }
                    
                    InputChip(
                        onClick = { 
                            onFilterStateChange(filterState.copy(dateRangeDays = null))
                        },
                        label = { Text(label) },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}