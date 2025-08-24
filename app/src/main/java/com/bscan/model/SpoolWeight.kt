package com.bscan.model

import java.time.LocalDateTime

/**
 * Represents an individual component that contributes to spool weight.
 * Components can be combined to create complete spool configurations.
 */
data class SpoolComponent(
    val id: String,                     // Unique identifier for this component
    val name: String,                   // Display name (e.g., "Bambu Spool", "Cardboard Core")
    val type: SpoolComponentType,       // Type of component
    val weightGrams: Float,             // Weight in grams
    val manufacturer: String = "Bambu Lab",
    val description: String = "",       // Optional description
    val isUserDefined: Boolean = false, // True for user-created components
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of spool components
 */
enum class SpoolComponentType {
    BASE_SPOOL,     // The main spool body (plastic reel)
    CORE_RING,      // Cardboard or plastic core ring
    ADAPTER,        // Conversion adapter between spool types
    PACKAGING       // Bags, boxes, or other packaging
}

/**
 * Represents a complete spool configuration made up of multiple components.
 * This defines what components are included in a specific spool setup.
 */
data class SpoolConfiguration(
    val id: String,                     // Unique identifier
    val name: String,                   // Display name (e.g., "Bambu Refill", "Sunlu v2 + Adapter")
    val components: List<String>,       // List of component IDs
    val totalWeightGrams: Float,        // Calculated total weight
    val packageType: PackageType,       // Type of packaging/configuration
    val isPreset: Boolean = false,      // True for built-in presets
    val isUserDefined: Boolean = false, // True for user-created configs
    val description: String = "",       // Optional description
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of filament packaging/configurations
 */
enum class PackageType {
    BOXED_REFILL,    // Filament + cardboard core in box
    BAGGED_REFILL,   // Filament + cardboard core in bag
    OPEN_REFILL,     // Filament + cardboard core, no packaging
    BOXED_SPOOL,     // Full spool in box
    BAGGED_SPOOL,    // Full spool in bag
    OPEN_SPOOL,      // Full spool, no packaging
    CUSTOM           // User-defined configuration
}

/**
 * Preset spool configurations with default weights for Bambu products.
 * These can be copied and modified by users.
 */
data class SpoolWeightPreset(
    val id: String,                     // Unique identifier
    val name: String,                   // Display name
    val packageType: PackageType,       // Package type
    val configurationId: String,        // Reference to SpoolConfiguration
    val supportedCapacities: List<Float>, // Supported filament weights (0.5, 0.75, 1.0 kg)
    val isFactory: Boolean = true,      // True for built-in presets
    val isModified: Boolean = false,    // True if user has modified a factory preset
    val description: String = "",       // Description
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Records an actual weight measurement for a specific filament reel.
 * Links measured weight to spool configuration for accurate filament calculation.
 */
data class FilamentWeightMeasurement(
    val id: String,                     // Unique identifier
    val trayUid: String,               // Links to FilamentReel
    val measuredWeightGrams: Float,     // Actual measured weight
    val spoolConfigurationId: String,   // Which spool configuration was used
    val measurementType: MeasurementType, // Full vs empty measurement
    val measuredAt: LocalDateTime,      // When measurement was taken
    val notes: String = "",             // Optional user notes
    val isVerified: Boolean = false     // User verification flag
) {
    /**
     * Calculate actual filament weight based on spool configuration
     */
    fun calculateFilamentWeight(spoolWeightGrams: Float): Float {
        return when (measurementType) {
            MeasurementType.FULL_WEIGHT -> measuredWeightGrams - spoolWeightGrams
            MeasurementType.EMPTY_WEIGHT -> 0f // Empty measurement, no filament
        }
    }
}

/**
 * Type of weight measurement
 */
enum class MeasurementType {
    FULL_WEIGHT,    // Weight with filament
    EMPTY_WEIGHT    // Weight without filament (dry spool only)
}

/**
 * Result of weight calculation operations
 */
data class WeightCalculationResult(
    val success: Boolean,
    val filamentWeightGrams: Float = 0f,
    val spoolWeightGrams: Float = 0f,
    val totalWeightGrams: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Container for weight inference operations
 */
data class WeightInferenceRequest(
    val knownTotalWeight: Float,        // Measured total weight
    val knownFilamentWeight: Float?,    // Expected filament weight (if known)
    val knownComponents: List<String>,  // Known component IDs
    val unknownComponentId: String      // Component to infer weight for
)