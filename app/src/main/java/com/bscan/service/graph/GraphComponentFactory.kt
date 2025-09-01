package com.bscan.service.graph

import android.content.Context
import android.util.Log
import com.bscan.interpreter.BambuFormatInterpreter
import com.bscan.model.*
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.security.MessageDigest

/**
 * Graph-based factory that converts scan data into graph entities and relationships.
 * Replaces the hierarchical component factory with a flexible graph structure.
 */
class GraphComponentFactory(
    private val context: Context,
    private val graphRepository: GraphRepository
) {
    
    private val unifiedDataAccess by lazy {
        UnifiedDataAccess(
            CatalogRepository(context),
            UserDataRepository(context)
        )
    }
    
    private val bambuInterpreter by lazy {
        BambuFormatInterpreter(FilamentMappings(), unifiedDataAccess)
    }
    
    companion object {
        private const val TAG = "GraphComponentFactory"
        
        /**
         * Create deterministic compound key ID from multiple components
         */
        fun createCompoundId(vararg components: Pair<String, String>): String {
            val keyString = components.joinToString("|") { "${it.first}:${it.second}" }
            return MessageDigest.getInstance("SHA-256")
                .digest(keyString.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }
                .take(16) // 16-char hex string for readability
        }
    }
    
    /**
     * Create graph entities from a scan
     */
    suspend fun createEntitiesFromScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphCreationResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Creating graph entities from scan: ${encryptedScanData.tagUid}")
            
            when (decryptedScanData.tagFormat) {
                TagFormat.BAMBU_PROPRIETARY -> createBambuEntities(encryptedScanData, decryptedScanData)
                TagFormat.CREALITY_ASCII -> createCrealityEntities(encryptedScanData, decryptedScanData)
                TagFormat.OPENTAG_V1 -> createOpenTagEntities(encryptedScanData, decryptedScanData)
                else -> createGenericEntities(encryptedScanData, decryptedScanData)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating graph entities from scan", e)
            GraphCreationResult.failure("Failed to create entities: ${e.message}")
        }
    }
    
    /**
     * Create Bambu-specific entities and relationships
     */
    private suspend fun createBambuEntities(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphCreationResult {
        
        // Interpret the scan data
        val filamentInfo = bambuInterpreter.interpret(decryptedScanData)
            ?: return GraphCreationResult.failure("Failed to interpret Bambu scan data")
        
        val entities = mutableListOf<Entity>()
        val edges = mutableListOf<Edge>()
        
        // 1. Create RFID Tag entity with compound key
        val rfidTagId = createCompoundId(
            "type" to "tag",
            "tagUid" to encryptedScanData.tagUid
        )
        
        val rfidTag = runBlocking { graphRepository.getExistingEntity(rfidTagId) } ?: PhysicalComponent(
            id = rfidTagId,
            label = "Bambu RFID Tag ${encryptedScanData.tagUid}",
        ).apply {
            category = "rfid_tag"
            manufacturer = "Bambu Lab"
            setProperty("chipType", "mifare-classic-1k")
            setProperty("technology", encryptedScanData.technology)
            setProperty("scanDuration", encryptedScanData.scanDurationMs)
        }
        entities.add(rfidTag)
        
        // 2. Create Tag UID identifier with compound key
        val tagIdentifierId = createCompoundId(
            "idType" to IdentifierTypes.RFID_HARDWARE,
            "value" to encryptedScanData.tagUid
        )
        
        val tagIdentifier = runBlocking { graphRepository.getExistingEntity(tagIdentifierId) } as? Identifier ?: Identifier(
            id = tagIdentifierId,
            identifierType = IdentifierTypes.RFID_HARDWARE,
            value = encryptedScanData.tagUid
        ).apply {
            format = "hex"
            purpose = "authentication"
            isUnique = true
        }
        entities.add(tagIdentifier)
        
        // 3. Create Tray UID identifier with compound key
        val trayIdentifierId = createCompoundId(
            "idType" to IdentifierTypes.CONSUMABLE_UNIT,
            "value" to filamentInfo.trayUid
        )
        
        val trayIdentifier = runBlocking { graphRepository.getExistingEntity(trayIdentifierId) } as? Identifier ?: Identifier(
            id = trayIdentifierId,
            identifierType = IdentifierTypes.CONSUMABLE_UNIT,
            value = filamentInfo.trayUid
        ).apply {
            format = "hex"
            purpose = "tracking"
            isUnique = true
        }
        entities.add(trayIdentifier)
        
        // 4. Create Tray entity (virtual container) with compound key  
        val trayId = createCompoundId(
            "type" to "tray",
            "trayUid" to filamentInfo.trayUid
        )
        
        val tray = runBlocking { graphRepository.getExistingEntity(trayId) } as? Virtual ?: Virtual(
            id = trayId,
            virtualType = "filament_tray",
            label = "Bambu ${filamentInfo.filamentType} - ${filamentInfo.colorName}"
        ).apply {
            setProperty("filamentType", filamentInfo.filamentType)
            setProperty("colorName", filamentInfo.colorName)
            setProperty("colorHex", filamentInfo.colorHex)
            setProperty("manufacturer", "Bambu Lab")
            setProperty("category", "filament-tray")
        }
        entities.add(tray)
        
        // 5. Create Filament entity with compound key
        val filamentId = createCompoundId(
            "type" to "filament",
            "trayUid" to filamentInfo.trayUid,
            "material" to filamentInfo.filamentType
        )
        
        var stockDefinition: StockDefinition? = null
        val filament = runBlocking { graphRepository.getExistingEntity(filamentId) } as? PhysicalComponent ?: PhysicalComponent(
            id = filamentId,
            label = "${filamentInfo.filamentType} Filament - ${filamentInfo.colorName}"
        ).apply {
            category = "filament"
            manufacturer = "Bambu Lab"
            setProperty("material", filamentInfo.filamentType)
            setProperty("color", filamentInfo.colorName)
            setProperty("colorHex", filamentInfo.colorHex)
            setProperty("variableMass", true)
            
            // Try to get mass from catalog and store the StockDefinition for relationship creation
            try {
                stockDefinition = unifiedDataAccess.findBestStockDefinitionMatch(filamentInfo.filamentType, filamentInfo.colorName)
                stockDefinition?.weight?.let { quantity ->
                    if (quantity.unit == "g") {
                        val weight = quantity.value.toFloat()
                        massGrams = weight
                        setProperty("catalogMass", weight)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get catalog mass for filament", e)
            }
        }
        entities.add(filament)
        
        // Add StockDefinition to entities if found
        stockDefinition?.let { stockDef ->
            // Ensure StockDefinition is in the graph (it should be from catalog initialization)
            runBlocking { 
                if (graphRepository.getExistingEntity(stockDef.id) == null) {
                    entities.add(stockDef)
                }
            }
        }
        
        // 5a. Create InventoryItem to track this filament with compound key
        val inventoryItemId = createCompoundId(
            "type" to "inventory",
            "tracks" to filament.id
        )
        
        val inventoryItem = runBlocking { graphRepository.getExistingEntity(inventoryItemId) } as? InventoryItem ?: InventoryItem(
            id = inventoryItemId,
            label = "Inventory: ${filament.label}",
            trackingMode = TrackingMode.CONTINUOUS  // Filament is measured by weight
        ).apply {
            // Initialize with catalog data if available
            stockDefinition?.let { stockDef ->
                val catalogWeight = stockDef.weight
                if (catalogWeight?.unit == "g") {
                    currentQuantity = catalogWeight.value.toFloat()
                    unitWeight = 1.0f  // 1 gram per unit for continuous tracking
                    notes = "Auto-created from scan, initialized with catalog weight"
                }
            }
        }
        entities.add(inventoryItem)
        
        // 6. Create Core entity with compound key
        val coreId = createCompoundId(
            "type" to "core",
            "trayUid" to filamentInfo.trayUid
        )
        
        val core = runBlocking { graphRepository.getExistingEntity(coreId) } as? PhysicalComponent ?: PhysicalComponent(
            id = coreId,
            label = "Cardboard Core"
        ).apply {
            category = "core"
            manufacturer = "Bambu Lab"
            massGrams = 33.0f  // Standard core mass
            setProperty("material", "cardboard")
        }
        entities.add(core)
        
        
        // 8. Create Scan Activity
        val scanActivity = Activity(
            activityType = ActivityTypes.SCAN,
            label = "RFID Scan ${LocalDateTime.now()}"
        ).apply {
            timestamp = decryptedScanData.timestamp
            duration = encryptedScanData.scanDurationMs
            setProperty("success", decryptedScanData.scanResult == ScanResult.SUCCESS)
            setProperty("tagUid", encryptedScanData.tagUid)
            setProperty("technology", encryptedScanData.technology)
            setProperty("tagFormat", decryptedScanData.tagFormat.name)
            setProperty("authenticatedSectors", decryptedScanData.authenticatedSectors.size)
            setProperty("failedSectors", decryptedScanData.failedSectors.size)
        }
        entities.add(scanActivity)
        
        // Create relationships (only if they don't exist)
        
        // RFID tag is identified by tag UID
        if (!runBlocking { graphRepository.edgeExists(rfidTag.id, tagIdentifier.id, RelationshipTypes.IDENTIFIED_BY) }) {
            edges.add(Edge(
                fromEntityId = rfidTag.id,
                toEntityId = tagIdentifier.id,
                relationshipType = RelationshipTypes.IDENTIFIED_BY,
                directional = true
            ))
        }
        
        // Tray is identified by tray UID  
        if (!runBlocking { graphRepository.edgeExists(tray.id, trayIdentifier.id, RelationshipTypes.IDENTIFIED_BY) }) {
            edges.add(Edge(
                fromEntityId = tray.id,
                toEntityId = trayIdentifier.id,
                relationshipType = RelationshipTypes.IDENTIFIED_BY,
                directional = true
            ))
        }
        
        // RFID tag is attached to tray
        if (!runBlocking { graphRepository.edgeExists(rfidTag.id, tray.id, RelationshipTypes.ATTACHED_TO) }) {
            edges.add(Edge(
                fromEntityId = rfidTag.id,
                toEntityId = tray.id,
                relationshipType = RelationshipTypes.ATTACHED_TO,
                directional = false
            ))
        }
        
        // Tray contains filament and core
        if (!runBlocking { graphRepository.edgeExists(tray.id, filament.id, RelationshipTypes.CONTAINS) }) {
            edges.add(Edge(
                fromEntityId = tray.id,
                toEntityId = filament.id,
                relationshipType = RelationshipTypes.CONTAINS,
                directional = true
            ))
        }
        
        if (!runBlocking { graphRepository.edgeExists(tray.id, core.id, RelationshipTypes.CONTAINS) }) {
            edges.add(Edge(
                fromEntityId = tray.id,
                toEntityId = core.id,
                relationshipType = RelationshipTypes.CONTAINS,
                directional = true
            ))
        }
        
        
        // Filament is attached to core
        if (!runBlocking { graphRepository.edgeExists(filament.id, core.id, RelationshipTypes.ATTACHED_TO) }) {
            edges.add(Edge(
                fromEntityId = filament.id,
                toEntityId = core.id,
                relationshipType = RelationshipTypes.ATTACHED_TO,
                directional = false
            ))
        }
        
        
        // Scan activity scanned the RFID tag
        edges.add(Edge(
            fromEntityId = scanActivity.id,
            toEntityId = rfidTag.id,
            relationshipType = RelationshipTypes.RELATED_TO,
            directional = true
        ).apply {
            setProperty("action", "scanned")
            setProperty("result", decryptedScanData.scanResult.name)
        })
        
        // Link filament to its StockDefinition (catalog specification)
        stockDefinition?.let { stockDef ->
            if (!runBlocking { graphRepository.edgeExists(filament.id, stockDef.id, RelationshipTypes.DEFINED_BY) }) {
                edges.add(Edge(
                    fromEntityId = filament.id,
                    toEntityId = stockDef.id,
                    relationshipType = RelationshipTypes.DEFINED_BY,
                    directional = true
                ))
            }
        }
        
        // Link InventoryItem to PhysicalComponent it tracks
        if (!runBlocking { graphRepository.edgeExists(inventoryItem.id, filament.id, InventoryRelationshipTypes.TRACKS) }) {
            edges.add(Edge(
                fromEntityId = inventoryItem.id,
                toEntityId = filament.id,
                relationshipType = InventoryRelationshipTypes.TRACKS,
                directional = true
            ))
        }
        
        Log.d(TAG, "Created ${entities.size} entities and ${edges.size} relationships for Bambu scan")
        
        return GraphCreationResult.success(
            entities = entities,
            edges = edges,
            rootEntity = tray,  // Tray is the main inventory item
            scannedEntity = rfidTag  // Tag is what was physically scanned
        )
    }
    
    /**
     * Create generic entities for unknown scan types
     */
    private suspend fun createGenericEntities(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphCreationResult {
        
        val entities = mutableListOf<Entity>()
        val edges = mutableListOf<Edge>()
        
        // Create generic component
        val component = PhysicalComponent(
            label = "Unknown Component ${encryptedScanData.tagUid}"
        ).apply {
            category = "unknown"
            setProperty("technology", encryptedScanData.technology)
            setProperty("tagFormat", decryptedScanData.tagFormat.name)
        }
        entities.add(component)
        
        // Create identifier
        val identifier = Identifier(
            identifierType = IdentifierTypes.RFID_HARDWARE,
            value = encryptedScanData.tagUid
        ).apply {
            format = "hex"
            purpose = "identification"
            isUnique = true
        }
        entities.add(identifier)
        
        // Create scan activity
        val scanActivity = Activity(
            activityType = ActivityTypes.SCAN,
            label = "Generic Scan ${LocalDateTime.now()}"
        ).apply {
            timestamp = decryptedScanData.timestamp
            duration = encryptedScanData.scanDurationMs
            setProperty("success", decryptedScanData.scanResult == ScanResult.SUCCESS)
            setProperty("tagUid", encryptedScanData.tagUid)
            setProperty("technology", encryptedScanData.technology)
        }
        entities.add(scanActivity)
        
        // Create relationships
        edges.add(Edge(
            fromEntityId = component.id,
            toEntityId = identifier.id,
            relationshipType = RelationshipTypes.IDENTIFIED_BY,
            directional = true
        ))
        
        edges.add(Edge(
            fromEntityId = scanActivity.id,
            toEntityId = component.id,
            relationshipType = RelationshipTypes.RELATED_TO,
            directional = true
        ).apply {
            setProperty("action", "scanned")
        })
        
        return GraphCreationResult.success(
            entities = entities,
            edges = edges,
            rootEntity = component,
            scannedEntity = component
        )
    }
    
    /**
     * Placeholder for Creality entities
     */
    private suspend fun createCrealityEntities(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphCreationResult {
        // TODO: Implement Creality-specific entity creation
        return createGenericEntities(encryptedScanData, decryptedScanData)
    }
    
    /**
     * Placeholder for OpenTag entities
     */
    private suspend fun createOpenTagEntities(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphCreationResult {
        // TODO: Implement OpenTag-specific entity creation
        return createGenericEntities(encryptedScanData, decryptedScanData)
    }
}

/**
 * Result of graph entity creation
 */
sealed class GraphCreationResult {
    data class Success(
        val entities: List<Entity>,
        val edges: List<Edge>,
        val rootEntity: Entity,  // Main inventory entity
        val scannedEntity: Entity  // Entity that was physically scanned
    ) : GraphCreationResult()
    
    data class Failure(val error: String) : GraphCreationResult()
    
    companion object {
        fun success(
            entities: List<Entity>,
            edges: List<Edge>,
            rootEntity: Entity,
            scannedEntity: Entity
        ) = Success(entities, edges, rootEntity, scannedEntity)
        
        fun failure(error: String) = Failure(error)
    }
    
    val isSuccess: Boolean get() = this is Success
}