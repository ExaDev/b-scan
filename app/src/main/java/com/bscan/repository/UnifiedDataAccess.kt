package com.bscan.repository

import android.util.Log
import com.bscan.model.*
import com.bscan.interpreter.InterpreterFactory
import com.bscan.service.ComponentFactory
import com.bscan.detector.TagDetector
import com.bscan.service.ProductLookupService
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
    
    // === Component Creation Workflow ===
    
    /**
     * Complete component creation workflow from RFID scan.
     * Integrates with ComponentFactory pattern for different tag formats.
     */
    suspend fun createComponentsFromScan(
        encryptedScanData: EncryptedScanData, 
        decryptedScanData: DecryptedScanData
    ): ComponentCreationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting component creation workflow for tag: ${encryptedScanData.tagUid}")
            
            // Detect tag format and create appropriate factory
            val factory = createFactorySafely(encryptedScanData)
            if (factory == null) {
                return@withContext ComponentCreationResult(
                    success = false,
                    errorMessage = "Unable to create appropriate component factory"
                )
            }
            Log.d(TAG, "Using factory: ${factory.factoryType}")
            
            // Process the scan to create components
            val rootComponent = factory.processScan(encryptedScanData, decryptedScanData)
            if (rootComponent == null) {
                Log.e(TAG, "Factory failed to create components")
                return@withContext ComponentCreationResult(
                    success = false,
                    errorMessage = "Failed to create components from scan data"
                )
            }
            
            Log.i(TAG, "Successfully created component hierarchy: ${rootComponent.name}")
            return@withContext ComponentCreationResult(
                success = true,
                rootComponent = rootComponent,
                totalComponentsCreated = countHierarchyComponents(rootComponent.id)
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in component creation workflow", e)
            ComponentCreationResult(
                success = false,
                errorMessage = "Unexpected error: ${e.message}"
            )
        }
    }
    
    /**
     * Find best product match from FilamentInfo for SKU lookup
     */
    suspend fun findBestProductMatch(filamentInfo: FilamentInfo): CatalogSku? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Looking up SKU for ${filamentInfo.filamentType}/${filamentInfo.colorName}")
            
            // First try exact match by material type and color name
            val manufacturerId = determineManufacturer(filamentInfo)
            val products = findProducts(manufacturerId, null, filamentInfo.filamentType)
            
            val exactMatch = products.firstOrNull { product ->
                product.colorName.equals(filamentInfo.colorName, ignoreCase = true)
            }
            
            if (exactMatch != null) {
                Log.d(TAG, "Found exact product match: ${exactMatch.variantId}")
                return@withContext CatalogSku(
                    sku = exactMatch.variantId,
                    productName = exactMatch.productName,
                    manufacturer = exactMatch.manufacturer,
                    materialType = exactMatch.materialType,
                    colorName = exactMatch.colorName,
                    colorHex = exactMatch.colorHex,
                    filamentWeightGrams = exactMatch.filamentWeightGrams,
                    url = exactMatch.url,
                    componentDefaults = getComponentDefaultsForSku(manufacturerId, exactMatch.variantId)
                )
            }
            
            // Try fuzzy color matching
            val fuzzyMatch = findFuzzyColorMatch(products, filamentInfo.colorName)
            if (fuzzyMatch != null) {
                Log.d(TAG, "Found fuzzy product match: ${fuzzyMatch.variantId}")
                return@withContext CatalogSku(
                    sku = fuzzyMatch.variantId,
                    productName = fuzzyMatch.productName,
                    manufacturer = fuzzyMatch.manufacturer,
                    materialType = fuzzyMatch.materialType,
                    colorName = fuzzyMatch.colorName,
                    colorHex = fuzzyMatch.colorHex,
                    filamentWeightGrams = fuzzyMatch.filamentWeightGrams,
                    url = fuzzyMatch.url,
                    componentDefaults = getComponentDefaultsForSku(manufacturerId, fuzzyMatch.variantId)
                )
            }
            
            Log.d(TAG, "No SKU match found for ${filamentInfo.filamentType}/${filamentInfo.colorName}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding product match", e)
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
     * Find a product by SKU ID for a specific manufacturer
     */
    fun findProductBySku(manufacturerId: String, skuId: String): ProductEntry? {
        return catalogRepo.findProductBySku(manufacturerId, skuId)
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
            
            // 2. Check legacy inventory items
            getInventoryItem(uniqueIdentifier)?.let { inventoryItem ->
                Log.d(TAG, "Found legacy inventory item")
                return@withContext InventoryResolutionResult(
                    success = true,
                    legacyInventoryItem = inventoryItem,
                    source = "LegacyRepository"
                )
            }
            
            // 3. Try RFID resolution if comprehensive fallback enabled
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
    
    // === Batch Operations ===
    
    /**
     * Create multiple components from batch scan data
     */
    suspend fun createComponentsBatch(
        scanDataList: List<Pair<EncryptedScanData, DecryptedScanData>>
    ): BatchComponentCreationResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<ComponentCreationResult>()
        var successCount = 0
        
        scanDataList.forEach { (encrypted, decrypted) ->
            val result = createComponentsFromScan(encrypted, decrypted)
            results.add(result)
            if (result.success) successCount++
        }
        
        BatchComponentCreationResult(
            totalProcessed = scanDataList.size,
            successCount = successCount,
            failureCount = scanDataList.size - successCount,
            results = results
        )
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
    private fun findFuzzyColorMatch(products: List<ProductEntry>, targetColor: String): ProductEntry? {
        val normalizedTarget = targetColor.lowercase().trim()
        
        return products.find { product ->
            val normalizedProductColor = product.colorName.lowercase().trim()
            
            // Check for partial matches or common variations
            when {
                normalizedProductColor.contains(normalizedTarget) -> true
                normalizedTarget.contains(normalizedProductColor) -> true
                normalizedTarget.replace(" ", "") == normalizedProductColor.replace(" ", "") -> true
                else -> false
            }
        }
    }
    
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
     * Get all products for a manufacturer
     * Combines catalog data with user-added products
     */
    fun getProducts(manufacturerId: String): List<ProductEntry> {
        val products = mutableListOf<ProductEntry>()
        
        // For Bambu Lab, use mapper-based product lookup service
        if (manufacturerId == "bambu") {
            products.addAll(ProductLookupService.getAllProducts())
        } else {
            // Add catalog products for non-Bambu manufacturers
            products.addAll(catalogRepo.getProducts(manufacturerId))
        }
        
        // Add user-added products from custom manufacturers
        val customManufacturer = userRepo.getUserData().customMappings.manufacturers[manufacturerId]
        if (customManufacturer != null) {
            // Convert custom manufacturer products to ProductEntry format
            customManufacturer.materials.forEach { (materialId, material) ->
                customManufacturer.colorPalette.forEach { (hex, colorName) ->
                    val productEntry = ProductEntry(
                        variantId = "${manufacturerId}_${materialId}_${hex.replace("#", "")}",
                        productHandle = material.displayName.lowercase().replace(" ", "-"),
                        productName = material.displayName,
                        colorName = colorName,
                        colorHex = hex,
                        colorCode = materialId,
                        price = 0.0, // Custom products don't have pricing
                        available = true,
                        url = "", // Custom products don't have store URLs
                        manufacturer = customManufacturer.displayName,
                        materialType = materialId.uppercase(),
                        internalCode = materialId,
                        lastUpdated = java.time.LocalDateTime.now().toString(),
                        filamentWeightGrams = 1000f, // Default 1kg for custom materials
                        spoolType = SpoolPackaging.REFILL // Default for custom products
                    )
                    products.add(productEntry)
                }
            }
        }
        
        return products
    }
    
    /**
     * Find products by color and material for a manufacturer
     */
    fun findProducts(
        manufacturerId: String,
        hex: String? = null,
        materialType: String? = null
    ): List<ProductEntry> {
        val products = getProducts(manufacturerId)
        return products.filter { product ->
            val hexMatches = hex?.let { product.colorHex?.equals(it, ignoreCase = true) } ?: true
            val materialMatches = materialType?.let { product.materialType.equals(it, ignoreCase = true) } ?: true
            hexMatches && materialMatches
        }
    }
    
    /**
     * Find best product match by material type and color name
     */
    fun findBestProductMatch(filamentType: String, colorName: String): ProductEntry? {
        // First try Bambu Lab (most common case)
        findProducts("bambu", materialType = filamentType).find { 
            it.colorName.equals(colorName, ignoreCase = true) 
        }?.let { return it }
        
        // Try other manufacturers
        catalogRepo.getManufacturers().keys.forEach { manufacturerId ->
            if (manufacturerId != "bambu") {
                findProducts(manufacturerId, materialType = filamentType).find { product ->
                    product.colorName.equals(colorName, ignoreCase = true)
                }?.let { return it }
            }
        }
        
        return null
    }
    
    // === Component Management ===
    
    /**
     * Get a physical component by ID
     */
    fun getComponent(componentId: String): PhysicalComponent? {
        return userRepo.getComponent(componentId)
    }
    
    /**
     * Get all physical components
     */
    fun getComponents(): Map<String, PhysicalComponent> {
        return userRepo.getComponents()
    }
    
    /**
     * Save a physical component
     */
    fun saveComponent(component: PhysicalComponent) {
        userRepo.saveComponent(component)
    }
    
    /**
     * Create components automatically from catalog defaults for a manufacturer
     */
    fun createDefaultComponents(
        manufacturerId: String,
        filamentType: String,
        trayUid: String
    ): List<PhysicalComponent> {
        val manufacturer = catalogRepo.getManufacturer(manufacturerId)
        val components = mutableListOf<PhysicalComponent>()
        
        if (manufacturer != null) {
            // Create components from catalog data
            // Create filament component
            val filamentDefault = manufacturer.componentDefaults["filament_1kg"]
            if (filamentDefault != null) {
                val filamentComponent = PhysicalComponent(
                    id = "${trayUid}_filament",
                    name = "${manufacturer.materials[filamentType]?.displayName ?: filamentType} Filament",
                    type = PhysicalComponentType.FILAMENT,
                    massGrams = filamentDefault.massGrams,
                    fullMassGrams = filamentDefault.massGrams,
                    variableMass = true,
                    manufacturer = manufacturerId,
                    description = filamentDefault.description
                )
                components.add(filamentComponent)
                saveComponent(filamentComponent)
            }
            
            // Create spool component
            val spoolDefault = manufacturer.componentDefaults["spool_standard"]
            if (spoolDefault != null) {
                val spoolComponent = PhysicalComponent(
                    id = "${trayUid}_spool",
                    name = spoolDefault.name,
                    type = PhysicalComponentType.BASE_SPOOL,
                    massGrams = spoolDefault.massGrams,
                    fullMassGrams = spoolDefault.massGrams,
                    variableMass = false,
                    manufacturer = manufacturerId,
                    description = spoolDefault.description
                )
                components.add(spoolComponent)
                saveComponent(spoolComponent)
            }
            
            // Create core component
            val coreDefault = manufacturer.componentDefaults["core_cardboard"]
            if (coreDefault != null) {
                val coreComponent = PhysicalComponent(
                    id = "${trayUid}_core",
                    name = coreDefault.name,
                    type = PhysicalComponentType.CORE_RING,
                    massGrams = coreDefault.massGrams,
                    fullMassGrams = coreDefault.massGrams,
                    variableMass = false,
                    manufacturer = manufacturerId,
                    description = coreDefault.description
                )
                components.add(coreComponent)
                saveComponent(coreComponent)
            }
        } else {
            // Create basic default components when no catalog data is available
            Log.w(TAG, "No manufacturer data found for '$manufacturerId', creating basic default components")
            
            // Create basic filament component
            val filamentComponent = PhysicalComponent(
                id = "${trayUid}_filament",
                name = "$filamentType Filament",
                type = PhysicalComponentType.FILAMENT,
                massGrams = 1000f, // Default 1kg filament
                fullMassGrams = 1000f,
                variableMass = true,
                manufacturer = manufacturerId,
                description = "Default filament component"
            )
            components.add(filamentComponent)
            saveComponent(filamentComponent)
            
            // Create basic spool component
            val spoolComponent = PhysicalComponent(
                id = "${trayUid}_spool",
                name = "Standard Spool",
                type = PhysicalComponentType.BASE_SPOOL,
                massGrams = 212f, // Standard Bambu spool weight
                fullMassGrams = 212f,
                variableMass = false,
                manufacturer = manufacturerId,
                description = "Default spool component"
            )
            components.add(spoolComponent)
            saveComponent(spoolComponent)
            
            // Create basic core component
            val coreComponent = PhysicalComponent(
                id = "${trayUid}_core",
                name = "Cardboard Core",
                type = PhysicalComponentType.CORE_RING,
                massGrams = 33f, // Standard cardboard core weight
                fullMassGrams = 33f,
                variableMass = false,
                manufacturer = manufacturerId,
                description = "Default core component"
            )
            components.add(coreComponent)
            saveComponent(coreComponent)
        }
        
        Log.i(TAG, "Created ${components.size} default components for $manufacturerId $filamentType")
        return components
    }
    
    // === Inventory Management ===
    
    /**
     * Get an inventory item by tray UID
     */
    fun getInventoryItem(trayUid: String): InventoryItem? {
        return userRepo.getInventoryItem(trayUid)
    }
    
    /**
     * Get all inventory items
     */
    fun getInventoryItems(): Map<String, InventoryItem> {
        return userRepo.getInventoryItems()
    }
    
    /**
     * Save an inventory item
     */
    fun saveInventoryItem(item: InventoryItem) {
        userRepo.saveInventoryItem(item)
    }
    
    /**
     * Create inventory item with automatic component setup
     */
    fun createInventoryItemWithComponents(
        trayUid: String,
        manufacturerId: String,
        filamentType: String
    ): InventoryItem {
        val components = createDefaultComponents(manufacturerId, filamentType, trayUid)
        val componentIds = components.map { it.id }
        
        val inventoryItem = InventoryItem(
            trayUid = trayUid,
            components = componentIds,
            totalMeasuredMass = null,
            measurements = emptyList(),
            lastUpdated = java.time.LocalDateTime.now(),
            notes = ""
        )
        
        saveInventoryItem(inventoryItem)
        return inventoryItem
    }
    
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
    
    /**
     * Safe factory creation with context check
     */
    private fun createFactorySafely(encryptedScanData: EncryptedScanData): ComponentFactory? {
        return try {
            val contextToUse = context ?: throw IllegalStateException("Context required for component factory operations")
            ComponentFactory.createFactory(contextToUse, encryptedScanData)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating component factory", e)
            null
        }
    }
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
    val legacyInventoryItem: InventoryItem? = null,
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