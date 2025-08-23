package com.bscan.repository

import android.content.Context
import android.net.Uri
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ExportData(
    val version: Int = 1,
    val exportDate: LocalDateTime,
    val deviceInfo: DeviceInfo,
    val encryptedScans: List<EncryptedScanData>,
    val decryptedScans: List<DecryptedScanData>,
    val statistics: ExportStatistics
)

data class DeviceInfo(
    val appVersion: String,
    val exportedBy: String = "B-Scan"
)

data class ExportStatistics(
    val totalScans: Int,
    val uniqueSpools: Int,
    val dateRange: DateRange
)

data class DateRange(
    val from: LocalDateTime?,
    val to: LocalDateTime?
)

data class ImportResult(
    val success: Boolean,
    val message: String,
    val importedEncryptedScans: Int = 0,
    val importedDecryptedScans: Int = 0,
    val skippedScans: Int = 0
)

enum class ExportScope {
    ALL_DATA,
    DATE_RANGE,
    SELECTED_SPOOLS
}

enum class ImportMode {
    MERGE_WITH_EXISTING,
    REPLACE_ALL
}

class DataExportManager(private val context: Context) {
    
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
                null
            }
        }
    }
    
    // Custom ByteArray adapter for Gson
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
        .setPrettyPrinting()
        .create()
    
    /**
     * Export scan data based on the specified scope
     */
    suspend fun exportData(
        repository: ScanHistoryRepository,
        uri: Uri,
        scope: ExportScope,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null,
        selectedTrayUids: List<String> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get data based on scope
            val (encryptedScans, decryptedScans) = when (scope) {
                ExportScope.ALL_DATA -> {
                    Pair(repository.getAllEncryptedScans(), repository.getAllDecryptedScans())
                }
                ExportScope.DATE_RANGE -> {
                    val encrypted = repository.getAllEncryptedScans().filter { scan ->
                        (fromDate == null || scan.timestamp >= fromDate) &&
                        (toDate == null || scan.timestamp <= toDate)
                    }
                    val decrypted = repository.getAllDecryptedScans().filter { scan ->
                        (fromDate == null || scan.timestamp >= fromDate) &&
                        (toDate == null || scan.timestamp <= toDate)
                    }
                    Pair(encrypted, decrypted)
                }
                ExportScope.SELECTED_SPOOLS -> {
                    // Filter by tray UIDs - need to match against both tag UID and potential tray UID
                    val encrypted = repository.getAllEncryptedScans().filter { scan ->
                        selectedTrayUids.contains(scan.tagUid)
                    }
                    val decrypted = repository.getAllDecryptedScans().filter { scan ->
                        selectedTrayUids.contains(scan.tagUid)
                    }
                    Pair(encrypted, decrypted)
                }
            }
            
            // Calculate statistics
            val uniqueSpools = decryptedScans
                .mapNotNull { scan -> 
                    // Try to extract tray UID from decrypted blocks (block 9)
                    scan.decryptedBlocks[9]?.substring(0, 32) // First 16 bytes as hex
                }
                .distinct()
                .size
            
            // Calculate date range from all scans
            val allTimestamps = encryptedScans.map { it.timestamp } + decryptedScans.map { it.timestamp }
            val dateRange = DateRange(
                from = allTimestamps.minOrNull(),
                to = allTimestamps.maxOrNull()
            )
            
            // Get app version from package manager
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val appVersion = packageInfo.versionName ?: "Unknown"
            
            // Create export data
            val exportData = ExportData(
                exportDate = LocalDateTime.now(),
                deviceInfo = DeviceInfo(appVersion = appVersion),
                encryptedScans = encryptedScans,
                decryptedScans = decryptedScans,
                statistics = ExportStatistics(
                    totalScans = encryptedScans.size + decryptedScans.size,
                    uniqueSpools = uniqueSpools,
                    dateRange = dateRange
                )
            )
            
            // Write to file
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                val jsonString = gson.toJson(exportData)
                outputStream.write(jsonString.toByteArray())
            } ?: throw IOException("Could not open output stream")
            
            Result.success("Successfully exported ${exportData.statistics.totalScans} scans from ${exportData.statistics.uniqueSpools} spools")
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import scan data from a JSON file
     */
    suspend fun importData(
        repository: ScanHistoryRepository,
        uri: Uri,
        mode: ImportMode
    ): Result<ImportResult> = withContext(Dispatchers.IO) {
        try {
            // Read and parse the file
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Could not read input stream")
            
            val exportData = gson.fromJson(jsonString, ExportData::class.java)
                ?: throw IllegalArgumentException("Invalid JSON format")
            
            // Validate version compatibility
            if (exportData.version > 1) {
                return@withContext Result.failure(
                    IllegalArgumentException("Export file version ${exportData.version} is not supported. Please update the app.")
                )
            }
            
            // Get existing data for deduplication
            val existingEncryptedIds = if (mode == ImportMode.MERGE_WITH_EXISTING) {
                repository.getAllEncryptedScans().map { it.id }.toSet()
            } else {
                emptySet()
            }
            
            val existingDecryptedIds = if (mode == ImportMode.MERGE_WITH_EXISTING) {
                repository.getAllDecryptedScans().map { it.id }.toSet()
            } else {
                emptySet()
            }
            
            // Filter out duplicates
            val newEncryptedScans = exportData.encryptedScans.filter { it.id !in existingEncryptedIds }
            val newDecryptedScans = exportData.decryptedScans.filter { it.id !in existingDecryptedIds }
            
            val skippedCount = (exportData.encryptedScans.size - newEncryptedScans.size) + 
                              (exportData.decryptedScans.size - newDecryptedScans.size)
            
            // Clear existing data if replace mode
            if (mode == ImportMode.REPLACE_ALL) {
                repository.clearHistory()
            }
            
            // Import new data
            newEncryptedScans.forEach { scan ->
                repository.saveEncryptedScan(scan)
            }
            
            newDecryptedScans.forEach { scan ->
                repository.saveDecryptedScan(scan)
            }
            
            val result = ImportResult(
                success = true,
                message = "Successfully imported ${newEncryptedScans.size + newDecryptedScans.size} scans" +
                         if (skippedCount > 0) " (skipped $skippedCount duplicates)" else "",
                importedEncryptedScans = newEncryptedScans.size,
                importedDecryptedScans = newDecryptedScans.size,
                skippedScans = skippedCount
            )
            
            Result.success(result)
            
        } catch (e: Exception) {
            Result.success(
                ImportResult(
                    success = false,
                    message = "Import failed: ${e.message}"
                )
            )
        }
    }
    
    /**
     * Validate an export file without importing it
     */
    suspend fun validateImportFile(uri: Uri): Result<ExportData> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: throw IOException("Could not read file")
            
            val exportData = gson.fromJson(jsonString, ExportData::class.java)
                ?: throw IllegalArgumentException("Invalid JSON format")
            
            // Basic validation
            if (exportData.version > 1) {
                throw IllegalArgumentException("Unsupported file version: ${exportData.version}")
            }
            
            Result.success(exportData)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate a filename for export
     */
    fun generateExportFilename(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = LocalDateTime.now().format(formatter)
        return "bscan_export_$timestamp.json"
    }
}