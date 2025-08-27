package com.bscan.data.bambu.data

import com.bscan.data.bambu.base.MappingInfo

/**
 * Bambu Lab material ID mappings data.
 * 
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Maps internal RFID material codes extracted from Block 1 (bytes 8-15) of Bambu RFID tags.
 */
object BambuMaterialMappings {
    
    /**
     * Real Bambu material IDs extracted from actual RFID tag data
     * Based on 428 .bin files from test-data/rfid-library/
     * Only contains display names - all other properties should be inferred elsewhere
     */
    val MATERIAL_ID_MAPPINGS = mapOf(
        // PLA Series (GFA##)
        "GFA00" to MappingInfo("PLA Basic", "Standard PLA filament (series A00)"),
        "GFA01" to MappingInfo("PLA Basic", "Standard PLA filament (series A01)"),
        "GFA02" to MappingInfo("PLA Basic", "Standard PLA filament (series A02)"),
        "GFA05" to MappingInfo("PLA Basic", "Standard PLA filament (series A05)"),
        "GFA06" to MappingInfo("PLA Basic", "Standard PLA filament (series A06)"),
        "GFA07" to MappingInfo("PLA Basic", "Standard PLA filament (series A07)"),
        "GFA08" to MappingInfo("PLA Basic", "Standard PLA filament (series A08)"),
        "GFA09" to MappingInfo("PLA Basic", "Standard PLA filament (series A09)"),
        "GFA11" to MappingInfo("PLA Basic", "Standard PLA filament (series A11)"),
        "GFA12" to MappingInfo("PLA Basic", "Standard PLA filament (series A12)"),
        "GFA15" to MappingInfo("PLA Basic", "Standard PLA filament (series A15)"),
        "GFA16" to MappingInfo("PLA Basic", "Standard PLA filament (series A16)"),
        "GFA18" to MappingInfo("PLA Basic", "Standard PLA filament (series A18)"),
        "GFA50" to MappingInfo("PLA-CF", "Carbon fiber reinforced PLA"),
        
        // ABS Series (GFB##)
        "GFB00" to MappingInfo("ABS", "Standard ABS filament"),
        "GFB01" to MappingInfo("ASA", "UV-resistant ASA filament"),
        "GFB02" to MappingInfo("ASA Aero", "Lightweight ASA variant"),
        "GFB50" to MappingInfo("ABS-GF", "Glass fiber reinforced ABS"),
        
        // PC Series (GFC##)
        "GFC00" to MappingInfo("PC", "Polycarbonate engineering plastic"),
        
        // PETG Series (GFG##)
        "GFG00" to MappingInfo("PETG", "Standard PETG filament (series G00)"),
        "GFG01" to MappingInfo("PETG", "Standard PETG filament (series G01)"),
        "GFG02" to MappingInfo("PETG", "Standard PETG filament (series G02)"),
        "GFG50" to MappingInfo("PETG-CF", "Carbon fiber reinforced PETG"),
        
        // Nylon/PA Series (GFN##)
        "GFN04" to MappingInfo("PA-CF", "Carbon fiber reinforced nylon"),
        "GFN08" to MappingInfo("PA-GF", "Glass fiber reinforced nylon"),
        
        // Support Series (GFS##)
        "GFS00" to MappingInfo("PLA-S", "PLA support material (series S00)"),
        "GFS02" to MappingInfo("PLA-S", "PLA support material (series S02)"),
        "GFS04" to MappingInfo("PVA", "Water-soluble PVA support material"),
        "GFS05" to MappingInfo("PLA-S", "PLA support material (series S05)"),
        "GFS06" to MappingInfo("ABS-S", "ABS support material"),
        
        // TPU Series (GFU##)
        "GFU02" to MappingInfo("TPU-AMS", "Flexible TPU for AMS system")
    )
}