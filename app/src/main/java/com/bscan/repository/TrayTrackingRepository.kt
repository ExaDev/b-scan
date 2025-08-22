package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TrayTrackingRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("tray_tracking", Context.MODE_PRIVATE)
    
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
    
    /**
     * Records a successful scan with tray UID tracking
     */
    fun recordScan(scanHistory: ScanHistory) {
        if (scanHistory.scanResult == ScanResult.SUCCESS && scanHistory.filamentInfo != null) {
            val trayUid = scanHistory.filamentInfo.trayUid
            val tagUid = scanHistory.uid
            
            if (trayUid.isNotBlank()) {
                addTagToTray(trayUid, tagUid, scanHistory.timestamp, scanHistory.filamentInfo)
            }
        }
    }
    
    /**
     * Associates a tag UID with a tray UID
     */
    private fun addTagToTray(trayUid: String, tagUid: String, timestamp: LocalDateTime, filamentInfo: FilamentInfo) {
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
    fun clearAllData() {
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * Removes a specific tray from tracking
     */
    fun removeTray(trayUid: String) {
        val trayData = getTrayData().toMutableMap()
        trayData.remove(trayUid)
        saveTrayData(trayData)
    }
    
    private fun getTrayData(): Map<String, TrayData> {
        val trayJson = sharedPreferences.getString("tray_data", null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, TrayData>>() {}.type
            gson.fromJson(trayJson, type) ?: emptyMap()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, clear and return empty
            clearAllData()
            emptyMap()
        }
    }
    
    private fun saveTrayData(trayData: Map<String, TrayData>) {
        val trayJson = gson.toJson(trayData)
        sharedPreferences.edit()
            .putString("tray_data", trayJson)
            .apply()
    }
}

/**
 * Data class representing a tracked tray and its associated tags
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
 * Data class representing a tag entry within a tray
 */
data class TrayTagEntry(
    val tagUid: String,
    val firstSeen: LocalDateTime,
    val lastSeen: LocalDateTime,
    val scanCount: Int,
    val filamentInfo: FilamentInfo
)

/**
 * Statistics summary for all tray tracking
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