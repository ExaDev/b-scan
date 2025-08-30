package com.bscan.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import com.bscan.ui.components.inventory.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Detailed screen for viewing and editing individual components
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ComponentDetailScreen(
    componentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ComponentRepository(context) }
    
    // State
    var component by remember { mutableStateOf<Component?>(null) }
    var allComponents by remember { mutableStateOf<List<Component>>(emptyList()) }
    var childComponents by remember { mutableStateOf<List<Component>>(emptyList()) }
    var parentComponent by remember { mutableStateOf<Component?>(null) }
    var siblingComponents by remember { mutableStateOf<List<Component>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    
    // Load component data
    LaunchedEffect(componentId) {
        scope.launch {
            try {
                isLoading = true
                error = null
                
                // Load all components to get relationships
                allComponents = repository.getComponents()
                
                // Find the specific component
                val foundComponent = allComponents.find { it.id == componentId }
                if (foundComponent == null) {
                    error = "Component not found"
                    isLoading = false
                    return@launch
                }
                
                component = foundComponent
                
                // Find related components
                childComponents = allComponents.filter { it.parentComponentId == componentId }
                parentComponent = foundComponent.parentComponentId?.let { parentId ->
                    allComponents.find { it.id == parentId }
                }
                siblingComponents = if (foundComponent.parentComponentId != null) {
                    allComponents.filter { 
                        it.parentComponentId == foundComponent.parentComponentId && it.id != componentId 
                    }
                } else {
                    emptyList()
                }
                
                isLoading = false
            } catch (e: Exception) {
                error = "Failed to load component: ${e.message}"
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(component?.name ?: "Component Details")
                        component?.let { comp ->
                            Text(
                                text = comp.category,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save changes" else "Edit component"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Error",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            component?.let { comp ->
                ComponentDetailContent(
                    component = comp,
                    childComponents = childComponents,
                    parentComponent = parentComponent,
                    siblingComponents = siblingComponents,
                    allComponents = allComponents,
                    isEditing = isEditing,
                    onNavigateToDetails = onNavigateToDetails,
                    modifier = modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Main content area with tabs for different views
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComponentDetailContent(
    component: Component,
    childComponents: List<Component>,
    parentComponent: Component?,
    siblingComponents: List<Component>,
    allComponents: List<Component>,
    isEditing: Boolean,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val tabTitles = listOf("Overview", "Identifiers", "Relationships", "History")
    val scope = rememberCoroutineScope()
    
    Column(modifier = modifier.fillMaxSize()) {
        // Tab row
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(title) }
                )
            }
        }
        
        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ComponentOverviewTab(
                    component = component,
                    childComponents = childComponents,
                    allComponents = allComponents,
                    isEditing = isEditing
                )
                1 -> ComponentIdentifiersTab(
                    component = component,
                    isEditing = isEditing
                )
                2 -> ComponentRelationshipsTab(
                    component = component,
                    childComponents = childComponents,
                    parentComponent = parentComponent,
                    siblingComponents = siblingComponents,
                    onNavigateToDetails = onNavigateToDetails
                )
                3 -> ComponentHistoryTab(component = component)
            }
        }
    }
}

/**
 * Overview tab showing general component information
 */
@Composable
private fun ComponentOverviewTab(
    component: Component,
    childComponents: List<Component>,
    allComponents: List<Component>,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic information card
        item {
            ComponentBasicInfoCard(
                component = component,
                isEditing = isEditing
            )
        }
        
        // Mass information card
        item {
            ComponentMassInfoCard(
                component = component,
                childComponents = childComponents,
                allComponents = allComponents,
                isEditing = isEditing
            )
        }
        
        // Metadata card
        if (component.metadata.isNotEmpty()) {
            item {
                ComponentMetadataCard(
                    component = component,
                    isEditing = isEditing
                )
            }
        }
        
        // Tags and categories
        item {
            ComponentTagsCard(
                component = component,
                isEditing = isEditing
            )
        }
    }
}

/**
 * Identifiers tab showing all component identifiers
 */
@Composable
private fun ComponentIdentifiersTab(
    component: Component,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Component Identifiers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (component.identifiers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No identifiers configured",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(component.identifiers) { identifier ->
                ComponentIdentifierCard(
                    identifier = identifier,
                    isEditing = isEditing
                )
            }
        }
    }
}

/**
 * Relationships tab showing component hierarchy
 */
@Composable
private fun ComponentRelationshipsTab(
    component: Component,
    childComponents: List<Component>,
    parentComponent: Component?,
    siblingComponents: List<Component>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Parent component
        if (parentComponent != null) {
            item {
                Text(
                    text = "Parent Component",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            item {
                ComponentRelationshipCard(
                    component = parentComponent,
                    relationship = "Parent",
                    onNavigateToDetails = onNavigateToDetails
                )
            }
        }
        
        // Child components
        if (childComponents.isNotEmpty()) {
            item {
                Text(
                    text = "Child Components (${childComponents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(childComponents) { childComponent ->
                ComponentRelationshipCard(
                    component = childComponent,
                    relationship = "Child",
                    onNavigateToDetails = onNavigateToDetails
                )
            }
        }
        
        // Sibling components
        if (siblingComponents.isNotEmpty()) {
            item {
                Text(
                    text = "Sibling Components (${siblingComponents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(siblingComponents) { siblingComponent ->
                ComponentRelationshipCard(
                    component = siblingComponent,
                    relationship = "Sibling",
                    onNavigateToDetails = onNavigateToDetails
                )
            }
        }
        
        // Empty state
        if (parentComponent == null && childComponents.isEmpty() && siblingComponents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "This component has no relationships",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * History tab showing component timeline
 */
@Composable
private fun ComponentHistoryTab(
    component: Component,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ComponentHistoryCard(component = component)
        }
    }
}

// Helper card components would go here...
// For brevity, I'll create simplified versions

@Composable
private fun ComponentBasicInfoCard(
    component: Component,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = getComponentIcon(component.category),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = component.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Category: ${component.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Manufacturer: ${component.manufacturer}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (component.description.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = component.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ComponentMassInfoCard(
    component: Component,
    childComponents: List<Component>,
    allComponents: List<Component>,
    isEditing: Boolean,
    modifier: Modifier = Modifier
) {
    val totalMass = calculateTotalMass(component, allComponents)
    
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Mass Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (component.massGrams != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Mass:")
                    Text(
                        text = "${String.format("%.1f", component.massGrams)}g",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            if (component.fullMassGrams != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Full Mass:")
                    Text(
                        text = "${String.format("%.1f", component.fullMassGrams)}g",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (totalMass != null && childComponents.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Mass (incl. children):")
                    Text(
                        text = "${String.format("%.1f", totalMass)}g",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            if (component.variableMass) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Variable mass component",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// Additional card components would be implemented similarly...
// For brevity, creating placeholder implementations

@Composable
private fun ComponentMetadataCard(component: Component, isEditing: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Implementation would show metadata key-value pairs
        }
    }
}

@Composable
private fun ComponentTagsCard(component: Component, isEditing: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // Implementation would show tags as chips
        }
    }
}

@Composable
private fun ComponentIdentifierCard(identifier: ComponentIdentifier, isEditing: Boolean, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(identifier.type.name, style = MaterialTheme.typography.titleSmall)
            Text(identifier.value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ComponentRelationshipCard(
    component: Component, 
    relationship: String,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            component.getPrimaryTrackingIdentifier()?.let { identifier ->
                onNavigateToDetails?.invoke(DetailType.COMPONENT, identifier.value)
            }
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(getComponentIcon(component.category), contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(component.name, fontWeight = FontWeight.Medium)
                Text(relationship, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun ComponentHistoryCard(component: Component, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Component History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Created: ${java.time.Instant.ofEpochMilli(component.createdAt)}", style = MaterialTheme.typography.bodyMedium)
            Text("Last Updated: ${component.lastUpdated.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComponentDetailScreenPreview() {
    MaterialTheme {
        ComponentDetailScreen(
            componentId = "test-component",
            onNavigateBack = { }
        )
    }
}