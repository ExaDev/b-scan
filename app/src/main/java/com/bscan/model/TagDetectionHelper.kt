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
     * Detect tag format from decrypted scan data by analyzing block structure
     */
    fun detectFormat(decryptedScan: DecryptedScanData): TagDetectionResult {
        // For decrypted scans, we can analyze the block structure directly
        // without needing to reconstruct the full raw data
        return detectFromBlockStructure(decryptedScan.technology, decryptedScan.decryptedBlocks, decryptedScan.tagSizeBytes, decryptedScan.sectorCount)
    }
    
    /**
     * Detect tag format from block structure and metadata
     */
    private fun detectFromBlockStructure(technology: String, blocks: Map<Int, String>, tagSizeBytes: Int, sectorCount: Int): TagDetectionResult {
        android.util.Log.d("TagDetectionHelper", "detectFromBlockStructure: technology=$technology, sectorCount=$sectorCount, tagSizeBytes=$tagSizeBytes, blocks=${blocks.size}")
        
        return when {
            // Mifare Classic with proper Bambu structure
            technology == "MifareClassic" && sectorCount == 16 && blocks.isNotEmpty() -> {
                // Check for Bambu indicators in the blocks
                val block2 = blocks[2] // Filament type block
                val block4 = blocks[4] // Detailed type block
                
                if (block2?.startsWith("504C41") == true || // "PLA"
                    block2?.startsWith("504554") == true || // "PET" (PETG)
                    block2?.startsWith("414253") == true || // "ABS"
                    block2?.startsWith("415341") == true || // "ASA"
                    block2?.startsWith("545055") == true || // "TPU"
                    block4?.contains("4D6174746") == true) { // "Matte"
                    android.util.Log.d("TagDetectionHelper", "Detected Bambu tag from block analysis")
                    TagDetectionResult(
                        tagFormat = TagFormat.BAMBU_PROPRIETARY,
                        technology = TagTechnology.MIFARE_CLASSIC,
                        confidence = 0.9f,
                        detectionReason = "Mifare Classic with Bambu block structure (${sectorCount} sectors)",
                        manufacturerName = "Bambu Lab"
                    )
                } else {
                    // Fall back to size-based detection
                    detectFromSize(technology, tagSizeBytes, sectorCount)
                }
            }
            else -> detectFromSize(technology, tagSizeBytes, sectorCount)
        }
    }
    
    /**
     * Fallback detection based on size and sector count
     */
    private fun detectFromSize(technology: String, tagSizeBytes: Int, sectorCount: Int): TagDetectionResult {
        return when {
            technology == "MifareClassic" && sectorCount == 16 -> {
                android.util.Log.d("TagDetectionHelper", "Detected Bambu tag from size/structure")
                TagDetectionResult(
                    tagFormat = TagFormat.BAMBU_PROPRIETARY,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.8f,
                    detectionReason = "16-sector Mifare Classic matches Bambu format",
                    manufacturerName = "Bambu Lab"
                )
            }
            else -> {
                android.util.Log.w("TagDetectionHelper", "Unknown tag format: technology=$technology, sectorCount=$sectorCount")
                TagDetectionResult(
                    tagFormat = TagFormat.UNKNOWN,
                    technology = TagTechnology.UNKNOWN,
                    confidence = 0.0f,
                    detectionReason = "Unrecognized format: $technology with $sectorCount sectors"
                )
            }
        }
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