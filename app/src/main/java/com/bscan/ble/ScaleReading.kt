package com.bscan.ble

import java.time.LocalDateTime

/**
 * Represents a complete weight reading from a BLE scale
 * Includes weight data, stability information, and metadata
 */
data class ScaleReading(
    val weight: Float,                    // Weight in grams
    val isStable: Boolean,                // Stability flag from scale
    val unit: WeightUnit,                 // Unit of measurement
    val batteryLevel: Int? = null,        // Battery percentage (if available)
    val signalStrength: Int? = null,      // RSSI in dBm
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val rawData: ByteArray,               // Original BLE data for debugging
    val parsingMethod: String             // Which parser was used (for debugging)
) {
    
    /**
     * Format weight for display with appropriate precision
     */
    fun getDisplayWeight(): String {
        return when {
            weight < 0 -> "-.--g"  // Negative weight (after tare)
            weight < 10 -> "%.2fg".format(weight)
            weight < 100 -> "%.1fg".format(weight) 
            else -> "%.0fg".format(weight)
        }
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
     */
    fun isValidForCapture(): Boolean {
        return isStable && weight >= 0 && weight <= 10000 // 10kg max
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
enum class WeightUnit(val displayName: String, val abbreviation: String) {
    GRAMS("Grams", "g"),
    OUNCES("Ounces", "oz"),
    POUNDS("Pounds", "lb"),
    KILOGRAMS("Kilograms", "kg");
    
    /**
     * Convert weight from this unit to grams
     */
    fun toGrams(weight: Float): Float {
        return when (this) {
            GRAMS -> weight
            OUNCES -> weight * 28.3495f
            POUNDS -> weight * 453.592f
            KILOGRAMS -> weight * 1000f
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