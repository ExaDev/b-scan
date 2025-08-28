package com.bscan.data.bambu

import com.bscan.model.SkuInfo

/**
 * Simple data class for color and SKU information without material type redundancy
 * @param sku The 5-digit SKU code
 * @param colorName The human-readable color name
 * @param materialOverride Optional material type override for special cases (uses series default if null)
 */
data class ColorSku(
    val sku: String,
    val colorName: String,
    val materialOverride: String? = null
)

/**
 * Mapper for Bambu Lab RFID codes to filament SKUs.
 * 
 * Maps full RFID codes (e.g., "GFA00:A00-K0") to 5-digit filament codes/SKUs (e.g., "10101")
 * extracted from the comprehensive Bambu Lab RFID library README documentation.
 * Contains ONLY verified data from the official README with confirmed variant IDs.
 * 
 * Structure: MaterialID -> VariantID -> SkuInfo
 * RFID Key Format: "MaterialID:VariantID" (e.g., "GFA00:A00-K0")
 */
object BambuVariantSkuMapper {
    
    /**
     * Comprehensive RFID code to SKU mappings
     * Based on all verified entries from test-data/rfid-library/README.md
     * Includes all variants with confirmed SKUs and ✅ status across all material types
     * Contains commented placeholders for ❌ (unverified) entries for future completion
     * Organised by series code for explicit material ID validation
     */
    /**
     * Material type for each series code - single source of truth
     */
    private val seriesMaterialTypes = mapOf(
        "A00" to "PLA Basic",
        "A01" to "PLA Matte", 
        "A02" to "PLA Metal",
        "A05" to "PLA Silk Multi-Color",
        "A06" to "PLA Silk+",
        "A07" to "PLA Marble",
        "A08" to "PLA Sparkle",
        "A09" to "PLA Basic",
        "A11" to "PLA Aero",
        "A12" to "PLA Glow",
        "A15" to "PLA Galaxy",
        "A16" to "PLA Wood",
        "A18" to "PLA Lite",
        "A50" to "PLA-CF",
        "B00" to "ABS",
        "B01" to "ASA",
        "B02" to "ASA Aero",
        "B50" to "ABS-GF",
        "C00" to "PC",
        "G01" to "PETG Translucent",
        "G02" to "PETG HF",
        "G50" to "PETG-CF",
        "N04" to "PAHT-CF",
        "N08" to "PA6-GF",
        "S02" to "Support for PLA/PETG",
        "S04" to "PVA",
        "S05" to "Support for PLA/PETG",
        "S06" to "Support for ABS",
        "U02" to "TPU for AMS"
    )


    /**
     * Series-based SKU mappings organized by series code only.
     * No material prefix assumptions - lookup is controlled by materialIdMappings.
     * Uses ColorSku to eliminate material type redundancy.
     */
    private val seriesSkuMappings = mapOf(
        // PLA Basic & PLA Basic Gradient
        "A00" to mapOf(
            "A0" to ColorSku("10300", "Orange"),
            "A1" to ColorSku("10301", "Pumpkin Orange"),
            "B1" to ColorSku("10602", "Blue Grey"),
            "B3" to ColorSku("10604", "Cobalt Blue"),
            "B5" to ColorSku("10605", "Turquoise"),
            "B8" to ColorSku("10603", "Cyan"),
            "D0" to ColorSku("10103", "Gray"),
            "D1" to ColorSku("10102", "Silver"),
            "D2" to ColorSku("10104", "Light Gray"),
            "D3" to ColorSku("10105", "Dark Gray"),
            "G1" to ColorSku("10501", "Bambu Green"),
            "G2" to ColorSku("10502", "Mistletoe Green"),
            "G3" to ColorSku("10503", "Bright Green"),
            "K0" to ColorSku("10101", "Black"),
            // PLA Basic Gradient series - uses material override
            "M0" to ColorSku("10900", "Arctic Whisper", "PLA Basic Gradient"),
            "M1" to ColorSku("10901", "Solar Breeze", "PLA Basic Gradient"),
            "M2" to ColorSku("10902", "Ocean to Meadow", "PLA Basic Gradient"),
            "M3" to ColorSku("10903", "Pink Citrus", "PLA Basic Gradient"),
            "M4" to ColorSku("10904", "Mint Lime", "PLA Basic Gradient"),
            "M5" to ColorSku("10905", "Blueberry Bubblegum", "PLA Basic Gradient"),
            "M6" to ColorSku("10906", "Dusk Glare", "PLA Basic Gradient"),
            "M7" to ColorSku("10907", "Cotton Candy Cloud", "PLA Basic Gradient"),
            "N0" to ColorSku("10800", "Brown"),
            "N1" to ColorSku("10802", "Cocoa Brown"),
            "P0" to ColorSku("10201", "Beige"),
            "P2" to ColorSku("10701", "Indigo Purple"),
            "P5" to ColorSku("10700", "Purple"),
            "P6" to ColorSku("10202", "Magenta"),
            "R0" to ColorSku("10200", "Red"),
            "R2" to ColorSku("10205", "Maroon Red"),
            "R3" to ColorSku("10204", "Hot Pink"),
            "W1" to ColorSku("10100", "Jade White"),
            "Y0" to ColorSku("10400", "Yellow"),
            "Y2" to ColorSku("10402", "Sunflower Yellow"),
            "Y3" to ColorSku("10801", "Bronze"),
            "Y4" to ColorSku("10401", "Gold")
        ),
        
        // PLA Matte
        "A01" to mapOf(
            "A2" to ColorSku("11300", "Mandarin Orange"),
            "B0" to ColorSku("11603", "Sky Blue"),
            "B3" to ColorSku("11600", "Marine Blue"),
            "B4" to ColorSku("11601", "Ice Blue"),
            "B6" to ColorSku("11602", "Dark Blue"),
            "D0" to ColorSku("11104", "Nardo Gray"),
            "D3" to ColorSku("11102", "Ash Grey"),
            "G0" to ColorSku("11502", "Apple Green"),
            "G1" to ColorSku("11500", "Grass Green"),
            "G7" to ColorSku("11501", "Dark Green"),
            "K1" to ColorSku("11101", "Charcoal"),
            "N0" to ColorSku("11802", "Dark Chocolate"),
            "N1" to ColorSku("11800", "Latte Brown"),
            "N2" to ColorSku("11801", "Dark Brown"),
            "N3" to ColorSku("11803", "Caramel"),
            "P3" to ColorSku("11201", "Sakura Pink"),
            "P4" to ColorSku("11700", "Lilac Purple"),
            "R1" to ColorSku("11200", "Scarlet Red"),
            "R2" to ColorSku("11203", "Terracotta"),
            "R3" to ColorSku("11204", "Plum"),
            "R4" to ColorSku("11202", "Dark Red"),
            "W2" to ColorSku("11100", "Ivory White"),
            "W3" to ColorSku("11103", "Bone White"),
            "Y2" to ColorSku("11400", "Lemon Yellow"),
            "Y3" to ColorSku("11401", "Desert Tan")
        ),
        
        // PLA Metal
        "A02" to mapOf(
            "B2" to ColorSku("13600", "Cobalt Blue Metallic"),
            "D2" to ColorSku("13100", "Iron Gray Metallic"),
            "G1" to ColorSku("13500", "Oxide Green Metallic"),
            "N1" to ColorSku("13800", "Copper Brown Metallic"),
            "Y1" to ColorSku("13400", "Iridium Gold Metallic")
        ),
        
        // PLA Silk Multi-Color
        "A05" to mapOf(
            "M1" to ColorSku("13906", "South Beach"),
            "M8" to ColorSku("13912", "Dawn Radiance"),
            "M9" to ColorSku("13909", "Aurora Purple"),
            "T1" to ColorSku("13901", "Gilded Rose (Pink-Gold)"),
            "T2" to ColorSku("13902", "Midnight Blaze (Blue-Red)"),
            "T3" to ColorSku("13903", "Neon City (Blue-Magenta)"),
            "T4" to ColorSku("13904", "Blue Hawaii (Blue-Green)"),
            "T5" to ColorSku("13905", "Velvet Eclipse (Black-Red)")
        ),
        
        // PLA Silk+
        "A06" to mapOf(
            "B0" to ColorSku("13603", "Baby Blue"),
            "B1" to ColorSku("13604", "Blue"),
            "D0" to ColorSku("13108", "Titan Gray"),
            "D1" to ColorSku("13109", "Silver"),
            "G0" to ColorSku("13506", "Candy Green"),
            "G1" to ColorSku("13507", "Mint"),
            "P0" to ColorSku("13702", "Purple"),
            "R0" to ColorSku("13205", "Candy Red"),
            "R1" to ColorSku("13206", "Rose Gold"),
            "R2" to ColorSku("13207", "Pink"),
            "W0" to ColorSku("13110", "White"),
            "Y0" to ColorSku("13404", "Champagne"),
            "Y1" to ColorSku("13405", "Gold")
        ),
        
        // PLA Marble
        "A07" to mapOf(
            "D4" to ColorSku("13103", "White Marble"),
            "R5" to ColorSku("13201", "Red Granite")
        ),
        
        // PLA Sparkle
        "A08" to mapOf(
            "B7" to ColorSku("13700", "Royal Purple Sparkle"),
            "D5" to ColorSku("13102", "Slate Gray Sparkle"),
            "G3" to ColorSku("13501", "Alpine Green Sparkle"),
            "K2" to ColorSku("13101", "Onyx Black Sparkle"),
            "R2" to ColorSku("13200", "Crimson Red Sparkle"),
            "Y1" to ColorSku("13402", "Classic Gold Sparkle")
        ),
        
        // PLA Basic alternate
        "A09" to mapOf(
            "B4" to ColorSku("10601", "Blue")
        ),
        
        // PLA Aero
        "A11" to mapOf(
            "D0" to ColorSku("14104", "Gray"),
            "K0" to ColorSku("14103", "Black"),
            "W0" to ColorSku("14102", "White")
        ),
        
        // PLA Glow
        "A12" to mapOf(
            "A0" to ColorSku("15300", "Orange"),
            "B0" to ColorSku("15600", "Blue"),
            "G0" to ColorSku("15500", "Green"),
            "R0" to ColorSku("15200", "Pink"),
            "Y0" to ColorSku("15400", "Yellow")
        ),
        
        // PLA Galaxy
        "A15" to mapOf(
            "B0" to ColorSku("13602", "Purple"),
            "G0" to ColorSku("13503", "Green"),
            "G1" to ColorSku("13504", "Nebulae"),
            "R0" to ColorSku("13203", "Brown")
        ),
        
        // PLA Wood
        "A16" to mapOf(
            "G0" to ColorSku("13505", "Classic Birch"),
            "K0" to ColorSku("13107", "Black Walnut"),
            "N0" to ColorSku("13801", "Clay Brown"),
            "R0" to ColorSku("13204", "Rosewood"),
            "W0" to ColorSku("13106", "White Oak"),
            "Y0" to ColorSku("13403", "Ochre Yellow")
        ),
        
        // PLA Lite
        "A18" to mapOf(
            "B0" to ColorSku("16600", "Cyan"),
            "B1" to ColorSku("16601", "Blue"),
            "D0" to ColorSku("16101", "Gray"),
            "K0" to ColorSku("16100", "Black"),
            "P0" to ColorSku("16602", "Matte Beige"),
            "R0" to ColorSku("16200", "Red"),
            "W0" to ColorSku("16103", "White"),
            "Y0" to ColorSku("16400", "Yellow")
        ),
        
        // PLA-CF
        "A50" to mapOf(
            "B1" to ColorSku("14600", "Jeans Blue"),
            "B6" to ColorSku("14601", "Royal Blue"),
            "D6" to ColorSku("14101", "Lava Gray"),
            "G1" to ColorSku("14500", "Matcha Green"),
            "K0" to ColorSku("14100", "Black"),
            "P1" to ColorSku("14700", "Iris Purple"),
            "R1" to ColorSku("14200", "Burgundy Red")
        ),
        
        // ABS
        "B00" to mapOf(
            "A0" to ColorSku("40300", "Orange"),
            "B0" to ColorSku("40600", "Blue"),
            "B4" to ColorSku("40601", "Azure"),
            "B6" to ColorSku("40602", "Navy Blue"),
            "D1" to ColorSku("40102", "Silver"),
            "G6" to ColorSku("40500", "Bambu Green"),
            "G7" to ColorSku("40502", "Olive"),
            "K0" to ColorSku("40101", "Black"),
            "R0" to ColorSku("40200", "Red"),
            "W0" to ColorSku("40100", "White"),
            "Y1" to ColorSku("40402", "Tangerine Yellow")
        ),
        
        // ASA
        "B01" to mapOf(
            "B0" to ColorSku("45600", "Blue"),
            "D0" to ColorSku("45102", "Gray"),
            "G0" to ColorSku("45500", "Green"),
            "K0" to ColorSku("45101", "Black"),
            "R0" to ColorSku("45200", "Red"),
            "W0" to ColorSku("45100", "White")
        ),
        
        // ASA Aero
        "B02" to mapOf(
            "W0" to ColorSku("46100", "White")
        ),
        
        // ABS-GF
        "B50" to mapOf(
            "A0" to ColorSku("41300", "Orange")
            // TODO: Add missing ABS-GF variants when variant IDs are confirmed:
            // "?" to ColorSku("41500", "Green"),   // ❌
            // "?" to ColorSku("41200", "Red"),     // ❌
            // "?" to ColorSku("41400", "Yellow"),  // ❌
            // "?" to ColorSku("41600", "Blue"),    // ❌
            // "?" to ColorSku("41100", "White"),   // ❌
            // "?" to ColorSku("41102", "Gray"),    // ❌
            // "?" to ColorSku("41101", "Black")    // ❌
        ),
        
        // PC
        "C00" to mapOf(
            "C1" to ColorSku("60103", "Transparent"),
            "K0" to ColorSku("60101", "Black"),
            "K1" to ColorSku("60102", "Clear Black"),
            "W0" to ColorSku("60100", "White")
        ),
        
        // PETG Translucent
        "G01" to mapOf(
            "A0" to ColorSku("32300", "Translucent Orange"),
            "B0" to ColorSku("32600", "Translucent Light Blue"),
            "C0" to ColorSku("32101", "Clear"),
            "D0" to ColorSku("32100", "Translucent Gray"),
            "G0" to ColorSku("32500", "Translucent Olive"),
            "G1" to ColorSku("32501", "Translucent Teal"),
            "N0" to ColorSku("32800", "Translucent Brown"),
            "P0" to ColorSku("32700", "Translucent Purple"),
            "P1" to ColorSku("32200", "Translucent Pink")
        ),
        
        // PETG HF
        "G02" to mapOf(
            "A0" to ColorSku("33300", "Orange"),
            "B0" to ColorSku("33600", "Blue"),
            "B1" to ColorSku("33601", "Lake Blue"),
            "D0" to ColorSku("33101", "Gray"),
            "D1" to ColorSku("33103", "Dark Gray"),
            "G0" to ColorSku("33500", "Green"),
            "G1" to ColorSku("33501", "Lime Green"),
            "G2" to ColorSku("33502", "Forest Green"),
            "K0" to ColorSku("33102", "Black"),
            "N1" to ColorSku("33801", "Peanut Brown"),
            "R0" to ColorSku("33200", "Red"),
            "W0" to ColorSku("33100", "White"),
            "Y0" to ColorSku("33400", "Yellow"),
            "Y1" to ColorSku("33401", "Cream")
        ),
        
        // PETG-CF
        "G50" to mapOf(
            "K0" to ColorSku("31100", "Black"),
            "P7" to ColorSku("31700", "Violet Purple")
            // TODO: Add missing PETG-CF variants when variant IDs are confirmed:
            // "?" to ColorSku("31600", "Indigo Blue"),     // ❌
            // "?" to ColorSku("31500", "Malachite Green"), // ❌
            // "?" to ColorSku("31101", "Titan Gray"),      // ❌
            // "?" to ColorSku("31200", "Brick Red")        // ❌
        ),
        
        // PAHT-CF
        "N04" to mapOf(
            "K0" to ColorSku("70100", "Black")
        ),
        
        // PA6-GF
        "N08" to mapOf(
            "K0" to ColorSku("72104", "Black")
            // TODO: Add missing PA6-GF variants when variant IDs are confirmed:
            // "?" to ColorSku("72600", "Blue"),     // ❌
            // "?" to ColorSku("72200", "Orange"),   // ❌
            // "?" to ColorSku("72400", "Yellow"),   // ❌
            // "?" to ColorSku("72500", "Lime"),     // ❌
            // "?" to ColorSku("72800", "Brown"),    // ❌
            // "?" to ColorSku("72102", "White"),    // ❌
            // "?" to ColorSku("72103", "Gray")      // ❌
        ),
        
        // Support for PLA/PETG
        "S02" to mapOf(
            "W0" to ColorSku("65102", "Nature")
        ),
        
        // PVA Support
        "S04" to mapOf(
            "Y0" to ColorSku("66400", "Clear")
        ),
        
        // Support for PLA/PETG - Black
        "S05" to mapOf(
            "C0" to ColorSku("65103", "Black")
        ),
        
        // Support for ABS
        "S06" to mapOf(
            "W0" to ColorSku("66100", "White")
        ),
        
        // TPU for AMS
        "U02" to mapOf(
            "B0" to ColorSku("53600", "Blue"),
            "D0" to ColorSku("53102", "Gray"),
            "K0" to ColorSku("53101", "Black")
            // TODO: Add missing TPU variants when variant IDs are confirmed:
            // "?" to ColorSku("53200", "Red"),         // ❌
            // "?" to ColorSku("53400", "Yellow"),      // ❌
            // "?" to ColorSku("53500", "Neon Green"),  // ❌
            // "?" to ColorSku("53100", "White")        // ❌
        )
        
        // TODO: Add series for materials with no confirmed variant IDs:
        // PLA Tough+ series - need variant IDs for all colors (❌)
        // PC FR series - need variant IDs for all colors (❌) 
        // ASA-CF series - need variant IDs (❌)
        // Support for PLA (New Version) - need variant IDs (❌)
        // Support for PA/PET - need variant IDs (❌)
    )
    
    /**
     * Explicit mapping of material IDs to their valid series codes.
     * Every valid combination must be explicitly listed here - no pattern assumptions or fallbacks.
     */
    private val materialIdMappings = mapOf(
        // PLA variants - explicitly map each material ID to its valid series
        "GFA00" to setOf("A00"),  // PLA Basic & PLA Basic Gradient
        "GFA01" to setOf("A01"),  // PLA Matte
        "GFA02" to setOf("A02"),  // PLA Metal
        "GFA05" to setOf("A05"),  // PLA Silk Multi-Color
        "GFA06" to setOf("A06"),  // PLA Silk+
        "GFA07" to setOf("A07"),  // PLA Marble
        "GFA08" to setOf("A08"),  // PLA Sparkle
        "GFA09" to setOf("A09"),  // PLA Basic alternate
        "GFA11" to setOf("A11"),  // PLA Aero
        "GFA12" to setOf("A12"),  // PLA Glow
        "GFA15" to setOf("A15"),  // PLA Galaxy
        "GFA16" to setOf("A16"),  // PLA Wood
        "GFA18" to setOf("A18"),  // PLA Lite
        "GFA50" to setOf("A50"),  // PLA-CF
        
        // ABS variants
        "GFB00" to setOf("B00"),  // ABS
        "GFB50" to setOf("B50"),  // ABS-GF
        
        // ASA variants
        "GFB01" to setOf("B01"),  // ASA
        "GFB02" to setOf("B02"),  // ASA Aero
        // "GFB51" to setOf("B51"),  // ASA-CF (TODO: need to confirm series code)
        
        // PC variants
        "GFC00" to setOf("C00"),  // PC
        // "GFC01" to setOf("C01"),  // PC FR (TODO: need to confirm series code)
        
        // PETG variants
        "GFG01" to setOf("G01"),  // PETG Translucent
        "GFG02" to setOf("G02"),  // PETG HF
        "GFG50" to setOf("G50"),  // PETG-CF
        
        // PA (Nylon) variants
        "GFN04" to setOf("N04"),  // PAHT-CF
        "GFN08" to setOf("N08"),  // PA6-GF
        
        // Support materials
        "GFS02" to setOf("S02"),  // Support for PLA/PETG
        "GFS04" to setOf("S04"),  // PVA Support
        "GFS05" to setOf("S05"),  // Support for PLA/PETG - Black
        "GFS06" to setOf("S06"),  // Support for ABS
        
        // TPU variants
        "GFU02" to setOf("U02")   // TPU for AMS
        
        // TODO: Add material IDs when variant data becomes available:
        // PLA Tough+ series - need material ID and variant confirmations (❌)
        // PC FR series - need material ID confirmation (❌)
        // ASA-CF series - need material ID confirmation (❌)
        // Support for PLA (New Version) - need material ID confirmation (❌)
        // Support for PA/PET - need material ID confirmation (❌)
        
        // Future: If Bambu breaks the pattern, add explicit exceptions:
        // "GFA00" to setOf("A00", "A01"),  // If GFA00 ever maps to A01 series
    )
    
    /**
     * Get SKU information by full RFID key
     * @param rfidKey The full RFID key (e.g., "GFA00:A00-K0", "GFB00:B00-B4")
     * @return SkuInfo or null if not found
     */
    private val tagUidSkuMappings = mapOf(
        "0090F85E" to SkuInfo("70100", "Black", "PAHT-CF")
    )

    fun getSkuByRfidKey(rfidKey: String): SkuInfo? {
        val parts = rfidKey.split(":")
        if (parts.size != 2) return null
        
        val materialId = parts[0] // e.g., "GFA00"
        val variantId = parts[1] // e.g., "A00-K0"

        if (tagUidSkuMappings.containsKey(variantId)) {
            return tagUidSkuMappings[variantId]
        }
        
        val seriesCode = variantId.split("-").getOrNull(0) ?: return null // "A00"
        val colorCode = variantId.split("-").getOrNull(1) ?: return null // "K0"
        
        // Only proceed if this material ID to series combination is explicitly mapped
        val validSeries = materialIdMappings[materialId] ?: return null
        if (seriesCode !in validSeries) return null
        
        // Look up ColorSku in the series-based structure  
        val colorSku = seriesSkuMappings[seriesCode]?.get(colorCode) ?: return null
        
        // Construct SkuInfo dynamically from ColorSku and series material type
        val materialType = colorSku.materialOverride ?: seriesMaterialTypes[seriesCode] ?: return null
        
        return SkuInfo(
            sku = colorSku.sku,
            colorName = colorSku.colorName,
            materialType = materialType
        )
    }
    
    /**
     * Get SKU by material ID and variant ID combination
     * @param materialId The material ID from RFID tag (e.g., "GFA00", "GFB00")
     * @param variantId The variant ID from RFID tag (e.g., "A00-K0", "B00-B4")
     * @return 5-digit SKU string or null if not found
     */
    fun getSkuByRfidCode(materialId: String, variantId: String): String? {
        val rfidKey = "$materialId:$variantId"
        return getSkuByRfidKey(rfidKey)?.sku
    }
    
    /**
     * Get SKU by full RFID key
     * @param rfidKey The full RFID key (e.g., "GFA00:A00-K0", "GFB00:B00-B4")
     * @return 5-digit SKU string or null if not found
     */
    fun getSkuByFullRfidKey(rfidKey: String): String? {
        return getSkuByRfidKey(rfidKey)?.sku
    }
    
    /**
     * Check if an RFID key is known
     * @param rfidKey The RFID key to check (e.g., "GFA00:A00-K0")
     * @return true if the RFID key is in the database
     */
    fun isKnownRfidKey(rfidKey: String): Boolean {
        return getSkuByRfidKey(rfidKey) != null
    }
    
    /**
     * Get all known RFID keys
     * @return Set of all known RFID keys in format "MaterialID:VariantID" 
     */
    fun getAllKnownRfidKeys(): Set<String> {
        val rfidKeys = mutableSetOf<String>()
        
        // Only generate keys for explicitly mapped material ID to series combinations
        materialIdMappings.forEach { (materialId, validSeriesCodes) ->
            validSeriesCodes.forEach { seriesCode ->
                // Get all color codes for this series
                seriesSkuMappings[seriesCode]?.keys?.forEach { colorCode ->
                    rfidKeys.add("$materialId:$seriesCode-$colorCode")
                }
            }
        }

        tagUidSkuMappings.keys.forEach { tagUid ->
            rfidKeys.add("GFN04:$tagUid")
        }
        
        return rfidKeys
    }
    
    /**
     * Get all SKU entries for a specific material type
     * @param materialType The material type to filter by (e.g., "PLA Basic", "ABS")
     * @return List of SkuInfo entries for the specified material type
     */
    fun getSkusForMaterialType(materialType: String): List<SkuInfo> {
        val allSkus = mutableListOf<SkuInfo>()
        seriesSkuMappings.forEach { (seriesCode, colorMap) ->
            val seriesMaterialType = seriesMaterialTypes[seriesCode] ?: return@forEach
            colorMap.values.forEach { colorSku ->
                val actualMaterialType = colorSku.materialOverride ?: seriesMaterialType
                if (actualMaterialType == materialType) {
                    allSkus.add(SkuInfo(
                        sku = colorSku.sku,
                        colorName = colorSku.colorName,
                        materialType = actualMaterialType
                    ))
                }
            }
        }
        return allSkus
    }
    
    /**
     * Get variant ID by SKU (reverse lookup)
     * @param sku The 5-digit SKU to search for
     * @return Variant ID or null if not found
     */
    fun getVariantIdBySku(sku: String): String? {
        seriesSkuMappings.forEach { (seriesCode, colorMap) ->
            colorMap.forEach { (colorCode, colorSku) ->
                if (colorSku.sku == sku) {
                    return "$seriesCode-$colorCode"
                }
            }
        }
        return null
    }
}