package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.interpreter.OpenTagInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * ComponentFactory implementation for OpenTag format RFID tags.
 * 
 * OpenTag is an open-source format designed for universal RFID component tracking.
 * Supports user-configurable component creation with flexible data structures.
 * 
 * Component Strategy:
 * - User-configurable component creation (hierarchical or independent)
 * - Supports arbitrary component types and categories
 * - Flexible metadata storage using JSON-like structures
 * - Can create single components or complex hierarchies based on tag content
 * 
 * Tag Format Support:
 * - NDEF (NFC Data Exchange Format) records
 * - JSON payload with flexible schema
 * - UTF-8 text encoding
 * - Supports component relationships and mass data
 */
class OpenTagComponentFactory(context: Context) : ComponentFactory(context) {
    
    private val interpreter = OpenTagInterpreter()
    
    override val factoryType: String = "OpenTagComponentFactory"
    
    /**
     * Helper function to create ComponentIdentifier list from optional unique identifier
     */
    private fun createIdentifiers(uniqueIdentifier: String?, tagUid: String): List<ComponentIdentifier> {
        return if (uniqueIdentifier != null) {
            listOf(
                ComponentIdentifier(
                    type = IdentifierType.CUSTOM,
                    value = uniqueIdentifier,
                    purpose = IdentifierPurpose.TRACKING,
                    metadata = mapOf(
                        "source" to "opentag_user_defined",
                        "format" to "string"
                    )
                )
            )
        } else {
            listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = tagUid,
                    purpose = IdentifierPurpose.TRACKING,
                    metadata = mapOf(
                        "source" to "rfid_hardware_uid",
                        "format" to "hex"
                    )
                )
            )
        }
    }
    
    override fun supportsTagFormat(encryptedScanData: EncryptedScanData): Boolean {
        return try {
            // Check for OpenTag format indicators
            val technology = encryptedScanData.technology
            val data = encryptedScanData.encryptedData
            
            when {
                // NTAG series commonly used for OpenTag
                technology.contains("NTAG", ignoreCase = true) -> true
                // Check for NDEF headers in the data
                hasNdefStructure(data) -> true
                // Check for OpenTag JSON signature
                hasOpenTagSignature(data) -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(factoryType, "Error checking OpenTag format", e)
            false
        }
    }
    
    override suspend fun processScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component? = withContext(Dispatchers.IO) {
        try {
            Log.d(factoryType, "Processing OpenTag RFID scan for tag: ${encryptedScanData.tagUid}")
            
            if (decryptedScanData.scanResult != com.bscan.model.ScanResult.SUCCESS) {
                Log.w(factoryType, "Scan failed: ${decryptedScanData.scanResult}")
                return@withContext null
            }
            
            // Interpret the decrypted data
            val openTagInfo = interpretOpenTagData(decryptedScanData)
            if (openTagInfo == null) {
                Log.e(factoryType, "Failed to interpret OpenTag data")
                return@withContext null
            }
            
            // Determine unique identifier from tag data or use hardware UID
            val uniqueId = openTagInfo.uniqueIdentifier ?: encryptedScanData.tagUid
            
            // Check if component already exists
            val existingComponent = componentRepository.findInventoryByUniqueId(uniqueId)
            if (existingComponent != null) {
                Log.i(factoryType, "Component already exists for ID: $uniqueId")
                return@withContext updateExistingComponent(existingComponent, openTagInfo)
            }
            
            // Create new components based on tag configuration
            val components = createComponents(
                tagUid = encryptedScanData.tagUid,
                interpretedData = openTagInfo,
                metadata = buildScanMetadata(encryptedScanData, decryptedScanData)
            )
            
            val rootComponent = findRootComponent(components)
            if (rootComponent != null && rootComponent.isInventoryItem) {
                // Legacy inventory item creation removed - using graph-based system
                Log.i(factoryType, "Created OpenTag component hierarchy with root: ${rootComponent.name}")
            }
            
            return@withContext rootComponent ?: components.firstOrNull()
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error processing OpenTag RFID scan", e)
            null
        }
    }
    
    override suspend fun createComponents(
        tagUid: String,
        interpretedData: Any,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        try {
            val openTagInfo = interpretedData as? OpenTagInfo
            if (openTagInfo == null) {
                Log.e(factoryType, "Invalid interpreted data type for OpenTag")
                return@withContext emptyList()
            }
            
            val components = mutableListOf<Component>()
            
            when (openTagInfo.creationStrategy) {
                ComponentCreationStrategy.HIERARCHICAL -> {
                    components.addAll(createHierarchicalComponents(tagUid, openTagInfo, metadata))
                }
                ComponentCreationStrategy.SIBLING_GROUP -> {
                    components.addAll(createSiblingComponents(tagUid, openTagInfo, metadata))
                }
                ComponentCreationStrategy.INDEPENDENT -> {
                    components.add(createIndependentComponent(tagUid, openTagInfo, metadata))
                }
                ComponentCreationStrategy.USER_DEFINED -> {
                    components.addAll(createUserDefinedComponents(tagUid, openTagInfo, metadata))
                }
            }
            
            // Components generated fresh each time - no persistence needed
            
            Log.d(factoryType, "Created ${components.size} OpenTag components using ${openTagInfo.creationStrategy}")
            return@withContext components
            
        } catch (e: Exception) {
            Log.e(factoryType, "Error creating OpenTag components", e)
            emptyList()
        }
    }
    
    override fun extractUniqueIdentifier(decryptedScanData: DecryptedScanData): String? {
        return try {
            val openTagInfo = interpretOpenTagData(decryptedScanData)
            openTagInfo?.uniqueIdentifier ?: decryptedScanData.tagUid
        } catch (e: Exception) {
            Log.e(factoryType, "Error extracting unique identifier", e)
            decryptedScanData.tagUid
        }
    }
    
    /**
     * Create hierarchical component structure (parent contains children)
     */
    private suspend fun createHierarchicalComponents(
        tagUid: String,
        openTagInfo: OpenTagInfo,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        val components = mutableListOf<Component>()
        
        // Create root component
        val rootComponent = Component(
            id = generateComponentId(openTagInfo.componentType),
            identifiers = createIdentifiers(openTagInfo.uniqueIdentifier, tagUid),
            name = openTagInfo.name,
            category = openTagInfo.componentType,
            tags = openTagInfo.tags + listOf("opentag", "composite", "inventory-item"),
            parentComponentId = null,
            childComponents = emptyList(), // Will be populated as we create children
            massGrams = openTagInfo.totalMass,
            fullMassGrams = openTagInfo.fullMass,
            variableMass = openTagInfo.variableMass,
            manufacturer = openTagInfo.manufacturer,
            description = openTagInfo.description,
            metadata = buildComponentMetadata(openTagInfo, metadata),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
        components.add(rootComponent)
        
        // Create child components
        val childIds = mutableListOf<String>()
        openTagInfo.childComponents.forEach { childInfo ->
            val childComponent = Component(
                id = generateComponentId(childInfo.componentType),
                identifiers = createIdentifiers(childInfo.uniqueIdentifier, tagUid),
                name = childInfo.name,
                category = childInfo.componentType,
                tags = childInfo.tags + listOf("opentag"),
                parentComponentId = rootComponent.id,
                childComponents = emptyList(),
                massGrams = childInfo.mass,
                fullMassGrams = childInfo.fullMass,
                variableMass = childInfo.variableMass,
                manufacturer = childInfo.manufacturer ?: openTagInfo.manufacturer,
                description = childInfo.description,
                metadata = childInfo.properties,
                createdAt = System.currentTimeMillis(),
                lastUpdated = LocalDateTime.now()
            )
            components.add(childComponent)
            childIds.add(childComponent.id)
        }
        
        // Update root component with child references
        if (childIds.isNotEmpty()) {
            val updatedRoot = rootComponent.copy(childComponents = childIds)
            components[0] = updatedRoot
        }
        
        return@withContext components
    }
    
    /**
     * Create sibling components under shared parent
     */
    private suspend fun createSiblingComponents(
        tagUid: String,
        openTagInfo: OpenTagInfo,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        val components = mutableListOf<Component>()
        
        // Create shared parent component
        val parentComponent = Component(
            id = generateComponentId("opentag_group"),
            identifiers = createIdentifiers(openTagInfo.uniqueIdentifier, tagUid),
            name = "OpenTag Group - ${openTagInfo.name}",
            category = "component-group",
            tags = listOf("opentag", "group", "inventory-item"),
            parentComponentId = null,
            childComponents = emptyList(),
            massGrams = null, // Will be calculated from children
            variableMass = false,
            manufacturer = openTagInfo.manufacturer,
            description = "OpenTag component group",
            metadata = buildComponentMetadata(openTagInfo, metadata),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
        components.add(parentComponent)
        
        // Create sibling components
        val siblingIds = mutableListOf<String>()
        
        // Add main component as sibling
        val mainComponent = Component(
            id = generateComponentId(openTagInfo.componentType),
            identifiers = emptyList(), // Not an inventory item
            name = openTagInfo.name,
            category = openTagInfo.componentType,
            tags = openTagInfo.tags + listOf("opentag"),
            parentComponentId = parentComponent.id,
            childComponents = emptyList(),
            massGrams = openTagInfo.totalMass,
            fullMassGrams = openTagInfo.fullMass,
            variableMass = openTagInfo.variableMass,
            manufacturer = openTagInfo.manufacturer,
            description = openTagInfo.description,
            metadata = openTagInfo.properties,
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
        components.add(mainComponent)
        siblingIds.add(mainComponent.id)
        
        // Add child components as additional siblings
        openTagInfo.childComponents.forEach { childInfo ->
            val siblingComponent = Component(
                id = generateComponentId(childInfo.componentType),
                identifiers = createIdentifiers(childInfo.uniqueIdentifier, tagUid),
                name = childInfo.name,
                category = childInfo.componentType,
                tags = childInfo.tags + listOf("opentag"),
                parentComponentId = parentComponent.id,
                childComponents = emptyList(),
                massGrams = childInfo.mass,
                fullMassGrams = childInfo.fullMass,
                variableMass = childInfo.variableMass,
                manufacturer = childInfo.manufacturer ?: openTagInfo.manufacturer,
                description = childInfo.description,
                metadata = childInfo.properties,
                createdAt = System.currentTimeMillis(),
                lastUpdated = LocalDateTime.now()
            )
            components.add(siblingComponent)
            siblingIds.add(siblingComponent.id)
        }
        
        // Update parent with all siblings
        val updatedParent = parentComponent.copy(childComponents = siblingIds)
        components[0] = updatedParent
        
        return@withContext components
    }
    
    /**
     * Create single independent component
     */
    private suspend fun createIndependentComponent(
        tagUid: String,
        openTagInfo: OpenTagInfo,
        metadata: Map<String, String>
    ): Component = withContext(Dispatchers.IO) {
        Component(
            id = generateComponentId(openTagInfo.componentType),
            identifiers = createIdentifiers(openTagInfo.uniqueIdentifier, tagUid),
            name = openTagInfo.name,
            category = openTagInfo.componentType,
            tags = openTagInfo.tags + listOf("opentag", "inventory-item"),
            parentComponentId = null,
            childComponents = emptyList(),
            massGrams = openTagInfo.totalMass,
            fullMassGrams = openTagInfo.fullMass,
            variableMass = openTagInfo.variableMass,
            manufacturer = openTagInfo.manufacturer,
            description = openTagInfo.description,
            metadata = buildComponentMetadata(openTagInfo, metadata),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Create user-defined component structure
     */
    private suspend fun createUserDefinedComponents(
        tagUid: String,
        openTagInfo: OpenTagInfo,
        metadata: Map<String, String>
    ): List<Component> = withContext(Dispatchers.IO) {
        // For now, fall back to hierarchical if user configuration is complex
        // In a real implementation, this would parse user-defined templates
        if (openTagInfo.childComponents.isNotEmpty()) {
            createHierarchicalComponents(tagUid, openTagInfo, metadata)
        } else {
            listOf(createIndependentComponent(tagUid, openTagInfo, metadata))
        }
    }
    
    /**
     * Find root component from component list
     */
    private fun findRootComponent(components: List<Component>): Component? {
        return components.firstOrNull { it.parentComponentId == null }
    }
    
    /**
     * Update existing component with new scan data
     */
    private suspend fun updateExistingComponent(
        existingComponent: Component,
        openTagInfo: OpenTagInfo
    ): Component = withContext(Dispatchers.IO) {
        try {
            val updatedMetadata = existingComponent.metadata + mapOf(
                "lastScanned" to LocalDateTime.now().toString(),
                "scanCount" to (existingComponent.metadata["scanCount"]?.toIntOrNull()?.plus(1) ?: 1).toString()
            ) + openTagInfo.properties
            
            val updatedComponent = existingComponent.copy(
                name = openTagInfo.name,
                description = openTagInfo.description,
                massGrams = openTagInfo.totalMass ?: existingComponent.massGrams,
                fullMassGrams = openTagInfo.fullMass ?: existingComponent.fullMassGrams,
                metadata = updatedMetadata,
                lastUpdated = LocalDateTime.now()
            )
            
            // Component updated in-memory only - no persistence needed
            Log.d(factoryType, "Updated existing OpenTag component: ${updatedComponent.name}")
            
            updatedComponent
        } catch (e: Exception) {
            Log.e(factoryType, "Error updating existing component", e)
            existingComponent
        }
    }
    
    /**
     * Build comprehensive metadata for OpenTag component
     */
    private fun buildComponentMetadata(
        openTagInfo: OpenTagInfo,
        scanMetadata: Map<String, String>
    ): Map<String, String> {
        val metadata = mutableMapOf<String, String>(
            "componentType" to openTagInfo.componentType,
            "source" to "opentag-scan",
            "factoryType" to factoryType,
            "creationStrategy" to openTagInfo.creationStrategy.name,
            "scanCount" to "1"
        )
        
        // Add OpenTag properties
        metadata.putAll(openTagInfo.properties)
        
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
            "dataSize" to encryptedScanData.encryptedData.size.toString(),
            "blockCount" to decryptedScanData.decryptedBlocks.size.toString()
        )
    }
    
    /**
     * Check if data has NDEF structure
     */
    private fun hasNdefStructure(data: ByteArray): Boolean {
        return try {
            // Look for NDEF header patterns
            if (data.size < 16) return false
            
            // Check for common NDEF headers
            val hex = data.take(16).joinToString("") { "%02X".format(it) }
            hex.startsWith("03") || // NDEF Message header
            hex.contains("D1") || // NDEF Record header
            hex.contains("54") // Text record type
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Interpret OpenTag data from decrypted scan data
     */
    private fun interpretOpenTagData(decryptedScanData: DecryptedScanData): OpenTagInfo? {
        return try {
            // Try using the standard interpreter first
            val filamentInfo = interpreter.interpret(decryptedScanData)
            if (filamentInfo != null) {
                // Convert FilamentInfo to OpenTagInfo for OpenTag format
                return OpenTagInfo(
                    name = "${filamentInfo.filamentType} ${filamentInfo.colorName}",
                    componentType = "filament",
                    uniqueIdentifier = filamentInfo.trayUid.takeIf { it.isNotEmpty() },
                    manufacturer = filamentInfo.manufacturerName,
                    description = "OpenTag filament component",
                    tags = listOf("opentag", "filament"),
                    creationStrategy = ComponentCreationStrategy.INDEPENDENT,
                    totalMass = filamentInfo.spoolWeight.toFloat(),
                    fullMass = filamentInfo.spoolWeight.toFloat(),
                    variableMass = true,
                    properties = mapOf(
                        "material" to filamentInfo.filamentType,
                        "colorName" to filamentInfo.colorName,
                        "colorHex" to filamentInfo.colorHex,
                        "diameter" to filamentInfo.filamentDiameter.toString(),
                        "printTemp" to filamentInfo.maxTemperature.toString(),
                        "bedTemp" to filamentInfo.bedTemperature.toString()
                    )
                )
            }
            
            // Fall back to parsing raw data as JSON or NDEF
            parseRawOpenTagData(decryptedScanData)
        } catch (e: Exception) {
            Log.e(factoryType, "Error interpreting OpenTag data", e)
            null
        }
    }
    
    /**
     * Parse raw OpenTag data from NDEF records or JSON
     */
    private fun parseRawOpenTagData(decryptedScanData: DecryptedScanData): OpenTagInfo? {
        try {
            // Convert all blocks to a single byte array
            val allData = decryptedScanData.decryptedBlocks.values
                .joinToString("") { it }
                .let { hex ->
                    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                }
            
            // Try to parse as UTF-8 text (JSON format)
            val text = String(allData, Charsets.UTF_8).trim()
            if (text.startsWith("{") && text.endsWith("}")) {
                return parseOpenTagJson(text)
            }
            
            // Create minimal OpenTag info from available data
            return OpenTagInfo(
                name = "OpenTag Component ${decryptedScanData.tagUid.take(8)}",
                componentType = "generic",
                uniqueIdentifier = decryptedScanData.tagUid,
                manufacturer = "Unknown",
                description = "Generic OpenTag component",
                creationStrategy = ComponentCreationStrategy.INDEPENDENT,
                properties = mapOf(
                    "tagUid" to decryptedScanData.tagUid,
                    "technology" to decryptedScanData.technology,
                    "blockCount" to decryptedScanData.decryptedBlocks.size.toString()
                )
            )
        } catch (e: Exception) {
            Log.w(factoryType, "Error parsing raw OpenTag data", e)
            return null
        }
    }
    
    /**
     * Parse OpenTag JSON format
     */
    private fun parseOpenTagJson(json: String): OpenTagInfo? {
        // This would typically use Gson or similar JSON parser
        // For now, return a basic OpenTag info
        return OpenTagInfo(
            name = "OpenTag JSON Component",
            componentType = "generic",
            description = "Parsed from OpenTag JSON format",
            creationStrategy = ComponentCreationStrategy.USER_DEFINED,
            properties = mapOf("format" to "json", "data" to json.take(100))
        )
    }
    
    /**
     * Check for OpenTag JSON signature
     */
    private fun hasOpenTagSignature(data: ByteArray): Boolean {
        return try {
            val text = String(data, Charsets.UTF_8)
            text.contains("\"opentag\"", ignoreCase = true) ||
            text.contains("\"componentType\"", ignoreCase = true) ||
            text.contains("\"creationStrategy\"", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    companion object {
        private const val TAG = "OpenTagComponentFactory"
    }
}

/**
 * Data class for OpenTag information
 */
data class OpenTagInfo(
    val name: String,
    val componentType: String,
    val uniqueIdentifier: String? = null,
    val manufacturer: String = "Unknown",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val creationStrategy: ComponentCreationStrategy = ComponentCreationStrategy.INDEPENDENT,
    val totalMass: Float? = null,
    val fullMass: Float? = null,
    val variableMass: Boolean = false,
    val properties: Map<String, String> = emptyMap(),
    val childComponents: List<OpenTagChildComponent> = emptyList()
)

/**
 * Data class for OpenTag child component information
 */
data class OpenTagChildComponent(
    val name: String,
    val componentType: String,
    val uniqueIdentifier: String? = null,
    val manufacturer: String? = null,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val mass: Float? = null,
    val fullMass: Float? = null,
    val variableMass: Boolean = false,
    val properties: Map<String, String> = emptyMap()
)