package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UniqueSpool
import com.bscan.ui.components.ColorPreviewCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolListScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    var spools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var filterByType by remember { mutableStateOf("All Types") }
    
    LaunchedEffect(Unit) {
        try {
            spools = repository.getUniqueSpools()
        } catch (e: Exception) {
            spools = emptyList()
        }
    }
    
    // Apply filters
    val filteredSpools = spools.filter { spool ->
        val matchesSuccessFilter = when (selectedFilter) {
            "Successful Only" -> spool.successCount > 0
            "High Success Rate" -> spool.successRate >= 0.8f
            else -> true
        }
        
        val matchesTypeFilter = when (filterByType) {
            "All Types" -> true
            else -> spool.filamentInfo.filamentType == filterByType || 
                    spool.filamentInfo.detailedFilamentType == filterByType
        }
        
        matchesSuccessFilter && matchesTypeFilter
    }
    
    // Get unique filament types for filter
    val availableTypes = remember(repository) {
        try {
            val types = repository.getFilamentTypes()
            listOf("All Types") + types
        } catch (e: Exception) {
            listOf("All Types")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spool Collection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statistics Card
            item {
                SpoolStatisticsCard(spools = spools)
            }
            
            // Filter Row
            item {
                SpoolFilterSection(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { selectedFilter = it },
                    selectedTypeFilter = filterByType,
                    onTypeFilterChanged = { filterByType = it },
                    availableTypes = availableTypes
                )
            }
            
            // Spools List
            items(filteredSpools) { spool ->
                SpoolCard(spool = spool)
            }
            
            // Empty state
            if (filteredSpools.isEmpty()) {
                item {
                    EmptySpoolListMessage(
                        hasSpools = spools.isNotEmpty(),
                        currentFilter = selectedFilter
                    )
                }
            }
        }
    }
}

@Composable
private fun SpoolStatisticsCard(spools: List<UniqueSpool>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val totalSpools = spools.size
                val uniqueTypes = spools.map { it.filamentInfo.filamentType }.toSet().size
                val totalScans = spools.sumOf { it.scanCount }
                val avgSuccessRate = if (spools.isNotEmpty()) {
                    (spools.sumOf { it.successRate.toDouble() } / spools.size * 100).toInt()
                } else 0
                
                StatItem("Unique Spools", totalSpools.toString())
                StatItem("Types", uniqueTypes.toString())
                StatItem("Total Scans", totalScans.toString())
                StatItem("Avg Success", "$avgSuccessRate%")
            }
            
            // Most recent scan info
            spools.maxByOrNull { it.lastScanned }?.let { mostRecent ->
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
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun SpoolFilterSection(
    selectedFilter: String,
    onFilterChanged: (String) -> Unit,
    selectedTypeFilter: String,
    onTypeFilterChanged: (String) -> Unit,
    availableTypes: List<String>
) {
    Column(
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
private fun SpoolCard(spool: UniqueSpool) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview with name
            ColorPreviewCard(
                colorHex = spool.filamentInfo.colorHex,
                colorName = spool.filamentInfo.colorName,
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
                        text = spool.filamentInfo.detailedFilamentType.ifEmpty { spool.filamentInfo.filamentType },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    if (spool.filamentInfo.detailedFilamentType.isNotEmpty() && 
                        spool.filamentInfo.detailedFilamentType != spool.filamentInfo.filamentType) {
                        Text(
                            text = spool.filamentInfo.filamentType,
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
                        text = "${spool.scanCount} scans",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(spool.successRate * 100).toInt()}% success rate",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (spool.successRate >= 0.8f) {
                            MaterialTheme.colorScheme.primary
                        } else if (spool.successRate >= 0.5f) {
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
                        text = spool.lastScanned.format(DateTimeFormatter.ofPattern("MMM dd")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = spool.lastScanned.format(DateTimeFormatter.ofPattern("yyyy HH:mm")),
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
                        text = spool.uid,
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
                        spool.successRate >= 0.8f -> Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "High Success Rate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        spool.successRate >= 0.5f -> Icon(
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
private fun EmptySpoolListMessage(
    hasSpools: Boolean,
    currentFilter: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (hasSpools) Icons.Default.FilterList else Icons.Default.Nfc,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val (title, subtitle) = if (hasSpools) {
            when (currentFilter) {
                "Successful Only" -> "No successfully scanned spools" to "Try adjusting your filters"
                "High Success Rate" -> "No high-success spools found" to "Spools with 80%+ success rate will appear here"
                else -> "No spools match your filters" to "Try adjusting your filters"
            }
        } else {
            "No spools in your collection yet" to "Scan NFC tags to build your spool collection"
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

