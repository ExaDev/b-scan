package com.bscan.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.ui.components.detail.*
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.screens.home.CatalogDisplayMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    detailType: DetailType,
    identifier: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: ((DetailType, String) -> Unit)? = null,
    onPurgeCache: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Log.d("DetailScreen", "Starting DetailScreen with type: $detailType, identifier: $identifier")
    
    val context = LocalContext.current
    val catalogRepository = remember { CatalogRepository(context) }
    val userRepository = remember { UserDataRepository(context) }
    val scanHistoryRepository = remember { ScanHistoryRepository(context) }
    val unifiedDataAccess = remember { UnifiedDataAccess(catalogRepository, userRepository, scanHistoryRepository) }
    val componentRepository = remember { com.bscan.repository.ComponentRepository(context) }
    val componentGroupingService = remember { com.bscan.service.ComponentGroupingService(componentRepository) }
    val massInferenceService = remember { com.bscan.service.MassInferenceService(componentRepository) }
    val interpreterFactory = remember { com.bscan.interpreter.InterpreterFactory(unifiedDataAccess) }
    val viewModel = remember { 
        DetailViewModel(
            unifiedDataAccess = unifiedDataAccess,
            componentRepository = componentRepository,
            componentGroupingService = componentGroupingService,
            massInferenceService = massInferenceService,
            interpreterFactory = interpreterFactory
        ) 
    }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Get user preferences for display settings similar to CatalogBrowser
    val userData by remember { derivedStateOf { userRepository.getUserData() } }
    val catalogDisplayMode = userData?.preferences?.catalogDisplayMode ?: CatalogDisplayMode.COMPLETE_TITLE
    val materialDisplaySettings = userData?.preferences?.materialDisplaySettings ?: MaterialDisplaySettings.DEFAULT
    
    // Add comprehensive error logging
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            Log.e("DetailScreen", "UI State Error: $error for type: $detailType, identifier: $identifier")
        }
    }
    
    LaunchedEffect(detailType, identifier) {
        Log.d("DetailScreen", "Loading details for type: $detailType, identifier: $identifier")
        try {
            viewModel.loadDetails(detailType, identifier)
        } catch (e: Exception) {
            Log.e("DetailScreen", "Failed to load details", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        when (detailType) {
                            DetailType.SCAN -> "Scan Details"
                            DetailType.TAG -> "Tag Details" 
                            DetailType.INVENTORY_STOCK -> "Inventory Stock Details"
                            DetailType.SKU -> "SKU Details"
                            DetailType.COMPONENT -> "Component Details"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
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
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary component section
                uiState.primaryComponent?.let { component ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Get visual properties from child components
                                val visualProperties = component.getAggregatedVisualProperties { childIds ->
                                    uiState.childComponents.filter { it.id in childIds }
                                }
                                
                                // Color preview box if we have color and material information
                                val colorHex = visualProperties["aggregatedColorHex"]
                                val materials = visualProperties["aggregatedMaterials"]
                                
                                // Debug logging for child components
                                Log.d("DetailScreen", "Component: ${component.name}, Category: ${component.category}")
                                Log.d("DetailScreen", "Visual properties: $visualProperties")
                                Log.d("DetailScreen", "Component metadata: ${component.metadata}")
                                Log.d("DetailScreen", "ColorHex: $colorHex, Materials: $materials")
                                
                                if (colorHex != null && materials != null) {
                                    FilamentColorBox(
                                        colorHex = colorHex,
                                        filamentType = materials,
                                        materialDisplaySettings = materialDisplaySettings,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                                
                                // Component information
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Title based on catalog display mode using visual properties
                                    val colors = visualProperties["aggregatedColors"]
                                    val displayTitle = when {
                                        colors != null && materials != null -> {
                                            when (catalogDisplayMode) {
                                                CatalogDisplayMode.COMPLETE_TITLE -> 
                                                    "$colors $materials"
                                                CatalogDisplayMode.COLOR_FOCUSED -> 
                                                    colors
                                            }
                                        }
                                        else -> visualProperties["displayName"] ?: component.name
                                    }
                                    
                                    Text(
                                        text = displayTitle,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Show original name if display name is different
                                    val originalName = component.name
                                    if (displayTitle != originalName) {
                                        Text(
                                            text = "Original: $originalName",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Properties based on display mode (similar to catalog)
                                    when (catalogDisplayMode) {
                                        CatalogDisplayMode.COMPLETE_TITLE -> {
                                            Text(
                                                text = "Category: ${component.category}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        CatalogDisplayMode.COLOR_FOCUSED -> {
                                            if (colors != null && materials != null) {
                                                Text(
                                                    text = "$materials • Category: ${component.category}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Text(
                                                    text = "Category: ${component.category}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = "Manufacturer: ${component.manufacturer}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    component.massGrams?.let { mass ->
                                        Text(
                                            text = "Mass: ${mass}g",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (component.description.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = component.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Component identifiers section
                uiState.primaryComponent?.identifiers?.takeIf { it.isNotEmpty() }?.let { identifiers ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Identifiers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                identifiers.forEach { identifier ->
                                    Text(
                                        text = "${identifier.type.name}: ${identifier.value}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Purpose: ${identifier.purpose.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (identifier != identifiers.last()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Child components section
                if (uiState.childComponents.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Child Components (${uiState.childComponents.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                uiState.childComponents.forEach { childComponent ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        onClick = {
                                            // Navigate to child component details
                                            onNavigateToDetails?.invoke(DetailType.COMPONENT, childComponent.id)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = childComponent.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Category: ${childComponent.category}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                                childComponent.massGrams?.let { mass ->
                                                    Text(
                                                        text = "Mass: ${mass}g",
                                                        style = MaterialTheme.typography.bodySmall
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "View details",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (childComponent != uiState.childComponents.last()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Component metadata section
                uiState.primaryComponent?.metadata?.takeIf { it.isNotEmpty() }?.let { metadata ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Metadata",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                metadata.forEach { (key, value) ->
                                    Text(
                                        text = "$key: $value",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPreview() {
    MaterialTheme {
        DetailScreen(
            detailType = DetailType.TAG,
            identifier = "sample-tag-id",
            onNavigateBack = {},
            onNavigateToDetails = { _, _ -> },
            onPurgeCache = {}
        )
    }
}

