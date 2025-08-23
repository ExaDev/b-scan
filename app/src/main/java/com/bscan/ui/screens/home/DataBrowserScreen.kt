package com.bscan.ui.screens.home

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.repository.UniqueSpool
import com.bscan.repository.InterpretedScan
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.ScanPromptScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DataBrowserScreen(
    viewMode: ViewMode,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    spools: List<UniqueSpool>,
    individualTags: List<UniqueSpool>,
    allScans: List<InterpretedScan>,
    availableFilamentTypes: Set<String>,
    availableColors: Set<String>,
    availableBaseMaterials: Set<String>,
    availableMaterialSeries: Set<String>,
    showSortMenu: Boolean,
    showFilterMenu: Boolean,
    scanState: ScanState,
    scanProgress: ScanProgress?,
    onSimulateScan: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onSortPropertyChange: (SortProperty) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onGroupByOptionChange: (GroupByOption) -> Unit,
    onFilterStateChange: (FilterState) -> Unit,
    onShowSortMenu: (Boolean) -> Unit,
    onShowFilterMenu: (Boolean) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    
    // Local state for GroupBy menu
    var showGroupByMenu by remember { mutableStateOf(false) }
    
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
    val configuration = LocalConfiguration.current
    val compactPromptHeightDp = 100.dp
    val fullPromptHeightDp = configuration.screenHeightDp.dp
    val compactPromptHeightPx = with(density) { compactPromptHeightDp.toPx() }
    val fullPromptHeightPx = with(density) { fullPromptHeightDp.toPx() }
    
    // Overscroll reveal state - start hidden for data view
    var overscrollOffset by remember { mutableFloatStateOf(0f) }
    var isRevealing by remember { mutableStateOf(false) }
    var isShowingFullPrompt by remember { mutableStateOf(false) }
    
    // Function to dismiss the scan prompt
    val dismissScanPrompt = {
        isRevealing = false
        isShowingFullPrompt = false
        overscrollOffset = 0f
    }
    
    // Animated offset for smooth transitions
    val animatedOffset by animateFloatAsState(
        targetValue = when {
            !isRevealing -> 0f // Hidden
            isShowingFullPrompt -> fullPromptHeightPx // Full page height
            else -> compactPromptHeightPx // Compact height
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
    
    // Auto-scroll to top and reveal scan prompt when scanning starts
    LaunchedEffect(scanState) {
        if (scanState == ScanState.TAG_DETECTED || scanState == ScanState.PROCESSING) {
            // Reveal scan prompt
            isRevealing = true
            overscrollOffset = compactPromptHeightPx
            
            // Scroll to top of current list
            val currentPageIndex = pagerState.currentPage % tabCount
            val currentListState = lazyListStates[currentPageIndex]
            currentListState.animateScrollToItem(0)
        }
    }
    
    // Pull-to-refresh style NestedScrollConnection - persistent when activated
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Handle overscroll reveal when pulling down at top
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    val currentPageIndex = pagerState.currentPage % tabCount
                    val currentListState = lazyListStates[currentPageIndex]
                    val isAtTop = currentListState.firstVisibleItemIndex == 0 && 
                                  currentListState.firstVisibleItemScrollOffset == 0
                    
                    // Handle pull reveal - true overscroll only (consumed nothing)
                    if (isAtTop && consumed.y == 0f) {
                        val newOffset = (overscrollOffset + available.y * 0.5f).coerceAtMost(fullPromptHeightPx)
                        val consumedOffset = newOffset - overscrollOffset
                        overscrollOffset = newOffset
                        
                        // Update revealing state and determine which prompt to show
                        if (overscrollOffset > 0f) {
                            isRevealing = true
                            isShowingFullPrompt = overscrollOffset > compactPromptHeightPx * 1.5f
                        }
                        
                        return Offset(0f, consumedOffset)
                    }
                }
                
                return Offset.Zero
            }
            
            override suspend fun onPreFling(available: Velocity): Velocity {
                // Pull-to-refresh style snapping: once pulled enough, it stays visible
                if (overscrollOffset > 0f) {
                    if (overscrollOffset > compactPromptHeightPx * 0.4f) {
                        // Snap to visible state and make it persistent
                        isRevealing = true
                        isShowingFullPrompt = overscrollOffset > compactPromptHeightPx * 1.2f
                        overscrollOffset = if (isShowingFullPrompt) fullPromptHeightPx else compactPromptHeightPx
                    } else {
                        // Snap back to hidden
                        isRevealing = false
                        isShowingFullPrompt = false
                        overscrollOffset = 0f
                    }
                }
                return Velocity.Zero
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("B-Scan") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "Scan History"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        modifier = modifier.systemBarsPadding()
    ) { paddingValues ->
        // Use Box to overlay scan prompt on top of main content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content column
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
        
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
                    text = { 
                        Text(
                            when (mode) {
                                ViewMode.SPOOLS -> "Spools"
                                ViewMode.SKUS -> "SKUs"
                                ViewMode.TAGS -> "Tags"
                                ViewMode.SCANS -> "Scans"
                            }
                        )
                    },
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
        
        // Compact controls row with icon-only buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Split sort button with direction on left, property on right
            Box {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.height(40.dp)
                ) {
                    // Left section - Sort direction (clickable)
                    Box(
                        modifier = Modifier
                            .clickable { onSortDirectionToggle() }
                            .padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = if (sortDirection == SortDirection.ASCENDING) 
                                "Sort ascending" 
                            else 
                                "Sort descending",
                            modifier = Modifier
                                .size(18.dp)
                                .then(
                                    if (sortDirection == SortDirection.ASCENDING)
                                        Modifier.scale(scaleY = -1f, scaleX = 1f)
                                    else
                                        Modifier
                                )
                        )
                    }
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    
                    // Right section - Sort property (clickable)
                    Box(
                        modifier = Modifier
                            .clickable { onShowSortMenu(true) }
                            .padding(start = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (sortProperty) {
                                    SortProperty.FIRST_SCAN -> "First Scan"
                                    SortProperty.LAST_SCAN -> "Last Scan"
                                    SortProperty.NAME -> "Name"
                                    SortProperty.SUCCESS_RATE -> "Success"
                                    SortProperty.COLOR -> "Color"
                                    SortProperty.MATERIAL_TYPE -> "Material"
                                }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Sort options",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onShowSortMenu(false) }
                ) {
                    SortProperty.values().forEach { property ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when (property) {
                                        SortProperty.FIRST_SCAN -> "First Scan"
                                        SortProperty.LAST_SCAN -> "Last Scan"
                                        SortProperty.NAME -> "Name"
                                        SortProperty.SUCCESS_RATE -> "Success Rate"
                                        SortProperty.COLOR -> "Color"
                                        SortProperty.MATERIAL_TYPE -> "Material Type"
                                    }
                                )
                            },
                            onClick = {
                                onSortPropertyChange(property)
                                onShowSortMenu(false)
                            }
                        )
                    }
                }
            }
            
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
                    filterState.colors.isNotEmpty() ||
                    filterState.baseMaterials.isNotEmpty() ||
                    filterState.materialSeries.isNotEmpty() ||
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
            
            // Group button with dropdown
            Box {
                OutlinedButton(
                    onClick = { showGroupByMenu = true }
                ) {
                    Icon(
                        Icons.Default.GroupWork,
                        contentDescription = "Group",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Group")
                }
                
                DropdownMenu(
                    expanded = showGroupByMenu,
                    onDismissRequest = { showGroupByMenu = false }
                ) {
                    GroupByOption.values().forEach { option ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when (option) {
                                        GroupByOption.NONE -> "None"
                                        GroupByOption.COLOR -> "Color"
                                        GroupByOption.BASE_MATERIAL -> "Material"
                                        GroupByOption.MATERIAL_SERIES -> "Series"
                                    }
                                )
                            },
                            onClick = {
                                onGroupByOptionChange(option)
                                showGroupByMenu = false
                            }
                        )
                    }
                }
            }
        }
        
        
        // Filter chips
        FilterChips(
            filterState = filterState,
            onFilterStateChange = onFilterStateChange
        )
        
                // Swipeable content pager with nested scroll connection
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) { page ->
                    val actualPage = page % tabCount
                    when (ViewMode.values()[actualPage]) {
                        ViewMode.SPOOLS -> SpoolsList(spools, sortProperty, sortDirection, groupByOption, filterState, lazyListStates[actualPage], onNavigateToDetails)
                        ViewMode.SKUS -> SkusList(allScans, sortProperty, sortDirection, groupByOption, filterState, lazyListStates[actualPage], onNavigateToDetails)
                        ViewMode.TAGS -> TagsList(individualTags, sortProperty, sortDirection, groupByOption, filterState, lazyListStates[actualPage], onNavigateToDetails)
                        ViewMode.SCANS -> ScansList(allScans, sortProperty, sortDirection, groupByOption, filterState, lazyListStates[actualPage], onNavigateToDetails)
                    }
                }
            }
            
            // Scan prompt overlay - starts hidden above screen and slides down
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        // Start above screen and slide down
                        val maxHeight = if (isShowingFullPrompt) fullPromptHeightPx else compactPromptHeightPx
                        translationY = -maxHeight + animatedOffset
                        alpha = if (isRevealing && animatedOffset > 0f) 1f else 0f
                    }
            ) {
                if (animatedOffset > 0) {
                    if (isShowingFullPrompt) {
                        // Full scan prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(with(LocalDensity.current) { fullPromptHeightDp })
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            ScanPromptScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                            
                            // Dismiss button for full prompt
                            IconButton(
                                onClick = dismissScanPrompt,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        // Compact scan prompt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(compactPromptHeightDp)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable { dismissScanPrompt() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            CompactScanPrompt(
                                scanState = scanState,
                                scanProgress = scanProgress,
                                onLongPress = onSimulateScan
                            )
                            
                            // Dismiss button
                            IconButton(
                                onClick = dismissScanPrompt,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Filter dialog
    if (showFilterMenu) {
        FilterDialog(
            filterState = filterState,
            availableFilamentTypes = availableFilamentTypes,
            availableColors = availableColors,
            availableBaseMaterials = availableBaseMaterials,
            availableMaterialSeries = availableMaterialSeries,
            onFilterStateChange = onFilterStateChange,
            onDismiss = { onShowFilterMenu(false) }
        )
    }
}