package com.bscan.tools

import com.bscan.tools.core.BambuStoreScraper
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Simple script to update Bambu Lab product catalog
 * Generates FilamentMappings JSON format compatible with B-Scan app
 */

data class FilamentMappings(
    val version: Int,
    val lastUpdated: String,
    val colorMappings: Map<String, String>,
    val materialMappings: Map<String, String>,
    val brandMappings: Map<String, String>,
    val temperatureMappings: Map<String, TemperatureRange>,
    val productCatalog: List<ProductEntry>
)

data class TemperatureRange(
    val minNozzle: Int,
    val maxNozzle: Int,
    val bed: Int
)

data class ProductEntry(
    val variantId: String,
    val productHandle: String,
    val productName: String,
    val colorName: String,
    val colorHex: String?,
    val colorCode: String,
    val price: Double,
    val available: Boolean,
    val url: String,
    val manufacturer: String,
    val materialType: String,
    val internalCode: String
)

fun main() = runBlocking {
    println("🔍 Updating Bambu Lab product catalog...")
    
    val scraper = BambuStoreScraper()
    
    try {
        // Fetch product URLs
        println("📥 Fetching product URLs...")
        val urls = scraper.fetchProductUrls()
        println("Found ${urls.size} product URLs")
        
        // Extract product data
        val allProducts = mutableListOf<ProductEntry>()
        var processed = 0
        
        println("📊 Extracting product data...")
        for ((index, url) in urls.withIndex()) {
            val progress = "${index + 1}/${urls.size}"
            println("[$progress] Processing: ${url.substringAfterLast("/")}")
            
            val products = scraper.extractProductData(url)
            products.forEach { product ->
                allProducts.add(
                    ProductEntry(
                        variantId = product.variantId,
                        productHandle = product.productHandle,
                        productName = product.productName,
                        colorName = product.colorName,
                        colorHex = product.colorHex,
                        colorCode = product.colorCode,
                        price = product.price,
                        available = product.available,
                        url = product.url,
                        manufacturer = "Bambu Lab",
                        materialType = extractMaterialType(product.productHandle),
                        internalCode = generateInternalCode(product.productHandle)
                    )
                )
            }
            processed++
        }
        
        println("✅ Extraction complete: ${allProducts.size} SKUs found")
        
        // Generate updated color mappings from scraped data
        val colorMappings = generateColorMappings(allProducts)
        val materialMappings = generateMaterialMappings(allProducts)
        
        // Create FilamentMappings structure
        val mappings = FilamentMappings(
            version = (System.currentTimeMillis() / 1000).toInt(), // Unix timestamp as version
            lastUpdated = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            colorMappings = colorMappings,
            materialMappings = materialMappings,
            brandMappings = mapOf("BAMBU" to "Bambu Lab", "BL" to "Bambu Lab"),
            temperatureMappings = getDefaultTemperatureMappings(),
            productCatalog = allProducts
        )
        
        // Save to JSON file
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        
        val outputFile = File("bambu_filament_mappings.json")
        outputFile.writeText(gson.toJson(mappings))
        
        println("💾 Saved ${allProducts.size} products to ${outputFile.name}")
        println("📈 Generated ${colorMappings.size} color mappings")
        println("🔧 Generated ${materialMappings.size} material mappings")
        
    } catch (e: Exception) {
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}

private fun extractMaterialType(productHandle: String): String {
    return when {
        productHandle.contains("pla-basic") -> "PLA_BASIC"
        productHandle.contains("pla-silk") -> "PLA_SILK"
        productHandle.contains("pla-metal") -> "PLA_METAL"
        productHandle.contains("pla-glow") -> "PLA_GLOW"
        productHandle.contains("pla-wood") -> "PLA_WOOD"
        productHandle.contains("pla-marble") -> "PLA_MARBLE"
        productHandle.contains("pla-matte") -> "PLA_MATTE"
        productHandle.contains("abs") -> "ABS"
        productHandle.contains("petg") -> "PETG"
        productHandle.contains("tpu") -> "TPU"
        productHandle.contains("asa") -> "ASA"
        productHandle.contains("pc") -> "PC"
        productHandle.contains("pa") || productHandle.contains("nylon") -> "PA_NYLON"
        productHandle.contains("ppa") -> "PPA"
        productHandle.contains("paht") -> "PAHT"
        productHandle.contains("pet-cf") -> "PET_CF"
        productHandle.contains("support") -> "SUPPORT"
        productHandle.contains("pva") -> "PVA"
        else -> "UNKNOWN"
    }
}

private fun generateInternalCode(productHandle: String): String {
    return when {
        productHandle.contains("pla-basic") -> "GFL00"
        productHandle.contains("abs") -> "GFL01"
        productHandle.contains("pla-silk") -> "GFL02"
        productHandle.contains("tpu") -> "GFL04"
        productHandle.contains("asa") -> "GFL05"
        productHandle.contains("pla-metal") -> "GFL06"
        productHandle.contains("pla-glow") -> "GFL07"
        productHandle.contains("pc") -> "GFL08"
        productHandle.contains("pla-wood") -> "GFL09"
        productHandle.contains("pla-marble") -> "GFL10"
        productHandle.contains("petg") -> "GFG01"
        productHandle.contains("pa") || productHandle.contains("nylon") -> "GFN04"
        productHandle.contains("ppa") -> "GFN05"
        productHandle.contains("paht") -> "GFN06"
        productHandle.contains("pet-cf") -> "GFN07"
        productHandle.contains("support") -> "GFS00"
        productHandle.contains("pva") -> "GFS03"
        else -> "GFL99"
    }
}

private fun generateColorMappings(products: List<ProductEntry>): Map<String, String> {
    val colorMappings = mutableMapOf<String, String>()
    
    // Add default color mappings
    colorMappings.putAll(mapOf(
        "#000000" to "Black",
        "#FFFFFF" to "White",
        "#FF0000" to "Red",
        "#00FF00" to "Green",
        "#0000FF" to "Blue",
        "#FFFF00" to "Yellow",
        "#FF00FF" to "Magenta",
        "#00FFFF" to "Cyan",
        "#FFA500" to "Orange",
        "#800080" to "Purple",
        "#FFC0CB" to "Pink",
        "#A52A2A" to "Brown",
        "#808080" to "Grey",
        "#C0C0C0" to "Silver",
        "#FFD700" to "Gold"
    ))
    
    // Add mappings from scraped products
    products.forEach { product ->
        if (product.colorHex != null && product.colorHex.isNotEmpty()) {
            colorMappings[product.colorHex.uppercase()] = product.colorName
        }
    }
    
    return colorMappings
}

private fun generateMaterialMappings(products: List<ProductEntry>): Map<String, String> {
    val materialTypes = products.map { it.materialType }.distinct()
    val materialMappings = mutableMapOf<String, String>()
    
    materialTypes.forEach { materialType ->
        val displayName = when (materialType) {
            "PLA_BASIC" -> "PLA Basic"
            "PLA_SILK" -> "PLA Silk"
            "PLA_METAL" -> "PLA Metal"
            "PLA_GLOW" -> "PLA Glow in the Dark"
            "PLA_WOOD" -> "PLA Wood-filled"
            "PLA_MARBLE" -> "PLA Marble"
            "PLA_MATTE" -> "PLA Matte"
            "ABS" -> "ABS"
            "PETG" -> "PETG"
            "TPU" -> "TPU (Flexible)"
            "ASA" -> "ASA"
            "PC" -> "Polycarbonate"
            "PA_NYLON" -> "PA (Nylon)"
            "PPA" -> "PPA"
            "PAHT" -> "PAHT-CF"
            "PET_CF" -> "PET-CF"
            "SUPPORT" -> "Support Material"
            "PVA" -> "PVA (Water Soluble)"
            else -> materialType
        }
        materialMappings[materialType] = displayName
    }
    
    return materialMappings
}

private fun getDefaultTemperatureMappings(): Map<String, TemperatureRange> {
    return mapOf(
        "PLA_BASIC" to TemperatureRange(190, 220, 60),
        "PLA_SILK" to TemperatureRange(200, 230, 60),
        "PLA_METAL" to TemperatureRange(200, 230, 60),
        "PLA_GLOW" to TemperatureRange(200, 230, 60),
        "PLA_WOOD" to TemperatureRange(190, 220, 60),
        "PLA_MARBLE" to TemperatureRange(190, 220, 60),
        "ABS" to TemperatureRange(220, 260, 80),
        "PETG" to TemperatureRange(220, 250, 70),
        "TPU" to TemperatureRange(200, 230, 60),
        "ASA" to TemperatureRange(240, 280, 90),
        "PC" to TemperatureRange(260, 310, 100),
        "PA_NYLON" to TemperatureRange(250, 300, 90),
        "PPA" to TemperatureRange(280, 320, 100),
        "PAHT" to TemperatureRange(280, 320, 100),
        "PET_CF" to TemperatureRange(240, 280, 80),
        "SUPPORT" to TemperatureRange(190, 220, 60),
        "PVA" to TemperatureRange(180, 200, 60)
    )
}

