package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import com.bscan.logic.WeightCalculationService
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
    
    private val spoolWeightRepository by lazy { SpoolWeightRepository(context) }
    private val calculationService by lazy { WeightCalculationService() }
    
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
    
    // === Weight Measurement Integration ===
    
    /**
     * Add a weight measurement to an inventory item
     */
    fun addWeightMeasurement(trayUid: String, measurement: FilamentWeightMeasurement) {
        val existingItem = getInventoryItem(trayUid)
        val updatedMeasurements = (existingItem?.measurements ?: emptyList()) + measurement
        
        val inventoryItem = existingItem?.copy(
            measurements = updatedMeasurements,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryItem(
            trayUid = trayUid,
            currentConfigurationId = measurement.spoolConfigurationId,
            expectedFilamentWeightGrams = null,
            measurements = listOf(measurement),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
        
        // Also save to SpoolWeightRepository for global access
        spoolWeightRepository.saveMeasurement(measurement)
    }
    
    /**
     * Set spool configuration for an inventory item
     */
    fun setSpoolConfiguration(trayUid: String, configurationId: String) {
        val existingItem = getInventoryItem(trayUid)
        val inventoryItem = existingItem?.copy(
            currentConfigurationId = configurationId,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryItem(
            trayUid = trayUid,
            currentConfigurationId = configurationId,
            expectedFilamentWeightGrams = null,
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now()
        )
        
        saveInventoryItem(inventoryItem)
    }
    
    /**
     * Set expected filament weight for an inventory item
     */
    fun setExpectedFilamentWeight(trayUid: String, expectedWeightGrams: Float) {
        val existingItem = getInventoryItem(trayUid)
        val inventoryItem = existingItem?.copy(
            expectedFilamentWeightGrams = expectedWeightGrams,
            lastUpdated = LocalDateTime.now()
        ) ?: InventoryItem(
            trayUid = trayUid,
            currentConfigurationId = null,
            expectedFilamentWeightGrams = expectedWeightGrams,
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
                remainingWeightGrams = 0f,
                remainingPercentage = 0f,
                consumedWeightGrams = 0f,
                lastMeasurement = null,
                spoolConfiguration = null,
                calculationSuccess = false,
                errorMessage = "Inventory item not found"
            )
        }
        
        val configuration = inventoryItem.currentConfigurationId?.let { 
            spoolWeightRepository.getConfiguration(it) 
        }
        
        if (configuration == null) {
            return FilamentStatus(
                remainingWeightGrams = 0f,
                remainingPercentage = 0f,
                consumedWeightGrams = 0f,
                lastMeasurement = inventoryItem.latestMeasurement,
                spoolConfiguration = null,
                calculationSuccess = false,
                errorMessage = "No spool configuration set"
            )
        }
        
        val components = spoolWeightRepository.getComponents()
        val latestMeasurement = inventoryItem.latestMeasurement
        
        if (latestMeasurement == null) {
            return FilamentStatus(
                remainingWeightGrams = 0f,
                remainingPercentage = 0f,
                consumedWeightGrams = 0f,
                lastMeasurement = null,
                spoolConfiguration = configuration,
                calculationSuccess = false,
                errorMessage = "No weight measurements available"
            )
        }
        
        // Calculate current filament weight from latest measurement
        val calculationResult = calculationService.calculateFilamentWeight(
            latestMeasurement.measuredWeightGrams,
            configuration,
            components,
            latestMeasurement.measurementType
        )
        
        if (!calculationResult.success) {
            return FilamentStatus(
                remainingWeightGrams = 0f,
                remainingPercentage = 0f,
                consumedWeightGrams = 0f,
                lastMeasurement = latestMeasurement,
                spoolConfiguration = configuration,
                calculationSuccess = false,
                errorMessage = calculationResult.errorMessage
            )
        }
        
        val currentFilamentWeight = calculationResult.filamentWeightGrams
        
        // Calculate consumption based on expected weight or first measurement
        val initialFilamentWeight = inventoryItem.expectedFilamentWeightGrams 
            ?: findInitialFilamentWeight(inventoryItem, configuration, components)
        
        if (initialFilamentWeight == null || initialFilamentWeight <= 0) {
            return FilamentStatus(
                remainingWeightGrams = currentFilamentWeight,
                remainingPercentage = 1f, // Unknown percentage
                consumedWeightGrams = 0f,
                lastMeasurement = latestMeasurement,
                spoolConfiguration = configuration,
                calculationSuccess = true,
                errorMessage = null
            )
        }
        
        val consumedWeight = maxOf(0f, initialFilamentWeight - currentFilamentWeight)
        val remainingPercentage = if (initialFilamentWeight > 0) {
            maxOf(0f, minOf(1f, currentFilamentWeight / initialFilamentWeight))
        } else 1f
        
        return FilamentStatus(
            remainingWeightGrams = currentFilamentWeight,
            remainingPercentage = remainingPercentage,
            consumedWeightGrams = consumedWeight,
            lastMeasurement = latestMeasurement,
            spoolConfiguration = configuration,
            calculationSuccess = true,
            errorMessage = null
        )
    }
    
    /**
     * Find initial filament weight from first measurement or highest measurement
     */
    private fun findInitialFilamentWeight(
        inventoryItem: InventoryItem,
        configuration: SpoolConfiguration,
        components: List<SpoolComponent>
    ): Float? {
        if (inventoryItem.measurements.isEmpty()) return null
        
        // Find the highest filament weight from measurements (likely the initial one)
        val components = spoolWeightRepository.getComponents()
        val highestFilamentWeight = inventoryItem.measurements.mapNotNull { measurement ->
            val result = calculationService.calculateFilamentWeight(
                measurement.measuredWeightGrams,
                configuration,
                components,
                measurement.measurementType
            )
            if (result.success) result.filamentWeightGrams else null
        }.maxOrNull()
        
        return highestFilamentWeight
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