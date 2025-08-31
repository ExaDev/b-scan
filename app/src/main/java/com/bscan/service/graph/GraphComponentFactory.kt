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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * Graph-based factory that converts scan data into graph entities and relationships.
 * Replaces the hierarchical component factory with a flexible graph structure.
 */
class GraphComponentFactory(private val context: Context) {
    
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
        
        // 1. Create RFID Tag entity
        val rfidTag = PhysicalComponent(
            label = "Bambu RFID Tag ${encryptedScanData.tagUid}",
        ).apply {
            category = "rfid_tag"
            manufacturer = "Bambu Lab"
            setProperty("chipType", "mifare-classic-1k")
            setProperty("technology", encryptedScanData.technology)
            setProperty("scanDuration", encryptedScanData.scanDurationMs)
        }
        entities.add(rfidTag)
        
        // 2. Create Tag UID identifier
        val tagIdentifier = Identifier(
            identifierType = IdentifierTypes.RFID_HARDWARE,
            value = encryptedScanData.tagUid
        ).apply {
            format = "hex"
            purpose = "authentication"
            isUnique = true
        }
        entities.add(tagIdentifier)
        
        // 3. Create Tray UID identifier  
        val trayIdentifier = Identifier(
            identifierType = IdentifierTypes.CONSUMABLE_UNIT,
            value = filamentInfo.trayUid
        ).apply {
            format = "hex"
            purpose = "tracking"
            isUnique = true
        }
        entities.add(trayIdentifier)
        
        // 4. Create Tray entity (virtual container)
        val tray = Virtual(
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
        
        // 5. Create Filament entity
        val filament = PhysicalComponent(
            label = "${filamentInfo.filamentType} Filament - ${filamentInfo.colorName}"
        ).apply {
            category = "filament"
            manufacturer = "Bambu Lab"
            setProperty("material", filamentInfo.filamentType)
            setProperty("color", filamentInfo.colorName)
            setProperty("colorHex", filamentInfo.colorHex)
            setProperty("variableMass", true)
            
            // Try to get mass from catalog
            try {
                val product = unifiedDataAccess.findBestProductMatch(filamentInfo.filamentType, filamentInfo.colorName)
                product?.filamentWeightGrams?.let { weight ->
                    massGrams = weight
                    setProperty("catalogMass", weight)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get catalog mass for filament", e)
            }
        }
        entities.add(filament)
        
        // 6. Create Core entity
        val core = PhysicalComponent(
            label = "Cardboard Core"
        ).apply {
            category = "core"
            manufacturer = "Bambu Lab"
            massGrams = 33.0f  // Standard core mass
            setProperty("material", "cardboard")
        }
        entities.add(core)
        
        // 7. Create Spool entity
        val spool = PhysicalComponent(
            label = "Refillable Spool"
        ).apply {
            category = "spool"
            manufacturer = "Bambu Lab"
            massGrams = 212.0f  // Standard spool mass
            setProperty("material", "plastic")
            setProperty("reusable", true)
        }
        entities.add(spool)
        
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
        
        // Create relationships
        
        // RFID tag is identified by tag UID
        edges.add(Edge(
            fromEntityId = rfidTag.id,
            toEntityId = tagIdentifier.id,
            relationshipType = RelationshipTypes.IDENTIFIED_BY,
            directional = true
        ))
        
        // Tray is identified by tray UID  
        edges.add(Edge(
            fromEntityId = tray.id,
            toEntityId = trayIdentifier.id,
            relationshipType = RelationshipTypes.IDENTIFIED_BY,
            directional = true
        ))
        
        // RFID tag is attached to tray
        edges.add(Edge(
            fromEntityId = rfidTag.id,
            toEntityId = tray.id,
            relationshipType = RelationshipTypes.ATTACHED_TO,
            directional = false
        ))
        
        // Tray contains filament, core, and spool
        edges.add(Edge(
            fromEntityId = tray.id,
            toEntityId = filament.id,
            relationshipType = RelationshipTypes.CONTAINS,
            directional = true
        ))
        
        edges.add(Edge(
            fromEntityId = tray.id,
            toEntityId = core.id,
            relationshipType = RelationshipTypes.CONTAINS,
            directional = true
        ))
        
        edges.add(Edge(
            fromEntityId = tray.id,
            toEntityId = spool.id,
            relationshipType = RelationshipTypes.CONTAINS,
            directional = true
        ))
        
        // Filament is attached to core
        edges.add(Edge(
            fromEntityId = filament.id,
            toEntityId = core.id,
            relationshipType = RelationshipTypes.ATTACHED_TO,
            directional = false
        ))
        
        // Core is attached to spool
        edges.add(Edge(
            fromEntityId = core.id,
            toEntityId = spool.id,
            relationshipType = RelationshipTypes.ATTACHED_TO,
            directional = false
        ))
        
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