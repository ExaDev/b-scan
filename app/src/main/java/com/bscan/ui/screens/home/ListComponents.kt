package com.bscan.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import android.util.Log
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.math.abs
import com.bscan.ScanState
import com.bscan.model.ScanDebugInfo
import com.bscan.model.ScanProgress
import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.repository.UniqueFilamentReel
import com.bscan.repository.InterpretedScan
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.ScanPromptScreen
import java.time.LocalDateTime

@Composable
fun OverscrollListWrapper(
    lazyListState: LazyListState,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp,
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: PaddingValues) -> Unit
) {
    // Overscroll-based scan prompt logic
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val singleRowHeight = compactPromptHeightDp
    val fullPageHeight = maxOf(200.dp, minOf(600.dp, screenHeightDp - 200.dp))
    
    // Track scan prompt state with discrete levels
    var promptState by remember { mutableStateOf(0) } // 0 = hidden, 1 = single row, 2 = full height
    var isUserDragging by remember { mutableStateOf(false) }
    var overscrollAccumulated by remember { mutableStateOf(0f) }
    var lastTransitionTime by remember { mutableStateOf(0L) } // Time of last state transition
    
    // Calculate scan prompt height based on discrete state
    val scanPromptHeight = when (promptState) {
        1 -> singleRowHeight // Single row
        2 -> fullPageHeight  // Full height
        else -> 0.dp         // Hidden
    }
    
    val animatedHeight by animateDpAsState(
        targetValue = scanPromptHeight,
        animationSpec = spring(),
        label = "scan_prompt_height"
    )
    
    // Overscroll connection for discrete gesture detection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                
                // Track if user is actively dragging (not momentum)
                val wasDragging = isUserDragging
                isUserDragging = source == NestedScrollSource.UserInput
                
                // Only handle deliberate drag gestures at the top
                if (isAtTop && available.y > 0 && isUserDragging) {
                    overscrollAccumulated += available.y
                    
                    // Discrete state transitions based on accumulated drag with time delay
                    val threshold = singleRowHeight.value * 0.3f // Threshold for state change
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastTransition = currentTime - lastTransitionTime
                    val minimumDelay = 300L // 300ms minimum between transitions
                    
                    when (promptState) {
                        0 -> { // Hidden -> Single row
                            if (overscrollAccumulated > threshold) {
                                promptState = 1
                                overscrollAccumulated = 0f // Reset for next transition
                                lastTransitionTime = currentTime
                                Log.d("ScanPrompt", "Transition 0->1 (single row)")
                            }
                        }
                        1 -> { // Single row -> Full height  
                            if (overscrollAccumulated > threshold && timeSinceLastTransition > minimumDelay) {
                                promptState = 2
                                overscrollAccumulated = 0f
                                lastTransitionTime = currentTime
                                Log.d("ScanPrompt", "Transition 1->2 (full height)")
                            }
                        }
                        // State 2 (full height) stays at 2
                    }
                    
                    return Offset(0f, available.y) // Consume the drag
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                
                // Reset states when actively scrolling down or significantly away from top
                // Use moderate tolerance to account for scan prompt height but still allow collapsing
                val tolerancePixels = 200 // Reduced tolerance to make collapsing more responsive
                if ((available.y < 0 && source == NestedScrollSource.UserInput) || 
                    (!isAtTop && lazyListState.firstVisibleItemScrollOffset > tolerancePixels)) {
                    promptState = 0
                    overscrollAccumulated = 0f
                    isUserDragging = false
                    lastTransitionTime = 0L // Reset transition timer
                }
                
                // Reset dragging flag when momentum stops
                if (source != NestedScrollSource.UserInput) {
                    isUserDragging = false
                    overscrollAccumulated = 0f
                }
                
                return Offset.Zero
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Content with padding adjusted for scan prompt
        content(
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp + animatedHeight, // Push content down by scan prompt height
                bottom = 8.dp
            )
        )
        
        // Overlay scan prompt that appears on overscroll
        if (animatedHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(animatedHeight)
                    .padding(horizontal = 16.dp)
            ) {
                when (promptState) {
                    2 -> {
                        // Show full scan prompt
                        ScanPromptScreen()
                    }
                    1 -> {
                        // Show compact scan prompt
                        CompactScanPrompt(
                            scanState = scanState,
                            scanProgress = scanProgress,
                            onLongPress = onSimulateScan
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilamentReelsList(
    filamentReels: List<UniqueFilamentReel>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp,
    hasData: Boolean = false
) {
    // Show full height scan prompt when no inventory data exists
    if (!hasData) {
        ScanPromptScreen()
        return
    }
    val filteredGroupedAndSortedSpools = remember(filamentReels, sortProperty, sortDirection, groupByOption, filterState) {
        val filtered = filamentReels.filter { filamentReel ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filterState.filamentTypes.contains(filamentReel.filamentInfo.filamentType)
            }
            
            // Filter by colors
            val matchesColor = if (filterState.colors.isEmpty()) {
                true
            } else {
                filterState.colors.contains(filamentReel.filamentInfo.colorName)
            }
            
            // Filter by base materials
            val matchesBaseMaterial = if (filterState.baseMaterials.isEmpty()) {
                true
            } else {
                val baseMaterial = filamentReel.filamentInfo.filamentType.split(" ").firstOrNull() ?: ""
                filterState.baseMaterials.contains(baseMaterial)
            }
            
            // Filter by material series
            val matchesMaterialSeries = if (filterState.materialSeries.isEmpty()) {
                true
            } else {
                val parts = filamentReel.filamentInfo.filamentType.split(" ")
                val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
                filterState.materialSeries.contains(series)
            }
            
            // Filter by success rate
            val matchesSuccessRate = filamentReel.successRate >= filterState.minSuccessRate
            
            // Filter by success/failure only
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> filamentReel.successRate == 1.0f
                filterState.showFailuresOnly -> filamentReel.successRate < 1.0f
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                filamentReel.lastScanned.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesColor && matchesBaseMaterial && matchesMaterialSeries && 
            matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        val sorted = when (sortProperty) {
            SortProperty.FIRST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // For now, use lastScanned - we'll need to calculate first scan from scan history
                filtered.sortedBy { it.lastScanned }
            } else {
                filtered.sortedByDescending { it.lastScanned }
            }
            SortProperty.LAST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.lastScanned }
            } else {
                filtered.sortedByDescending { it.lastScanned }
            }
            SortProperty.NAME -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.colorName }
            } else {
                filtered.sortedByDescending { it.filamentInfo.colorName }
            }
            SortProperty.SUCCESS_RATE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.successRate }
            } else {
                filtered.sortedByDescending { it.successRate }
            }
            SortProperty.COLOR -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.colorName }
            } else {
                filtered.sortedByDescending { it.filamentInfo.colorName }
            }
            SortProperty.MATERIAL_TYPE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.filamentType }
            } else {
                filtered.sortedByDescending { it.filamentInfo.filamentType }
            }
        }
        
        // Group the sorted filamentReels if grouping is enabled
        when (groupByOption) {
            GroupByOption.NONE -> sorted.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> sorted.groupBy { it.filamentInfo.colorName }.toList()
            GroupByOption.BASE_MATERIAL -> sorted.groupBy { 
                it.filamentInfo.filamentType.split(" ").firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> sorted.groupBy { 
                val parts = it.filamentInfo.filamentType.split(" ")
                if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredGroupedAndSortedSpools.forEach { (groupKey, groupSpools) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupSpools.size)
                    }
                }
                
                // Show filamentReels in the group
                items(groupSpools, key = { it.uid }) { filamentReel ->
                    FilamentReelCard(
                        filamentReel = filamentReel,
                        onClick = { trayUid ->
                            onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, trayUid)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SkusList(
    allScans: List<InterpretedScan>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp
) {
    // Group scans by SKU (filament type + color combination)
    // Include ALL scans with filament info to show incomplete/failed scans as separate SKUs
    val uniqueSkus = remember(allScans) {
        allScans
            .filter { it.filamentInfo != null }
            .groupBy { "${it.filamentInfo!!.filamentType}-${it.filamentInfo.colorName}" }
            .mapNotNull { (skuKey, scans) ->
                val mostRecentScan = scans.maxByOrNull { it.timestamp }
                val filamentInfo = mostRecentScan?.filamentInfo
                if (filamentInfo != null) {
                    val uniqueFilamentReels = scans.groupBy { it.filamentInfo!!.trayUid }.size
                    val totalScans = scans.size
                    val successfulScans = scans.count { it.scanResult == ScanResult.SUCCESS }
                    val lastScanned = scans.maxByOrNull { it.timestamp }?.timestamp
                    
                    SkuInfo(
                        skuKey = skuKey,
                        filamentInfo = filamentInfo,
                        filamentReelCount = uniqueFilamentReels,
                        totalScans = totalScans,
                        successfulScans = successfulScans,
                        lastScanned = lastScanned ?: LocalDateTime.now(),
                        successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
                    )
                } else null
            }
    }
    
    val filteredGroupedAndSortedSkus = remember(uniqueSkus, sortProperty, sortDirection, groupByOption, filterState) {
        val filtered = uniqueSkus.filter { sku ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filterState.filamentTypes.contains(sku.filamentInfo.filamentType)
            }
            
            // Filter by colors
            val matchesColor = if (filterState.colors.isEmpty()) {
                true
            } else {
                filterState.colors.contains(sku.filamentInfo.colorName)
            }
            
            // Filter by base materials
            val matchesBaseMaterial = if (filterState.baseMaterials.isEmpty()) {
                true
            } else {
                val baseMaterial = sku.filamentInfo.filamentType.split(" ").firstOrNull() ?: ""
                filterState.baseMaterials.contains(baseMaterial)
            }
            
            // Filter by material series
            val matchesMaterialSeries = if (filterState.materialSeries.isEmpty()) {
                true
            } else {
                val parts = sku.filamentInfo.filamentType.split(" ")
                val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
                filterState.materialSeries.contains(series)
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
                val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                sku.lastScanned.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesColor && matchesBaseMaterial && matchesMaterialSeries && 
            matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        val sorted = when (sortProperty) {
            SortProperty.FIRST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // For now, use lastScanned - we'll need to calculate first scan from scan history
                filtered.sortedBy { it.lastScanned }
            } else {
                filtered.sortedByDescending { it.lastScanned }
            }
            SortProperty.LAST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.lastScanned }
            } else {
                filtered.sortedByDescending { it.lastScanned }
            }
            SortProperty.NAME -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.colorName }
            } else {
                filtered.sortedByDescending { it.filamentInfo.colorName }
            }
            SortProperty.SUCCESS_RATE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.successRate }
            } else {
                filtered.sortedByDescending { it.successRate }
            }
            SortProperty.COLOR -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.colorName }
            } else {
                filtered.sortedByDescending { it.filamentInfo.colorName }
            }
            SortProperty.MATERIAL_TYPE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo.filamentType }
            } else {
                filtered.sortedByDescending { it.filamentInfo.filamentType }
            }
        }
        
        // Group the sorted SKUs if grouping is enabled
        when (groupByOption) {
            GroupByOption.NONE -> sorted.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> sorted.groupBy { it.filamentInfo.colorName }.toList()
            GroupByOption.BASE_MATERIAL -> sorted.groupBy { 
                it.filamentInfo.filamentType.split(" ").firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> sorted.groupBy { 
                val parts = it.filamentInfo.filamentType.split(" ")
                if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredGroupedAndSortedSkus.forEach { (groupKey, groupSkus) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupSkus.size)
                    }
                }
                
                // Show SKUs in the group
                items(groupSkus, key = { it.skuKey }) { sku ->
                    SkuCard(
                        sku = sku,
                        onClick = { skuKey ->
                            onNavigateToDetails?.invoke(DetailType.SKU, skuKey)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TagsList(
    individualTags: List<UniqueFilamentReel>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp
) {
    // Convert UniqueFilamentReel data to the format expected by the rest of the function
    val uniqueTags = remember(individualTags) {
        individualTags.map { filamentReel ->
            // Create a dummy InterpretedScan with the spool's information for display
            val dummyEncrypted = EncryptedScanData(
                timestamp = filamentReel.lastScanned,
                tagUid = filamentReel.uid,
                technology = "NFC",
                encryptedData = ByteArray(0)
            )
            val dummyDecrypted = DecryptedScanData(
                timestamp = filamentReel.lastScanned,
                tagUid = filamentReel.uid,
                technology = "NFC",
                scanResult = ScanResult.SUCCESS,
                decryptedBlocks = emptyMap(),
                authenticatedSectors = emptyList(),
                failedSectors = emptyList(),
                usedKeys = emptyMap(),
                derivedKeys = emptyList(),
                errors = emptyList()
            )
            val dummyScan = InterpretedScan(
                encryptedData = dummyEncrypted,
                decryptedData = dummyDecrypted,
                filamentInfo = filamentReel.filamentInfo
            )
            Triple(filamentReel.uid, dummyScan, filamentReel.filamentInfo)
        }
    }
    
    val filteredGroupedAndSortedTags = remember(uniqueTags, sortProperty, sortDirection, groupByOption, filterState, individualTags) {
        val tagSuccessRates = individualTags.associate { filamentReel ->
            filamentReel.uid to filamentReel.successRate
        }
        
        val filtered = uniqueTags.filter { (uid, mostRecentScan, filamentInfo) ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filamentInfo?.filamentType?.let { filterState.filamentTypes.contains(it) } ?: false
            }
            
            // Filter by colors
            val matchesColor = if (filterState.colors.isEmpty()) {
                true
            } else {
                filamentInfo?.colorName?.let { filterState.colors.contains(it) } ?: false
            }
            
            // Filter by base materials
            val matchesBaseMaterial = if (filterState.baseMaterials.isEmpty()) {
                true
            } else {
                filamentInfo?.filamentType?.split(" ")?.firstOrNull()?.let { 
                    filterState.baseMaterials.contains(it) 
                } ?: false
            }
            
            // Filter by material series
            val matchesMaterialSeries = if (filterState.materialSeries.isEmpty()) {
                true
            } else {
                filamentInfo?.filamentType?.let { type ->
                    val parts = type.split(" ")
                    val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
                    filterState.materialSeries.contains(series)
                } ?: false
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
                val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                mostRecentScan.timestamp.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesColor && matchesBaseMaterial && matchesMaterialSeries && 
            matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        val sorted = when (sortProperty) {
            SortProperty.FIRST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // Use most recent scan timestamp for now - would need earliest scan from history
                filtered.sortedBy { it.second.timestamp }
            } else {
                filtered.sortedByDescending { it.second.timestamp }
            }
            SortProperty.LAST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.second.timestamp }
            } else {
                filtered.sortedByDescending { it.second.timestamp }
            }
            SortProperty.NAME -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.third?.colorName ?: it.first }
            } else {
                filtered.sortedByDescending { it.third?.colorName ?: it.first }
            }
            SortProperty.SUCCESS_RATE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { tagSuccessRates[it.first] ?: 0f }
            } else {
                filtered.sortedByDescending { tagSuccessRates[it.first] ?: 0f }
            }
            SortProperty.COLOR -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.third?.colorName ?: it.first }
            } else {
                filtered.sortedByDescending { it.third?.colorName ?: it.first }
            }
            SortProperty.MATERIAL_TYPE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.third?.filamentType ?: "" }
            } else {
                filtered.sortedByDescending { it.third?.filamentType ?: "" }
            }
        }
        
        // Group the sorted tags if grouping is enabled
        when (groupByOption) {
            GroupByOption.NONE -> sorted.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> sorted.groupBy { it.third?.colorName ?: "Unknown" }.toList()
            GroupByOption.BASE_MATERIAL -> sorted.groupBy { 
                it.third?.filamentType?.split(" ")?.firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> sorted.groupBy { 
                val parts = it.third?.filamentType?.split(" ")
                if (parts != null && parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredGroupedAndSortedTags.forEach { (groupKey, groupTags) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupTags.size)
                    }
                }
                
                // Show tags in the group
                items(groupTags, key = { it.first }) { (uid, mostRecentScan, filamentInfo) ->
                    TagCard(
                        uid = uid,
                        mostRecentScan = mostRecentScan,
                        filamentInfo = filamentInfo,
                        allScans = listOf(mostRecentScan), // Pass just this tag's scan data
                        modifier = Modifier.clickable {
                            // Navigate to tag details using the tag UID
                            onNavigateToDetails?.invoke(DetailType.TAG, uid)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ScansList(
    allScans: List<InterpretedScan>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp
) {
    val filteredGroupedAndSortedScans = remember(allScans, sortProperty, sortDirection, groupByOption, filterState) {
        val filtered = allScans.filter { scan ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                scan.filamentInfo?.filamentType?.let { filterState.filamentTypes.contains(it) } ?: false
            }
            
            // Filter by colors
            val matchesColor = if (filterState.colors.isEmpty()) {
                true
            } else {
                scan.filamentInfo?.colorName?.let { filterState.colors.contains(it) } ?: false
            }
            
            // Filter by base materials
            val matchesBaseMaterial = if (filterState.baseMaterials.isEmpty()) {
                true
            } else {
                scan.filamentInfo?.filamentType?.split(" ")?.firstOrNull()?.let { 
                    filterState.baseMaterials.contains(it) 
                } ?: false
            }
            
            // Filter by material series
            val matchesMaterialSeries = if (filterState.materialSeries.isEmpty()) {
                true
            } else {
                scan.filamentInfo?.filamentType?.let { type ->
                    val parts = type.split(" ")
                    val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
                    filterState.materialSeries.contains(series)
                } ?: false
            }
            
            // Filter by success rate (not directly applicable to individual scans, but we can filter by success/failure)
            val matchesResultFilter = when {
                filterState.showSuccessOnly -> scan.scanResult == ScanResult.SUCCESS
                filterState.showFailuresOnly -> scan.scanResult != ScanResult.SUCCESS
                else -> true
            }
            
            // Filter by date range
            val matchesDateRange = filterState.dateRangeDays?.let { days ->
                val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                scan.timestamp.isAfter(cutoffDate)
            } ?: true
            
            matchesFilamentType && matchesColor && matchesBaseMaterial && matchesMaterialSeries && 
            matchesResultFilter && matchesDateRange
        }
        
        val sorted = when (sortProperty) {
            SortProperty.FIRST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // Individual scans - FIRST_SCAN and LAST_SCAN mean the same thing
                filtered.sortedBy { it.timestamp }
            } else {
                filtered.sortedByDescending { it.timestamp }
            }
            SortProperty.LAST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.timestamp }
            } else {
                filtered.sortedByDescending { it.timestamp }
            }
            SortProperty.NAME -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo?.colorName ?: it.uid }
            } else {
                filtered.sortedByDescending { it.filamentInfo?.colorName ?: it.uid }
            }
            SortProperty.SUCCESS_RATE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.scanResult != ScanResult.SUCCESS }
            } else {
                filtered.sortedByDescending { it.scanResult != ScanResult.SUCCESS }
            }
            SortProperty.COLOR -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo?.colorName ?: it.uid }
            } else {
                filtered.sortedByDescending { it.filamentInfo?.colorName ?: it.uid }
            }
            SortProperty.MATERIAL_TYPE -> if (sortDirection == SortDirection.ASCENDING) {
                filtered.sortedBy { it.filamentInfo?.filamentType ?: "" }
            } else {
                filtered.sortedByDescending { it.filamentInfo?.filamentType ?: "" }
            }
        }
        
        // Group the sorted scans if grouping is enabled
        when (groupByOption) {
            GroupByOption.NONE -> sorted.map { "ungrouped" to listOf(it) }
            GroupByOption.COLOR -> sorted.groupBy { it.filamentInfo?.colorName ?: "Unknown" }.toList()
            GroupByOption.BASE_MATERIAL -> sorted.groupBy { 
                it.filamentInfo?.filamentType?.split(" ")?.firstOrNull() ?: "Unknown"
            }.toList()
            GroupByOption.MATERIAL_SERIES -> sorted.groupBy { 
                val parts = it.filamentInfo?.filamentType?.split(" ")
                if (parts != null && parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
            }.toList()
        }
    }
    
    OverscrollListWrapper(
        lazyListState = lazyListState,
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan,
        compactPromptHeightDp = compactPromptHeightDp,
        fullPromptHeightDp = fullPromptHeightDp
    ) { contentPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filteredGroupedAndSortedScans.forEach { (groupKey, groupScans) ->
                // Show group header if grouping is enabled
                if (groupByOption != GroupByOption.NONE) {
                    item(key = "header_$groupKey") {
                        GroupHeader(title = groupKey, itemCount = groupScans.size)
                    }
                }
                
                // Show scans in the group
                items(groupScans, key = { "${it.uid}_${it.timestamp}" }) { scan ->
                    ScanCard(
                        scan = scan,
                        onClick = { scanHistory ->
                            val scanId = "${scanHistory.timestamp.toString().replace(":", "-").replace(".", "-")}_${scanHistory.uid}"
                            onNavigateToDetails?.invoke(DetailType.SCAN, scanId)
                        }
                    )
                }
            }
        }
    }
}