package com.bscan.logic

import com.bscan.repository.ComponentRepository

/**
 * Validation logic for hierarchical components
 */
class ComponentValidation(private val repository: ComponentRepository) {
    
    /**
     * Validate component name
     */
    fun validateName(name: String, existingComponentId: String? = null): String? {
        val trimmedName = name.trim()
        
        return when {
            trimmedName.isBlank() -> "Name is required"
            trimmedName.length < 2 -> "Name must be at least 2 characters"
            trimmedName.length > 50 -> "Name must be 50 characters or less"
            isNameAlreadyUsed(trimmedName, existingComponentId) -> "A component with this name already exists"
            else -> null
        }
    }
    
    /**
     * Validate mass value
     */
    fun validateMass(massText: String, category: String): String? {
        if (massText.isBlank()) {
            return "Mass is required"
        }
        
        val mass = massText.toFloatOrNull()
        if (mass == null) {
            return "Mass must be a valid number"
        }
        
        return when {
            mass < 0 -> "Mass cannot be negative"
            mass < getMinMassForCategory(category) -> "Mass is too low for this component category (min: ${getMinMassForCategory(category)}g)"
            mass > getMaxMassForCategory(category) -> "Mass is too high for this component category (max: ${getMaxMassForCategory(category)}g)"
            else -> null
        }
    }
    
    /**
     * Validate manufacturer name
     */
    fun validateManufacturer(manufacturer: String): String? {
        val trimmed = manufacturer.trim()
        
        return when {
            trimmed.length > 30 -> "Manufacturer name must be 30 characters or less"
            else -> null
        }
    }
    
    /**
     * Validate description
     */
    fun validateDescription(description: String): String? {
        return when {
            description.length > 200 -> "Description must be 200 characters or less"
            else -> null
        }
    }
    
    /**
     * Validate component category
     */
    fun validateCategory(category: String): String? {
        val trimmed = category.trim()
        
        return when {
            trimmed.isBlank() -> "Category is required"
            trimmed.length < 2 -> "Category must be at least 2 characters"
            trimmed.length > 30 -> "Category must be 30 characters or less"
            else -> null
        }
    }
    
    /**
     * Check if a component name is already in use
     */
    private fun isNameAlreadyUsed(name: String, existingComponentId: String?): Boolean {
        return repository.getComponents().any { component ->
            component.name.equals(name, ignoreCase = true) && component.id != existingComponentId
        }
    }
    
    /**
     * Get minimum reasonable mass for component category
     */
    private fun getMinMassForCategory(category: String): Float {
        return when (category.lowercase()) {
            "filament" -> 50f // Minimum 50g for filament samples
            "spool" -> 50f // Minimum 50g for very small spools
            "core" -> 5f // Minimum 5g for cardboard cores
            "adapter" -> 1f // Minimum 1g for small adapters
            "packaging" -> 1f // Minimum 1g for lightweight packaging
            "rfid-tag" -> 0.1f // Minimum 0.1g for RFID tags
            else -> 0.1f // Default minimum
        }
    }
    
    /**
     * Get maximum reasonable mass for component category
     */
    private fun getMaxMassForCategory(category: String): Float {
        return when (category.lowercase()) {
            "filament" -> 5000f // Max 5kg for large industrial filament
            "spool" -> 2000f // Max 2kg for very heavy industrial spools
            "core" -> 200f // Max 200g for heavy metal cores
            "adapter" -> 500f // Max 500g for metal adapters
            "packaging" -> 1000f // Max 1kg for heavy packaging
            "rfid-tag" -> 5f // Max 5g for RFID tags
            else -> 10000f // Default maximum
        }
    }
    
    /**
     * Get suggested mass ranges for component categories
     */
    fun getSuggestedMassRange(category: String): Pair<Float, Float> {
        return when (category.lowercase()) {
            "filament" -> 200f to 1200f // Typical filament weights
            "spool" -> 100f to 400f // Typical spool weights
            "core" -> 20f to 60f // Typical core weights
            "adapter" -> 10f to 100f // Typical adapter weights
            "packaging" -> 5f to 50f // Typical packaging weights
            "rfid-tag" -> 0.5f to 2f // Typical RFID tag weights
            else -> 10f to 1000f // Default range
        }
    }
    
    /**
     * Get helpful description for component category
     */
    fun getCategoryDescription(category: String): String {
        return when (category.lowercase()) {
            "filament" -> "The actual filament material (variable mass)"
            "spool" -> "The main spool body or reel (fixed mass)"
            "core" -> "Cardboard or plastic core insert (fixed mass)"
            "adapter" -> "Conversion adapter between spool types (fixed mass)"
            "packaging" -> "Bags, boxes, or protective packaging (fixed mass)"
            "rfid-tag" -> "RFID tags for identification (fixed mass)"
            "filament-tray" -> "Composite component containing multiple parts"
            else -> "General component category"
        }
    }
    
    /**
     * Validate entire component
     */
    fun validateComponent(
        name: String,
        massText: String,
        category: String,
        manufacturer: String,
        description: String,
        existingComponentId: String? = null
    ): ComponentValidationResult {
        val nameError = validateName(name, existingComponentId)
        val massError = validateMass(massText, category)
        val categoryError = validateCategory(category)
        val manufacturerError = validateManufacturer(manufacturer)
        val descriptionError = validateDescription(description)
        
        return ComponentValidationResult(
            isValid = nameError == null && massError == null && categoryError == null && manufacturerError == null && descriptionError == null,
            nameError = nameError,
            massError = massError,
            categoryError = categoryError,
            manufacturerError = manufacturerError,
            descriptionError = descriptionError
        )
    }
}

/**
 * Result of component validation
 */
data class ComponentValidationResult(
    val isValid: Boolean,
    val nameError: String? = null,
    val massError: String? = null,
    val categoryError: String? = null,
    val manufacturerError: String? = null,
    val descriptionError: String? = null
)