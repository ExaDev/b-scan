package com.bscan.ui.components.list

import com.bscan.repository.UniqueFilamentReel
import com.bscan.repository.InterpretedScan
import com.bscan.ui.screens.home.*
import java.time.LocalDateTime

/**
 * Utility functions for filtering and sorting list data consistently across different list types.
 * Provides reusable filtering and sorting logic for inventory items, SKUs, tags, and scans.
 */

// Filtering functions

/**
 * Filters UniqueFilamentReel items based on the provided FilterState.
 */
fun List<UniqueFilamentReel>.applyFilamentReelFilters(filterState: FilterState): List<UniqueFilamentReel> {
    return filter { filamentReel ->
        matchesFilamentTypeFilter(filamentReel.filamentInfo.filamentType, filterState) &&
        matchesColorFilter(filamentReel.filamentInfo.colorName, filterState) &&
        matchesBaseMaterialFilter(filamentReel.filamentInfo.filamentType, filterState) &&
        matchesMaterialSeriesFilter(filamentReel.filamentInfo.filamentType, filterState) &&
        matchesSuccessRateFilter(filamentReel.successRate, filterState) &&
        matchesResultFilter(filamentReel.successRate, filterState) &&
        matchesDateRangeFilter(filamentReel.lastScanned, filterState)
    }
}

/**
 * Filters SkuInfo items based on the provided FilterState.
 */
fun List<SkuInfo>.applySkuFilters(filterState: FilterState): List<SkuInfo> {
    return filter { sku ->
        matchesFilamentTypeFilter(sku.filamentInfo.filamentType, filterState) &&
        matchesColorFilter(sku.filamentInfo.colorName, filterState) &&
        matchesBaseMaterialFilter(sku.filamentInfo.filamentType, filterState) &&
        matchesMaterialSeriesFilter(sku.filamentInfo.filamentType, filterState) &&
        matchesSuccessRateFilter(sku.successRate, filterState) &&
        matchesResultFilter(sku.successRate, filterState) &&
        matchesDateRangeFilter(sku.lastScanned, filterState)
    }
}

/**
 * Filters InterpretedScan items based on the provided FilterState.
 */
fun List<InterpretedScan>.applyFilters(filterState: FilterState): List<InterpretedScan> {
    return filter { scan ->
        scan.filamentInfo?.let { filamentInfo ->
            matchesFilamentTypeFilter(filamentInfo.filamentType, filterState) &&
            matchesColorFilter(filamentInfo.colorName, filterState) &&
            matchesBaseMaterialFilter(filamentInfo.filamentType, filterState) &&
            matchesMaterialSeriesFilter(filamentInfo.filamentType, filterState) &&
            matchesDateRangeFilter(scan.timestamp, filterState)
        } ?: false &&
        matchesScanResultFilter(scan.scanResult, filterState)
    }
}

// Individual filter predicates

private fun matchesFilamentTypeFilter(filamentType: String, filterState: FilterState): Boolean {
    return filterState.filamentTypes.isEmpty() || filterState.filamentTypes.contains(filamentType)
}

private fun matchesColorFilter(colorName: String, filterState: FilterState): Boolean {
    return filterState.colors.isEmpty() || filterState.colors.contains(colorName)
}

private fun matchesBaseMaterialFilter(filamentType: String, filterState: FilterState): Boolean {
    if (filterState.baseMaterials.isEmpty()) return true
    val baseMaterial = filamentType.split(" ").firstOrNull() ?: ""
    return filterState.baseMaterials.contains(baseMaterial)
}

private fun matchesMaterialSeriesFilter(filamentType: String, filterState: FilterState): Boolean {
    if (filterState.materialSeries.isEmpty()) return true
    val parts = filamentType.split(" ")
    val series = if (parts.size >= 2) parts.drop(1).joinToString(" ") else ""
    return filterState.materialSeries.contains(series)
}

private fun matchesSuccessRateFilter(successRate: Float, filterState: FilterState): Boolean {
    return successRate >= filterState.minSuccessRate
}

private fun matchesResultFilter(successRate: Float, filterState: FilterState): Boolean {
    return when {
        filterState.showSuccessOnly -> successRate == 1.0f
        filterState.showFailuresOnly -> successRate < 1.0f
        else -> true
    }
}

private fun matchesScanResultFilter(scanResult: com.bscan.model.ScanResult, filterState: FilterState): Boolean {
    return when {
        filterState.showSuccessOnly -> scanResult == com.bscan.model.ScanResult.SUCCESS
        filterState.showFailuresOnly -> scanResult != com.bscan.model.ScanResult.SUCCESS
        else -> true
    }
}

private fun matchesDateRangeFilter(dateTime: LocalDateTime, filterState: FilterState): Boolean {
    return filterState.dateRangeDays?.let { days ->
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        dateTime.isAfter(cutoffDate)
    } ?: true
}

// Sorting functions

/**
 * Applies sorting to UniqueFilamentReel list based on sort property and direction.
 */
fun List<UniqueFilamentReel>.applyFilamentReelSorting(
    sortProperty: SortProperty,
    sortDirection: SortDirection
): List<UniqueFilamentReel> {
    return when (sortProperty) {
        SortProperty.FIRST_SCAN -> {
            // For now, use lastScanned - would need to calculate first scan from scan history
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.lastScanned }
            } else {
                sortedByDescending { it.lastScanned }
            }
        }
        SortProperty.LAST_SCAN -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.lastScanned }
            } else {
                sortedByDescending { it.lastScanned }
            }
        }
        SortProperty.NAME -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.colorName }
            } else {
                sortedByDescending { it.filamentInfo.colorName }
            }
        }
        SortProperty.SUCCESS_RATE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.successRate }
            } else {
                sortedByDescending { it.successRate }
            }
        }
        SortProperty.COLOR -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.colorName }
            } else {
                sortedByDescending { it.filamentInfo.colorName }
            }
        }
        SortProperty.MATERIAL_TYPE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.filamentType }
            } else {
                sortedByDescending { it.filamentInfo.filamentType }
            }
        }
    }
}

/**
 * Applies sorting to SkuInfo list based on sort property and direction.
 */
fun List<SkuInfo>.applySkuSorting(
    sortProperty: SortProperty,
    sortDirection: SortDirection
): List<SkuInfo> {
    return when (sortProperty) {
        SortProperty.FIRST_SCAN -> {
            // For now, use lastScanned - would need to calculate first scan from scan history
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.lastScanned }
            } else {
                sortedByDescending { it.lastScanned }
            }
        }
        SortProperty.LAST_SCAN -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.lastScanned }
            } else {
                sortedByDescending { it.lastScanned }
            }
        }
        SortProperty.NAME -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.colorName }
            } else {
                sortedByDescending { it.filamentInfo.colorName }
            }
        }
        SortProperty.SUCCESS_RATE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.successRate }
            } else {
                sortedByDescending { it.successRate }
            }
        }
        SortProperty.COLOR -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.colorName }
            } else {
                sortedByDescending { it.filamentInfo.colorName }
            }
        }
        SortProperty.MATERIAL_TYPE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo.filamentType }
            } else {
                sortedByDescending { it.filamentInfo.filamentType }
            }
        }
    }
}

/**
 * Applies sorting to InterpretedScan list based on sort property and direction.
 */
fun List<InterpretedScan>.applySorting(
    sortProperty: SortProperty,
    sortDirection: SortDirection
): List<InterpretedScan> {
    return when (sortProperty) {
        SortProperty.FIRST_SCAN, SortProperty.LAST_SCAN -> {
            // Individual scans - both mean the same thing
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.timestamp }
            } else {
                sortedByDescending { it.timestamp }
            }
        }
        SortProperty.NAME -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo?.colorName ?: it.uid }
            } else {
                sortedByDescending { it.filamentInfo?.colorName ?: it.uid }
            }
        }
        SortProperty.SUCCESS_RATE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.scanResult != com.bscan.model.ScanResult.SUCCESS }
            } else {
                sortedByDescending { it.scanResult != com.bscan.model.ScanResult.SUCCESS }
            }
        }
        SortProperty.COLOR -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo?.colorName ?: it.uid }
            } else {
                sortedByDescending { it.filamentInfo?.colorName ?: it.uid }
            }
        }
        SortProperty.MATERIAL_TYPE -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.filamentInfo?.filamentType ?: "" }
            } else {
                sortedByDescending { it.filamentInfo?.filamentType ?: "" }
            }
        }
    }
}

// Grouping functions

/**
 * Groups a list of UniqueFilamentReel by the specified grouping option.
 */
fun List<UniqueFilamentReel>.applyFilamentReelGrouping(groupByOption: GroupByOption): List<Pair<String, List<UniqueFilamentReel>>> {
    return when (groupByOption) {
        GroupByOption.NONE -> map { "ungrouped" to listOf(it) }
        GroupByOption.COLOR -> groupBy { it.filamentInfo.colorName }.toList()
        GroupByOption.BASE_MATERIAL -> groupBy { 
            it.filamentInfo.filamentType.split(" ").firstOrNull() ?: "Unknown"
        }.toList()
        GroupByOption.MATERIAL_SERIES -> groupBy { 
            val parts = it.filamentInfo.filamentType.split(" ")
            if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
        }.toList()
    }
}

/**
 * Groups a list of SkuInfo by the specified grouping option.
 */
fun List<SkuInfo>.applySkuGrouping(groupByOption: GroupByOption): List<Pair<String, List<SkuInfo>>> {
    return when (groupByOption) {
        GroupByOption.NONE -> map { "ungrouped" to listOf(it) }
        GroupByOption.COLOR -> groupBy { it.filamentInfo.colorName }.toList()
        GroupByOption.BASE_MATERIAL -> groupBy { 
            it.filamentInfo.filamentType.split(" ").firstOrNull() ?: "Unknown"
        }.toList()
        GroupByOption.MATERIAL_SERIES -> groupBy { 
            val parts = it.filamentInfo.filamentType.split(" ")
            if (parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
        }.toList()
    }
}

/**
 * Groups a list of InterpretedScan by the specified grouping option.
 */
fun List<InterpretedScan>.applyGrouping(groupByOption: GroupByOption): List<Pair<String, List<InterpretedScan>>> {
    return when (groupByOption) {
        GroupByOption.NONE -> map { "ungrouped" to listOf(it) }
        GroupByOption.COLOR -> groupBy { it.filamentInfo?.colorName ?: "Unknown" }.toList()
        GroupByOption.BASE_MATERIAL -> groupBy { 
            it.filamentInfo?.filamentType?.split(" ")?.firstOrNull() ?: "Unknown"
        }.toList()
        GroupByOption.MATERIAL_SERIES -> groupBy { 
            val parts = it.filamentInfo?.filamentType?.split(" ")
            if (parts != null && parts.size >= 2) parts.drop(1).joinToString(" ") else "Basic"
        }.toList()
    }
}