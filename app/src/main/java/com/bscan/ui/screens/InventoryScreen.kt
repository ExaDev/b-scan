package com.bscan.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bscan.MainViewModel
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import com.bscan.ui.components.common.ConfirmationDialog
import com.bscan.ui.components.inventory.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val inventoryViewModel = remember { InventoryViewModel(context) }
    val uiState by inventoryViewModel.uiState.collectAsStateWithLifecycle()
    
    // Show delete confirmation dialog
    var componentToDelete by remember { mutableStateOf<Component?>(null) }
    var componentsToDelete by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilters by remember { mutableStateOf(false) }
    
    
    // Delete confirmation dialog for single component
    componentToDelete?.let { component ->
        ConfirmationDialog(
            title = "Remove Component",
            message = "Remove component '${component.name}' and all its children? This cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                inventoryViewModel.deleteComponent(component)
                componentToDelete = null
            },
            onDismiss = { componentToDelete = null },
            isDestructive = true
        )
    }
    
    // Bulk delete confirmation dialog
    if (componentsToDelete.isNotEmpty()) {
        ConfirmationDialog(
            title = "Remove Components",
            message = "Remove ${componentsToDelete.size} components and all their children? This cannot be undone.",
            confirmText = "Remove All",
            onConfirm = {
                inventoryViewModel.deleteBulkComponents(componentsToDelete)
                componentsToDelete = emptySet()
            },
            onDismiss = { componentsToDelete = emptySet() },
            isDestructive = true
        )
    }
    
    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error somehow - could use SnackbarHost if added to Scaffold
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Bulk selection toggle
                    IconButton(onClick = { inventoryViewModel.toggleBulkSelectionMode() }) {
                        Icon(
                            imageVector = if (uiState.isBulkSelectionMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (uiState.isBulkSelectionMode) "Exit selection mode" else "Enter selection mode",
                            tint = if (uiState.isBulkSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    // Filter toggle
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Toggle filters",
                            tint = if (showFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Show bulk actions when in bulk selection mode
            if (uiState.isBulkSelectionMode && uiState.selectedComponents.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { componentsToDelete = uiState.selectedComponents },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Quick actions FAB for general inventory operations
                InventoryQuickActionsFAB(
                    onScanComponent = { /* TODO: Trigger NFC scan */ },
                    onAddComponent = { /* TODO: Manual component creation */ },
                    onExportInventory = { /* TODO: Export inventory to file */ },
                    onRefreshInventory = { inventoryViewModel.loadInventoryData() }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // View mode selector
            InventoryViewModeSelector(
                selectedViewMode = uiState.viewMode,
                onViewModeChange = { inventoryViewModel.setViewMode(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Filters panel (collapsible)
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                InventoryFilterPanel(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = { inventoryViewModel.setSearchQuery(it) },
                    selectedCategory = uiState.selectedCategory,
                    onCategoryChange = { inventoryViewModel.setSelectedCategory(it) },
                    selectedTag = uiState.selectedTag,
                    onTagChange = { inventoryViewModel.setSelectedTag(it) },
                    selectedManufacturer = uiState.selectedManufacturer,
                    onManufacturerChange = { inventoryViewModel.setSelectedManufacturer(it) },
                    availableCategories = inventoryViewModel.getAvailableCategories(),
                    availableTags = inventoryViewModel.getAvailableTags(),
                    availableManufacturers = inventoryViewModel.getAvailableManufacturers(),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Sorting selector
            InventorySortingSelector(
                selectedSortMode = uiState.sortMode,
                onSortModeChange = { inventoryViewModel.setSortMode(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Bulk selection info bar
            if (uiState.isBulkSelectionMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${uiState.selectedComponents.size} items selected",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Row {
                            TextButton(
                                onClick = { inventoryViewModel.selectAllComponents() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Select All")
                            }
                            
                            TextButton(
                                onClick = { inventoryViewModel.clearAllSelections() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
            
            // Content based on state
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredComponents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    InventoryEmptyState()
                }
            } else {
                // Statistics summary
                InventoryStatisticsCard(
                    allComponents = uiState.allComponents,
                    inventoryItems = uiState.inventoryItems,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                // Content based on view mode
                when (uiState.viewMode) {
                    InventoryViewMode.DETAILED -> {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.filteredComponents, key = { it.id }) { inventoryItem ->
                                InventoryItemCard(
                                    inventoryItem = inventoryItem,
                                    allComponents = uiState.allComponents,
                                    isExpanded = inventoryItem.id in uiState.expandedComponents,
                                    onToggleExpanded = { componentId ->
                                        inventoryViewModel.toggleComponentExpansion(componentId)
                                    },
                                    onDeleteComponent = { componentToDelete = it },
                                    onNavigateToDetails = onNavigateToDetails
                                )
                            }
                        }
                    }
                    
                    InventoryViewMode.COMPACT -> {
                        InventoryCompactListView(
                            inventoryItems = uiState.filteredComponents,
                            allComponents = uiState.allComponents,
                            selectedComponents = uiState.selectedComponents,
                            isBulkSelectionMode = uiState.isBulkSelectionMode,
                            onNavigateToDetails = onNavigateToDetails,
                            onToggleSelection = { inventoryViewModel.toggleComponentSelection(it) },
                            onDeleteComponent = { componentToDelete = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    InventoryViewMode.TABLE -> {
                        InventoryTableView(
                            inventoryItems = uiState.filteredComponents,
                            allComponents = uiState.allComponents,
                            selectedComponents = uiState.selectedComponents,
                            isBulkSelectionMode = uiState.isBulkSelectionMode,
                            onNavigateToDetails = onNavigateToDetails,
                            onToggleSelection = { inventoryViewModel.toggleComponentSelection(it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    InventoryViewMode.GALLERY -> {
                        InventoryGalleryView(
                            inventoryItems = uiState.filteredComponents,
                            allComponents = uiState.allComponents,
                            selectedComponents = uiState.selectedComponents,
                            isBulkSelectionMode = uiState.isBulkSelectionMode,
                            onNavigateToDetails = onNavigateToDetails,
                            onToggleSelection = { inventoryViewModel.toggleComponentSelection(it) },
                            onDeleteComponent = { componentToDelete = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}







@Preview(showBackground = true)
@Composable
fun InventoryScreenPreview() {
    MaterialTheme {
        InventoryScreen(
            viewModel = viewModel(),
            onNavigateBack = {},
            onNavigateToDetails = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun InventoryEmptyStatePreview() {
    MaterialTheme {
        InventoryEmptyState()
    }
}

