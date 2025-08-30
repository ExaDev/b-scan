package com.bscan.ui.components.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bscan.model.ScanResult
import com.bscan.repository.ScanHistoryRepository
import com.bscan.model.DecryptedScanData
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.screens.DetailType
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

@Composable
fun ScanStatisticsCard(
    repository: ScanHistoryRepository, 
    scans: List<DecryptedScanData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val successfulScans = scans.count { it.scanResult == ScanResult.SUCCESS }
            val failedScans = scans.size - successfulScans
            val successRate = if (scans.isNotEmpty()) (successfulScans.toFloat() / scans.size * 100).toInt() else 0
            
            val statistics = listOf(
                "Total Scans" to scans.size.toString(),
                "Success Rate" to "$successRate%",
                "Successful" to successfulScans.toString(),
                "Failed" to failedScans.toString()
            )
            
            StatisticGrid(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ScanHistoryFilters(
    selectedFilter: String, 
    onFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("All", "Success", "Failed").forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(filter) }
            )
        }
    }
}

@Composable
fun ScanHistoryCard(
    scan: DecryptedScanData,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    onScanClick: ((DetailType, String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { 
            val scanId = "${scan.timestamp.toString().replace(":", "-").replace(".", "-")}_${scan.tagUid}"
            onScanClick?.invoke(DetailType.SCAN, scanId)
        },
        colors = CardDefaults.cardColors(
            containerColor = when (scan.scanResult) {
                ScanResult.SUCCESS -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.tagUid,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = scan.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScanResultBadge(scan.scanResult)
                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            // Basic Info - Show scan result summary
            if (scan.scanResult == ScanResult.SUCCESS) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scan successful • ${scan.decryptedBlocks.size} blocks decrypted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Expanded Debug Info
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                ScanDebugSection(scan)
            }
        }
    }
}

@Composable
fun ScanResultBadge(
    result: ScanResult,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (result) {
        ScanResult.SUCCESS -> Triple(MaterialTheme.colorScheme.primary, Icons.Default.CheckCircle, "Success")
        ScanResult.AUTHENTICATION_FAILED -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Lock, "Auth Failed")
        ScanResult.INSUFFICIENT_DATA -> Triple(MaterialTheme.colorScheme.secondary, Icons.Default.Warning, "No Data")
        ScanResult.PARSING_FAILED -> Triple(MaterialTheme.colorScheme.error, Icons.Default.Error, "Parse Error")
        ScanResult.NO_NFC_TAG -> Triple(MaterialTheme.colorScheme.outline, Icons.Default.Nfc, "No Tag")
        ScanResult.UNKNOWN_ERROR -> Triple(MaterialTheme.colorScheme.error, Icons.AutoMirrored.Filled.Help, "Error")
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
fun ScanDebugSection(
    scan: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Debug Information",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        
        // Technical Details
        DebugInfoRow("Technology", scan.technology)
        DebugInfoRow("Tag UID", scan.tagUid)
        DebugInfoRow("Authenticated Sectors", scan.authenticatedSectors.size.toString())
        DebugInfoRow("Failed Sectors", scan.failedSectors.size.toString())
        DebugInfoRow("Decrypted Blocks", scan.decryptedBlocks.size.toString())
        
        // Authentication Details
        if (scan.authenticatedSectors.isNotEmpty()) {
            Text(
                text = "Authentication Success:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Sectors: ${scan.authenticatedSectors.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        
        if (scan.failedSectors.isNotEmpty()) {
            Text(
                text = "Authentication Failed:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Sectors: ${scan.failedSectors.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
        
        // Error Messages
        if (scan.errors.isNotEmpty()) {
            Text(
                text = "Errors:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            scan.errors.forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // Key block data
        if (scan.decryptedBlocks.isNotEmpty()) {
            Text(
                text = "Block Data (First 6 blocks):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            scan.decryptedBlocks.entries.take(6).forEach { (block, data) ->
                Text(
                    text = "Block $block: $data",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun DebugInfoRow(
    label: String, 
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ScanHistoryEmptyState(
    filter: String,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (filter) {
        "Success" -> "No successful scans yet" to "Scan an NFC tag to see successful scans here"
        "Failed" -> "No failed scans" to "Failed scans will appear here"
        else -> "No scan history yet" to "Scan an NFC tag to see history here"
    }
    
    EmptyStateView(
        icon = Icons.Default.History,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}

// Helper function to create mock successful scan
private fun createMockSuccessfulScan(): DecryptedScanData {
    return com.bscan.model.DecryptedScanData(
        timestamp = java.time.LocalDateTime.now().minusHours(2),
        tagUid = "A1B2C3D4",
        technology = "NFC-A",
        scanResult = ScanResult.SUCCESS,
        decryptedBlocks = mapOf(
            4 to "474641303A413030D2DA4B30000000",
            5 to "000000000000000000000000000000",
            6 to "123456789ABCDEF0123456789ABCDEF"
        ),
        authenticatedSectors = listOf(1, 2, 3, 4),
        failedSectors = emptyList(),
        usedKeys = mapOf(1 to "KeyA", 2 to "KeyA"),
        derivedKeys = listOf("A1B2C3D4E5F6"),
        errors = emptyList()
    )
}

// Helper function to create mock failed scan  
private fun createMockFailedScan(): DecryptedScanData {
    return com.bscan.model.DecryptedScanData(
        timestamp = java.time.LocalDateTime.now().minusHours(1),
        tagUid = "E5F6A7B8", 
        technology = "NFC-A",
        scanResult = ScanResult.AUTHENTICATION_FAILED,
        decryptedBlocks = emptyMap(),
        authenticatedSectors = emptyList(),
        failedSectors = listOf(1, 2, 3, 4),
        usedKeys = emptyMap(),
        derivedKeys = emptyList(),
        errors = listOf("Authentication failed for sector 1")
    )
}

@Preview(showBackground = true)
@Composable
fun ScanStatisticsCardPreview() {
    MaterialTheme {
        val mockScans = listOf(
            createMockSuccessfulScan(),
            createMockFailedScan()
        )
        
        ScanStatisticsCard(
            repository = com.bscan.repository.ScanHistoryRepository(androidx.compose.ui.platform.LocalContext.current),
            scans = mockScans
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScanHistoryFiltersPreview() {
    MaterialTheme {
        ScanHistoryFilters(
            selectedFilter = "All",
            onFilterChanged = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScanHistoryCardPreview() {
    MaterialTheme {
        ScanHistoryCard(
            scan = createMockSuccessfulScan(),
            isExpanded = false,
            onToggleExpanded = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScanResultBadgePreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            ScanResultBadge(ScanResult.SUCCESS)
            ScanResultBadge(ScanResult.AUTHENTICATION_FAILED)
            ScanResultBadge(ScanResult.PARSING_FAILED)
            ScanResultBadge(ScanResult.NO_NFC_TAG)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanHistoryEmptyStatePreview() {
    MaterialTheme {
        ScanHistoryEmptyState(filter = "All")
    }
}