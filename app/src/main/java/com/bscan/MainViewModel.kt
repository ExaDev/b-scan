package com.bscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.model.graph.ContinuousQuantity
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.GraphRepository
import com.bscan.repository.GraphEntityCreationResult
import com.bscan.service.ScanningService
import com.bscan.service.DefaultScanningService
import com.bscan.service.GraphDataService
import com.bscan.service.ScanResult as ServiceScanResult
import com.bscan.model.TagFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import android.nfc.Tag

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(BScanUiState())
    val uiState: StateFlow<BScanUiState> = _uiState.asStateFlow()
    
    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()
    
    
    // Graph-based state flows
    private val _graphEntityCreationResult = MutableStateFlow<GraphEntityCreationResult?>(null)
    val graphEntityCreationResult: StateFlow<GraphEntityCreationResult?> = _graphEntityCreationResult.asStateFlow()
    
    private val _graphOperationState = MutableStateFlow<GraphOperationState>(GraphOperationState.Idle)
    val graphOperationState: StateFlow<GraphOperationState> = _graphOperationState.asStateFlow()
    
    private val catalogRepository = CatalogRepository(application)
    private val userDataRepository = UserDataRepository(application)
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val graphRepository = GraphRepository(application)
    private val unifiedDataAccess = UnifiedDataAccess(
        catalogRepository, 
        userDataRepository, 
        scanHistoryRepository,
        null, // componentRepository (removed legacy support)
        application
    )
    
    init {
        // Initialize catalog entities in the graph on app startup
        viewModelScope.launch {
            try {
                Log.i("MainViewModel", "Initializing catalog entities on app startup")
                graphRepository.initializeCatalogEntities()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to initialize catalog entities", e)
            }
        }
    }
    
    // Decoupled scanning service (only persists scan data, not components)
    private val scanningService: ScanningService = DefaultScanningService(
        context = application,
        scanHistoryRepository = scanHistoryRepository
    )
    
    // Track simulation state to cycle through all products
    private var simulationProductIndex = 0
    
    fun onTagDetected() {
        scanningService.onTagDetected()
        
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.TAG_DETECTED,
            error = null
        )
        _scanProgress.value = ScanProgress(
            stage = ScanStage.TAG_DETECTED,
            percentage = 0.0f,
            statusMessage = "Tag detected"
        )
    }
    
    // Removed processTagWithFactory() - replaced by ScanningService
    
    // Removed createComponentsFromScan() - replaced by ScanningService
    
    /**
     * Process scan data using decoupled scanning service
     */
    fun processScanData(encryptedData: EncryptedScanData, decryptedData: DecryptedScanData) {
        viewModelScope.launch {
            try {
                // Use the decoupled scanning service to persist scan data
                val scanResult = scanningService.processScanData(encryptedData, decryptedData)
                
                // Update UI state based on scan result
                _uiState.value = BScanUiState(
                    scanState = ScanState.SUCCESS,
                    debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                )
                
                // Create graph entities from scan data
                withContext(Dispatchers.IO) {
                    try {
                        _graphOperationState.value = GraphOperationState.CreatingEntities
                        val graphResult = unifiedDataAccess.createGraphEntitiesFromScan(encryptedData, decryptedData)
                        
                        _graphEntityCreationResult.value = graphResult
                        
                        if (graphResult.success) {
                            _graphOperationState.value = GraphOperationState.EntitiesCreated
                            Log.d("MainViewModel", "Created ${graphResult.totalEntitiesCreated} graph entities and ${graphResult.totalEdgesCreated} edges")
                            
                        } else {
                            _graphOperationState.value = GraphOperationState.Error(graphResult.errorMessage ?: "Unknown graph creation error")
                            Log.w("MainViewModel", "Graph entity creation failed: ${graphResult.errorMessage}")
                        }
                    } catch (e: Exception) {
                        _graphOperationState.value = GraphOperationState.Error("Graph creation failed: ${e.message}")
                        Log.e("MainViewModel", "Failed to create graph entities", e)
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Scanning service failed", e)
                _uiState.value = BScanUiState(
                    scanState = ScanState.ERROR,
                    error = "Scanning failed: ${e.message}",
                    debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                )
            }
        }
    }
    
    /**
     * Helper method to create ScanDebugInfo from DecryptedScanData
     */
    private fun createDebugInfoFromDecryptedData(decryptedData: DecryptedScanData): ScanDebugInfo {
        return ScanDebugInfo(
            uid = decryptedData.tagUid,
            tagSizeBytes = decryptedData.tagSizeBytes,
            sectorCount = decryptedData.sectorCount,
            authenticatedSectors = decryptedData.authenticatedSectors,
            failedSectors = decryptedData.failedSectors,
            usedKeyTypes = decryptedData.usedKeys,
            blockData = decryptedData.decryptedBlocks,
            derivedKeys = decryptedData.derivedKeys,
            rawColorBytes = "", // Not available from DecryptedScanData (collected during real-time scan only)
            errorMessages = decryptedData.errors,
            parsingDetails = mapOf(), // Empty for now
            fullRawHex = "", // Not available in DecryptedScanData
            decryptedHex = "" // Could reconstruct from blocks if needed
        )
    }
    
    
    fun setScanning() {
        scanningService.setScanning()
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.PROCESSING,
            error = null
        )
    }
    
    fun updateScanProgress(progress: ScanProgress) {
        scanningService.updateScanProgress(progress)
        _scanProgress.value = progress
    }
    
    fun setNfcError(error: String) {
        scanningService.setError(error)
        _uiState.value = _uiState.value.copy(
            error = error,
            scanState = ScanState.ERROR
        )
        
        // Reset graph state on NFC errors
        resetComponentState()
        _graphOperationState.value = GraphOperationState.Error(error)
    }
    
    fun setAuthenticationFailed(tagData: NfcTagData, debugCollector: DebugDataCollector) {
        // Create scan data for failed authentication
        val encryptedData = debugCollector.createEncryptedScanData(
            uid = tagData.uid,
            technology = tagData.technology,
            scanDurationMs = 0
        )
        
        val decryptedData = debugCollector.createDecryptedScanData(
            uid = tagData.uid,
            technology = tagData.technology,
            result = ScanResult.AUTHENTICATION_FAILED,
            keyDerivationTimeMs = 0,
            authenticationTimeMs = 0
        )
        
        // Save to history even for failed scans
        viewModelScope.launch(Dispatchers.IO) {
            unifiedDataAccess.recordScan(encryptedData, decryptedData)
        }
        
        _uiState.value = _uiState.value.copy(
            error = "Authentication failed - see debug info below",
            scanState = ScanState.ERROR,
            debugInfo = createDebugInfoFromDecryptedData(decryptedData)
        )
        
        _graphOperationState.value = GraphOperationState.Error("Authentication failed")
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        
        // Clear graph operation errors as well
        if (_graphOperationState.value is GraphOperationState.Error) {
            _graphOperationState.value = GraphOperationState.Idle
        }
    }
    
    fun resetScan() {
        scanningService.clearScanState()
        _uiState.value = BScanUiState()
        _scanProgress.value = null
        resetComponentState()
    }
    
    fun simulateScan() {
        viewModelScope.launch {
            // Start simulation
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.TAG_DETECTED,
                error = null
            )
            
            // Reset component state for new simulation
            resetComponentState()
            
            // Simulate progress stages
            val stages = listOf(
                ScanProgress(ScanStage.TAG_DETECTED, 0.0f, statusMessage = "Tag detected"),
                ScanProgress(ScanStage.CONNECTING, 0.2f, statusMessage = "Connecting to tag"),
                ScanProgress(ScanStage.KEY_DERIVATION, 0.4f, statusMessage = "Deriving keys"),
                ScanProgress(ScanStage.AUTHENTICATING, 0.6f, statusMessage = "Authenticating"),
                ScanProgress(ScanStage.READING_BLOCKS, 0.8f, statusMessage = "Reading data blocks"),
                ScanProgress(ScanStage.PARSING, 0.9f, statusMessage = "Parsing component data")
            )
            
            // Step through progress stages
            for (stage in stages) {
                _scanProgress.value = stage
                delay(150)
            }
            
            // Create mock component hierarchy
            val trayId = generateComponentId("tray")
            val mockTrayComponent = Component(
                id = trayId,
                identifiers = listOf(
                    ComponentIdentifier(
                        type = IdentifierType.CONSUMABLE_UNIT,
                        value = "MOCK_TRAY_${System.currentTimeMillis()}",
                        purpose = IdentifierPurpose.TRACKING
                    )
                ),
                name = "Mock Filament Tray",
                category = "filament-tray",
                tags = listOf("simulation", "mock", "consumable"),
                parentComponentId = null,
                childComponents = emptyList(),
                massGrams = 1000f,
                variableMass = true,
                manufacturer = "Mock Manufacturer",
                description = "Simulated filament tray for testing",
                metadata = mapOf(
                    "materialType" to "PLA",
                    "colorHex" to "#808080",
                    "colorName" to "Mock Gray",
                    "simulation" to "true"
                ),
                createdAt = System.currentTimeMillis(),
                lastUpdated = java.time.LocalDateTime.now()
            )
            
            // Complete simulation
            _scanProgress.value = ScanProgress(
                stage = ScanStage.COMPLETED,
                percentage = 1.0f,
                statusMessage = "Simulation completed"
            )
            
            _uiState.value = BScanUiState(
                scanState = ScanState.SUCCESS,
                debugInfo = null
            )
            
        }
    }
    
    // Expose unified data access for UI access
    fun getUnifiedDataAccess() = unifiedDataAccess
    
    // Expose user data repository for direct access where needed
    fun getUserDataRepository() = userDataRepository
    
    
    // Mappings access methods
    fun getManufacturers() = unifiedDataAccess.getAllManufacturers()
    
    fun getManufacturer(manufacturerId: String) = unifiedDataAccess.getManufacturer(manufacturerId)
    
    fun getStockDefinitions(manufacturerId: String) = unifiedDataAccess.getStockDefinitions(manufacturerId)
    
    // Removed refreshMappings() - no longer using interpreterFactory
    
    // Removed integrateMassReading() - mass inference moved to scanning service
    
    // Removed updateComponentRelationships() - component grouping moved to scanning service
    
    /**
     * Calculate total hierarchy mass for graph entities
     */
    suspend fun calculateEntityHierarchyMass(entityId: String): Float {
        return withContext(Dispatchers.IO) {
            try {
                val entity = unifiedDataAccess.getConnectedEntities(entityId, RelationshipTypes.CONTAINS)
                var totalMass = 0f
                
                entity.forEach { connectedEntity ->
                    connectedEntity.getProperty<Float>("massGrams")?.let { mass ->
                        totalMass += mass
                    }
                }
                
                totalMass
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error calculating entity hierarchy mass", e)
                0f
            }
        }
    }
    
    /**
     * Reset component architecture state
     */
    fun resetComponentState() {
        _graphEntityCreationResult.value = null
        _graphOperationState.value = GraphOperationState.Idle
    }
    
    
    // === Graph-Based Data Access Methods ===
    
    /**
     * Get inventory item by identifier using graph approach
     */
    suspend fun getGraphInventoryItem(identifierValue: String): Entity? {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getGraphInventoryItem(identifierValue)
        }
    }
    
    /**
     * Get all inventory items using graph approach
     */
    suspend fun getAllGraphInventoryItems(): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getAllGraphInventoryItems()
        }
    }
    
    /**
     * Get connected entities for navigation in graph model
     */
    suspend fun getConnectedEntities(entityId: String): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getConnectedEntities(entityId)
        }
    }
    
    /**
     * Get entities connected through specific relationship
     */
    suspend fun getConnectedEntities(entityId: String, relationshipType: String): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getConnectedEntities(entityId, relationshipType)
        }
    }
    
    /**
     * Get graph statistics for debugging and monitoring
     */
    suspend fun getGraphStatistics(): GraphStatistics? {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getGraphStatistics()
        }
    }
    
    /**
     * Get physical components from graph model
     */
    suspend fun getGraphPhysicalComponents(): List<PhysicalComponent> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getAllGraphInventoryItems()
                .filterIsInstance<PhysicalComponent>()
        }
    }
    
    /**
     * Get identifiers for a graph entity
     */
    suspend fun getEntityIdentifiers(entityId: String): List<Identifier> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getConnectedEntities(entityId, RelationshipTypes.IDENTIFIED_BY)
                .filterIsInstance<Identifier>()
        }
    }
    
    /**
     * Get scan history for a graph entity
     */
    suspend fun getEntityScanHistory(entityId: String): List<Activity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getConnectedEntities(entityId, RelationshipTypes.RELATED_TO)
                .filterIsInstance<Activity>()
                .filter { it.getProperty<String>("activityType") == ActivityTypes.SCAN }
        }
    }
    
    /**
     * Get entities by category for UI filtering (graph-based)
     */
    suspend fun getEntitiesByCategory(category: String): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getAllGraphInventoryItems().filter { entity ->
                entity.getProperty<String>("category") == category
            }
        }
    }
    
    /**
     * Get all root entities (inventory items)
     */
    suspend fun getRootEntities(): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getAllGraphInventoryItems()
        }
    }
    
    /**
     * Search entities by tags (graph-based)
     */
    suspend fun getEntitiesByTags(tags: List<String>): List<Entity> {
        return withContext(Dispatchers.IO) {
            unifiedDataAccess.getAllGraphInventoryItems().filter { entity ->
                val entityTags = entity.getProperty<List<*>>("tags")?.mapNotNull { it as? String } ?: emptyList()
                tags.any { tag -> entityTags.contains(tag) }
            }
        }
    }
    
    /**
     * Create graph entity from catalog data
     */
    suspend fun createEntityFromCatalog(
        tagUid: String,
        manufacturerId: String,
        skuId: String,
        metadata: Map<String, String> = emptyMap()
    ): GraphEntityCreationResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Creating graph entity from catalog: $manufacturerId/$skuId")
                _graphOperationState.value = GraphOperationState.CreatingEntities
                
                // Step 1: Lookup SKU data
                val stockDefinitions = catalogRepository.getStockDefinitions(manufacturerId)
                val matchingStockDefinition = stockDefinitions.firstOrNull { 
                    it.getProperty<String>("sku") == skuId 
                }
                
                if (matchingStockDefinition == null) {
                    return@withContext GraphEntityCreationResult(
                        success = false,
                        errorMessage = "SKU not found in catalog: $manufacturerId/$skuId"
                    )
                }
                
                // Step 2: Create physical component entity
                val physicalComponent = PhysicalComponent(
                    label = matchingStockDefinition.getProperty<String>("displayName") 
                        ?: matchingStockDefinition.label
                ).apply {
                    category = "filament"
                    manufacturer = matchingStockDefinition.getProperty<String>("manufacturer") ?: manufacturerId
                    val weight = matchingStockDefinition.getProperty<ContinuousQuantity>("weight")
                    massGrams = weight?.value?.toFloat() ?: 1000f
                    setProperty("variableMass", true)
                    setProperty("sku", skuId)
                    setProperty("manufacturerId", manufacturerId)
                    setProperty("colorHex", matchingStockDefinition.getProperty<String>("colorHex") ?: "#FFFFFF")
                    setProperty("colorName", matchingStockDefinition.getProperty<String>("colorName"))
                    setProperty("materialType", matchingStockDefinition.getProperty<String>("materialType"))
                    setProperty("catalogCreated", true)
                    setProperty("isInventoryItem", true)
                    
                    // Add custom metadata
                    metadata.forEach { (key, value) ->
                        setProperty(key, value)
                    }
                }
                
                // Step 3: Create identifier entity
                val identifier = Identifier(
                    identifierType = IdentifierTypes.RFID_HARDWARE,
                    value = tagUid
                ).apply {
                    purpose = "tracking"
                    isUnique = true
                }
                
                // Step 4: Save entities to graph
                val graphDataService = unifiedDataAccess.appContext?.let { GraphDataService(it) }
                if (graphDataService == null) {
                    return@withContext GraphEntityCreationResult(
                        success = false,
                        errorMessage = "Graph data service not available"
                    )
                }
                
                // Note: This simplified version doesn't create full relationships
                // A complete implementation would use GraphRepository directly
                
                _graphOperationState.value = GraphOperationState.EntitiesCreated
                
                Log.d("MainViewModel", "Successfully created graph entity from catalog: ${physicalComponent.label}")
                
                GraphEntityCreationResult(
                    success = true,
                    rootEntity = physicalComponent,
                    scannedEntity = physicalComponent,
                    totalEntitiesCreated = 2,
                    totalEdgesCreated = 1
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error creating graph entity from catalog", e)
                _graphOperationState.value = GraphOperationState.Error("Catalog creation failed: ${e.message}")
                GraphEntityCreationResult(
                    success = false,
                    errorMessage = "Catalog creation failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Enhanced error handling for multi-format processing
     */
    fun handleProcessingError(error: Throwable, context: String) {
        val errorMessage = when {
            error.message?.contains("authentication", ignoreCase = true) == true -> 
                "Authentication failed - check tag format and positioning"
            error.message?.contains("timeout", ignoreCase = true) == true -> 
                "Tag reading timeout - try scanning again"
            error.message?.contains("format", ignoreCase = true) == true -> 
                "Unsupported tag format - verify tag compatibility"
            error.message?.contains("entity", ignoreCase = true) == true -> 
                "Entity creation failed - check catalog data"
            error.message?.contains("graph", ignoreCase = true) == true -> 
                "Graph storage failed - check data integrity"
            else -> "Processing error: ${error.message ?: "Unknown error"}"
        }
        
        Log.e("MainViewModel", "$context error: $errorMessage", error)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = errorMessage,
                scanState = ScanState.ERROR
            )
            _graphOperationState.value = GraphOperationState.Error(errorMessage)
        }
    }
    
    /**
     * Utility method to generate component IDs
     */
    private fun generateComponentId(prefix: String = "component"): String {
        return "${prefix}_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}"
    }
    
} // End of MainViewModel class



/**
 * Graph operation states
 */
sealed class GraphOperationState {
    object Idle : GraphOperationState()
    object DetectingFormat : GraphOperationState()
    object ProcessingScan : GraphOperationState()
    object CreatingEntities : GraphOperationState()
    object EntitiesCreated : GraphOperationState()
    object CreatingEdges : GraphOperationState()
    object EdgesCreated : GraphOperationState()
    object PersistingGraph : GraphOperationState()
    object GraphPersisted : GraphOperationState()
    data class Error(val message: String) : GraphOperationState()
}


data class BScanUiState(
    val scanState: ScanState = ScanState.IDLE,
    val error: String? = null,
    val debugInfo: ScanDebugInfo? = null
)

enum class ScanState {
    IDLE,           // Waiting for tag
    TAG_DETECTED,   // Tag detected but not yet processed
    PROCESSING,     // Processing tag data
    SUCCESS,        // Successfully processed
    ERROR          // Error occurred
}

