package com.bscan.service

import kotlin.math.PI
import kotlin.math.pow

/**
 * Service for converting between different measurement units.
 * Supports weight, volume, length, and percentage-based measurements with conversion paths.
 */
class UnitConversionService {
    
    /**
     * Convert between weight units
     */
    fun convertWeight(value: Float, fromUnit: WeightUnit, toUnit: WeightUnit): Float {
        if (fromUnit == toUnit) return value
        
        // Convert to grams as base unit, then to target unit
        val grams = when (fromUnit) {
            WeightUnit.GRAMS -> value
            WeightUnit.KILOGRAMS -> value * 1000f
            WeightUnit.OUNCES -> value * 28.3495f
            WeightUnit.POUNDS -> value * 453.592f
            WeightUnit.MILLIGRAMS -> value / 1000f
        }
        
        return when (toUnit) {
            WeightUnit.GRAMS -> grams
            WeightUnit.KILOGRAMS -> grams / 1000f
            WeightUnit.OUNCES -> grams / 28.3495f
            WeightUnit.POUNDS -> grams / 453.592f
            WeightUnit.MILLIGRAMS -> grams * 1000f
        }
    }
    
    /**
     * Convert between volume units
     */
    fun convertVolume(value: Float, fromUnit: VolumeUnit, toUnit: VolumeUnit): Float {
        if (fromUnit == toUnit) return value
        
        // Convert to millilitres as base unit, then to target unit
        val millilitres = when (fromUnit) {
            VolumeUnit.MILLILITRES -> value
            VolumeUnit.LITRES -> value * 1000f
            VolumeUnit.CUBIC_CENTIMETRES -> value  // 1 ml = 1 cm³
            VolumeUnit.FLUID_OUNCES -> value * 29.5735f
            VolumeUnit.CUPS -> value * 236.588f
            VolumeUnit.CUBIC_METRES -> value * 1_000_000f
        }
        
        return when (toUnit) {
            VolumeUnit.MILLILITRES -> millilitres
            VolumeUnit.LITRES -> millilitres / 1000f
            VolumeUnit.CUBIC_CENTIMETRES -> millilitres
            VolumeUnit.FLUID_OUNCES -> millilitres / 29.5735f
            VolumeUnit.CUPS -> millilitres / 236.588f
            VolumeUnit.CUBIC_METRES -> millilitres / 1_000_000f
        }
    }
    
    /**
     * Convert between length units
     */
    fun convertLength(value: Float, fromUnit: LengthUnit, toUnit: LengthUnit): Float {
        if (fromUnit == toUnit) return value
        
        // Convert to metres as base unit, then to target unit
        val metres = when (fromUnit) {
            LengthUnit.METRES -> value
            LengthUnit.CENTIMETRES -> value / 100f
            LengthUnit.MILLIMETRES -> value / 1000f
            LengthUnit.KILOMETRES -> value * 1000f
            LengthUnit.INCHES -> value * 0.0254f
            LengthUnit.FEET -> value * 0.3048f
            LengthUnit.YARDS -> value * 0.9144f
        }
        
        return when (toUnit) {
            LengthUnit.METRES -> metres
            LengthUnit.CENTIMETRES -> metres * 100f
            LengthUnit.MILLIMETRES -> metres * 1000f
            LengthUnit.KILOMETRES -> metres / 1000f
            LengthUnit.INCHES -> metres / 0.0254f
            LengthUnit.FEET -> metres / 0.3048f
            LengthUnit.YARDS -> metres / 0.9144f
        }
    }
    
    /**
     * Convert weight to volume using material density
     */
    fun convertWeightToVolume(
        weight: Float, 
        weightUnit: WeightUnit, 
        targetVolumeUnit: VolumeUnit,
        densityGramsPerCm3: Float
    ): ConversionResult {
        if (densityGramsPerCm3 <= 0) {
            return ConversionResult(
                success = false,
                error = "Density must be positive"
            )
        }
        
        val weightInGrams = convertWeight(weight, weightUnit, WeightUnit.GRAMS)
        val volumeInCm3 = weightInGrams / densityGramsPerCm3
        val volumeInMl = volumeInCm3 // 1 cm³ = 1 ml
        val targetVolume = convertVolume(volumeInMl, VolumeUnit.MILLILITRES, targetVolumeUnit)
        
        return ConversionResult(
            success = true,
            value = targetVolume,
            fromUnit = "${weightUnit.symbol} (density: ${densityGramsPerCm3}g/cm³)",
            toUnit = targetVolumeUnit.symbol,
            conversionPath = "Weight → Volume via density"
        )
    }
    
    /**
     * Convert volume to weight using material density
     */
    fun convertVolumeToWeight(
        volume: Float,
        volumeUnit: VolumeUnit,
        targetWeightUnit: WeightUnit,
        densityGramsPerCm3: Float
    ): ConversionResult {
        if (densityGramsPerCm3 <= 0) {
            return ConversionResult(
                success = false,
                error = "Density must be positive"
            )
        }
        
        val volumeInMl = convertVolume(volume, volumeUnit, VolumeUnit.MILLILITRES)
        val volumeInCm3 = volumeInMl // 1 ml = 1 cm³
        val weightInGrams = volumeInCm3 * densityGramsPerCm3
        val targetWeight = convertWeight(weightInGrams.toFloat(), WeightUnit.GRAMS, targetWeightUnit)
        
        return ConversionResult(
            success = true,
            value = targetWeight,
            fromUnit = "${volumeUnit.symbol} (density: ${densityGramsPerCm3}g/cm³)",
            toUnit = targetWeightUnit.symbol,
            conversionPath = "Volume → Weight via density"
        )
    }
    
    /**
     * Convert length to weight for filament (using diameter and density)
     */
    fun convertFilamentLengthToWeight(
        length: Float,
        lengthUnit: LengthUnit,
        targetWeightUnit: WeightUnit,
        diameterMm: Float,
        densityGramsPerCm3: Float
    ): ConversionResult {
        if (diameterMm <= 0 || densityGramsPerCm3 <= 0) {
            return ConversionResult(
                success = false,
                error = "Diameter and density must be positive"
            )
        }
        
        val lengthInMm = convertLength(length, lengthUnit, LengthUnit.MILLIMETRES)
        val radiusMm = diameterMm / 2f
        val crossSectionalAreaMm2 = PI * (radiusMm.pow(2))
        val volumeMm3 = crossSectionalAreaMm2 * lengthInMm
        val volumeCm3 = volumeMm3 / 1000f // mm³ to cm³
        val weightInGrams = volumeCm3 * densityGramsPerCm3
        val targetWeight = convertWeight(weightInGrams.toFloat(), WeightUnit.GRAMS, targetWeightUnit)
        
        return ConversionResult(
            success = true,
            value = targetWeight.toFloat(),
            fromUnit = "${lengthUnit.symbol} (⌀${diameterMm}mm, ${densityGramsPerCm3}g/cm³)",
            toUnit = targetWeightUnit.symbol,
            conversionPath = "Length → Volume → Weight via diameter and density"
        )
    }
    
    /**
     * Convert weight to filament length (using diameter and density)
     */
    fun convertWeightToFilamentLength(
        weight: Float,
        weightUnit: WeightUnit,
        targetLengthUnit: LengthUnit,
        diameterMm: Float,
        densityGramsPerCm3: Float
    ): ConversionResult {
        if (diameterMm <= 0 || densityGramsPerCm3 <= 0) {
            return ConversionResult(
                success = false,
                error = "Diameter and density must be positive"
            )
        }
        
        val weightInGrams = convertWeight(weight, weightUnit, WeightUnit.GRAMS)
        val volumeCm3 = weightInGrams / densityGramsPerCm3
        val volumeMm3 = volumeCm3 * 1000f // cm³ to mm³
        val radiusMm = diameterMm / 2f
        val crossSectionalAreaMm2 = PI * (radiusMm.pow(2))
        val lengthInMm = volumeMm3 / crossSectionalAreaMm2
        val targetLength = convertLength(lengthInMm.toFloat(), LengthUnit.MILLIMETRES, targetLengthUnit)
        
        return ConversionResult(
            success = true,
            value = targetLength,
            fromUnit = "${weightUnit.symbol} (⌀${diameterMm}mm, ${densityGramsPerCm3}g/cm³)",
            toUnit = targetLengthUnit.symbol,
            conversionPath = "Weight → Volume → Length via diameter and density"
        )
    }
    
    /**
     * Convert percentage to absolute value
     */
    fun convertPercentageToAbsolute(
        percentage: Float,
        totalValue: Float,
        unit: String
    ): ConversionResult {
        if (percentage < 0 || percentage > 100) {
            return ConversionResult(
                success = false,
                error = "Percentage must be between 0 and 100"
            )
        }
        
        val absoluteValue = totalValue * (percentage / 100f)
        
        return ConversionResult(
            success = true,
            value = absoluteValue,
            fromUnit = "%",
            toUnit = unit,
            conversionPath = "Percentage → Absolute value"
        )
    }
    
    /**
     * Convert absolute value to percentage
     */
    fun convertAbsoluteToPercentage(
        value: Float,
        totalValue: Float,
        unit: String
    ): ConversionResult {
        if (totalValue <= 0) {
            return ConversionResult(
                success = false,
                error = "Total value must be positive"
            )
        }
        
        val percentage = (value / totalValue) * 100f
        
        return ConversionResult(
            success = true,
            value = percentage,
            fromUnit = unit,
            toUnit = "%",
            conversionPath = "Absolute value → Percentage"
        )
    }
    
    /**
     * Get material density for common 3D printing materials
     */
    fun getMaterialDensity(materialType: String): Float? {
        return when (materialType.uppercase()) {
            "PLA" -> 1.24f
            "ABS" -> 1.04f
            "PETG" -> 1.27f
            "ASA" -> 1.05f
            "NYLON" -> 1.14f
            "PC", "POLYCARBONATE" -> 1.20f
            "PLA+" -> 1.25f
            "ABS+" -> 1.05f
            "TPU" -> 1.20f
            "WOOD", "PLA_WOOD" -> 1.28f
            "METAL", "PLA_METAL" -> 1.40f
            else -> null
        }
    }
    
    /**
     * Get common filament diameter
     */
    fun getStandardFilamentDiameter(): Float = 1.75f // Most common diameter
    
    /**
     * Find the best unit conversion path between two measurement types
     */
    fun findConversionPath(
        fromValue: Float,
        fromUnit: String,
        toUnit: String,
        materialContext: MaterialContext? = null
    ): ConversionResult {
        // Parse units to determine types
        val fromMeasurement = parseUnit(fromUnit)
        val toMeasurement = parseUnit(toUnit)
        
        if (fromMeasurement == null || toMeasurement == null) {
            return ConversionResult(
                success = false,
                error = "Unable to parse units: $fromUnit → $toUnit"
            )
        }
        
        // Direct conversion within same measurement type
        if (fromMeasurement.type == toMeasurement.type) {
            return when (fromMeasurement.type) {
                MeasurementType.WEIGHT -> {
                    val converted = convertWeight(fromValue, fromMeasurement.weightUnit!!, toMeasurement.weightUnit!!)
                    ConversionResult(true, converted, fromUnit, toUnit, "Direct weight conversion")
                }
                MeasurementType.VOLUME -> {
                    val converted = convertVolume(fromValue, fromMeasurement.volumeUnit!!, toMeasurement.volumeUnit!!)
                    ConversionResult(true, converted, fromUnit, toUnit, "Direct volume conversion")
                }
                MeasurementType.LENGTH -> {
                    val converted = convertLength(fromValue, fromMeasurement.lengthUnit!!, toMeasurement.lengthUnit!!)
                    ConversionResult(true, converted, fromUnit, toUnit, "Direct length conversion")
                }
                MeasurementType.PERCENTAGE -> {
                    ConversionResult(false, error = "Cannot convert between percentage units without context")
                }
            }
        }
        
        // Cross-type conversions require material context
        if (materialContext == null) {
            return ConversionResult(
                success = false,
                error = "Material context required for cross-unit conversion"
            )
        }
        
        // Weight ↔ Volume conversions
        if (fromMeasurement.type == MeasurementType.WEIGHT && toMeasurement.type == MeasurementType.VOLUME) {
            return convertWeightToVolume(
                fromValue,
                fromMeasurement.weightUnit!!,
                toMeasurement.volumeUnit!!,
                materialContext.densityGramsPerCm3
            )
        }
        
        if (fromMeasurement.type == MeasurementType.VOLUME && toMeasurement.type == MeasurementType.WEIGHT) {
            return convertVolumeToWeight(
                fromValue,
                fromMeasurement.volumeUnit!!,
                toMeasurement.weightUnit!!,
                materialContext.densityGramsPerCm3
            )
        }
        
        // Weight ↔ Length conversions (for filament)
        if (fromMeasurement.type == MeasurementType.WEIGHT && toMeasurement.type == MeasurementType.LENGTH) {
            return convertWeightToFilamentLength(
                fromValue,
                fromMeasurement.weightUnit!!,
                toMeasurement.lengthUnit!!,
                materialContext.diameterMm,
                materialContext.densityGramsPerCm3
            )
        }
        
        if (fromMeasurement.type == MeasurementType.LENGTH && toMeasurement.type == MeasurementType.WEIGHT) {
            return convertFilamentLengthToWeight(
                fromValue,
                fromMeasurement.lengthUnit!!,
                toMeasurement.weightUnit!!,
                materialContext.diameterMm,
                materialContext.densityGramsPerCm3
            )
        }
        
        return ConversionResult(
            success = false,
            error = "No conversion path found from ${fromMeasurement.type} to ${toMeasurement.type}"
        )
    }
    
    /**
     * Parse a unit string to determine measurement type and specific unit
     */
    private fun parseUnit(unitString: String): ParsedMeasurement? {
        return when (unitString.lowercase()) {
            "g", "grams" -> ParsedMeasurement(MeasurementType.WEIGHT, weightUnit = WeightUnit.GRAMS)
            "kg", "kilograms" -> ParsedMeasurement(MeasurementType.WEIGHT, weightUnit = WeightUnit.KILOGRAMS)
            "oz", "ounces" -> ParsedMeasurement(MeasurementType.WEIGHT, weightUnit = WeightUnit.OUNCES)
            "lb", "lbs", "pounds" -> ParsedMeasurement(MeasurementType.WEIGHT, weightUnit = WeightUnit.POUNDS)
            "mg", "milligrams" -> ParsedMeasurement(MeasurementType.WEIGHT, weightUnit = WeightUnit.MILLIGRAMS)
            
            "ml", "millilitres", "milliliters" -> ParsedMeasurement(MeasurementType.VOLUME, volumeUnit = VolumeUnit.MILLILITRES)
            "l", "litres", "liters" -> ParsedMeasurement(MeasurementType.VOLUME, volumeUnit = VolumeUnit.LITRES)
            "cm³", "cm3", "cubic_centimetres" -> ParsedMeasurement(MeasurementType.VOLUME, volumeUnit = VolumeUnit.CUBIC_CENTIMETRES)
            "fl_oz", "fluid_ounces" -> ParsedMeasurement(MeasurementType.VOLUME, volumeUnit = VolumeUnit.FLUID_OUNCES)
            
            "m", "metres", "meters" -> ParsedMeasurement(MeasurementType.LENGTH, lengthUnit = LengthUnit.METRES)
            "cm", "centimetres", "centimeters" -> ParsedMeasurement(MeasurementType.LENGTH, lengthUnit = LengthUnit.CENTIMETRES)
            "mm", "millimetres", "millimeters" -> ParsedMeasurement(MeasurementType.LENGTH, lengthUnit = LengthUnit.MILLIMETRES)
            "in", "inches" -> ParsedMeasurement(MeasurementType.LENGTH, lengthUnit = LengthUnit.INCHES)
            "ft", "feet" -> ParsedMeasurement(MeasurementType.LENGTH, lengthUnit = LengthUnit.FEET)
            
            "%", "percent", "percentage" -> ParsedMeasurement(MeasurementType.PERCENTAGE)
            
            else -> null
        }
    }
}

// Enum definitions
enum class WeightUnit(val symbol: String) {
    GRAMS("g"),
    KILOGRAMS("kg"),
    OUNCES("oz"),
    POUNDS("lb"),
    MILLIGRAMS("mg")
}

enum class VolumeUnit(val symbol: String) {
    MILLILITRES("ml"),
    LITRES("l"),
    CUBIC_CENTIMETRES("cm³"),
    FLUID_OUNCES("fl oz"),
    CUPS("cups"),
    CUBIC_METRES("m³")
}

enum class LengthUnit(val symbol: String) {
    METRES("m"),
    CENTIMETRES("cm"),
    MILLIMETRES("mm"),
    KILOMETRES("km"),
    INCHES("in"),
    FEET("ft"),
    YARDS("yd")
}

enum class MeasurementType {
    WEIGHT,
    VOLUME,
    LENGTH,
    PERCENTAGE
}

// Data classes
data class ParsedMeasurement(
    val type: MeasurementType,
    val weightUnit: WeightUnit? = null,
    val volumeUnit: VolumeUnit? = null,
    val lengthUnit: LengthUnit? = null
)

data class MaterialContext(
    val densityGramsPerCm3: Float,
    val diameterMm: Float = 1.75f,
    val materialType: String = ""
)

data class ConversionResult(
    val success: Boolean,
    val value: Float = 0f,
    val fromUnit: String = "",
    val toUnit: String = "",
    val conversionPath: String = "",
    val confidence: Float = 1.0f,
    val error: String? = null
)