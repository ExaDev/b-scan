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
import com.bscan.model.DecryptedScanData
import com.bscan.model.ScanResult
import com.bscan.repository.ScanHistoryRepository
import com.bscan.ui.components.scans.RawDataView
import com.bscan.ui.components.scans.EncryptedDataView
import com.bscan.ui.components.scans.DecryptedDataView
import com.bscan.ui.screens.DetailType
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScansBrowser(
    allScans: List<DecryptedScanData>,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    
    // Filter state
    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Success", "Failed")
    
    // Filter scans based on selected filter
    val filteredScans by remember(allScans, selectedFilter) {
        derivedStateOf {
            when (selectedFilter) {
                "Success" -> allScans.filter { it.scanResult == ScanResult.SUCCESS }
                "Failed" -> allScans.filter { it.scanResult != ScanResult.SUCCESS }
                else -> allScans
            }.sortedByDescending { it.timestamp } // Most recent first
        }
    }
    
    // Track expanded scans
    var expandedScans by remember { mutableStateOf(setOf<String>()) }
    
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
        if (allScans.isNotEmpty()) {
            item {
                ScansSummaryCard(
                    totalScans = allScans.size,
                    filteredScans = filteredScans.size,
                    selectedFilter = selectedFilter,
                    filterOptions = filterOptions,
                    onFilterChanged = { selectedFilter = it },
                    repository = repository
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
                val scanId = "${scan.timestamp}-${scan.tagUid}"
                ScanCard(
                    scan = scan,
                    isExpanded = expandedScans.contains(scanId),
                    onToggleExpanded = {
                        expandedScans = if (expandedScans.contains(scanId)) {
                            expandedScans - scanId
                        } else {
                            expandedScans + scanId
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
private fun ScansSummaryCard(
    totalScans: Int,
    filteredScans: Int,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterChanged: (String) -> Unit,
    repository: ScanHistoryRepository,
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
                        text = "${(repository.getSuccessRate() * 100).toInt()}%",
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
                        text = repository.getUniqueTagCount().toString(),
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
                                    "Success" -> "Success (${totalScans - (totalScans - (repository.getSuccessRate() * totalScans).toInt())})"
                                    "Failed" -> "Failed (${totalScans - (repository.getSuccessRate() * totalScans).toInt()})"
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ScanCard(
    scan: DecryptedScanData,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    repository: ScanHistoryRepository,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val encryptedScan = repository.getEncryptedScanForDecrypted(scan)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onToggleExpanded() },
        colors = CardDefaults.cardColors(
            containerColor = when (scan.scanResult) {
                ScanResult.SUCCESS -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with scan info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scan.tagUid,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = scan.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanResultChip(scanResult = scan.scanResult)
                        Text(
                            text = scan.technology,
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
            
            // Expanded content with tabs
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
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
                                DataNotAvailableMessage("Raw data not available")
                            }
                        }
                        1 -> {
                            if (encryptedScan != null) {
                                EncryptedDataView(encryptedScanData = encryptedScan)
                            } else {
                                DataNotAvailableMessage("Encrypted data not available")
                            }
                        }
                        2 -> {
                            DecryptedDataView(decryptedScanData = scan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanResultChip(
    scanResult: ScanResult,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (scanResult) {
        ScanResult.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primary, 
            Icons.Default.CheckCircle, 
            "Success"
        )
        ScanResult.AUTHENTICATION_FAILED -> Triple(
            MaterialTheme.colorScheme.error, 
            Icons.Default.Lock, 
            "Auth Failed"
        )
        ScanResult.INSUFFICIENT_DATA -> Triple(
            MaterialTheme.colorScheme.secondary, 
            Icons.Default.Warning, 
            "No Data"
        )
        ScanResult.PARSING_FAILED -> Triple(
            MaterialTheme.colorScheme.error, 
            Icons.Default.Error, 
            "Parse Error"
        )
        ScanResult.NO_NFC_TAG -> Triple(
            MaterialTheme.colorScheme.outline, 
            Icons.Default.Nfc, 
            "No Tag"
        )
        ScanResult.UNKNOWN_ERROR -> Triple(
            MaterialTheme.colorScheme.error, 
            Icons.Default.Help, 
            "Error"
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