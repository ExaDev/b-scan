package com.bscan.ui.components.graph

import androidx.compose.ui.graphics.Color
import com.bscan.model.graph.Entity
import com.bscan.model.graph.Edge
import com.bscan.model.graph.entities.*
import kotlin.math.*
import kotlin.random.Random

/**
 * Hierarchical clustering engine for graph visualization.
 * 
 * Groups related entities into clusters based on:
 * - Entity type similarity
 * - Relationship connectivity
 * - Domain-specific grouping rules
 * - Graph structure analysis
 */
class GraphClusteringEngine {
    
    companion object {
        // Clustering parameters
        private const val MAX_CLUSTER_SIZE = 8
        private const val MIN_CLUSTER_SIZE = 2
        private const val CONNECTIVITY_THRESHOLD = 0.3f
        private const val TYPE_SIMILARITY_WEIGHT = 0.4f
        private const val STRUCTURAL_SIMILARITY_WEIGHT = 0.6f
        
        // Color palette for clusters
        private val CLUSTER_COLORS = listOf(
            Color(0xFF2196F3), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFF9800), // Orange
            Color(0xFF9C27B0), // Purple
            Color(0xFFF44336), // Red
            Color(0xFF00BCD4), // Cyan
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF795548), // Brown
            Color(0xFF607D8B), // Blue Grey
            Color(0xFFE91E63), // Pink
            Color(0xFF3F51B5), // Indigo
            Color(0xFF009688)  // Teal
        )
    }
    
    /**
     * Perform hierarchical clustering on the entity graph
     */
    fun clusterGraph(
        entities: List<Entity>,
        edges: List<Edge>
    ): List<GraphCluster> {
        if (entities.size < MIN_CLUSTER_SIZE) {
            return emptyList()
        }
        
        // Build adjacency information
        val adjacencyMap = buildAdjacencyMap(entities, edges)
        
        // Calculate similarity matrix
        val similarities = calculateSimilarityMatrix(entities, adjacencyMap)
        
        // Perform agglomerative clustering
        val clusters = performAgglomerativeClustering(entities, similarities)
        
        // Convert to GraphCluster objects
        return clusters.mapIndexed { index, entityList ->
            createGraphCluster(index, entityList, adjacencyMap)
        }
    }
    
    /**
     * Build adjacency map for connectivity analysis
     */
    private fun buildAdjacencyMap(entities: List<Entity>, edges: List<Edge>): Map<String, Set<String>> {
        val adjacencyMap = mutableMapOf<String, MutableSet<String>>()
        
        // Initialize with all entities
        entities.forEach { entity ->
            adjacencyMap[entity.id] = mutableSetOf()
        }
        
        // Add connections from edges
        edges.forEach { edge ->
            adjacencyMap[edge.fromEntityId]?.add(edge.toEntityId)
            adjacencyMap[edge.toEntityId]?.add(edge.fromEntityId)
        }
        
        return adjacencyMap
    }
    
    /**
     * Calculate similarity matrix between all entity pairs
     */
    private fun calculateSimilarityMatrix(
        entities: List<Entity>,
        adjacencyMap: Map<String, Set<String>>
    ): Array<FloatArray> {
        val size = entities.size
        val similarities = Array(size) { FloatArray(size) }
        
        for (i in entities.indices) {
            for (j in i until entities.size) {
                val similarity = if (i == j) {
                    1.0f
                } else {
                    calculateEntitySimilarity(
                        entities[i], entities[j], 
                        adjacencyMap[entities[i].id] ?: emptySet(),
                        adjacencyMap[entities[j].id] ?: emptySet()
                    )
                }
                
                similarities[i][j] = similarity
                similarities[j][i] = similarity
            }
        }
        
        return similarities
    }
    
    /**
     * Calculate similarity between two entities
     */
    private fun calculateEntitySimilarity(
        entity1: Entity,
        entity2: Entity,
        neighbors1: Set<String>,
        neighbors2: Set<String>
    ): Float {
        // Type similarity
        val typeSimilarity = calculateTypeSimilarity(entity1, entity2)
        
        // Structural similarity (common neighbors)
        val structuralSimilarity = calculateStructuralSimilarity(neighbors1, neighbors2)
        
        // Domain-specific similarity
        val domainSimilarity = calculateDomainSimilarity(entity1, entity2)
        
        return (typeSimilarity * TYPE_SIMILARITY_WEIGHT + 
                structuralSimilarity * STRUCTURAL_SIMILARITY_WEIGHT + 
                domainSimilarity * (1 - TYPE_SIMILARITY_WEIGHT - STRUCTURAL_SIMILARITY_WEIGHT))
    }
    
    /**
     * Calculate similarity based on entity types
     */
    private fun calculateTypeSimilarity(entity1: Entity, entity2: Entity): Float {
        return when {
            // Exact type match
            entity1::class == entity2::class -> 1.0f
            
            // Related types
            isRelatedType(entity1, entity2) -> 0.7f
            
            // Same category
            isSameCategory(entity1, entity2) -> 0.5f
            
            // Different types
            else -> 0.1f
        }
    }
    
    /**
     * Check if entity types are related
     */
    private fun isRelatedType(entity1: Entity, entity2: Entity): Boolean {
        val relatedPairs = setOf(
            setOf(PhysicalComponent::class, InventoryItem::class),
            setOf(Activity::class, Information::class),
            setOf(Identifier::class, Information::class)
        )
        
        val entityTypes = setOf(entity1::class, entity2::class)
        return relatedPairs.any { it == entityTypes }
    }
    
    /**
     * Check if entities belong to same category
     */
    private fun isSameCategory(entity1: Entity, entity2: Entity): Boolean {
        val categories = mapOf(
            "physical" to setOf(PhysicalComponent::class, Location::class),
            "data" to setOf(Information::class, Identifier::class),
            "tracking" to setOf(InventoryItem::class, Activity::class),
            "abstract" to setOf(Virtual::class, Person::class)
        )
        
        return categories.values.any { category ->
            entity1::class in category && entity2::class in category
        }
    }
    
    /**
     * Calculate structural similarity based on common neighbors
     */
    private fun calculateStructuralSimilarity(neighbors1: Set<String>, neighbors2: Set<String>): Float {
        if (neighbors1.isEmpty() && neighbors2.isEmpty()) return 1.0f
        if (neighbors1.isEmpty() || neighbors2.isEmpty()) return 0.0f
        
        val intersection = neighbors1.intersect(neighbors2).size
        val union = neighbors1.union(neighbors2).size
        
        return intersection.toFloat() / union.toFloat() // Jaccard similarity
    }
    
    /**
     * Calculate domain-specific similarity
     */
    private fun calculateDomainSimilarity(entity1: Entity, entity2: Entity): Float {
        // Material-based grouping for physical components
        if (entity1 is PhysicalComponent && entity2 is PhysicalComponent) {
            val material1 = entity1.getProperty<String>("material") ?: ""
            val material2 = entity2.getProperty<String>("material") ?: ""
            if (material1.isNotEmpty() && material1 == material2) {
                return 1.0f
            }
            
            val color1 = entity1.getProperty<String>("color") ?: ""
            val color2 = entity2.getProperty<String>("color") ?: ""
            if (color1.isNotEmpty() && color1 == color2) {
                return 0.8f
            }
        }
        
        // Activity time-based grouping
        if (entity1 is Activity && entity2 is Activity) {
            val time1 = entity1.getProperty<Long>("timestamp") ?: 0L
            val time2 = entity2.getProperty<Long>("timestamp") ?: 0L
            val timeDiff = abs(time1 - time2)
            
            // Group activities within 1 hour
            if (timeDiff < 3600000) { // 1 hour in milliseconds
                return 0.9f
            }
        }
        
        // Location-based grouping
        val location1 = entity1.getProperty<String>("location") ?: ""
        val location2 = entity2.getProperty<String>("location") ?: ""
        if (location1.isNotEmpty() && location1 == location2) {
            return 0.8f
        }
        
        return 0.0f
    }
    
    /**
     * Perform agglomerative clustering using average linkage
     */
    private fun performAgglomerativeClustering(
        entities: List<Entity>,
        similarities: Array<FloatArray>
    ): List<List<Entity>> {
        // Initialize each entity as its own cluster
        val clusters = mutableListOf<MutableList<Entity>>()
        entities.forEach { entity ->
            clusters.add(mutableListOf(entity))
        }
        
        // Merge clusters until convergence
        while (clusters.size > 1) {
            var maxSimilarity = -1.0f
            var mergeI = -1
            var mergeJ = -1
            
            // Find most similar clusters to merge
            for (i in clusters.indices) {
                for (j in i + 1 until clusters.size) {
                    val similarity = calculateClusterSimilarity(
                        clusters[i], clusters[j], entities, similarities
                    )
                    
                    if (similarity > maxSimilarity && 
                        clusters[i].size + clusters[j].size <= MAX_CLUSTER_SIZE) {
                        maxSimilarity = similarity
                        mergeI = i
                        mergeJ = j
                    }
                }
            }
            
            // Stop if no good merges found
            if (maxSimilarity < CONNECTIVITY_THRESHOLD) {
                break
            }
            
            // Merge clusters
            if (mergeI != -1 && mergeJ != -1) {
                clusters[mergeI].addAll(clusters[mergeJ])
                clusters.removeAt(mergeJ) // Remove later index first
            } else {
                break
            }
        }
        
        // Filter out single-entity clusters
        return clusters.filter { it.size >= MIN_CLUSTER_SIZE }
    }
    
    /**
     * Calculate similarity between two clusters using average linkage
     */
    private fun calculateClusterSimilarity(
        cluster1: List<Entity>,
        cluster2: List<Entity>,
        allEntities: List<Entity>,
        similarities: Array<FloatArray>
    ): Float {
        var totalSimilarity = 0.0f
        var count = 0
        
        cluster1.forEach { entity1 ->
            cluster2.forEach { entity2 ->
                val index1 = allEntities.indexOf(entity1)
                val index2 = allEntities.indexOf(entity2)
                
                if (index1 != -1 && index2 != -1) {
                    totalSimilarity += similarities[index1][index2]
                    count++
                }
            }
        }
        
        return if (count > 0) totalSimilarity / count else 0.0f
    }
    
    /**
     * Create a GraphCluster from a list of entities
     */
    private fun createGraphCluster(
        index: Int,
        entities: List<Entity>,
        adjacencyMap: Map<String, Set<String>>
    ): GraphCluster {
        val clusterId = "cluster_${index}"
        val color = CLUSTER_COLORS[index % CLUSTER_COLORS.size]
        
        // Determine cluster type based on dominant entity type
        val typeCount = entities.groupingBy { it::class.simpleName }.eachCount()
        val dominantType = typeCount.maxByOrNull { it.value }?.key ?: "Mixed"
        
        // Generate cluster label
        val label = when {
            entities.size <= 3 -> entities.joinToString(" + ") { getEntityDisplayName(it) }
            dominantType != "Mixed" -> "$dominantType Cluster (${entities.size})"
            else -> "Mixed Cluster (${entities.size})"
        }
        
        // Calculate internal connectivity
        val internalEdges = countInternalEdges(entities, adjacencyMap)
        val maxPossibleEdges = entities.size * (entities.size - 1) / 2
        val connectivity = if (maxPossibleEdges > 0) {
            internalEdges.toFloat() / maxPossibleEdges.toFloat()
        } else {
            0.0f
        }
        
        return GraphCluster(
            id = clusterId,
            nodeIds = entities.map { it.id }.toSet(),
            label = label,
            color = color,
            clusterType = GraphCluster.ClusterType.CONNECTIVITY
        )
    }
    
    /**
     * Get display name for an entity
     */
    private fun getEntityDisplayName(entity: Entity): String {
        return entity.getProperty<String>("name") 
            ?: entity.getProperty<String>("label")
            ?: entity.getProperty<String>("material")
            ?: entity::class.simpleName?.take(8) 
            ?: "Entity"
    }
    
    /**
     * Count edges within a cluster
     */
    private fun countInternalEdges(entities: List<Entity>, adjacencyMap: Map<String, Set<String>>): Int {
        val entityIds = entities.map { it.id }.toSet()
        var internalEdges = 0
        
        entities.forEach { entity ->
            val neighbors = adjacencyMap[entity.id] ?: emptySet()
            internalEdges += neighbors.count { it in entityIds }
        }
        
        return internalEdges / 2 // Each edge counted twice
    }
    
    /**
     * Calculate optimal cluster layout positions
     */
    fun calculateClusterPositions(
        clusters: List<GraphCluster>,
        canvasWidth: Float,
        canvasHeight: Float
    ): Map<String, androidx.compose.ui.geometry.Offset> {
        val positions = mutableMapOf<String, androidx.compose.ui.geometry.Offset>()
        
        when (clusters.size) {
            0 -> return emptyMap()
            1 -> {
                positions[clusters[0].id] = androidx.compose.ui.geometry.Offset(
                    canvasWidth / 2, canvasHeight / 2
                )
            }
            else -> {
                // Arrange clusters in a circle
                clusters.forEachIndexed { index, cluster ->
                    val angle = 2 * PI * index / clusters.size
                    val radius = minOf(canvasWidth, canvasHeight) * 0.3f
                    val centerX = canvasWidth / 2
                    val centerY = canvasHeight / 2
                    
                    val x = centerX + cos(angle).toFloat() * radius
                    val y = centerY + sin(angle).toFloat() * radius
                    
                    positions[cluster.id] = androidx.compose.ui.geometry.Offset(x, y)
                }
            }
        }
        
        return positions
    }
}