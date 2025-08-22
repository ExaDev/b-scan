package com.bscan.ui.screens

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UniqueSpool
import com.bscan.ui.components.ScanStateIndicator
import java.time.format.DateTimeFormatter

enum class ViewMode { SPOOLS, TAGS, SCANS }
enum class SortOption { MOST_RECENT, OLDEST, NAME, SUCCESS_RATE }

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    
    // View state
    var viewMode by remember { mutableStateOf(ViewMode.SPOOLS) }
    var sortOption by remember { mutableStateOf(SortOption.MOST_RECENT) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Data state
    var spools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var allScans by remember { mutableStateOf(listOf<ScanHistory>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Load data
    LaunchedEffect(Unit) {
        try {
            spools = repository.getUniqueSpools()
            allScans = repository.getAllScans()
        } catch (e: Exception) {
            spools = emptyList()
            allScans = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (spools.isEmpty() && allScans.isEmpty()) {
        // Show full scan prompt for first-time users
        ScanPromptScreen(modifier = modifier)
    } else {
        // Show data browser
        DataBrowserScreen(
            viewMode = viewMode,
            sortOption = sortOption,
            spools = spools,
            allScans = allScans,
            showSortMenu = showSortMenu,
            onViewModeChange = { viewMode = it },
            onSortOptionChange = { sortOption = it },
            onShowSortMenu = { showSortMenu = it },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DataBrowserScreen(
    viewMode: ViewMode,
    sortOption: SortOption,
    spools: List<UniqueSpool>,
    allScans: List<ScanHistory>,
    showSortMenu: Boolean,
    onViewModeChange: (ViewMode) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onShowSortMenu: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()
    
    // Scan prompt dimensions
    val scanPromptHeightDp = 100.dp
    val scanPromptHeightPx = with(density) { scanPromptHeightDp.toPx() }
    
    // Overscroll reveal state - start hidden for data view
    var overscrollOffset by remember { mutableFloatStateOf(0f) }
    var isRevealing by remember { mutableStateOf(false) }
    
    // Animated offset for smooth transitions
    val animatedOffset by animateFloatAsState(
        targetValue = if (isRevealing && overscrollOffset > scanPromptHeightPx * 0.4f) {
            scanPromptHeightPx // Fully revealed
        } else {
            0f // Hidden
        },
        animationSpec = SpringSpec(
            stiffness = 300f,
            dampingRatio = 0.8f
        ),
        label = "scan_prompt_reveal"
    )
    
    // NestedScrollConnection for scan prompt reveal/hide
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Handle hiding scan prompt when scrolling up
                if (available.y < 0 && overscrollOffset > 0) {
                    val consumed = minOf(-available.y, overscrollOffset)
                    overscrollOffset -= consumed
                    if (overscrollOffset <= scanPromptHeightPx * 0.1f) {
                        isRevealing = false
                        overscrollOffset = 0f
                    }
                    return Offset(0f, -consumed)
                }
                
                return Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Handle scan prompt reveal when pulling down from top
                if (available.y > 0) {
                    val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                    
                    if (isAtTop && overscrollOffset < scanPromptHeightPx) {
                        // Reveal scan prompt when pulling down from top
                        isRevealing = true
                        val newOffset = (overscrollOffset + available.y).coerceAtMost(scanPromptHeightPx)
                        val consumed = newOffset - overscrollOffset
                        overscrollOffset = newOffset
                        return Offset(0f, consumed)
                    }
                }
                
                return Offset.Zero
            }
            
            override suspend fun onPreFling(available: Velocity): Velocity {
                // Handle fling to snap open/closed based on pull distance
                if (overscrollOffset > 0 && overscrollOffset < scanPromptHeightPx) {
                    isRevealing = overscrollOffset > scanPromptHeightPx * 0.3f
                    if (!isRevealing) {
                        overscrollOffset = 0f
                    }
                }
                return Velocity.Zero
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Scan prompt that slides down from above
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(LocalDensity.current) { animatedOffset.toDp() })
                .graphicsLayer {
                    alpha = (animatedOffset / scanPromptHeightPx).coerceIn(0f, 1f)
                }
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (animatedOffset > 0) {
                CompactScanPrompt()
            }
        }
        
        // Tab row for view modes
        TabRow(
            selectedTabIndex = viewMode.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = viewMode == ViewMode.SPOOLS,
                onClick = { onViewModeChange(ViewMode.SPOOLS) },
                text = { Text("Spools") },
                icon = { Icon(Icons.Default.Storage, contentDescription = null) }
            )
            Tab(
                selected = viewMode == ViewMode.TAGS,
                onClick = { onViewModeChange(ViewMode.TAGS) },
                text = { Text("Tags") },
                icon = { Icon(Icons.Default.Tag, contentDescription = null) }
            )
            Tab(
                selected = viewMode == ViewMode.SCANS,
                onClick = { onViewModeChange(ViewMode.SCANS) },
                text = { Text("Scans") },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }
        
        // Controls row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Box {
                OutlinedButton(
                    onClick = { onShowSortMenu(true) }
                ) {
                    Text(
                        text = when (sortOption) {
                            SortOption.MOST_RECENT -> "Most Recent"
                            SortOption.OLDEST -> "Oldest"
                            SortOption.NAME -> "Name"
                            SortOption.SUCCESS_RATE -> "Success Rate"
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onShowSortMenu(false) }
                ) {
                    SortOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when (option) {
                                        SortOption.MOST_RECENT -> "Most Recent"
                                        SortOption.OLDEST -> "Oldest"
                                        SortOption.NAME -> "Name"
                                        SortOption.SUCCESS_RATE -> "Success Rate"
                                    }
                                )
                            },
                            onClick = {
                                onSortOptionChange(option)
                                onShowSortMenu(false)
                            }
                        )
                    }
                }
            }
        }
        
        // Data list
        when (viewMode) {
            ViewMode.SPOOLS -> SpoolsList(spools, sortOption, lazyListState)
            ViewMode.TAGS -> TagsList(allScans, sortOption, lazyListState)
            ViewMode.SCANS -> ScansList(allScans, sortOption, lazyListState)
        }
    }
}

@Composable
private fun SpoolsList(
    spools: List<UniqueSpool>,
    sortOption: SortOption,
    lazyListState: LazyListState
) {
    val sortedSpools = remember(spools, sortOption) {
        when (sortOption) {
            SortOption.MOST_RECENT -> spools.sortedByDescending { it.lastScanned }
            SortOption.OLDEST -> spools.sortedBy { it.lastScanned }
            SortOption.NAME -> spools.sortedBy { it.filamentInfo.colorName }
            SortOption.SUCCESS_RATE -> spools.sortedByDescending { it.successRate }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedSpools) { spool ->
            SpoolCard(spool = spool)
        }
    }
}

@Composable
private fun TagsList(
    allScans: List<ScanHistory>,
    sortOption: SortOption,
    lazyListState: LazyListState
) {
    // Group scans by tag UID to show unique tags
    val uniqueTags = remember(allScans) {
        allScans.groupBy { it.uid }
            .mapNotNull { (uid, scans) ->
                val mostRecentScan = scans.maxByOrNull { it.timestamp }
                val successfulScan = scans.firstOrNull { it.scanResult == ScanResult.SUCCESS }
                if (mostRecentScan != null) {
                    Triple(uid, mostRecentScan, successfulScan?.filamentInfo)
                } else null
            }
    }
    
    val sortedTags = remember(uniqueTags, sortOption) {
        when (sortOption) {
            SortOption.MOST_RECENT -> uniqueTags.sortedByDescending { it.second.timestamp }
            SortOption.OLDEST -> uniqueTags.sortedBy { it.second.timestamp }
            SortOption.NAME -> uniqueTags.sortedBy { it.third?.colorName ?: it.first }
            SortOption.SUCCESS_RATE -> {
                val tagSuccessRates = allScans.groupBy { it.uid }.mapValues { (_, scans) ->
                    scans.count { it.scanResult == ScanResult.SUCCESS }.toFloat() / scans.size
                }
                uniqueTags.sortedByDescending { tagSuccessRates[it.first] ?: 0f }
            }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedTags) { (uid, mostRecentScan, filamentInfo) ->
            TagCard(
                uid = uid,
                mostRecentScan = mostRecentScan,
                filamentInfo = filamentInfo,
                allScans = allScans
            )
        }
    }
}

@Composable
private fun ScansList(
    allScans: List<ScanHistory>,
    sortOption: SortOption,
    lazyListState: LazyListState
) {
    val sortedScans = remember(allScans, sortOption) {
        when (sortOption) {
            SortOption.MOST_RECENT -> allScans.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> allScans.sortedBy { it.timestamp }
            SortOption.NAME -> allScans.sortedBy { it.filamentInfo?.colorName ?: it.uid }
            SortOption.SUCCESS_RATE -> allScans.sortedBy { it.scanResult != ScanResult.SUCCESS }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedScans) { scan ->
            ScanCard(scan = scan)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoolCard(
    spool: UniqueSpool,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = parseColor(spool.filamentInfo.colorHex),
                        shape = CircleShape
                    )
            )
            
            // Filament info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spool.filamentInfo.colorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${spool.filamentInfo.filamentType} • ${spool.filamentInfo.trayUid}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${spool.scanCount} scans • ${(spool.successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Success rate indicator
            when {
                spool.successRate >= 0.9f -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "High success rate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                spool.successRate >= 0.7f -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Good success rate", 
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Low success rate",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagCard(
    uid: String,
    mostRecentScan: ScanHistory,
    filamentInfo: com.bscan.model.FilamentInfo?,
    allScans: List<ScanHistory>,
    modifier: Modifier = Modifier
) {
    val tagScans = allScans.filter { it.uid == uid }
    val successRate = tagScans.count { it.scanResult == ScanResult.SUCCESS }.toFloat() / tagScans.size
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tag icon with color if available
            if (filamentInfo != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = parseColor(filamentInfo.colorHex),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Tag info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uid,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                if (filamentInfo != null) {
                    Text(
                        text = "${filamentInfo.colorName} • ${filamentInfo.trayUid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Unknown filament",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = "${tagScans.size} scans • ${(successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Result indicator
            Icon(
                imageVector = when (mostRecentScan.scanResult) {
                    ScanResult.SUCCESS -> Icons.Default.CheckCircle
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (mostRecentScan.scanResult) {
                    ScanResult.SUCCESS -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScanCard(
    scan: ScanHistory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Result icon
            Icon(
                imageVector = when (scan.scanResult) {
                    ScanResult.SUCCESS -> Icons.Default.CheckCircle
                    ScanResult.AUTHENTICATION_FAILED -> Icons.Default.Lock
                    ScanResult.INSUFFICIENT_DATA -> Icons.Default.Warning
                    ScanResult.PARSING_FAILED -> Icons.Default.Error
                    ScanResult.NO_NFC_TAG -> Icons.Default.SignalWifiOff
                    ScanResult.UNKNOWN_ERROR -> Icons.Default.Help
                },
                contentDescription = null,
                tint = when (scan.scanResult) {
                    ScanResult.SUCCESS -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(32.dp)
            )
            
            // Scan info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (scan.filamentInfo != null) {
                    Text(
                        text = scan.filamentInfo.colorName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${scan.filamentInfo.filamentType} • ${scan.uid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = scan.uid,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = scan.scanResult.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = scan.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactScanPrompt(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanStateIndicator(
                isIdle = true,
                modifier = Modifier.size(48.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Scan a Spool",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "Tap your device against a filament spool to read its information",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.NearMe,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private fun parseColor(colorHex: String): Color {
    return try {
        val hex = if (colorHex.startsWith("#")) colorHex.substring(1) else colorHex
        when (hex.length) {
            6 -> Color(android.graphics.Color.parseColor("#$hex"))
            8 -> Color(android.graphics.Color.parseColor("#$hex"))
            else -> Color.Gray
        }
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}