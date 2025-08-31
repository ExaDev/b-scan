package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import com.bscan.service.graph.GraphComponentFactory
import com.bscan.service.graph.GraphCreationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for integrating graph-based data model with scan processing.
 * Bridges between existing scan data and graph entities.
 */
class GraphDataService(private val context: Context) {
    
    private val graphRepository by lazy { GraphRepository(context) }
    private val graphComponentFactory by lazy { GraphComponentFactory(context) }
    
    companion object {
        private const val TAG = "GraphDataService"
    }
    
    /**
     * Process scan data and create graph entities
     */
    suspend fun processScanData(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphScanResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Processing scan data: ${encryptedScanData.tagUid}")
            
            // Create graph entities from scan
            val creationResult = graphComponentFactory.createEntitiesFromScan(
                encryptedScanData,
                decryptedScanData
            )
            
            when (creationResult) {
                is GraphCreationResult.Success -> {
                    // Add entities and edges to repository
                    val entitiesAdded = graphRepository.addEntities(creationResult.entities)
                    val edgesAdded = graphRepository.addEdges(creationResult.edges)
                    
                    Log.d(TAG, "Added $entitiesAdded entities and $edgesAdded edges to graph")
                    
                    GraphScanResult.Success(
                        rootEntity = creationResult.rootEntity,
                        scannedEntity = creationResult.scannedEntity,
                        allEntities = creationResult.entities,
                        allEdges = creationResult.edges,
                        entitiesAdded = entitiesAdded,
                        edgesAdded = edgesAdded
                    )
                }
                is GraphCreationResult.Failure -> {
                    Log.w(TAG, "Failed to create graph entities: ${creationResult.error}")
                    GraphScanResult.Failure(creationResult.error)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scan data", e)
            GraphScanResult.Failure("Failed to process scan: ${e.message}")
        }
    }
    
    /**
     * Get inventory item by identifier
     */
    suspend fun getInventoryItemByIdentifier(identifierValue: String): Entity? {
        return graphRepository.getInventoryItemByIdentifier(identifierValue)
    }
    
    /**
     * Get all inventory items
     */
    suspend fun getAllInventoryItems(): List<Entity> {
        return graphRepository.getAllInventoryItems()
    }
    
    /**
     * Get entity by ID
     */
    suspend fun getEntity(entityId: String): Entity? {
        return graphRepository.getEntity(entityId)
    }
    
    /**
     * Get connected entities (for navigation)
     */
    suspend fun getConnectedEntities(entityId: String): List<Entity> {
        return graphRepository.getConnectedEntities(entityId)
    }
    
    /**
     * Get entities connected through specific relationship
     */
    suspend fun getConnectedEntities(entityId: String, relationshipType: String): List<Entity> {
        return graphRepository.getConnectedEntities(entityId, relationshipType)
    }
    
    /**
     * Find shortest path between entities (for navigation)
     */
    suspend fun findPath(fromEntityId: String, toEntityId: String): List<String>? {
        return graphRepository.findShortestPath(fromEntityId, toEntityId)
    }
    
    /**
     * Get physical components (for traditional component view)
     */
    suspend fun getPhysicalComponents(): List<PhysicalComponent> {
        return graphRepository.getEntitiesByType(EntityTypes.PHYSICAL_COMPONENT)
            .filterIsInstance<PhysicalComponent>()
    }
    
    /**
     * Get identifiers for an entity
     */
    suspend fun getEntityIdentifiers(entityId: String): List<Identifier> {
        return graphRepository.getConnectedEntities(entityId, RelationshipTypes.IDENTIFIED_BY)
            .filterIsInstance<Identifier>()
    }
    
    /**
     * Get scan activities for an entity
     */
    suspend fun getEntityScanHistory(entityId: String): List<Activity> {
        return graphRepository.getConnectedEntities(entityId, RelationshipTypes.RELATED_TO)
            .filterIsInstance<Activity>()
            .filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
    }
    
    /**
     * Get entities by category
     */
    suspend fun getEntitiesByCategory(category: String): List<Entity> {
        return graphRepository.findEntitiesByProperties(
            mapOf("category" to PropertyValue.create(category))
        )
    }
    
    /**
     * Get graph statistics
     */
    suspend fun getGraphStatistics(): GraphStatistics {
        return graphRepository.getStatistics()
    }
    
    /**
     * Convert legacy Component to graph entities (migration helper)
     */
    suspend fun migrateComponent(component: Component): GraphMigrationResult = withContext(Dispatchers.IO) {
        try {
            val entities = mutableListOf<Entity>()
            val edges = mutableListOf<Edge>()
            
            // Create main physical component entity
            val physicalComponent = PhysicalComponent(
                id = component.id,
                label = component.name
            ).apply {
                category = component.category
                component.massGrams?.let { massGrams = it }
                setProperty("isInventoryItem", component.isInventoryItem)
                setProperty("migratedFrom", "Component")
            }
            entities.add(physicalComponent)
            
            // Create identifier entities
            component.identifiers.forEach { identifier ->
                val identifierEntity = Identifier(
                    identifierType = identifier.type.name.lowercase(),
                    value = identifier.value
                ).apply {
                    purpose = identifier.purpose.name.lowercase()
                    isUnique = identifier.isUnique
                }
                entities.add(identifierEntity)
                
                // Create identified-by relationship
                edges.add(Edge(
                    fromEntityId = physicalComponent.id,
                    toEntityId = identifierEntity.id,
                    relationshipType = RelationshipTypes.IDENTIFIED_BY,
                    directional = true
                ))
            }
            
            // Add entities and edges to repository
            val entitiesAdded = graphRepository.addEntities(entities)
            val edgesAdded = graphRepository.addEdges(edges)
            
            GraphMigrationResult.Success(
                migratedEntity = physicalComponent,
                entitiesCreated = entities.size,
                edgesCreated = edges.size,
                entitiesAdded = entitiesAdded,
                edgesAdded = edgesAdded
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate component: ${component.id}", e)
            GraphMigrationResult.Failure("Migration failed: ${e.message}")
        }
    }
    
    /**
     * Clear all graph data
     */
    suspend fun clearAll(): Boolean {
        return graphRepository.clearAll()
    }
}

/**
 * Result of processing scan data with graph model
 */
sealed class GraphScanResult {
    data class Success(
        val rootEntity: Entity,
        val scannedEntity: Entity,
        val allEntities: List<Entity>,
        val allEdges: List<Edge>,
        val entitiesAdded: Int,
        val edgesAdded: Int
    ) : GraphScanResult()
    
    data class Failure(val error: String) : GraphScanResult()
}

/**
 * Result of migrating legacy component to graph
 */
sealed class GraphMigrationResult {
    data class Success(
        val migratedEntity: Entity,
        val entitiesCreated: Int,
        val edgesCreated: Int,
        val entitiesAdded: Int,
        val edgesAdded: Int
    ) : GraphMigrationResult()
    
    data class Failure(val error: String) : GraphMigrationResult()
}