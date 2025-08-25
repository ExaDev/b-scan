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
    val massGrams: Float,                   // Current mass in grams
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
            MeasurementType.FULL_WEIGHT -> measuredMassGrams - fixedComponentMassGrams
            MeasurementType.EMPTY_WEIGHT -> 0f // Empty measurement, no filament
        }
    }
}

/**
 * Type of mass measurement
 */
enum class MeasurementType {
    FULL_WEIGHT,    // Weight with filament
    EMPTY_WEIGHT    // Weight without filament (dry components only)
}

/**
 * Result of mass calculation operations
 */
data class MassCalculationResult(
    val success: Boolean,
    val filamentMassGrams: Float = 0f,
    val fixedComponentsMassGrams: Float = 0f,
    val totalMassGrams: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Container for mass inference operations
 */
data class MassInferenceRequest(
    val knownTotalMass: Float,              // Measured total mass
    val knownFilamentMass: Float?,          // Expected filament mass (if known)
    val knownComponentIds: List<String>,    // Known component IDs
    val targetComponentId: String           // Component to infer mass for
)

