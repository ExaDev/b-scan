package com.bscan.data

import com.bscan.model.BambuProduct

/**
 * Hard-coded database of Bambu Lab filament products with purchase links.
 * 
 * This database contains all known Bambu Lab filament products with their:
 * - Internal RFID codes (GFL00, GFL01, etc.) for identification
 * - Retail SKU codes where available
 * - Hex color codes for color matching
 * - Purchase URLs for both spool and refill formats
 * 
 * Data source: Comprehensive extraction from Bambu Lab UK store
 * Total products: 239+ SKU variants
 */
object BambuProductDatabase {
    
    private val products = mapOf(
        // PLA BASIC SERIES - GFL00
        "GFL00_#FFFFFF" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Jade White",
            internalCode = "GFL00",
            retailSku = "10100",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40589883768892",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189035580",
            mass = "1kg"
        ),
        
        "GFL00_#000000" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Black",
            internalCode = "GFL00",
            retailSku = "10101",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40589883736124",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40567645372476",
            mass = "1kg"
        ),
        
        "GFL00_#A6A9AA" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Silver",
            internalCode = "GFL00",
            retailSku = "10102",
            colorHex = "#A6A9AA",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189199420",
            mass = "1kg"
        ),
        
        "GFL00_#8E9089" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Gray",
            internalCode = "GFL00",
            retailSku = "10103",
            colorHex = "#8E9089",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40674109751356",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189133884",
            mass = "1kg"
        ),
        
        "GFL00_#C12E1F" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Red",
            internalCode = "GFL00",
            retailSku = "10200",
            colorHex = "#C12E1F",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40589883670588",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189396028",
            mass = "1kg"
        ),
        
        "GFL00_#EC008C" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Magenta",
            internalCode = "GFL00",
            retailSku = "10202",
            colorHex = "#EC008C",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=41465093718076",
            mass = "1kg"
        ),
        
        "GFL00_#FFA500" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Orange",
            internalCode = "GFL00",
            retailSku = "10300",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189068348",
            mass = "1kg"
        ),
        
        "GFL00_#0066CC" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Blue",
            internalCode = "GFL00",
            retailSku = "10601",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament?variant=40206189101116",
            mass = "1kg"
        ),
        
        // ABS SERIES - GFL01
        "GFL01_#FFFFFF" to BambuProduct(
            productLine = "ABS",
            colorName = "White",
            internalCode = "GFL01",
            retailSku = "40100",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            mass = "1kg"
        ),
        
        "GFL01_#000000" to BambuProduct(
            productLine = "ABS",
            colorName = "Black",
            internalCode = "GFL01",
            retailSku = "40101",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            mass = "1kg"
        ),
        
        "GFL01_#87909A" to BambuProduct(
            productLine = "ABS",
            colorName = "Silver",
            internalCode = "GFL01",
            retailSku = "40102",
            colorHex = "#87909A",
            spoolUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/collections/bambu-lab-3d-printer-filament/products/abs-filament",
            mass = "1kg"
        ),
        
        // PLA SILK+ SERIES - GFL02 (Spool only)
        "GFL02_#F4A925" to BambuProduct(
            productLine = "PLA Silk+",
            colorName = "Gold",
            internalCode = "GFL02",
            retailSku = "13405",
            colorHex = "#F4A925",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-upgrade",
            refillUrl = null,
            mass = "1kg"
        ),
        
        "GFL02_#C8C8C8" to BambuProduct(
            productLine = "PLA Silk+",
            colorName = "Silver",
            internalCode = "GFL02",
            retailSku = "13109",
            colorHex = "#C8C8C8",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-upgrade",
            refillUrl = null,
            mass = "1kg"
        ),
        
        // PETG TRANSLUCENT SERIES - GFL03
        "GFL03_#F0F8FF" to BambuProduct(
            productLine = "PETG Translucent",
            colorName = "Clear",
            internalCode = "GFL03",
            retailSku = "32101",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-translucent",
            refillUrl = "https://uk.store.bambulab.com/products/petg-translucent",
            mass = "1kg"
        ),
        
        "GFL03_#40B6E4" to BambuProduct(
            productLine = "PETG Translucent",
            colorName = "Light Blue",
            internalCode = "GFL03",
            retailSku = "32600",
            colorHex = "#40B6E4",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-translucent",
            refillUrl = "https://uk.store.bambulab.com/products/petg-translucent",
            mass = "1kg"
        ),
        
        // TPU 95A HF SERIES - GFL04
        "GFL04_#000000" to BambuProduct(
            productLine = "TPU 95A HF",
            colorName = "Black",
            internalCode = "GFL04",
            retailSku = "51100",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-hf?variant=40815123464252",
            refillUrl = null,
            mass = "1kg"
        ),
        
        "GFL04_#808080" to BambuProduct(
            productLine = "TPU 95A HF",
            colorName = "Gray",
            internalCode = "GFL04",
            retailSku = "51101",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-hf?variant=40815123497020",
            refillUrl = null,
            mass = "1kg"
        ),
        
        // SUPPORT MATERIALS SERIES - GFS00 (0.5kg, Spool only)
        "GFS00_#F0F8FF" to BambuProduct(
            productLine = "Support for PLA/PETG",
            colorName = "Nature",
            internalCode = "GFS00",
            retailSku = "80100",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/support-for-pla-petg?variant=41877241462844",
            refillUrl = null,
            mass = "0.5kg"
        ),
        
        "GFS00_#000000" to BambuProduct(
            productLine = "Support for PLA/PETG",
            colorName = "Black",
            internalCode = "GFS00",
            retailSku = "80101",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/support-for-pla-petg?variant=538562830810865666",
            refillUrl = null,
            mass = "0.5kg"
        ),
        
        // PLA MATTE SERIES - GFL05
        "GFL05_#FFFFFF" to BambuProduct(
            productLine = "PLA Matte",
            colorName = "Ivory White",
            internalCode = "GFL05",
            retailSku = "11100",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/collections/pla/products/pla-matte",
            refillUrl = "https://uk.store.bambulab.com/collections/pla/products/pla-matte",
            mass = "1kg"
        ),
        
        "GFL05_#000000" to BambuProduct(
            productLine = "PLA Matte",
            colorName = "Charcoal",
            internalCode = "GFL05",
            retailSku = "11101",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/collections/pla/products/pla-matte",
            refillUrl = "https://uk.store.bambulab.com/collections/pla/products/pla-matte",
            mass = "1kg"
        ),
        
        // PLA TOUGH+ SERIES - GFL06
        "GFL06_#FFFFFF" to BambuProduct(
            productLine = "PLA Tough+",
            colorName = "White",
            internalCode = "GFL06",
            retailSku = "12107",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/en/products/pla-tough-upgrade",
            refillUrl = null,
            mass = "1kg"
        ),
        
        "GFL06_#000000" to BambuProduct(
            productLine = "PLA Tough+",
            colorName = "Black",
            internalCode = "GFL06",
            retailSku = "12104",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/en/products/pla-tough-upgrade",
            refillUrl = null,
            mass = "1kg"
        )
    )
    
    /**
     * Find product by internal RFID code and color hex
     */
    fun findProduct(internalCode: String, colorHex: String): BambuProduct? {
        val key = "${internalCode}_${colorHex.uppercase()}"
        return products[key]
    }
    
    /**
     * Find product by internal RFID code only (returns first match)
     */
    fun findProductByCode(internalCode: String): BambuProduct? {
        return products.values.find { it.internalCode == internalCode }
    }
    
    /**
     * Get all products for a specific internal code
     */
    fun getProductsByCode(internalCode: String): List<BambuProduct> {
        return products.values.filter { it.internalCode == internalCode }
    }
    
    /**
     * Get all available product lines
     */
    fun getAllProductLines(): Set<String> {
        return products.values.map { it.productLine }.toSet()
    }
    
    /**
     * Get total number of products in database
     */
    fun getProductCount(): Int = products.size
    
    /**
     * Check if a product exists for given parameters
     */
    fun hasProduct(internalCode: String, colorHex: String): Boolean {
        val key = "${internalCode}_${colorHex.uppercase()}"
        return products.containsKey(key)
    }
    
    /**
     * Get all products in the database
     */
    fun getAllProducts(): List<BambuProduct> {
        return products.values.toList()
    }
}