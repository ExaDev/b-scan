package com.bscan.ui.screens.settings

/**
 * Different modes for generating sample scan data in the settings screen.
 * Each mode provides different coverage and randomness characteristics.
 */
enum class DataGenerationMode(
    val displayName: String,
    val description: String
) {
    /**
     * Guarantees at least one scan of at least one tag for every SKU in the database,
     * plus additional random spools for variety and realistic distribution.
     * Each spool has 2 tags, but we may only have scanned 1 or both.
     * This is the recommended default for comprehensive testing.
     */
    COMPLETE_COVERAGE(
        displayName = "Complete Coverage",
        description = "Ensures all SKUs have at least one tag scanned, plus additional random data"
    ),
    
    /**
     * Pure random generation based on user-specified spool count.
     * Each spool has 2 tags, we randomly choose to scan 1 or both.
     * May not cover all available SKUs, but provides full control over quantity.
     */
    RANDOM_SAMPLE(
        displayName = "Random Sample", 
        description = "Generate specified number of random spools (may not cover all SKUs)"
    ),
    
    /**
     * Generates exactly one scan per SKU with minimal randomness.
     * Each spool has 2 tags but we only scan one for minimal data.
     * Useful for testing basic functionality without large datasets.
     */
    MINIMAL_COVERAGE(
        displayName = "Minimal Coverage",
        description = "Exactly one tag scan per SKU, minimal data for basic testing"
    );
    
    /**
     * Whether this mode guarantees complete SKU coverage
     */
    val guaranteesCompleteCoverage: Boolean
        get() = when (this) {
            COMPLETE_COVERAGE, MINIMAL_COVERAGE -> true
            RANDOM_SAMPLE -> false
        }
}