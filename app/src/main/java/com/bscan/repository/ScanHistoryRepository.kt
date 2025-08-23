package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.ScanHistory
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanHistoryRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    
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
    
    private val maxHistorySize = 100 // Keep last 100 scans
    
    fun saveScan(scanHistory: ScanHistory) {
        val scans = getAllScans().toMutableList()
        
        // Add new scan with current timestamp if not set
        val scanWithTimestamp = if (scanHistory.timestamp == LocalDateTime.MIN) {
            scanHistory.copy(timestamp = LocalDateTime.now())
        } else {
            scanHistory
        }
        
        scans.add(0, scanWithTimestamp) // Add to beginning (most recent first)
        
        // Keep only the most recent scans
        if (scans.size > maxHistorySize) {
            scans.subList(maxHistorySize, scans.size).clear()
        }
        
        // Save to SharedPreferences
        val scansJson = gson.toJson(scans)
        sharedPreferences.edit()
            .putString("scans", scansJson)
            .apply()
    }
    
    fun getAllScans(): List<ScanHistory> {
        val scansJson = sharedPreferences.getString("scans", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<ScanHistory>>() {}.type
            gson.fromJson(scansJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, return empty list and clear storage
            clearHistory()
            emptyList()
        }
    }
    
    fun getScansByResult(result: com.bscan.model.ScanResult): List<ScanHistory> {
        return getAllScans().filter { it.scanResult == result }
    }
    
    fun getSuccessfulScans(): List<ScanHistory> {
        return getScansByResult(com.bscan.model.ScanResult.SUCCESS)
    }
    
    fun getFailedScans(): List<ScanHistory> {
        return getAllScans().filter { it.scanResult != com.bscan.model.ScanResult.SUCCESS }
    }
    
    fun getScansByTagUid(tagUid: String): List<ScanHistory> {
        return getAllScans().filter { it.uid == tagUid }
    }
    
    fun clearHistory() {
        sharedPreferences.edit()
            .remove("scans")
            .apply()
    }
    
    fun getHistoryCount(): Int {
        return getAllScans().size
    }
    
    fun getSuccessRate(): Float {
        val allScans = getAllScans()
        if (allScans.isEmpty()) return 0f
        
        val successfulScans = allScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
        return successfulScans.toFloat() / allScans.size
    }
    
    fun getUniqueSpools(): List<UniqueSpool> {
        val allScans = getAllScans()
        
        // Group scans by UID
        val scansByUid = allScans.groupBy { it.uid }
        
        return scansByUid.mapNotNull { (uid, scans) ->
            // Find the most recent successful scan with filament info
            val mostRecentSuccessfulScan = scans
                .filter { it.scanResult == com.bscan.model.ScanResult.SUCCESS && it.filamentInfo != null }
                .maxByOrNull { it.timestamp }
                ?: return@mapNotNull null // Skip if no successful scans with filament info
            
            val scanCount = scans.size
            val successCount = scans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
            val lastScanned = scans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
            val successRate = if (scanCount > 0) successCount.toFloat() / scanCount else 0f
            
            UniqueSpool(
                uid = uid,
                filamentInfo = mostRecentSuccessfulScan.filamentInfo!!,
                scanCount = scanCount,
                successCount = successCount,
                lastScanned = lastScanned,
                successRate = successRate
            )
        }.sortedByDescending { it.lastScanned } // Most recently scanned first
    }
    
    /**
     * Groups scans by tray UID (spool) instead of tag UID.
     * Each physical spool has 2 tags with the same tray UID.
     * Returns one entry per physical spool.
     */
    fun getUniqueSpoolsByTray(): List<UniqueSpool> {
        val allScans = getAllScans()
        
        // Group scans by tray UID instead of tag UID
        val scansByTrayUid = allScans
            .filter { it.filamentInfo?.trayUid?.isNotEmpty() == true }
            .groupBy { it.filamentInfo!!.trayUid }
        
        return scansByTrayUid.mapNotNull { (trayUid, scans) ->
            // Find the most recent successful scan with filament info from this tray
            val mostRecentSuccessfulScan = scans
                .filter { it.scanResult == com.bscan.model.ScanResult.SUCCESS && it.filamentInfo != null }
                .maxByOrNull { it.timestamp }
                ?: return@mapNotNull null // Skip if no successful scans with filament info
            
            val scanCount = scans.size
            val successCount = scans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
            val lastScanned = scans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
            val successRate = if (scanCount > 0) successCount.toFloat() / scanCount else 0f
            
            // Get all tag UIDs for this tray
            val tagUidsForTray = scans
                .mapNotNull { it.filamentInfo?.tagUid }
                .distinct()
            
            UniqueSpool(
                uid = trayUid, // Use tray UID instead of tag UID
                filamentInfo = mostRecentSuccessfulScan.filamentInfo!!,
                scanCount = scanCount,
                successCount = successCount,
                lastScanned = lastScanned,
                successRate = successRate
            )
        }.sortedByDescending { it.lastScanned } // Most recently scanned first
    }
    
    fun getSpoolByTagUid(tagUid: String): UniqueSpool? {
        // First try to find by tag UID in the old method for backward compatibility
        val byTag = getUniqueSpools().firstOrNull { it.uid == tagUid }
        if (byTag != null) return byTag
        
        // If not found, try to find by tray UID (since we might be looking for tray-grouped spools)
        return getUniqueSpoolsByTray().firstOrNull { it.uid == tagUid }
    }
    
    fun getSpoolByTrayUid(trayUid: String): UniqueSpool? {
        return getUniqueSpoolsByTray().firstOrNull { it.uid == trayUid }
    }
    
    /**
     * Gets detailed information about a spool by tray UID, including all associated tags and scans
     */
    fun getSpoolDetails(trayUid: String): SpoolDetails? {
        val allScans = getAllScans()
        
        // Get all scans for this tray
        val spoolScans = allScans.filter { 
            it.filamentInfo?.trayUid == trayUid 
        }
        
        if (spoolScans.isEmpty()) return null
        
        // Get the most recent successful scan for filament info
        val mostRecentSuccessfulScan = spoolScans
            .filter { it.scanResult == com.bscan.model.ScanResult.SUCCESS && it.filamentInfo != null }
            .maxByOrNull { it.timestamp }
            ?: return null
        
        // Get all unique tag UIDs for this spool
        val tagUids = spoolScans
            .mapNotNull { it.filamentInfo?.tagUid }
            .distinct()
        
        // Get scans grouped by tag UID
        val scansByTag = spoolScans.groupBy { it.filamentInfo?.tagUid ?: "" }
            .filter { it.key.isNotEmpty() }
        
        return SpoolDetails(
            trayUid = trayUid,
            filamentInfo = mostRecentSuccessfulScan.filamentInfo!!,
            tagUids = tagUids,
            allScans = spoolScans.sortedByDescending { it.timestamp },
            scansByTag = scansByTag,
            totalScans = spoolScans.size,
            successfulScans = spoolScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS },
            lastScanned = spoolScans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
        )
    }
    
    fun getFilamentTypes(): List<String> {
        return getSuccessfulScans()
            .mapNotNull { it.filamentInfo?.filamentType }
            .distinct()
            .sorted()
    }
    
    fun clearGeneratedData() {
        val allScans = getAllScans()
        val realScans = allScans.filter { scan ->
            val isGenerated = scan.debugInfo.parsingDetails["sampleData"] as? Boolean ?: false
            !isGenerated
        }
        
        // Save only the real scans back to SharedPreferences
        val scansJson = gson.toJson(realScans)
        sharedPreferences.edit()
            .putString("scans", scansJson)
            .apply()
    }
    
    fun getGeneratedDataCount(): Int {
        return getAllScans().count { scan ->
            scan.debugInfo.parsingDetails["sampleData"] as? Boolean ?: false
        }
    }
}

/**
 * Represents a unique scanned tag and its associated filament information.
 * Note: Despite the name "UniqueSpool", this actually represents unique tags.
 * Each physical spool has 2 tags, so there can be 2 UniqueSpool entries per physical spool.
 * The uid field contains the tag UID (unique per tag).
 */
data class UniqueSpool(
    val uid: String, // Tag UID (unique per individual tag)
    val filamentInfo: com.bscan.model.FilamentInfo,
    val scanCount: Int,
    val successCount: Int,
    val lastScanned: LocalDateTime,
    val successRate: Float
)

/**
 * Represents detailed information about a physical spool (identified by tray UID).
 * Contains all associated tags, scans, and filament information.
 */
data class SpoolDetails(
    val trayUid: String, // Tray UID (shared across both tags on the spool)
    val filamentInfo: com.bscan.model.FilamentInfo,
    val tagUids: List<String>, // All tag UIDs associated with this spool
    val allScans: List<ScanHistory>, // All scans for this spool, sorted by timestamp desc
    val scansByTag: Map<String, List<ScanHistory>>, // Scans grouped by tag UID
    val totalScans: Int,
    val successfulScans: Int,
    val lastScanned: LocalDateTime
)