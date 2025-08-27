package com.bscan.model

import java.time.LocalDateTime

/**
 * User-created catalog SKU with full product information
 */
data class UserCatalogSku(
    val skuId: String,
    val name: String,
    val manufacturerId: String,
    val materialType: String,
    val colorName: String,
    val colorHex: String?,
    val filamentWeightGrams: Float?,
    val spoolType: SpoolPackaging?,
    val price: Double?,
    val available: Boolean = true,
    val url: String? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val modifiedAt: LocalDateTime = LocalDateTime.now()
) {
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
     * Check if this SKU matches the given material type and hex color
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
     * Check if this SKU has complete weight information for brand new spool setup
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
}

/**
 * Stock tracking information for a SKU (both user and build-time catalog SKUs)
 */
data class SkuStockInfo(
    val skuId: String,
    val totalQuantity: Int,
    val consumedQuantity: Int,
    val componentInstances: Set<String>,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
) {
    /**
     * Get available quantity (total - consumed)
     */
    fun getAvailableQuantity(): Int = totalQuantity - consumedQuantity
    
    /**
     * Check if stock is low (less than 20% remaining)
     */
    fun isLowStock(): Boolean = totalQuantity > 0 && (getAvailableQuantity().toDouble() / totalQuantity) < 0.2
    
    /**
     * Check if completely out of stock
     */
    fun isOutOfStock(): Boolean = getAvailableQuantity() <= 0
}

/**
 * Combined product information that includes both build-time catalog and user-created SKUs
 */
data class CombinedProductInfo(
    val productEntry: ProductEntry?,
    val userSku: UserCatalogSku?,
    val source: ProductSource,
    val stockInfo: SkuStockInfo?
) {
    /**
     * Get display name for this product
     */
    fun getDisplayName(): String {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.productName ?: "Unknown Product"
            ProductSource.USER_CREATED -> userSku?.name ?: "Unknown SKU"
        }
    }
    
    /**
     * Get color name
     */
    fun getColorName(): String {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.getDisplayColorName() ?: "Unknown"
            ProductSource.USER_CREATED -> userSku?.colorName ?: "Unknown"
        }
    }
    
    /**
     * Get color hex code
     */
    fun getColorHex(): String? {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.colorHex
            ProductSource.USER_CREATED -> userSku?.colorHex
        }
    }
    
    /**
     * Get material type
     */
    fun getMaterialType(): String {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.materialType ?: "Unknown"
            ProductSource.USER_CREATED -> userSku?.materialType ?: "Unknown"
        }
    }
    
    /**
     * Get manufacturer ID
     */
    fun getManufacturerId(): String {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.manufacturer ?: "unknown"
            ProductSource.USER_CREATED -> userSku?.manufacturerId ?: "unknown"
        }
    }
    
    /**
     * Get SKU ID for stock tracking
     */
    fun getSkuId(): String {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> "catalog_${productEntry?.variantId ?: "unknown"}"
            ProductSource.USER_CREATED -> userSku?.skuId ?: "unknown"
        }
    }
    
    /**
     * Check if this product is available
     */
    fun isAvailable(): Boolean {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.available ?: false
            ProductSource.USER_CREATED -> userSku?.available ?: false
        }
    }
    
    /**
     * Get price if available
     */
    fun getPrice(): Double? {
        return when (source) {
            ProductSource.BUILD_TIME_CATALOG -> productEntry?.price
            ProductSource.USER_CREATED -> userSku?.price
        }
    }
}

/**
 * Source of product information
 */
enum class ProductSource {
    BUILD_TIME_CATALOG,  // From assets/catalog_data.json
    USER_CREATED         // User-defined SKUs in SharedPreferences
}

/**
 * Export format for user catalog data
 */
data class UserCatalogExport(
    val version: Int,
    val exportedAt: LocalDateTime,
    val userSkus: List<UserCatalogSku>,
    val stockInfo: List<SkuStockInfo>
)

/**
 * Import merge modes for catalog data
 */
enum class CatalogMergeMode {
    MERGE_WITH_EXISTING,  // Merge with existing, update conflicts
    REPLACE_EXISTING,     // Replace all existing data
    PREVIEW_ONLY          // Preview changes without applying
}

/**
 * Result of catalog import operation
 */
data class CatalogImportResult(
    val addedSkus: Int,
    val updatedSkus: Int,
    val skippedSkus: Int,
    val addedStock: Int,
    val isPreview: Boolean
)