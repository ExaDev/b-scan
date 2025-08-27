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
import com.bscan.interpreter.InterpreterFactory
import com.bscan.data.BambuProductDatabase
import com.bscan.detector.TagDetector
import com.bscan.service.ComponentFactory
import com.bscan.service.MassInferenceService
import com.bscan.service.ComponentGroupingService
import com.bscan.service.InferenceResult
import com.bscan.service.InferredComponent
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
    private val unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
    
    // Note: Repository access now unified through UnifiedDataAccess
    
    // InterpreterFactory for runtime interpretation (legacy support)
    private var interpreterFactory = InterpreterFactory(unifiedDataAccess)
    
    // Component architecture services
    private val tagDetector = TagDetector()
    private val massInferenceService = MassInferenceService(componentRepository)
    private val componentGroupingService = ComponentGroupingService(componentRepository)
    
    // Track simulation state to cycle through all products
    private var simulationProductIndex = 0
    
    fun onTagDetected() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.TAG_DETECTED,
            error = null
        )
        _scanProgress.value = ScanProgress(
            stage = ScanStage.TAG_DETECTED,
            percentage = 0.0f,
            statusMessage = "Tag detected"
        )
        
        // Show tag detected state for a brief moment before processing
        viewModelScope.launch {
            delay(300) // Show detection for 300ms
            if (_uiState.value.scanState == ScanState.TAG_DETECTED) {
                // Only proceed if still in TAG_DETECTED state (not cancelled)
                // The actual processTag call will update the state to PROCESSING
            }
        }
    }
    
    /**
     * Enhanced tag processing using Component Factory pattern
     */
    suspend fun processTagWithFactory(tag: Tag): ComponentCreationResult {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Processing tag with component factory pattern")
                _componentOperationState.value = ComponentOperationState.DetectingFormat
                
                // Step 1: Detect tag format
                val detectionResult = tagDetector.detectTag(tag)
                Log.d("MainViewModel", "Tag detected: ${detectionResult.tagFormat} (confidence: ${detectionResult.confidence})")
                
                if (detectionResult.confidence < 0.5f) {
                    Log.w("MainViewModel", "Low confidence detection: ${detectionResult.detectionReason}")
                    return@async ComponentCreationResult.FormatDetectionFailed(
                        reason = detectionResult.detectionReason,
                        confidence = detectionResult.confidence
                    )
                }
                
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.ProcessingTag
                }
                
                // This method returns the result - actual NFC reading will be handled separately
                ComponentCreationResult.FormatDetected(
                    format = detectionResult.tagFormat,
                    confidence = detectionResult.confidence,
                    manufacturerName = detectionResult.manufacturerName
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in processTagWithFactory", e)
                ComponentCreationResult.ProcessingError("Tag processing failed: ${e.message}")
            }
        }.await()
    }
    
    /**
     * Create components from scan data using the appropriate factory
     */
    suspend fun createComponentsFromScan(
        encryptedData: EncryptedScanData, 
        decryptedData: DecryptedScanData
    ): ComponentCreationResult {
        return viewModelScope.async(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Creating components from scan data")
                _componentOperationState.value = ComponentOperationState.CreatingComponents
                
                // Step 1: Detect format from scan data
                val detectionResult = tagDetector.detectFromData(
                    encryptedData.technology, 
                    encryptedData.encryptedData
                )
                
                if (detectionResult.confidence < 0.5f) {
                    return@async ComponentCreationResult.FormatDetectionFailed(
                        reason = detectionResult.detectionReason,
                        confidence = detectionResult.confidence
                    )
                }
                
                // Step 2: Create appropriate factory
                val factory = ComponentFactory.createFactory(getApplication(), encryptedData)
                Log.d("MainViewModel", "Using factory: ${factory.factoryType}")
                
                // Step 3: Process scan with factory
                val rootComponent = factory.processScan(encryptedData, decryptedData)
                
                if (rootComponent == null) {
                    return@async ComponentCreationResult.ComponentCreationFailed(
                        "Factory failed to create components"
                    )
                }
                
                // Step 4: Update component operation state
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.ComponentsCreated
                    _componentCreationResult.value = ComponentCreationResult.Success(
                        rootComponent = rootComponent,
                        factoryType = factory.factoryType,
                        tagFormat = detectionResult.tagFormat
                    )
                }
                
                Log.d("MainViewModel", "Successfully created component hierarchy with root: ${rootComponent.name}")
                ComponentCreationResult.Success(
                    rootComponent = rootComponent,
                    factoryType = factory.factoryType,
                    tagFormat = detectionResult.tagFormat
                )
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error creating components from scan", e)
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.Error(e.message ?: "Unknown error")
                }
                ComponentCreationResult.ProcessingError("Component creation failed: ${e.message}")
            }
        }.await()
    }
    
    /**
     * Enhanced method to process scan data using both legacy and new component architecture
     */
    fun processScanData(encryptedData: EncryptedScanData, decryptedData: DecryptedScanData) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    scanState = ScanState.PROCESSING,
                    error = null
                )
                _scanProgress.value = ScanProgress(
                    stage = ScanStage.PARSING,
                    percentage = 0.9f,
                    statusMessage = "Interpreting filament data"
                )
            }
            
            // Store the raw scan data and record the scan
            unifiedDataAccess.recordScan(encryptedData, decryptedData)
            
            // Enhanced processing: Try component factory architecture first
            if (decryptedData.scanResult == ScanResult.SUCCESS) {
                try {
                    val componentResult = createComponentsFromScan(encryptedData, decryptedData)
                    if (componentResult is ComponentCreationResult.Success) {
                        Log.d("MainViewModel", "Successfully created components using factory pattern")
                        // Factory succeeded, update state flows
                        withContext(Dispatchers.Main) {
                            _componentCreationResult.value = componentResult
                        }
                    } else {
                        Log.w("MainViewModel", "Component factory failed, falling back to legacy processing")
                        // Fall back to legacy inventory item creation
                        createOrUpdateInventoryItem(decryptedData)
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Component architecture failed, using legacy processing", e)
                    // Fall back to legacy processing on any error
                    createOrUpdateInventoryItem(decryptedData)
                }
            }
            
            val result = if (decryptedData.scanResult == ScanResult.SUCCESS) {
                try {
                    // Use FilamentInterpreter to convert decrypted data to FilamentInfo
                    val filamentInfo = interpreterFactory.interpret(decryptedData)
                    if (filamentInfo != null) {
                        TagReadResult.Success(filamentInfo)
                    } else {
                        TagReadResult.InvalidTag
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    TagReadResult.ReadError
                }
            } else {
                // Map scan result to TagReadResult
                when (decryptedData.scanResult) {
                    ScanResult.AUTHENTICATION_FAILED -> TagReadResult.ReadError
                    ScanResult.INSUFFICIENT_DATA -> TagReadResult.InsufficientData
                    ScanResult.PARSING_FAILED -> TagReadResult.InvalidTag
                    else -> TagReadResult.ReadError
                }
            }
            withContext(Dispatchers.Main) {
                _scanProgress.value = when (result) {
                    is TagReadResult.Success -> ScanProgress(
                        stage = ScanStage.COMPLETED,
                        percentage = 1.0f,
                        statusMessage = "Scan completed successfully"
                    )
                    is TagReadResult.InvalidTag -> ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Invalid or unsupported tag"
                    )
                    is TagReadResult.ReadError -> ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Error reading or authenticating tag"
                    )
                    is TagReadResult.InsufficientData -> ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Insufficient data on tag"
                    )
                    else -> ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Unknown error occurred"
                    )
                }
                
                _uiState.value = when (result) {
                    is TagReadResult.Success -> BScanUiState(
                        filamentInfo = result.filamentInfo,
                        scanState = ScanState.SUCCESS,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                    is TagReadResult.InvalidTag -> BScanUiState(
                        error = "Invalid or unsupported tag",
                        scanState = ScanState.ERROR,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                    is TagReadResult.ReadError -> BScanUiState(
                        error = "Error reading or authenticating tag", 
                        scanState = ScanState.ERROR,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                    is TagReadResult.InsufficientData -> BScanUiState(
                        error = "Insufficient data on tag",
                        scanState = ScanState.ERROR,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                    else -> BScanUiState(
                        error = "Unknown error occurred",
                        scanState = ScanState.ERROR,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                }
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
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.PROCESSING,
            error = null
        )
    }
    
    fun updateScanProgress(progress: ScanProgress) {
        _scanProgress.value = progress
    }
    
    fun setNfcError(error: String) {
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
            
            // Try to detect format even for failed scans (for debugging)
            try {
                val detectionResult = tagDetector.detectFromData(tagData.technology, byteArrayOf())
                Log.d("MainViewModel", "Failed scan format detection: ${detectionResult.tagFormat} (${detectionResult.confidence})")
                
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.Error(
                        "Authentication failed for ${detectionResult.tagFormat} tag"
                    )
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Error detecting format for failed scan", e)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            error = "Authentication failed - see debug info below",
            scanState = ScanState.ERROR,
            debugInfo = createDebugInfoFromDecryptedData(decryptedData)
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
        
        // Clear component operation errors as well
        if (_componentOperationState.value is ComponentOperationState.Error) {
            _componentOperationState.value = ComponentOperationState.Idle
        }
    }
    
    fun resetScan() {
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
            
            val stages = listOf(
                ScanProgress(ScanStage.TAG_DETECTED, 0.0f, statusMessage = "Tag detected"),
                ScanProgress(ScanStage.CONNECTING, 0.05f, statusMessage = "Connecting to tag"),
                ScanProgress(ScanStage.KEY_DERIVATION, 0.1f, statusMessage = "Deriving keys"),
            )
            
            // Simulate authenticating sectors
            for (sector in 0..15) {
                _scanProgress.value = ScanProgress(
                    stage = ScanStage.AUTHENTICATING,
                    percentage = 0.15f + (sector * 0.04f), // 15% to 75%
                    currentSector = sector,
                    statusMessage = "Authenticating sector ${sector + 1}/16"
                )
                delay(80) // Short delay per sector
            }
            
            // Reading blocks
            _scanProgress.value = ScanProgress(
                stage = ScanStage.READING_BLOCKS,
                percentage = 0.8f,
                statusMessage = "Reading data blocks"
            )
            delay(200)
            
            // Assembling data
            _scanProgress.value = ScanProgress(
                stage = ScanStage.ASSEMBLING_DATA,
                percentage = 0.85f,
                statusMessage = "Assembling data"
            )
            delay(150)
            
            // Parsing
            _scanProgress.value = ScanProgress(
                stage = ScanStage.PARSING,
                percentage = 0.9f,
                statusMessage = "Parsing filament data"
            )
            delay(200)
            
            // Complete with mock data - cycle through all products
            val allProducts = BambuProductDatabase.getAllProducts()
            val product = allProducts[simulationProductIndex % allProducts.size]
            simulationProductIndex = (simulationProductIndex + 1) % allProducts.size
            
            val mockFilamentInfo = FilamentInfo(
                tagUid = "MOCK${System.currentTimeMillis()}",
                trayUid = "MOCK_TRAY_${String.format("%03d", (simulationProductIndex / 2) + 1)}",
                filamentType = product.productLine,
                detailedFilamentType = product.productLine,
                colorHex = product.colorHex,
                colorName = product.colorName,
                spoolWeight = if (product.mass == "1kg") 1000 else if (product.mass == "0.5kg") 500 else 1000,
                filamentDiameter = 1.75f,
                filamentLength = kotlin.random.Random.nextInt(100000, 500000),
                productionDate = "2024-${kotlin.random.Random.nextInt(1, 13).toString().padStart(2, '0')}",
                minTemperature = this@MainViewModel.getDefaultMinTemp(product.productLine),
                maxTemperature = this@MainViewModel.getDefaultMaxTemp(product.productLine),
                bedTemperature = this@MainViewModel.getDefaultBedTemp(product.productLine),
                dryingTemperature = this@MainViewModel.getDefaultDryingTemp(product.productLine),
                dryingTime = this@MainViewModel.getDefaultDryingTime(product.productLine),
                bambuProduct = product
            )
            
            _scanProgress.value = ScanProgress(
                stage = ScanStage.COMPLETED,
                percentage = 1.0f,
                statusMessage = "Scan completed successfully"
            )
            
            _uiState.value = BScanUiState(
                filamentInfo = mockFilamentInfo,
                scanState = ScanState.SUCCESS,
                debugInfo = null
            )
        }
    }
    
    // Expose unified data access for UI access
    fun getUnifiedDataAccess() = unifiedDataAccess
    
    // Expose user data repository for direct access where needed
    fun getUserDataRepository() = userDataRepository
    
    // Expose component repository for component architecture
    fun getComponentRepository() = componentRepository
    
    // Inventory tracking methods with proper reactive flows
    fun getInventoryItems() = unifiedDataAccess.getInventoryItems()
    
    fun getInventoryItem(trayUid: String) = unifiedDataAccess.getInventoryItem(trayUid)
    
    fun saveInventoryItem(item: InventoryItem) = unifiedDataAccess.saveInventoryItem(item)
    
    // Mappings access methods
    fun getManufacturers() = unifiedDataAccess.getAllManufacturers()
    
    fun getManufacturer(manufacturerId: String) = unifiedDataAccess.getManufacturer(manufacturerId)
    
    fun getProducts(manufacturerId: String) = unifiedDataAccess.getProducts(manufacturerId)
    
    /**
     * Refresh the FilamentInterpreter with updated mappings
     */
    fun refreshMappings() {
        interpreterFactory.refreshMappings()
    }
    
    /**
     * Integrate BLE scale reading for mass inference
     */
    fun integrateMassReading(scaleReading: Float, unit: String = "g") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Integrating mass reading: ${scaleReading}${unit}")
                
                // Get current component creation result
                val currentResult = _componentCreationResult.value
                if (currentResult !is ComponentCreationResult.Success) {
                    Log.w("MainViewModel", "No components available for mass integration")
                    return@launch
                }
                
                // Use mass inference service to distribute mass across components
                val massInferenceResult = massInferenceService.inferComponentMass(
                    parentComponent = currentResult.rootComponent,
                    totalMeasuredMass = scaleReading
                )
                
                withContext(Dispatchers.Main) {
                    _massInferenceResult.value = when (massInferenceResult) {
                        is InferenceResult.Success -> MassInferenceResult.Success(
                            updatedComponents = massInferenceResult.inferredComponents.map { it.component },
                            totalMass = massInferenceResult.totalMeasuredMass,
                            inferredMass = massInferenceResult.inferredComponents.sumOf { it.inferredMass.toDouble() }.toFloat()
                        )
                        is InferenceResult.Warning -> MassInferenceResult.PartialSuccess(
                            updatedComponents = massInferenceResult.inferredComponents.map { it.component },
                            failedComponents = massInferenceResult.validationWarnings,
                            totalMass = massInferenceResult.totalMeasuredMass
                        )
                        is InferenceResult.Error -> MassInferenceResult.Error(massInferenceResult.message)
                    }
                    
                    if (massInferenceResult is InferenceResult.Success) {
                        Log.d("MainViewModel", "Successfully inferred masses for ${massInferenceResult.inferredComponents.size} components")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error integrating mass reading", e)
                withContext(Dispatchers.Main) {
                    _massInferenceResult.value = MassInferenceResult.Error("Mass integration failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Create or update component relationships using ComponentGroupingService
     */
    fun updateComponentRelationships(parentId: String, childIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("MainViewModel", "Updating component relationships: parent=$parentId, children=${childIds.size}")
                _componentOperationState.value = ComponentOperationState.UpdatingRelationships
                
                // Use hierarchical grouping to establish parent-child relationships
                val groupingResult = componentGroupingService.createHierarchicalGroup(
                    parentId = parentId,
                    childIds = childIds
                )
                val success = groupingResult.success
                
                withContext(Dispatchers.Main) {
                    if (success) {
                        _componentOperationState.value = ComponentOperationState.RelationshipsUpdated
                        Log.d("MainViewModel", "Successfully updated component relationships")
                    } else {
                        _componentOperationState.value = ComponentOperationState.Error("Failed to update relationships")
                        Log.e("MainViewModel", "Failed to update component relationships")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error updating component relationships", e)
                withContext(Dispatchers.Main) {
                    _componentOperationState.value = ComponentOperationState.Error("Relationship update failed: ${e.message}")
                }
            }
        }
    }
    
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
                    uniqueIdentifier = tagUid,
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
    
    /**
     * Create or update inventory item with proper component setup for successful scans
     */
    private suspend fun createOrUpdateInventoryItem(decryptedData: DecryptedScanData) {
        // Try to interpret the scan to get trayUid
        val filamentInfo = interpreterFactory.interpret(decryptedData)
        val trayUid = filamentInfo?.trayUid ?: return
        
        // Check if inventory item already exists
        val existingItem = unifiedDataAccess.getInventoryItem(trayUid)
        if (existingItem == null) {
            // Create new inventory item with default components for Bambu Lab
            try {
                val manufacturerId = "bambu_lab"
                val filamentType = detectFilamentTypeFromScan(decryptedData)
                
                if (filamentType != null) {
                    unifiedDataAccess.createInventoryItemWithComponents(
                        trayUid = trayUid,
                        manufacturerId = manufacturerId,
                        filamentType = filamentType
                    )
                    Log.d("MainViewModel", "Created inventory item with components for tray $trayUid")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to create inventory item for tray $trayUid", e)
            }
        }
    }
    
    /**
     * Detect filament type from scan data for component creation
     */
    private fun detectFilamentTypeFromScan(decryptedData: DecryptedScanData): String? {
        // Try to extract material type from the decrypted blocks
        // This is a simplified version - in reality this would use the interpreters
        decryptedData.decryptedBlocks.forEach { (blockNumber, hexData) ->
            // Convert hex string to bytes for text analysis
            try {
                val bytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                val dataStr = String(bytes, Charsets.UTF_8).trim('\u0000')
                when {
                    dataStr.contains("PLA", ignoreCase = true) -> return "pla_basic"
                    dataStr.contains("ABS", ignoreCase = true) -> return "abs_basic"
                    dataStr.contains("PETG", ignoreCase = true) -> return "petg_basic"
                    dataStr.contains("TPU", ignoreCase = true) -> return "tpu_95a"
                    dataStr.contains("ASA", ignoreCase = true) -> return "asa_basic"
                    dataStr.contains("PC", ignoreCase = true) -> return "pc_basic"
                    dataStr.contains("PA", ignoreCase = true) -> return "pa_cf"
                }
            } catch (e: Exception) {
                // Skip this block if hex conversion fails
            }
        }
        
        // Default to PLA if we can't detect the type
        return "pla_basic"
    }
    
    /**
     * Get default printing temperatures based on material type
     */
    private fun getDefaultMinTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 190
        materialType.contains("ABS") -> 220
        materialType.contains("PETG") -> 220
        materialType.contains("TPU") -> 200
        else -> 190
    }
    
    private fun getDefaultMaxTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 220
        materialType.contains("ABS") -> 250
        materialType.contains("PETG") -> 250
        materialType.contains("TPU") -> 230
        else -> 220
    }
    
    private fun getDefaultBedTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 60
        materialType.contains("ABS") -> 80
        materialType.contains("PETG") -> 70
        materialType.contains("TPU") -> 50
        else -> 60
    }
    
    private fun getDefaultDryingTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 45
        materialType.contains("ABS") -> 60
        materialType.contains("PETG") -> 65
        materialType.contains("TPU") -> 40
        else -> 45
    }
    
    private fun getDefaultDryingTime(materialType: String): Int = when {
        materialType.contains("TPU") -> 12
        materialType.contains("PETG") -> 8
        materialType.contains("ABS") -> 4
        else -> 6
    }
} // End of MainViewModel class

/**
 * Component creation results for the factory pattern
 */
sealed class ComponentCreationResult {
    data class Success(
        val rootComponent: Component,
        val factoryType: String,
        val tagFormat: TagFormat
    ) : ComponentCreationResult()
    
    data class FormatDetected(
        val format: TagFormat,
        val confidence: Float,
        val manufacturerName: String?
    ) : ComponentCreationResult()
    
    data class FormatDetectionFailed(
        val reason: String,
        val confidence: Float
    ) : ComponentCreationResult()
    
    data class ComponentCreationFailed(
        val reason: String
    ) : ComponentCreationResult()
    
    data class ProcessingError(
        val message: String
    ) : ComponentCreationResult()
}

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
    val filamentInfo: FilamentInfo? = null,
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
    val filamentInfo: FilamentInfo? = null,
    val rootComponent: Component? = null,
    val componentHierarchy: List<Component>? = null,
    val scanState: ScanState = ScanState.IDLE,
    val componentOperationState: ComponentOperationState = ComponentOperationState.Idle,
    val tagFormat: TagFormat? = null,
    val factoryType: String? = null,
    val error: String? = null,
    val debugInfo: ScanDebugInfo? = null
)
