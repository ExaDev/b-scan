package com.bscan.data.bambu.rfid

import com.bscan.data.bambu.base.AbstractBambuMapper
import com.bscan.data.bambu.base.MappingInfo
import com.bscan.data.bambu.data.BambuSeriesMappings

/**
 * Mapper for Bambu Lab filament series codes.
 * 
 * Maps series/batch codes extracted from Block 1 (bytes 0-7, first part before dash) of Bambu RFID tags.
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Contains ONLY information NOT available from RFID tag - just display names
 */
object BambuSeriesCodeMapper : AbstractBambuMapper<MappingInfo>() {
    
    override val mappings = BambuSeriesMappings.SERIES_CODE_MAPPINGS
    
    override fun extractDisplayName(info: MappingInfo) = info.displayName
    
    override fun createUnknownPlaceholder(code: String) = "Unknown Series ($code)"
    
    /**
     * Get series information by series code
     * @param seriesCode The series code from RFID tag (e.g., "A00", "G50")
     * @return MappingInfo or null if not found
     */
    fun getSeriesInfo(seriesCode: String): MappingInfo? {
        return getInfo(seriesCode)
    }
    
    /**
     * Check if a series code is known
     * @param seriesCode The series code to check
     * @return true if the series code is in the database
     */
    fun isKnownSeries(seriesCode: String): Boolean {
        return isKnown(seriesCode)
    }
    
    /**
     * Get all known series codes
     * @return Set of all known series codes
     */
    fun getAllKnownSeriesCodes(): Set<String> {
        return getAllKnownCodes()
    }
}