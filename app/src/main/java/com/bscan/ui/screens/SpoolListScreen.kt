package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.UniqueSpool
import com.bscan.ui.screens.spool.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    var spools by remember { mutableStateOf(listOf<UniqueSpool>()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var filterByType by remember { mutableStateOf("All Types") }
    
    LaunchedEffect(Unit) {
        try {
            spools = repository.getUniqueSpoolsByTray() // Group by tray UID instead of tag UID
        } catch (e: Exception) {
            spools = emptyList()
        }
    }
    
    // Apply filters
    val filteredSpools = spools.filter { spool ->
        val matchesSuccessFilter = when (selectedFilter) {
            "Successful Only" -> spool.successCount > 0
            "High Success Rate" -> spool.successRate >= 0.8f
            else -> true
        }
        
        val matchesTypeFilter = when (filterByType) {
            "All Types" -> true
            else -> spool.filamentInfo.filamentType == filterByType || 
                    spool.filamentInfo.detailedFilamentType == filterByType
        }
        
        matchesSuccessFilter && matchesTypeFilter
    }
    
    // Get unique filament types for filter
    val availableTypes = remember(repository) {
        try {
            val types = repository.getAllScans()
                .mapNotNull { it.filamentInfo?.filamentType }
                .distinct()
                .sorted()
            listOf("All Types") + types
        } catch (e: Exception) {
            listOf("All Types")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spool Collection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Statistics Card
            item {
                SpoolStatisticsCard(spools = spools)
            }
            
            // Filter Row
            item {
                SpoolFilterSection(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { selectedFilter = it },
                    selectedTypeFilter = filterByType,
                    onTypeFilterChanged = { filterByType = it },
                    availableTypes = availableTypes
                )
            }
            
            // Spools List
            items(filteredSpools) { spool ->
                SpoolCard(
                    spool = spool,
                    onClick = { trayUid ->
                        onNavigateToDetails?.invoke(DetailType.SPOOL, trayUid)
                    }
                )
            }
            
            // Empty state
            if (filteredSpools.isEmpty()) {
                item {
                    SpoolListEmptyState(
                        hasSpools = spools.isNotEmpty(),
                        currentFilter = selectedFilter
                    )
                }
            }
        }
    }
}


