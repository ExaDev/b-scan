package com.bscan.ui.components.spool

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.repository.FilamentReelDetails
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolOverviewCard(
    spoolDetails: FilamentReelDetails,
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
                text = "Filament Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            val successRate = if (spoolDetails.totalScans > 0) {
                (spoolDetails.successfulScans.toFloat() / spoolDetails.totalScans * 100).toInt()
            } else 0
            
            StatisticGrid(
                statistics = listOf(
                    "Tray UID" to spoolDetails.trayUid,
                    "Associated Tags" to "${spoolDetails.tagUids.size} tags",
                    "Total Scans" to spoolDetails.totalScans.toString(),
                    "Successful Scans" to spoolDetails.successfulScans.toString(),
                    "Success Rate" to "$successRate%",
                    "Last Scanned" to spoolDetails.lastScanned.format(formatter)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssociatedTagsCard(
    spoolDetails: FilamentReelDetails,
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
                text = "Associated NFC Tags",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            spoolDetails.tagUids.forEach { tagUid ->
                val tagScans = spoolDetails.scansByTag[tagUid] ?: emptyList()
                val tagSuccessCount = tagScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
                val tagSuccessRate = if (tagScans.isNotEmpty()) {
                    (tagSuccessCount.toFloat() / tagScans.size * 100).toInt()
                } else 0
                
                TagInfoRow(
                    tagUid = tagUid,
                    scanCount = tagScans.size,
                    successCount = tagSuccessCount,
                    successRate = tagSuccessRate
                )
                
                if (tagUid != spoolDetails.tagUids.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TagInfoRow(
    tagUid: String,
    scanCount: Int,
    successCount: Int,
    successRate: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Nfc,
            contentDescription = "NFC Tag",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tagUid,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "$scanCount scans • $successCount successful ($successRate%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpoolOverviewCardPreview() {
    MaterialTheme {
        val mockSpoolDetails = FilamentReelDetails(
            trayUid = "01008023456789ABCDEF",
            tagUids = listOf("A1B2C3D4", "E5F6A7B8", "C9D0E1F2"),
            totalScans = 45,
            successfulScans = 42,
            lastScanned = LocalDateTime.of(2024, 3, 15, 14, 30),
            scansByTag = mapOf(
                "A1B2C3D4" to emptyList(),
                "E5F6A7B8" to emptyList(),
                "C9D0E1F2" to emptyList()
            )
        )
        
        SpoolOverviewCard(spoolDetails = mockSpoolDetails)
    }
}

@Preview(showBackground = true)  
@Composable
fun AssociatedTagsCardPreview() {
    MaterialTheme {
        val mockSpoolDetails = FilamentReelDetails(
            trayUid = "01008023456789ABCDEF",
            tagUids = listOf("A1B2C3D4", "E5F6A7B8"),
            totalScans = 30,
            successfulScans = 28,
            lastScanned = LocalDateTime.of(2024, 3, 15, 14, 30),
            scansByTag = mapOf(
                "A1B2C3D4" to emptyList(),
                "E5F6A7B8" to emptyList()
            )
        )
        
        AssociatedTagsCard(spoolDetails = mockSpoolDetails)
    }
}

@Preview(showBackground = true)
@Composable
fun TagInfoRowPreview() {
    MaterialTheme {
        TagInfoRow(
            tagUid = "A1B2C3D4E5F6",
            scanCount = 15,
            successCount = 14,
            successRate = 93
        )
    }
}