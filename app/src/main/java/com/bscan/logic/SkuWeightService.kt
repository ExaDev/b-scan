package com.bscan.logic

import com.bscan.model.*
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.ComponentRepository
import java.time.LocalDateTime

/**
 * Service for looking up component masses from SKU data.
 * Works with the new hierarchical Component system.
 */
class SkuWeightService(
    private val unifiedDataAccess: UnifiedDataAccess,
    private val componentRepository: ComponentRepository
) {
    
    companion object {
        private const val BAMBU_LAB_MANUFACTURER_ID = "bambu_lab"
    }
    
    /**
     * Get filament mass from SKU data with fallback defaults
     */
    fun getFilamentMassFromSku(filamentType: String, colorName: String): Float {
        val bestMatch = findBestProductMatch(filamentType, colorName)
        return if (bestMatch?.filamentWeightGrams != null) {
            bestMatch.filamentWeightGrams
        } else {
            getDefaultMassByMaterial(filamentType)
        }
    }
    
    /**
     * Get default mass based on material type
     */
    private fun getDefaultMassByMaterial(filamentType: String): Float {
        return when (filamentType.uppercase()) {
            "TPU" -> 500f  // TPU typically comes in 500g spools
            "PVA", "SUPPORT" -> 500f  // Support materials typically 500g
            "PC", "PA", "PAHT" -> 1000f  // Engineering materials typically 1kg
            else -> 1000f  // Standard 1kg for PLA, PETG, ABS, ASA
        }
    }
    
    /**
     * Find the best product match from catalog data
     */
    private fun findBestProductMatch(filamentType: String, colorName: String): ProductEntry? {
        // Since UnifiedDataAccess doesn't directly expose product search methods,
        // we need to add those methods. For now, we'll implement a workaround
        // by accessing the catalog indirectly through the manufacturer
        
        // Try to find products that match the material type and color name
        val matchingProducts = findProductsByColorAndMaterial(filamentType, colorName)
        
        // Return the first match, or null if none found
        return matchingProducts.firstOrNull()
    }
    
    /**
     * Find products by color name and material type
     * This is a simplified implementation that would need to be enhanced
     * once UnifiedDataAccess provides better product search methods
     */
    private fun findProductsByColorAndMaterial(filamentType: String, colorName: String): List<ProductEntry> {
        // For now, return empty list as this requires extending UnifiedDataAccess
        // with product search capabilities. The fallback to default weights will work.
        
        // TODO: This should be implemented by adding product search methods to UnifiedDataAccess
        // that delegate to CatalogRepository.findProducts()
        
        return emptyList()
    }
    
    /**
     * Get suggested spool type from SKU data
     */
    fun getSuggestedSpoolType(filamentType: String, colorName: String): SpoolPackaging? {
        val bestMatch = findBestProductMatch(filamentType, colorName)
        return bestMatch?.spoolType
    }
    
    /**
     * Create measurement record for component
     */
    fun createComponentMeasurement(
        componentId: String,
        measuredMass: Float,
        measurementType: MeasurementType = MeasurementType.TOTAL_MASS,
        notes: String = ""
    ): ComponentMeasurement {
        return ComponentMeasurement(
            id = "measurement_${System.currentTimeMillis()}",
            componentId = componentId,
            measuredMassGrams = measuredMass,
            measurementType = measurementType,
            measuredAt = LocalDateTime.now(),
            notes = notes
        )
    }
    
    /**
     * Check if a component can have its mass updated automatically from SKU data
     */
    fun canUpdateFromSku(component: Component): Boolean {
        return component.category == "filament" && component.variableMass
    }
    
    /**
     * Update component mass using latest SKU data
     */
    fun updateComponentFromSku(component: Component): Component? {
        if (!canUpdateFromSku(component)) return null
        
        // Extract material info from metadata if available
        val filamentType = component.metadata["filamentType"] ?: return null
        val colorName = component.metadata["colorName"] ?: return null
        
        val skuMass = getFilamentMassFromSku(filamentType, colorName)
        
        return component.copy(
            massGrams = skuMass,
            fullMassGrams = skuMass,
            lastUpdated = LocalDateTime.now(),
            metadata = component.metadata + ("source" to "sku-updated")
        )
    }
}