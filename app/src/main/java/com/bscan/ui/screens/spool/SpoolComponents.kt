package com.bscan.ui.screens.spool

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.repository.UniqueFilamentReel
import com.bscan.ui.components.ColorPreviewCard
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

@Composable
fun FilamentReelStatisticsCard(
    filamentReels: List<UniqueFilamentReel>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Collection Statistics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val totalFilamentReels = filamentReels.size
            val uniqueTypes = filamentReels.map { it.filamentInfo.filamentType }.toSet().size
            val totalScans = filamentReels.sumOf { it.scanCount }
            val avgSuccessRate = if (filamentReels.isNotEmpty()) {
                (filamentReels.sumOf { it.successRate.toDouble() } / filamentReels.size * 100).toInt()
            } else 0
            
            val statistics = listOf(
                "Unique Reels" to totalFilamentReels.toString(),
                "Types" to uniqueTypes.toString(),
                "Total Scans" to totalScans.toString(),
                "Avg Success" to "$avgSuccessRate%"
            )
            
            StatisticGrid(
                statistics = statistics,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Most recent scan info
            filamentReels.maxByOrNull { it.lastScanned }?.let { mostRecent ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Most recent scan: ${mostRecent.lastScanned.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun FilamentReelFilterSection(
    selectedFilter: String,
    onFilterChanged: (String) -> Unit,
    selectedTypeFilter: String,
    onTypeFilterChanged: (String) -> Unit,
    availableTypes: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Success rate filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Successful Only", "High Success Rate").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter) }
                )
            }
        }
        
        // Type filter chips
        if (availableTypes.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(availableTypes) { type ->
                    FilterChip(
                        selected = selectedTypeFilter == type,
                        onClick = { onTypeFilterChanged(type) },
                        label = { Text(type) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilamentReelCard(
    filamentReel: UniqueFilamentReel,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = { onClick?.invoke(filamentReel.filamentInfo.trayUid) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview with name
            ColorPreviewCard(
                colorHex = filamentReel.filamentInfo.colorHex,
                colorName = filamentReel.filamentInfo.colorName,
                filamentType = filamentReel.filamentInfo.filamentType,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Filament type information
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Filament Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = filamentReel.filamentInfo.detailedFilamentType.ifEmpty { filamentReel.filamentInfo.filamentType },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (filamentReel.filamentInfo.detailedFilamentType.isNotEmpty() && 
                        filamentReel.filamentInfo.detailedFilamentType != filamentReel.filamentInfo.filamentType) {
                        Text(
                            text = filamentReel.filamentInfo.filamentType,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Scan statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scan count and success rate
                Column {
                    Text(
                        text = "Scan History",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${filamentReel.scanCount} scans",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(filamentReel.successRate * 100).toInt()}% success rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (filamentReel.successRate >= 0.8f) {
                            MaterialTheme.colorScheme.primary
                        } else if (filamentReel.successRate >= 0.5f) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                
                // Last scanned
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Last Scanned",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = filamentReel.lastScanned.format(DateTimeFormatter.ofPattern("MMM dd")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = filamentReel.lastScanned.format(DateTimeFormatter.ofPattern("yyyy HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // UID information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tag UID",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = filamentReel.uid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
                
                // Success rate indicator
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                ) {
                    when {
                        filamentReel.successRate >= 0.8f -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "High Success Rate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        filamentReel.successRate >= 0.5f -> Icon(
                            Icons.Default.Warning,
                            contentDescription = "Medium Success Rate",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            Icons.Default.Error,
                            contentDescription = "Low Success Rate",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilamentReelListEmptyState(
    hasFilamentReels: Boolean,
    currentFilter: String,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, icon) = if (hasFilamentReels) {
        when (currentFilter) {
            "Successful Only" -> Triple(
                "No successfully scanned reels", 
                "Try adjusting your filters",
                Icons.Default.FilterList
            )
            "High Success Rate" -> Triple(
                "No high-success reels found",
                "Filament reels with 80%+ success rate will appear here",
                Icons.Default.FilterList
            )
            else -> Triple(
                "No filament reels match your filters",
                "Try adjusting your filters", 
                Icons.Default.FilterList
            )
        }
    } else {
        Triple(
            "No filament reels in your collection yet",
            "Scan NFC tags to build your filament collection",
            Icons.Default.Nfc
        )
    }
    
    EmptyStateView(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}