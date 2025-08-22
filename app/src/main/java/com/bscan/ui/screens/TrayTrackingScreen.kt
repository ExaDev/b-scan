package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bscan.MainViewModel
import com.bscan.repository.TrayData
import com.bscan.repository.TrayStatistics
import com.bscan.ui.components.ColorPreviewDot
import java.time.format.DateTimeFormatter

// Utility functions for tray UID display
private fun formatTrayId(trayUid: String): String {
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
fun TrayTrackingScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trayTrackingRepository = viewModel.getTrayTrackingRepository()
    
    // Load tray data
    var trayData by remember { mutableStateOf<List<TrayData>>(emptyList()) }
    var statistics by remember { mutableStateOf<TrayStatistics?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Show delete confirmation dialog
    var trayToDelete by remember { mutableStateOf<TrayData?>(null) }
    
    LaunchedEffect(Unit) {
        try {
            trayData = trayTrackingRepository.getAllTrays()
            statistics = trayTrackingRepository.getTrayStatistics()
        } finally {
            isLoading = false
        }
    }
    
    // Delete confirmation dialog
    trayToDelete?.let { tray ->
        AlertDialog(
            onDismissRequest = { trayToDelete = null },
            title = { Text("Remove Tray") },
            text = { 
                Text("Remove tray ${formatTrayId(tray.trayUid)} and all its tracking data? This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        trayTrackingRepository.removeTray(tray.trayUid)
                        trayData = trayTrackingRepository.getAllTrays()
                        statistics = trayTrackingRepository.getTrayStatistics()
                        trayToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { trayToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tray Tracking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        
        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (trayData.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No Trays Tracked",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Scan some filament tags to start tracking trays",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics summary
                statistics?.let { stats ->
                    item {
                        TrayStatisticsCard(statistics = stats)
                    }
                }
                
                // Individual tray cards
                items(trayData) { tray ->
                    TrayCard(
                        trayData = tray,
                        onDeleteTray = { trayToDelete = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrayStatisticsCard(
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Total Trays",
                    value = statistics.totalTrays.toString()
                )
                StatisticItem(
                    label = "Unique Tags",
                    value = statistics.totalUniqueTags.toString()
                )
                StatisticItem(
                    label = "Total Scans",
                    value = statistics.totalScans.toString()
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Avg Tags/Tray",
                    value = "%.1f".format(statistics.averageTagsPerTray)
                )
                StatisticItem(
                    label = "Avg Scans/Tray",
                    value = "%.1f".format(statistics.averageScansPerTray)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
            
            // Most active tray
            statistics.mostActiveTray?.let { tray ->
                Divider()
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

@Composable
private fun StatisticItem(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrayCard(
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Unique Tags",
                    value = trayData.uniqueTagCount.toString()
                )
                StatisticItem(
                    label = "Total Scans",
                    value = trayData.totalScans.toString()
                )
                StatisticItem(
                    label = "Filament Types",
                    value = trayData.filamentTypes.size.toString()
                )
            }
            
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
            Divider()
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