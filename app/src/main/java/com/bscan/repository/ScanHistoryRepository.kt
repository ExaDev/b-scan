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
        context.getSharedPreferences("scan_history_v2", Context.MODE_PRIVATE)
    
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
    
    
    /**
     * Create a synthetic DecryptedScanData for encrypted scans that failed to decrypt
     */
    private fun createFailedDecryptedScan(encrypted: EncryptedScanData): DecryptedScanData {
        return DecryptedScanData(
            id = encrypted.id + 1000000, // Ensure unique ID
            timestamp = encrypted.timestamp,
            tagUid = encrypted.tagUid,
            technology = encrypted.technology,
            scanResult = ScanResult.AUTHENTICATION_FAILED,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = emptyList(),
            failedSectors = (0..15).toList(), // Assume all sectors failed
            usedKeys = emptyMap(),
            derivedKeys = emptyList(),
            keyDerivationTimeMs = 0,
            authenticationTimeMs = 0,
            errors = listOf("Complete authentication failure - no decrypted data available")
        )
    }
    
    /**
     * Create placeholder FilamentInfo for unknown tags that scan successfully but can't be interpreted
     */
    private fun createUnknownTagFilamentInfo(scan: InterpretedScan): com.bscan.model.FilamentInfo {
        return com.bscan.model.FilamentInfo(
            tagUid = scan.uid,
            trayUid = scan.uid, // Use tag UID as tray UID for unknown tags
            tagFormat = com.bscan.model.TagFormat.UNKNOWN,
            manufacturerName = scan.decryptedData.manufacturerName,
            filamentType = "Unknown",
            detailedFilamentType = "Unknown Tag",
            colorHex = "#808080", // Gray color for unknown
            colorName = "Unknown",
            spoolWeight = 0,
            filamentDiameter = 1.75f, // Default diameter
            filamentLength = 0,
            productionDate = "Unknown",
            minTemperature = 0,
            maxTemperature = 0,
            bedTemperature = 0,
            dryingTemperature = 0,
            dryingTime = 0
        )
    }
    
    
    
    
    
    
}

