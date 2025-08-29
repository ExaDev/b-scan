package com.bscan.data.bambu

/**
 * Bambu Lab product catalog facade providing backward compatibility.
 * 
 * This facade delegates to NormalizedBambuData for all actual data storage
 * while maintaining the same API as the original denormalized catalog.
 */
object BambuProductCatalog {

    /**
     * Product information for a Bambu Lab filament SKU
     */
    data class BambuProduct(
        val sku: String,
        val colorName: String,
        val colorHex: String? = null,
        val baseMaterial: String,
        val variant: String
    ) {
        /**
         * Get material type string compatible with existing code
         */
        val materialType: String
            get() = if (variant.equals("Basic", ignoreCase = true)) {
                baseMaterial.uppercase()
            } else {
                "${baseMaterial}_${variant}".uppercase()
            }
    }

    /**
     * Get product by SKU using normalized data
     */
    fun getProductBySku(sku: String): BambuProduct? {
        val completeView = NormalizedBambuData.getCompleteProductView(sku) ?: return null
        
        return BambuProduct(
            sku = completeView.sku,
            colorName = completeView.color.colorName,
            colorHex = completeView.color.colorHex,
            baseMaterial = completeView.material.name,
            variant = completeView.variant.name
        )
    }

    /**
     * Get all products by base material using normalized data
     */
    fun getProductsByBaseMaterial(baseMaterial: String): List<BambuProduct> {
        return NormalizedBambuData.getAllNormalizedProducts()
            .filter { it.materialName.equals(baseMaterial, ignoreCase = true) }
            .mapNotNull { getProductBySku(it.sku) }
    }

    /**
     * Get all available products
     */
    fun getAllProducts(): List<BambuProduct> {
        return NormalizedBambuData.getAllNormalizedProducts()
            .mapNotNull { getProductBySku(it.sku) }
    }

    /**
     * Check if SKU exists
     */
    fun hasProduct(sku: String): Boolean {
        return NormalizedBambuData.getNormalizedProductBySku(sku) != null
    }
}