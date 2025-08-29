package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.net.Uri
import com.bscan.model.*
import com.bscan.interpreter.InterpreterFactory
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for managing runtime user data.
 * This data is read-write and stored in SharedPreferences.
 * Contains user's inventory, scans, custom mappings, and preferences.
 */
class UserDataRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_data", Context.MODE_PRIVATE)
    
    // In-memory cache of user data
    private var cachedUserData: UserData? = null
    
    // Reactive state flow for user data changes
    private val _userDataFlow = MutableStateFlow<UserData?>(null)
    val userDataFlow: StateFlow<UserData?> = _userDataFlow.asStateFlow()
    
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
    
    // Custom ByteArray adapter for Gson (for encrypted scan data)
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
    
    companion object {
        private const val TAG = "UserDataRepository"
        private const val USER_DATA_KEY = "user_data_v1"
    }
    
    /**
     * Get the current user data. Loads from storage on first access.
     */
    fun getUserData(): UserData {
        if (cachedUserData == null) {
            cachedUserData = loadFromPreferences()
            _userDataFlow.value = cachedUserData
        }
        return cachedUserData!!
    }
    
    /**
     * Update user data using a transform function. Changes are persisted immediately.
     */
    fun updateUserData(transform: (UserData) -> UserData) {
        val currentData = getUserData()
        val newData = transform(currentData).copy(
            metadata = currentData.metadata.copy(
                lastModified = LocalDateTime.now(),
                totalScans = countScans(currentData),
                totalComponents = currentData.components.size,
                totalInventoryItems = currentData.inventoryItems.size
            )
        )
        cachedUserData = newData
        saveToPreferences(newData)
        _userDataFlow.value = newData
    }
    
    /**
     * Save a physical component
     */
    fun saveComponent(component: PhysicalComponent) {
        updateUserData { userData ->
            userData.copy(
                components = userData.components + (component.id to component)
            )
        }
    }
    
    /**
     * Get a physical component by ID
     */
    fun getComponent(componentId: String): PhysicalComponent? {
        return getUserData().components[componentId]
    }
    
    /**
     * Get all physical components
     */
    fun getComponents(): Map<String, PhysicalComponent> {
        return getUserData().components
    }
    
    /**
     * Save an inventory item
     */
    fun saveInventoryItem(item: InventoryItem) {
        updateUserData { userData ->
            userData.copy(
                inventoryItems = userData.inventoryItems + (item.trayUid to item)
            )
        }
    }
    
    /**
     * Get an inventory item by tray UID
     */
    fun getInventoryItem(trayUid: String): InventoryItem? {
        return getUserData().inventoryItems[trayUid]
    }
    
    /**
     * Get all inventory items
     */
    fun getInventoryItems(): Map<String, InventoryItem> {
        return getUserData().inventoryItems
    }
    
    /**
     * Remove an inventory item
     */
    fun removeInventoryItem(trayUid: String) {
        updateUserData { userData ->
            userData.copy(
                inventoryItems = userData.inventoryItems - trayUid
            )
        }
    }
    
    /**
     * Remove a physical component
     */
    fun removeComponent(componentId: String) {
        updateUserData { userData ->
            userData.copy(
                components = userData.components - componentId
            )
        }
    }
    
    /**
     * Record a new scan (both encrypted and decrypted data)
     */
    fun recordScan(encryptedData: EncryptedScanData, decryptedData: DecryptedScanData) {
        updateUserData { userData ->
            userData.copy(
                scans = userData.scans.copy(
                    encryptedScans = userData.scans.encryptedScans + (encryptedData.id.toString() to encryptedData),
                    decryptedScans = userData.scans.decryptedScans + (decryptedData.id.toString() to decryptedData)
                )
            )
        }
    }
    
    /**
     * Add a mass measurement
     */
    fun addMeasurement(measurement: MassMeasurement) {
        updateUserData { userData ->
            userData.copy(
                measurements = userData.measurements + measurement
            )
        }
    }
    
    /**
     * Add a custom RFID mapping
     */
    fun addCustomRfidMapping(mapping: CustomRfidMapping) {
        updateUserData { userData ->
            userData.copy(
                customMappings = userData.customMappings.copy(
                    rfidOverrides = userData.customMappings.rfidOverrides + (mapping.tagUid to mapping)
                )
            )
        }
    }
    
    /**
     * Add or update a custom manufacturer
     */
    fun saveCustomManufacturer(manufacturerId: String, manufacturer: CustomManufacturer) {
        updateUserData { userData ->
            userData.copy(
                customMappings = userData.customMappings.copy(
                    manufacturers = userData.customMappings.manufacturers + (manufacturerId to manufacturer)
                )
            )
        }
    }
    
    /**
     * Update user preferences
     */
    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        updateUserData { userData ->
            userData.copy(preferences = transform(userData.preferences))
        }
    }
    
    /**
     * Find custom RFID mapping
     */
    fun findCustomRfidMapping(tagUid: String): CustomRfidMapping? {
        return getUserData().customMappings.rfidOverrides[tagUid]
    }
    
    // === Interpreted Scan Methods ===
    // Direct implementation using the new data structure
    
    private val interpreterFactory by lazy { 
        val catalogRepo = CatalogRepository(context)
        val unifiedDataAccess = UnifiedDataAccess(catalogRepo, this)
        InterpreterFactory(unifiedDataAccess)
    }
    
    /**
     * Get all interpreted scans from the new data structure
     */
    fun getAllInterpretedScans(): List<com.bscan.repository.InterpretedScan> {
        val userData = getUserData()
        val results = mutableListOf<com.bscan.repository.InterpretedScan>()
        
        // Process each encrypted scan
        userData.scans.encryptedScans.forEach { (encryptedId, encryptedScan) ->
            val decryptedScan = userData.scans.decryptedScans[encryptedId]
            
            if (decryptedScan != null) {
                // We have both encrypted and decrypted data
                val filamentInfo = if (decryptedScan.scanResult == ScanResult.SUCCESS) {
                    interpreterFactory.interpret(decryptedScan)
                } else null
                
                results.add(com.bscan.repository.InterpretedScan(encryptedScan, decryptedScan, filamentInfo))
            } else {
                // Only encrypted data (authentication failed completely)
                val syntheticDecrypted = createFailedDecryptedScan(encryptedScan)
                results.add(com.bscan.repository.InterpretedScan(encryptedScan, syntheticDecrypted, null))
            }
        }
        
        return results.sortedByDescending { it.timestamp }
    }
    
    /**
     * Get scans filtered by tag UID from the new data structure
     */
    fun getScansByTagUid(tagUid: String): List<com.bscan.repository.InterpretedScan> {
        return getAllInterpretedScans().filter { it.uid == tagUid }
    }
    
    /**
     * Get detailed information about a filament reel by tray UID or tag UID
     */
    fun getFilamentReelDetails(identifier: String): com.bscan.repository.FilamentReelDetails? {
        Log.d("UserDataRepository", "getFilamentReelDetails called with identifier: '$identifier'")
        val allScans = getAllInterpretedScans()
        Log.d("UserDataRepository", "Total scans available: ${allScans.size}")
        
        // Debug: Show all available trayUids and tag UIDs
        allScans.take(10).forEach { scan ->
            Log.d("UserDataRepository", "Available scan - uid: '${scan.uid}', trayUid: '${scan.filamentInfo?.trayUid}', tagUid: '${scan.filamentInfo?.tagUid}', colorName: '${scan.filamentInfo?.colorName}'")
        }
        
        // Strategy 1: Look for scans with matching tray UID (proper hierarchical approach)
        var matchingScans = allScans.filter { 
            it.filamentInfo?.trayUid == identifier 
        }
        Log.d("UserDataRepository", "Strategy 1 (tray UID match): Found ${matchingScans.size} scans")
        
        // Strategy 2: If none found, try tag UID (for individual tags without tray info)
        if (matchingScans.isEmpty()) {
            matchingScans = allScans.filter { it.uid == identifier }
            Log.d("UserDataRepository", "Strategy 2 (tag UID match): Found ${matchingScans.size} scans")
        }
        
        // Strategy 3: If still nothing, search by the displayed identifier (which might be tray UID shown in UI)
        if (matchingScans.isEmpty()) {
            // The identifier might be what's displayed in the UI (like "6E6C3E04B77948F")
            // Check if any FilamentInfo has this as a tag UID but we need to find all related scans
            val allScansForThisTag = allScans.filter { scan ->
                scan.filamentInfo?.tagUid == identifier || scan.uid == identifier
            }
            Log.d("UserDataRepository", "Strategy 3 (tagUid/uid fuzzy match): Found ${allScansForThisTag.size} scans")
            if (allScansForThisTag.isNotEmpty()) {
                // Found scans by tag UID - now get all scans for the same tray
                val trayUid = allScansForThisTag.first().filamentInfo?.trayUid
                Log.d("UserDataRepository", "Strategy 3 found trayUid: '$trayUid' from first matching scan")
                if (trayUid != null && trayUid.isNotEmpty()) {
                    matchingScans = allScans.filter { it.filamentInfo?.trayUid == trayUid }
                    Log.d("UserDataRepository", "Strategy 3 expanded to ${matchingScans.size} scans with same trayUid")
                } else {
                    // No tray UID available, just use the individual tag scans
                    matchingScans = allScansForThisTag
                    Log.d("UserDataRepository", "Strategy 3 using individual tag scans: ${matchingScans.size}")
                }
            }
        }
        
        if (matchingScans.isEmpty()) {
            Log.w("UserDataRepository", "No scans found for identifier '$identifier'. Searched ${allScans.size} total scans.")
            return null
        }
        
        Log.d("UserDataRepository", "Found ${matchingScans.size} matching scans for identifier '$identifier'")
        
        // Get most recent successful scan for filament info
        // First try successful scans, then fallback to any scan if no successful ones exist
        val mostRecentSuccess = matchingScans
            .filter { it.scanResult == ScanResult.SUCCESS }
            .maxByOrNull { it.timestamp }
            ?: matchingScans.maxByOrNull { it.timestamp }
            ?: return null
        
        val filamentInfo = mostRecentSuccess.filamentInfo
            ?: createUnknownTagFilamentInfo(mostRecentSuccess)
        
        val tagUids = matchingScans
            .map { it.filamentInfo?.tagUid ?: it.uid }
            .distinct()
        
        val scansByTag = matchingScans.groupBy { it.filamentInfo?.tagUid ?: it.uid }
        
        return com.bscan.repository.FilamentReelDetails(
            trayUid = filamentInfo.trayUid,
            filamentInfo = filamentInfo,
            tagUids = tagUids,
            allScans = matchingScans.sortedByDescending { it.timestamp },
            scansByTag = scansByTag,
            totalScans = matchingScans.size,
            successfulScans = matchingScans.count { it.scanResult == ScanResult.SUCCESS },
            lastScanned = matchingScans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
        )
    }
    
    /**
     * Create synthetic DecryptedScanData for failed authentication
     */
    private fun createFailedDecryptedScan(encryptedScan: EncryptedScanData): DecryptedScanData {
        return DecryptedScanData(
            id = encryptedScan.id + 1000000,
            timestamp = encryptedScan.timestamp,
            tagUid = encryptedScan.tagUid,
            technology = encryptedScan.technology,
            scanResult = ScanResult.AUTHENTICATION_FAILED,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = emptyList(),
            failedSectors = (0..15).toList(),
            usedKeys = emptyMap(),
            derivedKeys = emptyList(),
            keyDerivationTimeMs = 0,
            authenticationTimeMs = 0,
            errors = listOf("Complete authentication failure")
        )
    }
    
    /**
     * Create placeholder FilamentInfo for unknown tags
     */
    private fun createUnknownTagFilamentInfo(scan: com.bscan.repository.InterpretedScan): FilamentInfo {
        return FilamentInfo(
            tagUid = scan.uid,
            trayUid = scan.uid,
            filamentType = "Unknown",
            detailedFilamentType = "Unknown Tag",
            colorHex = "#808080",
            colorName = "Unknown",
            spoolWeight = 0,
            filamentDiameter = 1.75f,
            filamentLength = 0,
            productionDate = "Unknown",
            minTemperature = 0,
            maxTemperature = 0,
            bedTemperature = 0,
            dryingTemperature = 0,
            dryingTime = 0
        )
    }
    
    /**
     * Export entire user state to a file
     */
    suspend fun exportToFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val data = getUserData()
            val json = gson.toJson(data)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            
            Log.i(TAG, "Exported user data to file")
            Result.success("Export completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export user data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import user state from a file, completely replacing current data
     */
    suspend fun importFromFile(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            } ?: throw IOException("Could not read file")
            
            val importedData = gson.fromJson(json, UserData::class.java)
                ?: throw IllegalArgumentException("Invalid user data format")
            
            // Replace all data
            cachedUserData = importedData.copy(
                metadata = importedData.metadata.copy(lastModified = LocalDateTime.now())
            )
            saveToPreferences(cachedUserData!!)
            
            Log.i(TAG, "Imported user data from file")
            Result.success("Import completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import user data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Force reload data from storage (useful for testing or after imports)
     */
    fun reloadData() {
        cachedUserData = null
    }
    
    /**
     * Clear all user data (reset to defaults)
     */
    fun clearAllData() {
        cachedUserData = createDefaultUserData()
        saveToPreferences(cachedUserData!!)
    }
    
    /**
     * Get statistics about current user data
     */
    fun getDataStatistics(): UserDataStatistics {
        val userData = getUserData()
        return UserDataStatistics(
            totalComponents = userData.components.size,
            totalInventoryItems = userData.inventoryItems.size,
            totalScans = countScans(userData),
            totalMeasurements = userData.measurements.size,
            customManufacturers = userData.customMappings.manufacturers.size,
            customRfidMappings = userData.customMappings.rfidOverrides.size,
            lastModified = userData.metadata.lastModified
        )
    }
    
    private fun loadFromPreferences(): UserData {
        val savedJson = sharedPreferences.getString(USER_DATA_KEY, null)
        
        return if (savedJson != null) {
            try {
                Log.d(TAG, "Loading user data from SharedPreferences")
                gson.fromJson(savedJson, UserData::class.java) ?: createDefaultUserData()
            } catch (e: JsonSyntaxException) {
                Log.w(TAG, "Corrupted user data in SharedPreferences, using defaults", e)
                createDefaultUserData()
            }
        } else {
            Log.d(TAG, "No saved user data found, creating defaults")
            val defaultData = createDefaultUserData()
            saveToPreferences(defaultData)
            defaultData
        }
    }
    
    private fun saveToPreferences(userData: UserData) {
        try {
            val json = gson.toJson(userData)
            sharedPreferences.edit()
                .putString(USER_DATA_KEY, json)
                .apply()
            
            Log.d(TAG, "Saved user data to SharedPreferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user data", e)
        }
    }
    
    private fun createDefaultUserData(): UserData {
        Log.i(TAG, "Creating default user data")
        return UserData(
            version = 1,
            components = emptyMap(),
            inventoryItems = emptyMap(),
            scans = ScanDataContainer(
                encryptedScans = emptyMap(),
                decryptedScans = emptyMap()
            ),
            measurements = emptyList(),
            customMappings = CustomMappings(
                manufacturers = emptyMap(),
                rfidOverrides = emptyMap()
            ),
            preferences = UserPreferences(),
            metadata = UserDataMetadata(
                lastModified = LocalDateTime.now(),
                appVersion = "1.0.0"
            )
        )
    }
    
    /**
     * Clear all user data (for testing purposes)
     */
    fun clearUserData() {
        cachedUserData = null
        sharedPreferences.edit().clear().apply()
    }
    
    private fun countScans(userData: UserData): Int {
        return userData.scans.encryptedScans.size
    }
}

/**
 * Statistics about the current user data
 */
data class UserDataStatistics(
    val totalComponents: Int,
    val totalInventoryItems: Int,
    val totalScans: Int,
    val totalMeasurements: Int,
    val customManufacturers: Int,
    val customRfidMappings: Int,
    val lastModified: LocalDateTime
)

