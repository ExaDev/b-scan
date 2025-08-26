package com.bscan.interpreter

import com.bscan.model.DecryptedScanData
import com.bscan.model.FilamentInfo
import com.bscan.model.TagFormat
import com.bscan.model.TagFormat.*
import com.bscan.model.tagFormat
import com.bscan.repository.UnifiedDataAccess

/**
 * Factory for creating and managing tag interpreters for different formats
 */
class InterpreterFactory(
    private val unifiedDataAccess: UnifiedDataAccess
) {
    private val interpreters = mutableMapOf<TagFormat, TagInterpreter>()
    
    init {
        // Initialize interpreters for each supported format
        interpreters[TagFormat.BAMBU_PROPRIETARY] = BambuFormatInterpreter(unifiedDataAccess.getCurrentMappings(), unifiedDataAccess)
        interpreters[TagFormat.CREALITY_ASCII] = CrealityFormatInterpreter()
        interpreters[TagFormat.OPENTAG_V1] = OpenTagInterpreter()
    }
    
    /**
     * Get appropriate interpreter for the given tag format
     */
    fun getInterpreter(tagFormat: TagFormat): TagInterpreter? {
        return interpreters[tagFormat]
    }
    
    /**
     * Interpret decrypted scan data using the appropriate format interpreter
     */
    fun interpret(decryptedData: DecryptedScanData): FilamentInfo? {
        // First, try the tagged format
        if (decryptedData.tagFormat != TagFormat.UNKNOWN) {
            val interpreter = getInterpreter(decryptedData.tagFormat)
            if (interpreter != null && interpreter.canInterpret(decryptedData)) {
                return interpreter.interpret(decryptedData)
            }
        }
        
        // If format is UNKNOWN or tagged interpreter failed, try all interpreters
        // This handles legacy scan data that was stored with UNKNOWN format
        for ((_, interpreter) in interpreters) {
            if (interpreter.canInterpret(decryptedData)) {
                return interpreter.interpret(decryptedData)
            }
        }
        
        return null
    }
    
    /**
     * Refresh mappings for interpreters that use them (like Bambu format)
     */
    fun refreshMappings() {
        val updatedMappings = unifiedDataAccess.getCurrentMappings()
        interpreters[TagFormat.BAMBU_PROPRIETARY] = BambuFormatInterpreter(updatedMappings, unifiedDataAccess)
    }
    
    /**
     * Get all supported tag formats
     */
    fun getSupportedFormats(): List<TagFormat> {
        return interpreters.keys.toList()
    }
    
    /**
     * Get display names for all supported interpreters
     */
    fun getSupportedInterpreterNames(): Map<TagFormat, String> {
        return interpreters.mapValues { it.value.getDisplayName() }
    }
}