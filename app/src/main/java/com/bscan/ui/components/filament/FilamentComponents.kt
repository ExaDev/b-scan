package com.bscan.ui.components.filament

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.ui.components.common.ConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionInfoCard(
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
                value = filamentInfo.tagUid
            )
        }
    }
}

@Composable
fun ProductionInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CacheManagementCard(
    uid: String,
    onPurgeCache: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Cache Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "This spool's data is cached for faster scanning. You can purge the cache to force a fresh read on the next scan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Purge Cache for This Spool")
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        ConfirmationDialog(
            title = "Purge Cache?",
            message = "This will remove cached data for this spool. The next scan will take longer as it reads fresh data from the tag.",
            confirmText = "Purge",
            onConfirm = {
                onPurgeCache(uid)
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductionInfoCardPreview() {
    MaterialTheme {
        val mockFilamentInfo = FilamentInfo(
            filamentType = "PLA",
            detailedFilamentType = "PLA Basic",
            colorName = "Vibrant Blue",
            colorHex = "#1E88E5",
            productionDate = "2024-03-15",
            trayUid = "01008023456789ABCDEF",
            tagUid = "A1B2C3D4",
            spoolWeight = 1000,
            filamentDiameter = 1.75f,
            filamentLength = 330000,
            minTemperature = 210,
            maxTemperature = 230,
            bedTemperature = 60,
            dryingTemperature = 40,
            dryingTime = 8
        )
        
        ProductionInfoCard(filamentInfo = mockFilamentInfo)
    }
}

@Preview(showBackground = true)
@Composable
fun ProductionInfoRowPreview() {
    MaterialTheme {
        ProductionInfoRow(
            label = "Production Date",
            value = "2024-03-15"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CacheManagementCardPreview() {
    MaterialTheme {
        CacheManagementCard(
            uid = "A1B2C3D4E5F6G7H8",
            onPurgeCache = { }
        )
    }
}