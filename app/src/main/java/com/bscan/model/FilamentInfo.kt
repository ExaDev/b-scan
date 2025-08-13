package com.bscan.model

data class FilamentInfo(
    val uid: String,
    val trayUid: String,
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
    val dryingTime: Int // in hours
)