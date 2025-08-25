package com.bscan.logic

import com.bscan.model.PhysicalComponentType
import com.bscan.repository.PhysicalComponentRepository

/**
 * Validation logic for physical components
 */
class ComponentValidation(private val repository: PhysicalComponentRepository) {
    
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
    fun validateMass(massText: String, type: PhysicalComponentType): String? {
        if (massText.isBlank()) {
            return "Mass is required"
        }
        
        val mass = massText.toFloatOrNull()
        if (mass == null) {
            return "Mass must be a valid number"
        }
        
        return when {
            mass <= 0 -> "Mass must be greater than 0"
            mass < getMinMassForType(type) -> "Mass is too low for this component type (min: ${getMinMassForType(type)}g)"
            mass > getMaxMassForType(type) -> "Mass is too high for this component type (max: ${getMaxMassForType(type)}g)"
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
     * Check if a component name is already in use
     */
    private fun isNameAlreadyUsed(name: String, existingComponentId: String?): Boolean {
        return repository.getComponents().any { component ->
            component.name.equals(name, ignoreCase = true) && component.id != existingComponentId
        }
    }
    
    /**
     * Get minimum reasonable mass for component type
     */
    private fun getMinMassForType(type: PhysicalComponentType): Float {
        return when (type) {
            PhysicalComponentType.FILAMENT -> 50f // Minimum 50g for filament samples
            PhysicalComponentType.BASE_SPOOL -> 50f // Minimum 50g for very small spools
            PhysicalComponentType.CORE_RING -> 5f // Minimum 5g for cardboard cores
            PhysicalComponentType.ADAPTER -> 1f // Minimum 1g for small adapters
            PhysicalComponentType.PACKAGING -> 1f // Minimum 1g for lightweight packaging
        }
    }
    
    /**
     * Get maximum reasonable mass for component type
     */
    private fun getMaxMassForType(type: PhysicalComponentType): Float {
        return when (type) {
            PhysicalComponentType.FILAMENT -> 5000f // Max 5kg for large industrial filament
            PhysicalComponentType.BASE_SPOOL -> 2000f // Max 2kg for very heavy industrial spools
            PhysicalComponentType.CORE_RING -> 200f // Max 200g for heavy metal cores
            PhysicalComponentType.ADAPTER -> 500f // Max 500g for metal adapters
            PhysicalComponentType.PACKAGING -> 1000f // Max 1kg for heavy packaging
        }
    }
    
    /**
     * Get suggested mass ranges for component types
     */
    fun getSuggestedMassRange(type: PhysicalComponentType): Pair<Float, Float> {
        return when (type) {
            PhysicalComponentType.FILAMENT -> 200f to 1200f // Typical filament weights
            PhysicalComponentType.BASE_SPOOL -> 100f to 400f // Typical spool weights
            PhysicalComponentType.CORE_RING -> 20f to 60f // Typical core weights
            PhysicalComponentType.ADAPTER -> 10f to 100f // Typical adapter weights
            PhysicalComponentType.PACKAGING -> 5f to 50f // Typical packaging weights
        }
    }
    
    /**
     * Get helpful description for component type
     */
    fun getTypeDescription(type: PhysicalComponentType): String {
        return when (type) {
            PhysicalComponentType.FILAMENT -> "The actual filament material (variable mass)"
            PhysicalComponentType.BASE_SPOOL -> "The main spool body or reel (fixed mass)"
            PhysicalComponentType.CORE_RING -> "Cardboard or plastic core insert (fixed mass)"
            PhysicalComponentType.ADAPTER -> "Conversion adapter between spool types (fixed mass)"
            PhysicalComponentType.PACKAGING -> "Bags, boxes, or protective packaging (fixed mass)"
        }
    }
    
    /**
     * Validate entire component
     */
    fun validateComponent(
        name: String,
        massText: String,
        type: PhysicalComponentType,
        manufacturer: String,
        description: String,
        existingComponentId: String? = null
    ): ComponentValidationResult {
        val nameError = validateName(name, existingComponentId)
        val massError = validateMass(massText, type)
        val manufacturerError = validateManufacturer(manufacturer)
        val descriptionError = validateDescription(description)
        
        return ComponentValidationResult(
            isValid = nameError == null && massError == null && manufacturerError == null && descriptionError == null,
            nameError = nameError,
            massError = massError,
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
    val manufacturerError: String? = null,
    val descriptionError: String? = null
)