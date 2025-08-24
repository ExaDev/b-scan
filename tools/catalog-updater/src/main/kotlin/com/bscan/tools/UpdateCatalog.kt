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
    val available: Boolean, // true = product line is active, false = discontinued/retired
    val url: String,
    val manufacturer: String,
    val materialType: String,
    val internalCode: String,
    val lastUpdated: String
)

fun main() = runBlocking {
    println("🔍 Updating Bambu Lab product catalog...")
    println("ℹ️  Using 3-second delays with exponential backoff for rate limiting")
    println()
    
    // Load existing data if available
    val outputFile = File("bambu_filament_mappings.json")
    val existingMappings = loadExistingMappings(outputFile)
    val allProducts = existingMappings?.productCatalog?.toMutableList() ?: mutableListOf<ProductEntry>()
    
    val scraper = BambuStoreScraper(
        requestDelayMs = 3000, // Start with 3-second delays
        maxRetries = 5,        // Allow more retries
        backoffMultiplier = 2.5 // More aggressive backoff
    )
    
    try {
        // Fetch product URLs
        println("📥 Fetching product URLs...")
        val urls = scraper.fetchProductUrls()
        println("Found ${urls.size} product URLs")
        
        // Extract product data (continue with existing list)
        var processed = 0
        
        println("📊 Extracting product data...")
        println("⚠️  This may take 5-10 minutes due to rate limiting...")
        println()
        
        val overallStartTime = System.currentTimeMillis()
        
        for ((index, url) in urls.withIndex()) {
            val progress = "${index + 1}/${urls.size}"
            val productName = url.substringAfterLast("/")
            
            
            println("[$progress] Processing: $productName")
            
            val itemStartTime = System.currentTimeMillis()
            val products = scraper.extractProductData(url)
            val duration = System.currentTimeMillis() - itemStartTime
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            
            products.forEach { product ->
                // Remove existing entries for this variant to avoid duplicates
                allProducts.removeAll { it.variantId == product.variantId }
                
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
                        internalCode = generateInternalCode(product.productHandle),
                        lastUpdated = timestamp
                    )
                )
            }
            
            // Save incrementally after each product
            if (products.isNotEmpty()) {
                saveIncrementalUpdate(allProducts, outputFile)
            }
            processed++
            
            val durationSeconds = duration / 1000.0
            if (products.isEmpty()) {
                println("  ❌ No products extracted (${durationSeconds}s)")
            } else {
                println("  ✅ Found ${products.size} SKUs (${durationSeconds}s)")
            }
            
            // Show overall progress every 5 items
            if (processed % 5 == 0 || processed == urls.size) {
                val totalSKUs = allProducts.size
                val elapsedSeconds = (System.currentTimeMillis() - overallStartTime) / 1000.0
                val avgTimePerItem = if (processed > 0) elapsedSeconds / processed else 0.0
                val remaining = urls.size - processed
                val etaMinutes = (remaining * avgTimePerItem / 60).toInt()
                println("  📊 Progress: $processed/${urls.size} products, $totalSKUs total SKUs")
                if (remaining > 0) {
                    println("  ⏱️  ETA: ~${etaMinutes} minutes remaining")
                }
                println()
            }
        }
        
        println("✅ Extraction complete: ${allProducts.size} SKUs found")
        
        // Generate material mappings from scraped data  
        val materialMappings = generateMaterialMappings(allProducts)
        
        // Create final FilamentMappings structure
        val mappings = FilamentMappings(
            version = (System.currentTimeMillis() / 1000).toInt(), // Unix timestamp as version
            materialMappings = materialMappings,
            brandMappings = mapOf("BAMBU" to "Bambu Lab", "BL" to "Bambu Lab"),
            temperatureMappings = getDefaultTemperatureMappings(),
            productCatalog = allProducts
        )
        
        // Final save to JSON file
        saveIncrementalUpdate(allProducts.toList(), outputFile, mappings)
        
        println("💾 Saved ${allProducts.size} products to ${outputFile.name}")
        println("🔧 Generated ${materialMappings.size} material mappings")
        println("🎨 Color information preserved within ${allProducts.size} SKU entries")
        
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

private fun loadExistingMappings(outputFile: File): FilamentMappings? {
    if (!outputFile.exists()) return null
    
    return try {
        val gson = GsonBuilder().create()
        val json = outputFile.readText()
        gson.fromJson(json, FilamentMappings::class.java)
    } catch (e: Exception) {
        println("⚠️  Could not load existing mappings: ${e.message}")
        null
    }
}

private fun saveIncrementalUpdate(products: List<ProductEntry>, outputFile: File, finalMappings: FilamentMappings? = null) {
    val materialMappings = generateMaterialMappings(products)
    
    val mappings = finalMappings ?: FilamentMappings(
        version = (System.currentTimeMillis() / 1000).toInt(),
        materialMappings = materialMappings,
        brandMappings = mapOf("BAMBU" to "Bambu Lab", "BL" to "Bambu Lab"),
        temperatureMappings = getDefaultTemperatureMappings(),
        productCatalog = products
    )
    
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    outputFile.writeText(gson.toJson(mappings))
}

