package com.bscan.ui.components.graph

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.bscan.model.graph.Entity
import com.bscan.model.graph.RelationshipTypes

/**
 * Graph node representation with position and visual properties for rendering
 */
data class GraphNode(
    val id: String,
    val entity: Entity,
    var position: Offset = Offset.Zero,
    var velocity: Offset = Offset.Zero,
    val clusterId: String? = null
) {
    /**
     * Node radius based on entity type importance
     */
    val radius: Float = when (entity.entityType) {
        "virtual" -> 40f          // Larger for virtual containers (trays)
        "physical_component" -> 35f
        "inventory_item" -> 35f
        "stock_definition" -> 30f
        "identifier" -> 25f
        "activity" -> 20f
        "location" -> 30f
        "person" -> 25f
        "information" -> 20f
        else -> 25f
    }
    
    /**
     * Node color based on entity type
     */
    val color: Color
        get() = when (entity.entityType) {
            "virtual" -> Color(0xFF4CAF50)          // Green for containers
            "physical_component" -> Color(0xFF2196F3)  // Blue for physical items
            "inventory_item" -> Color(0xFFFF9800)      // Orange for inventory
            "stock_definition" -> Color(0xFF9C27B0)    // Purple for definitions
            "identifier" -> Color(0xFF00BCD4)         // Cyan for identifiers
            "activity" -> Color(0xFFFFC107)          // Amber for activities
            "location" -> Color(0xFF795548)          // Brown for locations
            "person" -> Color(0xFFE91E63)           // Pink for people
            "information" -> Color(0xFF607D8B)       // Blue grey for information
            else -> Color(0xFF9E9E9E)              // Grey default
        }
    
    /**
     * Icon representation for entity type
     */
    val icon: String
        get() = when (entity.entityType) {
            "virtual" -> "📦"
            "physical_component" -> "🔧"
            "inventory_item" -> "📊"
            "stock_definition" -> "📋"
            "identifier" -> "🏷️"
            "activity" -> "⚡"
            "location" -> "📍"
            "person" -> "👤"
            "information" -> "ℹ️"
            else -> "⭕"
        }
    
    /**
     * Short display label
     */
    val displayLabel: String
        get() = if (entity.label.length > 15) {
            entity.label.take(12) + "..."
        } else {
            entity.label
        }
}

/**
 * Graph edge with visual properties based on relationship type
 */
data class GraphEdge(
    val id: String,
    val fromNodeId: String,
    val toNodeId: String,
    val relationshipType: String,
    val directional: Boolean,
    val properties: Map<String, String> = emptyMap()
) {
    /**
     * Edge thickness based on relationship importance
     */
    val strokeWidth: Float = when (relationshipType) {
        RelationshipTypes.CONTAINS -> 4f           // Thick for containment
        RelationshipTypes.IDENTIFIED_BY -> 3f      // Medium for identification
        RelationshipTypes.TRACKS -> 3f            // Medium for inventory tracking
        RelationshipTypes.DEFINED_BY -> 2.5f      // Medium for definitions
        RelationshipTypes.ATTACHED_TO -> 2f       // Thin for attachments
        RelationshipTypes.LOCATED_AT -> 2f        // Thin for location
        else -> 1.5f                             // Default thin
    }
    
    /**
     * Edge color based on relationship type
     */
    val color: Color = when (relationshipType) {
        RelationshipTypes.CONTAINS -> Color(0xFF4CAF50)      // Green containment
        RelationshipTypes.IDENTIFIED_BY -> Color(0xFF2196F3) // Blue identification
        RelationshipTypes.TRACKS -> Color(0xFFFF9800)        // Orange tracking
        RelationshipTypes.DEFINED_BY -> Color(0xFF9C27B0)    // Purple definition
        RelationshipTypes.ATTACHED_TO -> Color(0xFF00BCD4)   // Cyan attachment
        RelationshipTypes.LOCATED_AT -> Color(0xFF795548)    // Brown location
        RelationshipTypes.RELATED_TO -> Color(0xFFFFC107)    // Amber relation
        else -> Color(0xFF9E9E9E)                           // Grey default
    }
    
    /**
     * Dash pattern for certain relationship types
     */
    val dashPattern: FloatArray? = when (relationshipType) {
        RelationshipTypes.RELATED_TO -> floatArrayOf(10f, 5f)       // Dashed for general relations
        RelationshipTypes.COMPATIBLE_WITH -> floatArrayOf(5f, 5f)   // Short dash for compatibility
        RelationshipTypes.SAME_AS -> floatArrayOf(15f, 5f)         // Long dash for equivalence
        else -> null                                               // Solid line default
    }
    
    /**
     * Alpha transparency based on relationship strength
     */
    val alpha: Float = when (relationshipType) {
        RelationshipTypes.CONTAINS, 
        RelationshipTypes.IDENTIFIED_BY, 
        RelationshipTypes.TRACKS -> 0.9f           // High opacity for strong relationships
        RelationshipTypes.DEFINED_BY,
        RelationshipTypes.ATTACHED_TO -> 0.7f      // Medium opacity
        else -> 0.5f                              // Low opacity for weak relationships
    }
}

/**
 * Graph cluster for grouping related nodes visually
 */
data class GraphCluster(
    val id: String,
    val nodeIds: Set<String>,
    val label: String,
    val color: Color,
    val clusterType: ClusterType,
    var bounds: androidx.compose.ui.geometry.Rect? = null,
    var isCollapsed: Boolean = false
) {
    enum class ClusterType {
        CONTAINMENT,        // Based on CONTAINS relationships (tray → filament → core)
        IDENTIFICATION,     // Based on shared identifiers
        INVENTORY,          // Based on inventory tracking relationships
        ACTIVITY,           // Based on shared activities
        LOCATION,           // Based on location proximity
        CONNECTIVITY        // Based on high connectivity
    }
}

/**
 * Complete graph layout with positioned nodes and edges
 */
data class GraphLayout(
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val clusters: List<GraphCluster> = emptyList(),
    val bounds: androidx.compose.ui.geometry.Rect
) {
    private val nodeMap = nodes.associateBy { it.id }
    private val edgeMap = edges.associateBy { it.id }
    
    /**
     * Find node at specific screen position within tolerance
     */
    fun findNodeAt(offset: Offset, tolerance: Float): GraphNode? {
        return nodes.firstOrNull { node ->
            val distance = (node.position - offset).getDistance()
            distance <= node.radius + tolerance
        }
    }
    
    /**
     * Get node by ID
     */
    fun getNode(id: String): GraphNode? = nodeMap[id]
    
    /**
     * Get edge by ID
     */
    fun getEdge(id: String): GraphEdge? = edgeMap[id]
    
    /**
     * Get all edges connected to a node
     */
    fun getConnectedEdges(nodeId: String): List<GraphEdge> {
        return edges.filter { 
            it.fromNodeId == nodeId || it.toNodeId == nodeId 
        }
    }
    
    /**
     * Get nodes within a specific cluster
     */
    fun getClusterNodes(clusterId: String): List<GraphNode> {
        val cluster = clusters.firstOrNull { it.id == clusterId } ?: return emptyList()
        return nodes.filter { it.id in cluster.nodeIds }
    }
    
    /**
     * Check if layout is empty
     */
    val isEmpty: Boolean
        get() = nodes.isEmpty()
    
    /**
     * Get statistics about the layout
     */
    val statistics: GraphLayoutStatistics
        get() = GraphLayoutStatistics(
            nodeCount = nodes.size,
            edgeCount = edges.size,
            clusterCount = clusters.size,
            entityTypes = nodes.groupBy { it.entity.entityType }.mapValues { it.value.size },
            relationshipTypes = edges.groupBy { it.relationshipType }.mapValues { it.value.size }
        )
}

/**
 * Graph layout computation statistics
 */
data class GraphLayoutStatistics(
    val nodeCount: Int,
    val edgeCount: Int,
    val clusterCount: Int,
    val entityTypes: Map<String, Int>,
    val relationshipTypes: Map<String, Int>
)

/**
 * Configuration for graph visualization appearance
 */
data class GraphVisualizationConfig(
    val showClusters: Boolean = true,
    val showEdgeLabels: Boolean = false,
    val showNodeLabels: Boolean = true,
    val showLegend: Boolean = true,
    val clusterPadding: Float = 20f,
    val baseNodeSize: Float = 25f,
    val baseLineWidth: Float = 2f,
    val minimumNodeDistance: Float = 60f,
    val edgeCurvature: Float = 0.1f,
    val animationDuration: Int = 300,
    val maxVisibleNodes: Int = 500,
    val levelOfDetailThreshold: Float = 0.5f
)

/**
 * Graph interaction state
 */
data class GraphInteractionState(
    val selectedNodeId: String? = null,
    val hoveredNodeId: String? = null,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val isDragging: Boolean = false,
    val collapsedClusters: Set<String> = emptySet()
) {
    fun withSelectedNode(nodeId: String?): GraphInteractionState {
        return copy(selectedNodeId = nodeId)
    }
    
    fun withScale(newScale: Float): GraphInteractionState {
        return copy(scale = newScale.coerceIn(0.1f, 5f))
    }
    
    fun withOffset(newOffset: Offset): GraphInteractionState {
        return copy(offset = newOffset)
    }
    
    fun withCollapsedCluster(clusterId: String, collapsed: Boolean): GraphInteractionState {
        return copy(
            collapsedClusters = if (collapsed) {
                collapsedClusters + clusterId
            } else {
                collapsedClusters - clusterId
            }
        )
    }
}