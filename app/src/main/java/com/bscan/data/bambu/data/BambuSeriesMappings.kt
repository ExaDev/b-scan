package com.bscan.data.bambu.data

import com.bscan.data.bambu.base.MappingInfo

/**
 * Bambu Lab series code mappings data.
 * 
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Maps series/batch codes extracted from Block 1 (bytes 0-7, first part before dash) of Bambu RFID tags.
 */
object BambuSeriesMappings {
    
    /**
     * Real Bambu series codes extracted from actual RFID tag data
     * Based on 428 .bin files from test-data/rfid-library/
     * Only contains display names - all other properties should be inferred elsewhere
     */
    val SERIES_CODE_MAPPINGS = mapOf(
        // PLA Series (A##)
        "A00" to MappingInfo("PLA Standard A00", "Standard PLA series A00"),
        "A01" to MappingInfo("PLA Standard A01", "Standard PLA series A01"),
        "A02" to MappingInfo("PLA Standard A02", "Standard PLA series A02"),
        "A05" to MappingInfo("PLA Standard A05", "Standard PLA series A05"),
        "A06" to MappingInfo("PLA Standard A06", "Standard PLA series A06"),
        "A07" to MappingInfo("PLA Standard A07", "Standard PLA series A07"),
        "A08" to MappingInfo("PLA Standard A08", "Standard PLA series A08"),
        "A09" to MappingInfo("PLA Standard A09", "Standard PLA series A09"),
        "A11" to MappingInfo("PLA Standard A11", "Standard PLA series A11"),
        "A12" to MappingInfo("PLA Standard A12", "Standard PLA series A12"),
        "A15" to MappingInfo("PLA Standard A15", "Standard PLA series A15"),
        "A16" to MappingInfo("PLA Standard A16", "Standard PLA series A16"),
        "A18" to MappingInfo("PLA Standard A18", "Standard PLA series A18"),
        "A50" to MappingInfo("PLA Carbon Fiber A50", "Carbon fiber reinforced PLA series"),
        
        // ABS Series (B##)
        "B00" to MappingInfo("ABS Standard B00", "Standard ABS series B00"),
        "B01" to MappingInfo("ASA Standard B01", "Standard ASA series B01"),
        "B02" to MappingInfo("ASA Aero B02", "ASA Aero lightweight series"),
        "B50" to MappingInfo("ABS Glass Fiber B50", "Glass fiber reinforced ABS series"),
        
        // PC Series (C##)
        "C00" to MappingInfo("PC Standard C00", "Standard PC (Polycarbonate) series C00"),
        
        // PETG Series (G##)
        "G00" to MappingInfo("PETG Standard G00", "Standard PETG series G00"),
        "G01" to MappingInfo("PETG Standard G01", "Standard PETG series G01"),
        "G02" to MappingInfo("PETG Standard G02", "Standard PETG series G02"),
        "G50" to MappingInfo("PETG Carbon Fiber G50", "Carbon fiber reinforced PETG series"),
        
        // Nylon Series (N##)
        "N04" to MappingInfo("PA Carbon Fiber N04", "Carbon fiber reinforced nylon series"),
        "N08" to MappingInfo("PA Glass Fiber N08", "Glass fiber reinforced nylon series"),
        
        // Support Series (S##)
        "S00" to MappingInfo("Support S00", "PLA support material series S00"),
        "S02" to MappingInfo("Support S02", "PLA support material series S02"),
        "S04" to MappingInfo("PVA Support S04", "Water-soluble PVA support material"),
        "S05" to MappingInfo("Support S05", "PLA support material series S05"),
        "S06" to MappingInfo("ABS Support S06", "ABS support material series S06"),
        
        // TPU Series (U##)
        "U02" to MappingInfo("TPU Flexible U02", "Flexible TPU for AMS system")
    )
}