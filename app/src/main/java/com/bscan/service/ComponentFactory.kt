package com.bscan.service

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import com.bscan.repository.CatalogRepository
import com.bscan.repository.UserDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.util.UUID

/**
 * Abstract factory pattern for creating hierarchical component structures from RFID scans.
 * 
 * Different tag formats use different component creation strategies:
 * - BambuComponentFactory: Creates tray component + sibling components (RFID tags, filament, core)
 * - CrealityComponentFactory: Creates single independent components
 * - OpenTagComponentFactory: User-configurable component creation
 * - GenericComponentFactory: Fallback for unknown tag systems
 * 
 * Key features:
 * - Support for both sibling grouping AND hierarchical nesting
 * - Catalog-driven instantiation (tag data → SKU lookup → component creation)
 * - Mass calculation and inference for unknown components
 * - Dual identifier system (hardware UID vs application data)
 * - Proper threading with Dispatchers.IO for repository operations
 */
abstract class ComponentFactory(
    protected val context: Context
) {
    
    protected val componentRepository = ComponentRepository(context)
    protected val catalogRepository = CatalogRepository(context)
    protected val userDataRepository = UserDataRepository(context)
    
    /**
     * Process an RFID scan and create/update the component hierarchy.
     * 
     * @param encryptedScanData Raw scan data from NFC tag
     * @param decryptedScanData Decrypted and authenticated block data
     * @return Root component created/updated, or null if processing failed
     */
    abstract suspend fun processScan(
        encryptedScanData: EncryptedScanData,
        decryptedScanData: DecryptedScanData
    ): Component?
    
    /**
     * Create components from interpreted tag data.
     * Each factory implements its own component creation strategy.
     * 
     * @param tagUid Hardware identifier from RFID chip
     * @param interpretedData Interpreted data from tag (e.g., FilamentInfo for Bambu)
     * @param metadata Additional context from scan
     * @return List of components created (may be hierarchical or flat)
     */
    abstract suspend fun createComponents(
        tagUid: String,
        interpretedData: Any,
        metadata: Map<String, String> = emptyMap()
    ): List<Component>
    
    /**
     * Get factory type identifier for logging and debugging
     */
    abstract val factoryType: String
    
    /**
     * Check if this factory supports the given tag format
     */
    abstract fun supportsTagFormat(encryptedScanData: EncryptedScanData): Boolean
    
    /**
     * Extract unique identifier from tag data (format-specific)
     * For Bambu: tray UID from application data
     * For others: may use hardware UID directly
     */
    abstract fun extractUniqueIdentifier(decryptedScanData: DecryptedScanData): String?
    
    /**
     * Shared utility: Generate unique component ID
     */
    protected fun generateComponentId(prefix: String = "component"): String {
        return "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    /**
     * Shared utility: Create an RFID tag component (hardware representation)
     */
    protected suspend fun createRfidTagComponent(
        tagUid: String,
        manufacturer: String = "Unknown",
        parentComponentId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Component = withContext(Dispatchers.IO) {
        Component(
            id = generateComponentId("rfid_tag"),
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = tagUid,
                    purpose = IdentifierPurpose.AUTHENTICATION,
                    metadata = mapOf(
                        "manufacturer" to manufacturer,
                        "chipType" to "mifare-classic-1k"
                    )
                )
            ),
            name = "RFID Tag $tagUid",
            category = "rfid-tag",
            tags = listOf("identifier", "fixed-mass", manufacturer.lowercase()),
            parentComponentId = parentComponentId,
            massGrams = 0.5f, // Negligible mass for RFID tags
            variableMass = false,
            manufacturer = manufacturer,
            description = "$manufacturer RFID tag for identification",
            metadata = metadata + mapOf(
                "tagUid" to tagUid,
                "scanTimestamp" to LocalDateTime.now().toString()
            ),
            createdAt = System.currentTimeMillis(),
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Shared utility: Look up SKU data for catalog-driven instantiation
     */
    protected suspend fun lookupSkuData(
        material: String?,
        color: String?,
        manufacturer: String?
    ): SkuData? = withContext(Dispatchers.IO) {
        try {
            if (material != null && color != null) {
                // Try to find exact match in catalog
                val manufacturerId = manufacturer?.lowercase() ?: "bambu"
                val products = catalogRepository.findProducts(manufacturerId, null, material)
                val bestMatch = products.firstOrNull { it.colorName.equals(color, ignoreCase = true) }
                
                if (bestMatch != null) {
                    Log.d(factoryType, "Found catalog match for $material/$color: ${bestMatch.variantId}")
                    return@withContext SkuData(
                        sku = bestMatch.variantId,
                        productName = bestMatch.productName,
                        filamentWeightGrams = bestMatch.filamentWeightGrams,
                        manufacturer = bestMatch.manufacturer,
                        material = bestMatch.materialType,
                        colorName = bestMatch.colorName,
                        colorHex = bestMatch.colorHex
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.e(factoryType, "Error looking up SKU data", e)
            null
        }
    }
    
    /**
     * Shared utility: Calculate total mass of component hierarchy
     */
    protected suspend fun calculateHierarchyMass(componentId: String): Float = withContext(Dispatchers.IO) {
        val component = componentRepository.getComponent(componentId) ?: return@withContext 0f
        
        val ownMass = component.massGrams ?: 0f
        val childMass = component.childComponents.sumOf { childId ->
            calculateHierarchyMass(childId).toDouble()
        }.toFloat()
        
        ownMass + childMass
    }
    
    /**
     * Shared utility: Infer unknown component mass from total measurement
     */
    protected suspend fun inferComponentMass(
        parentComponentId: String,
        totalMeasuredMass: Float,
        unknownComponentId: String
    ): Float? = withContext(Dispatchers.IO) {
        val parent = componentRepository.getComponent(parentComponentId) ?: return@withContext null
        
        // Calculate known mass from other children
        val knownMass = parent.childComponents
            .filter { it != unknownComponentId }
            .sumOf { childId ->
                componentRepository.getComponent(childId)?.massGrams?.toDouble() ?: 0.0
            }.toFloat()
        
        val inferredMass = totalMeasuredMass - knownMass
        if (inferredMass >= 0) {
            Log.d(factoryType, "Inferred mass for component $unknownComponentId: ${inferredMass}g")
            inferredMass
        } else {
            Log.w(factoryType, "Cannot infer negative mass: total=$totalMeasuredMass, known=$knownMass")
            null
        }
    }
    
    /**
     * Shared utility: Add component to existing parent hierarchy
     */
    protected suspend fun addComponentToParent(
        parentId: String,
        childComponent: Component
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val parent = componentRepository.getComponent(parentId) ?: return@withContext false
            
            // Save child with parent reference
            val updatedChild = childComponent.copy(parentComponentId = parentId)
            componentRepository.saveComponent(updatedChild)
            
            // Update parent to include child
            val updatedParent = parent.withChildComponent(updatedChild.id)
            componentRepository.saveComponent(updatedParent)
            
            Log.d(factoryType, "Added component ${childComponent.name} to parent $parentId")
            true
        } catch (e: Exception) {
            Log.e(factoryType, "Error adding component to parent", e)
            false
        }
    }
    
    /**
     * Shared utility: Create inventory item for root component
     */
    protected suspend fun createInventoryItem(
        rootComponent: Component,
        notes: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!rootComponent.isInventoryItem) {
                Log.w(factoryType, "Component ${rootComponent.id} is not an inventory item")
                return@withContext false
            }
            
            val uniqueId = rootComponent.identifiers.firstOrNull { it.isUnique }?.value
                ?: throw IllegalStateException("Root component missing unique identifier")
            
            val inventoryItem = InventoryItem(
                trayUid = uniqueId,
                components = listOf(rootComponent.id) + rootComponent.childComponents,
                totalMeasuredMass = null,
                measurements = emptyList(),
                lastUpdated = LocalDateTime.now(),
                notes = notes.ifEmpty { "Auto-created from ${factoryType} scan" }
            )
            
            userDataRepository.saveInventoryItem(inventoryItem)
            Log.d(factoryType, "Created inventory item for: $uniqueId")
            true
        } catch (e: Exception) {
            Log.e(factoryType, "Error creating inventory item", e)
            false
        }
    }
    
    companion object {
        /**
         * Factory method to create appropriate ComponentFactory based on tag data
         */
        fun createFactory(
            context: Context,
            encryptedScanData: EncryptedScanData
        ): ComponentFactory {
            return when {
                BambuComponentFactory(context).supportsTagFormat(encryptedScanData) -> 
                    BambuComponentFactory(context)
                CrealityComponentFactory(context).supportsTagFormat(encryptedScanData) -> 
                    CrealityComponentFactory(context)
                OpenTagComponentFactory(context).supportsTagFormat(encryptedScanData) -> 
                    OpenTagComponentFactory(context)
                else -> GenericComponentFactory(context)
            }
        }
    }
}

/**
 * Data class for SKU lookup results
 */
data class SkuData(
    val sku: String,
    val productName: String,
    val filamentWeightGrams: Float?,
    val manufacturer: String,
    val material: String?,
    val colorName: String?,
    val colorHex: String?
)

/**
 * Component creation strategy interface
 */
enum class ComponentCreationStrategy {
    HIERARCHICAL,    // Parent contains children (Bambu tray model)
    SIBLING_GROUP,   // Components are siblings under shared parent
    INDEPENDENT,     // Single standalone component (Creality model)
    USER_DEFINED     // User configurable (OpenTag model)
}

/**
 * Factory configuration for component creation behaviour
 */
data class FactoryConfig(
    val strategy: ComponentCreationStrategy,
    val enableMassInference: Boolean = true,
    val enableSkuLookup: Boolean = true,
    val createInventoryItems: Boolean = true,
    val defaultMasses: Map<String, Float> = emptyMap()
)