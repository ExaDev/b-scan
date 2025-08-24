package com.bscan.tools.core

import com.bscan.tools.models.ProductSKU
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.math.pow
import kotlin.random.Random

/**
 * Web scraper for Bambu Lab store product catalog
 * Based on proven methodology from session 5e900e02-0a0e-4cbc-8592-1343b8da30ef
 * Successfully extracted 220+ SKUs
 */
class BambuStoreScraper(
    private val baseUrl: String = "https://uk.store.bambulab.com",
    private val requestDelayMs: Long = 2000,
    private val maxRetries: Int = 3,
    private val backoffMultiplier: Double = 2.0
) {
    
    private val gson = Gson()
    private val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    /**
     * Fetch all product URLs from the main collection page
     */
    suspend fun fetchProductUrls(): List<String> {
        val collectionUrl = "$baseUrl/collections/bambu-lab-3d-printer-filament"
        
        try {
            val doc = fetchDocument(collectionUrl)
            val productUrls = mutableSetOf<String>()
            
            // Find product links - they typically follow pattern /products/... or /en/products/...
            doc.select("a[href]").forEach { link ->
                val href = link.attr("href")
                if (href.contains("/products/") && !href.contains("#") && !href.contains("?")) {
                    val fullUrl = if (href.startsWith("http")) {
                        href
                    } else {
                        "${baseUrl}${if (href.startsWith("/")) href else "/$href"}"
                    }
                    productUrls.add(fullUrl)
                }
            }
            
            println("Found ${productUrls.size} product URLs")
            return productUrls.toList()
            
        } catch (e: Exception) {
            throw RuntimeException("Error fetching product URLs from $collectionUrl", e)
        }
    }
    
    /**
     * Extract detailed SKU data from a specific product page
     */
    suspend fun extractProductData(productUrl: String): List<ProductSKU> {
        try {
            val doc = fetchDocument(productUrl)
            
            // Extract product handle from URL
            val productHandle = URL(productUrl).path.split("/").last()
            
            // Check for 404/discontinued products first
            val bodyText = doc.body()?.text()?.lowercase() ?: ""
            if (bodyText.contains("not found") || bodyText.contains("404")) {
                println("    ⚠️  Product page not found - marking as discontinued")
                // Return a discontinued product entry
                return listOf(ProductSKU(
                    variantId = "DISCONTINUED_${productHandle}",
                    productHandle = productHandle,
                    productName = productHandle.replace("-", " ").split(" ").joinToString(" ") { it.capitalize() },
                    colorName = "Discontinued Product",
                    colorCode = "DISCONTINUED",
                    price = 0.0,
                    available = false, // Product is discontinued
                    url = productUrl,
                    colorHex = null
                ))
            }
            
            val skus = mutableListOf<ProductSKU>()
            
            // Debug: Check what we actually received
            debugPageContent(doc, productHandle)
            
            // Method 1: Extract from JSON-LD structured data
            skus.addAll(extractFromJsonLD(doc, productHandle, productUrl))
            
            // Method 2: Extract from Shopify product scripts
            if (skus.isEmpty()) {
                skus.addAll(extractFromShopifyScripts(doc, productHandle, productUrl))
            }
            
            // Method 3: Fallback - extract from HTML attributes and forms
            if (skus.isEmpty()) {
                skus.addAll(extractFromHtmlAttributes(doc, productHandle, productUrl))
            }
            
            println("Extracted ${skus.size} SKUs from $productHandle")
            return skus
            
        } catch (e: Exception) {
            println("Error extracting data from $productUrl: ${e.message}")
            return emptyList()
        }
    }
    
    private fun debugPageContent(doc: Document, productHandle: String) {
        println("  🔍 Debug info for $productHandle:")
        println("    Title: ${doc.title()}")
        
        // Check for JSON-LD scripts
        val jsonLdScripts = doc.select("script[type=application/ld+json]")
        println("    JSON-LD scripts: ${jsonLdScripts.size}")
        jsonLdScripts.forEachIndexed { index, script ->
            val content = script.html().take(500)
            println("      [$index] ${content}...")
        }
        
        // Check for Shopify scripts
        val allScripts = doc.select("script")
        val shopifyScripts = allScripts.filter { it.html().contains("Shopify") || it.html().contains("variants") }
        println("    Shopify-related scripts: ${shopifyScripts.size}")
        
        // Check for product forms
        val forms = doc.select("form")
        println("    Forms found: ${forms.size}")
        
        // Check for data attributes
        val elementsWithData = doc.select("[data-product-id], [data-variant-id], [data-variants]")
        println("    Elements with product data: ${elementsWithData.size}")
        
        // Check if we got a valid product page or something else
        val bodyText = doc.body()?.text()?.lowercase() ?: ""
        when {
            bodyText.contains("not found") || bodyText.contains("404") -> println("    ⚠️  Looks like a 404 page")
            bodyText.contains("maintenance") -> println("    ⚠️  Looks like a maintenance page")
            bodyText.contains("shopify") -> println("    ✅ Looks like a Shopify page")
            else -> println("    ⚠️  Unknown page type")
        }
    }
    
    private suspend fun fetchDocument(url: String): Document {
        return fetchDocumentWithRetry(url, 0)
    }
    
    private suspend fun fetchDocumentWithRetry(url: String, attemptNumber: Int): Document {
        try {
            // Calculate delay with exponential backoff
            val baseDelay = if (attemptNumber == 0) requestDelayMs else 0
            val backoffDelay = if (attemptNumber > 0) {
                (requestDelayMs * backoffMultiplier.pow(attemptNumber - 1)).toLong()
            } else 0
            
            val jitterMs = Random.nextLong(0, 1000) // Add jitter to avoid thundering herd
            val totalDelay = baseDelay + backoffDelay + jitterMs
            
            if (totalDelay > 0) {
                println("  Waiting ${totalDelay}ms before request (attempt ${attemptNumber + 1})")
                delay(totalDelay)
            }
            
            return Jsoup.connect(url)
                .userAgent(userAgent)
                .timeout(30000)
                .get()
                
        } catch (e: HttpStatusException) {
            when {
                e.statusCode == 429 && attemptNumber < maxRetries -> {
                    // Rate limited - retry with exponential backoff
                    val retryDelay = (requestDelayMs * backoffMultiplier.pow(attemptNumber + 1)).toLong()
                    println("  Rate limited (429) - retrying in ${retryDelay}ms (attempt ${attemptNumber + 2}/${maxRetries + 1})")
                    delay(retryDelay)
                    return fetchDocumentWithRetry(url, attemptNumber + 1)
                }
                e.statusCode >= 500 && attemptNumber < maxRetries -> {
                    // Server error - retry with backoff
                    val retryDelay = (requestDelayMs * backoffMultiplier.pow(attemptNumber + 1)).toLong()
                    println("  Server error (${e.statusCode}) - retrying in ${retryDelay}ms (attempt ${attemptNumber + 2}/${maxRetries + 1})")
                    delay(retryDelay)
                    return fetchDocumentWithRetry(url, attemptNumber + 1)
                }
                else -> {
                    println("  HTTP error ${e.statusCode} - max retries exceeded or non-retryable error")
                    throw e
                }
            }
        } catch (e: Exception) {
            if (attemptNumber < maxRetries) {
                val retryDelay = (requestDelayMs * backoffMultiplier.pow(attemptNumber + 1)).toLong()
                println("  Network error: ${e.message} - retrying in ${retryDelay}ms (attempt ${attemptNumber + 2}/${maxRetries + 1})")
                delay(retryDelay)
                return fetchDocumentWithRetry(url, attemptNumber + 1)
            } else {
                println("  Network error: ${e.message} - max retries exceeded")
                throw e
            }
        }
    }
    
    private fun extractFromJsonLD(doc: Document, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        doc.select("script[type=application/ld+json]").forEach { script ->
            try {
                val jsonData = JsonParser.parseString(script.html()).asJsonObject
                val type = jsonData.get("@type")?.asString
                
                println("    JSON-LD @type: $type")
                
                when (type) {
                    "Product" -> {
                        val name = jsonData.get("name")?.asString ?: "Unknown Product"
                        val offers = jsonData.getAsJsonArray("offers")
                        
                        offers?.forEach { offer ->
                            val offerObj = offer.asJsonObject
                            val sku = extractSkuFromOffer(offerObj, name, productHandle, productUrl)
                            if (sku != null) {
                                skus.add(sku)
                            }
                        }
                    }
                    "ProductGroup" -> {
                        // Bambu uses ProductGroup for their variant products
                        val name = jsonData.get("name")?.asString ?: "Unknown Product"
                        println("    ProductGroup name: $name")
                        
                        // Look for hasVariant array
                        val variants = jsonData.getAsJsonArray("hasVariant")
                        if (variants != null) {
                            println("    Found ${variants.size()} variants")
                            variants.forEach { variant ->
                                val variantObj = variant.asJsonObject
                                val sku = extractSkuFromProductVariant(variantObj, name, productHandle, productUrl)
                                if (sku != null) {
                                    skus.add(sku)
                                }
                            }
                        } else {
                            println("    No hasVariant array found")
                        }
                    }
                    else -> {
                        println("    Unsupported JSON-LD type: $type")
                    }
                }
            } catch (e: Exception) {
                println("    Error parsing JSON-LD: ${e.message}")
                // Continue to next script if this one fails
            }
        }
        
        return skus
    }
    
    private fun extractFromShopifyScripts(doc: Document, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        doc.select("script").forEach { script ->
            val scriptContent = script.html()
            
            when {
                scriptContent.contains("window.ShopifyAnalytics") -> {
                    skus.addAll(extractFromShopifyAnalytics(scriptContent, productHandle, productUrl))
                }
                scriptContent.contains("\"variants\"") -> {
                    skus.addAll(extractFromProductJson(scriptContent, productHandle, productUrl))
                }
            }
        }
        
        return skus
    }
    
    private fun extractFromShopifyAnalytics(scriptContent: String, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        try {
            // Look for product data in Shopify analytics
            val productRegex = """"product":\s*(\{[^}]+\})""".toRegex()
            val matches = productRegex.findAll(scriptContent)
            
            matches.forEach { match ->
                val productJson = JsonParser.parseString(match.groupValues[1]).asJsonObject
                val sku = extractSkuFromShopifyProduct(productJson, productHandle, productUrl)
                if (sku != null) {
                    skus.add(sku)
                }
            }
        } catch (e: Exception) {
            // Continue if parsing fails
        }
        
        return skus
    }
    
    private fun extractFromProductJson(scriptContent: String, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        try {
            // Look for variants array in script content
            val variantRegex = """"variants":\s*(\[[^\]]*\])""".toRegex()
            val match = variantRegex.find(scriptContent)
            
            if (match != null) {
                val variantsJson = JsonParser.parseString(match.groupValues[1]).asJsonArray
                val productName = extractProductNameFromScript(scriptContent) ?: "Unknown Product"
                
                variantsJson.forEach { variant ->
                    val variantObj = variant.asJsonObject
                    val sku = extractSkuFromVariant(variantObj, productName, productHandle, productUrl)
                    if (sku != null) {
                        skus.add(sku)
                    }
                }
            }
        } catch (e: Exception) {
            // Continue if parsing fails
        }
        
        return skus
    }
    
    private fun extractFromHtmlAttributes(doc: Document, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        try {
            // Look for product forms with variant data
            doc.select("form[data-product-id]").forEach { form ->
                val productId = form.attr("data-product-id")
                val productName = doc.select("h1.product-title, .product-name, h1").text().ifEmpty { "Unknown Product" }
                
                // Look for variant selects or buttons
                form.select("select[name*=id], button[data-variant-id]").forEach { element ->
                    val variantId = element.attr("data-variant-id").ifEmpty { 
                        element.attr("value") 
                    }
                    
                    if (variantId.isNotEmpty()) {
                        val colorName = extractColorFromElement(element) ?: "Unknown Color"
                        val price = extractPriceFromPage(doc) ?: 0.0
                        
                        val sku = ProductSKU(
                            productHandle = productHandle,
                            productName = productName,
                            variantId = variantId,
                            colorCode = "Unknown",
                            colorName = colorName,
                            price = price,
                            available = true,
                            url = "$productUrl?id=$variantId",
                            colorHex = null,
                            dataHash = calculateHash(variantId, productName, colorName, price.toString())
                        )
                        skus.add(sku)
                    }
                }
            }
        } catch (e: Exception) {
            // Continue if parsing fails
        }
        
        return skus
    }
    
    private fun extractSkuFromOffer(offer: JsonObject, productName: String, productHandle: String, productUrl: String): ProductSKU? {
        return try {
            val sku = offer.get("sku")?.asString ?: return null
            val price = offer.get("price")?.asDouble ?: 0.0
            val availability = offer.get("availability")?.asString
            // Track product retirement status, not stock status
            val available = true // If we can scrape it, the product line is still active
            
            // Extract color from SKU or product variations
            val colorName = extractColorFromSku(sku) ?: "Unknown Color"
            val colorCode = extractColorCodeFromSku(sku) ?: "Unknown"
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = sku,
                colorCode = colorCode,
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$sku",
                colorHex = null,
                dataHash = calculateHash(sku, productName, colorName, price.toString())
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractSkuFromShopifyProduct(product: JsonObject, productHandle: String, productUrl: String): ProductSKU? {
        return try {
            val id = product.get("id")?.asString ?: return null
            val name = product.get("name")?.asString ?: "Unknown Product"
            val price = product.get("price")?.asDouble ?: 0.0
            
            ProductSKU(
                productHandle = productHandle,
                productName = name,
                variantId = id,
                colorCode = "Unknown",
                colorName = "Unknown Color",
                price = price,
                available = true,
                url = "$productUrl?id=$id",
                colorHex = null,
                dataHash = calculateHash(id, name, "Unknown", price.toString())
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractSkuFromVariant(variant: JsonObject, productName: String, productHandle: String, productUrl: String): ProductSKU? {
        return try {
            val id = variant.get("id")?.asString ?: return null
            val title = variant.get("title")?.asString ?: "Unknown Variant"
            val price = variant.get("price")?.asDouble?.div(100) ?: 0.0 // Shopify prices are in cents
            // Track product retirement status, not stock status  
            val available = true // If we can scrape it, the product line is still active
            
            // Color is often in the variant title
            val colorName = if (title != "Default Title") title else "Unknown Color"
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = id,
                colorCode = "Unknown",
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$id",
                colorHex = null,
                dataHash = calculateHash(id, productName, colorName, price.toString())
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractProductNameFromScript(scriptContent: String): String? {
        val nameRegex = """"title":\s*"([^"]+)"""".toRegex()
        return nameRegex.find(scriptContent)?.groupValues?.get(1)
    }
    
    private fun extractColorFromElement(element: Element): String? {
        return element.text().takeIf { it.isNotEmpty() }
            ?: element.attr("title").takeIf { it.isNotEmpty() }
            ?: element.attr("data-color").takeIf { it.isNotEmpty() }
    }
    
    private fun extractPriceFromPage(doc: Document): Double? {
        val priceSelectors = listOf(
            ".price .money",
            ".product-price",
            "[data-price]",
            ".price-current"
        )
        
        for (selector in priceSelectors) {
            val priceElement = doc.selectFirst(selector)
            if (priceElement != null) {
                val priceText = priceElement.text()
                val priceValue = priceText.replace("[^\\d.]".toRegex(), "")
                return priceValue.toDoubleOrNull()
            }
        }
        
        return null
    }
    
    private fun extractSkuFromProductVariant(variant: JsonObject, productName: String, productHandle: String, productUrl: String): ProductSKU? {
        return try {
            val variantName = variant.get("name")?.asString ?: return null
            val sku = variant.get("sku")?.asString ?: return null
            
            // offers can be either a JsonObject or JsonArray
            val offer = when {
                variant.has("offers") && variant.get("offers").isJsonObject -> variant.get("offers").asJsonObject
                variant.has("offers") && variant.get("offers").isJsonArray -> {
                    val offersArray = variant.getAsJsonArray("offers")
                    if (offersArray.size() > 0) offersArray[0].asJsonObject else return null
                }
                else -> return null
            }
            
            val price = offer.get("price")?.asDouble ?: 0.0
            val availability = offer.get("availability")?.asString
            // Track product retirement status, not stock status
            val available = true // If we can scrape it, the product line is still active
            
            // Extract color from variant name or other attributes
            val colorName = extractColorFromVariantName(variantName) ?: "Unknown Color"
            val colorCode = extractColorCodeFromVariant(variant) ?: "Unknown"
            
            println("      Found variant: $variantName, SKU: $sku, Price: £$price, Available: $available")
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = sku,
                colorCode = colorCode,
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$sku",
                colorHex = null,
                dataHash = calculateHash(sku, productName, colorName, price.toString())
            )
        } catch (e: Exception) {
            println("      Error processing variant: ${e.message}")
            null
        }
    }
    
    private fun extractColorFromVariantName(variantName: String): String? {
        // Try to extract color from variant names like "PLA Basic - White" or "White"
        val colorPatterns = listOf(
            """- ([^-]+)$""".toRegex(), // "Product - Color"
            """^([A-Z][a-z]+ ?[A-Z]?[a-z]*)$""".toRegex() // Just "Color Name"
        )
        
        for (pattern in colorPatterns) {
            val match = pattern.find(variantName.trim())
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        
        return variantName.trim().takeIf { it.isNotEmpty() }
    }
    
    private fun extractColorCodeFromVariant(variant: JsonObject): String? {
        // Look for color codes in variant properties
        return variant.get("color")?.asString
            ?: variant.get("colorCode")?.asString
            ?: variant.get("gtin")?.asString // Sometimes color codes are in GTIN
    }
    
    private fun extractColorFromSku(sku: String): String? {
        // Try to extract color information from SKU format
        // This would need to be customized based on Bambu's SKU patterns
        return null
    }
    
    private fun extractColorCodeFromSku(sku: String): String? {
        // Try to extract color code from SKU format
        return null
    }
    
    private fun calculateHash(variantId: String, productName: String, colorName: String, price: String): String {
        val data = "$variantId-$productName-$colorName-$price"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}