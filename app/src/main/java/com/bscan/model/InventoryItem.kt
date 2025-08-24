package com.bscan.model

import java.time.LocalDateTime

/**
 * Represents an inventory item that links a FilamentReel with weight tracking data.
 * 
 * This combines the logical filament reel with physical weight measurements,
 * enabling calculation of remaining filament based on spool configuration and
 * measured weights over time.
 */
data class InventoryItem(
    val trayUid: String,                        // Links to FilamentReel
    val currentConfigurationId: String?,        // Current spool configuration (null if not set)
    val expectedFilamentWeightGrams: Float?,    // Expected weight when new (null if unknown)
    val measurements: List<FilamentWeightMeasurement>, // Weight history
    val lastUpdated: LocalDateTime,             // When inventory was last modified
    val notes: String = ""                      // Optional user notes
) {
    /**
     * Get the most recent weight measurement
     */
    val latestMeasurement: FilamentWeightMeasurement? 
        get() = measurements.maxByOrNull { it.measuredAt }
    
    /**
     * Check if inventory item has an active spool configuration
     */
    val hasSpoolConfiguration: Boolean
        get() = currentConfigurationId != null
        
    /**
     * Check if we have weight measurements for this item
     */
    val hasWeightMeasurements: Boolean
        get() = measurements.isNotEmpty()
}

/**
 * Calculated status of filament remaining based on measurements and configuration
 */
data class FilamentStatus(
    val remainingWeightGrams: Float,           // Calculated remaining filament weight
    val remainingPercentage: Float,            // Percentage remaining (0.0 - 1.0)
    val consumedWeightGrams: Float,            // Amount consumed since initial measurement
    val lastMeasurement: FilamentWeightMeasurement?, // Most recent measurement
    val spoolConfiguration: SpoolConfiguration?, // Configuration used for calculations
    val calculationSuccess: Boolean,           // Whether calculation was successful
    val errorMessage: String? = null           // Error if calculation failed
) {
    /**
     * Check if filament is running low (less than 20%)
     */
    val isRunningLow: Boolean
        get() = calculationSuccess && remainingPercentage < 0.2f
    
    /**
     * Check if filament is nearly empty (less than 5%)
     */
    val isNearlyEmpty: Boolean
        get() = calculationSuccess && remainingPercentage < 0.05f
        
    /**
     * Get formatted remaining weight string
     */
    fun getFormattedRemainingWeight(unit: com.bscan.logic.WeightUnit): String {
        return if (calculationSuccess) {
            com.bscan.logic.WeightCalculationService().formatWeight(remainingWeightGrams, unit)
        } else {
            "Unknown"
        }
    }
}

/**
 * Request to calculate filament status for an inventory item
 */
data class FilamentStatusRequest(
    val inventoryItem: InventoryItem,
    val spoolConfiguration: SpoolConfiguration?,
    val components: List<SpoolComponent>
)