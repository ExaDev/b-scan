package com.bscan.tools.models

import java.time.LocalDateTime

/**
 * Core data models for the catalog updater
 * Based on the successful extraction methodology from session 5e900e02
 */

data class ProductSKU(
    val productHandle: String,          // e.g., "pla-basic-filament"  
    val productName: String,            // e.g., "PLA Basic"
    val variantId: String,              // Shopify variant ID (e.g., "40206189035580")
    val colorCode: String,              // Internal color code (e.g., "10100")
    val colorName: String,              // Human readable color (e.g., "Jade White")
    val price: Double,                  // Current price in GBP
    val available: Boolean,             // Stock availability
    val url: String,                    // Direct product URL
    val colorHex: String? = null,       // Hex color code if determinable
    val firstSeen: LocalDateTime = LocalDateTime.now(),
    val lastSeen: LocalDateTime = LocalDateTime.now(),
    val dataHash: String = ""           // SHA-256 hash for change detection
)

data class DiscontinuedProduct(
    val name: String,
    val colorName: String?,
    val variantId: String?,
    val internalCode: String?,
    val discontinuedDate: String?,
    val reason: String?,
    val confirmed: Boolean = false      // Whether discontinuation is confirmed
)

data class ProductCatalog(
    val metadata: CatalogMetadata,
    val products: List<ProductSKU>,
    val discontinuedProducts: List<DiscontinuedProduct> = emptyList()
)

data class CatalogMetadata(
    val generatedAt: LocalDateTime,
    val totalActiveProducts: Int,
    val totalDiscontinuedProducts: Int,
    val sourceUrl: String,
    val methodology: String,
    val version: String = "2.0",
    val generatedBy: String = "Bambu Lab Catalog Updater"
)

/**
 * Change detection models
 */
data class ChangeRecord(
    val timestamp: LocalDateTime,
    val changeType: ChangeType,
    val productHandle: String,
    val variantId: String,
    val oldValue: String?,
    val newValue: String?,
    val details: Map<String, String> = emptyMap()
)

enum class ChangeType(val displayName: String) {
    NEW("New Product"),
    DELETED("Product Deleted"),
    PRICE_CHANGE("Price Change"), 
    AVAILABILITY_CHANGE("Availability Change"),
    COLOR_CHANGE("Color Change"),
    NAME_CHANGE("Name Change")
}