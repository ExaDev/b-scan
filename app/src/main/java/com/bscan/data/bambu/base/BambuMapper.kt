package com.bscan.data.bambu.base

/**
 * Base interface for all Bambu Lab mappers.
 * Provides consistent method signatures across material, series, color, and SKU mappers.
 */
interface BambuMapper<T> {
    
    /**
     * Get mapping information by code
     * @param code The code to look up (e.g., material ID, series code, color code)
     * @return Mapping information or null if not found
     */
    fun getInfo(code: String): T?
    
    /**
     * Get display name for a code
     * @param code The code to look up
     * @return Human-readable display name or unknown placeholder
     */
    fun getDisplayName(code: String): String
    
    /**
     * Check if a code is known
     * @param code The code to check
     * @return true if the code is in the mapping database
     */
    fun isKnown(code: String): Boolean
    
    /**
     * Get all known codes
     * @return Set of all known codes in the mapping
     */
    fun getAllKnownCodes(): Set<String>
}