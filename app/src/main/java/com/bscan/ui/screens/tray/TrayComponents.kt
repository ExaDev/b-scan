package com.bscan.ui.screens.tray

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.repository.TrayData
import com.bscan.repository.TrayStatistics
import com.bscan.ui.components.ColorPreviewDot
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

// Utility functions for tray UID display
fun formatTrayId(trayUid: String): String {
    // Tray UID is already in hex format from BambuTagDecoder
    // Git-style: use last 8 characters of hex string
    return if (trayUid.length > 8) {
        trayUid.takeLast(8)
    } else {
        trayUid
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrayStatisticsCard(
    statistics: TrayStatistics,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tray Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            val mainStats = listOf(
                "Total Trays" to statistics.totalTrays.toString(),
                "Unique Tags" to statistics.totalUniqueTags.toString(),
                "Total Scans" to statistics.totalScans.toString()
            )
            
            StatisticGrid(
                statistics = mainStats,
                modifier = Modifier.fillMaxWidth()
            )
            
            val secondaryStats = listOf(
                "Avg Tags/Tray" to "%.1f".format(statistics.averageTagsPerTray),
                "Avg Scans/Tray" to "%.1f".format(statistics.averageScansPerTray)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                secondaryStats.forEach { (label, value) ->
                    StatisticDisplay(
                        label = label,
                        value = value
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Most active tray
            statistics.mostActiveTray?.let { tray ->
                HorizontalDivider()
                Text(
                    text = "Most Active: ${formatTrayId(tray.trayUid)} (${tray.totalScans} scans)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Tray with most tags
            statistics.trayWithMostTags?.let { tray ->
                Text(
                    text = "Most Tags: ${formatTrayId(tray.trayUid)} (${tray.uniqueTagCount} unique tags)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrayCard(
    trayData: TrayData,
    onDeleteTray: (TrayData) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with tray UID and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tray UID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTrayId(trayData.trayUid),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Full: ${trayData.trayUid}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = { onDeleteTray(trayData) },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove tray"
                    )
                }
            }
            
            // Statistics row
            val statistics = listOf(
                "Unique Tags" to trayData.uniqueTagCount.toString(),
                "Total Scans" to trayData.totalScans.toString(),
                "Filament Types" to trayData.filamentTypes.size.toString()
            )
            
            StatisticGrid(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Filament types and colors
            if (trayData.filamentTypes.isNotEmpty()) {
                Text(
                    text = "Filament Types:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = trayData.filamentTypes.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Colors preview
            if (trayData.colorNames.isNotEmpty()) {
                Text(
                    text = "Colors:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Show color dots for unique filaments
                    trayData.tagEntries.values.take(8).forEach { tagEntry ->
                        ColorPreviewDot(
                            colorHex = tagEntry.filamentInfo.colorHex,
                            size = 24.dp
                        )
                    }
                    if (trayData.tagEntries.size > 8) {
                        Text(
                            text = "+${trayData.tagEntries.size - 8}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Timestamps
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "First Seen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = trayData.firstSeen.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = trayData.lastUpdated.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun TrayTrackingEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.Storage,
        title = "No Trays Tracked",
        subtitle = "Scan some filament tags to start tracking trays",
        modifier = modifier
    )
}