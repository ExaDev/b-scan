package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import com.bscan.repository.UnifiedDataAccess
import com.bscan.interpreter.BambuFormatInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

/**
 * Factory service for creating hierarchical component structures from Bambu RFID scans.
 * Automatically creates tray components with child components for RFID tags, filament, and core.
 * 
 * Extends ComponentFactory to provide Bambu-specific component creation strategy.
 */
class BambuComponentFactory(context: Context) : ComponentFactory(context) {
    
    private val unifiedDataAccess = UnifiedDataAccess(catalogRepository, userDataRepository)
    private val bambuInterpreter = BambuFormatInterpreter(FilamentMappings.empty(), unifiedDataAccess)
    
    override val factoryType: String = "BambuComponentFactory"
    
    override fun supportsTagFormat(encryptedScanData: EncryptedScanData): Boolean {
        return try {
            // Check for Bambu format indicators
            val dataSize = encryptedScanData.encryptedData.size
            val technology = encryptedScanData.technology
            
            // Bambu tags use specific data sizes and require authentication
            when {
                dataSize == 768 || dataSize == 1024 -> true // Bambu-specific sizes
                technology.contains("MIFARE", ignoreCase = true) && dataSize >= 512 -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(factoryType, "Error checking Bambu tag format", e)
            false
        }
    }
    
    override suspend fun processScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component? = withContext(Dispatchers.IO) {
        try {
            Log.d(factoryType, "Processing Bambu RFID scan for tag: ${encryptedScanData.tagUid}")
            
            if (decryptedScanData.scanResult != ScanResult.SUCCESS) {
                Log.w(factoryType, "Scan failed: ${decryptedScanData.scanResult}")
                return@withContext null
            }
            
            // Interpret the decrypted data
            val filamentInfo = bambuInterpreter.interpret(decryptedScanData)
            if (filamentInfo == null) {
                Log.e(factoryType, "Failed to interpret Bambu tag data")
                return@withContext null
            }
            
            // Create RFID tag component
            val rfidTagComponent = createRfidTagComponent(encryptedScanData.tagUid, filamentInfo.trayUid, filamentInfo)
            componentRepository.saveComponent(rfidTagComponent)
            
            // Find or create tray component
            var trayComponent = componentRepository.findInventoryByUniqueId(filamentInfo.trayUid)
            
            if (trayComponent == null) {
                // Create new tray with all standard components
                trayComponent = createCompleteTrayComponent(filamentInfo.trayUid, filamentInfo, rfidTagComponent.id)
                Log.i(factoryType, "Created new tray component: ${trayComponent.name}")
            } else {
                // Add this RFID tag to existing tray
                trayComponent = trayComponent.withChildComponent(rfidTagComponent.id)
                componentRepository.saveComponent(trayComponent)
                Log.i(factoryType, "Added RFID tag ${encryptedScanData.tagUid} to existing tray: ${trayComponent.name}")
            }
            
            return@withContext trayComponent
        } catch (e: Exception) {
            Log.e(factoryType, "Error processing Bambu RFID scan", e)
            null
        }
    }
    
    override suspend fun createComponents(
        tagUid: String,
        interpretedData: Any,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        val filamentInfo = interpretedData as? FilamentInfo
        if (filamentInfo == null) {
            Log.e(factoryType, "Invalid interpreted data type for Bambu")
            return@withContext emptyList()
        }
        
        // Create RFID tag component first
        val rfidTagComponent = createRfidTagComponent(tagUid, filamentInfo.trayUid, filamentInfo)
        componentRepository.saveComponent(rfidTagComponent)
        
        // Create complete tray component hierarchy
        val trayComponent = createCompleteTrayComponent(filamentInfo.trayUid, filamentInfo, rfidTagComponent.id)
        return@withContext listOf(trayComponent)
    }
    
    override fun extractUniqueIdentifier(decryptedScanData: DecryptedScanData): String? {
        return try {
            val filamentInfo = bambuInterpreter.interpret(decryptedScanData)
            filamentInfo?.trayUid
        } catch (e: Exception) {
            Log.e(factoryType, "Error extracting unique identifier", e)
            null
        }
    }
    
    
    /**
     * Create a complete tray component with all standard child components
     */
    private suspend fun createCompleteTrayComponent(
        trayUid: String,
        filamentInfo: FilamentInfo,
        rfidTagComponentId: String
    ): Component = withContext(Dispatchers.IO) {
        
        // Create filament component
        val filamentComponent = createFilamentComponent(filamentInfo)
        componentRepository.saveComponent(filamentComponent)
        
        // Create core component
        val coreComponent = createCoreComponent()
        componentRepository.saveComponent(coreComponent)
        
        // Create spool component
        val spoolComponent = createSpoolComponent()
        componentRepository.saveComponent(spoolComponent)
        
        // Create tray component containing all others
        val trayComponent = Component(
            id = generateComponentId("tray"),
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = trayUid,
                    purpose = IdentifierPurpose.TRACKING,
                    metadata = mapOf(
                        "format" to "hex",
                        "source" to "bambu_rfid_application_data"
                    )
                )
            ),
            name = "Bambu ${filamentInfo.filamentType} - ${filamentInfo.colorName}",
            category = "filament-tray",
            tags = listOf("bambu", "composite", "inventory-item"),
            childComponents = listOf(
                rfidTagComponentId,
                filamentComponent.id,
                coreComponent.id,
                spoolComponent.id
            ),
            massGrams = null, // Will be calculated from children
            manufacturer = "Bambu Lab",
            description = "Bambu filament tray with RFID tags, filament, and core",
            metadata = buildTrayMetadata(trayUid, filamentInfo),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
        
        // Update child components to reference this tray as parent
        listOf(filamentComponent, coreComponent, spoolComponent).forEach { child ->
            val updatedChild = child.copy(parentComponentId = trayComponent.id)
            componentRepository.saveComponent(updatedChild)
        }
        
        componentRepository.saveComponent(trayComponent)
        
        // Create inventory item
        createInventoryItem(
            rootComponent = trayComponent,
            notes = "Bambu filament tray - ${filamentInfo.filamentType} ${filamentInfo.colorName}"
        )
        
        return@withContext trayComponent
    }
    
    /**
     * Create an RFID tag component
     */
    private suspend fun createRfidTagComponent(
        tagUid: String,
        trayUid: String,
        filamentInfo: FilamentInfo
    ): Component = withContext(Dispatchers.IO) {
        return@withContext Component(
            id = generateComponentId("rfid_tag"),
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = tagUid,
                    purpose = IdentifierPurpose.AUTHENTICATION,
                    metadata = mapOf(
                        "manufacturer" to "Bambu Lab",
                        "chipType" to "mifare-classic-1k",
                        "format" to "hex"
                    )
                )
            ),
            name = "RFID Tag $tagUid",
            category = "rfid-tag",
            tags = listOf("bambu", "identifier", "fixed-mass"),
            massGrams = 0.5f, // Negligible mass for RFID tag
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Bambu Lab RFID tag for filament identification",
            metadata = mapOf(
                "tagUid" to tagUid,
                "trayUid" to trayUid,
                "filamentType" to filamentInfo.filamentType,
                "colorName" to filamentInfo.colorName,
                "scanTimestamp" to LocalDateTime.now().toString()
            )
        )
    }
    
    /**
     * Create a filament component with mass from SKU data
     */
    private suspend fun createFilamentComponent(filamentInfo: FilamentInfo): Component = withContext(Dispatchers.IO) {
        val filamentMass = getFilamentMassFromSku(filamentInfo.filamentType, filamentInfo.colorName)
        
        return@withContext Component(
            id = generateComponentId("filament"),
            name = "${filamentInfo.filamentType} ${filamentInfo.colorName} Filament",
            category = "filament",
            tags = listOf("consumable", "variable-mass", "bambu"),
            massGrams = filamentMass,
            fullMassGrams = filamentMass,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Bambu Lab ${filamentInfo.filamentType} filament in ${filamentInfo.colorName}",
            metadata = buildFilamentMetadata(filamentInfo, filamentMass)
        )
    }
    
    /**
     * Create a cardboard core component
     */
    private suspend fun createCoreComponent(): Component = withContext(Dispatchers.IO) {
        return@withContext Component(
            id = generateComponentId("core"),
            name = "Bambu Cardboard Core",
            category = "core",
            tags = listOf("reusable", "fixed-mass", "bambu"),
            massGrams = 33f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab cardboard core (33g)",
            metadata = mapOf(
                "material" to "cardboard",
                "standardWeight" to "33g"
            )
        )
    }
    
    /**
     * Create a refillable spool component
     */
    private suspend fun createSpoolComponent(): Component = withContext(Dispatchers.IO) {
        return@withContext Component(
            id = generateComponentId("spool"),
            name = "Bambu Refillable Spool",
            category = "spool", 
            tags = listOf("reusable", "fixed-mass", "bambu"),
            massGrams = 212f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab refillable spool (212g)",
            metadata = mapOf(
                "material" to "plastic",
                "standardWeight" to "212g",
                "type" to "refillable"
            )
        )
    }
    
    /**
     * Get filament mass from SKU data with fallback to defaults
     */
    private suspend fun getFilamentMassFromSku(filamentType: String, colorName: String): Float = withContext(Dispatchers.IO) {
        try {
            val bestMatch = unifiedDataAccess.findBestProductMatch(filamentType, colorName)
            if (bestMatch?.filamentWeightGrams != null) {
                Log.d(factoryType, "Found SKU mass for $filamentType/$colorName: ${bestMatch.filamentWeightGrams}g")
                bestMatch.filamentWeightGrams
            } else {
                val defaultMass = getDefaultMassByMaterial(filamentType)
                Log.d(factoryType, "Using default mass for $filamentType: ${defaultMass}g")
                defaultMass
            }
        } catch (e: Exception) {
            Log.e(factoryType, "Error looking up SKU mass for $filamentType/$colorName", e)
            getDefaultMassByMaterial(filamentType)
        }
    }
    
    /**
     * Build comprehensive metadata for tray component with SKU linking
     */
    private suspend fun buildTrayMetadata(trayUid: String, filamentInfo: FilamentInfo): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>(
            "trayUid" to trayUid,
            "filamentType" to filamentInfo.filamentType,
            "colorName" to filamentInfo.colorName,
            "colorHex" to filamentInfo.colorHex,
            "source" to "bambu-scan"
        )
        
        // Add SKU information if available
        try {
            val bestMatch = unifiedDataAccess.findBestProductMatch(filamentInfo.filamentType, filamentInfo.colorName)
            if (bestMatch != null) {
                metadata["linkedSku"] = bestMatch.variantId
                metadata["productName"] = bestMatch.productName
                metadata["internalCode"] = bestMatch.internalCode
                metadata["skuSource"] = "automatic-match"
                metadata["purchaseUrl"] = bestMatch.url
                bestMatch.filamentWeightGrams?.let {
                    metadata["expectedWeightGrams"] = it.toString()
                }
                Log.i(factoryType, "Auto-linked tray $trayUid to SKU: ${bestMatch.variantId}")
            } else {
                metadata["skuLinkStatus"] = "no-match-found"
                Log.d(factoryType, "No SKU match found for ${filamentInfo.filamentType}/${filamentInfo.colorName}")
            }
        } catch (e: Exception) {
            metadata["skuLinkStatus"] = "lookup-error"
            Log.e(factoryType, "Error looking up SKU for tray metadata", e)
        }
        
        return@withContext metadata
    }
    
    /**
     * Build comprehensive metadata for filament component with SKU details
     */
    private suspend fun buildFilamentMetadata(filamentInfo: FilamentInfo, actualMass: Float): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>(
            "filamentType" to filamentInfo.filamentType,
            "colorName" to filamentInfo.colorName,
            "colorHex" to filamentInfo.colorHex,
            "diameter" to filamentInfo.filamentDiameter.toString(),
            "actualMassGrams" to actualMass.toString(),
            "source" to "sku-data"
        )
        
        // Add additional filament properties
        metadata["minTemperature"] = filamentInfo.minTemperature.toString()
        metadata["maxTemperature"] = filamentInfo.maxTemperature.toString()
        metadata["bedTemperature"] = filamentInfo.bedTemperature.toString()
        metadata["productionDate"] = filamentInfo.productionDate
        
        // Add exact SKU if available from scan
        filamentInfo.exactSku?.let { sku ->
            metadata["exactSku"] = sku
            metadata["skuSource"] = "rfid-scan"
        }
        
        filamentInfo.bambuProduct?.let { product ->
            metadata["retailSku"] = product.retailSku ?: ""
            metadata["productLine"] = product.productLine
        }
        
        return@withContext metadata
    }
    
    /**
     * Get default mass based on material type
     */
    private fun getDefaultMassByMaterial(filamentType: String): Float {
        return when (filamentType.uppercase()) {
            "TPU" -> 500f  // TPU typically comes in 500g spools
            "PVA", "SUPPORT" -> 500f  // Support materials typically 500g
            "PC", "PA", "PAHT" -> 1000f  // Engineering materials typically 1kg
            else -> 1000f  // Standard 1kg for PLA, PETG, ABS, ASA
        }
    }
    
    /**
     * Add additional components to an existing tray (e.g., bag, adapter)
     */
    suspend fun addComponentToTray(trayUid: String, component: Component): Boolean = withContext(Dispatchers.IO) {
        val tray = componentRepository.findInventoryByUniqueId(trayUid) ?: return@withContext false
        
        // Save the new component first
        componentRepository.saveComponent(component.copy(parentComponentId = tray.id))
        
        // Add to tray's children
        val updatedTray = tray.withChildComponent(component.id)
        componentRepository.saveComponent(updatedTray)
        
        Log.d(factoryType, "Added component ${component.name} to tray $trayUid")
        return@withContext true
    }
    
    /**
     * Infer and add a component based on total weight measurement
     */
    suspend fun inferAndAddComponent(
        trayUid: String,
        componentName: String,
        componentCategory: String,
        totalMeasuredMass: Float
    ): Component? = withContext(Dispatchers.IO) {
        val tray = componentRepository.findInventoryByUniqueId(trayUid) ?: return@withContext null
        
        // Calculate known mass from existing children
        val knownMass = tray.childComponents.sumOf { childId ->
            componentRepository.getComponent(childId)?.massGrams?.toDouble() ?: 0.0
        }.toFloat()
        
        val inferredMass = totalMeasuredMass - knownMass
        if (inferredMass < 0) {
            Log.w(factoryType, "Cannot infer component mass - total ($totalMeasuredMass) less than known mass ($knownMass)")
            return@withContext null
        }
        
        // Create inferred component
        val inferredComponent = Component(
            id = generateComponentId(componentCategory),
            name = componentName,
            category = componentCategory,
            tags = listOf("inferred", "fixed-mass"),
            massGrams = inferredMass,
            variableMass = false,
            inferredMass = true,
            description = "Component mass inferred from total measurement",
            metadata = mapOf(
                "inferredFrom" to "total_measurement",
                "totalMass" to totalMeasuredMass.toString(),
                "knownMass" to knownMass.toString()
            ),
            parentComponentId = tray.id
        )
        
        // Save and add to tray
        componentRepository.saveComponent(inferredComponent)
        val updatedTray = tray.withChildComponent(inferredComponent.id)
        componentRepository.saveComponent(updatedTray)
        
        Log.i(factoryType, "Inferred and added component: $componentName (${inferredMass}g) to tray $trayUid")
        return@withContext inferredComponent
    }
    
    /**
     * Create an InventoryItem for the tray component
     */
    private suspend fun createInventoryItemForTray(trayComponent: Component, filamentInfo: FilamentInfo) = withContext(Dispatchers.IO) {
        try {
            val trayUid = trayComponent.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)?.value
                ?: throw IllegalStateException("Tray component missing CONSUMABLE_UNIT identifier")
            
            val inventoryItem = InventoryItem(
                trayUid = trayUid,
                components = trayComponent.childComponents,
                totalMeasuredMass = null, // User hasn't measured yet
                measurements = emptyList(),
                lastUpdated = LocalDateTime.now(),
                notes = "Auto-created from Bambu RFID scan - ${filamentInfo.filamentType} ${filamentInfo.colorName}"
            )
            
            // Save to UserDataRepository
            userDataRepository.saveInventoryItem(inventoryItem)
            
            Log.i(factoryType, "Created inventory item for tray: $trayUid")
            
        } catch (e: Exception) {
            val trayUid = trayComponent.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)?.value ?: "unknown"
            Log.e(factoryType, "Error creating inventory item for tray: $trayUid", e)
        }
    }
    
    /**
     * Update existing InventoryItem when new RFID tags are scanned
     */
    private suspend fun updateInventoryItemForTray(trayComponent: Component, filamentInfo: FilamentInfo) = withContext(Dispatchers.IO) {
        try {
            val trayUid = trayComponent.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)?.value
                ?: throw IllegalStateException("Tray component missing CONSUMABLE_UNIT identifier")
                
            val existingItem = userDataRepository.getInventoryItem(trayUid)
            if (existingItem != null) {
                // Update component list and timestamp
                val updatedItem = existingItem.copy(
                    components = trayComponent.childComponents,
                    lastUpdated = LocalDateTime.now(),
                    notes = existingItem.notes + " | Additional RFID tag scanned"
                )
                
                userDataRepository.saveInventoryItem(updatedItem)
                Log.i(factoryType, "Updated inventory item for tray: $trayUid")
            } else {
                // Create new inventory item if somehow missing
                createInventoryItemForTray(trayComponent, filamentInfo)
            }
            
        } catch (e: Exception) {
            val trayUid = trayComponent.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)?.value ?: "unknown"
            Log.e(factoryType, "Error updating inventory item for tray: $trayUid", e)
        }
    }
    
    companion object {
        private const val TAG = "BambuComponentFactory"
    }
}