package com.bscan.ble

import java.time.LocalDateTime

/**
 * Represents a complete weight reading from a BLE scale
 * Includes weight data, stability information, and metadata
 */
data class ScaleReading(
    val weight: Float,                    // Weight value from scale
    val isStable: Boolean,                // Stability flag from scale
    val unit: WeightUnit,                 // Unit detected from scale (GRAMS, OUNCES, etc.)
    val isUnitValid: Boolean,             // True if unit matches app expectations
    val batteryLevel: Int? = null,        // Battery percentage (if available)
    val signalStrength: Int? = null,      // RSSI in dBm
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val rawData: ByteArray,               // Original BLE data for debugging
    val parsingMethod: String             // Which parser was used (for debugging)
) {
    
    /**
     * Format weight for display with appropriate precision and unit
     */
    fun getDisplayWeight(): String {
        val absWeight = kotlin.math.abs(weight)
        val sign = if (weight < 0) "-" else ""
        val unitSuffix = unit.abbreviation
        
        val formattedWeight = when {
            absWeight < 10 -> "${sign}%.2f".format(absWeight)
            absWeight < 100 -> "${sign}%.1f".format(absWeight) 
            else -> "${sign}%.0f".format(absWeight)
        }
        
        return "$formattedWeight$unitSuffix"
    }
    
    /**
     * Get display weight with unit validation warning
     */
    fun getDisplayWeightWithValidation(): String {
        val baseDisplay = getDisplayWeight()
        return if (!isUnitValid) "⚠ $baseDisplay" else baseDisplay
    }
    
    /**
     * Get stability indicator for UI
     */
    fun getStabilityIcon(): String {
        return if (isStable) "🟢" else "🟡"
    }
    
    /**
     * Get hex representation of raw data for debugging
     */
    fun getRawDataHex(): String {
        return rawData.joinToString(" ") { "%02X".format(it) }
    }
    
    /**
     * Check if reading is valid for capture
     * Requires stable reading, valid unit, and reasonable weight
     */
    fun isValidForCapture(): Boolean {
        return isStable && isUnitValid && weight >= 0 && weight <= 10000 // 10kg max
    }
    
    /**
     * Get time elapsed since reading
     */
    fun getAgeString(): String {
        val now = LocalDateTime.now()
        val seconds = java.time.Duration.between(timestamp, now).seconds
        return when {
            seconds < 1 -> "just now"
            seconds < 60 -> "${seconds}s ago"
            seconds < 3600 -> "${seconds / 60}m ago"
            else -> "${seconds / 3600}h ago"
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ScaleReading
        
        if (weight != other.weight) return false
        if (isStable != other.isStable) return false
        if (unit != other.unit) return false
        if (isUnitValid != other.isUnitValid) return false
        if (batteryLevel != other.batteryLevel) return false
        if (timestamp != other.timestamp) return false
        if (!rawData.contentEquals(other.rawData)) return false
        if (parsingMethod != other.parsingMethod) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = weight.hashCode()
        result = 31 * result + isStable.hashCode()
        result = 31 * result + unit.hashCode()
        result = 31 * result + isUnitValid.hashCode()
        result = 31 * result + (batteryLevel ?: 0)
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + parsingMethod.hashCode()
        return result
    }
}

/**
 * Weight units supported by scales
 */
enum class WeightUnit(val displayName: String, val abbreviation: String, val scaleByteValue: Int? = null) {
    GRAMS("Grams", "g", 0x00),
    OUNCES("Ounces", "oz", 0x01),
    POUNDS("Pounds", "lb", null),
    KILOGRAMS("Kilograms", "kg", null),
    MILLILITERS("Milliliters", "ml", 0x02),
    FLUID_OUNCES("Fluid Ounces", "fl oz", 0x03),
    UNKNOWN("Unknown", "?", null);
    
    companion object {
        /**
         * Get unit from scale byte value (byte 6 in FFE0 protocol)
         */
        fun fromScaleByte(byteValue: Int): WeightUnit {
            return values().find { it.scaleByteValue == byteValue } ?: UNKNOWN
        }
        
        /**
         * Check if a unit is expected/valid for the app configuration
         * Currently hardcoded to GRAMS, but can be extended for user preferences
         */
        fun isExpectedUnit(unit: WeightUnit): Boolean {
            return unit == GRAMS // TODO: Make configurable per user preferences
        }
    }
    
    /**
     * Convert weight from this unit to grams
     */
    fun toGrams(weight: Float): Float {
        return when (this) {
            GRAMS -> weight
            OUNCES -> weight * 28.3495f
            POUNDS -> weight * 453.592f
            KILOGRAMS -> weight * 1000f
            MILLILITERS -> weight // Volume unit, treat as 1:1 with grams for liquids
            FLUID_OUNCES -> weight * 29.5735f // 1 fl oz ≈ 29.57g for water
            UNKNOWN -> weight // Pass through unknown units
        }
    }
    
    /**
     * Convert weight from grams to this unit
     */
    fun fromGrams(weightInGrams: Float): Float {
        return when (this) {
            GRAMS -> weightInGrams
            OUNCES -> weightInGrams / 28.3495f
            POUNDS -> weightInGrams / 453.592f
            KILOGRAMS -> weightInGrams / 1000f
            MILLILITERS -> weightInGrams // Volume unit, treat as 1:1 with grams for liquids
            FLUID_OUNCES -> weightInGrams / 29.5735f // 1 fl oz ≈ 29.57g for water
            UNKNOWN -> weightInGrams // Pass through unknown units
        }
    }
}

/**
 * Scale control commands and responses
 */
sealed class ScaleCommand {
    object Tare : ScaleCommand()
    data class SetUnit(val unit: WeightUnit) : ScaleCommand()
    object EnterCalibration : ScaleCommand()
    object GetBatteryLevel : ScaleCommand()
}

/**
 * Results of scale control operations
 */
sealed class ScaleCommandResult {
    object Success : ScaleCommandResult()
    data class Error(val message: String, val exception: Throwable? = null) : ScaleCommandResult()
    object Timeout : ScaleCommandResult()
    object NotSupported : ScaleCommandResult()
}

/**
 * Scale connection and reading states
 */
enum class ScaleConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    READING,
    ERROR
}