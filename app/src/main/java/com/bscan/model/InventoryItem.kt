package com.bscan.model

import java.time.LocalDateTime

/**
 * Represents an inventory item that combines physical components with mass tracking.
 * 
 * This links a FilamentReel with its constituent physical components (filament, core, spool, etc.)
 * and enables precise mass tracking with automatic filament consumption calculation.
 */
data class InventoryItem(
    val trayUid: String,                        // Links to FilamentReel
    val components: List<String>,               // List of PhysicalComponent IDs
    val totalMeasuredMass: Float?,              // User-measured total mass (null if not measured)
    val measurements: List<MassMeasurement>,    // Mass measurement history
    val lastUpdated: LocalDateTime,             // When inventory was last modified
    val notes: String = ""                      // Optional user notes
) {
    /**
     * Get the most recent mass measurement
     */
    val latestMeasurement: MassMeasurement? 
        get() = measurements.maxByOrNull { it.measuredAt }
    
    /**
     * Check if inventory item has any components defined
     */
    val hasComponents: Boolean
        get() = components.isNotEmpty()
        
    /**
     * Check if we have mass measurements for this item
     */
    val hasMassMeasurements: Boolean
        get() = measurements.isNotEmpty()
        
    /**
     * Check if we have a manually measured total mass
     */
    val hasMeasuredTotalMass: Boolean
        get() = totalMeasuredMass != null
        
    /**
     * Create a new measurement and add to history
     */
    fun withNewMeasurement(measurement: MassMeasurement): InventoryItem {
        return copy(
            measurements = measurements + measurement,
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Update the total measured mass
     */
    fun withUpdatedTotalMass(massGrams: Float): InventoryItem {
        return copy(
            totalMeasuredMass = massGrams,
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Add a new component to this inventory item
     */
    fun withComponent(componentId: String): InventoryItem {
        return if (componentId !in components) {
            copy(
                components = components + componentId,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
}

/**
 * Calculated status of filament remaining based on measurements and components
 */
data class FilamentStatus(
    val remainingMassGrams: Float,              // Calculated remaining filament mass
    val remainingPercentage: Float,             // Percentage remaining (0.0 - 1.0)
    val consumedMassGrams: Float,               // Amount consumed since initial measurement
    val lastMeasurement: MassMeasurement?,     // Most recent measurement
    val components: List<Component>,   // Components used for calculations
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
     * Get formatted remaining mass string
     */
    fun getFormattedRemainingMass(unit: com.bscan.logic.WeightUnit): String {
        return if (calculationSuccess) {
            com.bscan.logic.MassCalculationService().formatWeight(remainingMassGrams, unit)
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
    val components: List<Component>
)