package com.bscan.model

import java.time.LocalDateTime

/**
 * User modifications and customisations that overlay on top of generated components.
 * This keeps user data separate from generated components, allowing generation logic to evolve
 * without losing user customisations.
 */
data class UserComponentOverlay(
    val componentId: String,                    // References the generated component ID
    val isUserCreated: Boolean = false,        // true = fully user-created, false = modification of generated component
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    // User modifications to component properties
    val nameOverride: String? = null,           // Custom name for the component
    val categoryOverride: String? = null,       // Custom category
    val tagsOverride: List<String>? = null,     // Custom tags (replaces generated tags)
    val tagsAdditions: List<String> = emptyList(), // Additional tags (added to generated tags)
    val descriptionOverride: String? = null,    // Custom description
    val massOverride: Float? = null,            // User-measured mass override
    val fullMassOverride: Float? = null,        // User-specified full mass override
    val manufacturerOverride: String? = null,   // Custom manufacturer
    
    // Custom metadata additions/overrides
    val metadataOverrides: Map<String, String> = emptyMap(),  // Overrides generated metadata
    val metadataAdditions: Map<String, String> = emptyMap(),  // Additional metadata
    
    // User-defined identifiers (in addition to generated ones)
    val additionalIdentifiers: List<ComponentIdentifier> = emptyList(),
    
    // Component hierarchy modifications
    val customParentId: String? = null,         // Override parent component assignment
    val additionalChildIds: List<String> = emptyList(), // Additional child components
    val hiddenChildIds: List<String> = emptyList(),     // Hide specific child components
    
    // UI preferences
    val isHidden: Boolean = false,              // Hide component from UI
    val isPinned: Boolean = false,              // Pin to top of lists
    val customColor: String? = null,            // Custom colour for UI display
    val customIcon: String? = null,             // Custom icon identifier
    
    // Notes and user data
    val userNotes: String = "",                 // User notes about this component
    val lastUsed: LocalDateTime? = null,        // When component was last used/referenced
    val usageCount: Int = 0,                    // How many times referenced/used
    
    // Flags for tracking state
    val isDeleted: Boolean = false,             // Soft delete flag
    val isArchived: Boolean = false,            // Archive flag
    
    // Version tracking for conflict resolution
    val version: Int = 1,                       // Increment on each modification
    val lastSyncedAt: LocalDateTime? = null     // For future cloud sync
) {
    
    /**
     * Check if this overlay has any modifications to apply
     */
    fun hasModifications(): Boolean {
        return nameOverride != null ||
                categoryOverride != null ||
                tagsOverride != null ||
                tagsAdditions.isNotEmpty() ||
                descriptionOverride != null ||
                massOverride != null ||
                fullMassOverride != null ||
                manufacturerOverride != null ||
                metadataOverrides.isNotEmpty() ||
                metadataAdditions.isNotEmpty() ||
                additionalIdentifiers.isNotEmpty() ||
                customParentId != null ||
                additionalChildIds.isNotEmpty() ||
                hiddenChildIds.isNotEmpty() ||
                isHidden ||
                isPinned ||
                customColor != null ||
                customIcon != null ||
                userNotes.isNotBlank() ||
                isDeleted ||
                isArchived
    }
    
    /**
     * Create a new version with updated modification time
     */
    fun withUpdate(): UserComponentOverlay {
        return copy(
            modifiedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
    
    /**
     * Mark as deleted (soft delete)
     */
    fun markDeleted(): UserComponentOverlay {
        return copy(
            isDeleted = true,
            modifiedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
    
    /**
     * Mark as archived
     */
    fun markArchived(): UserComponentOverlay {
        return copy(
            isArchived = true,
            modifiedAt = LocalDateTime.now(),
            version = version + 1
        )
    }
}

/**
 * Different types of modifications a user can make to components
 */
enum class UserModificationType {
    BASIC_INFO,      // Name, description, category changes
    MASS_OVERRIDE,   // Mass measurements and overrides
    METADATA,        // Custom metadata additions
    IDENTIFIERS,     // Additional identifiers
    HIERARCHY,       // Parent/child relationship changes
    UI_PREFERENCES,  // Display preferences
    NOTES,           // User notes and usage tracking
    STATE            // Hidden, archived, deleted states
}

/**
 * Represents a specific user modification for audit/undo purposes
 */
data class UserModificationRecord(
    val overlayId: String,                      // Which overlay this modification belongs to
    val modificationType: UserModificationType,
    val fieldName: String,                      // Which field was modified
    val oldValue: String?,                      // Previous value
    val newValue: String?,                      // New value
    val modifiedAt: LocalDateTime = LocalDateTime.now(),
    val reason: String = ""                     // Optional reason for the change
)