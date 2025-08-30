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
import com.bscan.model.DecryptedScanData
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.TagStatistics
import com.bscan.ui.components.scans.RawDataView
import com.bscan.ui.components.scans.EncryptedDataView
import com.bscan.ui.components.scans.DecryptedDataView
import com.bscan.ui.screens.DetailType
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsBrowser(
    allScans: List<DecryptedScanData>,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    
    // Group scans by tag UID
    val groupedScans by remember(allScans) {
        derivedStateOf {
            repository.getScansGroupedByTagUid()
                .entries
                .sortedByDescending { (_, scans) -> 
                    scans.maxOfOrNull { it.timestamp } ?: java.time.LocalDateTime.MIN
                }
        }
    }
    
    // Track expanded tag groups
    var expandedTags by remember { mutableStateOf(setOf<String>()) }
    
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
                    totalScans = allScans.size,
                    successRate = repository.getSuccessRate()
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
                    statistics = repository.getTagStatistics(tagUid),
                    isExpanded = expandedTags.contains(tagUid),
                    onToggleExpanded = { 
                        expandedTags = if (expandedTags.contains(tagUid)) {
                            expandedTags - tagUid
                        } else {
                            expandedTags + tagUid
                        }
                    },
                    repository = repository,
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagGroupCard(
    tagUid: String,
    scans: List<DecryptedScanData>,
    statistics: TagStatistics,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    repository: ScanHistoryRepository,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedScanIndex by remember { mutableIntStateOf(0) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onToggleExpanded() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with tag info
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "${statistics.totalScans} scans • ${(statistics.successRate * 100).toInt()}% success",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    statistics.latestScanTimestamp?.let { timestamp ->
                        Text(
                            text = "Latest: ${timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(onClick = onToggleExpanded) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }
            
            // Expanded content
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Scan selection dropdown if multiple scans
                if (scans.size > 1) {
                    var showScanSelector by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedButton(
                            onClick = { showScanSelector = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan ${selectedScanIndex + 1} of ${scans.size} - ${scans[selectedScanIndex].timestamp.format(DateTimeFormatter.ofPattern("MMM dd HH:mm"))}")
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select scan")
                        }
                        
                        DropdownMenu(
                            expanded = showScanSelector,
                            onDismissRequest = { showScanSelector = false }
                        ) {
                            scans.forEachIndexed { index, scan ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = scan.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = scan.scanResult.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (scan.scanResult.name == "SUCCESS") 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.error
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedScanIndex = index
                                        showScanSelector = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                val selectedScan = scans.getOrNull(selectedScanIndex) ?: scans.first()
                val encryptedScan = repository.getEncryptedScanForDecrypted(selectedScan)
                
                // Tab row for data views
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Raw Data") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Encrypted") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Decrypted") }
                    )
                }
                
                // Content based on selected tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            if (encryptedScan != null) {
                                RawDataView(encryptedScanData = encryptedScan)
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Raw data not available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        1 -> {
                            if (encryptedScan != null) {
                                EncryptedDataView(encryptedScanData = encryptedScan)
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Encrypted data not available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        2 -> {
                            DecryptedDataView(decryptedScanData = selectedScan)
                        }
                    }
                }
            }
        }
    }
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