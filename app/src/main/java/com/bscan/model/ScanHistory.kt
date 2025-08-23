package com.bscan.model

import java.time.LocalDateTime

data class ScanHistory(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val uid: String,
    val technology: String,
    val scanResult: ScanResult,
    val filamentInfo: FilamentInfo?,
    val debugInfo: ScanDebugInfo
)

enum class ScanResult {
    SUCCESS,
    AUTHENTICATION_FAILED,
    INSUFFICIENT_DATA,
    PARSING_FAILED,
    NO_NFC_TAG,
    UNKNOWN_ERROR
}

data class ScanDebugInfo(
    val uid: String, // Tag UID in hex format
    val tagSizeBytes: Int,
    val sectorCount: Int,
    val authenticatedSectors: List<Int>,
    val failedSectors: List<Int>,
    val usedKeyTypes: Map<Int, String>, // sector -> "KeyA" or "KeyB" or "None"
    val blockData: Map<Int, String>, // block -> hex data
    val derivedKeys: List<String>, // KDF-derived keys in hex
    val rawColorBytes: String,
    val errorMessages: List<String>,
    val parsingDetails: Map<String, Any?>, // flexible debug data
    val fullRawHex: String = "", // Complete 768-byte raw encrypted data
    val decryptedHex: String = "" // Complete decrypted data after authentication
)