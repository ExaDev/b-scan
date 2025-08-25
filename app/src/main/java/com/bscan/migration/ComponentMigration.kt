package com.bscan.migration

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Migration service to convert old InventoryItem + PhysicalComponent data
 * to the new hierarchical Component system.
 */
class ComponentMigration(private val context: Context) {
    
    private val componentRepository = ComponentRepository(context)
    
    // Old data model classes for migration
    private data class OldInventoryItem(
        val trayUid: String,
        val components: List<String>,
        val totalMeasuredMass: Float?,
        val measurements: List<OldMassMeasurement>,
        val lastUpdated: LocalDateTime,
        val notes: String = ""
    )
    
    private data class OldPhysicalComponent(
        val id: String,
        val name: String,
        val type: String, // Was PhysicalComponentType enum
        val massGrams: Float,
        val fullMassGrams: Float? = null,
        val variableMass: Boolean,
        val manufacturer: String = "Unknown",
        val description: String = "",
        val isUserDefined: Boolean = false,
        val createdAt: Long = System.currentTimeMillis()
    )
    
    private data class OldMassMeasurement(
        val id: String,
        val trayUid: String,
        val measuredMassGrams: Float,
        val componentIds: List<String>,
        val measurementType: String, // Was MeasurementType enum
        val measuredAt: LocalDateTime,
        val notes: String = "",
        val isVerified: Boolean = false
    )
    
    private val localDateTimeAdapter = object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
            } catch (e: Exception) {
                LocalDateTime.now()
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .create()
    
    /**
     * Perform the migration from old data model to new hierarchical system
     */
    fun migrate(): MigrationResult {
        try {
            Log.i(TAG, "Starting component migration...")
            
            val result = MigrationResult()
            
            // 1. Migrate old PhysicalComponents to new Components
            val oldComponents = loadOldPhysicalComponents()
            Log.d(TAG, "Found ${oldComponents.size} old physical components to migrate")
            
            val componentMapping = mutableMapOf<String, String>() // oldId -> newId
            
            oldComponents.forEach { oldComponent ->
                val newComponent = migratePhysicalComponent(oldComponent)
                componentRepository.saveComponent(newComponent)
                componentMapping[oldComponent.id] = newComponent.id
                result.migratedComponents++
                Log.d(TAG, "Migrated component: ${oldComponent.name} -> ${newComponent.id}")
            }
            
            // 2. Migrate old InventoryItems to hierarchical inventory components
            val oldInventoryItems = loadOldInventoryItems()
            Log.d(TAG, "Found ${oldInventoryItems.size} old inventory items to migrate")
            
            oldInventoryItems.forEach { oldInventory ->
                val newInventoryComponent = migrateInventoryItem(oldInventory, componentMapping)
                if (newInventoryComponent != null) {
                    componentRepository.saveComponent(newInventoryComponent)
                    
                    // Set up parent-child relationships
                    oldInventory.components.forEach { oldComponentId ->
                        val newComponentId = componentMapping[oldComponentId]
                        if (newComponentId != null) {
                            // Update child component to reference new parent
                            val childComponent = componentRepository.getComponent(newComponentId)
                            if (childComponent != null) {
                                val updatedChild = childComponent.copy(parentComponentId = newInventoryComponent.id)
                                componentRepository.saveComponent(updatedChild)
                            }
                        }
                    }
                    
                    result.migratedInventoryItems++
                    Log.d(TAG, "Migrated inventory item: ${oldInventory.trayUid} -> ${newInventoryComponent.id}")
                }
            }
            
            // 3. Migrate measurements
            val oldMeasurements = loadOldMeasurements()
            Log.d(TAG, "Found ${oldMeasurements.size} old measurements to migrate")
            
            oldMeasurements.forEach { oldMeasurement ->
                val newMeasurement = migrateMeasurement(oldMeasurement, componentMapping)
                componentRepository.saveMeasurement(newMeasurement)
                result.migratedMeasurements++
            }
            
            result.success = true
            Log.i(TAG, "Migration completed successfully: $result")
            return result
            
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            return MigrationResult(success = false, errorMessage = e.message)
        }
    }
    
    private fun migratePhysicalComponent(old: OldPhysicalComponent): Component {
        val category = when (old.type) {
            "FILAMENT" -> "filament"
            "BASE_SPOOL" -> "spool"
            "CORE_RING" -> "core"
            "ADAPTER" -> "adapter"
            "PACKAGING" -> "packaging"
            else -> "general"
        }
        
        val tags = mutableListOf<String>()
        if (old.variableMass) tags.add("variable-mass") else tags.add("fixed-mass")
        if (old.manufacturer == "Bambu Lab") tags.add("bambu")
        if (category == "filament") tags.add("consumable") else tags.add("reusable")
        if (old.isUserDefined) tags.add("user-defined")
        
        return Component(
            id = old.id, // Keep same ID for mapping
            name = old.name,
            category = category,
            tags = tags,
            massGrams = old.massGrams,
            fullMassGrams = old.fullMassGrams,
            variableMass = old.variableMass,
            manufacturer = old.manufacturer,
            description = old.description,
            createdAt = old.createdAt,
            lastUpdated = LocalDateTime.now()
        )
    }
    
    private fun migrateInventoryItem(
        old: OldInventoryItem,
        componentMapping: Map<String, String>
    ): Component? {
        // Map old component IDs to new ones
        val newChildComponents = old.components.mapNotNull { oldId ->
            componentMapping[oldId]
        }
        
        if (newChildComponents.isEmpty()) {
            Log.w(TAG, "Skipping inventory item ${old.trayUid} - no valid child components")
            return null
        }
        
        // Try to find filament component for naming
        val filamentComponent = newChildComponents
            .mapNotNull { componentRepository.getComponent(it) }
            .find { it.category == "filament" }
        
        val inventoryName = filamentComponent?.let { 
            "Inventory: ${it.name}"
        } ?: "Inventory Item ${old.trayUid}"
        
        return Component(
            id = "inventory_${System.currentTimeMillis()}",
            uniqueIdentifier = old.trayUid,
            name = inventoryName,
            category = "inventory-item",
            tags = listOf("migrated", "composite"),
            childComponents = newChildComponents,
            massGrams = old.totalMeasuredMass,
            description = "Migrated from legacy inventory system. ${old.notes}".trim(),
            metadata = mapOf(
                "migrated" to "true",
                "originalTrayUid" to old.trayUid,
                "originalNotes" to old.notes
            ),
            lastUpdated = old.lastUpdated
        )
    }
    
    private fun migrateMeasurement(
        old: OldMassMeasurement,
        componentMapping: Map<String, String>
    ): ComponentMeasurement {
        // For inventory-level measurements, find the inventory component by tray UID
        val inventoryComponent = componentRepository.findInventoryByUniqueId(old.trayUid)
        val componentId = inventoryComponent?.id ?: old.componentIds.firstOrNull() ?: "unknown"
        
        val measurementType = when (old.measurementType) {
            "FULL_WEIGHT" -> MeasurementType.TOTAL_MASS
            "EMPTY_WEIGHT" -> MeasurementType.COMPONENT_ONLY
            else -> MeasurementType.TOTAL_MASS
        }
        
        return ComponentMeasurement(
            id = old.id,
            componentId = componentId,
            measuredMassGrams = old.measuredMassGrams,
            measurementType = measurementType,
            measuredAt = old.measuredAt,
            notes = "Migrated: ${old.notes}".trim(),
            isVerified = old.isVerified
        )
    }
    
    private fun loadOldPhysicalComponents(): List<OldPhysicalComponent> {
        val prefs = context.getSharedPreferences("physical_component_data", Context.MODE_PRIVATE)
        val json = prefs.getString("physical_components", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<OldPhysicalComponent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load old physical components", e)
            emptyList()
        }
    }
    
    private fun loadOldInventoryItems(): List<OldInventoryItem> {
        val prefs = context.getSharedPreferences("inventory_data", Context.MODE_PRIVATE)
        val json = prefs.getString("inventory_items", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<OldInventoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load old inventory items", e)
            emptyList()
        }
    }
    
    private fun loadOldMeasurements(): List<OldMassMeasurement> {
        val prefs = context.getSharedPreferences("inventory_data", Context.MODE_PRIVATE)
        val json = prefs.getString("mass_measurements", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<OldMassMeasurement>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Could not load old measurements", e)
            emptyList()
        }
    }
    
    /**
     * Check if migration is needed
     */
    fun isMigrationNeeded(): Boolean {
        // Check if we have old data but no new components
        val hasOldPhysicalComponents = loadOldPhysicalComponents().isNotEmpty()
        val hasOldInventoryItems = loadOldInventoryItems().isNotEmpty()
        val hasNewComponents = componentRepository.getComponents().isNotEmpty()
        
        return (hasOldPhysicalComponents || hasOldInventoryItems) && !hasNewComponents
    }
    
    companion object {
        private const val TAG = "ComponentMigration"
    }
}

/**
 * Result of migration operation
 */
data class MigrationResult(
    var success: Boolean = false,
    var migratedComponents: Int = 0,
    var migratedInventoryItems: Int = 0,
    var migratedMeasurements: Int = 0,
    var errorMessage: String? = null
) {
    override fun toString(): String {
        return if (success) {
            "Migration successful - Components: $migratedComponents, Inventory: $migratedInventoryItems, Measurements: $migratedMeasurements"
        } else {
            "Migration failed: $errorMessage"
        }
    }
}