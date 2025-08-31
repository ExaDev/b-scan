package com.bscan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.model.ScanStage
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.GraphRepository
import com.bscan.model.graph.entities.*
import com.bscan.model.graph.Entity
import com.bscan.model.DecryptedScanData
import com.bscan.service.GraphDataService
import com.bscan.ui.screens.home.*
import com.bscan.ui.screens.home.ViewMode
import com.bscan.ui.screens.home.SortProperty
import com.bscan.ui.screens.home.SortDirection
import com.bscan.ui.screens.home.GroupByOption
import com.bscan.ui.screens.home.FilterState

@Composable
fun HomeScreen(
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val graphRepository = remember { GraphRepository(context) }
    val scanHistoryRepository = remember { ScanHistoryRepository(context) }
    
    // View state
    var viewMode by remember { mutableStateOf(ViewMode.INVENTORY) }
    var sortProperty by remember { mutableStateOf(SortProperty.LAST_SCAN) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var groupByOption by remember { mutableStateOf(GroupByOption.NONE) }
    var isLoading by remember { mutableStateOf(true) }
    var filterState by remember { mutableStateOf(FilterState()) }
    
    // Data state - modernized to use Entity-based graph architecture
    var physicalComponents by remember { mutableStateOf(listOf<PhysicalComponent>()) }
    var inventoryItems by remember { mutableStateOf(listOf<InventoryItem>()) }
    var scanActivities by remember { mutableStateOf(listOf<Activity>()) }
    var availableFilamentTypes by remember { mutableStateOf(setOf<String>()) }
    var availableColors by remember { mutableStateOf(setOf<String>()) }
    var availableBaseMaterials by remember { mutableStateOf(setOf<String>()) }
    var availableMaterialSeries by remember { mutableStateOf(setOf<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Load data using graph-based entity system
    LaunchedEffect(Unit) {
        try {
            // Load entities from graph
            val allPhysicalComponents = graphRepository.getEntitiesByType(EntityTypes.PHYSICAL_COMPONENT)
                .filterIsInstance<PhysicalComponent>()
            physicalComponents = allPhysicalComponents
            
            val allInventoryItems = graphRepository.getEntitiesByType(EntityTypes.INVENTORY_ITEM)
                .filterIsInstance<InventoryItem>()
            inventoryItems = allInventoryItems
            
            val allActivities = graphRepository.getEntitiesByType(EntityTypes.ACTIVITY)
                .filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
            scanActivities = allActivities
            
            // Extract filter options from physical components
            availableFilamentTypes = allPhysicalComponents
                .mapNotNull { component -> component.getProperty<String>("materialType") }
                .toSet()
            
            availableColors = allPhysicalComponents
                .mapNotNull { component -> component.getProperty<String>("colorName") }
                .toSet()
            
            availableBaseMaterials = allPhysicalComponents
                .mapNotNull { component -> component.getProperty<String>("baseMaterial") }
                .toSet()
            
            availableMaterialSeries = allPhysicalComponents
                .mapNotNull { component -> component.getProperty<String>("series") }
                .toSet()
                
        } catch (e: Exception) {
            // Fallback to empty state on error
            physicalComponents = emptyList()
            inventoryItems = emptyList()
            scanActivities = emptyList()
            availableFilamentTypes = emptySet()
            availableColors = emptySet()
            availableBaseMaterials = emptySet()
            availableMaterialSeries = emptySet()
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
    
    // Always show data browser - empty state handled via overscroll
    DataBrowserScreen(
            viewMode = viewMode,
            sortProperty = sortProperty,
            sortDirection = sortDirection,
            groupByOption = groupByOption,
            filterState = filterState,
            filamentComponents = physicalComponents.filter { it.getProperty<String>("category") == "filament" },
            inventoryItems = inventoryItems,
            allScanActivities = scanActivities,
            availableFilamentTypes = availableFilamentTypes,
            availableColors = availableColors,
            availableBaseMaterials = availableBaseMaterials,
            availableMaterialSeries = availableMaterialSeries,
            showSortMenu = showSortMenu,
            showFilterMenu = showFilterMenu,
            scanState = scanState,
            scanProgress = scanProgress,
            onSimulateScan = onSimulateScan,
            onViewModeChange = { viewMode = it },
            onSortPropertyChange = { sortProperty = it },
            onSortDirectionToggle = { 
                sortDirection = if (sortDirection == SortDirection.ASCENDING) 
                    SortDirection.DESCENDING 
                else 
                    SortDirection.ASCENDING 
            },
            onGroupByOptionChange = { groupByOption = it },
            onFilterStateChange = { filterState = it },
            onShowSortMenu = { showSortMenu = it },
            onShowFilterMenu = { showFilterMenu = it },
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToHistory = onNavigateToHistory,
            onNavigateToDetails = onNavigateToDetails,
            modifier = modifier
        )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        scanState = ScanState.IDLE,
        scanProgress = null,
        onSimulateScan = {},
        onNavigateToSettings = {},
        onNavigateToHistory = {},
        onNavigateToDetails = { _, _ -> }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenScanningPreview() {
    HomeScreen(
        scanState = ScanState.PROCESSING,
        scanProgress = ScanProgress(
            stage = ScanStage.READING_BLOCKS,
            percentage = 0.6f,
            statusMessage = "Reading NFC tag..."
        ),
        onSimulateScan = {},
        onNavigateToSettings = {},
        onNavigateToHistory = {},
        onNavigateToDetails = { _, _ -> }
    )
}