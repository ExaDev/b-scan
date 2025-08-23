package com.bscan.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import com.bscan.repository.UniqueSpool
import java.time.LocalDateTime

@Composable
fun SpoolsList(
    spools: List<UniqueSpool>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState
) {
    val filteredGroupedAndSortedSpools = remember(spools, sortProperty, sortDirection, groupByOption, filterState) {
        val filtered = spools.filter { spool ->
            // Filter by filament types
            val matchesFilamentType = if (filterState.filamentTypes.isEmpty()) {
                true
            } else {
                filterState.filamentTypes.contains(spool.filamentInfo.filamentType)
            }
            
            // Filter by colors
            val matchesColor = if (filterState.colors.isEmpty()) {
                true
            } else {
                filterState.colors.contains(spool.filamentInfo.colorName)
            }
            
            // Filter by base materials
            val matchesBaseMaterial = if (filterState.baseMaterials.isEmpty()) {
                true
            } else {
                val baseMaterial = spool.filamentInfo.filamentType.split(" ").firstOrNull() ?: ""
                filterState.baseMaterials.contains(baseMaterial)
            }
            
            // Filter by material series
            val matchesMaterialSeries = if (filterState.materialSeries.isEmpty()) {
                true
            } else {
                val parts = spool.filamentInfo.filamentType.split(" ")
                val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
                filterState.materialSeries.contains(series)
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
                val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                spool.lastScanned.isAfter(cutoffDate)
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
        
        // Group the sorted spools if grouping is enabled
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
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredGroupedAndSortedSpools.forEach { (groupKey, groupSpools) ->
            // Show group header if grouping is enabled
            if (groupByOption != GroupByOption.NONE) {
                item(key = "header_$groupKey") {
                    GroupHeader(title = groupKey, itemCount = groupSpools.size)
                }
            }
            
            // Show spools in the group
            items(groupSpools, key = { it.uid }) { spool ->
                SpoolCard(spool = spool)
            }
        }
    }
}

@Composable
fun SkusList(
    allScans: List<ScanHistory>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
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
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                SkuCard(sku = sku)
            }
        }
    }
}

@Composable
fun TagsList(
    allScans: List<ScanHistory>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
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
    
    val filteredGroupedAndSortedTags = remember(uniqueTags, sortProperty, sortDirection, groupByOption, filterState, allScans) {
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
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                    allScans = allScans
                )
            }
        }
    }
}

@Composable
fun ScansList(
    allScans: List<ScanHistory>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState
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
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                ScanCard(scan = scan)
            }
        }
    }
}