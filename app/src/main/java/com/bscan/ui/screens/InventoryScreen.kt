package com.bscan.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bscan.MainViewModel
import com.bscan.model.*
import com.bscan.ui.screens.DetailType
import com.bscan.repository.ComponentRepository
import com.bscan.ui.components.ColorPreviewDot
import com.bscan.ui.components.common.ConfirmationDialog
import com.bscan.ui.components.common.EmptyStateView
import com.bscan.ui.components.common.StatisticDisplay
import com.bscan.ui.components.common.StatisticGrid
import java.time.format.DateTimeFormatter

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
                allComponents.filter { component ->
                    val matchesSearch = searchQuery.isEmpty() || 
                        component.name.contains(searchQuery, ignoreCase = true) ||
                        component.identifiers.any { it.value.contains(searchQuery, ignoreCase = true) } ||
                        component.manufacturer.contains(searchQuery, ignoreCase = true)
                    
                    val matchesCategory = selectedCategory == null || component.category == selectedCategory
                    val matchesTag = selectedTag == null || selectedTag in component.tags
                    
                    matchesSearch && matchesCategory && matchesTag
                }
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
                    HierarchicalInventoryStatisticsCard(
                        allComponents = allComponents,
                        inventoryItems = inventoryItems
                    )
                }
                
                // Search and filter controls
                item {
                    InventoryControlsCard(
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
                    HierarchicalInventoryItemCard(
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

// Utility functions for formatting and hierarchy
fun formatComponentId(componentId: String): String {
    return if (componentId.length > 8) {
        componentId.takeLast(8)
    } else {
        componentId
    }
}

fun getComponentIcon(category: String): ImageVector = when (category.lowercase()) {
    "filament" -> Icons.Default.Polymer
    "spool" -> Icons.Default.Circle
    "core" -> Icons.Default.DonutLarge
    "adapter" -> Icons.Default.Transform
    "packaging" -> Icons.Default.LocalShipping
    "rfid-tag" -> Icons.Default.Sensors
    "filament-tray" -> Icons.Default.Inventory
    "nozzle" -> Icons.Default.Settings
    "hotend" -> Icons.Default.Thermostat
    "tool" -> Icons.Default.Build
    else -> Icons.Default.Category
}

fun getCategoryColor(category: String, colorScheme: ColorScheme) = when (category.lowercase()) {
    "filament" -> colorScheme.primaryContainer
    "rfid-tag" -> colorScheme.secondaryContainer
    "core", "spool" -> colorScheme.tertiaryContainer
    "adapter", "packaging" -> colorScheme.surfaceVariant
    else -> colorScheme.surface
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalInventoryStatisticsCard(
    allComponents: List<Component>,
    inventoryItems: List<Component>,
    modifier: Modifier = Modifier
) {
    val totalInventoryItems = inventoryItems.size
    val totalComponents = allComponents.size
    val uniqueManufacturers = allComponents.map { it.manufacturer }.distinct().size
    
    // Count by categories
    val categoryStats = allComponents.groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(5) // Show top 5 categories
    
    // Count by tags
    val tagStats = allComponents.flatMap { it.tags }
        .groupBy { it }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }
        .take(3) // Show top 3 tags
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Hierarchical Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            val mainStats = listOf(
                "Inventory Items" to totalInventoryItems.toString(),
                "Total Components" to totalComponents.toString(),
                "Manufacturers" to uniqueManufacturers.toString()
            )
            
            StatisticGrid(
                statistics = mainStats,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Category breakdown
            if (categoryStats.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Top Categories",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryStats) { (category, count) ->
                        AssistChip(
                            onClick = { },
                            label = { 
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        getComponentIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "$category ($count)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        )
                    }
                }
            }
            
            // Tag breakdown
            if (tagStats.isNotEmpty()) {
                Text(
                    text = "Popular Tags: ${tagStats.joinToString(", ") { "${it.first} (${it.second})" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryControlsCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    selectedTag: String?,
    onTagChange: (String?) -> Unit,
    allComponents: List<Component>,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Get available categories and tags
    val availableCategories = remember(allComponents) {
        allComponents.map { it.category }.distinct().sorted()
    }
    val availableTags = remember(allComponents) {
        allComponents.flatMap { it.tags }.distinct().sorted()
    }
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Search & Filter",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search components...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                )
            )
            
            // Category filter
            if (availableCategories.isNotEmpty()) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onCategoryChange(null) },
                            label = { Text("All") },
                            selected = selectedCategory == null
                        )
                    }
                    
                    items(availableCategories) { category ->
                        FilterChip(
                            onClick = { 
                                onCategoryChange(if (selectedCategory == category) null else category)
                            },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        getComponentIcon(category),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(category)
                                }
                            },
                            selected = selectedCategory == category
                        )
                    }
                }
            }
            
            // Tag filter
            if (availableTags.isNotEmpty()) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            onClick = { onTagChange(null) },
                            label = { Text("All") },
                            selected = selectedTag == null
                        )
                    }
                    
                    items(availableTags.take(10)) { tag -> // Limit to prevent UI overflow
                        FilterChip(
                            onClick = { 
                                onTagChange(if (selectedTag == tag) null else tag)
                            },
                            label = { Text(tag) },
                            selected = selectedTag == tag
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalInventoryItemCard(
    inventoryItem: Component,
    allComponents: List<Component>,
    isExpanded: Boolean,
    onToggleExpanded: (String) -> Unit,
    onDeleteComponent: (Component) -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    val childComponents = allComponents.filter { it.parentComponentId == inventoryItem.id }
    val totalMass = calculateTotalMass(inventoryItem, allComponents)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                inventoryItem.getPrimaryTrackingIdentifier()?.let { identifier ->
                    onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, identifier.value)
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = tween(300)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with component info and expand/collapse
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        getComponentIcon(inventoryItem.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = inventoryItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Show primary identifier
                        inventoryItem.getPrimaryTrackingIdentifier()?.let { identifier ->
                            Text(
                                text = "${identifier.type.name}: ${formatComponentId(identifier.value)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand/collapse button
                    if (childComponents.isNotEmpty()) {
                        IconButton(
                            onClick = { onToggleExpanded(inventoryItem.id) }
                        ) {
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand"
                            )
                        }
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = { onDeleteComponent(inventoryItem) },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove inventory item"
                        )
                    }
                }
            }
            
            // Component metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Manufacturer: ${inventoryItem.manufacturer}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show tags
                    if (inventoryItem.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(inventoryItem.tags) { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { 
                                        Text(
                                            tag, 
                                            style = MaterialTheme.typography.labelSmall
                                        ) 
                                    },
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }
                    }
                }
                
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${childComponents.size} components",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Mass information
            if (totalMass != null) {
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Mass:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${String.format("%.1f", totalMass)}g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Expanded child components
            if (isExpanded && childComponents.isNotEmpty()) {
                HorizontalDivider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Child Components",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    childComponents.forEach { childComponent ->
                        HierarchicalComponentCard(
                            component = childComponent,
                            allComponents = allComponents,
                            depth = 1,
                            onDeleteComponent = onDeleteComponent
                        )
                    }
                }
            }
            
            // Timestamps
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Last Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = inventoryItem.lastUpdated.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Show description if available
                if (inventoryItem.description.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HierarchicalComponentCard(
    component: Component,
    allComponents: List<Component>,
    depth: Int = 0,
    onDeleteComponent: (Component) -> Unit,
    modifier: Modifier = Modifier
) {
    val childComponents = allComponents.filter { it.parentComponentId == component.id }
    val indentSize = (depth * 16).dp
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = getCategoryColor(component.category, MaterialTheme.colorScheme)
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentSize)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Hierarchy indicator
                    if (depth > 0) {
                        Box(
                            modifier = Modifier
                                .size(2.dp, 20.dp)
                                .background(
                                    MaterialTheme.colorScheme.outline,
                                    MaterialTheme.shapes.extraSmall
                                )
                        )
                    }
                    
                    // Component icon
                    Icon(
                        getComponentIcon(component.category),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = component.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Category chip
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    component.category,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            
                            // Variable mass indicator
                            if (component.variableMass) {
                                Icon(
                                    Icons.Default.TrendingDown,
                                    contentDescription = "Variable mass",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // Inferred mass indicator
                            if (component.inferredMass) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = "Inferred mass",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
                
                // Mass and actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Mass display
                    Column(horizontalAlignment = Alignment.End) {
                        component.massGrams?.let { mass ->
                            Text(
                                text = "${String.format("%.1f", mass)}g",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            // Show remaining percentage for variable mass
                            component.getRemainingPercentage()?.let { percentage ->
                                Text(
                                    text = "${(percentage * 100).toInt()}% remaining",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        percentage < 0.05f -> MaterialTheme.colorScheme.error
                                        percentage < 0.20f -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        } ?: Text(
                            text = "Unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Delete button
                    IconButton(
                        onClick = { onDeleteComponent(component) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Remove component",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Show identifiers
            if (component.identifiers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(component.identifiers.take(3)) { identifier ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "${identifier.type.name}: ${formatComponentId(identifier.value)}",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Recursively display child components
            if (childComponents.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    childComponents.forEach { childComponent ->
                        HierarchicalComponentCard(
                            component = childComponent,
                            allComponents = allComponents,
                            depth = depth + 1,
                            onDeleteComponent = onDeleteComponent,
                            modifier = Modifier.clip(MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to calculate total mass including children
fun calculateTotalMass(component: Component, allComponents: List<Component>): Float? {
    val ownMass = component.massGrams ?: 0f
    val childComponents = allComponents.filter { it.parentComponentId == component.id }
    
    if (childComponents.isEmpty()) {
        return component.massGrams
    }
    
    val childMass = childComponents.mapNotNull { calculateTotalMass(it, allComponents) }.sum()
    
    return if (component.massGrams != null || childComponents.any { it.massGrams != null }) {
        ownMass + childMass
    } else {
        null
    }
}

@Composable
fun InventoryEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyStateView(
        icon = Icons.Default.Inventory,
        title = "No Inventory Items",
        subtitle = "Scan some filament tags to start building your inventory",
        modifier = modifier
    )
}

