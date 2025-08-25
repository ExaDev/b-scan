package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.PhysicalComponent
import com.bscan.model.PhysicalComponentType
import com.bscan.repository.PhysicalComponentRepository
import com.bscan.ui.components.component.ComponentListCard
import com.bscan.ui.components.component.ComponentEditDialog
import com.bscan.ui.components.component.ComponentCopyDialog
import com.bscan.ui.components.component.ComponentDeleteConfirmationDialog
import kotlinx.coroutines.launch

/**
 * Screen for managing physical components used in inventory tracking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalComponentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { PhysicalComponentRepository(context) }
    val scope = rememberCoroutineScope()
    
    // State
    var components by remember { mutableStateOf<List<PhysicalComponent>>(emptyList()) }
    var filteredComponents by remember { mutableStateOf<List<PhysicalComponent>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<PhysicalComponentType?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCopyDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedComponent by remember { mutableStateOf<PhysicalComponent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Load components
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                components = repository.getFixedComponents() // Only show fixed components
                filteredComponents = components
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load components: ${e.message}"
                isLoading = false
            }
        }
    }
    
    // Filter components based on search and type
    LaunchedEffect(components, searchQuery, selectedTypeFilter) {
        filteredComponents = components.filter { component ->
            val matchesSearch = searchQuery.isBlank() ||
                component.name.contains(searchQuery, ignoreCase = true) ||
                component.manufacturer.contains(searchQuery, ignoreCase = true) ||
                component.description.contains(searchQuery, ignoreCase = true)
            
            val matchesType = selectedTypeFilter == null || component.type == selectedTypeFilter
            
            matchesSearch && matchesType
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Physical Components") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Component")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else {
                // Search and filter controls
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search components") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    singleLine = true
                )
                
                // Type filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("All") }
                        )
                    }
                    
                    items(PhysicalComponentType.values()) { type ->
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = { 
                                selectedTypeFilter = if (selectedTypeFilter == type) null else type
                            },
                            label = { Text(type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                // Components summary
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Components Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val builtInCount = components.count { !it.isUserDefined }
                        val userDefinedCount = components.count { it.isUserDefined }
                        
                        Text("Total: ${components.size} components")
                        Text("Built-in: $builtInCount")
                        Text("User-created: $userDefinedCount")
                        Text("Showing: ${filteredComponents.size}")
                    }
                }
                
                // Components list
                if (filteredComponents.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedTypeFilter != null) {
                                    "No components match your filters"
                                } else {
                                    "No components found"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (searchQuery.isNotBlank() || selectedTypeFilter != null) {
                                Text(
                                    text = "Try adjusting your search or filters",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredComponents) { component ->
                            ComponentListCard(
                                component = component,
                                onEdit = if (component.isUserDefined) {
                                    {
                                        selectedComponent = component
                                        showEditDialog = true
                                    }
                                } else null,
                                onCopy = {
                                    selectedComponent = component
                                    showCopyDialog = true
                                },
                                onDelete = if (component.isUserDefined) {
                                    {
                                        selectedComponent = component
                                        showDeleteDialog = true
                                    }
                                } else null
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
                        components = repository.getFixedComponents()
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
                        components = repository.getFixedComponents()
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
    
    if (showCopyDialog && selectedComponent != null) {
        ComponentCopyDialog(
            sourceComponent = selectedComponent!!,
            onSave = { copiedComponent ->
                scope.launch {
                    try {
                        repository.saveComponent(copiedComponent)
                        components = repository.getFixedComponents()
                        showCopyDialog = false
                        selectedComponent = null
                    } catch (e: Exception) {
                        errorMessage = "Failed to copy component: ${e.message}"
                    }
                }
            },
            onDismiss = { 
                showCopyDialog = false 
                selectedComponent = null
            }
        )
    }
    
    if (showDeleteDialog && selectedComponent != null) {
        ComponentDeleteConfirmationDialog(
            component = selectedComponent!!,
            onConfirm = {
                scope.launch {
                    try {
                        repository.deleteComponent(selectedComponent!!.id)
                        components = repository.getFixedComponents()
                        showDeleteDialog = false
                        selectedComponent = null
                    } catch (e: Exception) {
                        errorMessage = "Failed to delete component: ${e.message}"
                    }
                }
            },
            onDismiss = { 
                showDeleteDialog = false 
                selectedComponent = null
            }
        )
    }
}

@Composable
private fun LazyRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        content = content
    )
}