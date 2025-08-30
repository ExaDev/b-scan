package com.bscan.ui.screens.home

import com.bscan.model.FilamentInfo
import java.time.LocalDateTime

/**
 * UI-specific SKU information that aggregates scan data and inventory statistics.
 * This is different from the model-level SkuInfo which only contains basic catalog data.
 */
data class SkuInfo(
    val skuKey: String,                    // Unique SKU identifier (e.g., "bambu:30105")
    val filamentInfo: FilamentInfo,        // Interpreted filament data
    val filamentReelCount: Int,            // Number of unique reels/trays scanned
    val totalScans: Int,                   // Total scan attempts for this SKU
    val successfulScans: Int,              // Number of successful scans
    val lastScanned: LocalDateTime,        // Most recent scan timestamp
    val successRate: Float                 // Success rate (0.0 to 1.0)
) {
    /**
     * Check if this SKU has been scanned at least once
     */
    val hasBeenScanned: Boolean
        get() = totalScans > 0
    
    /**
     * Check if this SKU has successful scan data
     */
    val hasSuccessfulData: Boolean
        get() = successfulScans > 0
        
    /**
     * Get success rate as percentage
     */
    val successRatePercentage: Int
        get() = (successRate * 100).toInt()
}