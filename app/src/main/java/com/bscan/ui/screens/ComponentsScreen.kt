package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.Component
import com.bscan.repository.ComponentRepository
import com.bscan.ui.components.component.ComponentListCard
import com.bscan.ui.components.component.ComponentEditDialog
import com.bscan.ui.components.component.ComponentSelectionDialog
import kotlinx.coroutines.launch

/**
 * Screen for managing hierarchical components in the inventory system
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComponentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ComponentRepository(context) }
    val scope = rememberCoroutineScope()
    
    // State
    var components by remember { mutableStateOf<List<Component>>(emptyList()) }
    var filteredComponents by remember { mutableStateOf<List<Component>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedViewMode by remember { mutableStateOf(ViewMode.ALL) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedComponent by remember { mutableStateOf<Component?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Available categories for filtering
    val categories = remember(components) { 
        components.map { it.category }.distinct().sorted()
    }
    
    // Load components
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                components = repository.getComponents()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "Failed to load components: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // Filter components
    LaunchedEffect(components, searchQuery, selectedCategoryFilter, selectedViewMode) {
        filteredComponents = components.filter { component ->
            val matchesSearch = searchQuery.isBlank() || 
                               component.name.contains(searchQuery, ignoreCase = true) ||
                               component.category.contains(searchQuery, ignoreCase = true) ||
                               component.manufacturer.contains(searchQuery, ignoreCase = true) ||
                               component.description.contains(searchQuery, ignoreCase = true)
                               
            val matchesCategory = selectedCategoryFilter == null || component.category == selectedCategoryFilter
            
            val matchesViewMode = when (selectedViewMode) {
                ViewMode.ALL -> true
                ViewMode.INVENTORY_ITEMS -> component.isInventoryItem
                ViewMode.CHILD_COMPONENTS -> !component.isInventoryItem
                ViewMode.VARIABLE_MASS -> component.variableMass
                ViewMode.FIXED_MASS -> !component.variableMass
            }
            
            matchesSearch && matchesCategory && matchesViewMode
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Components") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCreateDialog = true }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Component")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search components") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // View mode filters
            Text(
                text = "View Mode:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(ViewMode.values()) { mode ->
                    FilterChip(
                        onClick = { selectedViewMode = mode },
                        label = { Text(mode.displayName) },
                        selected = selectedViewMode == mode
                    )
                }
            }
            
            // Category filters
            if (categories.isNotEmpty()) {
                Text(
                    text = "Filter by category:",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { selectedCategoryFilter = null },
                            label = { Text("All") },
                            selected = selectedCategoryFilter == null
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            onClick = { 
                                selectedCategoryFilter = if (selectedCategoryFilter == category) null else category 
                            },
                            label = { Text(category) },
                            selected = selectedCategoryFilter == category
                        )
                    }
                }
            }
            
            // Components count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Components (${filteredComponents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (components.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${components.count { it.isInventoryItem }} inventory",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Loading, error, or components list
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                errorMessage != null -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                
                filteredComponents.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (components.isEmpty()) "No components yet" else "No components found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (components.isEmpty()) 
                                "Add components to start building your inventory" 
                            else 
                                "Try adjusting your search or filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (components.isEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateDialog = true }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add First Component")
                            }
                        }
                    }
                }
                
                else -> {
                    // Components list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredComponents) { component ->
                            ComponentListCard(
                                component = component,
                                onEdit = {
                                    selectedComponent = component
                                    showEditDialog = true
                                },
                                onCopy = {
                                    // Create a copy with a new ID
                                    val copiedComponent = component.copy(
                                        id = "component_${System.currentTimeMillis()}",
                                        name = "${component.name} (Copy)",
                                        uniqueIdentifier = null, // Remove unique identifier from copies
                                        parentComponentId = null, // Remove parent relationship from copies
                                        childComponents = emptyList() // Remove child relationships from copies
                                    )
                                    
                                    scope.launch {
                                        try {
                                            repository.saveComponent(copiedComponent)
                                            components = repository.getComponents()
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to copy component: ${e.message}"
                                        }
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        try {
                                            repository.deleteComponent(component.id)
                                            components = repository.getComponents()
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to delete component: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showCreateDialog) {
        ComponentEditDialog(
            component = null,
            onSave = { newComponent ->
                scope.launch {
                    try {
                        repository.saveComponent(newComponent)
                        components = repository.getComponents()
                        showCreateDialog = false
                    } catch (e: Exception) {
                        errorMessage = "Failed to create component: ${e.message}"
                    }
                }
            },
            onDismiss = { showCreateDialog = false }
        )
    }
    
    if (showEditDialog && selectedComponent != null) {
        ComponentEditDialog(
            component = selectedComponent,
            onSave = { updatedComponent ->
                scope.launch {
                    try {
                        repository.saveComponent(updatedComponent)
                        components = repository.getComponents()
                        showEditDialog = false
                        selectedComponent = null
                    } catch (e: Exception) {
                        errorMessage = "Failed to update component: ${e.message}"
                    }
                }
            },
            onDismiss = { 
                showEditDialog = false
                selectedComponent = null
            }
        )
    }
}

enum class ViewMode(val displayName: String) {
    ALL("All Components"),
    INVENTORY_ITEMS("Inventory Items"),
    CHILD_COMPONENTS("Parts & Components"),
    VARIABLE_MASS("Variable Mass"),
    FIXED_MASS("Fixed Mass")
}