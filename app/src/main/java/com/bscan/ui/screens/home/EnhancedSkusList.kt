package com.bscan.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bscan.repository.InterpretedScan
import com.bscan.repository.SkuRepository
import com.bscan.ui.screens.DetailType
import java.time.LocalDateTime

@Composable
fun EnhancedSkusList(
    allScans: List<InterpretedScan>,
    sortProperty: SortProperty,
    sortDirection: SortDirection,
    groupByOption: GroupByOption,
    filterState: FilterState,
    lazyListState: LazyListState,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val skuRepository = remember { SkuRepository(context) }
    
    // Get all SKUs from repository (includes both owned and virtual products)
    val allSkus = remember(allScans) {
        skuRepository.getAllSkus()
    }
    
    val filteredGroupedAndSortedSkus = remember(allSkus, sortProperty, sortDirection, groupByOption, filterState) {
        val filtered = allSkus.filter { sku ->
            // Filter by minimum spool count (0 = show all products, 1+ = only owned)
            val matchesSpoolCount = sku.spoolCount >= filterState.minSpoolCount
            
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
            
            // Filter by date range (skip for unscanned products with spoolCount = 0)
            val matchesDateRange = if (sku.spoolCount == 0) {
                true // Unscanned products always match date range
            } else {
                filterState.dateRangeDays?.let { days ->
                    val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
                    sku.lastScanned.isAfter(cutoffDate)
                } ?: true
            }
            
            matchesSpoolCount && matchesFilamentType && matchesColor && matchesBaseMaterial && 
            matchesMaterialSeries && matchesSuccessRate && matchesResultFilter && matchesDateRange
        }
        
        val sorted = when (sortProperty) {
            SortProperty.FIRST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // Sort by first scan date; unscanned products (epoch time) come first
                filtered.sortedBy { it.lastScanned }
            } else {
                // Sort by first scan date desc; scanned products come first
                filtered.sortedByDescending { it.lastScanned }
            }
            SortProperty.LAST_SCAN -> if (sortDirection == SortDirection.ASCENDING) {
                // Sort by last scan date; unscanned products (epoch time) come first
                filtered.sortedBy { it.lastScanned }
            } else {
                // Sort by last scan date desc; recently scanned products come first
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