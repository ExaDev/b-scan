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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.*
import com.bscan.logic.MassCalculationService
// WeightUnit is now defined in MassCalculationService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Card displaying the current filament status with visual progress indicator
 */
@Composable
fun FilamentStatusCard(
    inventoryItem: InventoryItem,
    filamentStatus: FilamentStatus,
    preferredWeightUnit: com.bscan.logic.WeightUnit,
    onRecordWeight: () -> Unit,
    onSetupComponents: () -> Unit,
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
                !inventoryItem.hasComponents -> {
                    // No components set
                    ComponentSetupRequiredSection(
                        onSetupComponents = onSetupComponents
                    )
                }
                
                !inventoryItem.hasMassMeasurements -> {
                    // Components set but no measurements
                    MeasurementRequiredSection(
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
private fun ComponentSetupRequiredSection(
    onSetupComponents: () -> Unit
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
            text = "Component Setup Required",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = "Set up the physical components for this spool to enable weight tracking",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onSetupComponents,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Setup Components")
        }
    }
}

@Composable
private fun MeasurementRequiredSection(
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
    preferredWeightUnit: com.bscan.logic.WeightUnit
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
                    text = filamentStatus.getFormattedRemainingMass(preferredWeightUnit),
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
        
        // Last measurement info
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

@Preview(showBackground = true)
@Composable
private fun FilamentStatusCardPreview() {
    val mockInventoryItem = InventoryItem(
        trayUid = "01008023ABC123",
        components = listOf("filament-001", "core-001", "spool-001"),
        totalMeasuredMass = 1245.0f,
        measurements = listOf(
            MassMeasurement(
                id = "measurement-001",
                componentId = "01008023ABC123",
                measuredMassGrams = 1245.0f,
                measurementType = MeasurementType.TOTAL_MASS,
                measuredAt = LocalDateTime.now().minusDays(7),
                notes = "Initial full spool measurement",
                isVerified = true
            ),
            MassMeasurement(
                id = "measurement-002",
                componentId = "01008023ABC123", 
                measuredMassGrams = 1050.0f,
                measurementType = MeasurementType.TOTAL_MASS,
                measuredAt = LocalDateTime.now(),
                notes = "Current measurement",
                isVerified = true
            )
        ),
        lastUpdated = LocalDateTime.now(),
        notes = "Red PLA filament spool"
    )

    val mockFilamentStatus = FilamentStatus(
        remainingMassGrams = 800.0f,
        remainingPercentage = 0.8f,
        consumedMassGrams = 200.0f,
        lastMeasurement = mockInventoryItem.latestMeasurement,
        components = emptyList(),
        calculationSuccess = true
    )

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilamentStatusCard(
                inventoryItem = mockInventoryItem,
                filamentStatus = mockFilamentStatus,
                preferredWeightUnit = com.bscan.logic.WeightUnit.GRAMS,
                onRecordWeight = {},
                onSetupComponents = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilamentStatusCardLowPreview() {
    val mockInventoryItem = InventoryItem(
        trayUid = "01008023ABC123",
        components = listOf("filament-001", "core-001", "spool-001"),
        totalMeasuredMass = 1245.0f,
        measurements = listOf(
            MassMeasurement(
                id = "measurement-001",
                componentId = "01008023ABC123",
                measuredMassGrams = 300.0f,
                measurementType = MeasurementType.TOTAL_MASS,
                measuredAt = LocalDateTime.now(),
                notes = "Running low!",
                isVerified = true
            )
        ),
        lastUpdated = LocalDateTime.now()
    )

    val mockFilamentStatus = FilamentStatus(
        remainingMassGrams = 50.0f,
        remainingPercentage = 0.05f,
        consumedMassGrams = 950.0f,
        lastMeasurement = mockInventoryItem.latestMeasurement,
        components = emptyList(),
        calculationSuccess = true
    )

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilamentStatusCard(
                inventoryItem = mockInventoryItem,
                filamentStatus = mockFilamentStatus,
                preferredWeightUnit = com.bscan.logic.WeightUnit.GRAMS,
                onRecordWeight = {},
                onSetupComponents = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilamentStatusCardNoComponentsPreview() {
    val mockInventoryItem = InventoryItem(
        trayUid = "01008023ABC123",
        components = emptyList(),
        totalMeasuredMass = null,
        measurements = emptyList(),
        lastUpdated = LocalDateTime.now()
    )

    val mockFilamentStatus = FilamentStatus(
        remainingMassGrams = 0.0f,
        remainingPercentage = 0.0f,
        consumedMassGrams = 0.0f,
        lastMeasurement = null,
        components = emptyList(),
        calculationSuccess = false,
        errorMessage = "No components configured"
    )

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilamentStatusCard(
                inventoryItem = mockInventoryItem,
                filamentStatus = mockFilamentStatus,
                preferredWeightUnit = com.bscan.logic.WeightUnit.GRAMS,
                onRecordWeight = {},
                onSetupComponents = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FilamentStatusCardErrorPreview() {
    val mockInventoryItem = InventoryItem(
        trayUid = "01008023ABC123",
        components = listOf("filament-001", "core-001", "spool-001"),
        totalMeasuredMass = null,
        measurements = emptyList(),
        lastUpdated = LocalDateTime.now()
    )

    val mockFilamentStatus = FilamentStatus(
        remainingMassGrams = 0.0f,
        remainingPercentage = 0.0f,
        consumedMassGrams = 0.0f,
        lastMeasurement = null,
        components = emptyList(),
        calculationSuccess = false,
        errorMessage = "Unable to calculate mass: component mass data missing"
    )

    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            FilamentStatusCard(
                inventoryItem = mockInventoryItem,
                filamentStatus = mockFilamentStatus,
                preferredWeightUnit = com.bscan.logic.WeightUnit.GRAMS,
                onRecordWeight = {},
                onSetupComponents = {}
            )
        }
    }
}