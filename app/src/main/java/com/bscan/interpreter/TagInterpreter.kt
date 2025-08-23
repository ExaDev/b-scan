package com.bscan.interpreter

import com.bscan.model.DecryptedScanData
import com.bscan.model.FilamentInfo
import com.bscan.model.TagFormat

/**
 * Abstract interface for interpreting different RFID tag formats
 */
interface TagInterpreter {
    /**
     * The tag format this interpreter supports
     */
    val tagFormat: TagFormat
    
    /**
     * Interpret decrypted scan data into FilamentInfo
     */
    fun interpret(decryptedData: DecryptedScanData): FilamentInfo?
    
    /**
     * Check if this interpreter can handle the given decrypted data
     */
    fun canInterpret(decryptedData: DecryptedScanData): Boolean {
        return decryptedData.tagFormat == tagFormat
    }
    
    /**
     * Get human-readable name for this interpreter
     */
    fun getDisplayName(): String
}