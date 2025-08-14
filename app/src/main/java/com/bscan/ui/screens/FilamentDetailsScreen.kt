package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanDebugInfo
import com.bscan.ui.components.*

@Composable
fun FilamentDetailsScreen(
    filamentInfo: FilamentInfo,
    debugInfo: ScanDebugInfo? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ColorPreviewCard(
                colorHex = filamentInfo.colorHex,
                colorName = filamentInfo.colorName
            )
        }
        
        item {
            InfoCard(
                title = "Filament Type",
                value = filamentInfo.detailedFilamentType.ifEmpty { filamentInfo.filamentType }
            )
        }
        
        item {
            SpecificationCard(filamentInfo = filamentInfo)
        }
        
        item {
            TemperatureCard(filamentInfo = filamentInfo)
        }
        
        item {
            ProductionInfoCard(filamentInfo = filamentInfo)
        }
        
        // Show debug info if available
        debugInfo?.let { debug ->
            item {
                DebugInfoCard(debugInfo = debug)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductionInfoCard(
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
                text = "Production Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ProductionInfoRow(
                label = "Production Date",
                value = filamentInfo.productionDate
            )
            
            ProductionInfoRow(
                label = "Tray UID",
                value = filamentInfo.trayUid
            )
            
            ProductionInfoRow(
                label = "Tag UID",
                value = filamentInfo.uid
            )
        }
    }
}

@Composable
private fun ProductionInfoRow(
    label: String,
    value: String
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}