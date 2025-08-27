package com.bscan.data.bambu.base

/**
 * Abstract base implementation for Bambu Lab mappers.
 * Provides common functionality for all mappers to reduce code duplication.
 */
abstract class AbstractBambuMapper<T> : BambuMapper<T> {
    
    /**
     * The mapping data for this mapper.
     * Must be implemented by concrete mapper classes.
     */
    protected abstract val mappings: Map<String, T>
    
    /**
     * Extract display name from the mapping info.
     * Must be implemented by concrete mapper classes.
     */
    protected abstract fun extractDisplayName(info: T): String
    
    /**
     * Create unknown placeholder text for a code.
     * Can be overridden by concrete mappers for custom formatting.
     */
    protected open fun createUnknownPlaceholder(code: String): String {
        return "Unknown ($code)"
    }
    
    override fun getInfo(code: String): T? {
        return mappings[code]
    }
    
    override fun getDisplayName(code: String): String {
        return getInfo(code)?.let { extractDisplayName(it) } 
            ?: createUnknownPlaceholder(code)
    }
    
    override fun isKnown(code: String): Boolean {
        return mappings.containsKey(code)
    }
    
    override fun getAllKnownCodes(): Set<String> {
        return mappings.keys
    }
}