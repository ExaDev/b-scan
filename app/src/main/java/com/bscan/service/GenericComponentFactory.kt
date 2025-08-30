package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * ComponentFactory implementation for unknown/generic RFID tag formats.
 * 
 * This is a fallback factory used when tag format cannot be identified by
 * more specific factories (Bambu, Creality, OpenTag).
 * 
 * Component Strategy:
 * - Creates minimal components based on available tag data
 * - Uses hardware UID as unique identifier
 * - Attempts basic data extraction where possible
 * - Focuses on data preservation and basic tracking
 * 
 * Tag Format Support:
 * - Any RFID/NFC tag format
 * - Preserves raw tag data for future reprocessing
 * - No format-specific interpretation
 * - Basic metadata extraction from scan process
 */
class GenericComponentFactory(context: Context) : ComponentFactory(context) {
    
    override val factoryType: String = "GenericComponentFactory"
    
    override fun supportsTagFormat(encryptedScanData: EncryptedScanData): Boolean {
        // Generic factory supports all formats as fallback
        return true
    }
    
    override suspend fun processScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component? = withContext(Dispatchers.IO) {
        try {
            Log.d(factoryType, "Processing generic RFID scan for tag: ${encryptedScanData.tagUid}")
            
            // Check if component already exists for this tag
            val existingComponent = componentRepository.findInventoryByUniqueId(encryptedScanData.tagUid)
            if (existingComponent != null) {
                Log.i(factoryType, "Component already exists for tag: ${encryptedScanData.tagUid}")
                return@withContext updateExistingComponent(existingComponent, encryptedScanData, decryptedScanData)
            }
            
            // Create generic component information
            val genericInfo = extractGenericInfo(encryptedScanData, decryptedScanData)
            
            // Create new component
            val components = createComponents(
                tagUid = encryptedScanData.tagUid,
                interpretedData = genericInfo,
                metadata = buildScanMetadata(encryptedScanData, decryptedScanData)
            )
            
            val rootComponent = components.firstOrNull()
            if (rootComponent != null) {
                // Create inventory item
                createInventoryItem(
                    rootComponent = rootComponent,
                    notes = "Generic RFID component - ${encryptedScanData.technology} tag"
                )
                
                Log.i(factoryType, "Created generic component: ${rootComponent.name}")
            }
            
            return@withContext rootComponent
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error processing generic RFID scan", e)
            null
        }
    }
    
    override suspend fun createComponents(
        tagUid: String,
        interpretedData: Any,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        try {
            val genericInfo = interpretedData as? GenericTagInfo
            if (genericInfo == null) {
                Log.e(factoryType, "Invalid interpreted data type for generic component")
                return@withContext emptyList()
            }
            
            // Create single generic component
            val component = Component(
                id = generateComponentId("generic"),
                identifiers = listOf(
                    ComponentIdentifier(
                        type = IdentifierType.RFID_HARDWARE,
                        value = tagUid,
                        purpose = IdentifierPurpose.TRACKING,
                        metadata = mapOf(
                            "manufacturer" to genericInfo.manufacturer,
                            "technology" to genericInfo.technology,
                            "format" to "hex"
                        )
                    )
                ),
                name = "RFID Component ${tagUid.take(8)}",
                category = "generic",
                tags = listOf("generic", "rfid", "inventory-item", genericInfo.technology.lowercase()),
                parentComponentId = null,
                childComponents = emptyList(),
                massGrams = null, // Unknown mass for generic components
                variableMass = false,
                manufacturer = genericInfo.manufacturer,
                description = "Generic RFID component - ${genericInfo.technology} technology",
                metadata = buildComponentMetadata(genericInfo, metadata),
                createdAt = System.currentTimeMillis(),
                lastUpdated = LocalDateTime.now()
            )
            
            // Component generated fresh each time - no persistence needed
            
            Log.d(factoryType, "Created generic component: ${component.name}")
            return@withContext listOf(component)
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error creating generic components", e)
            emptyList()
        }
    }
    
    override fun extractUniqueIdentifier(decryptedScanData: DecryptedScanData): String? {
        // For generic components, always use hardware UID
        return decryptedScanData.tagUid
    }
    
    /**
     * Extract basic information from unknown tag format
     */
    private fun extractGenericInfo(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): GenericTagInfo {
        // Try to determine manufacturer from technology or data patterns
        val manufacturer = detectManufacturer(encryptedScanData, decryptedScanData)
        
        // Extract any readable text data
        val extractedText = extractReadableText(decryptedScanData)
        
        // Determine likely component category based on data patterns
        val likelyCategory = detectComponentCategory(decryptedScanData, extractedText)
        
        return GenericTagInfo(
            tagUid = encryptedScanData.tagUid,
            technology = encryptedScanData.technology,
            manufacturer = manufacturer,
            dataSize = encryptedScanData.encryptedData.size,
            blockCount = decryptedScanData.decryptedBlocks.size,
            extractedText = extractedText,
            likelyCategory = likelyCategory,
            hasAuthenticatedData = decryptedScanData.scanResult == com.bscan.model.ScanResult.SUCCESS,
            rawDataHex = encryptedScanData.encryptedData.joinToString("") { "%02X".format(it) }
        )
    }
    
    /**
     * Attempt to detect manufacturer from tag technology or data patterns
     */
    private fun detectManufacturer(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): String {
        val technology = encryptedScanData.technology.uppercase()
        
        // Check technology type for manufacturer hints
        val manufacturerFromTech = when {
            technology.contains("MIFARE") -> "NXP"
            technology.contains("NTAG") -> "NXP"
            technology.contains("ULTRALIGHT") -> "NXP"
            technology.contains("DESFIRE") -> "NXP"
            technology.contains("FELICA") -> "Sony"
            else -> "Unknown"
        }
        
        // Try to extract manufacturer from data patterns
        val manufacturerFromData = extractManufacturerFromData(decryptedScanData)
        
        return manufacturerFromData ?: manufacturerFromTech
    }
    
    /**
     * Extract manufacturer information from tag data
     */
    private fun extractManufacturerFromData(decryptedScanData: DecryptedScanData): String? {
        return try {
            // Look for text patterns that might indicate manufacturer
            val allData = decryptedScanData.decryptedBlocks.values.joinToString("")
            val text = hexToAscii(allData).lowercase()
            
            when {
                text.contains("bambu") -> "Bambu Lab"
                text.contains("creality") -> "Creality"
                text.contains("prusa") -> "Prusa Research"
                text.contains("anycubic") -> "Anycubic"
                text.contains("elegoo") -> "Elegoo"
                text.contains("ender") -> "Creality"
                text.contains("voron") -> "Voron Design"
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract readable text from decrypted data
     */
    private fun extractReadableText(decryptedScanData: DecryptedScanData): String {
        return try {
            val allData = decryptedScanData.decryptedBlocks.values.joinToString("")
            val ascii = hexToAscii(allData)
            
            // Filter to printable ASCII characters and clean up
            ascii.filter { it.code in 32..126 }
                .replace(Regex("[\\x00-\\x1F\\x7F]+"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(100) // Limit length
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Detect likely component category from data patterns
     */
    private fun detectComponentCategory(
        decryptedScanData: DecryptedScanData,
        extractedText: String
    ): String {
        val text = extractedText.lowercase()
        
        return when {
            text.contains("filament") || text.contains("pla") || text.contains("abs") || 
            text.contains("petg") || text.contains("tpu") -> "filament"
            text.contains("nozzle") -> "nozzle"
            text.contains("tool") -> "tool"
            text.contains("part") -> "component"
            text.contains("spool") -> "spool"
            text.contains("material") -> "material"
            decryptedScanData.decryptedBlocks.size > 10 -> "complex-component"
            else -> "generic"
        }
    }
    
    /**
     * Convert hex string to ASCII text
     */
    private fun hexToAscii(hex: String): String {
        return try {
            val cleanHex = hex.replace(" ", "")
            if (cleanHex.length % 2 != 0) return ""
            
            val bytes = cleanHex.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Update existing component with new scan data
     */
    private suspend fun updateExistingComponent(
        existingComponent: Component,
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component = withContext(Dispatchers.IO) {
        try {
            // Update metadata with latest scan information
            val scanMetadata = buildScanMetadata(encryptedScanData, decryptedScanData)
            val updatedMetadata = existingComponent.metadata + scanMetadata + mapOf(
                "lastScanned" to LocalDateTime.now().toString(),
                "scanCount" to (existingComponent.metadata["scanCount"]?.toIntOrNull()?.plus(1) ?: 1).toString()
            )
            
            val updatedComponent = existingComponent.copy(
                metadata = updatedMetadata,
                lastUpdated = LocalDateTime.now()
            )
            
            // Component updated in-memory only - no persistence needed
            Log.d(factoryType, "Updated existing generic component: ${updatedComponent.name}")
            
            updatedComponent
        } catch (e: Exception) {
            Log.e(factoryType, "Error updating existing component", e)
            existingComponent
        }
    }
    
    /**
     * Build comprehensive metadata for generic component
     */
    private fun buildComponentMetadata(
        genericInfo: GenericTagInfo,
        scanMetadata: Map<String, String>
    ): Map<String, String> {
        val metadata = mutableMapOf<String, String>(
            "technology" to genericInfo.technology,
            "manufacturer" to genericInfo.manufacturer,
            "dataSize" to genericInfo.dataSize.toString(),
            "blockCount" to genericInfo.blockCount.toString(),
            "likelyCategory" to genericInfo.likelyCategory,
            "hasAuthenticatedData" to genericInfo.hasAuthenticatedData.toString(),
            "source" to "generic-scan",
            "factoryType" to factoryType,
            "scanCount" to "1"
        )
        
        // Add extracted text if available
        if (genericInfo.extractedText.isNotEmpty()) {
            metadata["extractedText"] = genericInfo.extractedText
        }
        
        // Add scan metadata
        metadata.putAll(scanMetadata)
        
        return metadata
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
            "scanResult" to decryptedScanData.scanResult.name,
            "authenticatedSectors" to decryptedScanData.authenticatedSectors.joinToString(","),
            "failedSectors" to decryptedScanData.failedSectors.joinToString(","),
            "errorCount" to decryptedScanData.errors.size.toString()
        )
    }
    
    companion object {
        private const val TAG = "GenericComponentFactory"
    }
}

/**
 * Data class for generic tag information
 */
data class GenericTagInfo(
    val tagUid: String,
    val technology: String,
    val manufacturer: String,
    val dataSize: Int,
    val blockCount: Int,
    val extractedText: String,
    val likelyCategory: String,
    val hasAuthenticatedData: Boolean,
    val rawDataHex: String
)