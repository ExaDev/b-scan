package com.bscan.model

import java.time.LocalDateTime

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

/**
 * Raw encrypted scan data captured before authentication.
 * This is the complete 1024-byte dump from the Mifare Classic tag.
 */
data class EncryptedScanData(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val tagUid: String, // Individual tag UID (unique per tag)
    val technology: String,
    val tagFormat: TagFormat = TagFormat.UNKNOWN,
    val manufacturerName: String = "Unknown", // Manufacturer name from tag data
    val encryptedData: ByteArray, // Complete raw dump from the tag (varies by format)
    val tagSizeBytes: Int,
    val sectorCount: Int,
    val scanDurationMs: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedScanData
        
        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (tagUid != other.tagUid) return false
        if (technology != other.technology) return false
        if (tagFormat != other.tagFormat) return false
        if (manufacturerName != other.manufacturerName) return false
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (tagSizeBytes != other.tagSizeBytes) return false
        if (sectorCount != other.sectorCount) return false
        if (scanDurationMs != other.scanDurationMs) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + tagUid.hashCode()
        result = 31 * result + technology.hashCode()
        result = 31 * result + tagFormat.hashCode()
        result = 31 * result + manufacturerName.hashCode()
        result = 31 * result + encryptedData.contentHashCode()
        result = 31 * result + tagSizeBytes
        result = 31 * result + sectorCount
        result = 31 * result + scanDurationMs.hashCode()
        return result
    }
}

/**
 * Decrypted scan data after successful authentication.
 * Contains the uninterpreted bytes that can be re-interpreted 
 * with updated mappings without needing to rescan.
 */
data class DecryptedScanData(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val tagUid: String, // Individual tag UID (unique per tag)
    val technology: String,
    val tagFormat: TagFormat = TagFormat.UNKNOWN,
    val manufacturerName: String = "Unknown", // Manufacturer name from tag data
    val scanResult: ScanResult,
    
    // Decrypted block data (after successful authentication)
    // Map of block number -> 16-byte block data in hex format
    val decryptedBlocks: Map<Int, String>,
    
    // Authentication metadata
    val authenticatedSectors: List<Int>,
    val failedSectors: List<Int>,
    val usedKeys: Map<Int, String>, // Sector number -> key type used ("KeyA" or "KeyB")
    val derivedKeys: List<String>, // KDF-derived keys used in hex format
    
    // Scanning metadata
    val tagSizeBytes: Int,
    val sectorCount: Int,
    
    // Error tracking for debugging
    val errors: List<String>,
    
    // Additional debug information
    val keyDerivationTimeMs: Long = 0,
    val authenticationTimeMs: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecryptedScanData

        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (tagUid != other.tagUid) return false
        if (technology != other.technology) return false
        if (tagFormat != other.tagFormat) return false
        if (manufacturerName != other.manufacturerName) return false
        if (scanResult != other.scanResult) return false
        if (decryptedBlocks != other.decryptedBlocks) return false
        if (authenticatedSectors != other.authenticatedSectors) return false
        if (failedSectors != other.failedSectors) return false
        if (usedKeys != other.usedKeys) return false
        if (derivedKeys != other.derivedKeys) return false
        if (tagSizeBytes != other.tagSizeBytes) return false
        if (sectorCount != other.sectorCount) return false
        if (errors != other.errors) return false
        if (keyDerivationTimeMs != other.keyDerivationTimeMs) return false
        if (authenticationTimeMs != other.authenticationTimeMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + tagUid.hashCode()
        result = 31 * result + technology.hashCode()
        result = 31 * result + tagFormat.hashCode()
        result = 31 * result + manufacturerName.hashCode()
        result = 31 * result + scanResult.hashCode()
        result = 31 * result + decryptedBlocks.hashCode()
        result = 31 * result + authenticatedSectors.hashCode()
        result = 31 * result + failedSectors.hashCode()
        result = 31 * result + usedKeys.hashCode()
        result = 31 * result + derivedKeys.hashCode()
        result = 31 * result + tagSizeBytes
        result = 31 * result + sectorCount
        result = 31 * result + errors.hashCode()
        result = 31 * result + keyDerivationTimeMs.hashCode()
        result = 31 * result + authenticationTimeMs.hashCode()
        return result
    }
}