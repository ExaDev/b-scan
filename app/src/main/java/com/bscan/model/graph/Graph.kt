package com.bscan.model.graph

/**
 * In-memory graph structure for entities and relationships.
 * Provides efficient querying and traversal capabilities.
 */
class Graph {
    
    private val entities = mutableMapOf<String, Entity>()
    private val edges = mutableMapOf<String, Edge>()
    
    // Adjacency lists for efficient traversal
    private val outgoingEdges = mutableMapOf<String, MutableSet<String>>()  // entityId -> edgeIds
    private val incomingEdges = mutableMapOf<String, MutableSet<String>>()  // entityId -> edgeIds
    private val entityTypes = mutableMapOf<String, MutableSet<String>>()    // type -> entityIds
    private val relationshipTypes = mutableMapOf<String, MutableSet<String>>() // type -> edgeIds
    
    /**
     * Add an entity to the graph
     */
    fun addEntity(entity: Entity): Boolean {
        if (entities.containsKey(entity.id)) return false
        
        entities[entity.id] = entity
        entityTypes.getOrPut(entity.entityType) { mutableSetOf() }.add(entity.id)
        outgoingEdges.getOrPut(entity.id) { mutableSetOf() }
        incomingEdges.getOrPut(entity.id) { mutableSetOf() }
        
        return true
    }
    
    /**
     * Remove an entity and all its edges
     */
    fun removeEntity(entityId: String): Boolean {
        val entity = entities[entityId] ?: return false
        
        // Remove all connected edges
        val allEdges = (outgoingEdges[entityId] ?: emptySet()) + (incomingEdges[entityId] ?: emptySet())
        allEdges.forEach { edgeId -> removeEdge(edgeId) }
        
        // Remove entity
        entities.remove(entityId)
        entityTypes[entity.entityType]?.remove(entityId)
        outgoingEdges.remove(entityId)
        incomingEdges.remove(entityId)
        
        return true
    }
    
    /**
     * Add an edge to the graph
     */
    fun addEdge(edge: Edge): Boolean {
        if (edges.containsKey(edge.id)) return false
        if (!entities.containsKey(edge.fromEntityId) || !entities.containsKey(edge.toEntityId)) {
            return false  // Both entities must exist
        }
        
        val validation = edge.validate()
        if (!validation.isValid) return false
        
        edges[edge.id] = edge
        outgoingEdges.getOrPut(edge.fromEntityId) { mutableSetOf() }.add(edge.id)
        incomingEdges.getOrPut(edge.toEntityId) { mutableSetOf() }.add(edge.id)
        relationshipTypes.getOrPut(edge.relationshipType) { mutableSetOf() }.add(edge.id)
        
        return true
    }
    
    /**
     * Remove an edge from the graph
     */
    fun removeEdge(edgeId: String): Boolean {
        val edge = edges[edgeId] ?: return false
        
        edges.remove(edgeId)
        outgoingEdges[edge.fromEntityId]?.remove(edgeId)
        incomingEdges[edge.toEntityId]?.remove(edgeId)
        relationshipTypes[edge.relationshipType]?.remove(edgeId)
        
        return true
    }
    
    /**
     * Get entity by ID
     */
    fun getEntity(entityId: String): Entity? = entities[entityId]
    
    /**
     * Get edge by ID
     */
    fun getEdge(edgeId: String): Edge? = edges[edgeId]
    
    /**
     * Get all entities
     */
    fun getAllEntities(): Collection<Entity> = entities.values
    
    /**
     * Get all edges
     */
    fun getAllEdges(): Collection<Edge> = edges.values
    
    /**
     * Get entities by type
     */
    fun getEntitiesByType(entityType: String): List<Entity> {
        return entityTypes[entityType]?.mapNotNull { entities[it] } ?: emptyList()
    }
    
    /**
     * Get edges by relationship type
     */
    fun getEdgesByType(relationshipType: String): List<Edge> {
        return relationshipTypes[relationshipType]?.mapNotNull { edges[it] } ?: emptyList()
    }
    
    /**
     * Get outgoing edges from an entity
     */
    fun getOutgoingEdges(entityId: String): List<Edge> {
        return outgoingEdges[entityId]?.mapNotNull { edges[it] } ?: emptyList()
    }
    
    /**
     * Get incoming edges to an entity
     */
    fun getIncomingEdges(entityId: String): List<Edge> {
        return incomingEdges[entityId]?.mapNotNull { edges[it] } ?: emptyList()
    }
    
    /**
     * Get all edges connected to an entity
     */
    fun getAllEdges(entityId: String): List<Edge> {
        return getOutgoingEdges(entityId) + getIncomingEdges(entityId)
    }
    
    /**
     * Get connected entities through any relationship
     */
    fun getConnectedEntities(entityId: String): List<Entity> {
        val connectedIds = mutableSetOf<String>()
        
        getOutgoingEdges(entityId).forEach { edge ->
            connectedIds.add(edge.toEntityId)
        }
        
        getIncomingEdges(entityId).forEach { edge ->
            if (!edge.directional) {  // Include source of non-directional edges
                connectedIds.add(edge.fromEntityId)
            }
        }
        
        return connectedIds.mapNotNull { entities[it] }
    }
    
    /**
     * Get connected entities through specific relationship type
     */
    fun getConnectedEntities(entityId: String, relationshipType: String): List<Entity> {
        val connectedIds = mutableSetOf<String>()
        
        getOutgoingEdges(entityId)
            .filter { it.relationshipType == relationshipType }
            .forEach { edge -> connectedIds.add(edge.toEntityId) }
        
        getIncomingEdges(entityId)
            .filter { it.relationshipType == relationshipType && !it.directional }
            .forEach { edge -> connectedIds.add(edge.fromEntityId) }
        
        return connectedIds.mapNotNull { entities[it] }
    }
    
    /**
     * Find shortest path between two entities
     */
    fun findShortestPath(fromEntityId: String, toEntityId: String): List<String>? {
        if (fromEntityId == toEntityId) return listOf(fromEntityId)
        if (!entities.containsKey(fromEntityId) || !entities.containsKey(toEntityId)) return null
        
        val queue = mutableListOf(listOf(fromEntityId))
        val visited = mutableSetOf<String>()
        
        while (queue.isNotEmpty()) {
            val path = queue.removeFirst()
            val currentEntity = path.last()
            
            if (currentEntity in visited) continue
            visited.add(currentEntity)
            
            getConnectedEntities(currentEntity).forEach { connectedEntity ->
                val newPath = path + connectedEntity.id
                if (connectedEntity.id == toEntityId) {
                    return newPath
                }
                if (connectedEntity.id !in visited) {
                    queue.add(newPath)
                }
            }
        }
        
        return null  // No path found
    }
    
    /**
     * Check if a path exists between two entities
     */
    fun hasPath(fromEntityId: String, toEntityId: String): Boolean {
        return findShortestPath(fromEntityId, toEntityId) != null
    }
    
    /**
     * Get all entities within N hops of a given entity
     */
    fun getEntitiesWithinDistance(entityId: String, maxDistance: Int): Map<Entity, Int> {
        if (!entities.containsKey(entityId)) return emptyMap()
        
        val result = mutableMapOf<Entity, Int>()
        val visited = mutableSetOf<String>()
        val queue = mutableListOf(entityId to 0)
        
        while (queue.isNotEmpty()) {
            val (currentId, distance) = queue.removeFirst()
            
            if (currentId in visited || distance > maxDistance) continue
            visited.add(currentId)
            
            entities[currentId]?.let { entity ->
                result[entity] = distance
            }
            
            if (distance < maxDistance) {
                getConnectedEntities(currentId).forEach { connectedEntity ->
                    if (connectedEntity.id !in visited) {
                        queue.add(connectedEntity.id to distance + 1)
                    }
                }
            }
        }
        
        return result
    }
    
    /**
     * Get graph statistics
     */
    fun getStatistics(): GraphStatistics {
        return GraphStatistics(
            entityCount = entities.size,
            edgeCount = edges.size,
            entityTypeCount = entityTypes.size,
            relationshipTypeCount = relationshipTypes.size,
            entityTypes = entityTypes.mapValues { it.value.size },
            relationshipTypes = relationshipTypes.mapValues { it.value.size }
        )
    }
    
    /**
     * Find root entities that represent unique subgraphs for inventory tracking.
     * These are entities that represent the main trackable items in inventory.
     */
    fun findInventoryRootEntities(): List<Entity> {
        val rootEntities = mutableListOf<Entity>()
        
        // Find Virtual entities with virtualType="filament_tray" (Bambu inventory items)
        val filamentTrays = getEntitiesByType("virtual")
            .filter { entity ->
                entity.getProperty<String>("virtualType") == "filament_tray"
            }
        rootEntities.addAll(filamentTrays)
        
        // Find PhysicalComponent entities that have no incoming edges (Creality and other root items)
        val physicalComponents = getEntitiesByType("physical_component")
            .filter { entity ->
                getIncomingEdges(entity.id).isEmpty()
            }
        rootEntities.addAll(physicalComponents)
        
        return rootEntities.distinctBy { it.id }
    }
    
    /**
     * Clear all data from the graph
     */
    fun clear() {
        entities.clear()
        edges.clear()
        outgoingEdges.clear()
        incomingEdges.clear()
        entityTypes.clear()
        relationshipTypes.clear()
    }
    
    /**
     * Get entities matching property criteria
     */
    fun findEntities(propertyFilters: Map<String, PropertyValue>): List<Entity> {
        return entities.values.filter { entity ->
            propertyFilters.all { (key, expectedValue) ->
                entity.properties[key] == expectedValue
            }
        }
    }
    
    /**
     * Get edges matching property criteria
     */
    fun findEdges(propertyFilters: Map<String, PropertyValue>): List<Edge> {
        return edges.values.filter { edge ->
            propertyFilters.all { (key, expectedValue) ->
                edge.properties[key] == expectedValue
            }
        }
    }
}

/**
 * Graph statistics for monitoring and debugging
 */
data class GraphStatistics(
    val entityCount: Int,
    val edgeCount: Int,
    val entityTypeCount: Int,
    val relationshipTypeCount: Int,
    val entityTypes: Map<String, Int>,
    val relationshipTypes: Map<String, Int>
)