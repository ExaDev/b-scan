package com.bscan.data.bambu

import com.bscan.model.SkuInfo

/**
 * Mapper for Bambu Lab variant IDs to filament codes (SKUs).
 * 
 * Maps variant IDs (e.g., "A00-K0") to 5-digit filament codes/SKUs (e.g., "10101")
 * extracted from the comprehensive Bambu Lab RFID library README documentation.
 * Contains ONLY verified data from the official README with confirmed variant IDs.
 */
object BambuVariantSkuMapper {
    
    /**
     * Comprehensive variant ID to SKU mappings
     * Based on 168 unique entries from test-data/rfid-library/README.md
     * Only includes variants with confirmed SKUs and ✅ status
     */
    private val variantSkuMappings = mapOf(
        // A00 series
        "A00-A0" to SkuInfo("10300", "Orange", "PLA Basic"),
        "A00-A1" to SkuInfo("10301", "Pumpkin Orange", "PLA Basic"),
        "A00-B1" to SkuInfo("10602", "Blue Grey", "PLA Basic"),
        "A00-B3" to SkuInfo("10604", "Cobalt Blue", "PLA Basic"),
        "A00-B5" to SkuInfo("10605", "Turquoise", "PLA Basic"),
        "A00-B8" to SkuInfo("10603", "Cyan", "PLA Basic"),
        "A00-D0" to SkuInfo("10103", "Gray", "PLA Basic"),
        "A00-D1" to SkuInfo("10102", "Silver", "PLA Basic"),
        "A00-D2" to SkuInfo("10104", "Light Gray", "PLA Basic"),
        "A00-D3" to SkuInfo("10105", "Dark Gray", "PLA Basic"),
        "A00-G1" to SkuInfo("10501", "Bambu Green", "PLA Basic"),
        "A00-G2" to SkuInfo("10502", "Mistletoe Green", "PLA Basic"),
        "A00-G3" to SkuInfo("10503", "Bright Green", "PLA Basic"),
        "A00-K0" to SkuInfo("10101", "Black", "PLA Basic"),
        "A00-M0" to SkuInfo("10900", "Arctic Whisper", "PLA Basic Gradient"),
        "A00-M1" to SkuInfo("10901", "Solar Breeze", "PLA Basic Gradient"),
        "A00-M2" to SkuInfo("10902", "Ocean to Meadow", "PLA Basic Gradient"),
        "A00-M3" to SkuInfo("10903", "Pink Citrus", "PLA Basic Gradient"),
        "A00-M4" to SkuInfo("10904", "Mint Lime", "PLA Basic Gradient"),
        "A00-M5" to SkuInfo("10905", "Blueberry Bubblegum", "PLA Basic Gradient"),
        "A00-M6" to SkuInfo("10906", "Dusk Glare", "PLA Basic Gradient"),
        "A00-M7" to SkuInfo("10907", "Cotton Candy Cloud", "PLA Basic Gradient"),
        "A00-N0" to SkuInfo("10800", "Brown", "PLA Basic"),
        "A00-N1" to SkuInfo("10802", "Cocoa Brown", "PLA Basic"),
        "A00-P0" to SkuInfo("10201", "Beige", "PLA Basic"),
        "A00-P2" to SkuInfo("10701", "Indigo Purple", "PLA Basic"),
        "A00-P5" to SkuInfo("10700", "Purple", "PLA Basic"),
        "A00-P6" to SkuInfo("10202", "Magenta", "PLA Basic"),
        "A00-R0" to SkuInfo("10200", "Red", "PLA Basic"),
        "A00-R2" to SkuInfo("10205", "Maroon Red", "PLA Basic"),
        "A00-R3" to SkuInfo("10204", "Hot Pink", "PLA Basic"),
        "A00-W1" to SkuInfo("10100", "Jade White", "PLA Basic"),
        "A00-Y0" to SkuInfo("10400", "Yellow", "PLA Basic"),
        "A00-Y2" to SkuInfo("10402", "Sunflower Yellow", "PLA Basic"),
        "A00-Y3" to SkuInfo("10801", "Bronze", "PLA Basic"),
        "A00-Y4" to SkuInfo("10401", "Gold", "PLA Basic"),

        // A01 series
        "A01-A2" to SkuInfo("11300", "Mandarin Orange", "PLA Matte"),
        "A01-B0" to SkuInfo("11603", "Sky Blue", "PLA Matte"),
        "A01-B3" to SkuInfo("11600", "Marine Blue", "PLA Matte"),
        "A01-B4" to SkuInfo("11601", "Ice Blue", "PLA Matte"),
        "A01-B6" to SkuInfo("11602", "Dark Blue", "PLA Matte"),
        "A01-D0" to SkuInfo("11104", "Nardo Gray", "PLA Matte"),
        "A01-D3" to SkuInfo("11102", "Ash Grey", "PLA Matte"),
        "A01-G0" to SkuInfo("11502", "Apple Green", "PLA Matte"),
        "A01-G1" to SkuInfo("11500", "Grass Green", "PLA Matte"),
        "A01-G7" to SkuInfo("11501", "Dark Green", "PLA Matte"),
        "A01-K1" to SkuInfo("11101", "Charcoal", "PLA Matte"),
        "A01-N0" to SkuInfo("11802", "Dark Chocolate", "PLA Matte"),
        "A01-N1" to SkuInfo("11800", "Latte Brown", "PLA Matte"),
        "A01-N2" to SkuInfo("11801", "Dark Brown", "PLA Matte"),
        "A01-N3" to SkuInfo("11803", "Caramel", "PLA Matte"),
        "A01-P3" to SkuInfo("11201", "Sakura Pink", "PLA Matte"),
        "A01-P4" to SkuInfo("11700", "Lilac Purple", "PLA Matte"),
        "A01-R1" to SkuInfo("11200", "Scarlet Red", "PLA Matte"),
        "A01-R2" to SkuInfo("11203", "Terracotta", "PLA Matte"),
        "A01-R3" to SkuInfo("11204", "Plum", "PLA Matte"),
        "A01-R4" to SkuInfo("11202", "Dark Red", "PLA Matte"),
        "A01-W2" to SkuInfo("11100", "Ivory White", "PLA Matte"),
        "A01-W3" to SkuInfo("11103", "Bone White", "PLA Matte"),
        "A01-Y2" to SkuInfo("11400", "Lemon Yellow", "PLA Matte"),
        "A01-Y3" to SkuInfo("11401", "Desert Tan", "PLA Matte"),

        // A02 series
        "A02-B2" to SkuInfo("13600", "Cobalt Blue Metallic", "PLA Metal"),

        // A05 series
        "A05-M1" to SkuInfo("13906", "South Beach", "PLA Silk Multi-Color"),
        "A05-M8" to SkuInfo("13912", "Dawn Radiance", "PLA Silk Multi-Color"),
        "A05-T1" to SkuInfo("13901", "Gilded Rose (Pink-Gold)", "PLA Silk Multi-Color"),
        "A05-T2" to SkuInfo("13902", "Midnight Blaze (Blue-Red)", "PLA Silk Multi-Color"),
        "A05-T3" to SkuInfo("13903", "Neon City (Blue-Magenta)", "PLA Silk Multi-Color"),
        "A05-T4" to SkuInfo("13904", "Blue Hawaii (Blue-Green)", "PLA Silk Multi-Color"),
        "A05-T5" to SkuInfo("13905", "Velvet Eclipse (Black-Red)", "PLA Silk Multi-Color"),

        // A06 series
        "A06-B0" to SkuInfo("13603", "Baby Blue", "PLA Silk+"),
        "A06-B1" to SkuInfo("13604", "Blue", "PLA Silk+"),
        "A06-D0" to SkuInfo("13108", "Titan Gray", "PLA Silk+"),
        "A06-D1" to SkuInfo("13109", "Silver", "PLA Silk+"),
        "A06-G0" to SkuInfo("13506", "Candy Green", "PLA Silk+"),
        "A06-G1" to SkuInfo("13507", "Mint", "PLA Silk+"),
        "A06-R0" to SkuInfo("13205", "Candy Red", "PLA Silk+"),
        "A06-R1" to SkuInfo("13206", "Rose Gold", "PLA Silk+"),
        "A06-R2" to SkuInfo("13207", "Pink", "PLA Silk+"),
        "A06-W0" to SkuInfo("13110", "White", "PLA Silk+"),
        "A06-Y0" to SkuInfo("13404", "Champagne", "PLA Silk+"),
        "A06-Y1" to SkuInfo("13405", "Gold", "PLA Silk+"),

        // A07 series
        "A07-D4" to SkuInfo("13103", "White Marble", "PLA Marble"),
        "A07-R5" to SkuInfo("13201", "Red Granite", "PLA Marble"),

        // A08 series
        "A08-B7" to SkuInfo("13700", "Royal Purple Sparkle", "PLA Sparkle"),
        "A08-D5" to SkuInfo("13102", "Slate Gray Sparkle", "PLA Sparkle"),
        "A08-G3" to SkuInfo("13501", "Alpine Green Sparkle", "PLA Sparkle"),
        "A08-K2" to SkuInfo("13101", "Onyx Black Sparkle", "PLA Sparkle"),
        "A08-R2" to SkuInfo("13200", "Crimson Red Sparkle", "PLA Sparkle"),
        "A08-Y1" to SkuInfo("13402", "Classic Gold Sparkle", "PLA Sparkle"),

        // A09 series
        "A09-B4" to SkuInfo("10601", "Blue", "PLA Basic"),

        // A11 series
        "A11-K0" to SkuInfo("14103", "Black", "PLA Aero"),
        "A11-W0" to SkuInfo("14102", "White", "PLA Aero"),

        // A12 series
        "A12-A0" to SkuInfo("15300", "Orange", "PLA Glow"),
        "A12-B0" to SkuInfo("15600", "Blue", "PLA Glow"),
        "A12-G0" to SkuInfo("15500", "Green", "PLA Glow"),
        "A12-R0" to SkuInfo("15200", "Pink", "PLA Glow"),
        "A12-Y0" to SkuInfo("15400", "Yellow", "PLA Glow"),

        // A15 series
        "A15-B0" to SkuInfo("13602", "Purple", "PLA Galaxy"),
        "A15-G0" to SkuInfo("13503", "Green", "PLA Galaxy"),
        "A15-G1" to SkuInfo("13504", "Nebulae", "PLA Galaxy"),
        "A15-R0" to SkuInfo("13203", "Brown", "PLA Galaxy"),

        // A16 series
        "A16-G0" to SkuInfo("13505", "Classic Birch", "PLA Wood"),
        "A16-K0" to SkuInfo("13107", "Black Walnut", "PLA Wood"),
        "A16-N0" to SkuInfo("13801", "Clay Brown", "PLA Wood"),
        "A16-R0" to SkuInfo("13204", "Rosewood", "PLA Wood"),
        "A16-W0" to SkuInfo("13106", "White Oak", "PLA Wood"),
        "A16-Y0" to SkuInfo("13403", "Ochre Yellow", "PLA Wood"),

        // A18 series
        "A18-B0" to SkuInfo("16600", "Cyan", "PLA Lite"),
        "A18-B1" to SkuInfo("16601", "Blue", "PLA Lite"),
        "A18-D0" to SkuInfo("16101", "Gray", "PLA Lite"),
        "A18-K0" to SkuInfo("16100", "Black", "PLA Lite"),
        "A18-P0" to SkuInfo("16602", "Matte Beige", "PLA Lite"),
        "A18-R0" to SkuInfo("16200", "Red", "PLA Lite"),
        "A18-W0" to SkuInfo("16103", "White", "PLA Lite"),
        "A18-Y0" to SkuInfo("16400", "Yellow", "PLA Lite"),

        // A50 series
        "A50-K0" to SkuInfo("14100", "Black", "PLA-CF"),

        // B00 series
        "B00-A0" to SkuInfo("40300", "Orange", "ABS"),
        "B00-B0" to SkuInfo("40600", "Blue", "ABS"),
        "B00-B4" to SkuInfo("40601", "Azure", "ABS"),
        "B00-B6" to SkuInfo("40602", "Navy Blue", "ABS"),
        "B00-D1" to SkuInfo("40102", "Silver", "ABS"),
        "B00-G6" to SkuInfo("40500", "Bambu Green", "ABS"),
        "B00-G7" to SkuInfo("40502", "Olive", "ABS"),
        "B00-K0" to SkuInfo("40101", "Black", "ABS"),
        "B00-R0" to SkuInfo("40200", "Red", "ABS"),
        "B00-W0" to SkuInfo("40100", "White", "ABS"),
        "B00-Y1" to SkuInfo("40402", "Tangerine Yellow", "ABS"),

        // B01 series
        "B01-D0" to SkuInfo("45102", "Gray", "ASA"),
        "B01-K0" to SkuInfo("45101", "Black", "ASA"),
        "B01-W0" to SkuInfo("45100", "White", "ASA"),

        // B02 series
        "B02-W0" to SkuInfo("46100", "White", "ASA Aero"),

        // B50 series
        "B50-A0" to SkuInfo("41300", "Orange", "ABS-GF"),

        // C00 series
        "C00-K0" to SkuInfo("60101", "Black", "PC"),
        "C00-W0" to SkuInfo("60100", "White", "PC"),

        // G01 series
        "G01-A0" to SkuInfo("32300", "Translucent Orange", "PETG Translucent"),
        "G01-B0" to SkuInfo("32600", "Translucent Light Blue", "PETG Translucent"),
        "G01-C0" to SkuInfo("32101", "Clear", "PETG Translucent"),
        "G01-D0" to SkuInfo("32100", "Translucent Gray", "PETG Translucent"),
        "G01-G0" to SkuInfo("32500", "Translucent Olive", "PETG Translucent"),
        "G01-G1" to SkuInfo("32501", "Translucent Teal", "PETG Translucent"),
        "G01-N0" to SkuInfo("32800", "Translucent Brown", "PETG Translucent"),
        "G01-P0" to SkuInfo("32700", "Translucent Purple", "PETG Translucent"),
        "G01-P1" to SkuInfo("32200", "Translucent Pink", "PETG Translucent"),

        // G02 series
        "G02-A0" to SkuInfo("33300", "Orange", "PETG HF"),
        "G02-B0" to SkuInfo("33600", "Blue", "PETG HF"),
        "G02-B1" to SkuInfo("33601", "Lake Blue", "PETG HF"),
        "G02-D0" to SkuInfo("33101", "Gray", "PETG HF"),
        "G02-D1" to SkuInfo("33103", "Dark Gray", "PETG HF"),
        "G02-G0" to SkuInfo("33500", "Green", "PETG HF"),
        "G02-G1" to SkuInfo("33501", "Lime Green", "PETG HF"),
        "G02-G2" to SkuInfo("33502", "Forest Green", "PETG HF"),
        "G02-K0" to SkuInfo("33102", "Black", "PETG HF"),
        "G02-N1" to SkuInfo("33801", "Peanut Brown", "PETG HF"),
        "G02-R0" to SkuInfo("33200", "Red", "PETG HF"),
        "G02-W0" to SkuInfo("33100", "White", "PETG HF"),
        "G02-Y0" to SkuInfo("33400", "Yellow", "PETG HF"),
        "G02-Y1" to SkuInfo("33401", "Cream", "PETG HF"),

        // G50 series
        "G50-K0" to SkuInfo("31100", "Black", "PETG-CF"),
        "G50-P7" to SkuInfo("31700", "Violet Purple", "PETG-CF"),

        // N04 series
        "N04-K0" to SkuInfo("70100", "Black", "PAHT-CF"),

        // N08 series
        "N08-K0" to SkuInfo("72104", "Black", "PA6-GF"),

        // S02 series
        "S02-W0" to SkuInfo("65102", "Nature", "Support for PLA/PETG"),

        // S04 series
        "S04-Y0" to SkuInfo("66400", "Clear", "PVA"),

        // S05 series
        "S05-C0" to SkuInfo("65103", "Black", "Support for PLA/PETG"),

        // S06 series
        "S06-W0" to SkuInfo("66100", "White", "Support for ABS"),

        // U02 series
        "U02-B0" to SkuInfo("53600", "Blue", "TPU for AMS"),
        "U02-D0" to SkuInfo("53102", "Gray", "TPU for AMS"),
        "U02-K0" to SkuInfo("53101", "Black", "TPU for AMS")
    )
    
    /**
     * Get SKU information by variant ID
     * @param variantId The variant ID from RFID tag (e.g., "A00-K0", "B00-B4")
     * @return SkuInfo or null if not found
     */
    fun getSkuByVariantId(variantId: String): SkuInfo? {
        return variantSkuMappings[variantId]
    }
    
    /**
     * Get SKU by material ID and variant ID combination
     * @param materialId The material ID from RFID tag (e.g., "GFA00", "GFB00")
     * @param variantId The variant ID from RFID tag (e.g., "A00-K0", "B00-B4")
     * @return 5-digit SKU string or null if not found
     */
    fun getSkuByRfidCode(materialId: String, variantId: String): String? {
        return variantSkuMappings[variantId]?.sku
    }
    
    /**
     * Get SKU by full RFID key
     * @param rfidKey The full RFID key (e.g., "GFA00:A00-K0", "GFB00:B00-B4")
     * @return 5-digit SKU string or null if not found
     */
    fun getSkuByFullRfidKey(rfidKey: String): String? {
        val variantId = rfidKey.split(":").getOrNull(1) ?: return null
        return variantSkuMappings[variantId]?.sku
    }
    
    /**
     * Check if a variant ID is known
     * @param variantId The variant ID to check
     * @return true if the variant ID is in the database
     */
    fun isKnownVariant(variantId: String): Boolean {
        return variantSkuMappings.containsKey(variantId)
    }
    
    /**
     * Get all known variant IDs
     * @return Set of all known variant IDs
     */
    fun getAllKnownVariants(): Set<String> {
        return variantSkuMappings.keys
    }
    
    /**
     * Get all SKU entries for a specific material type
     * @param materialType The material type to filter by (e.g., "PLA Basic", "ABS")
     * @return List of SkuInfo entries for the specified material type
     */
    fun getSkusForMaterialType(materialType: String): List<SkuInfo> {
        return variantSkuMappings.values.filter { it.materialType == materialType }
    }
    
    /**
     * Get variant ID by SKU (reverse lookup)
     * @param sku The 5-digit SKU to search for
     * @return Variant ID or null if not found
     */
    fun getVariantIdBySku(sku: String): String? {
        return variantSkuMappings.entries.find { it.value.sku == sku }?.key
    }
}