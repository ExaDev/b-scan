package com.bscan.repository

import android.util.Log
import com.bscan.model.*
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.model.graph.PropertyValue
import com.bscan.model.graph.ContinuousQuantity
import com.bscan.interpreter.InterpreterFactory
import com.bscan.service.GraphDataService
import com.bscan.service.GraphScanResult
import com.bscan.detector.TagDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context

/**
 * Unified data access layer that merges catalog data (build-time) with user data (runtime).
 * Provides a single interface for accessing all app data with proper override logic.
 * This replaces the old UnifiedDataRepository approach.
 */
class UnifiedDataAccess(
    private val catalogRepo: CatalogRepository,
    private val userRepo: UserDataRepository,
    private val scanHistoryRepo: ScanHistoryRepository? = null,
    private val componentRepo: ComponentRepository? = null,
    private val context: Context? = null
) {
    
    companion object {
        private const val TAG = "UnifiedDataAccess"
    }
    
    /**
     * Public accessor for the context
     */
    val appContext: Context? get() = context
    
    /**
     * Graph data service for graph-based operations
     */
    private val graphDataService: GraphDataService? by lazy {
        context?.let { GraphDataService(it) }
    }
    
    // === Graph-Based Data Operations ===
    
    /**
     * Process scan data using graph-based approach
     */
    suspend fun createGraphEntitiesFromScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GraphEntityCreationResult = withContext(Dispatchers.IO) {
        return@withContext graphDataService?.let { service ->
            try {
                when (val result = service.processScanData(encryptedScanData, decryptedScanData)) {
                    is GraphScanResult.Success -> GraphEntityCreationResult(
                        success = true,
                        rootEntity = result.rootEntity,
                        scannedEntity = result.scannedEntity,
                        totalEntitiesCreated = result.allEntities.size,
                        totalEdgesCreated = result.allEdges.size
                    )
                    is GraphScanResult.Failure -> GraphEntityCreationResult(
                        success = false,
                        errorMessage = result.error
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating graph entities from scan", e)
                GraphEntityCreationResult(
                    success = false,
                    errorMessage = "Graph entity creation failed: ${e.message}"
                )
            }
        } ?: GraphEntityCreationResult(
            success = false,
            errorMessage = "Graph data service not available"
        )
    }
    
    /**
     * Get inventory item by identifier using graph approach
     */
    suspend fun getGraphInventoryItem(identifierValue: String): Entity? {
        return graphDataService?.getInventoryItemByIdentifier(identifierValue)
    }
    
    /**
     * Get all inventory items using graph approach
     */
    suspend fun getAllGraphInventoryItems(): List<Entity> {
        return graphDataService?.getAllInventoryItems() ?: emptyList()
    }
    
    /**
     * Get connected entities for navigation
     */
    suspend fun getConnectedEntities(entityId: String): List<Entity> {
        return graphDataService?.getConnectedEntities(entityId) ?: emptyList()
    }
    
    /**
     * Get entities connected through specific relationship
     */
    suspend fun getConnectedEntities(entityId: String, relationshipType: String): List<Entity> {
        return graphDataService?.getConnectedEntities(entityId, relationshipType) ?: emptyList()
    }
    
    /**
     * Get graph statistics
     */
    suspend fun getGraphStatistics(): GraphStatistics? {
        return graphDataService?.getGraphStatistics()
    }
    
    
    /**
     * Find best product match from FilamentInfo for SKU lookup
     */
    // TODO: Reimplement with StockDefinitions
    suspend fun findBestStockDefinitionMatch(filamentInfo: FilamentInfo): StockDefinition? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Looking up stock definition for ${filamentInfo.filamentType}/${filamentInfo.colorName}")
            
            // First try exact match by material type and color name
            val manufacturerId = determineManufacturer(filamentInfo)
            val stockDefinitions = findStockDefinitions(manufacturerId, null, filamentInfo.filamentType)
            
            val exactMatch = stockDefinitions.firstOrNull { stockDef ->
                stockDef.getProperty<String>("colorName")?.equals(filamentInfo.colorName, ignoreCase = true) == true
            }
            
            if (exactMatch != null) {
                Log.d(TAG, "Found exact stock definition match: ${exactMatch.getProperty<String>("sku")}")
                return@withContext exactMatch
            }
            
            Log.d(TAG, "No stock definition match found for ${filamentInfo.filamentType}/${filamentInfo.colorName}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding stock definition match", e)
            null
        }
    }
    
    /**
     * Update component stock levels by SKU
     */
    suspend fun updateComponentStock(skuId: String, quantityChange: Int) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating stock for SKU: $skuId by $quantityChange")
            
            // Get current stock tracking data from user preferences metadata
            // For now, we'll store stock tracking in a simple format
            // This could be enhanced with a dedicated stock tracking system later
            val userData = userRepo.getUserData()
            
            // Count existing instances of this SKU in components
            val components = componentRepo?.getComponents() ?: emptyList()
            val existingCount = components.count { it.metadata["sku"] == skuId }
            val newCount = maxOf(0, existingCount + quantityChange)
            
            // For now, just log the stock change - could be enhanced with dedicated storage
            Log.i(TAG, "Stock tracking - SKU: $skuId, existing: $existingCount, change: $quantityChange, new: $newCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating component stock", e)
        }
    }
    
    /**
     * Get total stock across all component instances for a SKU
     */
    suspend fun getSkuStockLevel(skuId: String): StockLevel = withContext(Dispatchers.IO) {
        try {
            val components = componentRepo?.getComponents() ?: emptyList()
            
            // Find all components associated with this SKU
            val skuComponents = components.filter { component ->
                component.metadata["sku"] == skuId
            }
            
            val totalInstances = skuComponents.size
            val availableInstances = skuComponents.count { component ->
                // Consider component available if it's not consumed/empty
                val remainingPercentage = component.getRemainingPercentage()
                remainingPercentage == null || remainingPercentage > 0.05f // More than 5% remaining
            }
            
            // Count total quantity from component metadata
            val totalQuantity = components.count { it.metadata["sku"] == skuId }
            
            StockLevel(
                skuId = skuId,
                totalQuantity = totalQuantity,
                availableQuantity = availableInstances,
                totalInstances = totalInstances,
                runningLowThreshold = 2, // Alert when 2 or fewer available
                isRunningLow = availableInstances <= 2
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SKU stock level", e)
            StockLevel(
                skuId = skuId,
                totalQuantity = 0,
                availableQuantity = 0,
                totalInstances = 0,
                runningLowThreshold = 0,
                isRunningLow = true
            )
        }
    }
    
    // === Enhanced Resolution Methods ===
    
    /**
     * Find a stock definition by SKU ID for a specific manufacturer
     */
    fun findStockDefinitionBySku(manufacturerId: String, skuId: String): StockDefinition? {
        return catalogRepo.findStockDefinitionBySku(manufacturerId, skuId)
    }
    
    /**
     * Resolve components by identifier type and value
     */
    suspend fun resolveComponentsByIdentifier(
        identifierType: IdentifierType, 
        value: String
    ): List<Component> = withContext(Dispatchers.IO) {
        componentRepo?.getComponents()?.filter { component ->
            component.identifiers.any { identifier ->
                identifier.type == identifierType && identifier.value == value
            }
        } ?: emptyList()
    }
    
    /**
     * Resolve inventory items with priority system
     * Priority: User catalog > Build-time catalog > Interpreted data
     */
    suspend fun resolveInventoryItem(
        uniqueIdentifier: String,
        fallbackStrategy: ResolutionStrategy = ResolutionStrategy.COMPREHENSIVE
    ): InventoryResolutionResult = withContext(Dispatchers.IO) {
        try {
            // 1. Check component repository first (highest priority)
            componentRepo?.findInventoryByUniqueId(uniqueIdentifier)?.let { component ->
                Log.d(TAG, "Found inventory item in component repository")
                return@withContext InventoryResolutionResult(
                    success = true,
                    component = component,
                    source = "ComponentRepository"
                )
            }
            
            // 2. Try RFID resolution if comprehensive fallback enabled
            if (fallbackStrategy == ResolutionStrategy.COMPREHENSIVE) {
                // Attempt to resolve as RFID tag UID
                resolveRfidTag(uniqueIdentifier, byteArrayOf())?.let { filamentInfo ->
                    Log.d(TAG, "Resolved via RFID mapping")
                    return@withContext InventoryResolutionResult(
                        success = true,
                        filamentInfo = filamentInfo,
                        source = "RfidMapping"
                    )
                }
            }
            
            Log.d(TAG, "No resolution found for identifier: $uniqueIdentifier")
            InventoryResolutionResult(
                success = false,
                errorMessage = "No inventory item found with identifier: $uniqueIdentifier"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving inventory item", e)
            InventoryResolutionResult(
                success = false,
                errorMessage = "Resolution error: ${e.message}"
            )
        }
    }
    
    /**
     * Resolve component relationships and groupings
     */
    suspend fun resolveComponentHierarchy(componentId: String): ComponentHierarchy? = withContext(Dispatchers.IO) {
        try {
            val component = componentRepo?.getComponent(componentId) ?: return@withContext null
            val children = componentRepo.getChildComponents(componentId)
            val parent = component.parentComponentId?.let { componentRepo.getComponent(it) }
            val siblings = parent?.childComponents?.mapNotNull { siblingId ->
                if (siblingId != componentId) componentRepo.getComponent(siblingId) else null
            } ?: emptyList()
            
            ComponentHierarchy(
                component = component,
                parent = parent,
                children = children,
                siblings = siblings,
                totalHierarchyMass = calculateHierarchyMass(componentId)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving component hierarchy", e)
            null
        }
    }
    
    
    /**
     * Update stock levels for multiple SKUs
     */
    suspend fun updateMultipleStockLevels(stockUpdates: Map<String, Int>) = withContext(Dispatchers.IO) {
        stockUpdates.forEach { (skuId, quantityChange) ->
            updateComponentStock(skuId, quantityChange)
        }
    }
    
    // === Private Helper Methods ===
    
    /**
     * Determine manufacturer from FilamentInfo
     */
    private fun determineManufacturer(filamentInfo: FilamentInfo): String {
        return when {
            !filamentInfo.manufacturerName.isNullOrBlank() -> {
                filamentInfo.manufacturerName.lowercase().replace(" ", "")
            }
            else -> "bambu" // Default to Bambu Lab
        }
    }
    
    /**
     * Find fuzzy color match for similar color names
     */
    
    /**
     * Get component defaults for a specific SKU
     */
    private suspend fun getComponentDefaultsForSku(manufacturerId: String, skuId: String): Map<String, ComponentDefault> {
        return catalogRepo.getComponentDefaults(manufacturerId)
    }
    
    /**
     * Count total components in hierarchy
     */
    private suspend fun countHierarchyComponents(rootComponentId: String): Int {
        return try {
            val component = componentRepo?.getComponent(rootComponentId) ?: return 1
            val childrenCount = component.childComponents.sumOf { childId ->
                countHierarchyComponents(childId)
            }
            1 + childrenCount
        } catch (e: Exception) {
            Log.e(TAG, "Error counting hierarchy components", e)
            1
        }
    }
    
    /**
     * Calculate total mass of component hierarchy
     */
    private suspend fun calculateHierarchyMass(componentId: String): Float? = withContext(Dispatchers.IO) {
        componentRepo?.getTotalMass(componentId)
    }
    
    // === RFID Resolution ===
    
    /**
     * Resolve RFID tag to filament information using all available sources
     */
    fun resolveRfidTag(tagUid: String, tagData: ByteArray): FilamentInfo? {
        Log.d(TAG, "Resolving RFID tag: $tagUid")
        
        // 1. Check user custom mappings first (highest priority)
        // TODO: Implement custom mapping resolution when needed
        
        // 2. Check catalog RFID mappings (manufacturer defaults)
        // TODO: Implement catalog RFID mapping when needed
        
        // 3. Try format detection for OpenTag/other formats
        detectAndParseTag(tagData)?.let { parsedInfo ->
            Log.d(TAG, "Parsed tag data directly: $tagUid")
            return parsedInfo
        }
        
        Log.d(TAG, "No mapping found for RFID tag: $tagUid")
        return null
    }
    
    /**
     * Detect tag format and parse data directly from tag
     */
    private fun detectAndParseTag(tagData: ByteArray): FilamentInfo? {
        // This would integrate with InterpreterFactory for format detection
        // For now, return null - will be implemented with interpreter updates
        return null
    }
    
    // === Product/SKU Management ===
    
    /**
     * Get all stock definitions for a manufacturer
     * Combines catalog data with user-added stock definitions
     */
    fun getStockDefinitions(manufacturerId: String): List<StockDefinition> {
        val stockDefinitions = mutableListOf<StockDefinition>()
        
        // Get catalog stock definitions
        stockDefinitions.addAll(catalogRepo.getStockDefinitions(manufacturerId))
        
        // Add user-added stock definitions from custom manufacturers
        val customManufacturer = userRepo.getUserData().customMappings.manufacturers[manufacturerId]
        customManufacturer?.materials?.forEach { (materialId, material) ->
            customManufacturer.colorPalette.forEach { (hex, colorName) ->
                val stockDefinition = StockDefinition(
                    label = material.displayName,
                    properties = mutableMapOf(
                        "sku" to PropertyValue.StringValue("${manufacturerId}_${materialId}_${hex.replace("#", "")}"),
                        "displayName" to PropertyValue.StringValue(material.displayName),
                        "materialType" to PropertyValue.StringValue(materialId.uppercase()),
                        "colorName" to PropertyValue.StringValue(colorName),
                        "colorHex" to PropertyValue.StringValue(hex),
                        "weight" to PropertyValue.create(ContinuousQuantity(1000.0, "g")),
                        "manufacturer" to PropertyValue.StringValue(customManufacturer.displayName),
                        "consumable" to PropertyValue.BooleanValue(true),
                        "reusable" to PropertyValue.BooleanValue(false)
                    )
                )
                stockDefinitions.add(stockDefinition)
            }
        }
        
        return stockDefinitions
    }
    
    /**
     * Find stock definitions by color and material for a manufacturer
     */
    fun findStockDefinitions(
        manufacturerId: String,
        hex: String? = null,
        materialType: String? = null
    ): List<StockDefinition> {
        val stockDefinitions = getStockDefinitions(manufacturerId)
        return stockDefinitions.filter { stockDefinition ->
            val hexMatches = hex?.let { 
                stockDefinition.getProperty<String>("colorHex")?.equals(it, ignoreCase = true) 
            } ?: true
            val materialMatches = materialType?.let { 
                stockDefinition.getProperty<String>("materialType")?.equals(it, ignoreCase = true) 
            } ?: true
            hexMatches && materialMatches
        }
    }
    
    /**
     * Find best stock definition match by material type and color name
     */
    fun findBestStockDefinitionMatch(filamentType: String, colorName: String): StockDefinition? {
        // First try Bambu Lab (most common case)
        findStockDefinitions("bambu", materialType = filamentType).find { stockDef ->
            stockDef.getProperty<String>("colorName")?.equals(colorName, ignoreCase = true) == true
        }?.let { return it }
        
        // Try other manufacturers
        catalogRepo.getManufacturers().keys.forEach { manufacturerId ->
            if (manufacturerId != "bambu") {
                findStockDefinitions(manufacturerId, materialType = filamentType).find { stockDef ->
                    stockDef.getProperty<String>("colorName")?.equals(colorName, ignoreCase = true) == true
                }?.let { return it }
            }
        }
        
        return null
    }
    
    // === Component Management ===
    // Components are now generated on-demand, not persisted
    
    // === Inventory Management ===  
    // Inventory items are now generated on-demand from scan data
    
    
    
    // === Scan Management ===
    
    /**
     * Record a new scan with both encrypted and decrypted data
     */
    fun recordScan(encryptedData: EncryptedScanData, decryptedData: DecryptedScanData) {
        userRepo.recordScan(encryptedData, decryptedData)
    }
    
    // === Manufacturer Resolution ===
    
    /**
     * Get merged manufacturer information (catalog + custom)
     */
    fun getManufacturer(manufacturerId: String): MergedManufacturer? {
        val catalogManufacturer = catalogRepo.getManufacturer(manufacturerId)
        val userData = userRepo.getUserData()
        val customManufacturer = userData.customMappings.manufacturers[manufacturerId]
        
        return when {
            catalogManufacturer != null && customManufacturer != null -> {
                // Merge catalog and custom data
                MergedManufacturer.fromCatalogAndCustom(catalogManufacturer, customManufacturer)
            }
            catalogManufacturer != null -> {
                // Catalog only
                MergedManufacturer.fromCatalog(catalogManufacturer)
            }
            customManufacturer != null -> {
                // Custom only
                MergedManufacturer.fromCustom(customManufacturer)
            }
            else -> null
        }
    }
    
    /**
     * Get all available manufacturers (catalog + custom)
     */
    fun getAllManufacturers(): Map<String, MergedManufacturer> {
        val catalogManufacturers = catalogRepo.getManufacturers()
        val customManufacturers = userRepo.getUserData().customMappings.manufacturers
        
        val merged = mutableMapOf<String, MergedManufacturer>()
        
        // Add all catalog manufacturers
        catalogManufacturers.forEach { (id, catalog) ->
            val custom = customManufacturers[id]
            merged[id] = if (custom != null) {
                MergedManufacturer.fromCatalogAndCustom(catalog, custom)
            } else {
                MergedManufacturer.fromCatalog(catalog)
            }
        }
        
        // Add custom-only manufacturers
        customManufacturers.forEach { (id, custom) ->
            if (!merged.containsKey(id)) {
                merged[id] = MergedManufacturer.fromCustom(custom)
            }
        }
        
        return merged
    }
    
    // === Temperature and Material Resolution ===
    
    /**
     * Get temperature profile for a material, checking custom overrides first
     */
    fun getTemperatureProfile(manufacturerId: String, materialId: String): TemperatureProfile? {
        // Check custom manufacturer first
        userRepo.getUserData().customMappings.manufacturers[manufacturerId]?.let { custom ->
            custom.materials[materialId]?.temperatureProfile?.let { profileId ->
                return custom.temperatureProfiles[profileId]
            }
        }
        
        // Fall back to catalog
        catalogRepo.getManufacturer(manufacturerId)?.let { catalog ->
            catalog.materials[materialId]?.temperatureProfile?.let { profileId ->
                return catalog.temperatureProfiles[profileId]
            }
        }
        
        return null
    }
    
    // === Scan Data Access Methods ===
    
    /**
     * Get all encrypted scan data
     */
    fun getAllEncryptedScanData(): List<EncryptedScanData> {
        return scanHistoryRepo?.getAllEncryptedScans() ?: emptyList()
    }
    
    /**
     * Get all decrypted scan data  
     */
    fun getAllDecryptedScanData(): List<DecryptedScanData> {
        return scanHistoryRepo?.getAllDecryptedScans() ?: emptyList()
    }
    
    /**
     * Get scan data filtered by tag UID
     */
    fun getEncryptedScanDataByTagUid(tagUid: String): List<EncryptedScanData> {
        return scanHistoryRepo?.getEncryptedScansByTagUid(tagUid) ?: emptyList()
    }
    
    /**
     * Get decrypted scan data filtered by tag UID
     */
    fun getDecryptedScanDataByTagUid(tagUid: String): List<DecryptedScanData> {
        return scanHistoryRepo?.getDecryptedScansByTagUid(tagUid) ?: emptyList()
    }
    
    /**
     * Get component data by identifier (uses modern ComponentRepository)
     */
    fun getComponentByIdentifier(identifier: String): Component? {
        return componentRepo?.findComponentByUniqueId(identifier)
    }
    
    // === RFID Code Resolution ===
    
    /**
     * Get RFID mapping by material and variant IDs (for BambuFormatInterpreter)
     */
    fun getRfidMappingByCode(materialId: String, variantId: String): RfidMapping? {
        val rfidCode = "$materialId:$variantId"
        Log.d(TAG, "Looking up RFID code: $rfidCode")
        
        // Check catalog RFID mappings across all manufacturers
        catalogRepo.findRfidMapping(rfidCode)?.let { (manufacturerId, mapping) ->
            Log.d(TAG, "Found RFID mapping for $rfidCode in manufacturer $manufacturerId")
            return mapping
        }
        
        Log.d(TAG, "No RFID mapping found for code: $rfidCode")
        return null
    }
    
    
    // === Private Helper Methods (continued) ===
    
}

/**
 * Result of component creation workflow
 */
data class ComponentCreationResult(
    val success: Boolean,
    val rootComponent: Component? = null,
    val totalComponentsCreated: Int = 0,
    val skuData: CatalogSku? = null,
    val errorMessage: String? = null
)

/**
 * SKU data from catalog lookup
 */
data class CatalogSku(
    val sku: String,
    val productName: String,
    val manufacturer: String,
    val materialType: String?,
    val colorName: String?,
    val colorHex: String?,
    val filamentWeightGrams: Float?,
    val url: String?,
    val componentDefaults: Map<String, ComponentDefault> = emptyMap()
)

/**
 * Stock level information for a SKU
 */
data class StockLevel(
    val skuId: String,
    val totalQuantity: Int,
    val availableQuantity: Int,
    val totalInstances: Int,
    val runningLowThreshold: Int,
    val isRunningLow: Boolean
)

/**
 * Result of inventory item resolution
 */
data class InventoryResolutionResult(
    val success: Boolean,
    val component: Component? = null,
    val filamentInfo: FilamentInfo? = null,
    val source: String? = null,
    val errorMessage: String? = null
)

/**
 * Component hierarchy information
 */
data class ComponentHierarchy(
    val component: Component,
    val parent: Component?,
    val children: List<Component>,
    val siblings: List<Component>,
    val totalHierarchyMass: Float?
)

/**
 * Resolution strategy for fallback operations
 */
enum class ResolutionStrategy {
    EXACT_MATCH_ONLY,    // Only check primary repositories
    COMPREHENSIVE        // Use all available fallback methods
}

/**
 * Result of batch component creation
 */
data class BatchComponentCreationResult(
    val totalProcessed: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<ComponentCreationResult>
)

/**
 * Result of graph-based entity creation
 */
data class GraphEntityCreationResult(
    val success: Boolean,
    val rootEntity: Entity? = null,
    val scannedEntity: Entity? = null,
    val totalEntitiesCreated: Int = 0,
    val totalEdgesCreated: Int = 0,
    val errorMessage: String? = null
)

/**
 * Merged manufacturer data combining catalog and custom information
 */
data class MergedManufacturer(
    val id: String,
    val displayName: String,
    val materials: Map<String, MaterialDefinition>,
    val temperatureProfiles: Map<String, TemperatureProfile>,
    val colorPalette: Map<String, String>,
    val rfidMappings: Map<String, RfidMapping>,
    val componentDefaults: Map<String, ComponentDefault>,
    val tagFormat: TagFormat,
    val isCustom: Boolean = false
) {
    companion object {
        fun fromCatalog(catalog: ManufacturerCatalog): MergedManufacturer {
            return MergedManufacturer(
                id = catalog.name,
                displayName = catalog.displayName,
                materials = catalog.materials,
                temperatureProfiles = catalog.temperatureProfiles,
                colorPalette = catalog.colorPalette,
                rfidMappings = catalog.rfidMappings,
                componentDefaults = catalog.componentDefaults,
                tagFormat = catalog.tagFormat,
                isCustom = false
            )
        }
        
        fun fromCustom(custom: CustomManufacturer): MergedManufacturer {
            return MergedManufacturer(
                id = custom.name,
                displayName = custom.displayName,
                materials = custom.materials,
                temperatureProfiles = custom.temperatureProfiles,
                colorPalette = custom.colorPalette,
                rfidMappings = emptyMap(), // Custom manufacturers don't have RFID mappings
                componentDefaults = emptyMap(), // Custom manufacturers don't have component defaults
                tagFormat = custom.tagFormat,
                isCustom = true
            )
        }
        
        fun fromCatalogAndCustom(catalog: ManufacturerCatalog, custom: CustomManufacturer): MergedManufacturer {
            return MergedManufacturer(
                id = catalog.name,
                displayName = custom.displayName, // Custom display name takes precedence
                materials = catalog.materials + custom.materials, // Custom materials extend catalog
                temperatureProfiles = catalog.temperatureProfiles + custom.temperatureProfiles,
                colorPalette = catalog.colorPalette + custom.colorPalette,
                rfidMappings = catalog.rfidMappings, // Only catalog has RFID mappings
                componentDefaults = catalog.componentDefaults, // Only catalog has component defaults
                tagFormat = catalog.tagFormat, // Catalog tag format takes precedence
                isCustom = false // It's a hybrid, but primarily catalog-based
            )
        }
    }
}