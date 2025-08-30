package com.bscan.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.model.Component
import com.bscan.model.DecryptedScanData
import com.bscan.ui.components.ScanStateIndicator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentCard(
    component: Component,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onClick?.invoke(component.id) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Category icon
            Icon(
                imageVector = when (component.category.lowercase()) {
                    "filament" -> Icons.Default.Cable
                    "rfid-tag" -> Icons.Default.Tag
                    "core", "spool" -> Icons.Default.Circle
                    "tool" -> Icons.Default.Build
                    "equipment" -> Icons.Default.Devices
                    else -> Icons.Default.Inventory
                },
                contentDescription = "Component type: ${component.category}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            // Component info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = component.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${component.category} • ${component.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (component.parentComponentId != null) {
                    Text(
                        text = "Child of ${component.parentComponentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Mass information if available
            component.massGrams?.let { mass ->
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${mass}g",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "mass",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryCard(
    scanData: DecryptedScanData,
    modifier: Modifier = Modifier,
    onClick: ((DecryptedScanData) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it else it },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onClick?.invoke(scanData) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // NFC icon
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = "NFC scan",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            
            // Scan info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "NFC Scan",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "UID: ${scanData.tagUid}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = scanData.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Data size indicator
            Text(
                text = "${scanData.decryptedBlocks.size} blocks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompactScanPrompt(
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongPress
            ) {},
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScanStateIndicator(
                    isIdle = scanState == ScanState.IDLE,
                    isDetected = scanState == ScanState.TAG_DETECTED,
                    isProcessing = scanState == ScanState.PROCESSING,
                    modifier = Modifier.size(48.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val title = when (scanState) {
                        ScanState.IDLE -> "Scan a Component"
                        ScanState.TAG_DETECTED -> "Tag Detected"
                        ScanState.PROCESSING -> "Scanning..."
                        ScanState.SUCCESS -> "Scan Complete"
                        ScanState.ERROR -> "Scan Failed"
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    val description = when (scanState) {
                        ScanState.IDLE -> "Hold your device against an NFC/RFID tag to scan"
                        ScanState.TAG_DETECTED -> "Preparing to read tag data"
                        ScanState.PROCESSING -> scanProgress?.statusMessage ?: "Processing tag data"
                        ScanState.SUCCESS -> "Component information successfully read"
                        ScanState.ERROR -> "Unable to read tag data"
                    }
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val iconVector = when (scanState) {
                    ScanState.IDLE -> Icons.Default.NearMe
                    ScanState.TAG_DETECTED -> Icons.Default.CheckCircle
                    ScanState.PROCESSING -> Icons.Default.HourglassEmpty
                    ScanState.SUCCESS -> Icons.Default.CheckCircle
                    ScanState.ERROR -> Icons.Default.Error
                }
                
                val iconColor = when (scanState) {
                    ScanState.IDLE -> MaterialTheme.colorScheme.primary
                    ScanState.TAG_DETECTED -> MaterialTheme.colorScheme.secondary
                    ScanState.PROCESSING -> MaterialTheme.colorScheme.primary
                    ScanState.SUCCESS -> MaterialTheme.colorScheme.primary
                    ScanState.ERROR -> MaterialTheme.colorScheme.error
                }
                
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Show progress bar when scanning
            if (scanState == ScanState.PROCESSING && scanProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { scanProgress.percentage },
                    modifier = Modifier.fillMaxWidth()
                )
                if (scanProgress.currentSector > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sector ${scanProgress.currentSector}/${scanProgress.totalSectors}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Preview functions

@Preview(showBackground = true)
@Composable
fun ComponentCardPreview() {
    MaterialTheme {
        ComponentCard(
            component = Component(
                id = "PLA_RED_001",
                name = "Red PLA Filament",
                category = "filament",
                massGrams = 1000f
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScanHistoryCardPreview() {
    MaterialTheme {
        ScanHistoryCard(
            scanData = DecryptedScanData(
                tagUid = "A1B2C3D4",
                timestamp = LocalDateTime.now(),
                technology = "Mifare Classic 1K",
                scanResult = com.bscan.model.ScanResult.SUCCESS,
                decryptedBlocks = mapOf(1 to "0102030405060708", 2 to "090A0B0C0D0E0F10"),
                authenticatedSectors = listOf(1, 2),
                failedSectors = emptyList(),
                usedKeys = emptyMap(),
                derivedKeys = emptyList(),
                errors = emptyList()
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CompactScanPromptPreview() {
    MaterialTheme {
        CompactScanPrompt(
            scanState = ScanState.IDLE
        )
    }
}