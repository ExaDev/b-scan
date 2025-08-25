package com.bscan.ui.components.weight

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentStatus
import com.bscan.model.InventoryItem
import com.bscan.logic.WeightUnit
import java.time.format.DateTimeFormatter

/**
 * Card displaying the current filament status with visual progress indicator
 */
@Composable
fun FilamentStatusCard(
    inventoryItem: InventoryItem,
    filamentStatus: FilamentStatus,
    preferredWeightUnit: WeightUnit,
    onRecordWeight: () -> Unit,
    onSetConfiguration: () -> Unit,
    onSetExpectedWeight: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filament Weight Tracking",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = onRecordWeight) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Record Weight",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            when {
                !inventoryItem.hasSpoolConfiguration -> {
                    // No configuration set
                    ConfigurationRequiredSection(
                        onSetConfiguration = onSetConfiguration,
                        onSetExpectedWeight = onSetExpectedWeight
                    )
                }
                
                !inventoryItem.hasWeightMeasurements -> {
                    // Configuration set but no measurements
                    MeasurementRequiredSection(
                        configurationName = filamentStatus.spoolConfiguration?.name ?: "Unknown",
                        onRecordWeight = onRecordWeight
                    )
                }
                
                !filamentStatus.calculationSuccess -> {
                    // Error in calculation
                    ErrorSection(
                        errorMessage = filamentStatus.errorMessage ?: "Unknown error",
                        onRecordWeight = onRecordWeight
                    )
                }
                
                else -> {
                    // Normal status display
                    FilamentStatusSection(
                        filamentStatus = filamentStatus,
                        preferredWeightUnit = preferredWeightUnit
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationRequiredSection(
    onSetConfiguration: () -> Unit,
    onSetExpectedWeight: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = "Weight Tracking Setup",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Choose how to set up weight tracking for this spool",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Expected Weight Button (Recommended)
        Button(
            onClick = onSetExpectedWeight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Calculate, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Expected Weight (No Scales)")
        }
        
        // OR divider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "OR",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        
        // Configuration only button
        OutlinedButton(
            onClick = onSetConfiguration,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Configuration Only")
        }
    }
}

@Composable
private fun MeasurementRequiredSection(
    configurationName: String,
    onRecordWeight: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Scale,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = "Weight Measurement Required",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Configuration: $configurationName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Record the first weight measurement to begin tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onRecordWeight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record Weight")
        }
    }
}

@Composable
private fun ErrorSection(
    errorMessage: String,
    onRecordWeight: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp)
        )
        
        Text(
            text = "Calculation Error",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        OutlinedButton(
            onClick = onRecordWeight,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Record New Measurement")
        }
    }
}

@Composable
private fun FilamentStatusSection(
    filamentStatus: FilamentStatus,
    preferredWeightUnit: WeightUnit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress bar
        FilamentProgressBar(
            remainingPercentage = filamentStatus.remainingPercentage
        )
        
        // Weight information
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = filamentStatus.getFormattedRemainingWeight(preferredWeightUnit),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Percentage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(filamentStatus.remainingPercentage * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Configuration and last measurement info
        filamentStatus.spoolConfiguration?.let { config ->
            Text(
                text = "Configuration: ${config.name}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        filamentStatus.lastMeasurement?.let { measurement ->
            Text(
                text = "Last measured: ${measurement.measuredAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Warning indicators
        if (filamentStatus.isNearlyEmpty) {
            WarningIndicator(
                text = "Filament nearly empty!",
                color = MaterialTheme.colorScheme.error
            )
        } else if (filamentStatus.isRunningLow) {
            WarningIndicator(
                text = "Filament running low",
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Composable
private fun FilamentProgressBar(
    remainingPercentage: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = remainingPercentage,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )
    
    val progressColor = when {
        remainingPercentage < 0.05f -> MaterialTheme.colorScheme.error
        remainingPercentage < 0.2f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filament Remaining",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun WarningIndicator(
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}