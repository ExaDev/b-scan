package com.bscan.data.bambu.rfid

import com.bscan.data.bambu.NormalizedBambuData

/**
 * Mapper for Bambu Lab filament material IDs.
 *
 * Maps internal RFID material codes extracted from Block 1 (bytes 8-15) of Bambu RFID tags
 * to display names using the normalized data structure as the single source of truth.
 */
object BambuMaterialIdMapper {
    
    /**
     * Material ID to base material and variant mapping
     * This maps RFID material IDs to normalized material+variant combinations
     */
    private val materialIdMappings = mapOf(
        // PLA variants
        "GFA00" to ("PLA" to "Basic"),
        "GFA01" to ("PLA" to "Matte"), 
        "GFA02" to ("PLA" to "Metal"),
        "GFA05" to ("PLA" to "Silk"),
        "GFA07" to ("PLA" to "Marble"),
        "GFA12" to ("PLA" to "Glow"),
        
        // PETG variants  
        "GFG00" to ("PETG" to "Basic"),
        "GFG01" to ("PETG" to "Basic"),
        
        // ABS variants
        "GFL01" to ("ABS" to "Basic"),
        
        // ASA variants
        "GFL02" to ("ASA" to "Basic"),
        
        // PC variants
        "GFC00" to ("PC" to "Basic"),
        
        // PA variants  
        "GFN04" to ("PA" to "Basic"),
        
        // TPU variants
        "GFL04" to ("TPU" to "90A"),
        
        // Support variants
        "GFS00" to ("PVA" to "Basic"),
        "GFS01" to ("SUPPORT" to "Basic")
    )
    
    /**
     * Get display name for RFID material ID using normalized data
     */
    fun getDisplayName(materialId: String): String {
        val materialMapping = materialIdMappings[materialId]
        if (materialMapping == null) {
            return "Unknown ($materialId)"
        }
        
        val (baseMaterialName, variantName) = materialMapping
        val baseMaterial = NormalizedBambuData.baseMaterials.find { it.name == baseMaterialName }
        val variant = NormalizedBambuData.materialVariants.find { it.name == variantName }
        
        return when {
            baseMaterial != null && variant != null -> {
                if (variantName == "Basic") {
                    "Bambu ${baseMaterial.displayName}"
                } else {
                    "Bambu ${baseMaterial.displayName} ${variant.displayName}"
                }
            }
            baseMaterial != null -> "Bambu ${baseMaterial.displayName}"
            else -> "Unknown ($materialId)"
        }
    }
}