package com.bscan.model

import java.util.*

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

enum class WeightParser {
    STANDARD_16BIT_GRAMS,           // Standard Weight Scale Service format
    GENERIC_FFE0_RAW,               // Generic 0xFFE0 service raw data
    CUSTOM_LITTLE_ENDIAN_16BIT,     // Custom little-endian 16-bit
    CUSTOM_BIG_ENDIAN_16BIT,        // Custom big-endian 16-bit
    CUSTOM_32BIT_FLOAT,             // 32-bit float
    ASCII_STRING,                   // ASCII string format
    FFE0_BYTES_5_6_LITTLE_ENDIAN,   // User's scale: bytes 5-6 little-endian in grams
    FFE0_BYTES_4_5_6_LITTLE_ENDIAN  // User's scale: bytes 4-5-6 24-bit little-endian
}

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
    
    // Generic FFE0 service (common in many low-cost scales)
    val GENERIC_FFE0_SCALE = BleScalesConfig(
        id = "generic_ffe0",
        name = "Generic FFE0 Service Scale",
        manufacturer = "Generic",
        model = "FFE0 Compatible Scale",
        serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb",
        weightCharacteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb",
        batteryServiceUuid = "0000180f-0000-1000-8000-00805f9b34fb",
        batteryCharacteristicUuid = "00002a19-0000-1000-8000-00805f9b34fb",
        weightParser = WeightParser.GENERIC_FFE0_RAW,
        notes = "Common service UUID used by many generic BLE scales. User discovered: 0000ffe0-0000-1000-8000-00805f9b34fb. Data format: 08 07 03 01 00 5C 00 (7 bytes)"
    )
    
    // User's specific scale - confirmed paged encoding
    val USER_SCALE_E0_62_34 = BleScalesConfig(
        id = "user_scale_e0_62_34",
        name = "User's BLE Scale (E0:62:34:...)",
        manufacturer = "Unknown",
        model = "FFE0/FFE1 Scale",
        serviceUuid = "0000ffe0-0000-1000-8000-00805f9b34fb",
        weightCharacteristicUuid = "0000ffe1-0000-1000-8000-00805f9b34fb",
        weightParser = WeightParser.FFE0_BYTES_4_5_PAGED,
        notes = "User's specific scale with 7-byte protocol and paged encoding: weight = (byte4 × 256) + byte5. Examples: 262g = (1 × 256) + 6, 252g = (0 × 256) + 252"
    )
    
    // All available configurations
    // NOTE: Order matters - more specific configs should come first for better matching
    val ALL_CONFIGS = listOf(
        USER_SCALE_E0_62_34,        // Most specific - user's exact scale
        STANDARD_WEIGHT_SCALE,      // Standard BLE weight scale
        GENERIC_FFE0_SCALE         // Generic fallback for FFE0 scales
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