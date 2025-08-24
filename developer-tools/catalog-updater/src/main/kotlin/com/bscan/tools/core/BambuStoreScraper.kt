package com.bscan.tools.core

import com.bscan.tools.models.ProductSKU
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL
import java.security.MessageDigest
import java.time.LocalDateTime

/**
 * Web scraper for Bambu Lab store product catalog
 * Based on proven methodology from session 5e900e02-0a0e-4cbc-8592-1343b8da30ef
 * Successfully extracted 220+ SKUs
 */
class BambuStoreScraper(
    private val baseUrl: String = "https://uk.store.bambulab.com",
    private val requestDelayMs: Long = 1000
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
            val skus = mutableListOf<ProductSKU>()
            
            // Extract product handle from URL
            val productHandle = URL(productUrl).path.split("/").last()
            
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
    
    private suspend fun fetchDocument(url: String): Document {
        delay(requestDelayMs)
        return Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(30000)
            .get()
    }
    
    private fun extractFromJsonLD(doc: Document, productHandle: String, productUrl: String): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        doc.select("script[type=application/ld+json]").forEach { script ->
            try {
                val jsonData = JsonParser.parseString(script.html()).asJsonObject
                
                if (jsonData.get("@type")?.asString == "Product") {
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
            } catch (e: Exception) {
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
            val available = availability?.contains("InStock") == true
            
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
            val available = variant.get("available")?.asBoolean ?: true
            
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