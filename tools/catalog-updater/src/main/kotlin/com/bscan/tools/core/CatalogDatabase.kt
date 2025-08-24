package com.bscan.tools.core

import com.bscan.tools.models.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Database layer for catalog data persistence
 * Handles loading/saving catalog data in multiple formats
 */
class CatalogDatabase(
    private val dataDirectory: File
) {
    
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ ->
            com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        })
        .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ ->
            LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        })
        .create()
    
    init {
        if (!dataDirectory.exists()) {
            dataDirectory.mkdirs()
        }
    }
    
    /**
     * Save catalog data in specified format
     */
    fun saveCatalog(catalog: ProductCatalog, format: String = "json") {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        
        when (format.lowercase()) {
            "json" -> saveAsJson(catalog, timestamp)
            "csv" -> saveAsCsv(catalog, timestamp)
            "txt" -> saveAsTxt(catalog, timestamp)
            else -> throw IllegalArgumentException("Unsupported format: $format")
        }
        
        // Always save a summary regardless of format
        saveSummary(catalog, timestamp)
    }
    
    /**
     * Load the most recent catalog data
     */
    fun loadCatalog(): ProductCatalog? {
        // Look for the most recent catalog.json file
        val catalogFile = File(dataDirectory, "catalog.json")
        if (!catalogFile.exists()) {
            // Try to find the most recent timestamped file
            val jsonFiles = dataDirectory.listFiles { _, name -> 
                name.startsWith("catalog_") && name.endsWith(".json") 
            }?.sortedByDescending { it.lastModified() }
            
            if (jsonFiles.isNullOrEmpty()) {
                return null
            }
            
            return loadFromJson(jsonFiles.first())
        }
        
        return loadFromJson(catalogFile)
    }
    
    /**
     * Save change records
     */
    fun saveChanges(changes: List<ChangeRecord>) {
        if (changes.isEmpty()) return
        
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val changesFile = File(dataDirectory, "changes_$timestamp.json")
        
        val changesData = mapOf(
            "timestamp" to LocalDateTime.now(),
            "totalChanges" to changes.size,
            "changes" to changes
        )
        
        changesFile.writeText(gson.toJson(changesData))
        
        // Also save human-readable changes
        saveChangesAsText(changes, timestamp)
    }
    
    /**
     * Load all change records
     */
    fun loadChanges(): List<ChangeRecord> {
        val allChanges = mutableListOf<ChangeRecord>()
        
        dataDirectory.listFiles { _, name -> 
            name.startsWith("changes_") && name.endsWith(".json") 
        }?.forEach { file ->
            try {
                val json = file.readText()
                val data = gson.fromJson(json, Map::class.java)
                val changes = data["changes"] as? List<*>
                changes?.forEach { change ->
                    if (change is Map<*, *>) {
                        // Convert map back to ChangeRecord
                        // This is a simplified approach - you might want more robust deserialization
                        val timestamp = LocalDateTime.parse(change["timestamp"].toString())
                        val changeType = ChangeType.valueOf(change["changeType"].toString())
                        val productHandle = change["productHandle"].toString()
                        val variantId = change["variantId"].toString()
                        val oldValue = change["oldValue"]?.toString()
                        val newValue = change["newValue"]?.toString()
                        
                        allChanges.add(ChangeRecord(timestamp, changeType, productHandle, variantId, oldValue, newValue))
                    }
                }
            } catch (e: Exception) {
                println("Error loading changes from ${file.name}: ${e.message}")
            }
        }
        
        return allChanges.sortedBy { it.timestamp }
    }
    
    private fun saveAsJson(catalog: ProductCatalog, timestamp: String) {
        val file = File(dataDirectory, "catalog.json")
        val timestampedFile = File(dataDirectory, "catalog_$timestamp.json")
        
        val jsonData = gson.toJson(catalog)
        
        file.writeText(jsonData)
        timestampedFile.writeText(jsonData)
    }
    
    private fun saveAsCsv(catalog: ProductCatalog, timestamp: String) {
        val file = File(dataDirectory, "catalog.csv")
        val timestampedFile = File(dataDirectory, "catalog_$timestamp.csv")
        
        val csvContent = buildString {
            // Header
            appendLine("Product Handle,Product Name,Variant ID,Color Code,Color Name,Price,Available,URL,Color Hex,First Seen,Last Seen,Data Hash")
            
            // Data rows
            catalog.products.forEach { product ->
                appendLine("\"${product.productHandle}\",\"${product.productName}\",\"${product.variantId}\",\"${product.colorCode}\",\"${product.colorName}\",${product.price},${product.available},\"${product.url}\",\"${product.colorHex ?: ""}\",\"${product.firstSeen}\",\"${product.lastSeen}\",\"${product.dataHash}\"")
            }
        }
        
        file.writeText(csvContent)
        timestampedFile.writeText(csvContent)
    }
    
    private fun saveAsTxt(catalog: ProductCatalog, timestamp: String) {
        val file = File(dataDirectory, "catalog.txt")
        val timestampedFile = File(dataDirectory, "catalog_$timestamp.txt")
        
        val txtContent = buildString {
            appendLine("# Bambu Lab Product Catalog")
            appendLine("# Generated: ${catalog.metadata.generatedAt}")
            appendLine("# Total Products: ${catalog.metadata.totalActiveProducts}")
            appendLine("# Total Discontinued: ${catalog.metadata.totalDiscontinuedProducts}")
            appendLine("# Source: ${catalog.metadata.sourceUrl}")
            appendLine("# Version: ${catalog.metadata.version}")
            appendLine()
            
            appendLine("## Active Products")
            appendLine("=".repeat(50))
            
            val groupedProducts = catalog.products.groupBy { it.productHandle }
            
            for ((handle, products) in groupedProducts.entries.sortedBy { it.key }) {
                val productName = products.first().productName
                appendLine("\n### $productName ($handle)")
                
                products.sortedBy { it.colorName }.forEach { product ->
                    appendLine("- ${product.colorName} (${product.variantId}): £${product.price} - ${if (product.available) "Available" else "Out of Stock"}")
                }
            }
            
            if (catalog.discontinuedProducts.isNotEmpty()) {
                appendLine("\n## Discontinued Products")
                appendLine("=".repeat(50))
                
                catalog.discontinuedProducts.forEach { product ->
                    appendLine("- ${product.name} - ${product.colorName ?: "Unknown Color"} (${product.variantId ?: "No Variant ID"})")
                }
            }
        }
        
        file.writeText(txtContent)
        timestampedFile.writeText(txtContent)
    }
    
    private fun saveSummary(catalog: ProductCatalog, timestamp: String) {
        val summaryFile = File(dataDirectory, "summary.txt")
        
        val summary = buildString {
            appendLine("Bambu Lab Catalog Summary")
            appendLine("Generated: ${catalog.metadata.generatedAt}")
            appendLine("=".repeat(40))
            appendLine()
            
            appendLine("Statistics:")
            appendLine("- Active Products: ${catalog.metadata.totalActiveProducts}")
            appendLine("- Discontinued Products: ${catalog.metadata.totalDiscontinuedProducts}")
            appendLine("- Total SKUs: ${catalog.products.size}")
            appendLine("- Source: ${catalog.metadata.sourceUrl}")
            appendLine("- Methodology: ${catalog.metadata.methodology}")
            appendLine("- Version: ${catalog.metadata.version}")
            appendLine()
            
            // Product breakdown by type
            val productTypes = catalog.products.groupBy { it.productHandle }
            appendLine("Product Lines:")
            productTypes.entries.sortedBy { it.key }.forEach { (handle, products) ->
                appendLine("- ${products.first().productName}: ${products.size} variants")
            }
            
            appendLine()
            appendLine("Files Generated:")
            appendLine("- catalog.json (main database)")
            appendLine("- catalog_$timestamp.json (timestamped backup)")
            appendLine("- summary.txt (this file)")
        }
        
        summaryFile.writeText(summary)
    }
    
    private fun saveChangesAsText(changes: List<ChangeRecord>, timestamp: String) {
        val changesFile = File(dataDirectory, "changes.txt")
        val timestampedFile = File(dataDirectory, "changes_$timestamp.txt")
        
        val content = buildString {
            appendLine("Product Catalog Changes")
            appendLine("Generated: ${LocalDateTime.now()}")
            appendLine("=".repeat(40))
            appendLine()
            
            val groupedChanges = changes.groupBy { it.changeType }
            
            groupedChanges.forEach { (changeType, changeList) ->
                appendLine("## ${changeType.displayName} (${changeList.size})")
                appendLine()
                
                changeList.sortedBy { it.timestamp }.forEach { change ->
                    when (changeType) {
                        ChangeType.NEW -> {
                            appendLine("+ NEW: ${change.productHandle} (${change.variantId})")
                            appendLine("  Added: ${change.timestamp}")
                        }
                        ChangeType.DELETED -> {
                            appendLine("- DELETED: ${change.productHandle} (${change.variantId})")
                            appendLine("  Removed: ${change.timestamp}")
                        }
                        ChangeType.PRICE_CHANGE -> {
                            appendLine("~ PRICE: ${change.productHandle} (${change.variantId})")
                            appendLine("  ${change.oldValue} → ${change.newValue}")
                            appendLine("  Changed: ${change.timestamp}")
                        }
                        ChangeType.AVAILABILITY_CHANGE -> {
                            appendLine("~ AVAILABILITY: ${change.productHandle} (${change.variantId})")
                            appendLine("  ${change.oldValue} → ${change.newValue}")
                            appendLine("  Changed: ${change.timestamp}")
                        }
                        else -> {
                            appendLine("~ ${changeType.displayName}: ${change.productHandle} (${change.variantId})")
                            appendLine("  ${change.oldValue} → ${change.newValue}")
                            appendLine("  Changed: ${change.timestamp}")
                        }
                    }
                    appendLine()
                }
                appendLine()
            }
        }
        
        changesFile.writeText(content)
        timestampedFile.writeText(content)
    }
    
    private fun loadFromJson(file: File): ProductCatalog? {
        return try {
            val json = file.readText()
            gson.fromJson(json, ProductCatalog::class.java)
        } catch (e: Exception) {
            println("Error loading catalog from ${file.name}: ${e.message}")
            null
        }
    }
}