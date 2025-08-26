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
 * Contains only the actual data read from the tag - no interpretation.
 * Tag format and manufacturer are derived at runtime via TagDetector.
 * Supports intelligent format detection based on data size:
 * - 768 bytes: data blocks only (16 sectors × 3 blocks × 16 bytes)
 * - 1024 bytes: complete dump with trailer blocks (16 sectors × 4 blocks × 16 bytes)
 */
data class EncryptedScanData(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val tagUid: String, // Individual tag UID (unique per tag)
    val technology: String,
    val encryptedData: ByteArray, // Unified storage: 768-byte or 1024-byte format
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
        if (!encryptedData.contentEquals(other.encryptedData)) return false
        if (scanDurationMs != other.scanDurationMs) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + tagUid.hashCode()
        result = 31 * result + technology.hashCode()
        result = 31 * result + encryptedData.contentHashCode()
        result = 31 * result + scanDurationMs.hashCode()
        return result
    }
}

/**
 * Decrypted scan data after successful authentication.
 * Contains only the actual decrypted data from the tag - no interpretation.
 * Tag format and manufacturer are derived at runtime via TagDetector.
 * Contains the uninterpreted bytes that can be re-interpreted 
 * with updated mappings without needing to rescan.
 */
data class DecryptedScanData(
    val id: Long = 0,
    val timestamp: LocalDateTime,
    val tagUid: String, // Individual tag UID (unique per tag)
    val technology: String,
    val scanResult: ScanResult,
    
    // Decrypted block data (after successful authentication)
    // Map of block number -> 16-byte block data in hex format
    // Includes all available blocks based on scan format
    val decryptedBlocks: Map<Int, String>,
    
    // Authentication metadata
    val authenticatedSectors: List<Int>,
    val failedSectors: List<Int>,
    val usedKeys: Map<Int, String>, // Sector number -> key type used ("KeyA" or "KeyB")
    val derivedKeys: List<String>, // KDF-derived keys used in hex format
    
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
        if (scanResult != other.scanResult) return false
        if (decryptedBlocks != other.decryptedBlocks) return false
        if (authenticatedSectors != other.authenticatedSectors) return false
        if (failedSectors != other.failedSectors) return false
        if (usedKeys != other.usedKeys) return false
        if (derivedKeys != other.derivedKeys) return false
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
        result = 31 * result + scanResult.hashCode()
        result = 31 * result + decryptedBlocks.hashCode()
        result = 31 * result + authenticatedSectors.hashCode()
        result = 31 * result + failedSectors.hashCode()
        result = 31 * result + usedKeys.hashCode()
        result = 31 * result + derivedKeys.hashCode()
        result = 31 * result + errors.hashCode()
        result = 31 * result + keyDerivationTimeMs.hashCode()
        result = 31 * result + authenticationTimeMs.hashCode()
        return result
    }
}

/**
 * Format detection and data conversion utilities for RFID scan data
 */
object RfidDataFormat {
    const val DATA_ONLY_SIZE = 768    // 16 sectors × 3 blocks × 16 bytes (data blocks only)
    const val COMPLETE_SIZE = 1024    // 16 sectors × 4 blocks × 16 bytes (including trailers)
    
    /**
     * Detect the format of RFID data based on size
     */
    fun detectFormat(data: ByteArray): RfidFormat = when (data.size) {
        DATA_ONLY_SIZE -> RfidFormat.DATA_ONLY
        COMPLETE_SIZE -> RfidFormat.COMPLETE
        else -> RfidFormat.UNKNOWN
    }
    
    /**
     * Check if data includes trailer blocks
     */
    fun hasTrailerBlocks(data: ByteArray): Boolean = data.size == COMPLETE_SIZE
    
    /**
     * Extract data blocks from complete format (remove trailer blocks)
     * If already in data-only format, returns the original data
     */
    fun extractDataBlocks(data: ByteArray): ByteArray = when (data.size) {
        COMPLETE_SIZE -> {
            // Extract 3 blocks per sector (skip every 4th block which is trailer)
            val dataBlocks = ByteArray(DATA_ONLY_SIZE)
            var dataOffset = 0
            for (sector in 0 until 16) {
                val sectorStartComplete = sector * 64    // 4 blocks × 16 bytes
                val sectorStartData = sector * 48        // 3 blocks × 16 bytes
                // Copy 3 data blocks (48 bytes), skip trailer block
                System.arraycopy(data, sectorStartComplete, dataBlocks, sectorStartData, 48)
            }
            dataBlocks
        }
        DATA_ONLY_SIZE -> data // Already in correct format
        else -> data // Unknown format, return as-is
    }
    
    /**
     * Get all blocks from the data as a map of block number to hex string
     */
    fun getAllBlocks(data: ByteArray): Map<Int, String> = when (detectFormat(data)) {
        RfidFormat.DATA_ONLY -> getDataOnlyBlocks(data)
        RfidFormat.COMPLETE -> getCompleteBlocks(data)
        RfidFormat.UNKNOWN -> emptyMap()
    }
    
    /**
     * Get data blocks only (legacy format)
     */
    fun getDataBlocks(data: ByteArray): Map<Int, String> = when (detectFormat(data)) {
        RfidFormat.DATA_ONLY -> getDataOnlyBlocks(data)
        RfidFormat.COMPLETE -> getDataOnlyBlocks(extractDataBlocks(data))
        RfidFormat.UNKNOWN -> emptyMap()
    }
    
    private fun getDataOnlyBlocks(data: ByteArray): Map<Int, String> {
        val blocks = mutableMapOf<Int, String>()
        if (data.size != DATA_ONLY_SIZE) return blocks
        
        for (sector in 0 until 16) {
            for (blockInSector in 0 until 3) { // Only data blocks
                val absoluteBlock = sector * 4 + blockInSector
                val dataOffset = sector * 48 + blockInSector * 16
                val blockData = data.copyOfRange(dataOffset, dataOffset + 16)
                blocks[absoluteBlock] = blockData.joinToString("") { "%02X".format(it) }
            }
        }
        return blocks
    }
    
    private fun getCompleteBlocks(data: ByteArray): Map<Int, String> {
        val blocks = mutableMapOf<Int, String>()
        if (data.size != COMPLETE_SIZE) return blocks
        
        for (sector in 0 until 16) {
            for (blockInSector in 0 until 4) { // All blocks including trailers
                val absoluteBlock = sector * 4 + blockInSector
                val dataOffset = sector * 64 + blockInSector * 16
                val blockData = data.copyOfRange(dataOffset, dataOffset + 16)
                blocks[absoluteBlock] = blockData.joinToString("") { "%02X".format(it) }
            }
        }
        return blocks
    }
}

enum class RfidFormat {
    DATA_ONLY,    // 768 bytes: data blocks only
    COMPLETE,     // 1024 bytes: includes trailer blocks
    UNKNOWN       // Other sizes
}