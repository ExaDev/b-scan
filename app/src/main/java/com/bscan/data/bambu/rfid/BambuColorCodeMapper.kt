package com.bscan.data.bambu.rfid

import com.bscan.data.bambu.base.AbstractBambuMapper
import com.bscan.data.bambu.base.MappingInfo
import com.bscan.data.bambu.data.BambuColorMappings

/**
 * Mapper for Bambu Lab filament colour codes.
 * 
 * Maps colour variant codes extracted from Block 1 (bytes 0-7, part after dash) of Bambu RFID tags.
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Contains ONLY information NOT available from RFID tag - just display names (hex comes from Block 5)
 */
object BambuColorCodeMapper : AbstractBambuMapper<MappingInfo>() {
    
    override val mappings = BambuColorMappings.COLOR_CODE_MAPPINGS
    
    override fun extractDisplayName(info: MappingInfo) = info.displayName
    
    override fun createUnknownPlaceholder(code: String) = "Unknown Colour ($code)"
    
    
    /**
     * Get colour information by colour code
     * @param colourCode The colour code from RFID tag (e.g., "K0", "P7")
     * @return MappingInfo or null if not found
     */
    fun getColorInfo(colourCode: String): MappingInfo? {
        return getInfo(colourCode)
    }
    
    /**
     * Material-specific color override registry
     * Only contains colors that have different names compared to the base mapping
     */
    private sealed class MaterialColorOverride {
        abstract fun getColorOverride(colourCode: String): MappingInfo?
        
        object ABS : MaterialColorOverride() {
            private val overrides = mapOf(
                "B4" to MappingInfo("Azure", "Azure"),                     // SKU 40601 - ABS Azure (B00-B4)
                "B6" to MappingInfo("Navy Blue", "Navy Blue"),             // SKU 40602 - ABS Navy Blue (B00-B6)
                "G6" to MappingInfo("Bambu Green", "Bambu Green"),         // SKU 40500 - ABS Bambu Green (B00-G6)
                "G7" to MappingInfo("Olive", "Olive"),                     // SKU 40502 - ABS Olive (B00-G7)
                "Y1" to MappingInfo("Tangerine Yellow", "Tangerine Yellow") // SKU 40402 - ABS Tangerine Yellow (B00-Y1)
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        object PC : MaterialColorOverride() {
            private val overrides = mapOf(
                "K0" to MappingInfo("Clear Black", "Clear Black"),  // SKU 60101 - PC Black (C00-K0)
                "W0" to MappingInfo("Transparent", "Transparent")   // SKU 60100 - PC White (C00-W0)
                // TODO: Add when variant IDs are discovered:
                // "??" to MappingInfo("Transparent", "Transparent"),  // SKU 60103 - PC Transparent 
                // "??" to MappingInfo("Clear Black", "Clear Black")   // SKU 60102 - PC Clear Black
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        object PETGCarbonFiber : MaterialColorOverride() {
            private val overrides = mapOf(
                "P7" to MappingInfo("Violet Purple", "Violet Purple")      // SKU 31700 - PETG-CF Violet Purple (G50-P7)
                // TODO: Add when variant IDs are discovered:
                // "??" to MappingInfo("Indigo Blue", "Indigo Blue"),     // SKU 31600 - PETG-CF Indigo Blue
                // "??" to MappingInfo("Malachite Green", "Malachite Green"), // SKU 31500 - PETG-CF Malachite Green  
                // "??" to MappingInfo("Titan Gray", "Titan Gray"),       // SKU 31101 - PETG-CF Titan Gray
                // "??" to MappingInfo("Brick Red", "Brick Red")          // SKU 31200 - PETG-CF Brick Red
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        object PVA : MaterialColorOverride() {
            private val overrides = mapOf(
                "Y0" to MappingInfo("Clear", "Clear")              // SKU 66400 - PVA Clear (S04-Y0)
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        // TODO: Uncomment and add to materialOverrides when variant IDs are discovered:
        
        /*
        object PLACarbon : MaterialColorOverride() {
            private val overrides = mapOf(
                // TODO: Add when variant IDs are discovered:
                // "??" to MappingInfo("Burgundy Red", "Burgundy Red"),   // Filament Code 14200 - PLA-CF Burgundy Red
                // "??" to MappingInfo("Matcha Green", "Matcha Green"),   // Filament Code 14500 - PLA-CF Matcha Green
                // "??" to MappingInfo("Lava Gray", "Lava Gray"),         // Filament Code 14101 - PLA-CF Lava Gray
                // "??" to MappingInfo("Jeans Blue", "Jeans Blue"),       // Filament Code 14600 - PLA-CF Jeans Blue
                // "??" to MappingInfo("Royal Blue", "Royal Blue"),       // Filament Code 14601 - PLA-CF Royal Blue
                // "??" to MappingInfo("Iris Purple", "Iris Purple")      // Filament Code 14700 - PLA-CF Iris Purple
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        object PLAMetal : MaterialColorOverride() {
            private val overrides = mapOf(
                // TODO: Add when variant IDs are discovered:
                // "??" to MappingInfo("Oxide Green Metallic", "Oxide Green Metallic"),   // Filament Code 13500
                // "??" to MappingInfo("Iridium Gold Metallic", "Iridium Gold Metallic"), // Filament Code 13400
                // "??" to MappingInfo("Copper Brown Metallic", "Copper Brown Metallic"), // Filament Code 13800
                // "??" to MappingInfo("Iron Gray Metallic", "Iron Gray Metallic")        // Filament Code 13100
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        
        object TPUForAMS : MaterialColorOverride() {
            private val overrides = mapOf(
                // TODO: Add when variant IDs are discovered:
                // "??" to MappingInfo("Neon Green", "Neon Green")  // Filament Code 53500 - TPU Neon Green
                // Note: Other TPU colors appear to use standard base names
            )
            override fun getColorOverride(colourCode: String) = overrides[colourCode]
        }
        */
    }

    /**
     * Material-specific color override mappings
     * Only materials with actual color name differences are included
     */
    private val materialOverrides = mapOf(
        "GFB00" to MaterialColorOverride.ABS,           // ABS has Azure, Navy Blue, Bambu Green, Olive, Tangerine Yellow
        "GFC00" to MaterialColorOverride.PC,            // PC has Clear Black, Transparent  
        "GFG50" to MaterialColorOverride.PETGCarbonFiber, // PETG-CF has Violet Purple
        "GFS04" to MaterialColorOverride.PVA             // PVA has Clear instead of Yellow
        // TODO: Add when variant IDs are discovered and classes are uncommented:
        // "GFA50" to MaterialColorOverride.PLACarbon,     // PLA-CF material-specific colors
        // "GFA??" to MaterialColorOverride.PLAMetal,      // PLA Metal material-specific colors  
        // "GFU02" to MaterialColorOverride.TPUForAMS      // TPU material-specific colors (if Neon Green differs from base)
    )

    /**
     * Get material-aware colour information by colour code and material ID
     * Only returns material-specific colors when they differ from base colors
     * @param colourCode The colour code from RFID tag (e.g., "K0", "P7")
     * @param materialId The material ID from RFID tag (e.g., "GFB00", "GFC00", "GFG02")
     * @return MappingInfo with material-specific display name or fallback to base mapping
     */
    fun getColorInfoForMaterial(colourCode: String, materialId: String): MappingInfo? {
        // Check for material-specific color override first
        val override = materialOverrides[materialId]?.getColorOverride(colourCode)
        
        // Return material-specific color if available, otherwise fallback to base mapping
        return override ?: getColorInfo(colourCode)
    }
    
    
    /**
     * Check if a colour code is known
     * @param colourCode The colour code to check
     * @return true if the colour code is in the database
     */
    fun isKnownColour(colourCode: String): Boolean {
        return isKnown(colourCode)
    }
    
    /**
     * Get all known colour codes
     * @return Set of all known colour codes
     */
    fun getAllKnownColourCodes(): Set<String> {
        return getAllKnownCodes()
    }
}