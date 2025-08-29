package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bscan.MainViewModel
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import com.bscan.repository.ComponentRepository
import com.bscan.ui.components.common.ConfirmationDialog
import com.bscan.ui.components.inventory.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MainViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Load hierarchical components using ComponentRepository
    var allComponents by remember { mutableStateOf<List<Component>>(emptyList()) }
    var inventoryItems by remember { mutableStateOf<List<Component>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // UI state for hierarchy display
    var expandedComponents by remember { mutableStateOf<Set<String>>(emptySet()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    
    // Show delete confirmation dialog
    var componentToDelete by remember { mutableStateOf<Component?>(null) }
    
    // Load hierarchical component data
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val componentRepository = ComponentRepository(context)
                val loadedComponents = componentRepository.getComponents()
                val loadedInventoryItems = componentRepository.getInventoryItems()
                
                // Also check legacy data and provide migration option
                val unifiedDataAccess = viewModel.getUnifiedDataAccess()
                val legacyInventoryItems = unifiedDataAccess.getInventoryItems()
                val legacyComponents = unifiedDataAccess.getComponents()
                
                withContext(Dispatchers.Main) {
                    allComponents = loadedComponents
                    inventoryItems = loadedInventoryItems
                    isLoading = false
                    
                    // If we have legacy data but no new components, suggest migration
                    if (loadedComponents.isEmpty() && (legacyInventoryItems.isNotEmpty() || legacyComponents.isNotEmpty())) {
                        // Migration would be handled elsewhere - for now just show empty state
                        android.util.Log.i("InventoryScreen", "Legacy data detected: ${legacyInventoryItems.size} items, ${legacyComponents.size} components")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }
    
    // Delete confirmation dialog
    componentToDelete?.let { component ->
        ConfirmationDialog(
            title = "Remove Component",
            message = "Remove component '${component.name}' and all its children? This cannot be undone.",
            confirmText = "Remove",
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val componentRepository = ComponentRepository(context)
                        componentRepository.deleteComponent(component.id)
                        
                        // Refresh data
                        val updatedComponents = componentRepository.getComponents()
                        val updatedInventoryItems = componentRepository.getInventoryItems()
                        
                        withContext(Dispatchers.Main) {
                            allComponents = updatedComponents
                            inventoryItems = updatedInventoryItems
                        }
                    } catch (e: Exception) {
                        // Handle error silently for now
                    }
                }
                componentToDelete = null
            },
            onDismiss = { componentToDelete = null },
            isDestructive = true
        )
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
                }
            )
        }
    ) { paddingValues ->
        
        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (inventoryItems.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                InventoryEmptyState()
            }
        } else {
            // Filter components based on search and filters
            val filteredComponents = remember(allComponents, searchQuery, selectedCategory, selectedTag) {
                filterComponents(allComponents, searchQuery, selectedCategory, selectedTag)
            }
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Statistics summary
                item {
                    InventoryStatisticsCard(
                        allComponents = allComponents,
                        inventoryItems = inventoryItems
                    )
                }
                
                // Search and filter controls
                item {
                    InventorySearchAndFilters(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        selectedTag = selectedTag,
                        onTagChange = { selectedTag = it },
                        allComponents = allComponents
                    )
                }
                
                // Hierarchical inventory items display
                items(inventoryItems) { inventoryItem ->
                    InventoryItemCard(
                        inventoryItem = inventoryItem,
                        allComponents = filteredComponents,
                        isExpanded = inventoryItem.id in expandedComponents,
                        onToggleExpanded = { componentId ->
                            expandedComponents = if (componentId in expandedComponents) {
                                expandedComponents - componentId
                            } else {
                                expandedComponents + componentId
                            }
                        },
                        onDeleteComponent = { componentToDelete = it },
                        onNavigateToDetails = onNavigateToDetails
                    )
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

