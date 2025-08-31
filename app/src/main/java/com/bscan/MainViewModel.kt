package com.bscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.ComponentRepository
import com.bscan.repository.ScanHistoryRepository
import com.bscan.service.ComponentFactory
import com.bscan.service.ComponentGenerationService
import com.bscan.service.ScanningService
import com.bscan.service.DefaultScanningService
import com.bscan.service.BambuComponentFactory
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
    
    // Component factory architecture state flows
    private val _componentCreationResult = MutableStateFlow<ComponentCreationResult?>(null)
    val componentCreationResult: StateFlow<ComponentCreationResult?> = _componentCreationResult.asStateFlow()
    
    private val _componentOperationState = MutableStateFlow<ComponentOperationState>(ComponentOperationState.Idle)
    val componentOperationState: StateFlow<ComponentOperationState> = _componentOperationState.asStateFlow()
    
    private val _massInferenceResult = MutableStateFlow<MassInferenceResult?>(null)
    val massInferenceResult: StateFlow<MassInferenceResult?> = _massInferenceResult.asStateFlow()
    
    private val catalogRepository = CatalogRepository(application)
    private val userDataRepository = UserDataRepository(application)
    private val componentRepository = ComponentRepository(application)
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val componentFactory = BambuComponentFactory(application)
    private val unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
    
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
                
                // Generate components on-demand for navigation
                withContext(Dispatchers.IO) {
                    try {
                        val componentGenerationService = ComponentGenerationService(getApplication())
                        val generatedComponents = componentGenerationService.generateComponentsFromScans(listOf(decryptedData))
                        
                        // Get the first inventory item (root component) for navigation
                        val rootComponent = generatedComponents.firstOrNull { it.isInventoryItem }
                            ?: generatedComponents.firstOrNull()
                        
                        if (rootComponent != null) {
                            _componentCreationResult.value = ComponentCreationResult.Success(
                                rootComponent = rootComponent,
                                factoryType = "ComponentGenerationService",
                                tagFormat = decryptedData.tagFormat
                            )
                            Log.d("MainViewModel", "Generated component for navigation: ${rootComponent.name}")
                        } else {
                            Log.w("MainViewModel", "No components generated from scan data")
                        }
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to generate components for navigation", e)
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
        
        // Reset component state on NFC errors
        resetComponentState()
        _componentOperationState.value = ComponentOperationState.Error(error)
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
        
        _componentOperationState.value = ComponentOperationState.Error("Authentication failed")
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        
        // Clear component operation errors as well
        if (_componentOperationState.value is ComponentOperationState.Error) {
            _componentOperationState.value = ComponentOperationState.Idle
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
            
            // Store mock component creation result
            _componentCreationResult.value = ComponentCreationResult.Success(
                rootComponent = mockTrayComponent,
                factoryType = "MockSimulation",
                tagFormat = TagFormat.BAMBU_PROPRIETARY
            )
        }
    }
    
    // Expose unified data access for UI access
    fun getUnifiedDataAccess() = unifiedDataAccess
    
    // Expose user data repository for direct access where needed
    fun getUserDataRepository() = userDataRepository
    
    // Expose component repository for component architecture
    fun getComponentRepository() = componentRepository
    
    // Legacy inventory methods removed - use on-demand component generation instead
    
    // Mappings access methods
    fun getManufacturers() = unifiedDataAccess.getAllManufacturers()
    
    fun getManufacturer(manufacturerId: String) = unifiedDataAccess.getManufacturer(manufacturerId)
    
    fun getProducts(manufacturerId: String) = unifiedDataAccess.getProducts(manufacturerId)
    
    // Removed refreshMappings() - no longer using interpreterFactory
    
    // Removed integrateMassReading() - mass inference moved to scanning service
    
    // Removed updateComponentRelationships() - component grouping moved to scanning service
    
    /**
     * Calculate total hierarchy mass for a component
     */
    suspend fun calculateHierarchyMass(componentId: String): Float {
        return withContext(Dispatchers.IO) {
            try {
                val component = componentRepository.getComponent(componentId)
                if (component == null) {
                    Log.w("MainViewModel", "Component not found: $componentId")
                    return@withContext 0f
                }
                
                // Calculate mass manually for now - service doesn't have calculateTotalMass method
                var totalMass = component.massGrams ?: 0f
                component.childComponents.forEach { childId ->
                    val childComponent = componentRepository.getComponent(childId)
                    totalMass += childComponent?.massGrams ?: 0f
                }
                totalMass
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error calculating hierarchy mass", e)
                0f
            }
        }
    }
    
    /**
     * Reset component architecture state
     */
    fun resetComponentState() {
        _componentCreationResult.value = null
        _componentOperationState.value = ComponentOperationState.Idle
        _massInferenceResult.value = null
    }
    
    /**
     * Get components by category for UI filtering
     */
    fun getComponentsByCategory(category: String) = componentRepository.getComponentsByCategory(category)
    
    /**
     * Get all root components (inventory items)
     */
    fun getRootComponents() = componentRepository.getComponents().filter { it.isInventoryItem }
    
    /**
     * Search components by tags
     */
    fun getComponentsByTags(tags: List<String>) = componentRepository.getComponentsByTags(tags)
    
    /**
     * Catalog-driven component creation workflow
     */
    suspend fun createComponentFromCatalog(
        tagUid: String,
        manufacturerId: String,
        skuId: String,
        metadata: Map<String, String> = emptyMap()
    ): ComponentCreationResult {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Creating component from catalog: $manufacturerId/$skuId")
                _componentOperationState.value = ComponentOperationState.CreatingComponents
                
                // Step 1: Lookup SKU data
                val products = catalogRepository.getProducts(manufacturerId)
                val matchingProduct = products.firstOrNull { it.variantId == skuId }
                
                if (matchingProduct == null) {
                    return@async ComponentCreationResult.ComponentCreationFailed(
                        "SKU not found in catalog: $manufacturerId/$skuId"
                    )
                }
                
                // Step 2: Create component based on catalog data
                val component = Component(
                    id = generateComponentId("catalog"),
                    identifiers = listOf(
                        ComponentIdentifier(
                            type = IdentifierType.RFID_HARDWARE,
                            value = tagUid,
                            purpose = IdentifierPurpose.TRACKING
                        )
                    ),
                    name = matchingProduct.productName,
                    category = "filament",
                    tags = listOf(
                        manufacturerId.lowercase(),
                        matchingProduct.materialType.lowercase(),
                        "catalog-created",
                        "consumable",
                        "variable-mass"
                    ),
                    parentComponentId = null,
                    childComponents = emptyList(),
                    massGrams = matchingProduct.filamentWeightGrams,
                    variableMass = true,
                    manufacturer = matchingProduct.manufacturer,
                    description = "${matchingProduct.materialType} filament in ${matchingProduct.colorName}",
                    metadata = metadata + mapOf(
                        "sku" to skuId,
                        "manufacturerId" to manufacturerId,
                        "colorHex" to (matchingProduct.colorHex ?: "#FFFFFF"),
                        "colorName" to matchingProduct.colorName,
                        "materialType" to matchingProduct.materialType,
                        "catalogCreated" to "true",
                        "createdTimestamp" to System.currentTimeMillis().toString()
                    ),
                    createdAt = System.currentTimeMillis(),
                    lastUpdated = java.time.LocalDateTime.now()
                )
                
                // Step 3: Save component
                componentRepository.saveComponent(component)
                
                // Step 4: Update state
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.ComponentsCreated
                    _componentCreationResult.value = ComponentCreationResult.Success(
                        rootComponent = component,
                        factoryType = "CatalogFactory",
                        tagFormat = TagFormat.UNKNOWN // Catalog-created, no specific tag format
                    )
                }
                
                Log.d("MainViewModel", "Successfully created component from catalog: ${component.name}")
                ComponentCreationResult.Success(
                    rootComponent = component,
                    factoryType = "CatalogFactory",
                    tagFormat = TagFormat.UNKNOWN
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error creating component from catalog", e)
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.Error("Catalog creation failed: ${e.message}")
                }
                ComponentCreationResult.ProcessingError("Catalog creation failed: ${e.message}")
            }
        }.await()
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
            error.message?.contains("component", ignoreCase = true) == true -> 
                "Component creation failed - check catalog data"
            else -> "Processing error: ${error.message ?: "Unknown error"}"
        }
        
        Log.e("MainViewModel", "$context error: $errorMessage", error)
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = errorMessage,
                scanState = ScanState.ERROR
            )
            _componentOperationState.value = ComponentOperationState.Error(errorMessage)
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
 * Component operation states
 */
sealed class ComponentOperationState {
    object Idle : ComponentOperationState()
    object DetectingFormat : ComponentOperationState()
    object ProcessingTag : ComponentOperationState()
    object CreatingComponents : ComponentOperationState()
    object ComponentsCreated : ComponentOperationState()
    object UpdatingRelationships : ComponentOperationState()
    object RelationshipsUpdated : ComponentOperationState()
    data class Error(val message: String) : ComponentOperationState()
}

/**
 * Mass inference results
 */
sealed class MassInferenceResult {
    data class Success(
        val updatedComponents: List<Component>,
        val totalMass: Float,
        val inferredMass: Float
    ) : MassInferenceResult()
    
    data class PartialSuccess(
        val updatedComponents: List<Component>,
        val failedComponents: List<String>,
        val totalMass: Float
    ) : MassInferenceResult()
    
    data class Error(
        val message: String
    ) : MassInferenceResult()
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

/**
 * Enhanced UI state with component architecture support
 */
data class ComponentEnhancedUIState(
    val rootComponent: Component? = null,
    val componentHierarchy: List<Component>? = null,
    val scanState: ScanState = ScanState.IDLE,
    val componentOperationState: ComponentOperationState = ComponentOperationState.Idle,
    val tagFormat: TagFormat? = null,
    val factoryType: String? = null,
    val error: String? = null,
    val debugInfo: ScanDebugInfo? = null
)
