package com.bscan.data.bambu.rfid

import com.bscan.data.bambu.base.AbstractBambuMapper
import com.bscan.data.bambu.base.MappingInfo
import com.bscan.data.bambu.data.BambuMaterialMappings

/**
 * Mapper for Bambu Lab filament material IDs.
 * 
 * Maps internal RFID material codes extracted from Block 1 (bytes 8-15) of Bambu RFID tags.
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Contains ONLY information NOT available from RFID tag - just display names
 */
object BambuMaterialIdMapper : AbstractBambuMapper<MappingInfo>() {
    
    override val mappings = BambuMaterialMappings.MATERIAL_ID_MAPPINGS
    
    override fun extractDisplayName(info: MappingInfo) = info.displayName
    
    override fun createUnknownPlaceholder(code: String) = "Unknown Material ($code)"
    
    /**
     * Get material information by material ID
     * @param materialId The material ID from RFID tag (e.g., "GFA00")
     * @return MappingInfo or null if not found
     */
    fun getMaterialInfo(materialId: String): MappingInfo? {
        return getInfo(materialId)
    }
    
    /**
     * Check if a material ID is known
     * @param materialId The material ID to check
     * @return true if the material ID is in the database
     */
    fun isKnownMaterial(materialId: String): Boolean {
        return isKnown(materialId)
    }
    
    /**
     * Get all known material IDs
     * @return Set of all known material IDs
     */
    fun getAllKnownMaterialIds(): Set<String> {
        return getAllKnownCodes()
    }
}