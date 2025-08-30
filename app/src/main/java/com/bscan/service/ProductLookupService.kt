package com.bscan.service

import com.bscan.data.bambu.BambuVariantSkuMapper
import com.bscan.data.bambu.BambuProductCatalog
import com.bscan.data.bambu.NormalizedBambuData
import com.bscan.data.bambu.rfid.BambuMaterialIdMapper
import com.bscan.model.BambuProduct
import com.bscan.model.ProductEntry
import com.bscan.model.SpoolPackaging

/**
 * Service to provide product lookup functionality after removal of BambuProductDatabase.
 * Uses real mapper data to provide product information for the BambuFormatInterpreter.
 */
object ProductLookupService {

    /**
     * Get all available products as ProductEntry objects
     */
    fun getAllProducts(): List<ProductEntry> {
        val rfidKeys = BambuVariantSkuMapper.getAllKnownRfidKeys()
        
        return rfidKeys.mapNotNull { rfidKey ->
            val skuInfo = BambuVariantSkuMapper.getSkuByRfidKey(rfidKey)
            if (skuInfo != null) {
                // Extract material and color codes from RFID key (e.g., "GFA00:A00-K0")
                val keyParts = rfidKey.split(":")
                if (keyParts.size == 2) {
                    val materialCode = keyParts[0] // e.g., "GFA00"
                    val variantId = keyParts[1]    // e.g., "A00-K0"
                    val variantParts = variantId.split("-")
                    if (variantParts.size == 2) {
                        val colorCode = variantParts[1] // e.g., "K0"
                        
                        // Get enhanced color information from product catalog
                        val product = BambuProductCatalog.getProductBySku(skuInfo.sku)
                        val materialDisplayName = product?.baseMaterial ?: BambuMaterialIdMapper.getDisplayName(materialCode)
                        val colorName = product?.colorName ?: skuInfo.colorName
                        
                        
                        ProductEntry(
                        variantId = skuInfo.sku,
                        productHandle = (product?.materialType ?: skuInfo.materialType).lowercase().replace(" ", "-").replace("_", "-"),
                        productName = materialDisplayName,
                        colorName = colorName,
                        colorHex = product?.colorHex, // Use catalog hex or null for RFID Block 5
                        colorCode = colorCode,
                        price = 0.0,
                        available = true,
                        url = "https://bambulab.com/en/filament/${(product?.materialType ?: skuInfo.materialType).lowercase().replace(" ", "-").replace("_", "-")}",
                        manufacturer = "Bambu Lab",
                        materialType = product?.materialType ?: skuInfo.materialType,
                        internalCode = materialCode,
                        lastUpdated = "2025-08-27T00:00:00Z",
                        filamentWeightGrams = 1000f, // Default 1kg
                        spoolType = SpoolPackaging.WITH_SPOOL
                        )
                    } else null
                } else null
            } else null
        }
    }

    /**
     * Find products by hex color and material type
     */
    fun findProducts(hex: String? = null, materialType: String? = null): List<ProductEntry> {
        val allProducts = getAllProducts()
        
        return allProducts.filter { product ->
            val hexMatches = hex?.let { product.colorHex?.equals(it, ignoreCase = true) } ?: true
            val materialMatches = materialType?.let { 
                product.materialType.equals(it, ignoreCase = true) ||
                product.productName.equals(it, ignoreCase = true)
            } ?: true
            hexMatches && materialMatches
        }
    }

    /**
     * Convert ProductEntry to BambuProduct
     */
    fun convertToBambuProduct(product: ProductEntry): BambuProduct {
        return BambuProduct(
            productLine = product.productName,
            colorName = product.colorName,
            internalCode = product.internalCode ?: "GFA00",
            retailSku = product.variantId,
            colorHex = product.colorHex ?: "#808080",
            spoolUrl = product.url,
            refillUrl = product.url,
            mass = when (product.filamentWeightGrams?.toInt()) {
                500 -> "0.5kg"
                750 -> "0.75kg" 
                1000 -> "1kg"
                else -> "1kg"
            }
        )
    }
    
}