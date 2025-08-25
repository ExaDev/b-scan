package com.bscan.logic

import com.bscan.model.*
import kotlin.math.abs

/**
 * Service for performing mass calculations, inferences, and validations
 * for physical component inventory management.
 */
class MassCalculationService {
    
    /**
     * Calculate filament mass when total measured mass is updated
     */
    fun calculateFilamentMassFromTotal(
        totalMeasuredMass: Float,
        components: List<PhysicalComponent>,
        measurementType: MeasurementType = MeasurementType.FULL_WEIGHT
    ): MassCalculationResult {
        
        if (totalMeasuredMass <= 0f) {
            return MassCalculationResult(
                success = false,
                errorMessage = "Measured mass must be greater than 0"
            )
        }
        
        val fixedComponentMass = components.filter { !it.variableMass }
            .sumOf { it.massGrams.toDouble() }.toFloat()
        
        return when (measurementType) {
            MeasurementType.FULL_WEIGHT -> {
                val filamentMass = totalMeasuredMass - fixedComponentMass
                if (filamentMass < 0f) {
                    MassCalculationResult(
                        success = false,
                        errorMessage = "Calculated filament mass is negative - check component masses"
                    )
                } else {
                    MassCalculationResult(
                        success = true,
                        filamentMassGrams = filamentMass,
                        fixedComponentsMassGrams = fixedComponentMass,
                        totalMassGrams = totalMeasuredMass
                    )
                }
            }
            MeasurementType.EMPTY_WEIGHT -> {
                MassCalculationResult(
                    success = true,
                    filamentMassGrams = 0f,
                    fixedComponentsMassGrams = totalMeasuredMass,
                    totalMassGrams = totalMeasuredMass
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
            filamentMassGrams = currentFilamentMass,
            fixedComponentsMassGrams = fixedComponentsMass,
            totalMassGrams = currentTotalMass
        )
    }
    
    /**
     * Update variable mass components based on new total mass
     */
    fun updateVariableComponents(
        components: List<PhysicalComponent>,
        newTotalMass: Float
    ): List<PhysicalComponent> {
        val fixedComponentsMass = components.filter { !it.variableMass }
            .sumOf { it.massGrams.toDouble() }.toFloat()
        
        val availableFilamentMass = newTotalMass - fixedComponentsMass
        val variableComponents = components.filter { it.variableMass }
        
        if (variableComponents.isEmpty()) {
            return components
        }
        
        // For now, distribute equally among variable components
        val massPerVariableComponent = availableFilamentMass / variableComponents.size
        
        return components.map { component ->
            if (component.variableMass) {
                component.copy(massGrams = massPerVariableComponent)
            } else {
                component
            }
        }
    }
    
    /**
     * Validate component combination
     */
    fun validateComponents(components: List<PhysicalComponent>): ValidationResult {
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
        filamentComponent: PhysicalComponent,
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
        
        val inferredMass = request.knownTotalMass - knownMass
        
        return if (inferredMass >= 0f) {
            MassCalculationResult(
                success = true,
                filamentMassGrams = if (request.targetComponentId.contains("filament")) inferredMass else 0f,
                fixedComponentsMassGrams = if (!request.targetComponentId.contains("filament")) inferredMass else 0f,
                totalMassGrams = request.knownTotalMass
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