package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemperatureCard(
    filamentInfo: FilamentInfo,
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
                text = "Temperature Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TemperatureRow(
                label = "Hotend",
                minValue = filamentInfo.minTemperature,
                maxValue = filamentInfo.maxTemperature
            )
            
            TemperatureRow(
                label = "Bed",
                value = filamentInfo.bedTemperature
            )
            
            TemperatureRow(
                label = "Drying",
                value = filamentInfo.dryingTemperature,
                duration = "${filamentInfo.dryingTime}h"
            )
        }
    }
}

@Composable
private fun TemperatureRow(
    label: String,
    value: Int? = null,
    minValue: Int? = null,
    maxValue: Int? = null,
    duration: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        val temperatureText = when {
            minValue != null && maxValue != null -> "${minValue}-${maxValue}°C"
            value != null -> "${value}°C${duration?.let { " ($it)" } ?: ""}"
            else -> "N/A"
        }
        
        Text(
            text = temperatureText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}