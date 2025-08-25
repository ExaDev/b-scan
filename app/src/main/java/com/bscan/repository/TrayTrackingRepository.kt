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
 * Repository for tracking filament reels using the hierarchical component system.
 * 
 * This repository now delegates to the ComponentRepository and BambuComponentFactory
 * to create hierarchical component structures from RFID scans.
 */
class TrayTrackingRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("tray_tracking", Context.MODE_PRIVATE)
    
    // New component-based system
    private val componentRepository by lazy { com.bscan.repository.ComponentRepository(context) }
    private val bambuFactory by lazy { com.bscan.service.BambuComponentFactory(context) }
    
    // InterpreterFactory for runtime interpretation
    private val mappingsRepository by lazy { MappingsRepository(context) }
    private var interpreterFactory = InterpreterFactory(mappingsRepository)
    
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
    private val _trayDataFlow = MutableStateFlow<List<TrayData>>(emptyList())
    val trayDataFlow: StateFlow<List<TrayData>> = _trayDataFlow.asStateFlow()
    
    private val _statisticsFlow = MutableStateFlow<TrayStatistics?>(null)
    val statisticsFlow: StateFlow<TrayStatistics?> = _statisticsFlow.asStateFlow()
    
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
                    
                    // Still maintain legacy tray tracking for compatibility
                    addTagToTray(trayUid, tagUid, decryptedScanData.timestamp, filamentInfo)
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
     * Associates a tag UID with a tray UID
     */
    private suspend fun addTagToTray(trayUid: String, tagUid: String, timestamp: LocalDateTime, filamentInfo: FilamentInfo) {
        val trayData = getTrayData().toMutableMap()
        
        val existingTray = trayData[trayUid]
        if (existingTray != null) {
            // Update existing tray
            val updatedTagEntries = existingTray.tagEntries.toMutableMap()
            updatedTagEntries[tagUid] = TrayTagEntry(
                tagUid = tagUid,
                firstSeen = updatedTagEntries[tagUid]?.firstSeen ?: timestamp,
                lastSeen = timestamp,
                scanCount = (updatedTagEntries[tagUid]?.scanCount ?: 0) + 1,
                filamentInfo = filamentInfo
            )
            
            trayData[trayUid] = existingTray.copy(
                tagEntries = updatedTagEntries,
                lastUpdated = timestamp,
                totalScans = existingTray.totalScans + 1
            )
        } else {
            // Create new tray
            trayData[trayUid] = TrayData(
                trayUid = trayUid,
                firstSeen = timestamp,
                lastUpdated = timestamp,
                tagEntries = mapOf(
                    tagUid to TrayTagEntry(
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
        
        saveTrayData(trayData)
        refreshFlows() // Notify observers of data changes
    }
    
    /**
     * Gets all tracked tray data
     */
    fun getAllTrays(): List<TrayData> {
        return getTrayData().values.sortedByDescending { it.lastUpdated }
    }
    
    /**
     * Gets data for a specific tray
     */
    fun getTrayByUid(trayUid: String): TrayData? {
        return getTrayData()[trayUid]
    }
    
    /**
     * Gets summary statistics
     */
    fun getTrayStatistics(): TrayStatistics {
        val trayData = getTrayData()
        val totalTrays = trayData.size
        val totalUniqueTags = trayData.values.sumOf { it.tagEntries.size }
        val totalScans = trayData.values.sumOf { it.totalScans }
        
        val averageTagsPerTray = if (totalTrays > 0) totalUniqueTags.toFloat() / totalTrays else 0f
        val averageScansPerTray = if (totalTrays > 0) totalScans.toFloat() / totalTrays else 0f
        
        // Most active tray (by scan count)
        val mostActiveTray = trayData.values.maxByOrNull { it.totalScans }
        
        // Tray with most unique tags
        val trayWithMostTags = trayData.values.maxByOrNull { it.tagEntries.size }
        
        return TrayStatistics(
            totalTrays = totalTrays,
            totalUniqueTags = totalUniqueTags,
            totalScans = totalScans,
            averageTagsPerTray = averageTagsPerTray,
            averageScansPerTray = averageScansPerTray,
            mostActiveTray = mostActiveTray,
            trayWithMostTags = trayWithMostTags
        )
    }
    
    /**
     * Gets all filament types seen across all trays
     */
    fun getAllFilamentTypes(): List<String> {
        return getTrayData().values
            .flatMap { tray -> tray.tagEntries.values.map { it.filamentInfo.filamentType } }
            .distinct()
            .sorted()
    }
    
    /**
     * Finds all trays that contain a specific filament type
     */
    fun getTraysByFilamentType(filamentType: String): List<TrayData> {
        return getTrayData().values.filter { tray ->
            tray.tagEntries.values.any { it.filamentInfo.filamentType == filamentType }
        }.sortedByDescending { it.lastUpdated }
    }
    
    /**
     * Clears all tray tracking data
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
        refreshFlows() // Notify observers of data changes
    }
    
    /**
     * Removes a specific tray from tracking
     */
    suspend fun removeTray(trayUid: String) = withContext(Dispatchers.IO) {
        val trayData = getTrayData().toMutableMap()
        trayData.remove(trayUid)
        saveTrayData(trayData)
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
        private const val TAG = "TrayTrackingRepository"
    }
    
    private fun getTrayData(): Map<String, TrayData> {
        val trayJson = sharedPreferences.getString("tray_data", null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, TrayData>>() {}.type
            gson.fromJson(trayJson, type) ?: emptyMap()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, clear without refreshing flows to prevent recursion
            sharedPreferences.edit().clear().apply()
            emptyMap()
        }
    }
    
    private suspend fun saveTrayData(trayData: Map<String, TrayData>) = withContext(Dispatchers.IO) {
        val trayJson = gson.toJson(trayData)
        sharedPreferences.edit()
            .putString("tray_data", trayJson)
            .apply()
    }
    
    /**
     * Refreshes the StateFlows with current data
     */
    private fun refreshFlows() {
        _trayDataFlow.value = getAllTrays()
        _statisticsFlow.value = getTrayStatistics()
    }
}

/**
 * Data class representing a tracked filament reel and its associated RFID tags.
 * The trayUid uniquely identifies the filament reel (not the physical spool hardware).
 */
data class TrayData(
    val trayUid: String,
    val firstSeen: LocalDateTime,
    val lastUpdated: LocalDateTime,
    val tagEntries: Map<String, TrayTagEntry>,
    val totalScans: Int
) {
    val uniqueTagCount: Int get() = tagEntries.size
    val filamentTypes: List<String> get() = tagEntries.values.map { it.filamentInfo.filamentType }.distinct()
    val colorNames: List<String> get() = tagEntries.values.map { it.filamentInfo.colorName }.distinct()
}

/**
 * Data class representing an RFID tag entry associated with a filament reel
 */
data class TrayTagEntry(
    val tagUid: String,
    val firstSeen: LocalDateTime,
    val lastSeen: LocalDateTime,
    val scanCount: Int,
    val filamentInfo: FilamentInfo
)

/**
 * Statistics summary for all filament reel tracking
 */
data class TrayStatistics(
    val totalTrays: Int,
    val totalUniqueTags: Int,
    val totalScans: Int,
    val averageTagsPerTray: Float,
    val averageScansPerTray: Float,
    val mostActiveTray: TrayData?,
    val trayWithMostTags: TrayData?
)