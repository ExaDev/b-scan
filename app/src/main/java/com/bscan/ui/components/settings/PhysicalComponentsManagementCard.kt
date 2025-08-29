package com.bscan.ui.components.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * Card component for managing physical components
 */
@Composable
fun PhysicalComponentsManagementCard(
    totalComponents: Int,
    userDefinedComponents: Int,
    builtInComponents: Int,
    onManageComponents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Component Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Manage physical components used in inventory calculations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Statistics
            ComponentStatisticsRow(
                totalComponents = totalComponents,
                userDefinedComponents = userDefinedComponents,
                builtInComponents = builtInComponents
            )
            
            Button(
                onClick = onManageComponents,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Components")
            }
        }
    }
}

@Composable
private fun ComponentStatisticsRow(
    totalComponents: Int,
    userDefinedComponents: Int,
    builtInComponents: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ComponentStatistic(
            label = "Total Components",
            value = totalComponents.toString()
        )
        
        ComponentStatistic(
            label = "User-Defined",
            value = userDefinedComponents.toString()
        )
        
        ComponentStatistic(
            label = "Built-In",
            value = builtInComponents.toString()
        )
    }
}

@Composable
private fun ComponentStatistic(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


