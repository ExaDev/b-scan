package com.bscan.model

/**
 * Enum representing supported RFID tag data formats/standards
 */
enum class TagFormat {
    BAMBU_PROPRIETARY,  // Bambu Lab's proprietary encrypted format on Mifare Classic 1K
    CREALITY_ASCII,     // Creality's ASCII-encoded format in blocks 4-6
    NDEF_JSON,          // OpenTag NDEF JSON format
    OPENTAG_V1,         // OpenTag v1.x standard on NTAG216
    NDEF_URI,           // NDEF URI format
    PROPRIETARY,        // Other proprietary formats
    USER_DEFINED,       // User-created custom format
    UNKNOWN             // Unidentified or unsupported format
}

/**
 * Tag technology types supported by the app
 */
enum class TagTechnology {
    MIFARE_CLASSIC,  // Mifare Classic 1K (Bambu, potentially Creality)
    NTAG,           // NTAG213/215/216 (OpenTag)
    UNKNOWN         // Unidentified technology
}

/**
 * Result of tag format detection
 */
data class TagDetectionResult(
    val tagFormat: TagFormat,
    val technology: TagTechnology,
    val confidence: Float,  // 0.0 to 1.0 confidence in detection
    val detectionReason: String, // Human-readable reason for detection
    val manufacturerName: String = "Unknown" // Manufacturer name if extractable from tag data
)