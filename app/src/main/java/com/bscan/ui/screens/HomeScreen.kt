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
import com.bscan.repository.ComponentRepository
import com.bscan.repository.UserComponentRepository
import com.bscan.model.Component
import com.bscan.model.DecryptedScanData
import com.bscan.service.ComponentGenerationService
import com.bscan.service.ComponentMergerService
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
    val repository = remember { ComponentRepository(context) }
    val scanHistoryRepository = remember { ScanHistoryRepository(context) }
    
    // View state
    var viewMode by remember { mutableStateOf(ViewMode.INVENTORY) }
    var sortProperty by remember { mutableStateOf(SortProperty.LAST_SCAN) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var groupByOption by remember { mutableStateOf(GroupByOption.NONE) }
    var isLoading by remember { mutableStateOf(true) }
    var filterState by remember { mutableStateOf(FilterState()) }
    
    // Data state - modernized to use Component-based architecture
    var components by remember { mutableStateOf(listOf<Component>()) }
    var scanData by remember { mutableStateOf(listOf<DecryptedScanData>()) }
    var availableFilamentTypes by remember { mutableStateOf(setOf<String>()) }
    var availableColors by remember { mutableStateOf(setOf<String>()) }
    var availableBaseMaterials by remember { mutableStateOf(setOf<String>()) }
    var availableMaterialSeries by remember { mutableStateOf(setOf<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Load data using on-demand component generation + user overlay system
    LaunchedEffect(Unit) {
        try {
            // Initialize services
            val componentGenerationService = ComponentGenerationService(context)
            val userComponentRepository = UserComponentRepository(context)
            val componentMergerService = ComponentMergerService()
            
            // Load scan data (source of truth)
            val allScanData = scanHistoryRepository.getAllDecryptedScans()
            scanData = allScanData
            
            // Generate components from scan data (on-demand, not persisted)
            val generatedComponents = componentGenerationService.generateComponentsFromScans(allScanData)
            
            // Load user overlays (persisted user customisations)
            val userOverlays = userComponentRepository.getActiveOverlays()
            
            // Merge generated components with user customisations
            val mergedComponents = componentMergerService.mergeComponents(generatedComponents, userOverlays)
            components = mergedComponents
            
            // Extract filter options from final merged components
            availableFilamentTypes = mergedComponents
                .mapNotNull { component -> component.metadata["materialType"] }
                .toSet()
            
            availableColors = mergedComponents
                .mapNotNull { component -> component.metadata["colorName"] }
                .toSet()
            
            availableBaseMaterials = mergedComponents
                .mapNotNull { component -> component.metadata["baseMaterial"] }
                .toSet()
            
            availableMaterialSeries = mergedComponents
                .mapNotNull { component -> component.metadata["series"] }
                .toSet()
                
        } catch (e: Exception) {
            // Fallback to empty state on error
            components = emptyList()
            scanData = emptyList()
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
            filamentReels = components.filter { it.category == "filament" },
            individualTags = components.filter { it.category == "rfid-tag" },
            allScans = scanData,
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