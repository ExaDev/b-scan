package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime

/**
 * Repository for managing graph-based data persistence.
 * Provides CRUD operations for entities and edges with efficient querying.
 */
class GraphRepository(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val gson = Gson()
    
    // In-memory graph for fast queries
    private var _graph: Graph? = null
    private val graph: Graph
        get() {
            if (_graph == null) {
                loadGraphFromStorage()
            }
            return _graph ?: Graph().also { _graph = it }
        }
    
    companion object {
        private const val TAG = "GraphRepository"
        private const val PREFS_NAME = "graph_repository"
        private const val KEY_ENTITIES = "entities"
        private const val KEY_EDGES = "edges"
        private const val KEY_LAST_MODIFIED = "last_modified"
        private const val KEY_VERSION = "version"
        private const val CURRENT_VERSION = 1
    }
    
    /**
     * Add an entity to the graph
     */
    suspend fun addEntity(entity: Entity): Boolean = withContext(Dispatchers.IO) {
        val success = graph.addEntity(entity)
        if (success) {
            persistGraph()
            Log.d(TAG, "Added entity: ${entity.id} (${entity.entityType})")
        } else {
            Log.w(TAG, "Failed to add entity: ${entity.id} - entity already exists")
        }
        success
    }
    
    /**
     * Add multiple entities
     */
    suspend fun addEntities(entities: List<Entity>): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        entities.forEach { entity ->
            if (graph.addEntity(entity)) {
                addedCount++
            }
        }
        
        if (addedCount > 0) {
            persistGraph()
            Log.d(TAG, "Added $addedCount entities out of ${entities.size}")
        }
        
        addedCount
    }
    
    /**
     * Add an edge to the graph
     */
    suspend fun addEdge(edge: Edge): Boolean = withContext(Dispatchers.IO) {
        val success = graph.addEdge(edge)
        if (success) {
            persistGraph()
            Log.d(TAG, "Added edge: ${edge.fromEntityId} -> ${edge.toEntityId} (${edge.relationshipType})")
        } else {
            Log.w(TAG, "Failed to add edge: ${edge.id} - validation failed or entities missing")
        }
        success
    }
    
    /**
     * Add multiple edges
     */
    suspend fun addEdges(edges: List<Edge>): Int = withContext(Dispatchers.IO) {
        var addedCount = 0
        edges.forEach { edge ->
            if (graph.addEdge(edge)) {
                addedCount++
            }
        }
        
        if (addedCount > 0) {
            persistGraph()
            Log.d(TAG, "Added $addedCount edges out of ${edges.size}")
        }
        
        addedCount
    }
    
    /**
     * Get entity by ID
     */
    suspend fun getEntity(entityId: String): Entity? = withContext(Dispatchers.IO) {
        graph.getEntity(entityId)
    }
    
    /**
     * Get edge by ID
     */
    suspend fun getEdge(edgeId: String): Edge? = withContext(Dispatchers.IO) {
        graph.getEdge(edgeId)
    }
    
    /**
     * Get all entities of a specific type
     */
    suspend fun getEntitiesByType(entityType: String): List<Entity> = withContext(Dispatchers.IO) {
        graph.getEntitiesByType(entityType)
    }
    
    /**
     * Get all edges of a specific relationship type
     */
    suspend fun getEdgesByType(relationshipType: String): List<Edge> = withContext(Dispatchers.IO) {
        graph.getEdgesByType(relationshipType)
    }
    
    /**
     * Get entities connected to a specific entity
     */
    suspend fun getConnectedEntities(entityId: String): List<Entity> = withContext(Dispatchers.IO) {
        graph.getConnectedEntities(entityId)
    }
    
    /**
     * Get entities connected through a specific relationship type
     */
    suspend fun getConnectedEntities(entityId: String, relationshipType: String): List<Entity> = withContext(Dispatchers.IO) {
        graph.getConnectedEntities(entityId, relationshipType)
    }
    
    /**
     * Find root entities that represent unique subgraphs for inventory tracking
     */
    suspend fun findInventoryRootEntities(): List<Entity> = withContext(Dispatchers.IO) {
        graph.findInventoryRootEntities()
    }
    
    /**
     * Find shortest path between two entities
     */
    suspend fun findShortestPath(fromEntityId: String, toEntityId: String): List<String>? = withContext(Dispatchers.IO) {
        graph.findShortestPath(fromEntityId, toEntityId)
    }
    
    /**
     * Get all entities within N hops of a given entity
     */
    suspend fun getEntitiesWithinDistance(entityId: String, maxDistance: Int): Map<Entity, Int> = withContext(Dispatchers.IO) {
        graph.getEntitiesWithinDistance(entityId, maxDistance)
    }
    
    /**
     * Find entities by property values
     */
    suspend fun findEntitiesByProperties(propertyFilters: Map<String, PropertyValue>): List<Entity> = withContext(Dispatchers.IO) {
        graph.findEntities(propertyFilters)
    }
    
    /**
     * Find edges by property values
     */
    suspend fun findEdgesByProperties(propertyFilters: Map<String, PropertyValue>): List<Edge> = withContext(Dispatchers.IO) {
        graph.findEdges(propertyFilters)
    }
    
    /**
     * Get all inventory items (entities marked as inventory items)
     */
    suspend fun getAllInventoryItems(): List<Entity> = withContext(Dispatchers.IO) {
        graph.getAllEntities().filter { entity ->
            entity.getProperty<Boolean>("isInventoryItem") == true
        }
    }
    
    /**
     * Check if entity exists by compound ID
     */
    suspend fun entityExists(compoundId: String): Boolean = withContext(Dispatchers.IO) {
        graph.getEntity(compoundId) != null
    }
    
    /**
     * Get existing entity by compound ID or return null
     */
    suspend fun getExistingEntity(compoundId: String): Entity? = withContext(Dispatchers.IO) {
        graph.getEntity(compoundId)
    }
    
    /**
     * Check if edge exists between two entities with specific relationship
     */
    suspend fun edgeExists(fromEntityId: String, toEntityId: String, relationshipType: String): Boolean = withContext(Dispatchers.IO) {
        graph.getAllEdges().any { edge ->
            edge.fromEntityId == fromEntityId && 
            edge.toEntityId == toEntityId && 
            edge.relationshipType == relationshipType
        }
    }
    
    /**
     * Get inventory item by identifier value
     */
    suspend fun getInventoryItemByIdentifier(identifierValue: String): Entity? = withContext(Dispatchers.IO) {
        // Find identifier entity with matching value
        val identifier = graph.getEntitiesByType(EntityTypes.IDENTIFIER)
            .filterIsInstance<Identifier>()
            .find { it.value == identifierValue }
            ?: return@withContext null
        
        // Find entities identified by this identifier
        val identifiedByEdges = graph.getIncomingEdges(identifier.id)
            .filter { it.relationshipType == RelationshipTypes.IDENTIFIED_BY }
        
        if (identifiedByEdges.isNotEmpty()) {
            return@withContext graph.getEntity(identifiedByEdges.first().fromEntityId)
        }
        
        null
    }
    
    /**
     * Remove entity and all connected edges
     */
    suspend fun removeEntity(entityId: String): Boolean = withContext(Dispatchers.IO) {
        val success = graph.removeEntity(entityId)
        if (success) {
            persistGraph()
            Log.d(TAG, "Removed entity: $entityId")
        }
        success
    }
    
    /**
     * Remove edge
     */
    suspend fun removeEdge(edgeId: String): Boolean = withContext(Dispatchers.IO) {
        val success = graph.removeEdge(edgeId)
        if (success) {
            persistGraph()
            Log.d(TAG, "Removed edge: $edgeId")
        }
        success
    }
    
    /**
     * Get graph statistics
     */
    suspend fun getStatistics(): GraphStatistics = withContext(Dispatchers.IO) {
        graph.getStatistics()
    }
    
    /**
     * Clear all graph data
     */
    suspend fun clearAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            graph.clear()
            prefs.edit().clear().apply()
            Log.d(TAG, "Cleared all graph data")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear graph data", e)
            false
        }
    }
    
    /**
     * Load graph from persistent storage
     */
    private fun loadGraphFromStorage() {
        try {
            val version = prefs.getInt(KEY_VERSION, 0)
            if (version != CURRENT_VERSION) {
                Log.i(TAG, "Graph version mismatch. Expected: $CURRENT_VERSION, Found: $version")
                _graph = Graph()
                return
            }
            
            val entitiesJson = prefs.getString(KEY_ENTITIES, null)
            val edgesJson = prefs.getString(KEY_EDGES, null)
            
            if (entitiesJson == null || edgesJson == null) {
                Log.i(TAG, "No persisted graph data found")
                _graph = Graph()
                return
            }
            
            val newGraph = Graph()
            
            // Deserialize entities
            val entityData = gson.fromJson<List<SerializedEntity>>(entitiesJson, object : TypeToken<List<SerializedEntity>>() {}.type)
            entityData.forEach { serialized ->
                val entity = deserializeEntity(serialized)
                if (entity != null) {
                    newGraph.addEntity(entity)
                }
            }
            
            // Deserialize edges
            val edgeData = gson.fromJson<List<SerializedEdge>>(edgesJson, object : TypeToken<List<SerializedEdge>>() {}.type)
            edgeData.forEach { serialized ->
                val edge = deserializeEdge(serialized)
                if (edge != null) {
                    newGraph.addEdge(edge)
                }
            }
            
            _graph = newGraph
            Log.d(TAG, "Loaded graph from storage: ${newGraph.getStatistics()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load graph from storage", e)
            _graph = Graph()
        }
    }
    
    /**
     * Persist graph to storage
     */
    private fun persistGraph() {
        try {
            val entities = graph.getAllEntities().map { serializeEntity(it) }
            val edges = graph.getAllEdges().map { serializeEdge(it) }
            
            prefs.edit()
                .putString(KEY_ENTITIES, gson.toJson(entities))
                .putString(KEY_EDGES, gson.toJson(edges))
                .putString(KEY_LAST_MODIFIED, LocalDateTime.now().toString())
                .putInt(KEY_VERSION, CURRENT_VERSION)
                .apply()
            
            Log.d(TAG, "Persisted graph to storage")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist graph to storage", e)
        }
    }
    
    /**
     * Serialize entity for storage
     */
    private fun serializeEntity(entity: Entity): SerializedEntity {
        return SerializedEntity(
            id = entity.id,
            entityType = entity.entityType,
            label = entity.label,
            properties = entity.properties.mapValues { it.value.asString() },
            className = entity::class.java.simpleName
        )
    }
    
    /**
     * Deserialize entity from storage
     */
    private fun deserializeEntity(serialized: SerializedEntity): Entity? {
        return when (serialized.className) {
            "PhysicalComponent" -> PhysicalComponent(
                id = serialized.id,
                label = serialized.label,
                properties = deserializeProperties(serialized.properties)
            )
            "Identifier" -> {
                val identifierType = serialized.properties["identifierType"] ?: ""
                val value = serialized.properties["value"] ?: ""
                Identifier(
                    id = serialized.id,
                    identifierType = identifierType,
                    value = value,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "Location" -> Location(
                id = serialized.id,
                label = serialized.label,
                properties = deserializeProperties(serialized.properties)
            )
            "Person" -> Person(
                id = serialized.id,
                label = serialized.label,
                properties = deserializeProperties(serialized.properties)
            )
            "Activity" -> {
                val activityType = serialized.properties["activityType"] ?: ""
                Activity(
                    id = serialized.id,
                    activityType = activityType,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "Information" -> {
                val informationType = serialized.properties["informationType"] ?: ""
                Information(
                    id = serialized.id,
                    informationType = informationType,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "Virtual" -> {
                val virtualType = serialized.properties["virtualType"] ?: ""
                Virtual(
                    id = serialized.id,
                    virtualType = virtualType,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "InventoryItem" -> {
                val trackingModeStr = serialized.properties["trackingMode"] ?: "DISCRETE"
                val trackingMode = try {
                    TrackingMode.valueOf(trackingModeStr)
                } catch (e: IllegalArgumentException) {
                    TrackingMode.DISCRETE
                }
                InventoryItem(
                    id = serialized.id,
                    label = serialized.label,
                    trackingMode = trackingMode,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "CalibrationActivity" -> {
                CalibrationActivity(
                    id = serialized.id,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "MeasurementActivity" -> {
                MeasurementActivity(
                    id = serialized.id,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            "StockMovementActivity" -> {
                val movementTypeStr = serialized.properties["movementType"] ?: "ADJUSTMENT"
                val movementType = try {
                    StockMovementType.valueOf(movementTypeStr)
                } catch (e: IllegalArgumentException) {
                    StockMovementType.ADJUSTMENT
                }
                StockMovementActivity(
                    id = serialized.id,
                    movementType = movementType,
                    label = serialized.label,
                    properties = deserializeProperties(serialized.properties)
                )
            }
            else -> {
                Log.w(TAG, "Unknown entity class: ${serialized.className}")
                null
            }
        }
    }
    
    /**
     * Serialize edge for storage
     */
    private fun serializeEdge(edge: Edge): SerializedEdge {
        return SerializedEdge(
            id = edge.id,
            fromEntityId = edge.fromEntityId,
            toEntityId = edge.toEntityId,
            relationshipType = edge.relationshipType,
            properties = edge.properties.mapValues { it.value.asString() },
            directional = edge.directional
        )
    }
    
    /**
     * Deserialize edge from storage
     */
    private fun deserializeEdge(serialized: SerializedEdge): Edge? {
        return try {
            Edge(
                id = serialized.id,
                fromEntityId = serialized.fromEntityId,
                toEntityId = serialized.toEntityId,
                relationshipType = serialized.relationshipType,
                properties = deserializeProperties(serialized.properties),
                directional = serialized.directional
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize edge: ${serialized.id}", e)
            null
        }
    }
    
    /**
     * Deserialize properties from string map
     */
    private fun deserializeProperties(stringProps: Map<String, String>): MutableMap<String, PropertyValue> {
        val properties = mutableMapOf<String, PropertyValue>()
        
        stringProps.forEach { (key, value) ->
            // Try to infer property type and create appropriate PropertyValue
            properties[key] = when {
                value == "true" || value == "false" -> PropertyValue.create(value.toBoolean())
                value.matches(Regex("-?\\d+")) -> PropertyValue.create(value.toLongOrNull() ?: value)
                value.matches(Regex("-?\\d+\\.\\d+")) -> PropertyValue.create(value.toDoubleOrNull() ?: value)
                else -> PropertyValue.create(value)
            }
        }
        
        return properties
    }
}

/**
 * Serializable entity representation
 */
private data class SerializedEntity(
    val id: String,
    val entityType: String,
    val label: String,
    val properties: Map<String, String>,
    val className: String
)

/**
 * Serializable edge representation
 */
private data class SerializedEdge(
    val id: String,
    val fromEntityId: String,
    val toEntityId: String,
    val relationshipType: String,
    val properties: Map<String, String>,
    val directional: Boolean
)