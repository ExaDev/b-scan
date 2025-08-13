package com.bscan.debug

import com.bscan.model.FilamentInfo
import com.bscan.model.NfcTagData
import com.bscan.model.ScanDebugInfo
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
import java.time.LocalDateTime

class DebugDataCollector {
    private val authenticatedSectors = mutableListOf<Int>()
    private val failedSectors = mutableListOf<Int>()
    private val usedKeyTypes = mutableMapOf<Int, String>()
    private val blockData = mutableMapOf<Int, String>()
    private val derivedKeys = mutableListOf<String>()
    private val errorMessages = mutableListOf<String>()
    private val parsingDetails = mutableMapOf<String, Any?>()
    private var rawColorBytes = ""
    private var tagSizeBytes = 0
    private var sectorCount = 0
    
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
    
    fun createScanHistory(
        uid: String,
        technology: String,
        result: ScanResult,
        filamentInfo: FilamentInfo?
    ): ScanHistory {
        return ScanHistory(
            id = System.currentTimeMillis(), // Simple ID generation
            timestamp = LocalDateTime.now(),
            uid = uid,
            technology = technology,
            scanResult = result,
            filamentInfo = filamentInfo,
            debugInfo = ScanDebugInfo(
                tagSizeBytes = tagSizeBytes,
                sectorCount = sectorCount,
                authenticatedSectors = authenticatedSectors.toList(),
                failedSectors = failedSectors.toList(),
                usedKeyTypes = usedKeyTypes.toMap(),
                blockData = blockData.toMap(),
                derivedKeys = derivedKeys.toList(),
                rawColorBytes = rawColorBytes,
                errorMessages = errorMessages.toList(),
                parsingDetails = parsingDetails.toMap()
            )
        )
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
    }
}