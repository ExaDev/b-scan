package com.bscan.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.FilamentReelDetails
import com.bscan.repository.UniqueFilamentReel
import com.bscan.repository.InterpretedScan
import com.bscan.repository.ComponentRepository
import com.bscan.ui.screens.home.SkuInfo
import com.bscan.model.Component
import com.bscan.model.ComponentIdentifier
import com.bscan.model.ComponentMeasurement
import com.bscan.model.MeasurementType
import com.bscan.service.ComponentGroupingService
import com.bscan.service.MassInferenceService
import com.bscan.service.ComponentFactory
import com.bscan.ble.ScaleReading
import com.bscan.ble.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

enum class DetailType {
    SCAN, TAG, INVENTORY_STOCK, SKU, COMPONENT
}

data class DetailUiState(
    val isLoading: Boolean = true,
    val detailType: DetailType = DetailType.INVENTORY_STOCK,
    val identifier: String = "",
    val error: String? = null,
    // Primary entity data
    val primaryScan: InterpretedScan? = null,
    val primaryTag: InterpretedScan? = null, // Most recent scan for this tag UID
    val primarySpool: FilamentReelDetails? = null,
    val primarySku: SkuInfo? = null,
    val primaryComponent: Component? = null,
    // Component hierarchy data
    val parentComponent: Component? = null,
    val childComponents: List<Component> = emptyList(),
    val siblingComponents: List<Component> = emptyList(),
    val hierarchyExpanded: Map<String, Boolean> = emptyMap(),
    val componentMeasurements: List<ComponentMeasurement> = emptyList(),
    val totalMass: Float? = null,
    val inferredMasses: Map<String, Float> = emptyMap(), // Component ID to inferred mass
    // Relationship state
    val isAddingChild: Boolean = false,
    val isEditingRelationships: Boolean = false,
    val massInferenceInProgress: Boolean = false,
    val lastScaleReading: ScaleReading? = null,
    // Undo/Redo state
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val pendingOperations: List<String> = emptyList(),
    // Related data
    val relatedScans: List<InterpretedScan> = emptyList(),
    val relatedTags: List<String> = emptyList(), // Tag UIDs
    val relatedFilamentReels: List<UniqueFilamentReel> = emptyList(),
    val relatedSkus: List<SkuInfo> = emptyList()
)

class DetailViewModel(
    private val unifiedDataAccess: UnifiedDataAccess,
    private val componentRepository: ComponentRepository? = null,
    private val componentGroupingService: ComponentGroupingService? = null,
    private val massInferenceService: MassInferenceService? = null
) : ViewModel() {
    
    // Secondary constructor for backward compatibility
    constructor(unifiedDataAccess: UnifiedDataAccess) : this(
        unifiedDataAccess = unifiedDataAccess,
        componentRepository = null,
        componentGroupingService = null,
        massInferenceService = null
    )
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    // Undo/Redo state management
    private val undoStack = mutableListOf<UndoableOperation>()
    private val redoStack = mutableListOf<UndoableOperation>()
    private val maxUndoStackSize = 20
    
    // Component relationship state
    private var currentOperationId: String? = null
    
    fun loadDetails(detailType: DetailType, identifier: String) {
        Log.d("DetailViewModel", "Loading details for type: $detailType, identifier: $identifier")
        
        // Validate inputs before proceeding
        if (identifier.isBlank()) {
            Log.e("DetailViewModel", "Empty identifier provided for detail type: $detailType")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid identifier provided"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            detailType = detailType,
            identifier = identifier,
            error = null
        )
        
        viewModelScope.launch {
            try {
                Log.d("DetailViewModel", "Starting load operation for $detailType")
                
                withContext(Dispatchers.IO) {
                    when (detailType) {
                        DetailType.SCAN -> loadScanDetails(identifier)
                        DetailType.TAG -> loadTagDetails(identifier)
                        DetailType.INVENTORY_STOCK -> loadSpoolDetails(identifier)
                        DetailType.SKU -> loadSkuDetails(identifier)
                        DetailType.COMPONENT -> loadComponentDetails(identifier)
                    }
                }
                
                Log.d("DetailViewModel", "Successfully loaded details for $detailType")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to load details for $detailType", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load details: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    private suspend fun loadScanDetails(scanIdentifier: String) {
        // Scan identifier format: "formatted_timestamp_uid"
        val parts = scanIdentifier.split("_")
        if (parts.size < 2) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid scan identifier"
            )
            return
        }
        
        val uid = parts.last()
        val timestampStr = parts.dropLast(1).joinToString("_")
        
        val allScans = unifiedDataAccess.getAllScans()
        
        // Try to find scan by generating the same ID format and comparing
        val primaryScan = allScans.find { scan ->
            val generatedId = "${scan.timestamp.toString().replace(":", "-").replace(".", "-")}_${scan.uid}"
            generatedId == scanIdentifier
        } ?: allScans.find { scan ->
            // Fallback: match by UID only if exact match fails
            scan.uid == uid
        }
        
        if (primaryScan == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Scan not found: $scanIdentifier"
            )
            return
        }
        
        // Get related data
        val trayUid = primaryScan.filamentInfo?.trayUid
        val relatedSpools = if (trayUid != null) {
            unifiedDataAccess.getFilamentReelDetails(trayUid)?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        
        val relatedTags = if (trayUid != null) {
            allScans
                .filter { it.filamentInfo?.trayUid == trayUid }
                .map { it.uid }
                .distinct()
        } else {
            listOf(uid)
        }
        
        val relatedScans = allScans.filter { it.uid == uid }
        val associatedSku = getAssociatedSku(primaryScan.filamentInfo)
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primaryScan = primaryScan,
            relatedScans = relatedScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = associatedSku
        )
    }
    
    private suspend fun loadTagDetails(tagUid: String) {
        val allScans = unifiedDataAccess.getAllScans()
        val tagScans = unifiedDataAccess.getScansByTagUid(tagUid)
        val primaryTag = tagScans.maxByOrNull { it.timestamp }
        
        if (primaryTag == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Tag not found"
            )
            return
        }
        
        // Get related data
        val trayUid = primaryTag.filamentInfo?.trayUid
        val relatedSpools = if (trayUid != null) {
            unifiedDataAccess.getFilamentReelDetails(trayUid)?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        
        val relatedTags = if (trayUid != null) {
            allScans
                .filter { it.filamentInfo?.trayUid == trayUid }
                .map { it.uid }
                .distinct()
        } else {
            listOf(tagUid)
        }
        
        val associatedSku = getAssociatedSku(primaryTag.filamentInfo)
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primaryTag = primaryTag,
            relatedScans = tagScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = associatedSku
        )
    }
    
    private suspend fun loadSpoolDetails(trayUid: String) {
        Log.d("DetailViewModel", "Loading spool details for trayUid: '$trayUid'")
        
        // Debug: List all available data
        val allScans = unifiedDataAccess.getAllScans()
        Log.d("DetailViewModel", "Available scan count: ${allScans.size}")
        allScans.take(5).forEach { scan ->
            Log.d("DetailViewModel", "Available scan - uid: '${scan.uid}', trayUid: '${scan.filamentInfo?.trayUid}', colorName: '${scan.filamentInfo?.colorName}'")
        }
        
        // Debug: Show scan data patterns to understand the identifier mismatch
        Log.d("DetailViewModel", "Looking for data matching identifier: '$trayUid'")
        
        try {
            val filamentReelDetails = unifiedDataAccess.getFilamentReelDetails(trayUid)
            
            if (filamentReelDetails == null) {
                Log.w("DetailViewModel", "No filament reel found for trayUid: '$trayUid'")
                
                // Try component system approach - maybe the ID is for an inventory item
                val inventoryItem = unifiedDataAccess.getInventoryItem(trayUid)
                if (inventoryItem != null) {
                    Log.d("DetailViewModel", "Found inventory item for ID: $trayUid, redirecting to component view")
                    
                    // Handle inventory item with component list (component IDs)
                    if (inventoryItem.hasComponents && componentRepository != null) {
                        // Try to find a root component from the component list
                        val rootComponentId = withContext(Dispatchers.IO) {
                            inventoryItem.components.firstOrNull { componentId ->
                                val component = componentRepository!!.getComponent(componentId)
                                component?.isRootComponent == true
                            }
                        }
                        
                        if (rootComponentId != null) {
                            // Switch to component detail mode
                            _uiState.value = _uiState.value.copy(
                                isLoading = true,
                                detailType = DetailType.COMPONENT
                            )
                            loadComponentDetails(rootComponentId)
                            return
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Found inventory item with ${inventoryItem.components.size} component IDs but no accessible root component."
                            )
                            return
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Found inventory item but component functionality not available or no components defined."
                        )
                        return
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Inventory item not found for ID: $trayUid"
                )
                return
            }
            
            Log.d("DetailViewModel", "Found filament reel details, loading associated data")
            
            // Safely get associated SKU with error handling
            val associatedSku = try {
                getAssociatedSku(filamentReelDetails.filamentInfo)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to get associated SKU for filament reel", e)
                emptyList()
            }
            
            // Safely convert to unique component representation
            val uniqueComponent = try {
                filamentReelDetailsToUniqueComponent(filamentReelDetails)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to convert filament reel details to unique component", e)
                null
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                primarySpool = filamentReelDetails,
                relatedScans = filamentReelDetails.allScans,
                relatedTags = filamentReelDetails.tagUids,
                relatedFilamentReels = if (uniqueComponent != null) listOf(uniqueComponent) else emptyList(),
                relatedSkus = associatedSku
            )
            
            Log.d("DetailViewModel", "Successfully loaded filament reel details with ${filamentReelDetails.allScans.size} related scans")
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Exception loading filament reel details", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load filament reel details: ${e.message}"
            )
        }
    }
    
    private suspend fun loadSkuDetails(skuKey: String) {
        val allScans = unifiedDataAccess.getAllScans()
        
        // Parse the SKU key to understand what we're looking for
        val parts = skuKey.split("-")
        val filamentType = parts.getOrNull(0) ?: ""
        val colorName = parts.getOrNull(1) ?: ""
        
        // Find scans that match this SKU pattern - include ALL scans, not just successful ones
        // This ensures we show actual scanned data even for incomplete/failed scans
        var skuScans = allScans.filter { scan ->
            scan.filamentInfo?.let { info ->
                val scanKey = "${info.filamentType}-${info.colorName}"
                scanKey == skuKey
            } ?: false
        }
        
        // If still no exact matches, try more flexible matching for incomplete data
        if (skuScans.isEmpty()) {
            skuScans = allScans.filter { scan ->
                scan.filamentInfo?.let { info ->
                    // Match if filament type matches and color is empty/null (for cases like "PLA-")
                    if (colorName.isEmpty() || colorName.isBlank()) {
                        info.filamentType == filamentType && (info.colorName.isEmpty() || info.colorName.isBlank())
                    } else {
                        // Try partial matching
                        info.filamentType.contains(filamentType, ignoreCase = true) ||
                        info.colorName.contains(colorName, ignoreCase = true)
                    }
                } ?: false
            }
        }
        
        // Even broader search - try to find any scan with similar filament type
        if (skuScans.isEmpty() && filamentType.isNotEmpty()) {
            skuScans = allScans.filter { scan ->
                scan.filamentInfo?.filamentType?.contains(filamentType, ignoreCase = true) == true
            }
        }
        
        // Final fallback - if we still have nothing, create a synthetic entry from the SKU key itself
        if (skuScans.isEmpty()) {
            // We'll create a minimal SkuInfo based on the parsed key information
            val syntheticFilamentInfo = createSyntheticFilamentInfo(filamentType, colorName)
            val primarySku = SkuInfo(
                skuKey = skuKey,
                filamentInfo = syntheticFilamentInfo,
                filamentReelCount = 0,
                totalScans = 0,
                successfulScans = 0,
                lastScanned = LocalDateTime.now(),
                successRate = 0f
            )
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                primarySku = primarySku,
                relatedScans = emptyList(),
                relatedTags = emptyList(),
                relatedFilamentReels = emptyList(),
                relatedSkus = listOf(primarySku)
            )
            return
        }
        
        val mostRecentScan = skuScans.maxByOrNull { it.timestamp }!!
        val filamentInfo = mostRecentScan.filamentInfo!!
        
        val uniqueFilamentReels = skuScans.groupBy { it.filamentInfo!!.trayUid }.size
        val totalScans = skuScans.size
        val successfulScans = skuScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
        val lastScanned = skuScans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
        
        val primarySku = SkuInfo(
            skuKey = skuKey,
            filamentInfo = filamentInfo,
            filamentReelCount = uniqueFilamentReels,
            totalScans = totalScans,
            successfulScans = successfulScans,
            lastScanned = lastScanned,
            successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
        )
        
        // Get related spools and tags
        val relatedSpools = skuScans
            .groupBy { it.filamentInfo!!.trayUid }
            .mapNotNull { (trayUid, scans) ->
                unifiedDataAccess.getFilamentReelDetails(trayUid)
            }
        
        val relatedTags = skuScans.map { it.uid }.distinct()
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primarySku = primarySku,
            relatedScans = skuScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = listOf(primarySku)
        )
    }
    
    private fun getAssociatedSku(filamentInfo: com.bscan.model.FilamentInfo?): List<SkuInfo> {
        if (filamentInfo == null) {
            Log.d("DetailViewModel", "No filament info provided for SKU association")
            return emptyList()
        }
        
        try {
            val allScans = unifiedDataAccess.getAllScans()
            
            // Find scans that match BOTH filament type AND color for this specific SKU
            val skuScans = allScans.filter { scan ->
                try {
                    scan.filamentInfo?.filamentType == filamentInfo.filamentType &&
                    scan.filamentInfo?.colorName == filamentInfo.colorName
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error filtering scan for SKU association", e)
                    false
                }
            }
            
            if (skuScans.isEmpty()) {
                Log.d("DetailViewModel", "No matching scans found for SKU: ${filamentInfo.filamentType}-${filamentInfo.colorName}")
                return emptyList()
            }
            
            // Create the single associated SKU
            val skuKey = "${filamentInfo.filamentType}-${filamentInfo.colorName}"
            val successfulScans = skuScans.filter { 
                try {
                    it.scanResult == com.bscan.model.ScanResult.SUCCESS 
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error checking scan result", e)
                    false
                }
            }
            
            if (successfulScans.isNotEmpty()) {
                val mostRecentScan = successfulScans.maxByOrNull { it.timestamp }
                val info = mostRecentScan?.filamentInfo ?: filamentInfo
                
                val uniqueFilamentReels = try {
                    skuScans.groupBy { it.filamentInfo?.trayUid ?: "unknown" }.size
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error calculating unique filament reels", e)
                    1
                }
                
                val totalScans = skuScans.size
                val successfulCount = successfulScans.size
                val lastScanned = skuScans.maxByOrNull { it.timestamp }?.timestamp
                
                val sku = SkuInfo(
                    skuKey = skuKey,
                    filamentInfo = info,
                    filamentReelCount = uniqueFilamentReels,
                    totalScans = totalScans,
                    successfulScans = successfulCount,
                    lastScanned = lastScanned ?: LocalDateTime.now(),
                    successRate = if (totalScans > 0) successfulCount.toFloat() / totalScans else 0f
                )
                
                Log.d("DetailViewModel", "Created SKU info: $skuKey with ${totalScans} scans")
                return listOf(sku)
            }
            
            Log.d("DetailViewModel", "No successful scans found for SKU: $skuKey")
            return emptyList()
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error getting associated SKU", e)
            return emptyList()
        }
    }
    
    private fun filamentReelDetailsToUniqueComponent(filamentReelDetails: FilamentReelDetails): UniqueFilamentReel {
        return try {
            UniqueFilamentReel(
                uid = filamentReelDetails.trayUid, // Use tray UID as the identifier
                filamentInfo = filamentReelDetails.filamentInfo,
                scanCount = filamentReelDetails.totalScans,
                successCount = filamentReelDetails.successfulScans,
                lastScanned = filamentReelDetails.lastScanned,
                successRate = if (filamentReelDetails.totalScans > 0) 
                    filamentReelDetails.successfulScans.toFloat() / filamentReelDetails.totalScans else 0f
            )
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error converting filament reel details to unique component", e)
            // Return a minimal valid component if conversion fails
            UniqueFilamentReel(
                uid = filamentReelDetails.trayUid,
                filamentInfo = filamentReelDetails.filamentInfo,
                scanCount = 0,
                successCount = 0,
                lastScanned = LocalDateTime.now(),
                successRate = 0f
            )
        }
    }
    
    private fun filamentReelDetailsToUniqueFilamentReel(filamentReelDetails: FilamentReelDetails): UniqueFilamentReel {
        return filamentReelDetailsToUniqueComponent(filamentReelDetails)
    }
    
    private fun createSyntheticFilamentInfo(filamentType: String, colorName: String): com.bscan.model.FilamentInfo {
        return com.bscan.model.FilamentInfo(
            tagUid = "unknown",
            trayUid = "unknown",
            filamentType = filamentType.ifEmpty { "Unknown Material" },
            detailedFilamentType = filamentType.ifEmpty { "Unknown Material" },
            colorHex = "#808080", // Grey for unknown
            colorName = colorName.ifEmpty { "Unknown Colour" },
            spoolWeight = 0,
            filamentDiameter = 1.75f,
            filamentLength = 0,
            productionDate = "",
            minTemperature = 0,
            maxTemperature = 0,
            bedTemperature = 0,
            dryingTemperature = 0,
            dryingTime = 0
        )
    }
    
    // === Component Hierarchy Management ===
    
    /**
     * Load component details including hierarchy relationships
     */
    private suspend fun loadComponentDetails(componentId: String) {
        Log.d("DetailViewModel", "Loading component details for ID: $componentId")
        
        if (componentRepository == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Component functionality not available - ComponentRepository not injected"
            )
            return
        }
        
        try {
            val component = componentRepository!!.getComponent(componentId)
            
            if (component == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Component not found: $componentId"
                )
                return
            }
            
            // Load hierarchy information
            val parentComponent = component.parentComponentId?.let { 
                componentRepository!!.getComponent(it) 
            }
            val childComponents = componentRepository!!.getChildComponents(componentId)
            val siblingComponents = if (parentComponent != null) {
                componentRepository!!.getChildComponents(parentComponent.id)
                    .filter { it.id != componentId }
            } else {
                emptyList()
            }
            
            // Load measurements and mass data
            val measurements = componentRepository!!.getMeasurementsForComponent(componentId)
            val totalMass = componentRepository!!.getTotalMass(componentId)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                primaryComponent = component,
                parentComponent = parentComponent,
                childComponents = childComponents,
                siblingComponents = siblingComponents,
                componentMeasurements = measurements,
                totalMass = if (totalMass > 0) totalMass else null
            )
            
            Log.d("DetailViewModel", "Successfully loaded component hierarchy for ${component.name}")
            
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error loading component details", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load component details: ${e.message}"
            )
        }
    }
    
    /**
     * Load complete component hierarchy starting from root
     */
    fun loadComponentHierarchy(componentId: String) {
        Log.d("DetailViewModel", "Loading complete hierarchy for component: $componentId")
        
        if (componentRepository == null) {
            _uiState.value = _uiState.value.copy(
                error = "Component functionality not available - ComponentRepository not injected"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val rootComponent = componentRepository!!.getRootComponent(componentId)
                    if (rootComponent != null) {
                        loadDetails(DetailType.COMPONENT, rootComponent.id)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Unable to find root component for $componentId"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error loading component hierarchy", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load hierarchy: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Add a child component to the current component
     */
    fun addChildComponent(parentId: String, childComponent: Component) {
        Log.d("DetailViewModel", "Adding child component ${childComponent.name} to $parentId")
        
        if (componentRepository == null) {
            _uiState.value = _uiState.value.copy(
                error = "Component functionality not available - ComponentRepository not injected"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val operation = UndoableOperation(
                    type = OperationType.ADD_CHILD,
                    componentId = parentId,
                    targetComponentId = childComponent.id,
                    description = "Added ${childComponent.name} as child"
                )
                
                withContext(Dispatchers.IO) {
                    // Save the child component first if it's new
                    componentRepository!!.saveComponent(childComponent)
                    
                    // Create parent-child relationship
                    componentRepository!!.addChildComponent(parentId, childComponent.id)
                    
                    // Add to undo stack
                    addToUndoStack(operation)
                }
                
                // Refresh the current view
                if (_uiState.value.primaryComponent?.id == parentId) {
                    loadComponentDetails(parentId)
                }
                
                Log.d("DetailViewModel", "Successfully added child component")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error adding child component", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add child component: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Remove a child component from its parent
     */
    fun removeChildComponent(parentId: String, childId: String) {
        Log.d("DetailViewModel", "Removing child component $childId from $parentId")
        
        viewModelScope.launch {
            try {
                val childComponent = withContext(Dispatchers.IO) {
                    componentRepository!!.getComponent(childId)
                }
                
                val operation = UndoableOperation(
                    type = OperationType.REMOVE_CHILD,
                    componentId = parentId,
                    targetComponentId = childId,
                    description = "Removed ${childComponent?.name ?: "component"} from parent",
                    previousData = childComponent
                )
                
                withContext(Dispatchers.IO) {
                    componentRepository!!.removeChildComponent(parentId, childId)
                    addToUndoStack(operation)
                }
                
                // Refresh the current view
                if (_uiState.value.primaryComponent?.id == parentId) {
                    loadComponentDetails(parentId)
                }
                
                Log.d("DetailViewModel", "Successfully removed child component")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error removing child component", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to remove child component: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Create sibling relationship between components
     */
    fun createSiblingRelationship(componentId: String, siblingId: String) {
        Log.d("DetailViewModel", "Creating sibling relationship: $componentId <-> $siblingId")
        
        viewModelScope.launch {
            try {
                val operation = UndoableOperation(
                    type = OperationType.ADD_SIBLING,
                    componentId = componentId,
                    targetComponentId = siblingId,
                    description = "Created sibling relationship"
                )
                
                withContext(Dispatchers.IO) {
                    val component1 = componentRepository!!.getComponent(componentId)
                    val component2 = componentRepository!!.getComponent(siblingId)
                    
                    if (component1 != null && component2 != null) {
                        // Add bidirectional sibling references
                        val updated1 = component1.addSiblingReference(siblingId)
                        val updated2 = component2.addSiblingReference(componentId)
                        
                        componentRepository!!.saveComponent(updated1)
                        componentRepository!!.saveComponent(updated2)
                        
                        addToUndoStack(operation)
                    }
                }
                
                // Refresh current view
                if (_uiState.value.primaryComponent?.id == componentId) {
                    loadComponentDetails(componentId)
                }
                
                Log.d("DetailViewModel", "Successfully created sibling relationship")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error creating sibling relationship", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to create sibling relationship: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Move component to a new parent
     */
    fun moveComponentToParent(componentId: String, newParentId: String) {
        Log.d("DetailViewModel", "Moving component $componentId to new parent $newParentId")
        
        viewModelScope.launch {
            try {
                val component = withContext(Dispatchers.IO) {
                    componentRepository!!.getComponent(componentId)
                }
                
                val operation = UndoableOperation(
                    type = OperationType.MOVE_COMPONENT,
                    componentId = componentId,
                    targetComponentId = newParentId,
                    description = "Moved component to new parent",
                    previousData = component?.parentComponentId
                )
                
                withContext(Dispatchers.IO) {
                    // Remove from old parent if exists
                    component?.parentComponentId?.let { oldParentId ->
                        componentRepository!!.removeChildComponent(oldParentId, componentId)
                    }
                    
                    // Add to new parent
                    componentRepository!!.addChildComponent(newParentId, componentId)
                    
                    addToUndoStack(operation)
                }
                
                // Refresh current view
                loadComponentDetails(componentId)
                
                Log.d("DetailViewModel", "Successfully moved component")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error moving component", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to move component: ${e.message}"
                )
            }
        }
    }
    
    // === Mass Calculation and Inference ===
    
    /**
     * Perform mass inference on the current component
     */
    fun performMassInference(totalMeasuredMass: Float) {
        Log.d("DetailViewModel", "Starting mass inference with measured mass: ${totalMeasuredMass}g")
        
        if (massInferenceService == null) {
            _uiState.value = _uiState.value.copy(
                error = "Mass inference functionality not available - MassInferenceService not injected"
            )
            return
        }
        
        val component = _uiState.value.primaryComponent ?: return
        
        _uiState.value = _uiState.value.copy(massInferenceInProgress = true)
        
        viewModelScope.launch {
            try {
                val inferenceResult = withContext(Dispatchers.Default) {
                    massInferenceService!!.inferComponentMass(component, totalMeasuredMass)
                }
                
                when {
                    inferenceResult.isSuccess -> {
                        // Extract inferred masses for UI display
                        val inferredMasses = inferenceResult.inferredComponents.associate { 
                            it.component.id to it.inferredMass 
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            massInferenceInProgress = false,
                            inferredMasses = inferredMasses,
                            error = null
                        )
                        
                        Log.d("DetailViewModel", "Mass inference successful: ${inferenceResult.message}")
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            massInferenceInProgress = false,
                            error = "Mass inference failed: ${inferenceResult.message}"
                        )
                        
                        Log.e("DetailViewModel", "Mass inference failed: ${inferenceResult.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error during mass inference", e)
                _uiState.value = _uiState.value.copy(
                    massInferenceInProgress = false,
                    error = "Mass inference error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Apply inferred masses to components
     */
    fun applyInferredMasses() {
        Log.d("DetailViewModel", "Applying inferred masses")
        
        val inferredMasses = _uiState.value.inferredMasses
        if (inferredMasses.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No inferred masses to apply"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val operations = mutableListOf<UndoableOperation>()
                
                withContext(Dispatchers.IO) {
                    for ((componentId, inferredMass) in inferredMasses) {
                        val component = componentRepository!!.getComponent(componentId)
                        if (component != null) {
                            val operation = UndoableOperation(
                                type = OperationType.UPDATE_MASS,
                                componentId = componentId,
                                description = "Applied inferred mass: ${inferredMass}g",
                                previousData = component.massGrams
                            )
                            
                            val updatedComponent = component.copy(
                                massGrams = inferredMass,
                                inferredMass = true,
                                lastUpdated = LocalDateTime.now()
                            )
                            
                            componentRepository!!.saveComponent(updatedComponent)
                            operations.add(operation)
                        }
                    }
                    
                    // Add batch operation to undo stack
                    if (operations.isNotEmpty()) {
                        addToUndoStack(UndoableOperation(
                            type = OperationType.BATCH,
                            componentId = "multiple",
                            description = "Applied ${operations.size} inferred masses",
                            previousData = operations
                        ))
                    }
                }
                
                // Clear inferred masses and refresh view
                _uiState.value = _uiState.value.copy(
                    inferredMasses = emptyMap()
                )
                
                _uiState.value.primaryComponent?.let { component ->
                    loadComponentDetails(component.id)
                }
                
                Log.d("DetailViewModel", "Successfully applied ${inferredMasses.size} inferred masses")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error applying inferred masses", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to apply inferred masses: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Process BLE scale reading for real-time mass measurement
     */
    fun processScaleReading(scaleReading: ScaleReading) {
        Log.d("DetailViewModel", "Processing scale reading: ${scaleReading.weight} ${scaleReading.unit}")
        
        val component = _uiState.value.primaryComponent ?: return
        
        _uiState.value = _uiState.value.copy(
            lastScaleReading = scaleReading,
            massInferenceInProgress = true
        )
        
        viewModelScope.launch {
            try {
                val scaleInferenceResult = withContext(Dispatchers.Default) {
                    massInferenceService!!.processScaleReading(component, scaleReading)
                }
                
                when {
                    scaleInferenceResult.isSuccess -> {
                        val result = scaleInferenceResult as com.bscan.service.ScaleInferenceResult.Success
                        
                        // Extract inferred masses
                        val inferredMasses = result.inferenceResult.inferredComponents.associate { 
                            it.component.id to it.inferredMass 
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            massInferenceInProgress = false,
                            inferredMasses = inferredMasses,
                            error = null
                        )
                        
                        Log.d("DetailViewModel", "Scale inference successful")
                    }
                    else -> {
                        val result = scaleInferenceResult as com.bscan.service.ScaleInferenceResult.Error
                        
                        _uiState.value = _uiState.value.copy(
                            massInferenceInProgress = false,
                            error = "Scale reading error: ${result.message}"
                        )
                        
                        Log.e("DetailViewModel", "Scale inference failed: ${result.message}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error processing scale reading", e)
                _uiState.value = _uiState.value.copy(
                    massInferenceInProgress = false,
                    error = "Scale processing error: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Record a manual mass measurement
     */
    fun recordMassMeasurement(
        componentId: String, 
        measuredMass: Float, 
        measurementType: MeasurementType,
        notes: String = ""
    ) {
        Log.d("DetailViewModel", "Recording mass measurement: ${measuredMass}g for $componentId")
        
        if (componentRepository == null) {
            _uiState.value = _uiState.value.copy(
                error = "Component functionality not available - ComponentRepository not injected"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val measurement = ComponentMeasurement(
                    id = "measurement_${System.currentTimeMillis()}",
                    componentId = componentId,
                    measuredMassGrams = measuredMass,
                    measurementType = measurementType,
                    measuredAt = LocalDateTime.now(),
                    notes = notes
                )
                
                withContext(Dispatchers.IO) {
                    componentRepository!!.saveMeasurement(measurement)
                }
                
                // Refresh measurements
                if (_uiState.value.primaryComponent?.id == componentId) {
                    loadComponentDetails(componentId)
                }
                
                Log.d("DetailViewModel", "Successfully recorded measurement")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error recording measurement", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to record measurement: ${e.message}"
                )
            }
        }
    }
    
    // === UI State Management ===
    
    /**
     * Toggle expansion state of hierarchy node
     */
    fun toggleHierarchyExpansion(componentId: String) {
        val currentState = _uiState.value.hierarchyExpanded[componentId] ?: false
        val newExpansionState = _uiState.value.hierarchyExpanded.toMutableMap()
        newExpansionState[componentId] = !currentState
        
        _uiState.value = _uiState.value.copy(
            hierarchyExpanded = newExpansionState
        )
        
        Log.d("DetailViewModel", "Toggled hierarchy expansion for $componentId: ${!currentState}")
    }
    
    /**
     * Toggle relationship editing mode
     */
    fun toggleRelationshipEditingMode() {
        val currentMode = _uiState.value.isEditingRelationships
        _uiState.value = _uiState.value.copy(
            isEditingRelationships = !currentMode
        )
        
        Log.d("DetailViewModel", "Toggled relationship editing mode: ${!currentMode}")
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // === Undo/Redo Operations ===
    
    /**
     * Undo the last operation
     */
    fun undoLastOperation() {
        Log.d("DetailViewModel", "Undoing last operation")
        
        if (undoStack.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No operations to undo"
            )
            return
        }
        
        val operation = undoStack.removeLastOrNull() ?: return
        redoStack.add(operation)
        
        // Trim redo stack if needed
        while (redoStack.size > maxUndoStackSize) {
            redoStack.removeFirstOrNull()
        }
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    performUndoOperation(operation)
                }
                
                // Update undo/redo state
                updateUndoRedoState()
                
                // Refresh current view
                _uiState.value.primaryComponent?.let { component ->
                    loadComponentDetails(component.id)
                }
                
                Log.d("DetailViewModel", "Successfully undid operation: ${operation.description}")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error undoing operation", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to undo operation: ${e.message}"
                )
                
                // Restore operation to undo stack
                undoStack.add(operation)
                redoStack.removeLastOrNull()
            }
        }
    }
    
    /**
     * Redo the last undone operation
     */
    fun redoLastOperation() {
        Log.d("DetailViewModel", "Redoing last operation")
        
        if (redoStack.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "No operations to redo"
            )
            return
        }
        
        val operation = redoStack.removeLastOrNull() ?: return
        undoStack.add(operation)
        
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    performRedoOperation(operation)
                }
                
                // Update undo/redo state
                updateUndoRedoState()
                
                // Refresh current view
                _uiState.value.primaryComponent?.let { component ->
                    loadComponentDetails(component.id)
                }
                
                Log.d("DetailViewModel", "Successfully redid operation: ${operation.description}")
                
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Error redoing operation", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to redo operation: ${e.message}"
                )
                
                // Restore operation to redo stack
                redoStack.add(operation)
                undoStack.removeLastOrNull()
            }
        }
    }
    
    // === Private Helper Methods ===
    
    private fun addToUndoStack(operation: UndoableOperation) {
        undoStack.add(operation)
        redoStack.clear() // Clear redo stack when new operation is added
        
        // Trim undo stack if needed
        while (undoStack.size > maxUndoStackSize) {
            undoStack.removeFirstOrNull()
        }
        
        updateUndoRedoState()
    }
    
    private fun updateUndoRedoState() {
        _uiState.value = _uiState.value.copy(
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }
    
    private suspend fun performUndoOperation(operation: UndoableOperation) {
        when (operation.type) {
            OperationType.ADD_CHILD -> {
                // Undo by removing the child
                componentRepository!!.removeChildComponent(operation.componentId, operation.targetComponentId!!)
            }
            OperationType.REMOVE_CHILD -> {
                // Undo by adding the child back
                componentRepository!!.addChildComponent(operation.componentId, operation.targetComponentId!!)
            }
            OperationType.ADD_SIBLING -> {
                // Undo by removing sibling references
                val component1 = componentRepository!!.getComponent(operation.componentId)
                val component2 = componentRepository!!.getComponent(operation.targetComponentId!!)
                
                if (component1 != null && component2 != null) {
                    val updated1 = component1.removeSiblingReference(operation.targetComponentId)
                    val updated2 = component2.removeSiblingReference(operation.componentId)
                    
                    componentRepository!!.saveComponent(updated1)
                    componentRepository!!.saveComponent(updated2)
                }
            }
            OperationType.MOVE_COMPONENT -> {
                // Undo by restoring previous parent
                val oldParentId = operation.previousData as? String
                val componentId = operation.componentId
                
                // Remove from current parent
                componentRepository!!.removeChildComponent(operation.targetComponentId!!, componentId)
                
                // Add back to old parent if it existed
                if (oldParentId != null) {
                    componentRepository!!.addChildComponent(oldParentId, componentId)
                }
            }
            OperationType.UPDATE_MASS -> {
                // Undo by restoring previous mass
                val component = componentRepository!!.getComponent(operation.componentId)
                val previousMass = operation.previousData as? Float
                
                if (component != null) {
                    val updatedComponent = component.copy(
                        massGrams = previousMass,
                        inferredMass = false,
                        lastUpdated = LocalDateTime.now()
                    )
                    componentRepository!!.saveComponent(updatedComponent)
                }
            }
            OperationType.BATCH -> {
                // Undo batch operations in reverse order
                val operations = operation.previousData as? List<*>
                if (operations?.all { it is UndoableOperation } == true) {
                    @Suppress("UNCHECKED_CAST")
                    val typedOperations = operations as List<UndoableOperation>
                    typedOperations.reversed().forEach { batchOp ->
                        performUndoOperation(batchOp)
                    }
                } else {
                    // Log error or handle invalid data
                    android.util.Log.w("DetailViewModel", "Invalid previousData for BATCH undo operation: expected List<UndoableOperation>, got ${operation.previousData?.javaClass?.name}")
                }
            }
        }
    }
    
    private suspend fun performRedoOperation(operation: UndoableOperation) {
        when (operation.type) {
            OperationType.ADD_CHILD -> {
                componentRepository!!.addChildComponent(operation.componentId, operation.targetComponentId!!)
            }
            OperationType.REMOVE_CHILD -> {
                componentRepository!!.removeChildComponent(operation.componentId, operation.targetComponentId!!)
            }
            OperationType.ADD_SIBLING -> {
                val component1 = componentRepository!!.getComponent(operation.componentId)
                val component2 = componentRepository!!.getComponent(operation.targetComponentId!!)
                
                if (component1 != null && component2 != null) {
                    val updated1 = component1.addSiblingReference(operation.targetComponentId)
                    val updated2 = component2.addSiblingReference(operation.componentId)
                    
                    componentRepository!!.saveComponent(updated1)
                    componentRepository!!.saveComponent(updated2)
                }
            }
            OperationType.MOVE_COMPONENT -> {
                // Remove from old parent and add to new parent
                val component = componentRepository!!.getComponent(operation.componentId)
                component?.parentComponentId?.let { oldParentId ->
                    componentRepository!!.removeChildComponent(oldParentId, operation.componentId)
                }
                componentRepository!!.addChildComponent(operation.targetComponentId!!, operation.componentId)
            }
            OperationType.UPDATE_MASS -> {
                // Reapply the mass change
                val component = componentRepository!!.getComponent(operation.componentId)
                // Note: We would need to store the new mass value in the operation for proper redo
                // This is a simplified implementation
            }
            OperationType.BATCH -> {
                // Redo batch operations in original order
                val operations = operation.previousData as? List<*>
                if (operations?.all { it is UndoableOperation } == true) {
                    @Suppress("UNCHECKED_CAST")
                    val typedOperations = operations as List<UndoableOperation>
                    typedOperations.forEach { batchOp ->
                        performRedoOperation(batchOp)
                    }
                } else {
                    // Log error or handle invalid data
                    android.util.Log.w("DetailViewModel", "Invalid previousData for BATCH redo operation: expected List<UndoableOperation>, got ${operation.previousData?.javaClass?.name}")
                }
            }
        }
    }
}

// === Supporting Data Classes ===

/**
 * Represents an undoable operation for state management
 */
data class UndoableOperation(
    val type: OperationType,
    val componentId: String,
    val targetComponentId: String? = null,
    val description: String,
    val previousData: Any? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * Types of operations that can be undone
 */
enum class OperationType {
    ADD_CHILD,
    REMOVE_CHILD,
    ADD_SIBLING,
    MOVE_COMPONENT,
    UPDATE_MASS,
    BATCH // For multiple operations
}