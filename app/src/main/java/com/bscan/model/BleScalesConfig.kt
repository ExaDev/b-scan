package com.bscan.model

/**
 * Configuration for different BLE scales models
 * Stores protocol details for parsing weight data from various scale manufacturers
 */
data class BleScalesConfig(
    val id: String,
    val name: String,
    val manufacturer: String,
    val model: String,
    val serviceUuid: String,
    val weightCharacteristicUuid: String,
    val batteryServiceUuid: String? = null,
    val batteryCharacteristicUuid: String? = null,
    val weightParser: WeightParser = WeightParser.STANDARD_16BIT_GRAMS,
    val notes: String = ""
)

/**
 * Different weight parsing strategies for various BLE scale protocols
 */
enum class WeightParser {
    STANDARD_16BIT_GRAMS,           // Standard Weight Scale Service format
    GENERIC_FFE0_RAW,               // Generic 0xFFE0 service raw data
    CUSTOM_LITTLE_ENDIAN_16BIT,     // Custom little-endian 16-bit
    CUSTOM_BIG_ENDIAN_16BIT,        // Custom big-endian 16-bit
    CUSTOM_32BIT_FLOAT,             // 32-bit float
    ASCII_STRING,                   // ASCII string format
    FFE0_BYTES_5_6_LITTLE_ENDIAN,   // Bytes 5-6 little-endian in grams
    FFE0_BYTES_4_5_6_LITTLE_ENDIAN  // Bytes 4-5-6 24-bit little-endian (discovered pattern)
}

/**
 * Predefined BLE scales configurations
 * Based on our previous research and testing
 */
object BleScalesConfigs {
    
    // Standard Weight Scale Service (official BLE SIG specification)
    val STANDARD_WEIGHT_SCALE = BleScalesConfig(
        id = "standard_weight_scale",
        name = "Standard Weight Scale Service",
        manufacturer = "Generic",
        model = "BLE Weight Scale",
        serviceUuid = "0000181d-0000-1000-8000-00805f9b34fb",
        weightCharacteristicUuid = "00002a9d-0000-1000-8000-00805f9b34fb",
        batteryServiceUuid = "0000180f-0000-1000-8000-00805f9b34fb",
        batteryCharacteristicUuid = "00002a19-0000-1000-8000-00805f9b34fb",
        weightParser = WeightParser.STANDARD_16BIT_GRAMS,
        notes = "Official BLE Weight Scale Service specification"
    )
    
    // Generic FFE0 service (discovered through testing)
    val GENERIC_FFE0_SCALE = BleScalesConfig(
        id = "generic_ffe0",
        name = "Generic FFE0 Service Scale",
        manufacturer = "Generic",
        model = "FFE0 Compatible Scale",
        serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb",
        weightCharacteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb",
        batteryServiceUuid = "0000180f-0000-1000-8000-00805f9b34fb",
        batteryCharacteristicUuid = "00002a19-0000-1000-8000-00805f9b34fb",
        weightParser = WeightParser.FFE0_BYTES_4_5_6_LITTLE_ENDIAN,
        notes = "Generic FFE0 service. Protocol discovered: 7-byte format with weight in bytes 4-5-6 as 24-bit little-endian. Tested with 252g and 262g measurements."
    )
    
    // All available configurations
    val ALL_CONFIGS = listOf(
        GENERIC_FFE0_SCALE,      // Prioritize our tested configuration
        STANDARD_WEIGHT_SCALE    // Standard fallback
    )
    
    fun getConfigById(id: String): BleScalesConfig? {
        return ALL_CONFIGS.find { it.id == id }
    }
    
    fun getConfigByServiceUuid(serviceUuid: String): BleScalesConfig? {
        return ALL_CONFIGS.find { 
            it.serviceUuid.equals(serviceUuid, ignoreCase = true) 
        }
    }
    
    fun getConfigsByManufacturer(manufacturer: String): List<BleScalesConfig> {
        return ALL_CONFIGS.filter { 
            it.manufacturer.equals(manufacturer, ignoreCase = true) 
        }
    }
}