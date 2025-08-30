package com.bscan.ui.components.list

import com.bscan.model.Component
import com.bscan.model.DecryptedScanData
import java.time.LocalDateTime

/**
 * Utility functions for filtering and sorting Component data consistently across different views.
 * Provides reusable filtering and sorting logic for components and scan history.
 */

// Component filtering functions

/**
 * Simple text-based filter for components based on name and category.
 */
fun List<Component>.filterByText(searchText: String): List<Component> {
    if (searchText.isBlank()) return this
    val query = searchText.lowercase()
    return filter { component ->
        component.name.lowercase().contains(query) ||
        component.category.lowercase().contains(query) ||
        component.id.lowercase().contains(query)
    }
}

/**
 * Filter components by category.
 */
fun List<Component>.filterByCategory(categories: Set<String>): List<Component> {
    if (categories.isEmpty()) return this
    return filter { component ->
        categories.contains(component.category)
    }
}

/**
 * Filter scan history by date range.
 */
fun List<DecryptedScanData>.filterByDateRange(days: Int?): List<DecryptedScanData> {
    if (days == null) return this
    val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
    return filter { scanData ->
        scanData.timestamp.isAfter(cutoffDate)
    }
}

// Component sorting functions

enum class ComponentSortProperty {
    NAME,
    CATEGORY,
    CREATION_TIME
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

/**
 * Applies sorting to Component list based on sort property and direction.
 */
fun List<Component>.applySorting(
    sortProperty: ComponentSortProperty,
    sortDirection: SortDirection
): List<Component> {
    return when (sortProperty) {
        ComponentSortProperty.NAME -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.name }
            } else {
                sortedByDescending { it.name }
            }
        }
        ComponentSortProperty.CATEGORY -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.category }
            } else {
                sortedByDescending { it.category }
            }
        }
        ComponentSortProperty.CREATION_TIME -> {
            if (sortDirection == SortDirection.ASCENDING) {
                sortedBy { it.createdAt }
            } else {
                sortedByDescending { it.createdAt }
            }
        }
    }
}

/**
 * Applies sorting to scan history based on timestamp.
 */
fun List<DecryptedScanData>.applySorting(
    sortDirection: SortDirection
): List<DecryptedScanData> {
    return if (sortDirection == SortDirection.ASCENDING) {
        sortedBy { it.timestamp }
    } else {
        sortedByDescending { it.timestamp }
    }
}

// Component grouping functions

enum class ComponentGroupByOption {
    NONE,
    CATEGORY,
    PARENT_COMPONENT
}

/**
 * Groups a list of Components by the specified grouping option.
 */
fun List<Component>.applyGrouping(groupByOption: ComponentGroupByOption): List<Pair<String, List<Component>>> {
    return when (groupByOption) {
        ComponentGroupByOption.NONE -> map { "All Components" to listOf(it) }
        ComponentGroupByOption.CATEGORY -> groupBy { it.category.replaceFirstChar { char -> char.uppercase() } }.toList()
        ComponentGroupByOption.PARENT_COMPONENT -> groupBy { 
            it.parentComponentId ?: "Root Components"
        }.toList()
    }
}