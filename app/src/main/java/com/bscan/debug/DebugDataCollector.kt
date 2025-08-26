package com.bscan.debug

import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.TagFormat
import com.bscan.model.RfidDataFormat
import com.bscan.detector.TagDetector
import java.time.LocalDateTime

class DebugDataCollector {
    private val authenticatedSectors = mutableListOf<Int>()
    private val failedSectors = mutableListOf<Int>()
    private val usedKeyTypes = mutableMapOf<Int, String>()
    private val blockData = mutableMapOf<Int, String>() // All available blocks based on scan format
    private val derivedKeys = mutableListOf<String>()
    private val errorMessages = mutableListOf<String>()
    private val parsingDetails = mutableMapOf<String, Any?>()
    private var rawColorBytes = ""
    private var tagSizeBytes = 0
    private var sectorCount = 0
    private var cacheHit = false
    private var rawData: ByteArray = ByteArray(0) // Unified storage: 768-byte or 1024-byte format
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
    
    /**
     * Record block data (supports both data-only and complete formats)
     */
    fun recordBlockData(block: Int, hexData: String) {
        blockData[block] = hexData
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
    
    /**
     * Record raw tag data (supports both 768-byte and 1024-byte formats)
     */
    fun recordRawData(data: ByteArray) {
        rawData = data.copyOf()
        android.util.Log.d("DebugDataCollector", "recordRawData called with ${data.size} bytes")
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
        rawData = ByteArray(0)
        decryptedData = ByteArray(0)
    }
    
    // Getter methods for accessing collected data
    fun getRawData(): ByteArray = rawData.copyOf()
    fun getDecryptedData(): ByteArray = decryptedData.copyOf()
    fun getBlockData(): Map<Int, String> = blockData.toMap()
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
        val rawData = getRawData()
        android.util.Log.d("DebugDataCollector", "createEncryptedScanData: rawData.size=${rawData.size}, tagSizeBytes=$tagSizeBytes, sectorCount=$sectorCount")
        
        return EncryptedScanData(
            id = System.currentTimeMillis(),
            timestamp = LocalDateTime.now(),
            tagUid = uid,
            technology = technology,
            encryptedData = rawData, // Unified storage: 768-byte or 1024-byte format
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
        return DecryptedScanData(
            id = System.currentTimeMillis() + 1, // Ensure different ID from encrypted
            timestamp = LocalDateTime.now(),
            tagUid = uid,
            technology = technology,
            scanResult = result,
            decryptedBlocks = getBlockData(), // All available blocks based on scan format
            authenticatedSectors = getAuthenticatedSectors(),
            failedSectors = getFailedSectors(),
            usedKeys = getUsedKeyTypes(),
            derivedKeys = getDerivedKeys(),
            errors = getErrorMessages(),
            keyDerivationTimeMs = keyDerivationTimeMs,
            authenticationTimeMs = authenticationTimeMs
        )
    }
}