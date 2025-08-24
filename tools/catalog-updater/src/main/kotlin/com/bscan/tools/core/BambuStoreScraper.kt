package com.bscan.tools.core

import com.bscan.tools.models.ProductSKU
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.jsoup.Jsoup
import org.jsoup.HttpStatusException
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.ByteArrayInputStream
import java.io.IOException
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
            
            // Extract hex codes from the page first
            println("  🎨 Extracting hex codes from page...")
            var hexCodes = extractHexCodesFromPage(doc)
            
            // Filter out generic/UI hex codes, keep only filament-specific ones
            val filamentHexCodes = hexCodes.filterKeys { colorName ->
                // Keep only colors that look like actual filament colors
                !colorName.contains("Turn Ideas Into Reality", ignoreCase = true) &&
                !colorName.contains("View all", ignoreCase = true) &&
                !colorName.contains("INCL. VAT", ignoreCase = true) &&
                !colorName.contains("rolls from", ignoreCase = true) &&
                !colorName.contains("Quick add", ignoreCase = true) &&
                !colorName.contains("Product Features", ignoreCase = true) &&
                !colorName.contains("Discover More", ignoreCase = true) &&
                !colorName.contains("Filament", ignoreCase = true) &&
                !colorName.contains("guide", ignoreCase = true) &&
                !colorName.contains("here", ignoreCase = true) &&
                !colorName.contains("The color and texture", ignoreCase = true) &&
                !colorName.equals("Hex Code", ignoreCase = true) && // This is likely a link, not a color
                colorName.length <= 30 && // Reasonable color name length
                !colorName.contains("mm") && // Exclude technical specs
                !colorName.contains("nozzle", ignoreCase = true)
            }
            
            println("  🎯 Found ${filamentHexCodes.size} filament-specific hex codes after filtering")
            hexCodes = filamentHexCodes
            
            // Always check for PDFs, especially if we have few/no filament hex codes
            println("  📄 Looking for hex code PDFs...")
            val pdfLinks = findHexCodePdfLinks(doc)
            for (pdfUrl in pdfLinks) {
                val pdfHexCodes = extractHexCodesFromPdf(pdfUrl)
                hexCodes = hexCodes + pdfHexCodes
                if (pdfHexCodes.isNotEmpty()) {
                    println("  ✅ Added ${pdfHexCodes.size} hex codes from PDF")
                }
            }
            
            println("  💡 Found ${hexCodes.size} hex codes total")
            
            // Method 1: Extract from JSON-LD structured data
            skus.addAll(extractFromJsonLD(doc, productHandle, productUrl, hexCodes))
            
            // Method 2: Extract from Shopify product scripts
            if (skus.isEmpty()) {
                skus.addAll(extractFromShopifyScripts(doc, productHandle, productUrl, hexCodes))
            }
            
            // Method 3: Fallback - extract from HTML attributes and forms
            if (skus.isEmpty()) {
                skus.addAll(extractFromHtmlAttributes(doc, productHandle, productUrl, hexCodes))
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
    
    private fun extractFromJsonLD(doc: Document, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): List<ProductSKU> {
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
                            val sku = extractSkuFromOffer(offerObj, name, productHandle, productUrl, hexCodes)
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
                                val sku = extractSkuFromProductVariant(variantObj, name, productHandle, productUrl, hexCodes)
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
    
    private fun extractFromShopifyScripts(doc: Document, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        doc.select("script").forEach { script ->
            val scriptContent = script.html()
            
            when {
                scriptContent.contains("window.ShopifyAnalytics") -> {
                    skus.addAll(extractFromShopifyAnalytics(scriptContent, productHandle, productUrl, hexCodes))
                }
                scriptContent.contains("\"variants\"") -> {
                    skus.addAll(extractFromProductJson(scriptContent, productHandle, productUrl, hexCodes))
                }
            }
        }
        
        return skus
    }
    
    private fun extractFromShopifyAnalytics(scriptContent: String, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): List<ProductSKU> {
        val skus = mutableListOf<ProductSKU>()
        
        try {
            // Look for product data in Shopify analytics
            val productRegex = """"product":\s*(\{[^}]+\})""".toRegex()
            val matches = productRegex.findAll(scriptContent)
            
            matches.forEach { match ->
                val productJson = JsonParser.parseString(match.groupValues[1]).asJsonObject
                val sku = extractSkuFromShopifyProduct(productJson, productHandle, productUrl, hexCodes)
                if (sku != null) {
                    skus.add(sku)
                }
            }
        } catch (e: Exception) {
            // Continue if parsing fails
        }
        
        return skus
    }
    
    private fun extractFromProductJson(scriptContent: String, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): List<ProductSKU> {
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
                    val sku = extractSkuFromVariant(variantObj, productName, productHandle, productUrl, hexCodes)
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
    
    private fun extractFromHtmlAttributes(doc: Document, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): List<ProductSKU> {
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
                        val colorHex = findHexCodeForColor(colorName, hexCodes)
                        
                        val sku = ProductSKU(
                            productHandle = productHandle,
                            productName = productName,
                            variantId = variantId,
                            colorCode = "Unknown",
                            colorName = colorName,
                            price = price,
                            available = true,
                            url = "$productUrl?id=$variantId",
                            colorHex = colorHex,
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
    
    private fun extractSkuFromOffer(offer: JsonObject, productName: String, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): ProductSKU? {
        return try {
            val sku = offer.get("sku")?.asString ?: return null
            val price = offer.get("price")?.asDouble ?: 0.0
            val availability = offer.get("availability")?.asString
            // Track product retirement status, not stock status
            val available = true // If we can scrape it, the product line is still active
            
            // Extract color from SKU or product variations
            val colorName = extractColorFromSku(sku) ?: "Unknown Color"
            val colorCode = extractColorCodeFromSku(sku) ?: "Unknown"
            val colorHex = findHexCodeForColor(colorName, hexCodes)
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = sku,
                colorCode = colorCode,
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$sku",
                colorHex = colorHex,
                dataHash = calculateHash(sku, productName, colorName, price.toString())
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractSkuFromShopifyProduct(product: JsonObject, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): ProductSKU? {
        return try {
            val id = product.get("id")?.asString ?: return null
            val name = product.get("name")?.asString ?: "Unknown Product"
            val price = product.get("price")?.asDouble ?: 0.0
            val colorName = "Unknown Color"
            val colorHex = findHexCodeForColor(colorName, hexCodes)
            
            ProductSKU(
                productHandle = productHandle,
                productName = name,
                variantId = id,
                colorCode = "Unknown",
                colorName = colorName,
                price = price,
                available = true,
                url = "$productUrl?id=$id",
                colorHex = colorHex,
                dataHash = calculateHash(id, name, "Unknown", price.toString())
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun extractSkuFromVariant(variant: JsonObject, productName: String, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): ProductSKU? {
        return try {
            val id = variant.get("id")?.asString ?: return null
            val title = variant.get("title")?.asString ?: "Unknown Variant"
            val price = variant.get("price")?.asDouble?.div(100) ?: 0.0 // Shopify prices are in cents
            // Track product retirement status, not stock status  
            val available = true // If we can scrape it, the product line is still active
            
            // Color is often in the variant title
            val colorName = if (title != "Default Title") title else "Unknown Color"
            val colorHex = findHexCodeForColor(colorName, hexCodes)
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = id,
                colorCode = "Unknown",
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$id",
                colorHex = colorHex,
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
    
    private fun extractSkuFromProductVariant(variant: JsonObject, productName: String, productHandle: String, productUrl: String, hexCodes: Map<String, String> = emptyMap()): ProductSKU? {
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
            val colorHex = findHexCodeForColor(colorName, hexCodes)
            
            println("      Found variant: $variantName, SKU: $sku, Price: £$price, Available: $available, Hex: $colorHex")
            
            ProductSKU(
                productHandle = productHandle,
                productName = productName,
                variantId = sku,
                colorCode = colorCode,
                colorName = colorName,
                price = price,
                available = available,
                url = "$productUrl?id=$sku",
                colorHex = colorHex,
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
    
    /**
     * Extract hex color codes directly from the product page HTML
     */
    private fun extractHexCodesFromPage(doc: Document): Map<String, String> {
        val hexCodes = mutableMapOf<String, String>()
        
        // Method 1: Look for hex codes in text content
        val pageText = doc.text()
        val hexPattern = """([A-Za-z\s]+)\s*[:\-]?\s*(#[0-9A-Fa-f]{6})""".toRegex()
        val matches = hexPattern.findAll(pageText)
        
        for (match in matches) {
            val colorName = match.groupValues[1].trim()
            val hexCode = match.groupValues[2].uppercase()
            
            if (colorName.isNotEmpty() && hexCode.matches(Regex("#[0-9A-F]{6}"))) {
                hexCodes[colorName] = hexCode
                println("      Found hex on page: $colorName -> $hexCode")
            }
        }
        
        // Method 2: Look for CSS background-color or color styles
        doc.select("[style*='background-color'], [style*='color']").forEach { element ->
            val style = element.attr("style")
            val colorText = element.text().trim()
            
            // Extract hex from CSS
            val cssHexPattern = """(#[0-9A-Fa-f]{6})""".toRegex()
            val cssMatch = cssHexPattern.find(style)
            
            if (cssMatch != null && colorText.isNotEmpty()) {
                val hexCode = cssMatch.groupValues[1].uppercase()
                hexCodes[colorText] = hexCode
                println("      Found hex in CSS: $colorText -> $hexCode")
            }
        }
        
        // Method 3: Look for data attributes with hex codes
        doc.select("[data-color], [data-hex], [data-color-code]").forEach { element ->
            val colorName = element.text().trim()
            val hexCode = element.attr("data-color")
                .ifEmpty { element.attr("data-hex") }
                .ifEmpty { element.attr("data-color-code") }
            
            if (hexCode.isNotEmpty() && hexCode.matches(Regex("#?[0-9A-Fa-f]{6}"))) {
                val normalizedHex = if (hexCode.startsWith("#")) hexCode.uppercase() else "#${hexCode.uppercase()}"
                if (colorName.isNotEmpty()) {
                    hexCodes[colorName] = normalizedHex
                    println("      Found hex in data attr: $colorName -> $normalizedHex")
                }
            }
        }
        
        // Method 4: Look for color swatch elements
        doc.select(".color-swatch, .colour-swatch, .color-option, .variant-color").forEach { swatch ->
            val colorName = swatch.text().trim()
                .ifEmpty { swatch.attr("title") }
                .ifEmpty { swatch.attr("alt") }
            
            val style = swatch.attr("style")
            val hexPattern = """(#[0-9A-Fa-f]{6})""".toRegex()
            val match = hexPattern.find(style)
            
            if (match != null && colorName.isNotEmpty()) {
                val hexCode = match.groupValues[1].uppercase()
                hexCodes[colorName] = hexCode
                println("      Found hex in color swatch: $colorName -> $hexCode")
            }
        }
        
        // Method 5: Look in script tags for color data
        doc.select("script").forEach { script ->
            val scriptContent = script.html()
            if (scriptContent.contains("color") || scriptContent.contains("hex")) {
                val hexPattern = """"([^"]*(?:color|Color|colour|Colour)[^"]*)"\s*:\s*"(#[0-9A-Fa-f]{6})"""".toRegex()
                val matches = hexPattern.findAll(scriptContent)
                
                for (match in matches) {
                    val colorName = match.groupValues[1]
                    val hexCode = match.groupValues[2].uppercase()
                    hexCodes[colorName] = hexCode
                    println("      Found hex in script: $colorName -> $hexCode")
                }
            }
        }
        
        return hexCodes
    }
    
    /**
     * Find hex code PDF links on the product page
     */
    private fun findHexCodePdfLinks(doc: Document): List<String> {
        val pdfLinks = mutableListOf<String>()
        
        // Method 1: Look for links with "Hex Code" text specifically
        doc.select("a[href]").forEach { link ->
            val href = link.attr("href")
            val linkText = link.text().trim()
            
            println("    Checking link: '$linkText' -> '$href'")
            
            // High priority: "Hex Code" links (even if not .pdf extension)
            if (linkText.equals("Hex Code", ignoreCase = true)) {
                val fullUrl = if (href.startsWith("http")) {
                    href
                } else {
                    "${baseUrl}${if (href.startsWith("/")) href else "/$href"}"
                }
                pdfLinks.add(fullUrl)
                println("    ✅ Found 'Hex Code' link: $linkText -> $fullUrl")
                return@forEach
            }
            
            // Standard PDF detection
            if (href.endsWith(".pdf", ignoreCase = true) && 
                (linkText.contains("hex", ignoreCase = true) || 
                 linkText.contains("color", ignoreCase = true) || 
                 linkText.contains("colour", ignoreCase = true) ||
                 linkText.contains("code", ignoreCase = true))) {
                
                val fullUrl = if (href.startsWith("http")) {
                    href
                } else {
                    "${baseUrl}${if (href.startsWith("/")) href else "/$href"}"
                }
                pdfLinks.add(fullUrl)
                println("    ✅ Found hex code PDF: $linkText -> $fullUrl")
            }
        }
        
        // Also look for data attributes that might point to hex code PDFs
        doc.select("[data-pdf-url], [data-hex-pdf], [data-color-guide]").forEach { element ->
            val pdfUrl = element.attr("data-pdf-url")
                .ifEmpty { element.attr("data-hex-pdf") }
                .ifEmpty { element.attr("data-color-guide") }
            
            if (pdfUrl.isNotEmpty()) {
                val fullUrl = if (pdfUrl.startsWith("http")) {
                    pdfUrl
                } else {
                    "${baseUrl}${if (pdfUrl.startsWith("/")) pdfUrl else "/$pdfUrl"}"
                }
                pdfLinks.add(fullUrl)
                println("    Found hex code PDF from data attribute: $fullUrl")
            }
        }
        
        return pdfLinks.distinct()
    }
    
    /**
     * Download and parse PDF to extract hex color codes
     */
    private suspend fun extractHexCodesFromPdf(pdfUrl: String): Map<String, String> {
        val hexCodes = mutableMapOf<String, String>()
        
        try {
            println("    Downloading hex code PDF: $pdfUrl")
            delay(requestDelayMs) // Respect rate limiting
            
            val pdfBytes = Jsoup.connect(pdfUrl)
                .userAgent(userAgent)
                .timeout(30000)
                .ignoreContentType(true)
                .execute()
                .bodyAsBytes()
            
            val document = Loader.loadPDF(pdfBytes)
            try {
                val pdfStripper = PDFTextStripper()
                val text = pdfStripper.getText(document)
                
                // Extract hex codes from PDF text
                hexCodes.putAll(parseHexCodesFromText(text))
                println("    Extracted ${hexCodes.size} hex codes from PDF")
            } finally {
                document.close()
            }
            
        } catch (e: Exception) {
            println("    Error extracting hex codes from PDF $pdfUrl: ${e.message}")
        }
        
        return hexCodes
    }
    
    /**
     * Parse hex color codes from PDF text content
     */
    private fun parseHexCodesFromText(text: String): Map<String, String> {
        val hexCodes = mutableMapOf<String, String>()
        val lines = text.split('\n')
        
        println("      📄 Parsing PDF text for hex codes...")
        println("      📄 Full PDF text preview (first 2000 chars):")
        println("      ${text.take(2000)}...")
        println("      📄 Lines containing '#':")
        
        // Parse the PDF in pairs: color name line followed by hex line
        var i = 0
        var currentColorName: String? = null
        
        while (i < lines.size) {
            val line = lines[i].trim()
            
            // Check if this line is a hex code
            val bambuHexPattern = """^Hex:(#[0-9A-Fa-f]{6})\s*$""".toRegex()
            val hexMatch = bambuHexPattern.find(line)
            
            if (hexMatch != null) {
                val hexCode = hexMatch.groupValues[1].uppercase()
                
                // If we have a color name from the previous line, pair them
                if (currentColorName != null && isValidColorName(currentColorName)) {
                    hexCodes[currentColorName] = hexCode
                    println("        ✅ Paired: '$currentColorName' -> $hexCode")
                } else {
                    // Fallback: store with placeholder
                    val placeholderName = "Color_$hexCode"
                    hexCodes[placeholderName] = hexCode
                    println("        ⚠️ Unpaired hex: '$placeholderName' -> $hexCode")
                }
                currentColorName = null // Reset for next pair
                
            } else if (line.isNotEmpty() && !line.startsWith("Hex:") && !line.contains("Bambu Lab") && 
                      !line.contains("Filament") && !line.contains("Table") && !line.contains("PLA")) {
                // This might be a color name line
                if (isValidColorName(line)) {
                    currentColorName = line
                    println("        📝 Found potential color name: '$line'")
                }
            }
            
            i++
        }
        
        // Also try the old pattern-matching as fallback for other PDF formats
        for (line in lines.filter { it.contains("#") }) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            
            // Pattern 1: "Color Name #FFFFFF" or "Color Name: #FFFFFF"
            val hexPattern = """^([A-Za-z][A-Za-z\s]{2,30}?)\s*[:\-]?\s*(#[0-9A-Fa-f]{6})\s*$""".toRegex()
            hexPattern.find(trimmedLine)?.let { match ->
                val colorName = match.groupValues[1].trim()
                val hexCode = match.groupValues[2].uppercase()
                
                if (isValidColorName(colorName) && !hexCodes.containsKey(colorName)) {
                    hexCodes[colorName] = hexCode
                    println("        ✅ Pattern 1: '$colorName' -> $hexCode")
                }
            }
        }
        
        return hexCodes
    }
    
    /**
     * Validate if a string looks like a reasonable color name
     */
    private fun isValidColorName(colorName: String): Boolean {
        val trimmed = colorName.trim()
        return trimmed.length in 3..50 && // Reasonable length
               trimmed.matches(Regex("[A-Za-z][A-Za-z\\s]*")) && // Only letters and spaces, starts with letter
               !trimmed.matches(Regex(".*\\b(PDF|Page|Document|Color|Hex|Code|Table|List)\\b.*", RegexOption.IGNORE_CASE)) // Exclude document metadata
    }
    
    /**
     * Find matching hex code for a specific SKU color name from extracted hex codes
     */
    private fun findHexCodeForColor(colorName: String, hexCodes: Map<String, String>): String? {
        if (colorName.isBlank() || hexCodes.isEmpty()) {
            return null
        }
        
        println("        🔍 Looking for hex code for: '$colorName'")
        
        // Method 1: Direct exact match
        hexCodes[colorName]?.let { 
            println("        ✅ Direct match: $colorName -> $it")
            return it 
        }
        
        // Method 2: Case-insensitive exact match
        hexCodes.entries.find { (key, _) -> 
            key.equals(colorName, ignoreCase = true) 
        }?.let { (key, value) ->
            println("        ✅ Case-insensitive match: '$colorName' -> '$key' -> $value")
            return value
        }
        
        // Method 3: Extract base color name and match
        // Handle cases like "Matte Jade White (10100)" -> "Jade White" and "Jade White (10100)" -> "Jade White"
        val baseColorName = colorName
            .replace(Regex("""\s*\([^)]+\)\s*.*$"""), "") // Remove (10100) / Refill / 1 kg
            .replace(Regex("""^Matte\s+"""), "") // Remove "Matte " prefix
            .trim()
            
        if (baseColorName != colorName) {
            println("        🔄 Trying base color name: '$baseColorName'")
            
            // Try direct match on base color
            hexCodes[baseColorName]?.let { 
                println("        ✅ Base color direct match: $baseColorName -> $it")
                return it 
            }
            
            // Try case-insensitive match on base color
            hexCodes.entries.find { (key, _) -> 
                key.equals(baseColorName, ignoreCase = true) 
            }?.let { (key, value) ->
                println("        ✅ Base color case-insensitive match: '$baseColorName' -> '$key' -> $value")
                return value
            }
        }
        
        // Method 4: Partial matching as last resort
        // Only match if the color name is reasonably long to avoid false positives
        if (baseColorName.length >= 4) {
            hexCodes.entries.find { (key, _) -> 
                // Check if the key contains our color name (or vice versa)
                // But be more strict to avoid false matches
                val keyNormalized = key.lowercase().trim()
                val colorNormalized = baseColorName.lowercase().trim()
                
                // Must be a substantial match (at least 70% of shorter string)
                val minLength = minOf(keyNormalized.length, colorNormalized.length)
                val matchThreshold = (minLength * 0.7).toInt()
                
                (keyNormalized.contains(colorNormalized) && colorNormalized.length >= matchThreshold) ||
                (colorNormalized.contains(keyNormalized) && keyNormalized.length >= matchThreshold)
            }?.let { (key, value) ->
                println("        ⚠️ Partial match: '$baseColorName' -> '$key' -> $value")
                return value
            }
        }
        
        println("        ❌ No hex code found for: '$colorName'")
        return null
    }
    
    private fun calculateHash(variantId: String, productName: String, colorName: String, price: String): String {
        val data = "$variantId-$productName-$colorName-$price"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}