package com.bscan.logic

import com.bscan.model.*
import kotlin.math.abs

/**
 * Service for performing weight calculations, inferences, and validations
 * for spool weight management features.
 */
class WeightCalculationService {
    
    /**
     * Calculate actual filament weight from measured weight and spool configuration
     */
    fun calculateFilamentWeight(
        measuredWeight: Float,
        spoolConfiguration: SpoolConfiguration,
        components: List<SpoolComponent>,
        measurementType: MeasurementType = MeasurementType.FULL_WEIGHT
    ): WeightCalculationResult {
        
        if (measuredWeight <= 0f) {
            return WeightCalculationResult(
                success = false,
                errorMessage = "Measured weight must be greater than 0"
            )
        }
        
        val spoolWeight = calculateTotalSpoolWeight(spoolConfiguration, components)
        if (spoolWeight < 0f) {
            return WeightCalculationResult(
                success = false,
                errorMessage = "Unable to calculate spool weight - missing component data"
            )
        }
        
        return when (measurementType) {
            MeasurementType.FULL_WEIGHT -> {
                val filamentWeight = measuredWeight - spoolWeight
                if (filamentWeight < 0f) {
                    WeightCalculationResult(
                        success = false,
                        errorMessage = "Calculated filament weight is negative - check spool configuration"
                    )
                } else {
                    WeightCalculationResult(
                        success = true,
                        filamentWeightGrams = filamentWeight,
                        spoolWeightGrams = spoolWeight,
                        totalWeightGrams = measuredWeight
                    )
                }
            }
            MeasurementType.EMPTY_WEIGHT -> {
                WeightCalculationResult(
                    success = true,
                    filamentWeightGrams = 0f,
                    spoolWeightGrams = measuredWeight,
                    totalWeightGrams = measuredWeight
                )
            }
        }
    }
    
    /**
     * Calculate total weight of a spool configuration
     */
    fun calculateTotalSpoolWeight(
        spoolConfiguration: SpoolConfiguration,
        components: List<SpoolComponent>
    ): Float {
        return spoolConfiguration.components.sumOf { componentId ->
            components.find { it.id == componentId }?.weightGrams?.toDouble() ?: 0.0
        }.toFloat()
    }
    
    /**
     * Infer unknown component weight from known measurements
     */
    fun inferComponentWeight(
        request: WeightInferenceRequest,
        components: List<SpoolComponent>
    ): WeightCalculationResult {
        
        if (request.knownTotalWeight <= 0f) {
            return WeightCalculationResult(
                success = false,
                errorMessage = "Known total weight must be greater than 0"
            )
        }
        
        // Calculate weight of known components
        val knownComponentsWeight = request.knownComponents.sumOf { componentId ->
            components.find { it.id == componentId }?.weightGrams?.toDouble() ?: 0.0
        }.toFloat()
        
        // Calculate unknown component weight
        val unknownWeight = when {
            request.knownFilamentWeight != null -> {
                // We know filament weight: unknown = total - filament - known components
                request.knownTotalWeight - request.knownFilamentWeight - knownComponentsWeight
            }
            else -> {
                // We don't know filament weight: unknown = total - known components
                request.knownTotalWeight - knownComponentsWeight
            }
        }
        
        if (unknownWeight < 0f) {
            return WeightCalculationResult(
                success = false,
                errorMessage = "Inferred component weight is negative - check your measurements"
            )
        }
        
        val filamentWeight = request.knownFilamentWeight ?: 0f
        
        return WeightCalculationResult(
            success = true,
            filamentWeightGrams = filamentWeight,
            spoolWeightGrams = unknownWeight + knownComponentsWeight,
            totalWeightGrams = request.knownTotalWeight
        )
    }
    
    /**
     * Verify weight measurement against expected values
     */
    fun verifyWeightMeasurement(
        measurement: FilamentWeightMeasurement,
        expectedFilamentWeight: Float?,
        spoolConfiguration: SpoolConfiguration,
        components: List<SpoolComponent>,
        tolerancePercent: Float = 5f
    ): WeightVerificationResult {
        
        val calculationResult = calculateFilamentWeight(
            measurement.measuredWeightGrams,
            spoolConfiguration,
            components,
            measurement.measurementType
        )
        
        if (!calculationResult.success) {
            return WeightVerificationResult(
                isValid = false,
                deviation = 0f,
                message = calculationResult.errorMessage ?: "Calculation failed"
            )
        }
        
        // If we have expected filament weight, verify against it
        expectedFilamentWeight?.let { expected ->
            val actualFilament = calculationResult.filamentWeightGrams
            val deviation = abs(actualFilament - expected) / expected * 100f
            
            return WeightVerificationResult(
                isValid = deviation <= tolerancePercent,
                deviation = deviation,
                message = when {
                    deviation <= tolerancePercent -> "Weight measurement is within tolerance"
                    deviation > tolerancePercent * 2 -> "Weight measurement significantly differs from expected"
                    else -> "Weight measurement differs from expected by ${String.format("%.1f", deviation)}%"
                },
                expectedWeight = expected,
                actualWeight = actualFilament
            )
        }
        
        // No expected weight to verify against
        return WeightVerificationResult(
            isValid = true,
            deviation = 0f,
            message = "Measurement recorded successfully",
            actualWeight = calculationResult.filamentWeightGrams
        )
    }
    
    /**
     * Suggest optimal spool configuration based on measured weight
     */
    fun suggestSpoolConfiguration(
        measuredWeight: Float,
        expectedFilamentWeight: Float?,
        availableConfigurations: List<SpoolConfiguration>,
        components: List<SpoolComponent>
    ): List<SpoolConfigurationSuggestion> {
        
        val suggestions = mutableListOf<SpoolConfigurationSuggestion>()
        
        for (config in availableConfigurations) {
            val spoolWeight = calculateTotalSpoolWeight(config, components)
            if (spoolWeight < 0f) continue // Skip if missing component data
            
            val calculatedFilamentWeight = measuredWeight - spoolWeight
            if (calculatedFilamentWeight < 0f) continue // Skip if impossible
            
            val score = when {
                expectedFilamentWeight != null -> {
                    val deviation = abs(calculatedFilamentWeight - expectedFilamentWeight) / expectedFilamentWeight * 100f
                    100f - deviation.coerceAtMost(100f)
                }
                else -> {
                    // Score based on reasonable filament weight ranges
                    when {
                        calculatedFilamentWeight in 450f..550f -> 95f // ~0.5kg
                        calculatedFilamentWeight in 700f..800f -> 95f // ~0.75kg
                        calculatedFilamentWeight in 950f..1050f -> 95f // ~1kg
                        calculatedFilamentWeight in 400f..1100f -> 80f // Reasonable range
                        else -> 50f // Outside typical ranges
                    }
                }
            }
            
            suggestions.add(
                SpoolConfigurationSuggestion(
                    configuration = config,
                    calculatedFilamentWeight = calculatedFilamentWeight,
                    spoolWeight = spoolWeight,
                    confidenceScore = score,
                    deviation = expectedFilamentWeight?.let { 
                        abs(calculatedFilamentWeight - it) / it * 100f 
                    }
                )
            )
        }
        
        return suggestions.sortedByDescending { it.confidenceScore }
    }
    
    /**
     * Convert between weight units
     */
    fun convertWeight(weightGrams: Float, targetUnit: WeightUnit): Float {
        return when (targetUnit) {
            WeightUnit.GRAMS -> weightGrams
            WeightUnit.OUNCES -> weightGrams * 0.035274f
            WeightUnit.POUNDS -> weightGrams * 0.00220462f
            WeightUnit.KILOGRAMS -> weightGrams / 1000f
        }
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
}

/**
 * Result of weight verification operations
 */
data class WeightVerificationResult(
    val isValid: Boolean,
    val deviation: Float,
    val message: String,
    val expectedWeight: Float? = null,
    val actualWeight: Float? = null
)

/**
 * Suggestion for spool configuration based on measurements
 */
data class SpoolConfigurationSuggestion(
    val configuration: SpoolConfiguration,
    val calculatedFilamentWeight: Float,
    val spoolWeight: Float,
    val confidenceScore: Float,
    val deviation: Float? = null
)

/**
 * Weight units for display and conversion
 */
enum class WeightUnit {
    GRAMS,
    OUNCES, 
    POUNDS,
    KILOGRAMS
}