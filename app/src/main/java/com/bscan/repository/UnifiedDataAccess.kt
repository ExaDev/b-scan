package com.bscan.repository

import android.util.Log
import com.bscan.model.*
import com.bscan.data.BambuProductDatabase
import com.bscan.interpreter.InterpreterFactory

/**
 * Unified data access layer that merges catalog data (build-time) with user data (runtime).
 * Provides a single interface for accessing all app data with proper override logic.
 * This replaces the old UnifiedDataRepository approach.
 */
class UnifiedDataAccess(
    private val catalogRepo: CatalogRepository,
    private val userRepo: UserDataRepository
) {
    
    companion object {
        private const val TAG = "UnifiedDataAccess"
    }
    
    // === RFID Resolution ===
    
    /**
     * Resolve RFID tag to filament information using all available sources
     */
    fun resolveRfidTag(tagUid: String, tagData: ByteArray): FilamentInfo? {
        Log.d(TAG, "Resolving RFID tag: $tagUid")
        
        // 1. Check user custom mappings first (highest priority)
        userRepo.findCustomRfidMapping(tagUid)?.let { customMapping ->
            Log.d(TAG, "Found custom RFID mapping for $tagUid")
            return createFilamentInfoFromCustomMapping(customMapping)
        }
        
        // 2. Check catalog RFID mappings (manufacturer defaults)
        catalogRepo.findRfidMapping(tagUid)?.let { (manufacturerId, rfidMapping) ->
            Log.d(TAG, "Found catalog RFID mapping for $tagUid in $manufacturerId")
            return createFilamentInfoFromCatalogMapping(manufacturerId, rfidMapping)
        }
        
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
     * Combines built-in BambuProductDatabase with user-added products
     */
    fun getProducts(manufacturerId: String): List<ProductEntry> {
        val products = mutableListOf<ProductEntry>()
        
        // Add built-in products from BambuProductDatabase for Bambu Lab
        if (manufacturerId == "bambu") {
            products.addAll(convertBambuProductsToEntries())
        }
        
        // Add catalog products (if any)
        products.addAll(catalogRepo.getProducts(manufacturerId))
        
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
     * Convert BambuProductDatabase products to ProductEntry format
     */
    private fun convertBambuProductsToEntries(): List<ProductEntry> {
        return BambuProductDatabase.getAllProducts().map { bambuProduct ->
            ProductEntry(
                variantId = bambuProduct.retailSku ?: "${bambuProduct.internalCode}_${bambuProduct.colorHex.replace("#", "")}",
                productHandle = bambuProduct.productLine.lowercase().replace(" ", "-"),
                productName = bambuProduct.productLine,
                colorName = bambuProduct.colorName,
                colorHex = bambuProduct.colorHex,
                colorCode = bambuProduct.internalCode,
                price = 0.0, // Price not available in BambuProductDatabase
                available = true,
                url = bambuProduct.spoolUrl ?: bambuProduct.refillUrl ?: "",
                manufacturer = "Bambu Lab",
                materialType = inferMaterialTypeFromProductLine(bambuProduct.productLine),
                internalCode = bambuProduct.internalCode,
                lastUpdated = "2025-08-26T00:00:00Z", // Static timestamp for built-in data
                filamentWeightGrams = parseFilamentWeight(bambuProduct.mass),
                spoolType = if (bambuProduct.spoolUrl != null) SpoolPackaging.WITH_SPOOL else SpoolPackaging.REFILL
            )
        }
    }
    
    /**
     * Infer material type from product line name
     */
    private fun inferMaterialTypeFromProductLine(productLine: String): String {
        return when {
            productLine.contains("PLA Basic") -> "PLA_BASIC"
            productLine.contains("PLA Matte") -> "PLA_MATTE"
            productLine.contains("PLA Silk") -> "PLA_SILK"
            productLine.contains("PLA Metal") -> "PLA_METAL"
            productLine.contains("PLA Wood") -> "PLA_WOOD"
            productLine.contains("PLA Marble") -> "PLA_MARBLE"
            productLine.contains("PLA Glow") -> "PLA_GLOW"
            productLine.contains("ABS") -> "ABS"
            productLine.contains("ASA") -> "ASA"
            productLine.contains("PETG") -> "PETG"
            productLine.contains("TPU") -> "TPU"
            productLine.contains("PA") || productLine.contains("Nylon") -> "PA_NYLON"
            productLine.contains("PC") || productLine.contains("Polycarbonate") -> "PC"
            productLine.contains("PET-CF") -> "PET_CF"
            productLine.contains("Support") -> "SUPPORT"
            productLine.contains("PVA") -> "PVA"
            else -> productLine.uppercase().replace(" ", "_")
        }
    }
    
    /**
     * Parse filament weight from mass string
     */
    private fun parseFilamentWeight(massString: String): Float? {
        return when {
            massString.contains("0.5kg") || massString.contains("500g") -> 500f
            massString.contains("0.75kg") || massString.contains("750g") -> 750f
            massString.contains("1kg") || massString.contains("1000g") -> 1000f
            else -> 1000f // Default to 1kg
        }
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
            ?: throw IllegalArgumentException("Unknown manufacturer: $manufacturerId")
        
        val components = mutableListOf<PhysicalComponent>()
        
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
     * Get all interpreted scans
     */
    fun getAllScans(): List<InterpretedScan> {
        // This delegates to the user repository for scan data
        return userRepo.getAllInterpretedScans()
    }
    
    /**
     * Get scans filtered by tag UID
     */
    fun getScansByTagUid(tagUid: String): List<InterpretedScan> {
        return userRepo.getScansByTagUid(tagUid)
    }
    
    /**
     * Get detailed information about a filament reel by tray UID or tag UID
     */
    fun getFilamentReelDetails(identifier: String): FilamentReelDetails? {
        return userRepo.getFilamentReelDetails(identifier)
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
    
    
    // === Legacy Compatibility Methods ===
    
    /**
     * Get current mappings in legacy format (for backward compatibility)
     */
    fun getCurrentMappings(): FilamentMappings {
        // This would convert the new format back to legacy FilamentMappings
        // for compatibility with existing interpreters until they're updated
        return FilamentMappings()
    }
    
    private fun createFilamentInfoFromCustomMapping(mapping: CustomRfidMapping): FilamentInfo {
        return FilamentInfo(
            tagUid = mapping.tagUid,
            trayUid = mapping.tagUid, // Use tag UID as tray UID for custom mappings
            filamentType = mapping.material,
            detailedFilamentType = mapping.material,
            colorHex = mapping.hex ?: "#808080",
            colorName = mapping.color,
            spoolWeight = getDefaultSpoolWeight(mapping.material),
            filamentDiameter = 1.75f,
            filamentLength = estimateFilamentLength(getDefaultSpoolWeight(mapping.material)),
            productionDate = "Custom",
            minTemperature = getDefaultMinTemp(mapping.material),
            maxTemperature = getDefaultMaxTemp(mapping.material),
            bedTemperature = getDefaultBedTemp(mapping.material),
            dryingTemperature = getDefaultDryingTemp(mapping.material),
            dryingTime = getDefaultDryingTime(mapping.material)
        )
    }
    
    private fun createFilamentInfoFromCatalogMapping(
        manufacturerId: String,
        mapping: RfidMapping
    ): FilamentInfo {
        val manufacturer = catalogRepo.getManufacturer(manufacturerId)
        val materialDef = manufacturer?.materials?.get(mapping.material)
        val tempProfile = materialDef?.temperatureProfile?.let { profileId ->
            manufacturer.temperatureProfiles[profileId]
        }
        
        return FilamentInfo(
            tagUid = mapping.rfidCode,
            trayUid = mapping.rfidCode, // Use RFID code as tray UID for catalog mappings
            manufacturerName = manufacturer?.displayName ?: manufacturerId,
            filamentType = materialDef?.displayName ?: mapping.material,
            detailedFilamentType = materialDef?.displayName ?: mapping.material,
            colorHex = mapping.hex ?: "#808080",
            colorName = mapping.color,
            spoolWeight = getDefaultSpoolWeight(mapping.material),
            filamentDiameter = 1.75f, // MaterialDefinition doesn't have diameter property
            filamentLength = estimateFilamentLength(getDefaultSpoolWeight(mapping.material)),
            productionDate = "Catalog",
            minTemperature = tempProfile?.minNozzle ?: getDefaultMinTemp(mapping.material),
            maxTemperature = tempProfile?.maxNozzle ?: getDefaultMaxTemp(mapping.material),
            bedTemperature = tempProfile?.bed ?: getDefaultBedTemp(mapping.material),
            dryingTemperature = getDefaultDryingTemp(mapping.material), // TemperatureProfile doesn't have drying properties
            dryingTime = getDefaultDryingTime(mapping.material)
        )
    }
    
    // === Helper Methods for FilamentInfo Creation ===
    
    private fun getDefaultSpoolWeight(materialType: String): Int {
        return when (materialType.uppercase()) {
            "TPU" -> 500
            "PVA", "SUPPORT" -> 500
            "PC", "PA", "PAHT" -> 1000
            else -> 1000 // Standard 1kg for PLA, PETG, ABS, ASA
        }
    }
    
    private fun getDefaultMinTemp(materialType: String): Int {
        return when (materialType.uppercase()) {
            "PLA" -> 190
            "ABS" -> 220
            "PETG" -> 220
            "TPU" -> 200
            "ASA" -> 230
            "PC" -> 260
            "PA", "PAHT" -> 250
            else -> 190
        }
    }
    
    private fun getDefaultMaxTemp(materialType: String): Int {
        return when (materialType.uppercase()) {
            "PLA" -> 220
            "ABS" -> 250
            "PETG" -> 250
            "TPU" -> 230
            "ASA" -> 250
            "PC" -> 280
            "PA", "PAHT" -> 280
            else -> 220
        }
    }
    
    private fun getDefaultBedTemp(materialType: String): Int {
        return when (materialType.uppercase()) {
            "PLA" -> 60
            "ABS" -> 80
            "PETG" -> 70
            "TPU" -> 50
            "ASA" -> 85
            "PC" -> 90
            "PA", "PAHT" -> 80
            else -> 60
        }
    }
    
    private fun getDefaultDryingTemp(materialType: String): Int {
        return when (materialType.uppercase()) {
            "PLA" -> 45
            "ABS" -> 60
            "PETG" -> 65
            "TPU" -> 40
            "ASA" -> 60
            "PC" -> 70
            "PA", "PAHT" -> 80
            else -> 45
        }
    }
    
    private fun getDefaultDryingTime(materialType: String): Int {
        return when (materialType.uppercase()) {
            "TPU" -> 12
            "PETG" -> 8
            "ABS" -> 4
            "ASA" -> 4
            "PC" -> 6
            "PA", "PAHT" -> 8
            else -> 6
        }
    }
    
    private fun estimateFilamentLength(spoolWeightGrams: Int): Int {
        // Rough estimate: 1kg PLA ≈ 330m at 1.75mm diameter
        // Adjust based on weight
        return (spoolWeightGrams * 330) / 1000
    }
}

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