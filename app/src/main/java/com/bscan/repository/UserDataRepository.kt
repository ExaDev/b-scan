package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.bscan.model.*
import com.google.gson.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    // These methods bridge the gap between the new architecture and the existing ScanHistoryRepository
    // until we fully migrate to the new data structure
    
    private val scanHistoryRepository by lazy { ScanHistoryRepository(context) }
    
    /**
     * Get all interpreted scans using existing ScanHistoryRepository
     * TODO: This is a temporary bridge - replace with direct access to new data structure
     */
    fun getAllInterpretedScans(): List<InterpretedScan> {
        return scanHistoryRepository.getAllScans()
    }
    
    /**
     * Get scans filtered by tag UID using existing ScanHistoryRepository
     * TODO: This is a temporary bridge - replace with direct access to new data structure
     */
    fun getScansByTagUid(tagUid: String): List<InterpretedScan> {
        return scanHistoryRepository.getScansByTagUid(tagUid)
    }
    
    /**
     * Get detailed information about a filament reel by tray UID or tag UID
     * TODO: This is a temporary bridge - replace with direct access to new data structure
     */
    fun getFilamentReelDetails(identifier: String): FilamentReelDetails? {
        return scanHistoryRepository.getFilamentReelDetails(identifier)
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