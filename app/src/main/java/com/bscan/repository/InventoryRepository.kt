package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.logic.MassCalculationService
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing inventory items and their weight tracking data.
 * 
 * This repository handles the relationship between FilamentReel items and their
 * physical weight measurements, enabling calculation of remaining filament.
 */
class InventoryRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("inventory_data", Context.MODE_PRIVATE)
    
    private val physicalComponentRepository by lazy { PhysicalComponentRepository(context) }
    private val calculationService = MassCalculationService()
    private val mappingsRepository by lazy { MappingsRepository(context) }
    
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
    
    // === Inventory Item Management ===
    
    /**
     * Get all inventory items
     */
    fun getInventoryItems(): List<InventoryItem> {
        val json = sharedPreferences.getString(INVENTORY_ITEMS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<InventoryItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get inventory item by tray UID
     */
    fun getInventoryItem(trayUid: String): InventoryItem? {
        return getInventoryItems().find { it.trayUid == trayUid }
    }
    
    /**
     * Save or update an inventory item
     */
    fun saveInventoryItem(inventoryItem: InventoryItem) {
        val items = getInventoryItems().toMutableList()
        val existingIndex = items.indexOfFirst { it.trayUid == inventoryItem.trayUid }
        
        val updatedItem = inventoryItem.copy(lastUpdated = LocalDateTime.now())
        
        if (existingIndex >= 0) {
            items[existingIndex] = updatedItem
        } else {
            items.add(updatedItem)
        }
        
        saveInventoryItems(items)
    }
    
    /**
     * Create automatic component setup for a new Bambu scan
     */
    fun createBambuInventoryItem(
        trayUid: String,
        filamentInfo: FilamentInfo,
        includeRefillableSpool: Boolean = false
    ): InventoryItem {
        // Create filament component from scan data
        val filamentComponent = physicalComponentRepository.createFilamentComponent(
            filamentType = filamentInfo.filamentType,
            colorName = filamentInfo.colorName,
            colorHex = filamentInfo.colorHex,
            massGrams = 1000f, // Default to 1kg - will be updated from SKU or measurement
            manufacturer = filamentInfo.manufacturerName
        )
        
        // Save the filament component
        physicalComponentRepository.saveComponent(filamentComponent)
        
        // Create component setup  
        val componentIds = mutableListOf<String>()
        componentIds.add(filamentComponent.id)
        componentIds.add("bambu_cardboard_core")
        if (includeRefillableSpool) {
            componentIds.add("bambu_refillable_spool")
        }
        
        // Try to get mass from SKU data if available
        val skuMass = getFilamentMassFromSku(filamentInfo.filamentType, filamentInfo.colorName)
        
        return InventoryItem(
            trayUid = trayUid,
            components = componentIds,
            totalMeasuredMass = null, // User will measure manually
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now(),
            notes = "Automatically created from Bambu scan"
        )
    }
    
    /**
     * Try to get filament mass from SKU data
     */
    private fun getFilamentMassFromSku(filamentType: String, colorName: String): Float? {
        val bestMatch = mappingsRepository.findBestProductMatch(filamentType, colorName)
        return bestMatch?.filamentWeightGrams
    }
    
    /**
     * Delete an inventory item
     */
    fun deleteInventoryItem(trayUid: String) {
        val items = getInventoryItems().filterNot { it.trayUid == trayUid }
        saveInventoryItems(items)
    }
    
    private fun saveInventoryItems(items: List<InventoryItem>) {
        val json = gson.toJson(items)
        sharedPreferences.edit()
            .putString(INVENTORY_ITEMS_KEY, json)
            .apply()
    }
    
    // === Mass Measurement Integration ===
    
    /**
     * Add a mass measurement to an inventory item
     */
    fun addMassMeasurement(trayUid: String, measurement: MassMeasurement) {
        val existingItem = getInventoryItem(trayUid)
        val updatedMeasurements = (existingItem?.measurements ?: emptyList()) + measurement
        
        val inventoryItem = existingItem?.copy(
            measurements = updatedMeasurements,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryItem(
            trayUid = trayUid,
            components = emptyList(), // Will be set separately
            totalMeasuredMass = measurement.measuredMassGrams,
            measurements = listOf(measurement),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
    }
    
    /**
     * Update total measured mass and recalculate filament mass
     */
    fun updateTotalMeasuredMass(trayUid: String, totalMassGrams: Float) {
        val existingItem = getInventoryItem(trayUid) ?: return
        
        // Get components for this inventory item
        val components = physicalComponentRepository.getComponents()
            .filter { it.id in existingItem.components }
        
        // Update variable mass components based on new total
        val updatedComponents = calculationService.updateVariableComponents(components, totalMassGrams)
        
        // Save updated components
        updatedComponents.forEach { component ->
            physicalComponentRepository.saveComponent(component)
        }
        
        // Update inventory item with new total mass
        val updatedItem = existingItem.copy(
            totalMeasuredMass = totalMassGrams,
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(updatedItem)
    }
    
    /**
     * Add a component to an inventory item
     */
    fun addComponentToInventoryItem(trayUid: String, componentId: String) {
        val existingItem = getInventoryItem(trayUid)
        val inventoryItem = existingItem?.withComponent(componentId) ?: InventoryItem(
            trayUid = trayUid,
            components = listOf(componentId),
            totalMeasuredMass = null,
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
    }
    
    // === Filament Status Calculation ===
    
    /**
     * Calculate current filament status for an inventory item
     */
    fun calculateFilamentStatus(trayUid: String): FilamentStatus {
        val inventoryItem = getInventoryItem(trayUid)
        if (inventoryItem == null) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = null,
                components = emptyList(),
                calculationSuccess = false,
                errorMessage = "Inventory item not found"
            )
        }
        
        // Get components for this inventory item
        val components = physicalComponentRepository.getComponents()
            .filter { it.id in inventoryItem.components }
        
        if (components.isEmpty()) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = inventoryItem.latestMeasurement,
                components = emptyList(),
                calculationSuccess = false,
                errorMessage = "No components defined"
            )
        }
        
        // Validate component combination
        val validation = calculationService.validateComponents(components)
        if (!validation.valid) {
            return FilamentStatus(
                remainingMassGrams = 0f,
                remainingPercentage = 0f,
                consumedMassGrams = 0f,
                lastMeasurement = inventoryItem.latestMeasurement,
                components = components,
                calculationSuccess = false,
                errorMessage = validation.message
            )
        }
        
        val latestMeasurement = inventoryItem.latestMeasurement
        val filamentComponents = components.filter { it.variableMass }
        val currentFilamentMass = filamentComponents.sumOf { it.massGrams.toDouble() }.toFloat()
        
        // If we don't have measurements, return current component mass
        if (latestMeasurement == null || inventoryItem.totalMeasuredMass == null) {
            return FilamentStatus(
                remainingMassGrams = currentFilamentMass,
                remainingPercentage = 1f, // Unknown percentage
                consumedMassGrams = 0f,
                lastMeasurement = latestMeasurement,
                components = components,
                calculationSuccess = true,
                errorMessage = null
            )
        }
        
        // Get initial filament mass (from first measurement or SKU data)
        val initialFilamentMass = findInitialFilamentMass(inventoryItem, components)
        
        if (initialFilamentMass == null || initialFilamentMass <= 0) {
            return FilamentStatus(
                remainingMassGrams = currentFilamentMass,
                remainingPercentage = 1f, // Unknown percentage
                consumedMassGrams = 0f,
                lastMeasurement = latestMeasurement,
                components = components,
                calculationSuccess = true,
                errorMessage = null
            )
        }
        
        val consumedMass = maxOf(0f, initialFilamentMass - currentFilamentMass)
        val remainingPercentage = if (initialFilamentMass > 0) {
            maxOf(0f, minOf(1f, currentFilamentMass / initialFilamentMass))
        } else 1f
        
        return FilamentStatus(
            remainingMassGrams = currentFilamentMass,
            remainingPercentage = remainingPercentage,
            consumedMassGrams = consumedMass,
            lastMeasurement = latestMeasurement,
            components = components,
            calculationSuccess = true,
            errorMessage = null
        )
    }
    
    /**
     * Find the initial filament mass from measurements or SKU data
     */
    private fun findInitialFilamentMass(
        inventoryItem: InventoryItem, 
        components: List<PhysicalComponent>
    ): Float? {
        // Try to get from earliest measurement
        val earliestMeasurement = inventoryItem.measurements.minByOrNull { it.measuredAt }
        if (earliestMeasurement != null && earliestMeasurement.measurementType == MeasurementType.FULL_WEIGHT) {
            val fixedComponentsMass = components.filter { !it.variableMass }
                .sumOf { it.massGrams.toDouble() }.toFloat()
            return earliestMeasurement.measuredMassGrams - fixedComponentsMass
        }
        
        // Fallback to current filament component mass (from SKU or default)
        return components.filter { it.variableMass }
            .sumOf { it.massGrams.toDouble() }.toFloat()
            .takeIf { it > 0 }
    }
    
    
    /**
     * Get all inventory items with calculated status
     */
    fun getInventoryItemsWithStatus(): List<Pair<InventoryItem, FilamentStatus>> {
        return getInventoryItems().map { item ->
            item to calculateFilamentStatus(item.trayUid)
        }
    }
    
    companion object {
        private const val INVENTORY_ITEMS_KEY = "inventory_items"
    }
}