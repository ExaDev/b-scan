package com.bscan.service

/**
 * Centralized definitions for standard Bambu Lab components.
 * This ensures DRY compliance and prevents duplication across factories and repositories.
 */
object BambuComponentDefinitions {
    
    /**
     * Standard Bambu Lab cardboard core component definition
     */
    object Core {
        const val NAME = "Bambu Cardboard Core"
        const val CATEGORY = "core"
        const val MASS_GRAMS = 33f
        const val MANUFACTURER = "Bambu Lab"
        const val DESCRIPTION = "Standard Bambu Lab cardboard core (33g)"
        val TAGS = listOf("reusable", "fixed-mass", "bambu")
        val METADATA = mapOf("standardWeight" to "33g")
    }
    
    /**
     * Standard Bambu Lab refillable spool component definition
     */
    object Spool {
        const val NAME = "Bambu Refillable Spool"
        const val CATEGORY = "spool"
        const val MASS_GRAMS = 212f
        const val MANUFACTURER = "Bambu Lab"
        const val DESCRIPTION = "Standard Bambu Lab refillable spool (212g)"
        val TAGS = listOf("reusable", "fixed-mass", "bambu")
        val METADATA = mapOf(
            "standardWeight" to "212g",
            "type" to "refillable"
        )
    }
    
    /**
     * Standard Bambu Lab RFID tag component definition
     */
    object RfidTag {
        const val NAME = "Bambu RFID Tag"
        const val CATEGORY = "rfid-tag"
        const val MASS_GRAMS = 0.5f
        const val MANUFACTURER = "Bambu Lab"
        const val DESCRIPTION = "Mifare Classic 1K authentication tag"
        val TAGS = listOf("fixed-mass", "bambu", "authentication")
        fun getMetadata(tagUid: String) = mapOf(
            "tagUid" to tagUid,
            "tagType" to "Mifare Classic 1K",
            "authenticationTag" to "true"
        )
    }
    
    /**
     * Standard filament component definition template
     */
    object Filament {
        const val CATEGORY = "filament"
        const val MANUFACTURER = "Bambu Lab"
        val TAGS = listOf("consumable", "variable-mass", "bambu")
        const val VARIABLE_MASS = true
        
        fun getName(filamentType: String, colorName: String): String {
            val type = filamentType.takeIf { it.isNotBlank() } ?: "PLA_BASIC"
            val color = colorName.takeIf { it.isNotBlank() } ?: "Unknown Color"
            return "$type $color Filament"
        }
        
        fun getDescription(filamentType: String, colorName: String): String {
            val type = filamentType.takeIf { it.isNotBlank() } ?: "PLA_BASIC"
            val color = colorName.takeIf { it.isNotBlank() } ?: "Unknown Color"
            return "Bambu Lab $type filament in $color"
        }
        
        fun getMetadata(filamentType: String, colorName: String, trayUid: String) = mapOf(
            "material" to filamentType,
            "color" to colorName,
            "trayUid" to trayUid
        )
    }
    
    /**
     * Default filament masses by material type (in grams)
     */
    val DEFAULT_FILAMENT_MASSES = mapOf(
        "PLA" to 1000f,
        "PLA_BASIC" to 1000f,
        "PLA_MATTE" to 1000f,
        "PLA_SILK" to 1000f,
        "ABS" to 1000f,
        "PETG" to 1000f,
        "PETG_BASIC" to 1000f,
        "TPU" to 500f,
        "ASA" to 1000f,
        "PA" to 1000f,
        "PC" to 1000f,
        "PVA" to 500f,
        "PVOH" to 500f,
        "HIPS" to 1000f
    )
}