package com.bscan.logic

import com.bscan.model.*
import com.bscan.repository.MappingsRepository
import com.bscan.repository.PhysicalComponentRepository
import java.time.LocalDateTime

/**
 * Service for creating automatic component setups from SKU data.
 * Simplified version for the new PhysicalComponent system.
 */
class SkuWeightService(
    private val mappingsRepository: MappingsRepository,
    private val physicalComponentRepository: PhysicalComponentRepository
) {
    
    /**
     * Create automatic component setup for a Bambu scan
     */
    fun createBambuComponentSetup(
        trayUid: String,
        filamentInfo: FilamentInfo,
        includeRefillableSpool: Boolean = false
    ): List<String> {
        // Get filament mass from SKU data if available
        val skuMass = getFilamentMassFromSku(filamentInfo.filamentType, filamentInfo.colorName)
        
        // Create filament component
        val filamentComponent = physicalComponentRepository.createFilamentComponent(
            filamentType = filamentInfo.filamentType,
            colorName = filamentInfo.colorName,
            colorHex = filamentInfo.colorHex,
            massGrams = skuMass ?: 1000f, // Default to 1kg if no SKU data
            manufacturer = filamentInfo.manufacturerName,
            fullMassGrams = skuMass // Use SKU mass as full mass when available
        )
        
        // Save the filament component
        physicalComponentRepository.saveComponent(filamentComponent)
        
        val componentIds = mutableListOf<String>()
        
        // Add the filament component
        componentIds.add(filamentComponent.id)
        
        // Always add cardboard core for Bambu
        componentIds.add("bambu_cardboard_core")
        
        // Optionally add refillable spool
        if (includeRefillableSpool) {
            componentIds.add("bambu_refillable_spool")
        }
        
        return componentIds
    }
    
    /**
     * Try to get filament mass from SKU data
     */
    private fun getFilamentMassFromSku(filamentType: String, colorName: String): Float? {
        val bestMatch = mappingsRepository.findBestProductMatch(filamentType, colorName)
        return bestMatch?.filamentWeightGrams
    }
    
    /**
     * Get suggested spool type from SKU data
     */
    fun getSuggestedSpoolType(filamentType: String, colorName: String): SpoolPackaging? {
        val bestMatch = mappingsRepository.findBestProductMatch(filamentType, colorName)
        return bestMatch?.spoolType
    }
    
    /**
     * Create measurement record for setup
     */
    fun createSetupMeasurement(
        trayUid: String,
        componentIds: List<String>,
        totalMass: Float
    ): MassMeasurement {
        return MassMeasurement(
            id = "setup_${System.currentTimeMillis()}",
            trayUid = trayUid,
            measuredMassGrams = totalMass,
            componentIds = componentIds,
            measurementType = MeasurementType.FULL_WEIGHT,
            measuredAt = LocalDateTime.now(),
            notes = "Initial setup measurement"
        )
    }
}