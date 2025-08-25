package com.bscan.debug

import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.TagFormat
import com.bscan.detector.TagDetector
import java.time.LocalDateTime

class DebugDataCollector {
    private val authenticatedSectors = mutableListOf<Int>()
    private val failedSectors = mutableListOf<Int>()
    private val usedKeyTypes = mutableMapOf<Int, String>()
    private val blockData = mutableMapOf<Int, String>() // Legacy: data blocks only
    private val allBlockData = mutableMapOf<Int, String>() // Complete: all blocks including trailers
    private val derivedKeys = mutableListOf<String>()
    private val errorMessages = mutableListOf<String>()
    private val parsingDetails = mutableMapOf<String, Any?>()
    private var rawColorBytes = ""
    private var tagSizeBytes = 0
    private var sectorCount = 0
    private var cacheHit = false
    private var fullRawData: ByteArray = ByteArray(0) // Legacy 768-byte format
    private var completeTagData: ByteArray = ByteArray(0) // Complete 1024-byte format
    private var decryptedData: ByteArray = ByteArray(0)
    
    private val tagDetector = TagDetector()
    
    fun recordTagInfo(sizeBytes: Int, sectors: Int) {
        tagSizeBytes = sizeBytes
        sectorCount = sectors
    }
    
    fun recordSectorAuthentication(sector: Int, success: Boolean, keyType: String = "Unknown") {
        if (success) {
            authenticatedSectors.add(sector)
            usedKeyTypes[sector] = keyType
        } else {
            failedSectors.add(sector)
        }
    }
    
    fun recordBlockData(block: Int, hexData: String) {
        blockData[block] = hexData
    }
    
    /**
     * Record block data for ALL blocks including trailer blocks
     */
    fun recordAllBlockData(block: Int, hexData: String, isTrailerBlock: Boolean) {
        allBlockData[block] = hexData
        
        // Also record in legacy format for data blocks only
        if (!isTrailerBlock) {
            blockData[block] = hexData
        }
    }
    
    fun recordDerivedKeys(keys: Array<ByteArray>) {
        derivedKeys.clear()
        keys.forEach { key ->
            derivedKeys.add(key.joinToString("") { "%02X".format(it) })
        }
    }
    
    fun recordColorBytes(colorBytes: ByteArray) {
        rawColorBytes = colorBytes.joinToString("") { "%02X".format(it) }
    }
    
    fun recordError(message: String) {
        errorMessages.add(message)
    }
    
    fun recordParsingDetail(key: String, value: Any?) {
        parsingDetails[key] = value
    }
    
    fun recordCacheHit() {
        cacheHit = true
    }
    
    fun recordFullRawData(rawData: ByteArray) {
        fullRawData = rawData.copyOf()
    }
    
    /**
     * Record complete tag data including trailer blocks
     */
    fun recordCompleteTagData(completeData: ByteArray) {
        completeTagData = completeData.copyOf()
    }
    
    fun recordDecryptedData(decryptedData: ByteArray) {
        this.decryptedData = decryptedData.copyOf()
    }
    
    fun hasAuthenticatedSectors(): Boolean {
        return authenticatedSectors.isNotEmpty()
    }
    
    
    fun reset() {
        authenticatedSectors.clear()
        failedSectors.clear()
        usedKeyTypes.clear()
        blockData.clear()
        derivedKeys.clear()
        errorMessages.clear()
        parsingDetails.clear()
        rawColorBytes = ""
        tagSizeBytes = 0
        sectorCount = 0
        cacheHit = false
        fullRawData = ByteArray(0)
        completeTagData = ByteArray(0)
        decryptedData = ByteArray(0)
        allBlockData.clear()
    }
    
    // Getter methods for accessing collected data
    fun getFullRawData(): ByteArray = fullRawData.copyOf()
    fun getCompleteTagData(): ByteArray = completeTagData.copyOf()
    fun getDecryptedData(): ByteArray = decryptedData.copyOf()
    fun getBlockData(): Map<Int, String> = blockData.toMap()
    fun getAllBlockData(): Map<Int, String> = allBlockData.toMap()
    fun getAuthenticatedSectors(): List<Int> = authenticatedSectors.toList()
    fun getFailedSectors(): List<Int> = failedSectors.toList()
    fun getUsedKeyTypes(): Map<Int, String> = usedKeyTypes.toMap()
    fun getDerivedKeys(): List<String> = derivedKeys.toList()
    fun getErrorMessages(): List<String> = errorMessages.toList()
    fun getTagSizeBytes(): Int = tagSizeBytes
    fun getSectorCount(): Int = sectorCount
    
    /**
     * Create EncryptedScanData from collected information
     */
    fun createEncryptedScanData(
        uid: String,
        technology: String,
        scanDurationMs: Long = 0
    ): EncryptedScanData {
        // Detect format from raw data
        val detection = tagDetector.detectFromData(technology, getFullRawData())
        
        return EncryptedScanData(
            id = System.currentTimeMillis(),
            timestamp = LocalDateTime.now(),
            tagUid = uid,
            technology = technology,
            tagFormat = detection.tagFormat,
            manufacturerName = detection.manufacturerName,
            encryptedData = getFullRawData(), // Legacy 768-byte format for compatibility
            completeTagData = getCompleteTagData().takeIf { it.isNotEmpty() }, // Complete 1024-byte format with trailers
            tagSizeBytes = tagSizeBytes,
            sectorCount = sectorCount,
            scanDurationMs = scanDurationMs
        )
    }
    
    /**
     * Create DecryptedScanData from collected information
     */
    fun createDecryptedScanData(
        uid: String,
        technology: String,
        result: ScanResult,
        keyDerivationTimeMs: Long = 0,
        authenticationTimeMs: Long = 0
    ): DecryptedScanData {
        // Detect format from raw data
        val detection = tagDetector.detectFromData(technology, getFullRawData())
        
        return DecryptedScanData(
            id = System.currentTimeMillis() + 1, // Ensure different ID from encrypted
            timestamp = LocalDateTime.now(),
            tagUid = uid,
            technology = technology,
            tagFormat = detection.tagFormat,
            manufacturerName = detection.manufacturerName,
            scanResult = result,
            decryptedBlocks = getBlockData(), // Legacy: data blocks only for compatibility
            allBlocks = getAllBlockData(), // Complete: all blocks including trailers
            authenticatedSectors = getAuthenticatedSectors(),
            failedSectors = getFailedSectors(),
            usedKeys = getUsedKeyTypes(),
            derivedKeys = getDerivedKeys(),
            tagSizeBytes = tagSizeBytes,
            sectorCount = sectorCount,
            errors = getErrorMessages(),
            keyDerivationTimeMs = keyDerivationTimeMs,
            authenticationTimeMs = authenticationTimeMs
        )
    }
}