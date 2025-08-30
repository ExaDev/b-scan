package com.bscan.model

/**
 * Product entry from scraped catalog data containing full SKU information.
 * Each entry represents a specific product variant (e.g., specific color/spool size)
 * with color name preserved in its original SKU context.
 */
data class ProductEntry(
    val variantId: String,
    val productHandle: String,
    val productName: String,
    val colorName: String,           // SKU-specific color name (e.g., "Matte Ivory White (10100) / Refill / 1 kg")
    val colorHex: String?,          // Hex code for this specific color (nullable)
    val colorCode: String,          // Internal color code
    val price: Double,
    val available: Boolean,         // true = product line is active, false = discontinued/retired
    val url: String,
    val manufacturer: String,       // Always "Bambu Lab" for scraped products
    val materialType: String,       // e.g., "PLA_BASIC", "PLA_MATTE", "ABS"
    val internalCode: String,       // Bambu internal code (e.g., "GFL00")
    val lastUpdated: String,        // ISO timestamp when this entry was scraped
    val filamentWeightGrams: Float?, // Expected filament weight in grams (e.g., 500, 750, 1000)
    val spoolType: SpoolPackaging?,  // Packaging type: REFILL or WITH_SPOOL
    val alternativeIds: Set<String> = emptySet() // Additional identifiers for lookup (SKU ID, internal codes, etc.)
) {
    /**
     * Extract a clean color name suitable for display by removing variant information.
     * Preserves material context but removes packaging details.
     */
    fun getDisplayColorName(): String {
        return colorName
            // Remove variant information like "(10100) / Refill / 1 kg"
            .replace(Regex("\\s*\\([^)]+\\)\\s*/.*$"), "")
            .replace(Regex("\\s*/\\s*(Refill|Filament with spool).*$", RegexOption.IGNORE_CASE), "")
            .trim()
    }
    
    /**
     * Get the base material type without variant (e.g., "PLA" from "PLA_MATTE")
     */
    fun getBaseMaterialType(): String {
        return materialType.split("_").firstOrNull() ?: materialType
    }
    
    /**
     * Get the material variant (e.g., "MATTE" from "PLA_MATTE", null for "ABS")
     */
    fun getMaterialVariant(): String? {
        val parts = materialType.split("_")
        return if (parts.size > 1) parts[1] else null
    }
    
    /**
     * Check if this product matches the given material type and hex color
     */
    fun matchesColorAndMaterial(hexColor: String?, materialType: String?): Boolean {
        val hexMatches = colorHex?.equals(hexColor, ignoreCase = true) ?: false
        val materialMatches = if (materialType != null) {
            // Match both exact material type and base material type
            this.materialType.equals(materialType, ignoreCase = true) ||
            getBaseMaterialType().equals(materialType, ignoreCase = true)
        } else true
        
        return hexMatches && materialMatches
    }
    
    /**
     * Check if this product has complete weight information for brand new spool setup
     */
    fun hasCompleteWeightInfo(): Boolean {
        return filamentWeightGrams != null && spoolType != null
    }
    
    /**
     * Get expected total weight for a brand new spool including spool components
     */
    fun getExpectedTotalWeight(cardboardCoreWeight: Float = 33f, emptySpoolWeight: Float = 212f): Float? {
        return filamentWeightGrams?.let { filamentWeight ->
            val spoolComponentWeight = when (spoolType) {
                SpoolPackaging.REFILL -> cardboardCoreWeight
                SpoolPackaging.WITH_SPOOL -> cardboardCoreWeight + emptySpoolWeight
                null -> 0f
            }
            filamentWeight + spoolComponentWeight
        }
    }
    
    /**
     * Check if this product can be found by the given identifier
     * Checks the primary variantId and all alternative identifiers
     */
    fun matchesIdentifier(identifier: String): Boolean {
        return variantId == identifier || alternativeIds.contains(identifier)
    }
}

/**
 * Spool packaging type extracted from product catalog
 */
enum class SpoolPackaging {
    REFILL,      // Just cardboard core (33g)
    WITH_SPOOL   // Cardboard core + reusable spool (245g total)
}