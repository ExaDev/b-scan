package com.bscan.model

import java.time.LocalDateTime

/**
 * Represents a weight measurement for an individual spool
 * Spool ID is unique combination of trayUid + tagUid since each tray can contain multiple spools
 */
data class SpoolWeight(
    val spoolId: String, // "${trayUid}_${tagUid}" - unique identifier for individual spool
    val trayUid: String, // Physical tray identifier
    val tagUid: String, // NFC tag identifier  
    val weightGrams: Float, // Current measured weight in grams
    val timestamp: LocalDateTime, // When the measurement was taken
    val deviceId: String, // BLE scales device identifier
    val deviceName: String = "", // Human-readable device name
    val batteryLevel: Int? = null, // Optional battery level of scales (0-100)
    val measurementType: WeightMeasurementType = WeightMeasurementType.MANUAL
)

/**
 * Different types of weight measurements
 */
enum class WeightMeasurementType {
    MANUAL,     // User-initiated measurement
    AUTOMATIC,  // Automatic reading when spool placed on scales
    PERIODIC,   // Scheduled/periodic monitoring
    TARE        // Tare/zero measurement
}

/**
 * BLE scales device information
 */
data class BleScalesDevice(
    val deviceId: String,        // MAC address or unique ID
    val name: String,            // Device name
    val manufacturer: String = "",// Manufacturer name
    val modelNumber: String = "", // Model number
    val isConnected: Boolean = false,
    val batteryLevel: Int? = null,
    val lastSeen: LocalDateTime? = null,
    val signalStrength: Int? = null // RSSI value
)

/**
 * Statistics for a spool over time
 */
data class SpoolWeightStats(
    val spoolId: String,
    val initialWeight: Float,     // First recorded weight
    val currentWeight: Float,     // Most recent weight  
    val minimumWeight: Float,     // Lowest recorded weight
    val maximumWeight: Float,     // Highest recorded weight
    val totalUsage: Float,        // initialWeight - currentWeight
    val measurementCount: Int,    // Number of weight readings
    val firstMeasurement: LocalDateTime,
    val lastMeasurement: LocalDateTime,
    val averageUsagePerDay: Float // Usage rate over time
) {
    val usagePercentage: Float 
        get() = if (initialWeight > 0) (totalUsage / initialWeight) * 100 else 0f
        
    val remainingPercentage: Float
        get() = if (initialWeight > 0) (currentWeight / initialWeight) * 100 else 0f
}