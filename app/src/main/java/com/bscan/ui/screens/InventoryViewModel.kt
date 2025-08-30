package com.bscan.ui.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.model.Component
import com.bscan.repository.ComponentRepository
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UserComponentRepository
import com.bscan.service.ComponentGenerationService
import com.bscan.service.ComponentMergerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * View modes for displaying inventory items
 */
enum class InventoryViewMode {
    DETAILED,    // Current expandable cards
    COMPACT,     // Minimal cards with key info
    TABLE,       // Dense grid-like display
    GALLERY      // Visual grid with icons
}

/**
 * Sorting options for inventory list
 */
enum class InventorySortMode {
    NAME_ASC,        // A-Z
    NAME_DESC,       // Z-A
    CATEGORY,        // Grouped by category
    MANUFACTURER,    // Grouped by manufacturer
    LAST_UPDATED,    // Most recently updated first
    MASS_DESC,       // Heaviest first
    CHILD_COUNT,     // Most components first
    CREATED_DATE     // Most recently created first
}

/**
 * UI state for the inventory screen
 */
data class InventoryUiState(
    val allComponents: List<Component> = emptyList(),
    val inventoryItems: List<Component> = emptyList(),
    val filteredComponents: List<Component> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val viewMode: InventoryViewMode = InventoryViewMode.DETAILED,
    val sortMode: InventorySortMode = InventorySortMode.NAME_ASC,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedTag: String? = null,
    val selectedManufacturer: String? = null,
    val expandedComponents: Set<String> = emptySet(),
    val selectedComponents: Set<String> = emptySet(),
    val isBulkSelectionMode: Boolean = false,
    val showOnlyWithChildren: Boolean = false,
    val showOnlyWithMass: Boolean = false,
    val massRangeMin: Float? = null,
    val massRangeMax: Float? = null
)

/**
 * ViewModel for managing inventory screen state and operations
 */
class InventoryViewModel(private val context: Context) : ViewModel() {
    
    private val componentRepository = ComponentRepository(context)
    
    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()
    
    init {
        loadInventoryData()
    }
    
    /**
     * Load all component data using on-demand generation + user overlay system
     */
    fun loadInventoryData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Initialize services
                val componentGenerationService = ComponentGenerationService(context)
                val scanHistoryRepository = ScanHistoryRepository(context)
                val userComponentRepository = UserComponentRepository(context)
                val componentMergerService = ComponentMergerService()
                
                // Load scan data (source of truth)
                val allScanData = scanHistoryRepository.getAllDecryptedScans()
                
                // Generate components from scan data (on-demand, not persisted)
                val generatedComponents = componentGenerationService.generateComponentsFromScans(allScanData)
                
                // Load user overlays (persisted user customisations)
                val userOverlays = userComponentRepository.getActiveOverlays()
                
                // Merge generated components with user customisations
                val allComponents = componentMergerService.mergeComponents(generatedComponents, userOverlays)
                
                // Filter for inventory items (root components with unique identifiers)
                val inventoryItems = allComponents.filter { it.isInventoryItem }
                
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        allComponents = allComponents,
                        inventoryItems = inventoryItems,
                        isLoading = false
                    )
                    
                    // Apply current filters
                    applyFiltersAndSorting()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load inventory: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Change the view mode (detailed, compact, table, gallery)
     */
    fun setViewMode(viewMode: InventoryViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = viewMode)
    }
    
    /**
     * Change the sorting mode
     */
    fun setSortMode(sortMode: InventorySortMode) {
        _uiState.value = _uiState.value.copy(sortMode = sortMode)
        applyFiltersAndSorting()
    }
    
    /**
     * Update search query
     */
    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFiltersAndSorting()
    }
    
    /**
     * Set category filter
     */
    fun setSelectedCategory(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        applyFiltersAndSorting()
    }
    
    /**
     * Set tag filter
     */
    fun setSelectedTag(tag: String?) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        applyFiltersAndSorting()
    }
    
    /**
     * Set manufacturer filter
     */
    fun setSelectedManufacturer(manufacturer: String?) {
        _uiState.value = _uiState.value.copy(selectedManufacturer = manufacturer)
        applyFiltersAndSorting()
    }
    
    /**
     * Toggle component expansion
     */
    fun toggleComponentExpansion(componentId: String) {
        val currentExpanded = _uiState.value.expandedComponents
        val newExpanded = if (componentId in currentExpanded) {
            currentExpanded - componentId
        } else {
            currentExpanded + componentId
        }
        _uiState.value = _uiState.value.copy(expandedComponents = newExpanded)
    }
    
    /**
     * Toggle bulk selection mode
     */
    fun toggleBulkSelectionMode() {
        val newMode = !_uiState.value.isBulkSelectionMode
        _uiState.value = _uiState.value.copy(
            isBulkSelectionMode = newMode,
            selectedComponents = if (!newMode) emptySet() else _uiState.value.selectedComponents
        )
    }
    
    /**
     * Toggle component selection in bulk mode
     */
    fun toggleComponentSelection(componentId: String) {
        val currentSelected = _uiState.value.selectedComponents
        val newSelected = if (componentId in currentSelected) {
            currentSelected - componentId
        } else {
            currentSelected + componentId
        }
        _uiState.value = _uiState.value.copy(selectedComponents = newSelected)
    }
    
    /**
     * Select all visible components
     */
    fun selectAllComponents() {
        val allVisibleIds = _uiState.value.filteredComponents.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedComponents = allVisibleIds)
    }
    
    /**
     * Clear all selections
     */
    fun clearAllSelections() {
        _uiState.value = _uiState.value.copy(selectedComponents = emptySet())
    }
    
    /**
     * Delete a single component
     */
    fun deleteComponent(component: Component) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                componentRepository.deleteComponent(component.id)
                loadInventoryData() // Refresh the data
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete component: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Delete multiple components in bulk
     */
    fun deleteBulkComponents(componentIds: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                componentIds.forEach { componentId ->
                    componentRepository.deleteComponent(componentId)
                }
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(selectedComponents = emptySet())
                }
                loadInventoryData() // Refresh the data
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete components: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Apply current filters and sorting to the component list
     */
    private fun applyFiltersAndSorting() {
        val state = _uiState.value
        var filtered = state.inventoryItems
        
        // Apply search filter
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { component ->
                component.name.contains(state.searchQuery, ignoreCase = true) ||
                component.category.contains(state.searchQuery, ignoreCase = true) ||
                component.manufacturer.contains(state.searchQuery, ignoreCase = true) ||
                component.description.contains(state.searchQuery, ignoreCase = true) ||
                component.tags.any { tag -> tag.contains(state.searchQuery, ignoreCase = true) } ||
                component.identifiers.any { identifier -> 
                    identifier.value.contains(state.searchQuery, ignoreCase = true) 
                }
            }
        }
        
        // Apply category filter
        state.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category == category }
        }
        
        // Apply tag filter
        state.selectedTag?.let { tag ->
            filtered = filtered.filter { it.tags.contains(tag) }
        }
        
        // Apply manufacturer filter
        state.selectedManufacturer?.let { manufacturer ->
            filtered = filtered.filter { it.manufacturer == manufacturer }
        }
        
        // Apply mass range filter
        if (state.massRangeMin != null || state.massRangeMax != null) {
            filtered = filtered.filter { component ->
                val mass = component.massGrams
                if (mass == null) false
                else {
                    val minCheck = state.massRangeMin?.let { mass >= it } ?: true
                    val maxCheck = state.massRangeMax?.let { mass <= it } ?: true
                    minCheck && maxCheck
                }
            }
        }
        
        // Apply other filters
        if (state.showOnlyWithChildren) {
            filtered = filtered.filter { component ->
                state.allComponents.any { it.parentComponentId == component.id }
            }
        }
        
        if (state.showOnlyWithMass) {
            filtered = filtered.filter { it.massGrams != null }
        }
        
        // Apply sorting
        filtered = when (state.sortMode) {
            InventorySortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            InventorySortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
            InventorySortMode.CATEGORY -> filtered.sortedBy { it.category }
            InventorySortMode.MANUFACTURER -> filtered.sortedBy { it.manufacturer }
            InventorySortMode.LAST_UPDATED -> filtered.sortedByDescending { it.lastUpdated }
            InventorySortMode.MASS_DESC -> filtered.sortedByDescending { it.massGrams ?: 0f }
            InventorySortMode.CHILD_COUNT -> filtered.sortedByDescending { component ->
                state.allComponents.count { it.parentComponentId == component.id }
            }
            InventorySortMode.CREATED_DATE -> filtered.sortedByDescending { it.createdAt }
        }
        
        _uiState.value = state.copy(filteredComponents = filtered)
    }
    
    /**
     * Get available categories for filtering
     */
    fun getAvailableCategories(): List<String> {
        return _uiState.value.allComponents
            .map { it.category }
            .distinct()
            .sorted()
    }
    
    /**
     * Get available tags for filtering
     */
    fun getAvailableTags(): List<String> {
        return _uiState.value.allComponents
            .flatMap { it.tags }
            .distinct()
            .sorted()
    }
    
    /**
     * Get available manufacturers for filtering
     */
    fun getAvailableManufacturers(): List<String> {
        return _uiState.value.allComponents
            .map { it.manufacturer }
            .distinct()
            .sorted()
    }
}