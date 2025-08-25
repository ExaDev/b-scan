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
 * Repository for managing physical components that make up inventory items.
 * Handles both filament components (variable mass) and hardware components (fixed mass).
 */
class PhysicalComponentRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("physical_component_data", Context.MODE_PRIVATE)
    
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
                LocalDateTime.now() // Fallback to current time
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .create()
    
    init {
        // Initialize with factory defaults if first run
        if (getComponents().isEmpty()) {
            initializeFactoryDefaults()
        }
    }
    
    // === Component Management ===
    
    /**
     * Get all physical components
     */
    fun getComponents(): List<PhysicalComponent> {
        val json = sharedPreferences.getString(COMPONENTS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<PhysicalComponent>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get component by ID
     */
    fun getComponent(id: String): PhysicalComponent? {
        return getComponents().find { it.id == id }
    }
    
    /**
     * Get components by type
     */
    fun getComponentsByType(type: PhysicalComponentType): List<PhysicalComponent> {
        return getComponents().filter { it.type == type }
    }
    
    /**
     * Get all filament components (variable mass)
     */
    fun getFilamentComponents(): List<PhysicalComponent> {
        return getComponents().filter { it.variableMass }
    }
    
    /**
     * Get all fixed components (non-variable mass)
     */
    fun getFixedComponents(): List<PhysicalComponent> {
        return getComponents().filter { !it.variableMass }
    }
    
    /**
     * Save or update a component
     */
    fun saveComponent(component: PhysicalComponent) {
        val components = getComponents().toMutableList()
        val existingIndex = components.indexOfFirst { it.id == component.id }
        
        if (existingIndex >= 0) {
            components[existingIndex] = component
        } else {
            components.add(component)
        }
        
        saveComponents(components)
    }
    
    /**
     * Delete a component
     */
    fun deleteComponent(componentId: String) {
        val components = getComponents().filterNot { it.id == componentId }
        saveComponents(components)
    }
    
    /**
     * Create a new filament component from SKU data
     */
    fun createFilamentComponent(
        filamentType: String,
        colorName: String,
        colorHex: String,
        massGrams: Float,
        manufacturer: String = "Bambu Lab"
    ): PhysicalComponent {
        val componentId = "filament_${System.currentTimeMillis()}"
        return PhysicalComponent(
            id = componentId,
            name = "$filamentType - $colorName",
            type = PhysicalComponentType.FILAMENT,
            massGrams = massGrams,
            variableMass = true,
            manufacturer = manufacturer,
            description = "Filament component created from SKU data"
        )
    }
    
    /**
     * Create a standard Bambu cardboard core component
     */
    fun createBambuCoreComponent(): PhysicalComponent {
        return PhysicalComponent(
            id = "bambu_cardboard_core",
            name = "Bambu Cardboard Core",
            type = PhysicalComponentType.CORE_RING,
            massGrams = 33f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab cardboard core (33g)"
        )
    }
    
    /**
     * Create a standard Bambu refillable spool component
     */
    fun createBambuSpoolComponent(): PhysicalComponent {
        return PhysicalComponent(
            id = "bambu_refillable_spool",
            name = "Bambu Refillable Spool",
            type = PhysicalComponentType.BASE_SPOOL,
            massGrams = 212f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab refillable spool (212g)"
        )
    }
    
    /**
     * Calculate total mass for a list of component IDs
     */
    fun calculateTotalMass(componentIds: List<String>): Float {
        val components = getComponents().filter { it.id in componentIds }
        return components.sumOf { it.massGrams.toDouble() }.toFloat()
    }
    
    /**
     * Calculate fixed component mass (non-variable components only)
     */
    fun calculateFixedComponentMass(componentIds: List<String>): Float {
        val components = getComponents().filter { it.id in componentIds && !it.variableMass }
        return components.sumOf { it.massGrams.toDouble() }.toFloat()
    }
    
    /**
     * Get variable mass components from a list of component IDs
     */
    fun getVariableComponents(componentIds: List<String>): List<PhysicalComponent> {
        return getComponents().filter { it.id in componentIds && it.variableMass }
    }
    
    /**
     * Get fixed mass components from a list of component IDs
     */
    fun getFixedComponentsFromIds(componentIds: List<String>): List<PhysicalComponent> {
        return getComponents().filter { it.id in componentIds && !it.variableMass }
    }
    
    /**
     * Get user-defined components only
     */
    fun getUserDefinedComponents(): List<PhysicalComponent> {
        return getComponents().filter { it.isUserDefined }
    }
    
    /**
     * Get built-in components only
     */
    fun getBuiltInComponents(): List<PhysicalComponent> {
        return getComponents().filter { !it.isUserDefined }
    }
    
    /**
     * Check if a component can be deleted (user-defined and not in use)
     */
    fun canDeleteComponent(componentId: String): Boolean {
        val component = getComponent(componentId) ?: return false
        return component.isUserDefined && !isComponentInUse(componentId)
    }
    
    /**
     * Check if a component is currently in use by any inventory items
     */
    fun isComponentInUse(componentId: String): Boolean {
        // This would require access to InventoryRepository, but we can't inject it here
        // to avoid circular dependency. The check will be done in the UI layer.
        return false
    }
    
    /**
     * Create a copy of an existing component with a new name and ID
     */
    fun createCopyOfComponent(sourceId: String, newName: String): PhysicalComponent? {
        val sourceComponent = getComponent(sourceId) ?: return null
        
        return sourceComponent.copy(
            id = "user_${System.currentTimeMillis()}",
            name = newName,
            isUserDefined = true,
            createdAt = System.currentTimeMillis()
        )
    }
    
    /**
     * Get components by manufacturer
     */
    fun getComponentsByManufacturer(manufacturer: String): List<PhysicalComponent> {
        return getComponents().filter { it.manufacturer.equals(manufacturer, ignoreCase = true) }
    }
    
    /**
     * Get unique manufacturers from all components
     */
    fun getUniqueManufacturers(): List<String> {
        return getComponents()
            .map { it.manufacturer }
            .filter { it.isNotBlank() && it != "Unknown" }
            .distinct()
            .sorted()
    }
    
    /**
     * Search components by name, manufacturer, or description
     */
    fun searchComponents(query: String): List<PhysicalComponent> {
        if (query.isBlank()) return getComponents()
        
        val lowercaseQuery = query.lowercase()
        return getComponents().filter { component ->
            component.name.lowercase().contains(lowercaseQuery) ||
            component.manufacturer.lowercase().contains(lowercaseQuery) ||
            component.description.lowercase().contains(lowercaseQuery)
        }
    }
    
    private fun saveComponents(components: List<PhysicalComponent>) {
        val json = gson.toJson(components)
        sharedPreferences.edit()
            .putString(COMPONENTS_KEY, json)
            .apply()
    }
    
    /**
     * Initialize factory default components
     */
    private fun initializeFactoryDefaults() {
        val defaults = listOf(
            createBambuCoreComponent(),
            createBambuSpoolComponent()
        )
        
        saveComponents(defaults)
    }
    
    /**
     * Clear all components (for testing/reset)
     */
    fun clearComponents() {
        sharedPreferences.edit()
            .remove(COMPONENTS_KEY)
            .apply()
    }
    
    companion object {
        private const val COMPONENTS_KEY = "physical_components"
    }
}