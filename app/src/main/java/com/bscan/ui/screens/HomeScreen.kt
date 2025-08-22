package com.bscan.ui.screens

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

enum class ViewMode { SPOOLS, SKUS, TAGS, SCANS }
enum class SortOption { MOST_RECENT, OLDEST, NAME, SUCCESS_RATE }

data class FilterState(
    val filamentTypes: Set<String> = emptySet(),
    val minSuccessRate: Float = 0f,
    val showSuccessOnly: Boolean = false,
    val showFailuresOnly: Boolean = false,
    val dateRangeDays: Int? = null // null = all time, 7 = last week, 30 = last month, etc.
)

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
    var filterState by remember { mutableStateOf(FilterState()) }
    
    // Data state
    var spools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var allScans by remember { mutableStateOf(listOf<ScanHistory>()) }
    var availableFilamentTypes by remember { mutableStateOf(setOf<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Load data
    LaunchedEffect(Unit) {
        try {
            spools = repository.getUniqueSpools()
            allScans = repository.getAllScans()
            availableFilamentTypes = allScans
                .mapNotNull { it.filamentInfo?.filamentType }
                .toSet()
        } catch (e: Exception) {
            spools = emptyList()
            allScans = emptyList()
            availableFilamentTypes = emptySet()
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
            filterState = filterState,
            spools = spools,
            allScans = allScans,
            availableFilamentTypes = availableFilamentTypes,
            showSortMenu = showSortMenu,
            showFilterMenu = showFilterMenu,
            onViewModeChange = { viewMode = it },
            onSortOptionChange = { sortOption = it },
            onFilterStateChange = { filterState = it },
            onShowSortMenu = { showSortMenu = it },
            onShowFilterMenu = { showFilterMenu = it },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun DataBrowserScreen(
    viewMode: ViewMode,
    sortOption: SortOption,
    filterState: FilterState,
    spools: List<UniqueSpool>,
    allScans: List<ScanHistory>,
    availableFilamentTypes: Set<String>,
    showSortMenu: Boolean,
    showFilterMenu: Boolean,
    onViewModeChange: (ViewMode) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onFilterStateChange: (FilterState) -> Unit,
    onShowSortMenu: (Boolean) -> Unit,
    onShowFilterMenu: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Pager state for swipeable tabs with wrap-around
    val tabCount = ViewMode.values().size
    val virtualPageCount = tabCount * 1000 // Large number for infinite scrolling effect
    val startPage = virtualPageCount / 2 + viewMode.ordinal // Start in middle to allow wrapping both ways
    
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { virtualPageCount }
    )
    
    // Sync pager state with view mode
    LaunchedEffect(viewMode) {
        val targetPage = pagerState.currentPage - (pagerState.currentPage % tabCount) + viewMode.ordinal
        if (targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }
    
    LaunchedEffect(pagerState.currentPage) {
        val actualPage = pagerState.currentPage % tabCount
        val currentMode = ViewMode.values()[actualPage]
        if (currentMode != viewMode) {
            onViewModeChange(currentMode)
        }
    }
    
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
    
    // Store LazyListState for each page
    val lazyListStates = listOf(
        rememberLazyListState(), // SPOOLS
        rememberLazyListState(), // SKUS
        rememberLazyListState(), // TAGS  
        rememberLazyListState()  // SCANS
    )
    
    // NestedScrollConnection for scan prompt reveal/hide
    val nestedScrollConnection = remember(pagerState.currentPage) {
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
                    val currentPageIndex = pagerState.currentPage % tabCount
                    val currentListState = lazyListStates[currentPageIndex]
                    val isAtTop = currentListState.firstVisibleItemIndex == 0 && currentListState.firstVisibleItemScrollOffset == 0
                    
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
        
        // Tab row for view modes (synced with pager)
        TabRow(
            selectedTabIndex = pagerState.currentPage % tabCount,
            modifier = Modifier.fillMaxWidth()
        ) {
            ViewMode.values().forEachIndexed { index, mode ->
                Tab(
                    selected = (pagerState.currentPage % tabCount) == index,
                    onClick = { 
                        scope.launch {
                            val targetPage = pagerState.currentPage - (pagerState.currentPage % tabCount) + index
                            pagerState.animateScrollToPage(targetPage)
                        }
                    },
                    text = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    icon = { 
                        Icon(
                            imageVector = when (mode) {
                                ViewMode.SPOOLS -> Icons.Default.Storage
                                ViewMode.SKUS -> Icons.Default.Inventory
                                ViewMode.TAGS -> Icons.Default.Tag
                                ViewMode.SCANS -> Icons.Default.History
                            },
                            contentDescription = null
                        )
                    }
                )
            }
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
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Filter button
            OutlinedButton(
                onClick = { onShowFilterMenu(true) }
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Filter")
                // Show badge if filters are active
                if (filterState.filamentTypes.isNotEmpty() || 
                    filterState.minSuccessRate > 0f || 
                    filterState.showSuccessOnly || 
                    filterState.showFailuresOnly || 
                    filterState.dateRangeDays != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge(
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
            }
        }
        
        // Active filter chips
        if (filterState.filamentTypes.isNotEmpty() || 
            filterState.minSuccessRate > 0f || 
            filterState.showSuccessOnly || 
            filterState.showFailuresOnly || 
            filterState.dateRangeDays != null) {
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterState.filamentTypes.toList()) { type ->
                    InputChip(
                        onClick = { 
                            val newTypes = filterState.filamentTypes - type
                            onFilterStateChange(filterState.copy(filamentTypes = newTypes))
                        },
                        label = { Text(type) },
                        selected = false,
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                
                if (filterState.minSuccessRate > 0f) {
                    item {
                        InputChip(
                            onClick = { 
                                onFilterStateChange(filterState.copy(minSuccessRate = 0f))
                            },
                            label = { Text("Success ≥ ${(filterState.minSuccessRate * 100).toInt()}%") },
                            selected = false,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                
                if (filterState.showSuccessOnly) {
                    item {
                        InputChip(
                            onClick = { 
                                onFilterStateChange(filterState.copy(showSuccessOnly = false))
                            },
                            label = { Text("Success Only") },
                            selected = false,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                
                if (filterState.showFailuresOnly) {
                    item {
                        InputChip(
                            onClick = { 
                                onFilterStateChange(filterState.copy(showFailuresOnly = false))
                            },
                            label = { Text("Failures Only") },
                            selected = false,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                
                filterState.dateRangeDays?.let { days ->
                    item {
                        val label = when (days) {
                            1 -> "Last Day"
                            7 -> "Last Week"
                            30 -> "Last Month"
                            90 -> "Last 3 Months"
                            else -> "Last $days days"
                        }
                        
                        InputChip(
                            onClick = { 
                                onFilterStateChange(filterState.copy(dateRangeDays = null))
                            },
                            label = { Text(label) },
                            selected = false,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove filter",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
        
        // Swipeable content pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val actualPage = page % tabCount
            when (ViewMode.values()[actualPage]) {
                ViewMode.SPOOLS -> SpoolsList(spools, sortOption, filterState, lazyListStates[actualPage])
                ViewMode.SKUS -> SkusList(allScans, sortOption, filterState, lazyListStates[actualPage])
                ViewMode.TAGS -> TagsList(allScans, sortOption, filterState, lazyListStates[actualPage])
                ViewMode.SCANS -> ScansList(allScans, sortOption, filterState, lazyListStates[actualPage])
            }
        }
    }
    
    // Filter dialog
    if (showFilterMenu) {
        FilterDialog(
            filterState = filterState,
            availableFilamentTypes = availableFilamentTypes,
            onFilterStateChange = onFilterStateChange,
            onDismiss = { onShowFilterMenu(false) }
        )
    }
}

@Composable
private fun SpoolsList(
    spools: List<UniqueSpool>,
    sortOption: SortOption,
    filterState: FilterState,
    lazyListState: LazyListState
) {
    val filteredAndSortedSpools = remember(spools, sortOption, filterState) {
        val filtered = spools.filter { spool ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filterState.filamentTypes.contains(spool.filamentInfo.filamentType)
            }
            
            // Filter by success rate
            val matchesSuccessRate = spool.successRate >= filterState.minSuccessRate
            
            // Filter by success/failure only
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> spool.successRate == 1.0f
                filterState.showFailuresOnly -> spool.successRate < 1.0f
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = java.time.LocalDateTime.now().minusDays(days.toLong())
                spool.lastScanned.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        when (sortOption) {
            SortOption.MOST_RECENT -> filtered.sortedByDescending { it.lastScanned }
            SortOption.OLDEST -> filtered.sortedBy { it.lastScanned }
            SortOption.NAME -> filtered.sortedBy { it.filamentInfo.colorName }
            SortOption.SUCCESS_RATE -> filtered.sortedByDescending { it.successRate }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredAndSortedSpools) { spool ->
            SpoolCard(spool = spool)
        }
    }
}

@Composable
private fun SkusList(
    allScans: List<ScanHistory>,
    sortOption: SortOption,
    filterState: FilterState,
    lazyListState: LazyListState
) {
    // Group scans by SKU (filament type + color combination)
    val uniqueSkus = remember(allScans) {
        allScans
            .filter { it.scanResult == ScanResult.SUCCESS && it.filamentInfo != null }
            .groupBy { "${it.filamentInfo!!.filamentType}-${it.filamentInfo.colorName}" }
            .mapNotNull { (skuKey, scans) ->
                val mostRecentScan = scans.maxByOrNull { it.timestamp }
                val filamentInfo = mostRecentScan?.filamentInfo
                if (filamentInfo != null) {
                    val uniqueSpools = scans.groupBy { it.filamentInfo!!.trayUid }.size
                    val totalScans = scans.size
                    val successfulScans = scans.count { it.scanResult == ScanResult.SUCCESS }
                    val lastScanned = scans.maxByOrNull { it.timestamp }?.timestamp
                    
                    SkuInfo(
                        skuKey = skuKey,
                        filamentInfo = filamentInfo,
                        spoolCount = uniqueSpools,
                        totalScans = totalScans,
                        successfulScans = successfulScans,
                        lastScanned = lastScanned ?: java.time.LocalDateTime.now(),
                        successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
                    )
                } else null
            }
    }
    
    val filteredAndSortedSkus = remember(uniqueSkus, sortOption, filterState) {
        val filtered = uniqueSkus.filter { sku ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filterState.filamentTypes.contains(sku.filamentInfo.filamentType)
            }
            
            // Filter by success rate
            val matchesSuccessRate = sku.successRate >= filterState.minSuccessRate
            
            // Filter by success/failure only
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> sku.successRate == 1.0f
                filterState.showFailuresOnly -> sku.successRate < 1.0f
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = java.time.LocalDateTime.now().minusDays(days.toLong())
                sku.lastScanned.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        when (sortOption) {
            SortOption.MOST_RECENT -> filtered.sortedByDescending { it.lastScanned }
            SortOption.OLDEST -> filtered.sortedBy { it.lastScanned }
            SortOption.NAME -> filtered.sortedBy { it.filamentInfo.colorName }
            SortOption.SUCCESS_RATE -> filtered.sortedByDescending { it.successRate }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredAndSortedSkus) { sku ->
            SkuCard(sku = sku)
        }
    }
}

@Composable
private fun TagsList(
    allScans: List<ScanHistory>,
    sortOption: SortOption,
    filterState: FilterState,
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
    
    val filteredAndSortedTags = remember(uniqueTags, sortOption, filterState, allScans) {
        val tagSuccessRates = allScans.groupBy { it.uid }.mapValues { (_, scans) ->
            scans.count { it.scanResult == ScanResult.SUCCESS }.toFloat() / scans.size
        }
        
        val filtered = uniqueTags.filter { (uid, mostRecentScan, filamentInfo) ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filamentInfo?.filamentType?.let { filterState.filamentTypes.contains(it) } ?: false
            }
            
            // Filter by success rate
            val successRate = tagSuccessRates[uid] ?: 0f
            val matchesSuccessRate = successRate >= filterState.minSuccessRate
            
            // Filter by success/failure only
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> successRate == 1.0f
                filterState.showFailuresOnly -> successRate < 1.0f
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = java.time.LocalDateTime.now().minusDays(days.toLong())
                mostRecentScan.timestamp.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        when (sortOption) {
            SortOption.MOST_RECENT -> filtered.sortedByDescending { it.second.timestamp }
            SortOption.OLDEST -> filtered.sortedBy { it.second.timestamp }
            SortOption.NAME -> filtered.sortedBy { it.third?.colorName ?: it.first }
            SortOption.SUCCESS_RATE -> filtered.sortedByDescending { tagSuccessRates[it.first] ?: 0f }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredAndSortedTags) { (uid, mostRecentScan, filamentInfo) ->
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
    filterState: FilterState,
    lazyListState: LazyListState
) {
    val filteredAndSortedScans = remember(allScans, sortOption, filterState) {
        val filtered = allScans.filter { scan ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                scan.filamentInfo?.filamentType?.let { filterState.filamentTypes.contains(it) } ?: false
            }
            
            // Filter by success rate (not directly applicable to individual scans, but we can filter by success/failure)
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> scan.scanResult == ScanResult.SUCCESS
                filterState.showFailuresOnly -> scan.scanResult != ScanResult.SUCCESS
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = java.time.LocalDateTime.now().minusDays(days.toLong())
                scan.timestamp.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesResultFilter && matchesDateRange
        }
        
        when (sortOption) {
            SortOption.MOST_RECENT -> filtered.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> filtered.sortedBy { it.timestamp }
            SortOption.NAME -> filtered.sortedBy { it.filamentInfo?.colorName ?: it.uid }
            SortOption.SUCCESS_RATE -> filtered.sortedBy { it.scanResult != ScanResult.SUCCESS }
        }
    }
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filteredAndSortedScans) { scan ->
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

data class SkuInfo(
    val skuKey: String,
    val filamentInfo: com.bscan.model.FilamentInfo,
    val spoolCount: Int,
    val totalScans: Int,
    val successfulScans: Int,
    val lastScanned: java.time.LocalDateTime,
    val successRate: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkuCard(
    sku: SkuInfo,
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
                        color = parseColor(sku.filamentInfo.colorHex),
                        shape = CircleShape
                    )
            )
            
            // SKU info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sku.filamentInfo.colorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = sku.filamentInfo.filamentType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${sku.spoolCount} spool${if (sku.spoolCount != 1) "s" else ""} • ${sku.totalScans} scans • ${(sku.successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Inventory indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    sku.successRate >= 0.9f -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "High success rate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    sku.successRate >= 0.7f -> {
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
                
                Text(
                    text = "${sku.spoolCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDialog(
    filterState: FilterState,
    availableFilamentTypes: Set<String>,
    onFilterStateChange: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter Options") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filament types filter
                if (availableFilamentTypes.isNotEmpty()) {
                    Text(
                        text = "Filament Types",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    availableFilamentTypes.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = filterState.filamentTypes.contains(type),
                                onCheckedChange = { checked ->
                                    val newTypes = if (checked) {
                                        filterState.filamentTypes + type
                                    } else {
                                        filterState.filamentTypes - type
                                    }
                                    onFilterStateChange(filterState.copy(filamentTypes = newTypes))
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = type)
                        }
                    }
                    
                    HorizontalDivider()
                }
                
                // Success rate filter
                Text(
                    text = "Minimum Success Rate: ${(filterState.minSuccessRate * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Slider(
                    value = filterState.minSuccessRate,
                    onValueChange = { onFilterStateChange(filterState.copy(minSuccessRate = it)) },
                    valueRange = 0f..1f,
                    steps = 9 // 0%, 10%, 20%, ..., 100%
                )
                
                HorizontalDivider()
                
                // Result type filters
                Text(
                    text = "Result Types",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterState.showSuccessOnly,
                        onCheckedChange = { 
                            onFilterStateChange(
                                filterState.copy(
                                    showSuccessOnly = it,
                                    showFailuresOnly = if (it) false else filterState.showFailuresOnly
                                )
                            ) 
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Success Only")
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = filterState.showFailuresOnly,
                        onCheckedChange = { 
                            onFilterStateChange(
                                filterState.copy(
                                    showFailuresOnly = it,
                                    showSuccessOnly = if (it) false else filterState.showSuccessOnly
                                )
                            ) 
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Failures Only")
                }
                
                HorizontalDivider()
                
                // Date range filter
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                val dateRangeOptions = listOf(
                    null to "All Time",
                    1 to "Last Day",
                    7 to "Last Week", 
                    30 to "Last Month",
                    90 to "Last 3 Months"
                )
                
                dateRangeOptions.forEach { (days, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = filterState.dateRangeDays == days,
                            onClick = { onFilterStateChange(filterState.copy(dateRangeDays = days)) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onFilterStateChange(FilterState()) // Reset filters
                    onDismiss()
                }
            ) {
                Text("Clear All")
            }
        }
    )
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