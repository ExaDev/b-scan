package com.bscan.model

import java.time.LocalDateTime

/**
 * Unified component model that represents any trackable item.
 * Components can contain other components hierarchically.
 * "Inventory Items" are root components with unique identifiers.
 */
data class Component(
    val id: String,                              // Internal component ID
    val uniqueIdentifier: String? = null,        // External unique ID (trayUid, serial number, etc.)
    val name: String,
    val category: String = "general",
    val tags: List<String> = emptyList(),
    
    // Hierarchical structure
    val childComponents: List<String> = emptyList(),  // IDs of child components
    val parentComponentId: String? = null,            // ID of parent (if this is a sub-component)
    
    // Mass properties
    val massGrams: Float?,                       // Current mass (null if unknown/inferred)
    val fullMassGrams: Float? = null,           // Original/max mass for variable components
    val variableMass: Boolean = false,          // Can mass change over time?
    val inferredMass: Boolean = false,          // Was mass calculated vs measured?
    
    // Metadata
    val manufacturer: String = "Unknown",
    val description: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Check if this component represents an inventory item (uniquely identifiable)
     */
    val isInventoryItem: Boolean
        get() = uniqueIdentifier != null && parentComponentId == null
    
    /**
     * Check if this component is a root component (has no parent)
     */
    val isRootComponent: Boolean
        get() = parentComponentId == null
    
    /**
     * Check if this component is a composite (has child components)
     */
    val isComposite: Boolean
        get() = childComponents.isNotEmpty()
    
    /**
     * Check if this component can have its mass adjusted
     */
    val canAdjustMass: Boolean
        get() = variableMass
        
    /**
     * Create a copy with an added child component
     */
    fun withChildComponent(childId: String): Component {
        return if (childId !in childComponents) {
            copy(
                childComponents = childComponents + childId,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
    
    /**
     * Create a copy with a removed child component
     */
    fun withoutChildComponent(childId: String): Component {
        return copy(
            childComponents = childComponents.filter { it != childId },
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Create a copy with updated mass
     */
    fun withUpdatedMass(newMassGrams: Float): Component {
        return copy(
            massGrams = newMassGrams,
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Create a copy with updated full mass (for variable mass components)
     */
    fun withUpdatedFullMass(newFullMassGrams: Float): Component {
        return if (variableMass) {
            copy(
                fullMassGrams = newFullMassGrams,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
    
    /**
     * Calculate remaining percentage based on full vs current mass
     */
    fun getRemainingPercentage(): Float? {
        return if (variableMass && fullMassGrams != null && fullMassGrams > 0 && massGrams != null) {
            maxOf(0f, minOf(1f, massGrams / fullMassGrams))
        } else {
            null
        }
    }
    
    /**
     * Calculate consumed mass based on full vs current mass
     */
    fun getConsumedMass(): Float? {
        return if (variableMass && fullMassGrams != null && massGrams != null) {
            maxOf(0f, fullMassGrams - massGrams)
        } else {
            null
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
 * Records an actual mass measurement for a specific component.
 */
data class ComponentMeasurement(
    val id: String,                         // Unique identifier
    val componentId: String,               // Which component was measured
    val measuredMassGrams: Float,          // Actual measured mass
    val measurementType: MeasurementType,  // Total vs partial measurement
    val measuredAt: LocalDateTime,         // When measurement was taken
    val notes: String = "",                // Optional user notes
    val isVerified: Boolean = false        // User verification flag
)

/**
 * Type of mass measurement
 */
enum class MeasurementType {
    TOTAL_MASS,     // Total mass of component and all children
    COMPONENT_ONLY, // Mass of just this component (excluding children)
    FULL_WEIGHT,    // Weight with filament (PhysicalComponent compatibility)
    EMPTY_WEIGHT    // Weight without filament (PhysicalComponent compatibility)
}

/**
 * Result of mass calculation operations
 */
data class MassCalculationResult(
    val success: Boolean,
    val componentMass: Float = 0f,
    val totalMass: Float = 0f,
    val errorMessage: String? = null
)

/**
 * Request for mass inference operations
 */
data class MassInferenceRequest(
    val parentComponentId: String,          // Parent component containing unknown component
    val totalMeasuredMass: Float,          // Measured total mass
    val unknownComponentId: String,        // Component to infer mass for
    val knownComponentIds: List<String>    // Known child components with masses
)