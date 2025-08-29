package com.bscan.ui.components.inventory

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.vector.ImageVector
import com.bscan.model.Component

/**
 * Utility functions for inventory UI components
 */

/**
 * Get icon for component category
 */
fun getComponentIcon(category: String): ImageVector = when (category.lowercase()) {
    "filament" -> Icons.Default.Polymer
    "spool" -> Icons.Default.Circle
    "core" -> Icons.Default.DonutLarge
    "adapter" -> Icons.Default.Transform
    "packaging" -> Icons.Default.LocalShipping
    "rfid-tag" -> Icons.Default.Sensors
    "filament-tray" -> Icons.Default.Inventory
    "nozzle" -> Icons.Default.Settings
    "hotend" -> Icons.Default.Thermostat
    "tool" -> Icons.Default.Build
    "equipment" -> Icons.Default.Engineering
    "consumable" -> Icons.Default.Inventory2
    "part" -> Icons.Default.Hardware
    "assembly" -> Icons.Default.AccountTree
    else -> Icons.Default.Category
}

/**
 * Get category-specific background colour
 */
fun getCategoryColor(category: String, colorScheme: ColorScheme) = when (category.lowercase()) {
    "filament" -> colorScheme.primaryContainer
    "rfid-tag" -> colorScheme.secondaryContainer
    "core", "spool" -> colorScheme.tertiaryContainer
    "adapter", "packaging" -> colorScheme.surfaceVariant
    "tool", "equipment" -> colorScheme.primaryContainer
    "nozzle", "hotend" -> colorScheme.secondaryContainer
    "consumable" -> colorScheme.tertiaryContainer
    else -> colorScheme.surface
}

/**
 * Format component ID for display (show last 8 characters if longer)
 */
fun formatComponentId(componentId: String): String {
    return if (componentId.length > 8) {
        componentId.takeLast(8)
    } else {
        componentId
    }
}

/**
 * Calculate total mass including all child components recursively
 */
fun calculateTotalMass(component: Component, allComponents: List<Component>): Float? {
    val ownMass = component.massGrams ?: 0f
    val childComponents = allComponents.filter { it.parentComponentId == component.id }
    
    if (childComponents.isEmpty()) {
        return component.massGrams
    }
    
    val childMass = childComponents.mapNotNull { calculateTotalMass(it, allComponents) }.sum()
    
    return if (component.massGrams != null || childComponents.any { it.massGrams != null }) {
        ownMass + childMass
    } else {
        null
    }
}

/**
 * Get hierarchical depth of a component
 */
fun getComponentDepth(component: Component, allComponents: List<Component>): Int {
    var depth = 0
    var currentParentId = component.parentComponentId
    
    while (currentParentId != null) {
        val parent = allComponents.find { it.id == currentParentId }
        currentParentId = parent?.parentComponentId
        depth++
        
        // Prevent infinite loops in case of circular references
        if (depth > 10) break
    }
    
    return depth
}

/**
 * Get all child components recursively
 */
fun getAllChildComponents(parentComponent: Component, allComponents: List<Component>): List<Component> {
    val directChildren = allComponents.filter { it.parentComponentId == parentComponent.id }
    val allChildren = mutableListOf<Component>()
    
    for (child in directChildren) {
        allChildren.add(child)
        allChildren.addAll(getAllChildComponents(child, allComponents))
    }
    
    return allChildren
}

/**
 * Check if a component has any children
 */
fun hasChildComponents(component: Component, allComponents: List<Component>): Boolean {
    return allComponents.any { it.parentComponentId == component.id }
}

/**
 * Get component hierarchy path (breadcrumb trail)
 */
fun getComponentPath(component: Component, allComponents: List<Component>): List<Component> {
    val path = mutableListOf<Component>()
    var current: Component? = component
    
    while (current != null) {
        path.add(0, current) // Add to beginning to maintain order
        current = if (current.parentComponentId != null) {
            allComponents.find { it.id == current.parentComponentId }
        } else {
            null
        }
        
        // Prevent infinite loops
        if (path.size > 10) break
    }
    
    return path
}

/**
 * Calculate inventory statistics
 */
data class InventoryStats(
    val totalItems: Int,
    val totalComponents: Int,
    val uniqueManufacturers: Int,
    val categoryBreakdown: List<Pair<String, Int>>,
    val tagBreakdown: List<Pair<String, Int>>,
    val totalMass: Float?
)

/**
 * Calculate comprehensive inventory statistics
 */
fun calculateInventoryStats(
    inventoryItems: List<Component>,
    allComponents: List<Component>
): InventoryStats {
    val totalItems = inventoryItems.size
    val totalComponents = allComponents.size
    val uniqueManufacturers = allComponents.map { it.manufacturer }.distinct().size
    
    // Category breakdown (top 5)
    val categoryBreakdown = allComponents.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    
    // Tag breakdown (top 5)
    val tagBreakdown = allComponents.flatMap { it.tags }
        .groupBy { it }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5)
    
    // Total mass calculation
    val totalMass = inventoryItems.mapNotNull { calculateTotalMass(it, allComponents) }.sum()
        .takeIf { it > 0f }
    
    return InventoryStats(
        totalItems = totalItems,
        totalComponents = totalComponents,
        uniqueManufacturers = uniqueManufacturers,
        categoryBreakdown = categoryBreakdown,
        tagBreakdown = tagBreakdown,
        totalMass = totalMass
    )
}

/**
 * Filter components based on search query
 */
fun filterComponents(
    components: List<Component>,
    searchQuery: String,
    selectedCategory: String?,
    selectedTag: String?
): List<Component> {
    return components.filter { component ->
        val matchesSearch = searchQuery.isEmpty() || 
            component.name.contains(searchQuery, ignoreCase = true) ||
            component.identifiers.any { it.value.contains(searchQuery, ignoreCase = true) } ||
            component.manufacturer.contains(searchQuery, ignoreCase = true) ||
            component.description.contains(searchQuery, ignoreCase = true)
        
        val matchesCategory = selectedCategory == null || component.category == selectedCategory
        val matchesTag = selectedTag == null || selectedTag in component.tags
        
        matchesSearch && matchesCategory && matchesTag
    }
}