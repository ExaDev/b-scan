package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing hierarchical components and their relationships.
 * Handles both simple components and composite components containing others.
 */
class ComponentRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("component_data", Context.MODE_PRIVATE)
    
    private val measurementPreferences: SharedPreferences =
        context.getSharedPreferences("component_measurement_data", Context.MODE_PRIVATE)
    
    // Custom LocalDateTime adapter for Gson
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
    
    // === Component Management ===
    
    /**
     * Get all components
     */
    fun getComponents(): List<Component> {
        val json = sharedPreferences.getString(COMPONENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Component>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading components", e)
            emptyList()
        }
    }
    
    /**
     * Get component by ID
     */
    fun getComponent(id: String): Component? {
        return getComponents().find { it.id == id }
    }
    
    /**
     * Save or update a component
     */
    fun saveComponent(component: Component) {
        val components = getComponents().toMutableList()
        val existingIndex = components.indexOfFirst { it.id == component.id }
        
        if (existingIndex >= 0) {
            components[existingIndex] = component
        } else {
            components.add(component)
        }
        
        saveComponents(components)
        android.util.Log.d(TAG, "Saved component: ${component.name} (${component.id})")
    }
    
    /**
     * Delete a component and update any parent references
     */
    fun deleteComponent(componentId: String) {
        val components = getComponents().toMutableList()
        val componentToDelete = components.find { it.id == componentId }
        
        if (componentToDelete != null) {
            // Remove from parent's child list if has parent
            componentToDelete.parentComponentId?.let { parentId ->
                val parent = components.find { it.id == parentId }
                parent?.let { parentComponent ->
                    val updatedParent = parentComponent.withoutChildComponent(componentId)
                    val parentIndex = components.indexOfFirst { it.id == parentId }
                    if (parentIndex >= 0) {
                        components[parentIndex] = updatedParent
                    }
                }
            }
            
            // Remove the component itself
            components.removeAll { it.id == componentId }
            
            // Remove any orphaned children (or update their parent reference)
            components.removeAll { it.parentComponentId == componentId }
            
            saveComponents(components)
            android.util.Log.d(TAG, "Deleted component: ${componentToDelete.name}")
        }
    }
    
    // === Inventory Item Queries (Components with unique identifiers) ===
    
    /**
     * Get all inventory items (root components with unique identifiers)
     */
    fun getInventoryItems(): List<Component> {
        return getComponents().filter { it.isInventoryItem }
    }
    
    /**
     * Find inventory item by unique identifier
     */
    fun findInventoryByUniqueId(uniqueId: String): Component? {
        return getComponents().find { 
            it.getPrimaryTrackingIdentifier()?.value == uniqueId && it.isRootComponent 
        }
    }
    
    /**
     * Find component anywhere in hierarchy by unique identifier
     */
    fun findComponentByUniqueId(uniqueId: String): Component? {
        return getComponents().find { 
            it.getPrimaryTrackingIdentifier()?.value == uniqueId 
        }
    }
    
    // === Hierarchical Queries ===
    
    /**
     * Get all child components of a component
     */
    fun getChildComponents(componentId: String): List<Component> {
        val component = getComponent(componentId) ?: return emptyList()
        return getComponents().filter { it.id in component.childComponents }
    }
    
    /**
     * Get all descendant components (children, grandchildren, etc.)
     */
    fun getDescendantComponents(componentId: String): List<Component> {
        val descendants = mutableListOf<Component>()
        val children = getChildComponents(componentId)
        
        for (child in children) {
            descendants.add(child)
            descendants.addAll(getDescendantComponents(child.id))
        }
        
        return descendants
    }
    
    /**
     * Get the root component for any component
     */
    fun getRootComponent(componentId: String): Component? {
        val component = getComponent(componentId) ?: return null
        
        return if (component.isRootComponent) {
            component
        } else {
            component.parentComponentId?.let { getRootComponent(it) }
        }
    }
    
    /**
     * Calculate total mass of a component including all children
     */
    fun getTotalMass(componentId: String): Float {
        val component = getComponent(componentId) ?: return 0f
        
        val ownMass = component.massGrams ?: 0f
        val childrenMass = component.childComponents.sumOf { childId ->
            getTotalMass(childId).toDouble()
        }.toFloat()
        
        return ownMass + childrenMass
    }
    
    /**
     * Get components that have unknown masses (null)
     */
    fun getComponentsWithUnknownMass(parentComponentId: String): List<Component> {
        return getChildComponents(parentComponentId).filter { it.massGrams == null }
    }
    
    // === Category and Tag Queries ===
    
    /**
     * Get components by category
     */
    fun getComponentsByCategory(category: String): List<Component> {
        return getComponents().filter { it.category == category }
    }
    
    /**
     * Get components by tag
     */
    fun getComponentsByTag(tag: String): List<Component> {
        return getComponents().filter { tag in it.tags }
    }
    
    /**
     * Get components by multiple tags (AND logic)
     */
    fun getComponentsByTags(tags: List<String>): List<Component> {
        return getComponents().filter { component ->
            tags.all { tag -> tag in component.tags }
        }
    }
    
    /**
     * Get variable mass components
     */
    fun getVariableMassComponents(): List<Component> {
        return getComponents().filter { it.variableMass }
    }
    
    /**
     * Get fixed mass components
     */
    fun getFixedMassComponents(): List<Component> {
        return getComponents().filter { !it.variableMass }
    }
    
    // === Mass Measurement Management ===
    
    /**
     * Get all measurements
     */
    fun getMeasurements(): List<ComponentMeasurement> {
        val json = measurementPreferences.getString(MEASUREMENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<ComponentMeasurement>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading measurements", e)
            emptyList()
        }
    }
    
    /**
     * Save a measurement
     */
    fun saveMeasurement(measurement: ComponentMeasurement) {
        val measurements = getMeasurements().toMutableList()
        val existingIndex = measurements.indexOfFirst { it.id == measurement.id }
        
        if (existingIndex >= 0) {
            measurements[existingIndex] = measurement
        } else {
            measurements.add(measurement)
        }
        
        saveMeasurements(measurements)
    }
    
    /**
     * Get measurements for a component
     */
    fun getMeasurementsForComponent(componentId: String): List<ComponentMeasurement> {
        return getMeasurements().filter { it.componentId == componentId }
    }
    
    // === Mass Inference ===
    
    /**
     * Infer the mass of an unknown component based on total measurement
     */
    fun inferComponentMass(request: MassInferenceRequest): Float? {
        val parent = getComponent(request.parentComponentId) ?: return null
        
        // Calculate known mass from specified components
        val knownMass = request.knownComponentIds.sumOf { componentId ->
            getComponent(componentId)?.massGrams?.toDouble() ?: 0.0
        }.toFloat()
        
        // Inferred mass = total - known
        val inferredMass = request.totalMeasuredMass - knownMass
        
        return if (inferredMass >= 0) inferredMass else null
    }
    
    /**
     * Auto-infer and update component mass
     */
    fun autoInferAndUpdateMass(
        parentComponentId: String,
        totalMeasuredMass: Float,
        unknownComponentId: String
    ): Boolean {
        val parent = getComponent(parentComponentId) ?: return false
        val unknownComponent = getComponent(unknownComponentId) ?: return false
        
        // Get all other child components with known masses
        val knownComponents = getChildComponents(parentComponentId)
            .filter { it.id != unknownComponentId && it.massGrams != null }
        
        val knownMass = knownComponents.sumOf { it.massGrams!!.toDouble() }.toFloat()
        val inferredMass = totalMeasuredMass - knownMass
        
        if (inferredMass >= 0) {
            val updatedComponent = unknownComponent.copy(
                massGrams = inferredMass,
                inferredMass = true,
                lastUpdated = LocalDateTime.now()
            )
            saveComponent(updatedComponent)
            
            android.util.Log.d(TAG, "Inferred mass for ${unknownComponent.name}: ${inferredMass}g")
            return true
        }
        
        return false
    }
    
    // === Component Hierarchy Management ===
    
    /**
     * Add a child component to a parent
     */
    fun addChildComponent(parentId: String, childId: String) {
        val parent = getComponent(parentId) ?: return
        val child = getComponent(childId) ?: return
        
        // Update parent to include child
        val updatedParent = parent.withChildComponent(childId)
        saveComponent(updatedParent)
        
        // Update child to reference parent
        val updatedChild = child.copy(parentComponentId = parentId)
        saveComponent(updatedChild)
        
        android.util.Log.d(TAG, "Added ${child.name} as child of ${parent.name}")
    }
    
    /**
     * Remove a child component from parent
     */
    fun removeChildComponent(parentId: String, childId: String) {
        val parent = getComponent(parentId) ?: return
        val child = getComponent(childId) ?: return
        
        // Update parent to remove child
        val updatedParent = parent.withoutChildComponent(childId)
        saveComponent(updatedParent)
        
        // Update child to remove parent reference
        val updatedChild = child.copy(parentComponentId = null)
        saveComponent(updatedChild)
        
        android.util.Log.d(TAG, "Removed ${child.name} from ${parent.name}")
    }
    
    // === Private Helper Methods ===
    
    private fun saveComponents(components: List<Component>) {
        val json = gson.toJson(components)
        sharedPreferences.edit()
            .putString(COMPONENTS_KEY, json)
            .apply()
    }
    
    private fun saveMeasurements(measurements: List<ComponentMeasurement>) {
        val json = gson.toJson(measurements)
        measurementPreferences.edit()
            .putString(MEASUREMENTS_KEY, json)
            .apply()
    }
    
    /**
     * Regenerate inventory items with corrected structure (includes root component IDs)
     * This fixes legacy inventory items that only contained child components
     */
    fun regenerateInventoryItems() {
        val inventoryRepository = InventoryRepository(context)
        val existingInventoryItems = inventoryRepository.getInventoryItems()
        
        Log.d(TAG, "Regenerating ${existingInventoryItems.size} inventory items with corrected structure")
        
        existingInventoryItems.forEach { inventoryItem ->
            // Get all components for this inventory item
            val allComponents = inventoryItem.components.mapNotNull { componentId ->
                getComponent(componentId)
            }
            
            // Find the root component (should exist now due to BambuComponentFactory fix)
            val rootComponent = allComponents.find { it.isRootComponent }
            
            if (rootComponent != null) {
                // Update inventory item to include root component ID first
                val updatedInventoryItem = inventoryItem.copy(
                    components = listOf(rootComponent.id) + rootComponent.childComponents
                )
                
                // Save the corrected inventory item
                inventoryRepository.addOrUpdateInventoryItem(updatedInventoryItem)
                
                Log.d(TAG, "Updated inventory item ${inventoryItem.trayUid} to include root component ${rootComponent.id}")
            } else {
                Log.w(TAG, "No root component found for inventory item ${inventoryItem.trayUid}, skipping regeneration")
            }
        }
        
        Log.d(TAG, "Completed inventory item regeneration")
    }
    
    /**
     * Clear all components (for testing/reset)
     */
    fun clearComponents() {
        sharedPreferences.edit()
            .remove(COMPONENTS_KEY)
            .apply()
    }
    
    /**
     * Clear all measurements (for testing/reset)
     */
    fun clearMeasurements() {
        measurementPreferences.edit()
            .remove(MEASUREMENTS_KEY)
            .apply()
    }
    
    companion object {
        private const val COMPONENTS_KEY = "components"
        private const val MEASUREMENTS_KEY = "component_measurements"
        private const val TAG = "ComponentRepository"
    }
}