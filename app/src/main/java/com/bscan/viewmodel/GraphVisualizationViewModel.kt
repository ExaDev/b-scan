package com.bscan.viewmodel

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphRepository
import com.bscan.ui.components.graph.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for managing graph visualization state and operations
 */
class GraphVisualizationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val graphRepository = GraphRepository(application)
    
    // Layout engine and clustering
    private val layoutEngine = GraphLayoutEngine()
    private val clusteringEngine = GraphClusteringEngine()
    
    // Core state flows
    private val _graphLayout = MutableStateFlow(GraphLayout(
        nodes = emptyList(),
        edges = emptyList(),
        bounds = androidx.compose.ui.geometry.Rect.Zero
    ))
    val graphLayout: StateFlow<GraphLayout> = _graphLayout.asStateFlow()
    
    private val _visualizationConfig = MutableStateFlow(GraphVisualizationConfig())
    val visualizationConfig: StateFlow<GraphVisualizationConfig> = _visualizationConfig.asStateFlow()
    
    private val _interactionState = MutableStateFlow(GraphInteractionState())
    val interactionState: StateFlow<GraphInteractionState> = _interactionState.asStateFlow()
    
    private val _isSimulationRunning = MutableStateFlow(false)
    val isSimulationRunning: StateFlow<Boolean> = _isSimulationRunning.asStateFlow()
    
    private val _simulationStats = MutableStateFlow<SimulationStats?>(null)
    val simulationStats: StateFlow<SimulationStats?> = _simulationStats.asStateFlow()
    
    private val _selectedEntity = MutableStateFlow<Entity?>(null)
    val selectedEntity: StateFlow<Entity?> = _selectedEntity.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load graph data from repository and initialize visualization
     */
    fun loadGraphData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                withContext(Dispatchers.IO) {
                    // Load entities and edges from repository
                    val entities = graphRepository.getAllEntities().toList()
                    val edges = graphRepository.getAllEdges().toList()
                    
                    withContext(Dispatchers.Default) {
                        // Convert to graph visualization format
                        val graphNodes = entities.map { entity ->
                            GraphNode(
                                id = entity.id,
                                entity = entity,
                                clusterId = null // Will be assigned during clustering
                            )
                        }
                        
                        val graphEdges = edges.map { edge ->
                            GraphEdge(
                                id = "${edge.fromEntityId}_${edge.toEntityId}_${edge.relationshipType}",
                                fromNodeId = edge.fromEntityId,
                                toNodeId = edge.toEntityId,
                                relationshipType = edge.relationshipType,
                                directional = isDirectedRelationship(edge.relationshipType)
                            )
                        }
                        
                        // Perform clustering
                        val clusters = clusteringEngine.clusterGraph(entities, edges)
                        
                        // Create cluster ID mapping from cluster nodeIds
                        val nodeClusterMap = mutableMapOf<String, String>()
                        clusters.forEach { cluster ->
                            cluster.nodeIds.forEach { nodeId ->
                                nodeClusterMap[nodeId] = cluster.id
                            }
                        }
                        
                        // Recreate nodes with cluster IDs assigned
                        val clusteredNodes = graphNodes.map { node ->
                            node.copy(clusterId = nodeClusterMap[node.id])
                        }
                        
                        // Create layout
                        val layout = GraphLayout(
                            nodes = clusteredNodes,
                            edges = graphEdges,
                            clusters = clusters,
                            bounds = androidx.compose.ui.geometry.Rect.Zero
                        )
                        
                        // Initialize node positions
                        layoutEngine.initializeLayout(layout)
                        
                        // Initial centering will be done by the Screen after first composition
                        
                        withContext(Dispatchers.Main) {
                            _graphLayout.value = layout
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to load graph data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start physics simulation for layout
     */
    fun startSimulation() {
        if (_isSimulationRunning.value) return
        
        _isSimulationRunning.value = true
        
        layoutEngine.startSimulation(
            layout = _graphLayout.value,
            onUpdate = { updatedLayout ->
                _graphLayout.value = updatedLayout
                _simulationStats.value = layoutEngine.getSimulationStats(updatedLayout)
            },
            scope = viewModelScope
        )
    }
    
    /**
     * Stop physics simulation
     */
    fun stopSimulation() {
        layoutEngine.stopSimulation()
        _isSimulationRunning.value = false
        _simulationStats.value = null
    }
    
    /**
     * Handle node tap events
     */
    fun onNodeTapped(node: GraphNode) {
        _selectedEntity.value = node.entity
    }
    
    /**
     * Handle node drag events
     */
    fun onNodeDragged(node: GraphNode, dragAmount: Offset) {
        // Update node position directly (mutable)
        node.position = node.position + dragAmount
        
        // Trigger UI update by updating the layout reference
        val currentLayout = _graphLayout.value
        _graphLayout.value = currentLayout.copy()
    }
    
    /**
     * Handle node selection events
     */
    fun onNodeSelected(node: GraphNode?) {
        _selectedEntity.value = node?.entity
        _interactionState.value = _interactionState.value.copy(selectedNodeId = node?.id)
    }
    
    /**
     * Handle canvas tap events (empty space)
     */
    fun onCanvasTapped(position: Offset) {
        _selectedEntity.value = null
        _interactionState.value = _interactionState.value.copy(selectedNodeId = null)
    }
    
    /**
     * Handle zoom changes
     */
    fun onZoomChanged(scale: Float) {
        val currentState = _interactionState.value
        _interactionState.value = currentState.copy(scale = scale)
    }
    
    /**
     * Handle pan changes
     */
    fun onPanChanged(dragAmount: Offset) {
        val currentState = _interactionState.value
        _interactionState.value = currentState.copy(offset = currentState.offset + dragAmount)
    }
    
    /**
     * Update visualization configuration
     */
    fun updateConfig(newConfig: GraphVisualizationConfig) {
        _visualizationConfig.value = newConfig
    }
    
    /**
     * Zoom to specific entity by ID
     */
    fun zoomToEntity(entityId: String, onZoom: (GraphNode) -> Unit) {
        val node = _graphLayout.value.nodes.find { it.id == entityId }
        if (node != null) {
            onZoom(node)
            _selectedEntity.value = node.entity
            _interactionState.value = _interactionState.value.copy(selectedNodeId = node.id)
        }
    }
    
    /**
     * Add new entity to the graph
     */
    fun addEntity(entity: Entity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                graphRepository.addEntity(entity)
            }
            
            // Reload graph to include new entity
            loadGraphData()
        }
    }
    
    /**
     * Remove entity from the graph
     */
    fun removeEntity(entityId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                graphRepository.removeEntity(entityId)
            }
            
            // Reload graph to reflect removal
            loadGraphData()
        }
    }
    
    /**
     * Filter graph by entity types
     */
    fun filterByEntityTypes(entityTypes: Set<String>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allEntities = graphRepository.getAllEntities().toList()
                val allEdges = graphRepository.getAllEdges().toList()
                
                // Filter entities by type
                val filteredEntities = if (entityTypes.isEmpty()) {
                    allEntities
                } else {
                    allEntities.filter { entity ->
                        entity::class.simpleName in entityTypes
                    }
                }
                
                val entityIds = filteredEntities.map { it.id }.toSet()
                
                // Filter edges to only include those between filtered entities
                val filteredEdges = allEdges.filter { edge ->
                    edge.fromEntityId in entityIds && edge.toEntityId in entityIds
                }
                
                withContext(Dispatchers.Default) {
                    // Create filtered graph layout
                    val graphNodes = filteredEntities.map { entity ->
                        GraphNode(
                            id = entity.id,
                            entity = entity,
                            clusterId = null
                        )
                    }
                    
                    val graphEdges = filteredEdges.map { edge ->
                        GraphEdge(
                            id = "${edge.fromEntityId}_${edge.toEntityId}_${edge.relationshipType}",
                            fromNodeId = edge.fromEntityId,
                            toNodeId = edge.toEntityId,
                            relationshipType = edge.relationshipType,
                            directional = isDirectedRelationship(edge.relationshipType)
                        )
                    }
                    
                    // Perform clustering on filtered data
                    val clusters = clusteringEngine.clusterGraph(filteredEntities, filteredEdges)
                    
                    val layout = GraphLayout(
                        nodes = graphNodes,
                        edges = graphEdges,
                        clusters = clusters,
                        bounds = androidx.compose.ui.geometry.Rect.Zero
                    )
                    
                    layoutEngine.initializeLayout(layout)
                    
                    withContext(Dispatchers.Main) {
                        _graphLayout.value = layout
                    }
                }
            }
        }
    }
    
    /**
     * Search for entities by text query
     */
    fun searchEntities(query: String): List<Entity> {
        val currentLayout = _graphLayout.value
        return currentLayout.nodes
            .map { it.entity }
            .filter { entity ->
                val searchableText = listOfNotNull(
                    entity.getProperty<String>("name"),
                    entity.getProperty<String>("label"),
                    entity.getProperty<String>("material"),
                    entity::class.simpleName
                ).joinToString(" ").lowercase()
                
                searchableText.contains(query.lowercase())
            }
    }
    
    /**
     * Get entity statistics
     */
    fun getEntityStatistics(): Map<String, Int> {
        val entities = _graphLayout.value.nodes.map { it.entity }
        return entities.groupingBy { it::class.simpleName ?: "Unknown" }.eachCount()
    }
    
    /**
     * Calculate edge weight based on relationship type
     */
    private fun calculateEdgeWeight(edge: com.bscan.model.graph.Edge): Float {
        return when (edge.relationshipType) {
            "IDENTIFIED_BY" -> 1.5f
            "CONTAINS" -> 1.2f
            "TRACKS" -> 1.0f
            "HAD_MOVEMENT" -> 0.8f
            "STORED_AT" -> 0.6f
            else -> 1.0f
        }
    }
    
    /**
     * Determine if relationship is directional
     */
    private fun isDirectedRelationship(relationshipType: String): Boolean {
        return when (relationshipType) {
            "IDENTIFIED_BY", "CONTAINS", "TRACKS", "HAD_MOVEMENT", "STORED_AT" -> true
            else -> false
        }
    }
    
    /**
     * Determine if relationship should be rendered with dashed line
     */
    private fun isDashedRelationship(relationshipType: String): Boolean {
        return when (relationshipType) {
            "HAD_MOVEMENT", "INFERRED_FROM" -> true
            else -> false
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Refresh graph data
     */
    fun refreshGraph() {
        stopSimulation()
        loadGraphData()
    }
    
    /**
     * Center the graph view on the nodes with proper viewport size
     */
    fun centerGraphOnNodes(viewportSize: androidx.compose.ui.geometry.Size) {
        val currentLayout = _graphLayout.value
        if (currentLayout.nodes.isNotEmpty()) {
            val nodesBounds = calculateNodesBounds(currentLayout.nodes)
            val nodesCenter = Offset(
                (nodesBounds.left + nodesBounds.right) / 2,
                (nodesBounds.top + nodesBounds.bottom) / 2
            )
            
            val viewportCenter = Offset(viewportSize.width / 2, viewportSize.height / 2)
            val offsetToCenter = viewportCenter - nodesCenter
            
            _interactionState.value = _interactionState.value.copy(offset = offsetToCenter)
        }
    }
    
    /**
     * Calculate the bounding rectangle that contains all nodes
     */
    private fun calculateNodesBounds(nodes: List<GraphNode>): androidx.compose.ui.geometry.Rect {
        if (nodes.isEmpty()) return androidx.compose.ui.geometry.Rect.Zero
        
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        
        nodes.forEach { node ->
            minX = minOf(minX, node.position.x - node.radius)
            minY = minOf(minY, node.position.y - node.radius)
            maxX = maxOf(maxX, node.position.x + node.radius)
            maxY = maxOf(maxY, node.position.y + node.radius)
        }
        
        return androidx.compose.ui.geometry.Rect(
            left = minX,
            top = minY,
            right = maxX,
            bottom = maxY
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        stopSimulation()
    }
}