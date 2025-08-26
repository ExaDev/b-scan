package com.bscan.model

import com.bscan.detector.TagDetector

/**
 * Helper for runtime tag format detection from scan data
 */
object TagDetectionHelper {
    private val tagDetector = TagDetector()
    
    /**
     * Detect tag format from encrypted scan data
     */
    fun detectFormat(encryptedScan: EncryptedScanData): TagDetectionResult {
        return tagDetector.detectFromData(encryptedScan.technology, encryptedScan.encryptedData)
    }
    
    /**
     * Detect tag format from decrypted scan data by reconstructing raw data
     */
    fun detectFormat(decryptedScan: DecryptedScanData): TagDetectionResult {
        val rawData = reconstructRawDataFromBlocks(decryptedScan.decryptedBlocks)
        return tagDetector.detectFromData(decryptedScan.technology, rawData)
    }
    
    /**
     * Reconstruct raw 1024-byte data from decrypted block data
     */
    private fun reconstructRawDataFromBlocks(blocks: Map<Int, String>): ByteArray {
        return try {
            val rawData = ByteArray(1024)
            
            // Reconstruct complete 1024-byte format from blocks
            for (sector in 0 until 16) {
                for (blockInSector in 0 until 4) {
                    val absoluteBlock = sector * 4 + blockInSector
                    val hexData = blocks[absoluteBlock] ?: "00000000000000000000000000000000"
                    
                    // Convert hex string to bytes
                    val blockBytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    val offset = sector * 64 + blockInSector * 16
                    
                    // Copy block data to correct position
                    System.arraycopy(blockBytes, 0, rawData, offset, minOf(16, blockBytes.size))
                }
            }
            
            rawData
        } catch (e: Exception) {
            android.util.Log.w("TagDetectionHelper", "Failed to reconstruct raw data from blocks", e)
            ByteArray(0)
        }
    }
}

/**
 * Extension properties for runtime tag format detection
 */
val EncryptedScanData.tagFormat: TagFormat
    get() = TagDetectionHelper.detectFormat(this).tagFormat

val EncryptedScanData.manufacturerName: String
    get() = TagDetectionHelper.detectFormat(this).manufacturerName ?: "Unknown"

val DecryptedScanData.tagFormat: TagFormat
    get() = TagDetectionHelper.detectFormat(this).tagFormat

val DecryptedScanData.manufacturerName: String
    get() = TagDetectionHelper.detectFormat(this).manufacturerName ?: "Unknown"