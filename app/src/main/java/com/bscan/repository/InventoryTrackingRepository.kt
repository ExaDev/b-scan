package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.FilamentInfo
import com.bscan.model.DecryptedScanData
import com.bscan.model.ScanResult
import com.bscan.interpreter.InterpreterFactory
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for tracking inventory items using the hierarchical component system.
 * 
 * This repository now delegates to the ComponentRepository and BambuComponentFactory
 * to create hierarchical component structures from RFID scans.
 */
class InventoryTrackingRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("inventory_tracking", Context.MODE_PRIVATE)
    
    // New component-based system
    private val componentRepository by lazy { com.bscan.repository.ComponentRepository(context) }
    private val bambuFactory by lazy { com.bscan.service.BambuComponentFactory(context) }
    
    // InterpreterFactory for runtime interpretation
    private val catalogRepository by lazy { CatalogRepository(context) }
    private val userRepository by lazy { UserDataRepository(context) }
    private val unifiedDataAccess by lazy { UnifiedDataAccess(catalogRepository, userRepository) }
    private var interpreterFactory = InterpreterFactory(unifiedDataAccess)
    
    // Custom LocalDateTime adapter for Gson (copied from ScanHistoryRepository)
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
    
    // StateFlow for reactive data updates
    private val _inventoryDataFlow = MutableStateFlow<List<InventoryData>>(emptyList())
    val inventoryDataFlow: StateFlow<List<InventoryData>> = _inventoryDataFlow.asStateFlow()
    
    private val _statisticsFlow = MutableStateFlow<InventoryStatistics?>(null)
    val statisticsFlow: StateFlow<InventoryStatistics?> = _statisticsFlow.asStateFlow()
    
    init {
        // Initialize with current data
        refreshFlows()
    }
    
    /**
     * Records a successful scan with hierarchical component creation.
     * Creates or updates component hierarchy for the scanned Bambu reel.
     */
    suspend fun recordScan(decryptedScanData: DecryptedScanData) = withContext(Dispatchers.IO) {
        if (decryptedScanData.scanResult == ScanResult.SUCCESS) {
            // Use FilamentInterpreter to get FilamentInfo at runtime
            val filamentInfo = try {
                interpreterFactory.interpret(decryptedScanData)
            } catch (e: Exception) {
                null
            }
            
            if (filamentInfo != null) {
                val trayUid = filamentInfo.trayUid
                val tagUid = decryptedScanData.tagUid
                
                if (trayUid.isNotBlank()) {
                    // Use new component system to create/update hierarchical structure
                    val trayComponent = bambuFactory.processBambuRfidScan(tagUid, trayUid, filamentInfo)
                    
                    if (trayComponent != null) {
                        android.util.Log.i(TAG, "Successfully processed RFID scan for tray: $trayUid")
                        refreshFlows()
                    } else {
                        android.util.Log.e(TAG, "Failed to process RFID scan for tray: $trayUid")
                    }
                    
                    // Still maintain legacy inventory tracking for compatibility
                    addTagToInventory(trayUid, tagUid, decryptedScanData.timestamp, filamentInfo)
                }
            }
        }
    }
    
    /**
     * Refresh the FilamentInterpreter with updated mappings
     */
    fun refreshMappings() {
        interpreterFactory.refreshMappings()
    }
    
    /**
     * Associates a tag UID with an inventory UID
     */
    private suspend fun addTagToInventory(inventoryUid: String, tagUid: String, timestamp: LocalDateTime, filamentInfo: FilamentInfo) {
        val inventoryData = getInventoryData().toMutableMap()
        
        val existingInventory = inventoryData[inventoryUid]
        if (existingInventory != null) {
            // Update existing inventory item
            val updatedTagEntries = existingInventory.tagEntries.toMutableMap()
            updatedTagEntries[tagUid] = InventoryTagEntry(
                tagUid = tagUid,
                firstSeen = updatedTagEntries[tagUid]?.firstSeen ?: timestamp,
                lastSeen = timestamp,
                scanCount = (updatedTagEntries[tagUid]?.scanCount ?: 0) + 1,
                filamentInfo = filamentInfo
            )
            
            inventoryData[inventoryUid] = existingInventory.copy(
                tagEntries = updatedTagEntries,
                lastUpdated = timestamp,
                totalScans = existingInventory.totalScans + 1
            )
        } else {
            // Create new inventory item
            inventoryData[inventoryUid] = InventoryData(
                inventoryUid = inventoryUid,
                firstSeen = timestamp,
                lastUpdated = timestamp,
                tagEntries = mapOf(
                    tagUid to InventoryTagEntry(
                        tagUid = tagUid,
                        firstSeen = timestamp,
                        lastSeen = timestamp,
                        scanCount = 1,
                        filamentInfo = filamentInfo
                    )
                ),
                totalScans = 1
            )
        }
        
        saveInventoryData(inventoryData)
        refreshFlows() // Notify observers of data changes
    }
    
    /**
     * Gets all tracked inventory data
     */
    fun getAllInventoryItems(): List<InventoryData> {
        return getInventoryData().values.sortedByDescending { it.lastUpdated }
    }
    
    /**
     * Gets data for a specific inventory item
     */
    fun getInventoryByUid(inventoryUid: String): InventoryData? {
        return getInventoryData()[inventoryUid]
    }
    
    /**
     * Gets summary statistics
     */
    fun getInventoryStatistics(): InventoryStatistics {
        val inventoryData = getInventoryData()
        val totalItems = inventoryData.size
        val totalUniqueTags = inventoryData.values.sumOf { it.tagEntries.size }
        val totalScans = inventoryData.values.sumOf { it.totalScans }
        
        val averageTagsPerItem = if (totalItems > 0) totalUniqueTags.toFloat() / totalItems else 0f
        val averageScansPerItem = if (totalItems > 0) totalScans.toFloat() / totalItems else 0f
        
        // Most active item (by scan count)
        val mostActiveItem = inventoryData.values.maxByOrNull { it.totalScans }
        
        // Item with most unique tags
        val itemWithMostTags = inventoryData.values.maxByOrNull { it.tagEntries.size }
        
        return InventoryStatistics(
            totalInventoryItems = totalItems,
            totalUniqueTags = totalUniqueTags,
            totalScans = totalScans,
            averageTagsPerItem = averageTagsPerItem,
            averageScansPerItem = averageScansPerItem,
            mostActiveItem = mostActiveItem,
            itemWithMostTags = itemWithMostTags
        )
    }
    
    /**
     * Gets all filament types seen across all inventory items
     */
    fun getAllFilamentTypes(): List<String> {
        return getInventoryData().values
            .flatMap { item -> item.tagEntries.values.map { it.filamentInfo.filamentType } }
            .distinct()
            .sorted()
    }
    
    /**
     * Finds all inventory items that contain a specific filament type
     */
    fun getInventoryByFilamentType(filamentType: String): List<InventoryData> {
        return getInventoryData().values.filter { item ->
            item.tagEntries.values.any { it.filamentInfo.filamentType == filamentType }
        }.sortedByDescending { it.lastUpdated }
    }
    
    /**
     * Clears all inventory tracking data
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
        refreshFlows() // Notify observers of data changes
    }
    
    /**
     * Removes a specific inventory item from tracking
     */
    suspend fun removeInventoryItem(inventoryUid: String) = withContext(Dispatchers.IO) {
        val inventoryData = getInventoryData().toMutableMap()
        inventoryData.remove(inventoryUid)
        saveInventoryData(inventoryData)
        refreshFlows() // Notify observers of data changes
    }
    
    // === Bridge methods for new Component system ===
    
    /**
     * Get inventory items (components with unique identifiers)
     */
    fun getInventoryItems(): List<com.bscan.model.Component> {
        return componentRepository.getInventoryItems()
    }
    
    /**
     * Find inventory item by tray UID
     */
    fun findInventoryByTrayUid(trayUid: String): com.bscan.model.Component? {
        return componentRepository.findInventoryByUniqueId(trayUid)
    }
    
    /**
     * Get all components for an inventory item
     */
    fun getComponentsForInventory(trayUid: String): List<com.bscan.model.Component> {
        val inventory = findInventoryByTrayUid(trayUid) ?: return emptyList()
        return componentRepository.getChildComponents(inventory.id)
    }
    
    /**
     * Get total mass of an inventory item including all components
     */
    fun getInventoryTotalMass(trayUid: String): Float {
        val inventory = findInventoryByTrayUid(trayUid) ?: return 0f
        return componentRepository.getTotalMass(inventory.id)
    }
    
    /**
     * Add a component to an existing inventory item
     */
    suspend fun addComponentToInventory(trayUid: String, component: com.bscan.model.Component): Boolean = withContext(Dispatchers.IO) {
        return@withContext bambuFactory.addComponentToTray(trayUid, component)
    }
    
    /**
     * Infer and add a component based on total weight measurement
     */
    suspend fun inferComponentFromWeight(
        trayUid: String,
        componentName: String,
        componentCategory: String,
        totalMeasuredMass: Float
    ): com.bscan.model.Component? = withContext(Dispatchers.IO) {
        return@withContext bambuFactory.inferAndAddComponent(trayUid, componentName, componentCategory, totalMeasuredMass)
    }
    
    companion object {
        private const val TAG = "InventoryTrackingRepository"
    }
    
    private fun getInventoryData(): Map<String, InventoryData> {
        val inventoryJson = sharedPreferences.getString("inventory_data", null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, InventoryData>>() {}.type
            gson.fromJson(inventoryJson, type) ?: emptyMap()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, clear without refreshing flows to prevent recursion
            sharedPreferences.edit().clear().apply()
            emptyMap()
        }
    }
    
    private suspend fun saveInventoryData(inventoryData: Map<String, InventoryData>) = withContext(Dispatchers.IO) {
        val inventoryJson = gson.toJson(inventoryData)
        sharedPreferences.edit()
            .putString("inventory_data", inventoryJson)
            .apply()
    }
    
    /**
     * Refreshes the StateFlows with current data
     */
    private fun refreshFlows() {
        _inventoryDataFlow.value = getAllInventoryItems()
        _statisticsFlow.value = getInventoryStatistics()
    }
}

/**
 * Data class representing a tracked inventory item and its associated RFID tags.
 * The inventoryUid uniquely identifies the inventory item (not the physical spool hardware).
 */
data class InventoryData(
    val inventoryUid: String,
    val firstSeen: LocalDateTime,
    val lastUpdated: LocalDateTime,
    val tagEntries: Map<String, InventoryTagEntry>,
    val totalScans: Int
) {
    val uniqueTagCount: Int get() = tagEntries.size
    val filamentTypes: List<String> get() = tagEntries.values.map { it.filamentInfo.filamentType }.distinct()
    val colorNames: List<String> get() = tagEntries.values.map { it.filamentInfo.colorName }.distinct()
}

/**
 * Data class representing an RFID tag entry associated with an inventory item
 */
data class InventoryTagEntry(
    val tagUid: String,
    val firstSeen: LocalDateTime,
    val lastSeen: LocalDateTime,
    val scanCount: Int,
    val filamentInfo: FilamentInfo
)

/**
 * Statistics summary for all inventory tracking
 */
data class InventoryStatistics(
    val totalInventoryItems: Int,
    val totalUniqueTags: Int,
    val totalScans: Int,
    val averageTagsPerItem: Float,
    val averageScansPerItem: Float,
    val mostActiveItem: InventoryData?,
    val itemWithMostTags: InventoryData?
)