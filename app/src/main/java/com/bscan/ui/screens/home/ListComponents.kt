package com.bscan.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.repository.UniqueFilamentReel
import com.bscan.repository.InterpretedScan
import com.bscan.ui.screens.DetailType
import com.bscan.ui.components.list.*
import java.time.LocalDateTime

// OverscrollListWrapper has been moved to com.bscan.ui.components.list.OverscrollListWrapper

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
    val filteredGroupedAndSortedSpools = remember(filamentReels, sortProperty, sortDirection, groupByOption, filterState) {
        filamentReels
            .applyFilamentReelFilters(filterState)
            .applyFilamentReelSorting(sortProperty, sortDirection)
            .applyFilamentReelGrouping(groupByOption)
    }
    
    val emptyStateType = if (!hasData) {
        EmptyStateType.NO_INVENTORY_DATA
    } else {
        EmptyStateType.NO_FILTERED_RESULTS
    }
    
    OverscrollGroupedListComponent(
        items = filteredGroupedAndSortedSpools,
        lazyListState = lazyListState,
        groupByOption = groupByOption,
        emptyStateType = emptyStateType,
        keySelector = { filamentReel: UniqueFilamentReel -> filamentReel.uid },
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan
    ) { filamentReel ->
        FilamentReelCard(
            filamentReel = filamentReel,
            onClick = { trayUid ->
                onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, trayUid)
            }
        )
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
        uniqueSkus
            .applySkuFilters(filterState)
            .applySkuSorting(sortProperty, sortDirection)
            .applySkuGrouping(groupByOption)
    }
    
    OverscrollGroupedListComponent(
        items = filteredGroupedAndSortedSkus,
        lazyListState = lazyListState,
        groupByOption = groupByOption,
        emptyStateType = EmptyStateType.NO_FILTERED_RESULTS,
        keySelector = { sku: SkuInfo -> sku.skuKey },
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan
    ) { sku ->
        SkuCard(
            sku = sku,
            onClick = { skuKey ->
                onNavigateToDetails?.invoke(DetailType.SKU, skuKey)
            }
        )
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
    // Convert UniqueFilamentReel data to the format expected by TagCard
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
    
    val filteredGroupedAndSortedTags = remember(individualTags, sortProperty, sortDirection, groupByOption, filterState) {
        // Use the filtering/sorting utilities on the original individualTags
        individualTags
            .applyFilamentReelFilters(filterState)
            .applyFilamentReelSorting(sortProperty, sortDirection)
            .applyFilamentReelGrouping(groupByOption)
            .map { (groupKey, groupItems) ->
                groupKey to groupItems.map { filamentReel ->
                    // Convert back to the Triple format expected by the UI
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
    }
    
    OverscrollGroupedListComponent(
        items = filteredGroupedAndSortedTags,
        lazyListState = lazyListState,
        groupByOption = groupByOption,
        emptyStateType = EmptyStateType.NO_FILTERED_RESULTS,
        keySelector = { tagTriple: Triple<String, InterpretedScan, com.bscan.model.FilamentInfo?> -> tagTriple.first },
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan
    ) { (uid, mostRecentScan, filamentInfo) ->
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
        allScans
            .applyFilters(filterState)
            .applySorting(sortProperty, sortDirection)
            .applyGrouping(groupByOption)
    }
    
    OverscrollGroupedListComponent(
        items = filteredGroupedAndSortedScans,
        lazyListState = lazyListState,
        groupByOption = groupByOption,
        emptyStateType = EmptyStateType.NO_FILTERED_RESULTS,
        keySelector = { scan: InterpretedScan -> "${scan.uid}_${scan.timestamp}" },
        scanState = scanState,
        scanProgress = scanProgress,
        onSimulateScan = onSimulateScan
    ) { scan ->
        ScanCard(
            scan = scan,
            onClick = { scanHistory ->
                val scanId = "${scanHistory.timestamp.toString().replace(":", "-").replace(".", "-")}_${scanHistory.uid}"
                onNavigateToDetails?.invoke(DetailType.SCAN, scanId)
            }
        )
    }
}