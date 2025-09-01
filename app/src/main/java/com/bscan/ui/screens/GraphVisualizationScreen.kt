package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bscan.ui.components.graph.*
import com.bscan.viewmodel.GraphVisualizationViewModel
import kotlinx.coroutines.launch

/**
 * Main screen for graph visualization with controls and integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphVisualizationScreen(
    navController: NavController,
    viewModel: GraphVisualizationViewModel = viewModel()
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    
    // Observe view model state
    val layout by viewModel.graphLayout.collectAsState()
    val config by viewModel.visualizationConfig.collectAsState()
    val interactionState by viewModel.interactionState.collectAsState()
    val isSimulationRunning by viewModel.isSimulationRunning.collectAsState()
    val simulationStats by viewModel.simulationStats.collectAsState()
    val selectedEntity by viewModel.selectedEntity.collectAsState()
    
    // Create interaction handler
    val interactionHandler = remember(interactionState) {
        GraphInteractionHandler(
            interactionState = interactionState,
            onNodeTapped = { node -> 
                viewModel.onNodeTapped(node)
            },
            onNodeDragged = { node, offset ->
                viewModel.onNodeDragged(node, offset)
            },
            onNodeSelected = { node ->
                viewModel.onNodeSelected(node)
            },
            onCanvasTapped = { offset ->
                viewModel.onCanvasTapped(offset)
            },
            onZoomChanged = { scale ->
                viewModel.onZoomChanged(scale)
            },
            onPanChanged = { offset ->
                viewModel.onPanChanged(offset)
            }
        )
    }
    
    // UI State
    var showControls by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    var showEntityDetails by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main graph visualization
        GraphVisualizationCanvas(
            layout = layout,
            config = config,
            interactionState = interactionState,
            onNodeTapped = { node -> 
                viewModel.onNodeTapped(node)
                showEntityDetails = true
            },
            onNodeDragged = { node, offset ->
                viewModel.onNodeDragged(node, offset)
            },
            onCanvasTapped = { offset ->
                viewModel.onCanvasTapped(offset)
                showEntityDetails = false
            },
            onPanChanged = { dragAmount ->
                viewModel.onPanChanged(dragAmount)
            },
            onZoomChanged = { scale ->
                viewModel.onZoomChanged(scale)
            }
        )
        
        // Top app bar with controls
        if (showControls) {
            TopAppBar(
                title = { 
                    Text("Entity Graph") 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Zoom to fit button
                    IconButton(
                        onClick = {
                            val canvasSize = androidx.compose.ui.geometry.Size(
                                configuration.screenWidthDp.toFloat() * density.density,
                                configuration.screenHeightDp.toFloat() * density.density
                            )
                            interactionHandler.zoomToFit(layout, canvasSize)
                        }
                    ) {
                        Icon(Icons.Default.ZoomOutMap, contentDescription = "Zoom to Fit")
                    }
                    
                    // Reset view button
                    IconButton(
                        onClick = { interactionHandler.resetView() }
                    ) {
                        Icon(Icons.Default.CenterFocusWeak, contentDescription = "Reset View")
                    }
                    
                    // Settings button
                    IconButton(
                        onClick = { showSettings = !showSettings }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    // Hide controls button
                    IconButton(
                        onClick = { showControls = false }
                    ) {
                        Icon(Icons.Default.VisibilityOff, contentDescription = "Hide Controls")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Floating action buttons
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Show controls FAB (when hidden)
            if (!showControls) {
                FloatingActionButton(
                    onClick = { showControls = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = "Show Controls")
                }
            }
            
            // Simulation control FAB
            ExtendedFloatingActionButton(
                onClick = {
                    if (isSimulationRunning) {
                        viewModel.stopSimulation()
                    } else {
                        viewModel.startSimulation()
                    }
                },
                icon = {
                    Icon(
                        if (isSimulationRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSimulationRunning) "Stop" else "Start"
                    )
                },
                text = {
                    Text(if (isSimulationRunning) "Stop" else "Start")
                },
                containerColor = if (isSimulationRunning) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
        
        // Simulation stats overlay (bottom left)
        if (isSimulationRunning && simulationStats != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Simulation Stats",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Nodes: ${simulationStats!!.nodeCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Edges: ${simulationStats!!.edgeCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Energy: ${"%.2f".format(simulationStats!!.totalEnergy)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // Settings panel (slide in from right)
        if (showSettings) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .width(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                GraphSettingsPanel(
                    config = config,
                    onConfigChanged = { newConfig ->
                        viewModel.updateConfig(newConfig)
                    },
                    onDismiss = { showSettings = false }
                )
            }
        }
        
        // Entity details panel (slide in from left)
        if (showEntityDetails && selectedEntity != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .width(320.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                GraphEntityDetailsPanel(
                    entity = selectedEntity!!,
                    onNavigateToEntity = { entityId ->
                        navController.navigate("entity_detail/$entityId")
                        showEntityDetails = false
                    },
                    onZoomToEntity = { entityId ->
                        viewModel.zoomToEntity(entityId) { node ->
                            val canvasSize = androidx.compose.ui.geometry.Size(
                                configuration.screenWidthDp.toFloat() * density.density,
                                configuration.screenHeightDp.toFloat() * density.density
                            )
                            interactionHandler.zoomToNode(node, canvasSize)
                        }
                    },
                    onDismiss = { showEntityDetails = false }
                )
            }
        }
    }
    
    // Initialize graph data when screen loads
    LaunchedEffect(Unit) {
        viewModel.loadGraphData()
    }
    
    // Center graph on nodes when layout is ready and screen size is known
    LaunchedEffect(layout) {
        if (layout.nodes.isNotEmpty()) {
            val canvasSize = androidx.compose.ui.geometry.Size(
                configuration.screenWidthDp.toFloat() * density.density,
                configuration.screenHeightDp.toFloat() * density.density
            )
            viewModel.centerGraphOnNodes(canvasSize)
        }
    }
}

/**
 * Settings panel for graph visualization configuration
 */
@Composable
private fun GraphSettingsPanel(
    config: GraphVisualizationConfig,
    onConfigChanged: (GraphVisualizationConfig) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Display Settings",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show labels toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Labels")
            Switch(
                checked = config.showNodeLabels,
                onCheckedChange = { 
                    onConfigChanged(config.copy(showNodeLabels = it))
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show clusters toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Clusters")
            Switch(
                checked = config.showClusters,
                onCheckedChange = { 
                    onConfigChanged(config.copy(showClusters = it))
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show directions toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Edge Directions")
            Switch(
                checked = true,
                onCheckedChange = { 
                    /* No-op for now */
                }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show legend toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Show Legend")
            Switch(
                checked = config.showLegend,
                onCheckedChange = { 
                    onConfigChanged(config.copy(showLegend = it))
                }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Node size slider
        Text(
            text = "Node Size: ${(config.baseNodeSize).toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.baseNodeSize,
            onValueChange = { 
                onConfigChanged(config.copy(baseNodeSize = it))
            },
            valueRange = 15f..50f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Line width slider
        Text(
            text = "Line Width: ${(config.baseLineWidth).toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.baseLineWidth,
            onValueChange = { 
                onConfigChanged(config.copy(baseLineWidth = it))
            },
            valueRange = 1f..8f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Cluster padding slider
        Text(
            text = "Cluster Padding: ${(config.clusterPadding).toInt()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = config.clusterPadding,
            onValueChange = { 
                onConfigChanged(config.copy(clusterPadding = it))
            },
            valueRange = 20f..80f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Entity details panel showing information about selected entity
 */
@Composable
private fun GraphEntityDetailsPanel(
    entity: com.bscan.model.graph.Entity,
    onNavigateToEntity: (String) -> Unit,
    onZoomToEntity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entity::class.simpleName ?: "Entity",
                style = MaterialTheme.typography.headlineSmall
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Entity ID
        Text(
            text = "ID: ${entity.id.take(8)}...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Key properties
        val displayName = entity.getProperty<String>("name") 
            ?: entity.getProperty<String>("label")
            ?: entity.getProperty<String>("material")
            ?: "Unnamed"
            
        Text(
            text = displayName,
            style = MaterialTheme.typography.titleMedium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Show a few key properties
        val keyProperties = listOf("material", "color", "weight", "quantity", "location")
        keyProperties.forEach { key ->
            entity.getProperty<Any>(key)?.let { value ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onZoomToEntity(entity.id) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Focus")
            }
            
            Button(
                onClick = { onNavigateToEntity(entity.id) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Details")
            }
        }
    }
}