package com.bscan.service

import android.util.Log
import com.bscan.ble.ScaleReading
import com.bscan.ble.WeightUnit
import com.bscan.model.*
import com.bscan.repository.ComponentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import kotlin.math.*

/**
 * Service for inferring unknown component masses from total measurements and scale readings.
 * Handles complex mass distribution, validation, and suggestion algorithms.
 */
class MassInferenceService(private val componentRepository: ComponentRepository) {
    
    companion object {
        private const val TAG = "MassInferenceService"
        
        // Component mass ranges for validation (in grams)
        private val COMPONENT_MASS_RANGES = mapOf(
            "filament" to 0.1f..2000.0f,      // Minimal to 2kg spool
            "rfid-tag" to 0.1f..5.0f,         // RFID tag mass
            "core" to 10.0f..100.0f,          // Cardboard/plastic core
            "spool" to 50.0f..500.0f,         // Spool hardware
            "nozzle" to 1.0f..50.0f,          // Printer nozzle
            "hotend" to 10.0f..200.0f,        // Hotend assembly
            "general" to 0.1f..10000.0f       // General components
        )
        
        // Default masses for common components when distributing unknown mass
        private val DEFAULT_COMPONENT_MASSES = mapOf(
            "rfid-tag" to 1.5f,      // Typical RFID tag
            "core" to 33.0f,         // Bambu cardboard core
            "spool" to 212.0f        // Bambu refillable spool
        )
        
        // Minimum reasonable measurement precision (grams)
        private const val MIN_MEASUREMENT_PRECISION = 0.1f
        
        // Maximum allowed measurement error percentage
        private const val MAX_MEASUREMENT_ERROR_PERCENT = 5.0f
    }
    
    /**
     * Infer the mass of unknown components from total measured mass
     */
    suspend fun inferComponentMass(
        parentComponent: Component, 
        totalMeasuredMass: Float
    ): InferenceResult = withContext(Dispatchers.Default) {
        
        try {
            Log.d(TAG, "Starting mass inference for component ${parentComponent.name} (${parentComponent.id})")
            Log.d(TAG, "Total measured mass: ${totalMeasuredMass}g")
            
            if (totalMeasuredMass < MIN_MEASUREMENT_PRECISION) {
                return@withContext InferenceResult.error("Total measured mass too small: ${totalMeasuredMass}g")
            }
            
            val childComponents = componentRepository.getChildComponents(parentComponent.id)
            
            if (childComponents.isEmpty()) {
                return@withContext InferenceResult.error("No child components found for inference")
            }
            
            val knownComponents = childComponents.filter { it.massGrams != null }
            val unknownComponents = childComponents.filter { it.massGrams == null }
            
            Log.d(TAG, "Child components: ${childComponents.size}, Known: ${knownComponents.size}, Unknown: ${unknownComponents.size}")
            
            if (unknownComponents.isEmpty()) {
                return@withContext handleNoUnknownComponents(childComponents, totalMeasuredMass)
            }
            
            val knownMass = knownComponents.sumOf { it.massGrams!!.toDouble() }.toFloat()
            val availableForUnknown = totalMeasuredMass - knownMass
            
            Log.d(TAG, "Known mass: ${knownMass}g, Available for unknown: ${availableForUnknown}g")
            
            if (availableForUnknown < 0) {
                return@withContext InferenceResult.error(
                    "Known component mass (${knownMass}g) exceeds total measured mass (${totalMeasuredMass}g)",
                    knownMass = knownMass,
                    totalMeasuredMass = totalMeasuredMass,
                    massDiscrepancy = abs(availableForUnknown)
                )
            }
            
            return@withContext when (unknownComponents.size) {
                1 -> inferSingleUnknown(unknownComponents.first(), availableForUnknown, totalMeasuredMass)
                else -> inferMultipleUnknowns(unknownComponents, availableForUnknown, totalMeasuredMass)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during mass inference", e)
            return@withContext InferenceResult.error("Mass inference failed: ${e.message}")
        }
    }
    
    /**
     * Infer mass for a single unknown component
     */
    private suspend fun inferSingleUnknown(
        unknownComponent: Component,
        availableForUnknown: Float,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val inferredMass = availableForUnknown
        val validation = validateInferredMass(unknownComponent, inferredMass)
        
        if (!validation.isValid) {
            return InferenceResult.warning(
                message = "Inferred mass may be unrealistic: ${inferredMass}g for ${unknownComponent.category}",
                inferredComponents = listOf(
                    InferredComponent(
                        component = unknownComponent,
                        inferredMass = inferredMass,
                        confidence = validation.confidence
                    )
                ),
                totalMeasuredMass = totalMeasuredMass,
                validationWarnings = validation.warnings
            )
        }
        
        return InferenceResult.success(
            message = "Successfully inferred mass for ${unknownComponent.name}: ${inferredMass}g",
            inferredComponents = listOf(
                InferredComponent(
                    component = unknownComponent,
                    inferredMass = inferredMass,
                    confidence = validation.confidence
                )
            ),
            totalMeasuredMass = totalMeasuredMass
        )
    }
    
    /**
     * Infer masses for multiple unknown components using distribution strategies
     */
    private suspend fun inferMultipleUnknowns(
        unknownComponents: List<Component>,
        availableForUnknown: Float,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val strategy = determineDistributionStrategy(unknownComponents, availableForUnknown)
        
        return when (strategy) {
            DistributionStrategy.CATEGORY_BASED -> distributeByCategory(unknownComponents, availableForUnknown, totalMeasuredMass)
            DistributionStrategy.EQUAL_DISTRIBUTION -> distributeEqually(unknownComponents, availableForUnknown, totalMeasuredMass)
            DistributionStrategy.VARIABLE_PRIORITY -> distributePrioritisedToVariable(unknownComponents, availableForUnknown, totalMeasuredMass)
        }
    }
    
    /**
     * Handle case where all components have known masses (validation scenario)
     */
    private fun handleNoUnknownComponents(
        childComponents: List<Component>,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val calculatedMass = childComponents.sumOf { it.massGrams!!.toDouble() }.toFloat()
        val discrepancy = abs(totalMeasuredMass - calculatedMass)
        val discrepancyPercent = (discrepancy / totalMeasuredMass) * 100
        
        return if (discrepancyPercent <= MAX_MEASUREMENT_ERROR_PERCENT) {
            InferenceResult.success(
                message = "Mass validation successful. Calculated: ${calculatedMass}g, Measured: ${totalMeasuredMass}g",
                inferredComponents = emptyList(),
                totalMeasuredMass = totalMeasuredMass,
                massDiscrepancy = discrepancy
            )
        } else {
            InferenceResult.warning(
                message = "Mass discrepancy detected. Calculated: ${calculatedMass}g, Measured: ${totalMeasuredMass}g",
                inferredComponents = emptyList(),
                totalMeasuredMass = totalMeasuredMass,
                massDiscrepancy = discrepancy,
                validationWarnings = listOf("Mass discrepancy exceeds ${MAX_MEASUREMENT_ERROR_PERCENT}%: ${discrepancyPercent.roundToInt()}%")
            )
        }
    }
    
    /**
     * Distribute mass based on component categories and defaults
     */
    private fun distributeByCategory(
        unknownComponents: List<Component>,
        availableForUnknown: Float,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val inferredComponents = mutableListOf<InferredComponent>()
        var remainingMass = availableForUnknown
        
        // First pass: Assign default masses to components with known defaults
        val componentsWithDefaults = unknownComponents.filter { DEFAULT_COMPONENT_MASSES.containsKey(it.category) }
        val componentsWithoutDefaults = unknownComponents.filter { !DEFAULT_COMPONENT_MASSES.containsKey(it.category) }
        
        for (component in componentsWithDefaults) {
            val defaultMass = DEFAULT_COMPONENT_MASSES[component.category]!!
            val assignedMass = min(defaultMass, remainingMass)
            
            inferredComponents.add(
                InferredComponent(
                    component = component,
                    inferredMass = assignedMass,
                    confidence = if (assignedMass == defaultMass) 0.8f else 0.5f
                )
            )
            
            remainingMass -= assignedMass
        }
        
        // Second pass: Distribute remaining mass to components without defaults
        if (remainingMass > 0 && componentsWithoutDefaults.isNotEmpty()) {
            val massPerComponent = remainingMass / componentsWithoutDefaults.size
            
            for (component in componentsWithoutDefaults) {
                inferredComponents.add(
                    InferredComponent(
                        component = component,
                        inferredMass = massPerComponent,
                        confidence = 0.4f // Lower confidence for equal distribution
                    )
                )
            }
        }
        
        // Validate all inferred masses
        val warnings = mutableListOf<String>()
        for (inferred in inferredComponents) {
            val validation = validateInferredMass(inferred.component, inferred.inferredMass)
            if (!validation.isValid) {
                warnings.addAll(validation.warnings)
            }
        }
        
        return if (warnings.isEmpty()) {
            InferenceResult.success(
                message = "Successfully distributed mass across ${unknownComponents.size} components using category defaults",
                inferredComponents = inferredComponents,
                totalMeasuredMass = totalMeasuredMass
            )
        } else {
            InferenceResult.warning(
                message = "Mass distributed but some values may be unrealistic",
                inferredComponents = inferredComponents,
                totalMeasuredMass = totalMeasuredMass,
                validationWarnings = warnings
            )
        }
    }
    
    /**
     * Distribute mass equally among unknown components
     */
    private fun distributeEqually(
        unknownComponents: List<Component>,
        availableForUnknown: Float,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val massPerComponent = availableForUnknown / unknownComponents.size
        val inferredComponents = unknownComponents.map { component ->
            InferredComponent(
                component = component,
                inferredMass = massPerComponent,
                confidence = 0.3f // Lower confidence for equal distribution
            )
        }
        
        return InferenceResult.warning(
            message = "Mass distributed equally among ${unknownComponents.size} components (${massPerComponent}g each)",
            inferredComponents = inferredComponents,
            totalMeasuredMass = totalMeasuredMass,
            validationWarnings = listOf("Equal distribution used - results may be inaccurate")
        )
    }
    
    /**
     * Prioritise variable mass components (filament) for mass assignment
     */
    private fun distributePrioritisedToVariable(
        unknownComponents: List<Component>,
        availableForUnknown: Float,
        totalMeasuredMass: Float
    ): InferenceResult {
        
        val variableComponents = unknownComponents.filter { it.variableMass }
        val fixedComponents = unknownComponents.filter { !it.variableMass }
        
        val inferredComponents = mutableListOf<InferredComponent>()
        var remainingMass = availableForUnknown
        
        // Assign default masses to fixed components first
        for (component in fixedComponents) {
            val defaultMass = DEFAULT_COMPONENT_MASSES[component.category] ?: 10.0f // Fallback
            val assignedMass = min(defaultMass, remainingMass)
            
            inferredComponents.add(
                InferredComponent(
                    component = component,
                    inferredMass = assignedMass,
                    confidence = 0.6f
                )
            )
            
            remainingMass -= assignedMass
        }
        
        // Assign remaining mass to variable components
        if (remainingMass > 0 && variableComponents.isNotEmpty()) {
            val massPerVariable = remainingMass / variableComponents.size
            
            for (component in variableComponents) {
                inferredComponents.add(
                    InferredComponent(
                        component = component,
                        inferredMass = massPerVariable,
                        confidence = 0.7f // Higher confidence for variable mass components
                    )
                )
            }
        }
        
        return InferenceResult.success(
            message = "Mass distributed with priority to variable components",
            inferredComponents = inferredComponents,
            totalMeasuredMass = totalMeasuredMass
        )
    }
    
    /**
     * Determine the best distribution strategy for multiple unknowns
     */
    private fun determineDistributionStrategy(
        unknownComponents: List<Component>,
        availableForUnknown: Float
    ): DistributionStrategy {
        
        // If we have defaults for most components, use category-based
        val componentsWithDefaults = unknownComponents.count { DEFAULT_COMPONENT_MASSES.containsKey(it.category) }
        if (componentsWithDefaults >= unknownComponents.size / 2) {
            return DistributionStrategy.CATEGORY_BASED
        }
        
        // If we have variable mass components, prioritise them
        val hasVariableComponents = unknownComponents.any { it.variableMass }
        if (hasVariableComponents) {
            return DistributionStrategy.VARIABLE_PRIORITY
        }
        
        // Fall back to equal distribution
        return DistributionStrategy.EQUAL_DISTRIBUTION
    }
    
    /**
     * Validate component masses against expected ranges
     */
    suspend fun validateComponentMasses(component: Component): ValidationResult = withContext(Dispatchers.Default) {
        
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // Validate the component itself
            val componentValidation = validateSingleComponent(component)
            errors.addAll(componentValidation.errors)
            warnings.addAll(componentValidation.warnings)
            
            // Validate all child components
            val childComponents = componentRepository.getChildComponents(component.id)
            for (child in childComponents) {
                val childValidation = validateSingleComponent(child)
                errors.addAll(childValidation.errors)
                warnings.addAll(childValidation.warnings)
            }
            
            // Validate hierarchical relationships
            val totalMass = componentRepository.getTotalMass(component.id)
            val componentMass = component.massGrams ?: 0f
            
            if (totalMass > 0 && componentMass > 0 && abs(totalMass - componentMass) > totalMass * 0.1f) {
                warnings.add("Significant discrepancy between component mass (${componentMass}g) and total calculated mass (${totalMass}g)")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during component validation", e)
            errors.add("Validation failed: ${e.message}")
        }
        
        return@withContext ValidationResult(
            isValid = errors.isEmpty(),
            confidence = if (errors.isEmpty() && warnings.isEmpty()) 1.0f else if (errors.isEmpty()) 0.7f else 0.0f,
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Suggest component adjustments based on mass measurements
     */
    suspend fun suggestComponentAdjustments(
        component: Component,
        measuredMass: Float
    ): List<ComponentSuggestion> = withContext(Dispatchers.Default) {
        
        val suggestions = mutableListOf<ComponentSuggestion>()
        
        try {
            val childComponents = componentRepository.getChildComponents(component.id)
            val totalCalculatedMass = componentRepository.getTotalMass(component.id)
            val discrepancy = measuredMass - totalCalculatedMass
            val discrepancyPercent = if (totalCalculatedMass > 0) abs(discrepancy) / totalCalculatedMass * 100 else 0f
            
            Log.d(TAG, "Generating suggestions for ${component.name}: Measured=${measuredMass}g, Calculated=${totalCalculatedMass}g, Discrepancy=${discrepancy}g")
            
            when {
                discrepancy > 10.0f -> {
                    suggestions.add(
                        ComponentSuggestion(
                            type = SuggestionType.ADD_COMPONENT,
                            description = "Consider adding missing components. Measured mass is ${discrepancy}g higher than calculated.",
                            confidence = 0.7f,
                            suggestedMass = discrepancy
                        )
                    )
                }
                
                discrepancy < -10.0f -> {
                    suggestions.add(
                        ComponentSuggestion(
                            type = SuggestionType.REMOVE_COMPONENT,
                            description = "Consider removing or adjusting components. Measured mass is ${abs(discrepancy)}g lower than calculated.",
                            confidence = 0.6f
                        )
                    )
                }
                
                discrepancyPercent > MAX_MEASUREMENT_ERROR_PERCENT -> {
                    suggestions.add(
                        ComponentSuggestion(
                            type = SuggestionType.RECALIBRATE,
                            description = "Mass discrepancy of ${discrepancyPercent.roundToInt()}% detected. Consider recalibrating scale or checking component masses.",
                            confidence = 0.5f
                        )
                    )
                }
            }
            
            // Suggest mass inference if unknown components exist
            val unknownComponents = childComponents.filter { it.massGrams == null }
            if (unknownComponents.isNotEmpty()) {
                suggestions.add(
                    ComponentSuggestion(
                        type = SuggestionType.INFER_MASS,
                        description = "Infer masses for ${unknownComponents.size} unknown component(s) using current measurement.",
                        confidence = 0.8f,
                        suggestedComponentIds = unknownComponents.map { it.id }
                    )
                )
            }
            
            // Suggest updating variable mass components
            val variableComponents = childComponents.filter { it.variableMass && it.massGrams != null }
            if (variableComponents.isNotEmpty() && abs(discrepancy) > 1.0f) {
                suggestions.add(
                    ComponentSuggestion(
                        type = SuggestionType.UPDATE_VARIABLE_MASS,
                        description = "Update variable component masses to match measurement.",
                        confidence = 0.9f,
                        suggestedComponentIds = variableComponents.map { it.id }
                    )
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating suggestions", e)
            suggestions.add(
                ComponentSuggestion(
                    type = SuggestionType.ERROR,
                    description = "Unable to generate suggestions: ${e.message}",
                    confidence = 0.0f
                )
            )
        }
        
        return@withContext suggestions
    }
    
    /**
     * Integrate BLE scale reading for real-time mass inference
     */
    suspend fun processScaleReading(
        parentComponent: Component,
        scaleReading: ScaleReading
    ): ScaleInferenceResult = withContext(Dispatchers.Default) {
        
        try {
            if (!scaleReading.isValidForCapture()) {
                return@withContext ScaleInferenceResult.error(
                    "Scale reading not valid for capture: ${scaleReading.getDisplayWeightWithValidation()}",
                    scaleReading
                )
            }
            
            // Convert scale reading to grams
            val measurementInGrams = scaleReading.unit.toGrams(scaleReading.weight)
            
            // Perform mass inference
            val inferenceResult = inferComponentMass(parentComponent, measurementInGrams)
            
            // Generate suggestions
            val suggestions = suggestComponentAdjustments(parentComponent, measurementInGrams)
            
            return@withContext ScaleInferenceResult.success(
                scaleReading = scaleReading,
                inferenceResult = inferenceResult,
                suggestions = suggestions
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing scale reading", e)
            return@withContext ScaleInferenceResult.error("Failed to process scale reading: ${e.message}", scaleReading)
        }
    }
    
    /**
     * Apply inferred masses to components and save
     */
    suspend fun applyInferredMasses(inferenceResult: InferenceResult): ApplyResult = withContext(Dispatchers.IO) {
        
        if (!inferenceResult.isSuccess) {
            return@withContext ApplyResult.error("Cannot apply masses from unsuccessful inference")
        }
        
        try {
            var appliedCount = 0
            val failures = mutableListOf<String>()
            
            for (inferred in inferenceResult.inferredComponents) {
                try {
                    val updatedComponent = inferred.component.copy(
                        massGrams = inferred.inferredMass,
                        inferredMass = true,
                        lastUpdated = LocalDateTime.now()
                    )
                    
                    componentRepository.saveComponent(updatedComponent)
                    appliedCount++
                    
                    Log.d(TAG, "Applied inferred mass to ${updatedComponent.name}: ${inferred.inferredMass}g")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply mass to component ${inferred.component.id}", e)
                    failures.add("${inferred.component.name}: ${e.message}")
                }
            }
            
            return@withContext if (failures.isEmpty()) {
                ApplyResult.success("Successfully applied masses to $appliedCount component(s)")
            } else {
                ApplyResult.warning(
                    "Applied masses to $appliedCount component(s), but ${failures.size} failed",
                    failures
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error applying inferred masses", e)
            return@withContext ApplyResult.error("Failed to apply masses: ${e.message}")
        }
    }
    
    // === Private Helper Methods ===
    
    private fun validateInferredMass(component: Component, inferredMass: Float): MassValidationResult {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        if (inferredMass < 0) {
            errors.add("${component.name}: Negative mass not allowed")
            return MassValidationResult(false, 0.0f, errors, warnings)
        }
        
        val range = COMPONENT_MASS_RANGES[component.category] ?: COMPONENT_MASS_RANGES["general"]!!
        
        if (inferredMass !in range) {
            val message = "${component.name}: Mass ${inferredMass}g outside expected range ${range.start}-${range.endInclusive}g for category '${component.category}'"
            if (inferredMass < range.start * 0.1f || inferredMass > range.endInclusive * 10f) {
                errors.add(message)
            } else {
                warnings.add(message)
            }
        }
        
        val confidence = when {
            errors.isNotEmpty() -> 0.0f
            warnings.isNotEmpty() -> 0.5f
            inferredMass in (range.start * 0.5f)..(range.endInclusive * 2f) -> 0.9f
            else -> 0.7f
        }
        
        return MassValidationResult(errors.isEmpty(), confidence, errors, warnings)
    }
    
    private fun validateSingleComponent(component: Component): ComponentValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        val mass = component.massGrams
        if (mass != null) {
            if (mass < 0) {
                errors.add("${component.name}: Negative mass not allowed")
            } else {
                val validation = validateInferredMass(component, mass)
                errors.addAll(validation.errors)
                warnings.addAll(validation.warnings)
            }
        }
        
        if (component.variableMass && component.fullMassGrams != null) {
            if (mass != null && component.fullMassGrams < mass) {
                errors.add("${component.name}: Full mass (${component.fullMassGrams}g) cannot be less than current mass (${mass}g)")
            }
        }
        
        return ComponentValidationResult(errors, warnings)
    }
}

// === Data Classes ===

/**
 * Result of mass inference operation
 */
sealed class InferenceResult {
    abstract val isSuccess: Boolean
    abstract val message: String
    abstract val inferredComponents: List<InferredComponent>
    abstract val totalMeasuredMass: Float
    abstract val knownMass: Float?
    abstract val massDiscrepancy: Float?
    abstract val validationWarnings: List<String>
    
    data class Success(
        override val message: String,
        override val inferredComponents: List<InferredComponent>,
        override val totalMeasuredMass: Float,
        override val knownMass: Float? = null,
        override val massDiscrepancy: Float? = null,
        override val validationWarnings: List<String> = emptyList()
    ) : InferenceResult() {
        override val isSuccess = true
    }
    
    data class Warning(
        override val message: String,
        override val inferredComponents: List<InferredComponent>,
        override val totalMeasuredMass: Float,
        override val knownMass: Float? = null,
        override val massDiscrepancy: Float? = null,
        override val validationWarnings: List<String> = emptyList()
    ) : InferenceResult() {
        override val isSuccess = true
    }
    
    data class Error(
        override val message: String,
        override val knownMass: Float? = null,
        override val totalMeasuredMass: Float = 0f,
        override val massDiscrepancy: Float? = null,
        override val validationWarnings: List<String> = emptyList()
    ) : InferenceResult() {
        override val isSuccess = false
        override val inferredComponents = emptyList<InferredComponent>()
    }
    
    companion object {
        fun success(
            message: String,
            inferredComponents: List<InferredComponent>,
            totalMeasuredMass: Float,
            knownMass: Float? = null,
            massDiscrepancy: Float? = null
        ) = Success(message, inferredComponents, totalMeasuredMass, knownMass, massDiscrepancy)
        
        fun warning(
            message: String,
            inferredComponents: List<InferredComponent>,
            totalMeasuredMass: Float,
            knownMass: Float? = null,
            massDiscrepancy: Float? = null,
            validationWarnings: List<String> = emptyList()
        ) = Warning(message, inferredComponents, totalMeasuredMass, knownMass, massDiscrepancy, validationWarnings)
        
        fun error(
            message: String,
            knownMass: Float? = null,
            totalMeasuredMass: Float = 0f,
            massDiscrepancy: Float? = null
        ) = Error(message, knownMass, totalMeasuredMass, massDiscrepancy)
    }
}

/**
 * Component with inferred mass and confidence level
 */
data class InferredComponent(
    val component: Component,
    val inferredMass: Float,
    val confidence: Float // 0.0 to 1.0
)

/**
 * Validation result for component masses
 */
data class ValidationResult(
    val isValid: Boolean,
    val confidence: Float,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Suggestion for component adjustments
 */
data class ComponentSuggestion(
    val type: SuggestionType,
    val description: String,
    val confidence: Float,
    val suggestedMass: Float? = null,
    val suggestedComponentIds: List<String> = emptyList()
)

/**
 * Types of component suggestions
 */
enum class SuggestionType {
    ADD_COMPONENT,
    REMOVE_COMPONENT,
    INFER_MASS,
    UPDATE_VARIABLE_MASS,
    RECALIBRATE,
    ERROR
}

/**
 * Result of BLE scale reading integration
 */
sealed class ScaleInferenceResult {
    abstract val scaleReading: ScaleReading?
    abstract val isSuccess: Boolean
    
    data class Success(
        override val scaleReading: ScaleReading,
        val inferenceResult: InferenceResult,
        val suggestions: List<ComponentSuggestion>
    ) : ScaleInferenceResult() {
        override val isSuccess = true
    }
    
    data class Error(
        val message: String,
        override val scaleReading: ScaleReading?
    ) : ScaleInferenceResult() {
        override val isSuccess = false
    }
    
    companion object {
        fun success(
            scaleReading: ScaleReading,
            inferenceResult: InferenceResult,
            suggestions: List<ComponentSuggestion>
        ) = Success(scaleReading, inferenceResult, suggestions)
        
        fun error(message: String, scaleReading: ScaleReading? = null) = Error(message, scaleReading)
    }
}

/**
 * Result of applying inferred masses to components
 */
sealed class ApplyResult {
    abstract val isSuccess: Boolean
    abstract val message: String
    
    data class Success(override val message: String) : ApplyResult() {
        override val isSuccess = true
    }
    
    data class Warning(override val message: String, val failures: List<String>) : ApplyResult() {
        override val isSuccess = true
    }
    
    data class Error(override val message: String) : ApplyResult() {
        override val isSuccess = false
    }
    
    companion object {
        fun success(message: String) = Success(message)
        fun warning(message: String, failures: List<String>) = Warning(message, failures)
        fun error(message: String) = Error(message)
    }
}

// === Internal Helper Classes ===

/**
 * Strategy for distributing mass among multiple unknown components
 */
private enum class DistributionStrategy {
    CATEGORY_BASED,      // Use component category defaults
    EQUAL_DISTRIBUTION,  // Distribute equally
    VARIABLE_PRIORITY    // Prioritise variable mass components
}

/**
 * Internal validation result for individual components
 */
private data class ComponentValidationResult(
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Internal mass validation result
 */
private data class MassValidationResult(
    val isValid: Boolean,
    val confidence: Float,
    val errors: List<String>,
    val warnings: List<String>
)