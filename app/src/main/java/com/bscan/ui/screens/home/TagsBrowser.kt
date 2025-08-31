package com.bscan.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsBrowser(
    allScans: List<Activity>,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    
    // Filter scan activities and group by tag UID
    val scanActivities = remember(allScans) {
        allScans.filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
    }
    
    val groupedScans by remember(scanActivities) {
        derivedStateOf {
            scanActivities
                .groupBy { activity -> 
                    activity.getProperty<String>("tagUid") ?: "Unknown"
                }
                .filter { it.key != "Unknown" }
                .toList()
                .sortedByDescending { (_, activities) -> 
                    activities.maxOfOrNull { activity ->
                        activity.getProperty<String>("timestamp")?.let { 
                            java.time.LocalDateTime.parse(it)
                        } ?: java.time.LocalDateTime.MIN
                    } ?: java.time.LocalDateTime.MIN
                }
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
        
        // Statistics summary
        if (groupedScans.isNotEmpty()) {
            item {
                TagsSummaryCard(
                    totalTags = groupedScans.size,
                    totalScans = scanActivities.size,
                    successRate = calculateSuccessRate(scanActivities)
                )
            }
        }
        
        // Tag groups
        if (groupedScans.isEmpty()) {
            item {
                EmptyTagsState()
            }
        } else {
            items(groupedScans) { (tagUid, scans) ->
                TagGroupCard(
                    tagUid = tagUid,
                    scans = scans,
                    onNavigateToDetails = onNavigateToDetails
                )
            }
        }
    }
}

@Composable
private fun TagsSummaryCard(
    totalTags: Int,
    totalScans: Int,
    successRate: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tags Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = totalTags.toString(),
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagGroupCard(
    tagUid: String,
    scans: List<Activity>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val successfulScans = scans.count { it.getProperty<Boolean>("success") == true }
    val successRate = if (scans.isNotEmpty()) successfulScans.toFloat() / scans.size else 0f
    val latestScan = scans.maxByOrNull { activity ->
        activity.getProperty<String>("timestamp")?.let { 
            java.time.LocalDateTime.parse(it)
        } ?: java.time.LocalDateTime.MIN
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { 
            onNavigateToDetails?.invoke(DetailType.TAG, tagUid)
        }
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
                
                Text(
                    text = "${scans.size} scans • ${(successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                latestScan?.getProperty<String>("timestamp")?.let { timestampStr ->
                    val displayTimestamp = remember(timestampStr) {
                        try {
                            val timestamp = java.time.LocalDateTime.parse(timestampStr)
                            "Latest: ${timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}"
                        } catch (e: Exception) {
                            null // Handle parsing error gracefully
                        }
                    }
                    displayTimestamp?.let { displayText ->
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View latest scan details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Helper function to calculate success rate from activities
private fun calculateSuccessRate(activities: List<Activity>): Float {
    if (activities.isEmpty()) return 0f
    val successfulScans = activities.count { it.getProperty<Boolean>("success") == true }
    return successfulScans.toFloat() / activities.size
}

@Composable
private fun EmptyTagsState(
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
                imageVector = Icons.Default.Tag,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "No tags scanned yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = "Scan an NFC tag to see tag groups here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}