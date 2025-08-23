package com.bscan.model

data class FilamentInfo(
    val tagUid: String, // Individual tag UID (unique per tag)
    val trayUid: String, // Tray UID (shared across both tags on a spool)
    val tagFormat: TagFormat = TagFormat.UNKNOWN,
    val manufacturerName: String = "Unknown",
    val filamentType: String,
    val detailedFilamentType: String,
    val colorHex: String,
    val colorName: String,
    val spoolWeight: Int, // in grams
    val filamentDiameter: Float, // in mm
    val filamentLength: Int, // in mm
    val productionDate: String,
    val minTemperature: Int, // in °C
    val maxTemperature: Int, // in °C
    val bedTemperature: Int, // in °C
    val dryingTemperature: Int, // in °C
    val dryingTime: Int, // in hours
    
    // Extended fields from comprehensive block parsing
    val materialVariantId: String = "",
    val materialId: String = "",
    val nozzleDiameter: Float = 0.4f, // in mm
    val spoolWidth: Float = 0f, // in mm
    val bedTemperatureType: Int = 0,
    val shortProductionDate: String = "",
    val colorCount: Int = 1, // 1 = single color, 2 = dual color
    
    // Research fields for unknown data blocks
    val shortProductionDateHex: String = "", // Block 13 raw hex data
    val unknownBlock17Hex: String = "" // Block 17 full raw hex data for research
)