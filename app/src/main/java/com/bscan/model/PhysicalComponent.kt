package com.bscan.model

import java.time.LocalDateTime

/**
 * Represents an individual physical component that contributes to inventory mass.
 * Components can be filament (variable mass) or hardware (fixed mass).
 */
data class PhysicalComponent(
    val id: String,                         // Unique identifier for this component
    val name: String,                       // Display name (e.g., "PLA Basic Filament", "Cardboard Core")
    val type: PhysicalComponentType,        // Type of component
    val massGrams: Float,                   // Current/remaining mass in grams
    val fullMassGrams: Float? = null,       // Original/maximum mass (used for variable components)
    val variableMass: Boolean,              // True for filament, false for fixed hardware
    val manufacturer: String = "Unknown",   // Component manufacturer
    val description: String = "",           // Optional description
    val isUserDefined: Boolean = false,     // True for user-created components
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Check if this component can have its mass adjusted
     */
    val canAdjustMass: Boolean
        get() = variableMass
        
    /**
     * Create a copy with updated mass (only for variable mass components)
     */
    fun withUpdatedMass(newMassGrams: Float): PhysicalComponent {
        return if (variableMass) {
            copy(massGrams = newMassGrams)
        } else {
            this // Return unchanged for fixed mass components
        }
    }
    
    /**
     * Create a copy with updated full mass (only for variable mass components)
     */
    fun withUpdatedFullMass(newFullMassGrams: Float): PhysicalComponent {
        return if (variableMass) {
            copy(fullMassGrams = newFullMassGrams)
        } else {
            this // Return unchanged for fixed mass components
        }
    }
    
    /**
     * Set both current and full mass (for initial setup of variable mass components)
     */
    fun withFullMass(fullMass: Float, currentMass: Float = fullMass): PhysicalComponent {
        return if (variableMass) {
            copy(
                massGrams = currentMass,
                fullMassGrams = fullMass
            )
        } else {
            this // Return unchanged for fixed mass components
        }
    }
    
    /**
     * Calculate remaining percentage based on full vs current mass
     */
    fun getRemainingPercentage(): Float? {
        return if (variableMass && fullMassGrams != null && fullMassGrams > 0) {
            maxOf(0f, minOf(1f, massGrams / fullMassGrams))
        } else {
            null // Not applicable for fixed mass or unknown full mass
        }
    }
    
    /**
     * Calculate consumed mass based on full vs current mass
     */
    fun getConsumedMass(): Float? {
        return if (variableMass && fullMassGrams != null) {
            maxOf(0f, fullMassGrams - massGrams)
        } else {
            null // Not applicable for fixed mass or unknown full mass
        }
    }
    
    /**
     * Check if component is running low (less than 20% remaining)
     */
    val isRunningLow: Boolean
        get() = getRemainingPercentage()?.let { it < 0.2f } ?: false
        
    /**
     * Check if component is nearly empty (less than 5% remaining)
     */
    val isNearlyEmpty: Boolean
        get() = getRemainingPercentage()?.let { it < 0.05f } ?: false
}

/**
 * Types of physical components in the inventory system
 */
enum class PhysicalComponentType {
    FILAMENT,       // The actual filament material (variable mass)
    BASE_SPOOL,     // The main spool body (plastic reel) - fixed mass
    CORE_RING,      // Cardboard or plastic core ring - fixed mass
    ADAPTER,        // Conversion adapter between spool types - fixed mass
    PACKAGING       // Bags, boxes, or other packaging - fixed mass
}

/**
 * Records an actual mass measurement for a specific inventory item.
 * Links measured mass to the combination of physical components.
 */
data class MassMeasurement(
    val id: String,                         // Unique identifier
    val trayUid: String,                   // Links to inventory item
    val measuredMassGrams: Float,          // Actual measured mass
    val componentIds: List<String>,        // Which components were measured together
    val measurementType: MeasurementType,  // Full vs empty measurement
    val measuredAt: LocalDateTime,         // When measurement was taken
    val notes: String = "",                // Optional user notes
    val isVerified: Boolean = false        // User verification flag
) {
    /**
     * Calculate actual filament mass based on known fixed component masses
     */
    fun calculateFilamentMass(fixedComponentMassGrams: Float): Float {
        return when (measurementType) {
            MeasurementType.TOTAL_MASS -> measuredMassGrams - fixedComponentMassGrams
            MeasurementType.COMPONENT_ONLY -> 0f // Component only, no filament
            MeasurementType.TOTAL_MASS -> measuredMassGrams - fixedComponentMassGrams
            MeasurementType.COMPONENT_ONLY -> 0f // Component only, no filament
        }
    }
}


