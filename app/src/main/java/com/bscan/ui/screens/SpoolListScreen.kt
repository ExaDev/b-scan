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
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.service.ComponentGenerationService
import com.bscan.model.Component
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.spool.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val catalogRepo = remember { CatalogRepository(context) }
    val userRepo = remember { UserDataRepository(context) }
    val unifiedDataAccess = remember { UnifiedDataAccess(catalogRepo, userRepo, context = context) }
    val componentGenerationService = remember { ComponentGenerationService(context) }
    var filamentComponents by remember { mutableStateOf(listOf<Component>()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var filterByType by remember { mutableStateOf("All Types") }
    
    LaunchedEffect(Unit) {
        try {
            // Generate components from all scan data and filter for filament-related items
            val allScans = unifiedDataAccess.getAllDecryptedScanData()
            val allComponents = componentGenerationService.generateComponentsFromScans(allScans)
            filamentComponents = allComponents.filter { component ->
                component.category == "filament" || 
                component.metadata.containsKey("filamentType") ||
                component.tags.contains("filament") ||
                component.isInventoryItem // Include inventory root components
            }
        } catch (e: Exception) {
            filamentComponents = emptyList()
        }
    }
    
    // Apply filters
    val filteredFilamentComponents = filamentComponents.filter { component ->
        val matchesSuccessFilter = when (selectedFilter) {
            "Successful Only" -> component.hasUniqueIdentifier()
            "High Success Rate" -> component.massGrams != null // Components with known mass are "successful"
            else -> true
        }
        
        val componentType = component.metadata["filamentType"] ?: component.category
        val detailedType = component.metadata["detailedFilamentType"]
        val matchesTypeFilter = when (filterByType) {
            "All Types" -> true
            else -> componentType == filterByType || detailedType == filterByType
        }
        
        matchesSuccessFilter && matchesTypeFilter
    }
    
    // Get unique filament types for filter
    val availableTypes = remember(filamentComponents) {
        try {
            val types = filamentComponents
                .mapNotNull { it.metadata["filamentType"] ?: it.category.takeIf { it != "general" } }
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
                FilamentReelStatisticsCard(filamentComponents = filamentComponents)
            }
            
            // Filter Row
            item {
                FilamentReelFilterSection(
                    selectedFilter = selectedFilter,
                    onFilterChanged = { selectedFilter = it },
                    selectedTypeFilter = filterByType,
                    onTypeFilterChanged = { filterByType = it },
                    availableTypes = availableTypes
                )
            }
            
            // Components List
            items(filteredFilamentComponents) { component ->
                FilamentReelCard(
                    component = component,
                    onClick = { componentId ->
                        onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, componentId)
                    }
                )
            }
            
            // Empty state
            if (filteredFilamentComponents.isEmpty()) {
                item {
                    FilamentReelListEmptyState(
                        hasFilamentComponents = filamentComponents.isNotEmpty(),
                        currentFilter = selectedFilter
                    )
                }
            }
        }
    }
}


