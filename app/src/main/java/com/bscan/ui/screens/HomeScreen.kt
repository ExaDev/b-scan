package com.bscan.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.bscan.ScanState
import com.bscan.model.ScanHistory
import com.bscan.model.ScanProgress
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UniqueSpool
import com.bscan.ui.screens.home.*

@Composable
fun HomeScreen(
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToDetails: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    
    // View state
    var viewMode by remember { mutableStateOf(ViewMode.SPOOLS) }
    var sortProperty by remember { mutableStateOf(SortProperty.LAST_SCAN) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var groupByOption by remember { mutableStateOf(GroupByOption.NONE) }
    var isLoading by remember { mutableStateOf(true) }
    var filterState by remember { mutableStateOf(FilterState()) }
    
    // Data state
    var spools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var individualTags by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var allScans by remember { mutableStateOf(listOf<ScanHistory>()) }
    var availableFilamentTypes by remember { mutableStateOf(setOf<String>()) }
    var availableColors by remember { mutableStateOf(setOf<String>()) }
    var availableBaseMaterials by remember { mutableStateOf(setOf<String>()) }
    var availableMaterialSeries by remember { mutableStateOf(setOf<String>()) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    
    // Load data
    LaunchedEffect(Unit) {
        try {
            spools = repository.getUniqueSpoolsByTray() // Group by tray UID for spools tab
            individualTags = repository.getUniqueSpools() // Individual tags for tags tab
            allScans = repository.getAllScans()
            availableFilamentTypes = allScans
                .mapNotNull { it.filamentInfo?.detailedFilamentType }
                .toSet()
            availableColors = allScans
                .mapNotNull { it.filamentInfo?.colorName }
                .toSet()
            availableBaseMaterials = allScans
                .mapNotNull { it.filamentInfo?.filamentType }
                .toSet()
            availableMaterialSeries = allScans
                .mapNotNull { it.filamentInfo?.detailedFilamentType }
                .mapNotNull { detailedType ->
                    // Extract series from detailed type (e.g., "Basic" from "PLA Basic")
                    val parts = detailedType.split(" ")
                    if (parts.size >= 2) parts.drop(1).joinToString(" ") else null
                }
                .toSet()
        } catch (e: Exception) {
            spools = emptyList()
            allScans = emptyList()
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
    
    if (spools.isEmpty() && allScans.isEmpty()) {
        // Show full scan prompt for first-time users
        ScanPromptScreen(modifier = modifier)
    } else {
        // Show data browser
        DataBrowserScreen(
            viewMode = viewMode,
            sortProperty = sortProperty,
            sortDirection = sortDirection,
            groupByOption = groupByOption,
            filterState = filterState,
            spools = spools,
            individualTags = individualTags,
            allScans = allScans,
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
}