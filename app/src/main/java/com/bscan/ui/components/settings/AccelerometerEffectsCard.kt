package com.bscan.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import com.bscan.repository.UserPreferencesRepository
import com.bscan.ui.components.FilamentColorBox

/**
 * Card component for accelerometer effects settings
 */
@Composable
fun AccelerometerEffectsCard(
    userPrefsRepository: UserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accelerometerManager = remember { com.bscan.sensor.AccelerometerManager(context) }
    val deviceCapabilities = remember { com.bscan.utils.DeviceCapabilities(context) }
    
    var accelerometerEffectsEnabled by remember { 
        mutableStateOf(userPrefsRepository.isAccelerometerEffectsEnabled()) 
    }
    var motionSensitivity by remember {
        mutableStateOf(userPrefsRepository.getMotionSensitivity())
    }
    val scope = rememberCoroutineScope()
    
    val isAccelerometerAvailable = accelerometerManager.isAvailable()
    val deviceSupportsEffects = remember { !deviceCapabilities.shouldDisableAccelerometerEffects() }
    val wasAutoDisabled = userPrefsRepository.wasAccelerometerEffectsAutoDisabled()
    val deviceInfo = userPrefsRepository.getAccelerometerEffectsDeviceInfo()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main toggle row
            AccelerometerToggleSection(
                isAccelerometerAvailable = isAccelerometerAvailable,
                deviceSupportsEffects = deviceSupportsEffects,
                wasAutoDisabled = wasAutoDisabled,
                deviceInfo = deviceInfo,
                accelerometerEffectsEnabled = accelerometerEffectsEnabled,
                onAccelerometerEffectsChanged = { enabled ->
                    if (isAccelerometerAvailable) {
                        accelerometerEffectsEnabled = enabled
                        scope.launch {
                            userPrefsRepository.setAccelerometerEffectsEnabled(enabled)
                        }
                    }
                }
            )
            
            // Motion sensitivity slider (only show when effects are enabled)
            if (accelerometerEffectsEnabled && isAccelerometerAvailable) {
                MotionSensitivitySection(
                    motionSensitivity = motionSensitivity,
                    onMotionSensitivityChanged = { sensitivity ->
                        motionSensitivity = sensitivity
                    },
                    onMotionSensitivityChangeFinished = {
                        scope.launch {
                            userPrefsRepository.setMotionSensitivity(motionSensitivity)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AccelerometerToggleSection(
    isAccelerometerAvailable: Boolean,
    deviceSupportsEffects: Boolean,
    wasAutoDisabled: Boolean,
    deviceInfo: String,
    accelerometerEffectsEnabled: Boolean,
    onAccelerometerEffectsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Motion Effects",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                // Show performance indicator
                if (wasAutoDisabled) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Auto-disabled for performance",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Text(
                text = when {
                    !isAccelerometerAvailable -> "Accelerometer not available on this device"
                    !deviceSupportsEffects -> deviceInfo
                    wasAutoDisabled -> "$deviceInfo (can be manually enabled)"
                    else -> "Reflections and shimmer follow device tilt"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !isAccelerometerAvailable -> MaterialTheme.colorScheme.error
                    !deviceSupportsEffects -> MaterialTheme.colorScheme.secondary
                    wasAutoDisabled -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        Switch(
            checked = accelerometerEffectsEnabled && isAccelerometerAvailable,
            enabled = isAccelerometerAvailable,
            onCheckedChange = onAccelerometerEffectsChanged
        )
    }
}

@Composable
private fun MotionSensitivitySection(
    motionSensitivity: Float,
    onMotionSensitivityChanged: (Float) -> Unit,
    onMotionSensitivityChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Motion Sensitivity",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when {
                    motionSensitivity <= 0.3f -> "Subtle"
                    motionSensitivity <= 0.7f -> "Balanced"
                    else -> "Dynamic"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Slider(
            value = motionSensitivity,
            onValueChange = onMotionSensitivityChanged,
            onValueChangeFinished = onMotionSensitivityChangeFinished,
            valueRange = 0.1f..1.0f,
            steps = 17, // 0.1 to 1.0 in steps of 0.05
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Subtle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Dynamic",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Preview box to show current sensitivity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Small preview box with PETG reflection effect
            FilamentColorBox(
                colorHex = "#1E88E5", // Blue PETG color
                filamentType = "PETG Basic",
                size = 32.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccelerometerEffectsCardPreview() {
    MaterialTheme {
        // Create a mock repository implementation for preview
        val mockRepository = object : UserPreferencesRepository {
            override fun isAccelerometerEffectsEnabled(): Boolean = true
            override fun getMotionSensitivity(): Float = 0.5f
            override suspend fun setAccelerometerEffectsEnabled(enabled: Boolean) {}
            override suspend fun setMotionSensitivity(sensitivity: Float) {}
            override fun wasAccelerometerEffectsAutoDisabled(): Boolean = false
            override fun getAccelerometerEffectsDeviceInfo(): String = "Samsung Galaxy S21"
            override fun getMassDisplayMode(): String = "percentage"
            override suspend fun setMassDisplayMode(mode: String) {}
            override fun isBleScaleEnabled(): Boolean = false
            override suspend fun setBleScaleEnabled(enabled: Boolean) {}
            override fun getBleDeviceAddress(): String? = null
            override suspend fun setBleDeviceAddress(address: String?) {}
            override fun getThemeMode(): String = "system"
            override suspend fun setThemeMode(mode: String) {}
            override fun getCatalogDisplayMode(): String = "grid"
            override suspend fun setCatalogDisplayMode(mode: String) {}
        }
        
        AccelerometerEffectsCard(
            userPrefsRepository = mockRepository
        )
    }
}

