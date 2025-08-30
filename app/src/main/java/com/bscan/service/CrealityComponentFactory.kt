package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.interpreter.CrealityFormatInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * ComponentFactory implementation for Creality 3D printer RFID tags.
 * 
 * Creality tags typically use a simpler format with basic material information.
 * Creates single independent components (not hierarchical like Bambu).
 * 
 * Component Strategy:
 * - Creates one component per scan representing the complete filament spool
 * - Uses hardware UID as unique identifier (no separate tray UID)
 * - Components are independent inventory items, not part of a hierarchy
 * - Supports basic material and color information
 * 
 * Tag Format Support:
 * - Detects Creality format based on data patterns
 * - Handles simplified data structure compared to Bambu
 * - No encryption/authentication required
 */
class CrealityComponentFactory(context: Context) : ComponentFactory(context) {
    
    private val interpreter = CrealityFormatInterpreter()
    
    override val factoryType: String = "CrealityComponentFactory"
    
    override fun supportsTagFormat(encryptedScanData: EncryptedScanData): Boolean {
        return try {
            // Check for Creality format indicators
            val dataSize = encryptedScanData.encryptedData.size
            val technology = encryptedScanData.technology
            
            // Creality tags are typically simpler format
            // Look for specific data patterns or sizes that indicate Creality
            when {
                // Check for Creality-specific data size or patterns
                dataSize < 768 -> true // Smaller than Bambu format
                technology.contains("MIFARE", ignoreCase = true) && 
                    !hasBambuSignature(encryptedScanData.encryptedData) -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(factoryType, "Error checking Creality tag format", e)
            false
        }
    }
    
    override suspend fun processScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component? = withContext(Dispatchers.IO) {
        try {
            Log.d(factoryType, "Processing Creality RFID scan for tag: ${encryptedScanData.tagUid}")
            
            if (decryptedScanData.scanResult != com.bscan.model.ScanResult.SUCCESS) {
                Log.w(factoryType, "Scan failed: ${decryptedScanData.scanResult}")
                return@withContext null
            }
            
                    // Interpret the decrypted data
            val filamentInfo = interpreter.interpret(decryptedScanData)
            if (filamentInfo == null) {
                Log.e(factoryType, "Failed to interpret Creality tag data")
                return@withContext null
            }
            
            // Convert to Creality-specific info
            val crealityInfo = CrealityFilamentInfo(
                material = filamentInfo.filamentType,
                colorName = filamentInfo.colorName,
                colorHex = filamentInfo.colorHex,
                diameter = filamentInfo.filamentDiameter,
                printTemperature = filamentInfo.maxTemperature,
                bedTemperature = filamentInfo.bedTemperature
            )
            
            // Check if component already exists
            val existingComponent = componentRepository.findInventoryByUniqueId(encryptedScanData.tagUid)
            if (existingComponent != null) {
                Log.i(factoryType, "Component already exists for tag: ${encryptedScanData.tagUid}")
                return@withContext updateExistingComponent(existingComponent, crealityInfo)
            }
            
            // Create new component
            val components = createComponents(
                tagUid = encryptedScanData.tagUid,
                interpretedData = crealityInfo,
                metadata = buildScanMetadata(encryptedScanData, decryptedScanData)
            )
            
            val rootComponent = components.firstOrNull()
            if (rootComponent != null) {
                // Create inventory item
                createInventoryItem(
                    rootComponent = rootComponent,
                    notes = "Creality filament spool - ${crealityInfo.material} ${crealityInfo.colorName}"
                )
                
                Log.i(factoryType, "Created Creality component: ${rootComponent.name}")
            }
            
            return@withContext rootComponent
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error processing Creality RFID scan", e)
            null
        }
    }
    
    override suspend fun createComponents(
        tagUid: String,
        interpretedData: Any,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        try {
            val crealityInfo = interpretedData as? CrealityFilamentInfo
            if (crealityInfo == null) {
                Log.e(factoryType, "Invalid interpreted data type for Creality")
                return@withContext emptyList()
            }
            
            // Look up SKU data for mass and additional properties
            val skuData = lookupSkuData(
                material = crealityInfo.material,
                color = crealityInfo.colorName,
                manufacturer = "Creality"
            )
            
            // Determine filament mass
            val filamentMass = skuData?.filamentWeightGrams 
                ?: getDefaultMassByMaterial(crealityInfo.material)
            
            // Create single component representing complete spool
            val component = Component(
                id = generateComponentId("creality_spool"),
                identifiers = listOf(
                    ComponentIdentifier(
                        type = IdentifierType.RFID_HARDWARE,
                        value = tagUid,
                        purpose = IdentifierPurpose.TRACKING,
                        metadata = mapOf(
                            "manufacturer" to "Creality",
                            "chipType" to "ntag213",
                            "format" to "hex",
                            "encoding" to "ascii"
                        )
                    )
                ),
                name = "Creality ${crealityInfo.material} - ${crealityInfo.colorName}",
                category = "filament-spool",
                tags = listOf("creality", "consumable", "variable-mass", "inventory-item"),
                parentComponentId = null, // Independent component
                childComponents = emptyList(), // No children for Creality
                massGrams = filamentMass,
                fullMassGrams = filamentMass,
                variableMass = true,
                manufacturer = "Creality",
                description = "Creality ${crealityInfo.material} filament spool in ${crealityInfo.colorName}",
                metadata = buildComponentMetadata(crealityInfo, skuData, metadata),
                createdAt = System.currentTimeMillis(),
                lastUpdated = LocalDateTime.now()
            )
            
            // Component generated fresh each time - no persistence needed
            
            Log.d(factoryType, "Created Creality component: ${component.name} (${filamentMass}g)")
            return@withContext listOf(component)
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error creating Creality components", e)
            emptyList()
        }
    }
    
    override fun extractUniqueIdentifier(decryptedScanData: DecryptedScanData): String? {
        // For Creality, use the hardware tag UID as unique identifier
        return decryptedScanData.tagUid
    }
    
    /**
     * Update existing component with new scan data
     */
    private suspend fun updateExistingComponent(
        existingComponent: Component,
        crealityInfo: CrealityFilamentInfo
    ): Component = withContext(Dispatchers.IO) {
        try {
            // Update metadata with latest scan information
            val updatedMetadata = existingComponent.metadata + mapOf(
                "lastScanned" to LocalDateTime.now().toString(),
                "scanCount" to (existingComponent.metadata["scanCount"]?.toIntOrNull()?.plus(1) ?: 1).toString()
            )
            
            val updatedComponent = existingComponent.copy(
                metadata = updatedMetadata,
                lastUpdated = LocalDateTime.now()
            )
            
            // Component updated in-memory only - no persistence needed
            Log.d(factoryType, "Updated existing Creality component: ${updatedComponent.name}")
            
            updatedComponent
        } catch (e: Exception) {
            Log.e(factoryType, "Error updating existing component", e)
            existingComponent
        }
    }
    
    /**
     * Build comprehensive metadata for Creality component
     */
    private suspend fun buildComponentMetadata(
        crealityInfo: CrealityFilamentInfo,
        skuData: SkuData?,
        scanMetadata: Map<String, String>
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, String>(
            "material" to crealityInfo.material,
            "colorName" to crealityInfo.colorName,
            "diameter" to crealityInfo.diameter.toString(),
            "source" to "creality-scan",
            "factoryType" to factoryType,
            "scanCount" to "1"
        )
        
        // Add color hex if available
        crealityInfo.colorHex?.let { metadata["colorHex"] = it }
        
        // Add temperature information if available
        crealityInfo.printTemperature?.let { metadata["printTemperature"] = it.toString() }
        crealityInfo.bedTemperature?.let { metadata["bedTemperature"] = it.toString() }
        
        // Add SKU information if found
        skuData?.let { sku ->
            metadata["linkedSku"] = sku.sku
            metadata["productName"] = sku.productName
            metadata["skuSource"] = "catalog-lookup"
            sku.filamentWeightGrams?.let { metadata["expectedWeightGrams"] = it.toString() }
        }
        
        // Add scan metadata
        metadata.putAll(scanMetadata)
        
        return@withContext metadata
    }
    
    /**
     * Build metadata from scan process
     */
    private fun buildScanMetadata(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Map<String, String> {
        return mapOf(
            "tagUid" to encryptedScanData.tagUid,
            "technology" to encryptedScanData.technology,
            "scanTimestamp" to encryptedScanData.timestamp.toString(),
            "scanDurationMs" to encryptedScanData.scanDurationMs.toString(),
            "authenticatedSectors" to decryptedScanData.authenticatedSectors.joinToString(","),
            "blockCount" to decryptedScanData.decryptedBlocks.size.toString()
        )
    }
    
    /**
     * Check if data has Bambu-specific signature patterns
     */
    private fun hasBambuSignature(data: ByteArray): Boolean {
        return try {
            // Look for patterns that indicate Bambu format
            // This could be specific byte sequences or data structure patterns
            val hexString = data.joinToString("") { "%02X".format(it) }
            
            // Check for known Bambu patterns (placeholder - would need actual patterns)
            hexString.contains("BAMBU", ignoreCase = true) ||
            data.size == 768 || data.size == 1024 // Bambu-specific sizes
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get default mass based on material type for Creality filaments
     */
    private fun getDefaultMassByMaterial(material: String): Float {
        return when (material.uppercase()) {
            "PLA" -> 1000f      // Standard 1kg spools
            "ABS" -> 1000f      
            "PETG" -> 1000f     
            "TPU" -> 500f       // TPU typically 500g
            "WOOD", "SILK" -> 1000f
            else -> 1000f       // Default 1kg
        }
    }
    
    companion object {
        private const val TAG = "CrealityComponentFactory"
    }
}

/**
 * Data class for Creality filament information
 */
data class CrealityFilamentInfo(
    val material: String,
    val colorName: String,
    val colorHex: String? = null,
    val diameter: Float = 1.75f,
    val printTemperature: Int? = null,
    val bedTemperature: Int? = null,
    val brand: String = "Creality",
    val spoolWeight: Float? = null
)