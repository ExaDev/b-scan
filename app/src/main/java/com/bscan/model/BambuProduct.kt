package com.bscan.model

/**
 * Represents a Bambu Lab filament product with purchase links
 */
data class BambuProduct(
    val productLine: String,        // "PLA Basic", "ABS", etc.
    val colorName: String,          // "Jade White", "Black", etc.
    val internalCode: String,       // "GFL00", "GFL01", etc.
    val retailSku: String?,         // "10100", "40101", etc.
    val colorHex: String,           // "#FFFFFF", "#000000", etc.
    val spoolUrl: String?,          // URL with spool
    val refillUrl: String?,         // Refill-only URL
    val mass: String = "1kg"        // "0.5kg", "0.75kg", "1kg"
)

/**
 * Purchase link availability for UI display
 */
data class PurchaseLinks(
    val spoolAvailable: Boolean,
    val refillAvailable: Boolean,
    val spoolUrl: String?,
    val refillUrl: String?
) {
    companion object {
        fun from(product: BambuProduct): PurchaseLinks {
            return PurchaseLinks(
                spoolAvailable = product.spoolUrl != null,
                refillAvailable = product.refillUrl != null,
                spoolUrl = product.spoolUrl,
                refillUrl = product.refillUrl
            )
        }
    }
}