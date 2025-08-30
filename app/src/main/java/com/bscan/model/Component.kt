package com.bscan.model

import java.time.LocalDateTime

/**
 * Types of identifiers that components can have
 */
enum class IdentifierType {
    RFID_HARDWARE,    // Hardware UID burned into RFID chip
    CONSUMABLE_UNIT,  // Application data like tray UID for filament tracking
    QR,               // QR code identifier
    BARCODE,          // Barcode identifier
    SERIAL_NUMBER,    // Manufacturer serial number
    SKU,              // Stock keeping unit
    BATCH_NUMBER,     // Production batch identifier
    MODEL_NUMBER,     // Product model identifier
    CUSTOM            // User-defined identifier type
}

/**
 * Purpose of an identifier in the system
 */
enum class IdentifierPurpose {
    AUTHENTICATION,   // Used for RFID authentication and security
    TRACKING,         // Used for inventory tracking and consumable management
    LOOKUP,           // Used for catalog or database lookup
    DISPLAY,          // Human-readable identifier for display
    LINKING           // Links related components together
}

/**
 * Identifier for components supporting multiple types and purposes
 */
data class ComponentIdentifier(
    val type: IdentifierType,
    val value: String,
    val format: String? = null,        // Format specification (e.g., "hex", "uuid", "ean13")
    val purpose: IdentifierPurpose,
    val metadata: Map<String, String> = emptyMap()  // Additional identifier-specific data
) {
    /**
     * Check if this identifier is unique (suitable for inventory tracking)
     */
    val isUnique: Boolean
        get() = when (type) {
            IdentifierType.RFID_HARDWARE, IdentifierType.SERIAL_NUMBER, 
            IdentifierType.CONSUMABLE_UNIT -> true
            else -> false
        }
}

/**
 * Unified component model that represents any trackable item.
 * Components can contain other components hierarchically.
 * "Inventory Items" are root components with unique identifiers.
 */
data class Component(
    val id: String,                              // Internal component ID
    val identifiers: List<ComponentIdentifier> = emptyList(),  // Multiple identifier support
    val name: String,
    val category: String = "general",
    val tags: List<String> = emptyList(),
    
    // Hierarchical structure
    val childComponents: List<String> = emptyList(),  // IDs of child components
    val siblingReferences: List<String> = emptyList(), // IDs of sibling components for cross-referencing
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
        get() = hasUniqueIdentifier() && parentComponentId == null
    
    /**
     * Check if this component has any unique identifiers
     */
    fun hasUniqueIdentifier(): Boolean {
        return identifiers.any { it.isUnique }
    }
    
    /**
     * Get identifier by type
     */
    fun getIdentifierByType(type: IdentifierType): ComponentIdentifier? {
        return identifiers.find { it.type == type }
    }
    
    /**
     * Get all identifiers with a specific purpose
     */
    fun getIdentifiersByPurpose(purpose: IdentifierPurpose): List<ComponentIdentifier> {
        return identifiers.filter { it.purpose == purpose }
    }
    
    /**
     * Get primary tracking identifier (for inventory management)
     */
    fun getPrimaryTrackingIdentifier(): ComponentIdentifier? {
        return identifiers.find { it.purpose == IdentifierPurpose.TRACKING && it.isUnique }
            ?: identifiers.find { it.isUnique }
    }
    
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
    fun withChildComponent(componentId: String): Component {
        return if (componentId !in childComponents) {
            copy(
                childComponents = childComponents + componentId,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
    
    /**
     * Create a copy with a removed child component
     */
    fun withoutChildComponent(componentId: String): Component {
        return copy(
            childComponents = childComponents.filter { it != componentId },
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Create a copy with an added sibling reference
     */
    fun addSiblingReference(siblingId: String): Component {
        return if (siblingId !in siblingReferences && siblingId != id) {
            copy(
                siblingReferences = siblingReferences + siblingId,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
    
    /**
     * Create a copy with a removed sibling reference
     */
    fun removeSiblingReference(siblingId: String): Component {
        return copy(
            siblingReferences = siblingReferences.filter { it != siblingId },
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Calculate hierarchy depth (0 for root components)
     */
    fun getHierarchyDepth(): Int {
        // Note: This requires repository access to traverse the hierarchy
        // Implementation would need access to ComponentRepository
        // For now, return simple depth based on parent presence
        return if (parentComponentId == null) 0 else 1
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
    
    /**
     * Find child components by category
     */
    fun findChildrenByCategory(category: String): List<String> {
        // Note: This requires repository access to check child component categories
        // For now, return the child component IDs (caller must filter)
        return childComponents
    }
    
    /**
     * Check if this component is an ancestor of another component
     * Note: This requires repository access for full implementation
     */
    fun isAncestorOf(componentId: String): Boolean {
        // Simple implementation - check direct children only
        return componentId in childComponents
    }
    
    /**
     * Create a copy with an added identifier
     */
    fun withIdentifier(identifier: ComponentIdentifier): Component {
        return if (identifiers.none { it.type == identifier.type && it.value == identifier.value }) {
            copy(
                identifiers = identifiers + identifier,
                lastUpdated = LocalDateTime.now()
            )
        } else {
            this
        }
    }
    
    /**
     * Create a copy with a removed identifier
     */
    fun withoutIdentifier(type: IdentifierType, value: String): Component {
        return copy(
            identifiers = identifiers.filter { !(it.type == type && it.value == value) },
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Create a copy with updated identifiers
     */
    fun withUpdatedIdentifiers(newIdentifiers: List<ComponentIdentifier>): Component {
        return copy(
            identifiers = newIdentifiers,
            lastUpdated = LocalDateTime.now()
        )
    }
    
    /**
     * Extract visual properties from child components and combine them intelligently
     */
    fun getAggregatedVisualProperties(getChildComponents: (List<String>) -> List<Component>): Map<String, String> {
        if (childComponents.isEmpty()) return emptyMap()
        
        val children = getChildComponents(childComponents)
        val aggregatedProperties = mutableMapOf<String, String>()
        val colorProperties = mutableSetOf<String>()
        val materialProperties = mutableSetOf<String>()
        
        // Extract color and material properties from all children
        children.forEach { child ->
            // Check for color properties in various keys
            listOf("color", "colour", "colorName", "colourName").forEach { key ->
                child.metadata[key]?.let { value ->
                    if (value.isNotBlank()) colorProperties.add(value)
                }
            }
            
            // Check for material properties
            listOf("material", "materialType", "filamentType").forEach { key ->
                child.metadata[key]?.let { value ->
                    if (value.isNotBlank()) materialProperties.add(value)
                }
            }
        }
        
        // Combine properties intelligently
        if (colorProperties.size == 1 && materialProperties.size == 1) {
            // Simple case: one color, one material
            aggregatedProperties["displayName"] = "${colorProperties.first()} ${materialProperties.first()}"
        } else if (colorProperties.size == 1 && materialProperties.isEmpty()) {
            // Only color available
            aggregatedProperties["displayName"] = colorProperties.first()
        } else if (colorProperties.isEmpty() && materialProperties.size == 1) {
            // Only material available  
            aggregatedProperties["displayName"] = materialProperties.first()
        } else if (colorProperties.size > 1 || materialProperties.size > 1) {
            // Multiple colors or materials - show as mixed
            val colorPart = when (colorProperties.size) {
                0 -> ""
                1 -> colorProperties.first()
                else -> "Mixed Colors"
            }
            val materialPart = when (materialProperties.size) {
                0 -> ""
                1 -> materialProperties.first()
                else -> "Mixed Materials"
            }
            
            aggregatedProperties["displayName"] = listOf(colorPart, materialPart)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .takeIf { it.isNotBlank() } ?: "Mixed Properties"
        }
        
        // Store individual properties for reference
        if (colorProperties.isNotEmpty()) {
            aggregatedProperties["aggregatedColors"] = colorProperties.joinToString(", ")
        }
        if (materialProperties.isNotEmpty()) {
            aggregatedProperties["aggregatedMaterials"] = materialProperties.joinToString(", ")
        }
        
        return aggregatedProperties
    }
    
    /**
     * Get display name with visual properties from children if available
     */
    fun getDisplayNameWithVisualProperties(getChildComponents: (List<String>) -> List<Component>): String {
        val visualProperties = getAggregatedVisualProperties(getChildComponents)
        return visualProperties["displayName"] ?: name
    }
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
    COMPONENT_ONLY  // Mass of just this component (excluding children)
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

/**
 * Mass calculation helper functions for Component
 * These functions require access to ComponentRepository for full hierarchy traversal
 */
class ComponentMassCalculator {
    /**
     * Calculate total mass including all children
     * Note: Requires ComponentRepository to resolve child components
     */
    fun getTotalMass(component: Component, getChildComponents: (List<String>) -> List<Component>): Float? {
        val ownMass = component.massGrams ?: 0f
        val childComponents = getChildComponents(component.childComponents)
        val childMass = childComponents.mapNotNull { it.massGrams }.sum()
        
        return if (component.massGrams != null || childComponents.any { it.massGrams != null }) {
            ownMass + childMass
        } else {
            null
        }
    }
    
    /**
     * Calculate total known mass of child components
     */
    fun getKnownChildrenMass(component: Component, getChildComponents: (List<String>) -> List<Component>): Float {
        val childComponents = getChildComponents(component.childComponents)
        return childComponents.mapNotNull { it.massGrams }.sum()
    }
    
    /**
     * Check if component mass can be inferred from total measurement
     */
    fun canInferMass(component: Component, totalMeasuredMass: Float, getChildComponents: (List<String>) -> List<Component>): Boolean {
        if (component.massGrams != null) return false // Already has mass
        
        val childComponents = getChildComponents(component.childComponents)
        val knownChildMass = childComponents.mapNotNull { it.massGrams }.sum()
        val unknownChildCount = childComponents.count { it.massGrams == null }
        
        // Can infer if this is the only unknown component
        return unknownChildCount <= 1 && totalMeasuredMass > knownChildMass
    }
}