package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanResult
import com.bscan.model.RfidDataFormat
import com.bscan.model.tagFormat
import com.bscan.model.manufacturerName
import com.bscan.model.sectorCount
import com.bscan.model.tagSizeBytes
import com.bscan.interpreter.InterpreterFactory
import com.bscan.repository.CatalogRepository
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScanHistoryRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    
    // InterpreterFactory for runtime interpretation
    private val catalogRepository by lazy { CatalogRepository(context) }
    private val userRepository by lazy { UserDataRepository(context) }
    private val unifiedDataAccess by lazy { UnifiedDataAccess(catalogRepository, userRepository) }
    private var interpreterFactory = InterpreterFactory(unifiedDataAccess)
    
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
    
    // Custom ByteArray adapter for Gson (for encrypted data)
    private val byteArrayAdapter = object : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
        override fun serialize(src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.let { android.util.Base64.encodeToString(it, android.util.Base64.DEFAULT) })
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ByteArray? {
            return try {
                json?.asString?.let { android.util.Base64.decode(it, android.util.Base64.DEFAULT) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .registerTypeAdapter(ByteArray::class.java, byteArrayAdapter)
        .create()
    
    private val maxHistorySize = 100 // Keep last 100 scans
    
    /**
     * Save both encrypted and decrypted scan data
     */
    suspend fun saveScan(encryptedScan: EncryptedScanData, decryptedScan: DecryptedScanData) {
        saveEncryptedScan(encryptedScan)
        saveDecryptedScan(decryptedScan)
    }
    
    /**
     * Save encrypted scan data
     */
    suspend fun saveEncryptedScan(encryptedScan: EncryptedScanData) = withContext(Dispatchers.IO) {
        val scans = getAllEncryptedScans().toMutableList()
        
        // Add new scan with current timestamp if not set
        val scanWithTimestamp = if (encryptedScan.timestamp == LocalDateTime.MIN) {
            encryptedScan.copy(timestamp = LocalDateTime.now())
        } else {
            encryptedScan
        }
        
        scans.add(0, scanWithTimestamp) // Add to beginning (most recent first)
        
        // Keep only the most recent scans
        if (scans.size > maxHistorySize) {
            scans.subList(maxHistorySize, scans.size).clear()
        }
        
        // Save to SharedPreferences
        val scansJson = gson.toJson(scans)
        sharedPreferences.edit()
            .putString("encrypted_scans", scansJson)
            .apply()
    }
    
    /**
     * Save decrypted scan data
     */
    suspend fun saveDecryptedScan(decryptedScan: DecryptedScanData) = withContext(Dispatchers.IO) {
        val scans = getAllDecryptedScans().toMutableList()
        
        // Add new scan with current timestamp if not set
        val scanWithTimestamp = if (decryptedScan.timestamp == LocalDateTime.MIN) {
            decryptedScan.copy(timestamp = LocalDateTime.now())
        } else {
            decryptedScan
        }
        
        scans.add(0, scanWithTimestamp) // Add to beginning (most recent first)
        
        // Keep only the most recent scans
        if (scans.size > maxHistorySize) {
            scans.subList(maxHistorySize, scans.size).clear()
        }
        
        // Save to SharedPreferences
        val scansJson = gson.toJson(scans)
        sharedPreferences.edit()
            .putString("decrypted_scans", scansJson)
            .apply()
    }
    
    /**
     * Get all encrypted scan data
     */
    fun getAllEncryptedScans(): List<EncryptedScanData> {
        val scansJson = sharedPreferences.getString("encrypted_scans", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<EncryptedScanData>>() {}.type
            gson.fromJson(scansJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, return empty list and clear storage
            sharedPreferences.edit()
                .remove("encrypted_scans")
                .apply()
            emptyList()
        }
    }
    
    /**
     * Get all decrypted scan data
     */
    fun getAllDecryptedScans(): List<DecryptedScanData> {
        val scansJson = sharedPreferences.getString("decrypted_scans", null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<List<DecryptedScanData>>() {}.type
            gson.fromJson(scansJson, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            // If data is corrupted, return empty list and clear storage
            sharedPreferences.edit()
                .remove("decrypted_scans")
                .apply()
            emptyList()
        }
    }
    
    /**
     * Get decrypted scans by result type
     */
    fun getDecryptedScansByResult(result: ScanResult): List<DecryptedScanData> {
        return getAllDecryptedScans().filter { it.scanResult == result }
    }
    
    /**
     * Get successful decrypted scans
     */
    fun getSuccessfulDecryptedScans(): List<DecryptedScanData> {
        return getDecryptedScansByResult(ScanResult.SUCCESS)
    }
    
    /**
     * Get failed decrypted scans
     */
    fun getFailedDecryptedScans(): List<DecryptedScanData> {
        return getAllDecryptedScans().filter { it.scanResult != ScanResult.SUCCESS }
    }
    
    /**
     * Get decrypted scans by tag UID
     */
    fun getDecryptedScansByTagUid(tagUid: String): List<DecryptedScanData> {
        return getAllDecryptedScans().filter { it.tagUid == tagUid }
    }
    
    /**
     * Get encrypted scan by tag UID
     */
    fun getEncryptedScansByTagUid(tagUid: String): List<EncryptedScanData> {
        return getAllEncryptedScans().filter { it.tagUid == tagUid }
    }
    
    /**
     * Clear all history
     */
    suspend fun clearHistory() {
        clearEncryptedHistory()
        clearDecryptedHistory()
    }
    
    /**
     * Clear encrypted history
     */
    suspend fun clearEncryptedHistory() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove("encrypted_scans")
            .apply()
    }
    
    /**
     * Clear decrypted history
     */
    suspend fun clearDecryptedHistory() = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .remove("decrypted_scans")
            .apply()
    }
    
    /**
     * Get history count
     */
    fun getHistoryCount(): Int {
        return getAllDecryptedScans().size
    }
    
    /**
     * Get success rate
     */
    fun getSuccessRate(): Float {
        val allScans = getAllDecryptedScans()
        if (allScans.isEmpty()) return 0f
        
        val successfulScans = allScans.count { it.scanResult == ScanResult.SUCCESS }
        return successfulScans.toFloat() / allScans.size
    }
    
    /**
     * Get scans grouped by tag UID
     */
    fun getScansGroupedByTagUid(): Map<String, List<DecryptedScanData>> {
        return getAllDecryptedScans().groupBy { it.tagUid }
    }
    
    /**
     * Get encrypted scan data for a decrypted scan by matching timestamp and UID
     */
    fun getEncryptedScanForDecrypted(decryptedScan: DecryptedScanData): EncryptedScanData? {
        val encryptedScans = getAllEncryptedScans()
        return encryptedScans.find { encrypted ->
            encrypted.tagUid == decryptedScan.tagUid && 
            encrypted.timestamp == decryptedScan.timestamp
        }
    }
    
    /**
     * Get count of unique tags scanned
     */
    fun getUniqueTagCount(): Int {
        return getAllDecryptedScans().map { it.tagUid }.distinct().size
    }
    
    /**
     * Get summary statistics for a specific tag UID
     */
    fun getTagStatistics(tagUid: String): TagStatistics {
        val scans = getDecryptedScansByTagUid(tagUid)
        val successfulScans = scans.count { it.scanResult == ScanResult.SUCCESS }
        val successRate = if (scans.isNotEmpty()) successfulScans.toFloat() / scans.size else 0f
        val latestScan = scans.maxByOrNull { it.timestamp }
        val firstScan = scans.minByOrNull { it.timestamp }
        
        return TagStatistics(
            tagUid = tagUid,
            totalScans = scans.size,
            successfulScans = successfulScans,
            successRate = successRate,
            latestScanTimestamp = latestScan?.timestamp,
            firstScanTimestamp = firstScan?.timestamp
        )
    }
    
    /**
     * Refresh the FilamentInterpreter with updated mappings
     */
    fun refreshMappings() {
        interpreterFactory.refreshMappings()
    }
    
    /**
     * Helper method to interpret DecryptedScanData to FilamentInfo
     */
    private fun interpretScanData(decryptedData: DecryptedScanData): com.bscan.model.FilamentInfo? {
        return if (decryptedData.scanResult == ScanResult.SUCCESS) {
            try {
                val result = interpreterFactory.interpret(decryptedData)
                if (result == null) {
                    android.util.Log.w("ScanHistoryRepository", "Failed to interpret tag UID: ${decryptedData.tagUid}")
                } else {
                    android.util.Log.d("ScanHistoryRepository", "Successfully interpreted tag UID: ${decryptedData.tagUid} -> ${result.filamentType}")
                }
                result
            } catch (e: Exception) {
                android.util.Log.e("ScanHistoryRepository", "Exception interpreting tag UID: ${decryptedData.tagUid}", e)
                null
            }
        } else {
            android.util.Log.d("ScanHistoryRepository", "Skipping interpretation for failed scan: ${decryptedData.tagUid}")
            null
        }
    }
}

/**
 * Statistics for a specific tag UID
 */
data class TagStatistics(
    val tagUid: String,
    val totalScans: Int,
    val successfulScans: Int,
    val successRate: Float,
    val latestScanTimestamp: LocalDateTime?,
    val firstScanTimestamp: LocalDateTime?
)
