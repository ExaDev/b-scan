package com.bscan.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.graph.entities.Activity
import com.bscan.model.graph.entities.ActivityTypes
import com.bscan.repository.GraphRepository
import com.bscan.ui.components.scans.EncodedDataView
import com.bscan.ui.components.scans.DecodedDataView
import com.bscan.ui.components.scans.DecryptedDataView
import com.bscan.ui.screens.DetailType
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScansBrowser(
    allScans: List<Activity>,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    
    // Filter scan activities
    val scanActivities = remember(allScans) {
        allScans.filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
    }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Success", "Failed")
    
    // Filter scans based on selected filter
    val filteredScans by remember(scanActivities, selectedFilter) {
        derivedStateOf {
            when (selectedFilter) {
                "Success" -> scanActivities.filter { it.getProperty<Boolean>("success") == true }
                "Failed" -> scanActivities.filter { it.getProperty<Boolean>("success") != true }
                else -> scanActivities
            }.sortedByDescending { activity ->
                activity.getProperty<String>("timestamp")?.let { 
                    java.time.LocalDateTime.parse(it)
                } ?: java.time.LocalDateTime.MIN
            } // Most recent first
        }
    }
    
    
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Spacer for first item positioning
        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Statistics and filter section
        if (scanActivities.isNotEmpty()) {
            item {
                ScansSummaryCard(
                    totalScans = scanActivities.size,
                    filteredScans = filteredScans.size,
                    selectedFilter = selectedFilter,
                    filterOptions = filterOptions,
                    onFilterChanged = { selectedFilter = it },
                    activities = scanActivities
                )
            }
        }
        
        // Scans list
        if (filteredScans.isEmpty()) {
            item {
                EmptyScansState(filter = selectedFilter)
            }
        } else {
            items(filteredScans) { scan ->
                val timestamp = scan.getProperty<String>("timestamp") ?: ""
                val tagUid = scan.getProperty<String>("tagUid") ?: ""
                val scanId = "$timestamp-$tagUid"
                ScanCard(
                    scan = scan,
                    onNavigateToDetails = onNavigateToDetails
                )
            }
        }
    }
}

@Composable
private fun ScansSummaryCard(
    totalScans: Int,
    filteredScans: Int,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterChanged: (String) -> Unit,
    activities: List<Activity>,
    modifier: Modifier = Modifier
) {
    val successRate = if (activities.isNotEmpty()) {
        activities.count { it.getProperty<Boolean>("success") == true }.toFloat() / activities.size
    } else 0f
    
    val uniqueTagsCount = activities
        .mapNotNull { it.getProperty<String>("tagUid") }
        .distinct()
        .size
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Scans Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = totalScans.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Total Scans",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column {
                    Text(
                        text = "${(successRate * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Success Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column {
                    Text(
                        text = uniqueTagsCount.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Unique Tags",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { onFilterChanged(filter) },
                        label = { 
                            Text(
                                text = when (filter) {
                                    "All" -> "All ($totalScans)"
                                    "Success" -> "Success (${(successRate * totalScans).toInt()})"
                                    "Failed" -> "Failed (${totalScans - (successRate * totalScans).toInt()})"
                                    else -> filter
                                }
                            )
                        }
                    )
                }
            }
            
            if (selectedFilter != "All") {
                Text(
                    text = "Showing $filteredScans of $totalScans scans",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanCard(
    scan: Activity,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val timestamp = scan.getProperty<String>("timestamp") ?: ""
    val tagUid = scan.getProperty<String>("tagUid") ?: "Unknown"
    val success = scan.getProperty<Boolean>("success") ?: false
    val technology = scan.getProperty<String>("technology") ?: "NFC"
    val scanId = "$timestamp-$tagUid"
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { 
            onNavigateToDetails?.invoke(DetailType.SCAN, scanId)
        },
        colors = CardDefaults.cardColors(
            containerColor = if (success) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tagUid,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                try {
                    val parsedTimestamp = java.time.LocalDateTime.parse(timestamp)
                    Text(
                        text = parsedTimestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } catch (e: Exception) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScanResultChip(success = success)
                    Text(
                        text = technology,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ScanResultChip(
    success: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = if (success) {
        Triple(
            MaterialTheme.colorScheme.primary, 
            Icons.Default.CheckCircle, 
            "Success"
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.error, 
            Icons.Default.Error, 
            "Failed"
        )
    }
    
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun DataNotAvailableMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyScansState(
    filter: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            val (title, subtitle) = when (filter) {
                "Success" -> "No successful scans yet" to "Successful scans will appear here"
                "Failed" -> "No failed scans" to "Failed scans will appear here"
                else -> "No scans yet" to "Scan an NFC tag to see scan history here"
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}