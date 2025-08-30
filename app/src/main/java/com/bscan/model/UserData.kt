package com.bscan.model

import java.time.LocalDateTime
import com.bscan.ui.screens.home.CatalogDisplayMode
import com.bscan.ui.components.MaterialDisplaySettings

/**
 * Runtime user data that extends the catalog data.
 * Contains user's inventory, scans, custom mappings, and preferences.
 * This data is read-write and stored in SharedPreferences.
 */
data class UserData(
    val version: Int,
    val components: Map<String, Component>,
    val inventoryItems: Map<String, InventoryItem>,
    val scans: ScanDataContainer,
    val measurements: List<MassMeasurement>,
    val customMappings: CustomMappings,
    val preferences: UserPreferences,
    val metadata: UserDataMetadata = UserDataMetadata()
)

/**
 * Container for all scan data (encrypted and decrypted)
 */
data class ScanDataContainer(
    val encryptedScans: Map<String, EncryptedScanData>,
    val decryptedScans: Map<String, DecryptedScanData>
)

/**
 * User's custom mappings that extend or override catalog data
 */
data class CustomMappings(
    val manufacturers: Map<String, CustomManufacturer>,
    val rfidOverrides: Map<String, CustomRfidMapping>
)

/**
 * User-defined manufacturer with custom materials and settings
 */
data class CustomManufacturer(
    val name: String,
    val displayName: String,
    val materials: Map<String, MaterialDefinition>,
    val temperatureProfiles: Map<String, TemperatureProfile>,
    val colorPalette: Map<String, String>,
    val tagFormat: TagFormat = TagFormat.USER_DEFINED,
    val notes: String = ""
)

/**
 * User-defined RFID mapping for custom or unknown tags
 */
data class CustomRfidMapping(
    val tagUid: String,
    val manufacturer: String,
    val material: String,
    val color: String,
    val hex: String?,
    val sku: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * User preferences and application settings
 */
data class UserPreferences(
    val defaultManufacturer: String = "bambu",
    val weightUnit: WeightUnit = WeightUnit.GRAMS,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val theme: AppTheme = AppTheme.AUTO,
    val enableNotifications: Boolean = true,
    val enableAnalytics: Boolean = false,
    val enableDebugLogging: Boolean = false,
    val autoSetupComponents: Boolean = true,
    val defaultExportFormat: String = "JSON",
    val catalogDisplayMode: CatalogDisplayMode = CatalogDisplayMode.COMPLETE_TITLE,
    val materialDisplaySettings: MaterialDisplaySettings = MaterialDisplaySettings.DEFAULT,
    val lastBackupDate: LocalDateTime? = null
)

/**
 * Weight units for mass display
 */
enum class WeightUnit {
    GRAMS,
    OUNCES,
    POUNDS
}

/**
 * Temperature units for display
 */
enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

/**
 * Application theme options
 */
enum class AppTheme {
    LIGHT,
    DARK,
    WHITE,
    BLACK,
    AUTO
}

/**
 * Metadata about the user data
 */
data class UserDataMetadata(
    val lastModified: LocalDateTime = LocalDateTime.now(),
    val appVersion: String = "1.0.0",
    val totalScans: Int = 0,
    val totalComponents: Int = 0,
    val totalInventoryItems: Int = 0
)