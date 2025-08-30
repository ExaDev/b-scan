package com.bscan.logic

import com.bscan.model.*
import kotlin.math.abs

/**
 * Service for performing mass calculations, inferences, and validations
 * for hierarchical component inventory management.
 */
class MassCalculationService {
    
    /**
     * Calculate filament mass when total measured mass is updated
     */
    fun calculateFilamentMassFromTotal(
        totalMeasuredMass: Float,
        components: List<Component>,
        measurementType: MeasurementType = MeasurementType.TOTAL_MASS
    ): MassCalculationResult {
        
        if (totalMeasuredMass <= 0f) {
            return MassCalculationResult(
                success = false,
                errorMessage = "Measured mass must be greater than 0"
            )
        }
        
        val fixedComponentMass = components.filter { !it.variableMass }
            .sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        
        return when (measurementType) {
            MeasurementType.TOTAL_MASS -> {
                val filamentMass = totalMeasuredMass - fixedComponentMass
                if (filamentMass < 0f) {
                    MassCalculationResult(
                        success = false,
                        errorMessage = "Calculated filament mass is negative - check component masses"
                    )
                } else {
                    MassCalculationResult(
                        success = true,
                        componentMass = filamentMass,
                        totalMass = totalMeasuredMass
                    )
                }
            }
            MeasurementType.COMPONENT_ONLY -> {
                MassCalculationResult(
                    success = true,
                    componentMass = 0f,
                    totalMass = totalMeasuredMass
                )
            }
            MeasurementType.TOTAL_MASS -> {
                val filamentMass = totalMeasuredMass - fixedComponentMass
                MassCalculationResult(
                    success = filamentMass >= 0f,
                    componentMass = if (filamentMass >= 0f) filamentMass else 0f,
                    totalMass = totalMeasuredMass,
                    errorMessage = if (filamentMass < 0f) "Calculated filament mass is negative" else null
                )
            }
            MeasurementType.COMPONENT_ONLY -> {
                MassCalculationResult(
                    success = true,
                    componentMass = 0f, // No filament
                    totalMass = totalMeasuredMass
                )
            }
        }
    }
    
    /**
     * Calculate remaining filament mass based on measurements
     */
    fun calculateRemainingFilament(
        initialFilamentMass: Float,
        currentTotalMass: Float,
        fixedComponentsMass: Float
    ): MassCalculationResult {
        
        val currentFilamentMass = currentTotalMass - fixedComponentsMass
        val consumedMass = initialFilamentMass - currentFilamentMass
        
        if (currentFilamentMass < 0f) {
            return MassCalculationResult(
                success = false,
                errorMessage = "Current filament mass is negative"
            )
        }
        
        return MassCalculationResult(
            success = true,
            componentMass = currentFilamentMass,
            totalMass = currentTotalMass
        )
    }
    
    /**
     * Update variable mass components based on new total mass
     */
    fun updateVariableComponents(
        components: List<Component>,
        newTotalMass: Float
    ): List<Component> {
        val fixedComponentsMass = components.filter { !it.variableMass }
            .sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        
        val availableFilamentMass = newTotalMass - fixedComponentsMass
        val variableComponents = components.filter { it.variableMass }
        
        if (variableComponents.isEmpty()) {
            return components
        }
        
        if (availableFilamentMass <= 0) {
            return components.map { component ->
                if (component.variableMass) {
                    component.copy(massGrams = 0f)
                } else {
                    component
                }
            }
        }
        
        // Distribute proportionally based on current masses
        val currentVariableMass = variableComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        
        return components.map { component ->
            if (!component.variableMass) {
                component
            } else if (currentVariableMass > 0) {
                val proportion = (component.massGrams ?: 0f) / currentVariableMass
                component.copy(massGrams = availableFilamentMass * proportion)
            } else {
                // Equal distribution if no current mass
                component.copy(massGrams = availableFilamentMass / variableComponents.size)
            }
        }
    }
    
    /**
     * Calculate new total mass when a component's mass changes
     */
    fun calculateTotalFromComponents(components: List<Component>): Float {
        return components.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
    }
    
    /**
     * Distribute total mass proportionally to variable components
     */
    fun distributeToVariableComponents(
        components: List<Component>,
        newTotalMass: Float
    ): DistributionResult {
        val fixedComponents = components.filter { !it.variableMass }
        val variableComponents = components.filter { it.variableMass }
        
        val fixedMass = fixedComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        val availableForVariable = newTotalMass - fixedMass
        
        if (availableForVariable < 0) {
            return DistributionResult(
                success = false,
                updatedComponents = components,
                errorMessage = "Total mass is less than fixed component mass (${formatWeight(fixedMass, WeightUnit.GRAMS)})"
            )
        }
        
        if (variableComponents.isEmpty()) {
            return DistributionResult(
                success = newTotalMass == fixedMass,
                updatedComponents = components,
                errorMessage = if (newTotalMass != fixedMass) 
                    "No variable components to adjust for mass difference" else null
            )
        }
        
        // Calculate proportional distribution
        val currentVariableMass = variableComponents.sumOf { (it.massGrams ?: 0f).toDouble() }.toFloat()
        val updatedComponents = components.map { component ->
            if (!component.variableMass) {
                component
            } else if (currentVariableMass > 0) {
                val proportion = (component.massGrams ?: 0f) / currentVariableMass
                component.copy(massGrams = availableForVariable * proportion)
            } else {
                // Equal distribution if no current variable mass
                val equalShare = availableForVariable / variableComponents.size
                component.copy(massGrams = equalShare)
            }
        }
        
        return DistributionResult(
            success = true,
            updatedComponents = updatedComponents,
            errorMessage = null
        )
    }
    
    /**
     * Update a single component and recalculate total
     */
    fun updateSingleComponent(
        components: List<Component>,
        componentId: String,
        newMass: Float,
        newFullMass: Float? = null
    ): SingleComponentUpdateResult {
        val targetComponent = components.find { it.id == componentId }
            ?: return SingleComponentUpdateResult(
                success = false,
                updatedComponents = components,
                newTotalMass = calculateTotalFromComponents(components),
                errorMessage = "Component not found"
            )
        
        // Validate mass constraints
        if (newMass < 0) {
            return SingleComponentUpdateResult(
                success = false,
                updatedComponents = components,
                newTotalMass = calculateTotalFromComponents(components),
                errorMessage = "Mass cannot be negative"
            )
        }
        
        if (targetComponent.variableMass && newFullMass != null && newMass > newFullMass) {
            return SingleComponentUpdateResult(
                success = false,
                updatedComponents = components,
                newTotalMass = calculateTotalFromComponents(components),
                errorMessage = "Current mass cannot exceed full mass"
            )
        }
        
        // Update the component
        val updatedComponents = components.map { component ->
            if (component.id == componentId) {
                var updated = component.copy(massGrams = newMass)
                if (newFullMass != null && component.variableMass) {
                    updated = updated.copy(fullMassGrams = newFullMass)
                }
                updated
            } else {
                component
            }
        }
        
        val newTotalMass = calculateTotalFromComponents(updatedComponents)
        
        return SingleComponentUpdateResult(
            success = true,
            updatedComponents = updatedComponents,
            newTotalMass = newTotalMass,
            errorMessage = null
        )
    }
    
    /**
     * Validate mass relationships in components
     */
    fun validateMassConstraints(components: List<Component>): MassValidationResult {
        val errors = mutableListOf<String>()
        
        components.forEach { component ->
            val mass = component.massGrams ?: 0f
            if (mass < 0) {
                errors.add("${component.name}: Mass cannot be negative")
            }
            
            if (component.variableMass && component.fullMassGrams != null) {
                if (component.fullMassGrams < 0) {
                    errors.add("${component.name}: Full mass cannot be negative")
                }
                if (mass > component.fullMassGrams) {
                    errors.add("${component.name}: Current mass cannot exceed full mass")
                }
            }
        }
        
        return MassValidationResult(
            valid = errors.isEmpty(),
            errors = errors
        )
    }
    
    /**
     * Validate component combination
     */
    fun validateComponents(components: List<Component>): ValidationResult {
        val variableCount = components.count { it.variableMass }
        val fixedCount = components.count { !it.variableMass }
        
        return when {
            components.isEmpty() -> ValidationResult(
                valid = false,
                message = "No components defined"
            )
            variableCount == 0 -> ValidationResult(
                valid = false,
                message = "No filament component found - add at least one variable mass component"
            )
            variableCount > 1 -> ValidationResult(
                valid = false,
                message = "Multiple filament components not supported - use one filament component only"
            )
            else -> ValidationResult(
                valid = true,
                message = "Component combination is valid"
            )
        }
    }
    
    /**
     * Create automatic component setup for Bambu filaments
     */
    fun createBambuComponentSetup(
        filamentComponent: Component,
        includeRefillableSpool: Boolean = false
    ): List<String> {
        val componentIds = mutableListOf<String>()
        
        // Add the filament component
        componentIds.add(filamentComponent.id)
        
        // Always add cardboard core for Bambu
        componentIds.add("bambu_cardboard_core")
        
        // Optionally add refillable spool
        if (includeRefillableSpool) {
            componentIds.add("bambu_refillable_spool")
        }
        
        return componentIds
    }
    
    /**
     * Format weight for display
     */
    fun formatWeight(weightGrams: Float, unit: WeightUnit, decimals: Int = 1): String {
        val convertedWeight = convertWeight(weightGrams, unit)
        val formatString = "%.${decimals}f"
        
        return when (unit) {
            WeightUnit.GRAMS -> "${String.format(formatString, convertedWeight)}g"
            WeightUnit.OUNCES -> "${String.format(formatString, convertedWeight)}oz"
            WeightUnit.POUNDS -> "${String.format(formatString, convertedWeight)}lbs"
            WeightUnit.KILOGRAMS -> "${String.format(formatString, convertedWeight)}kg"
        }
    }
    
    /**
     * Convert weight between units
     */
    private fun convertWeight(weightGrams: Float, targetUnit: WeightUnit): Float {
        return when (targetUnit) {
            WeightUnit.GRAMS -> weightGrams
            WeightUnit.OUNCES -> weightGrams * 0.035274f
            WeightUnit.POUNDS -> weightGrams * 0.00220462f
            WeightUnit.KILOGRAMS -> weightGrams * 0.001f
        }
    }
    
    /**
     * Infer component mass from known total and other components
     */
    fun inferComponentMass(request: MassInferenceRequest): MassCalculationResult {
        val knownMass = request.knownComponentIds.sumOf { 
            // This would need to be resolved with actual component masses
            0.0 // Placeholder - would lookup actual components
        }.toFloat()
        
        val inferredMass = request.totalMeasuredMass - knownMass
        
        return if (inferredMass >= 0f) {
            MassCalculationResult(
                success = true,
                componentMass = inferredMass,
                totalMass = request.totalMeasuredMass
            )
        } else {
            MassCalculationResult(
                success = false,
                errorMessage = "Inferred mass would be negative"
            )
        }
    }
}

/**
 * Weight units for display formatting
 */
enum class WeightUnit {
    GRAMS,
    OUNCES, 
    POUNDS,
    KILOGRAMS
}

/**
 * Result of component validation
 */
data class ValidationResult(
    val valid: Boolean,
    val message: String
)

/**
 * Result of mass distribution to variable components
 */
data class DistributionResult(
    val success: Boolean,
    val updatedComponents: List<Component>,
    val errorMessage: String?
)

/**
 * Result of single component mass update
 */
data class SingleComponentUpdateResult(
    val success: Boolean,
    val updatedComponents: List<Component>,
    val newTotalMass: Float,
    val errorMessage: String?
)

/**
 * Result of mass constraint validation
 */
data class MassValidationResult(
    val valid: Boolean,
    val errors: List<String>
)