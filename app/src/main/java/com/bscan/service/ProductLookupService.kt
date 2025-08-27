package com.bscan.service

import com.bscan.data.bambu.BambuVariantSkuMapper
import com.bscan.data.bambu.rfid.BambuMaterialIdMapper
import com.bscan.data.bambu.rfid.BambuColorCodeMapper
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
        val variants = BambuVariantSkuMapper.getAllKnownVariants()
        
        return variants.mapNotNull { variantId ->
            val skuInfo = BambuVariantSkuMapper.getSkuByVariantId(variantId)
            if (skuInfo != null) {
                // Extract material and color codes from variant ID (e.g., "A00-K0")
                val parts = variantId.split("-")
                if (parts.size == 2) {
                    val materialCode = "GF${parts[0]}" // Convert A00 to GFA00
                    val colorCode = parts[1]
                    
                    val materialDisplayName = BambuMaterialIdMapper.getDisplayName(materialCode)
                    val colorInfo = BambuColorCodeMapper.getColorInfo(colorCode)
                    
                    ProductEntry(
                        variantId = skuInfo.sku,
                        productHandle = skuInfo.materialType.lowercase().replace(" ", "-"),
                        productName = materialDisplayName,
                        colorName = colorInfo?.displayName ?: "Unknown",
                        colorHex = "#808080", // Default - real hex comes from RFID tag Block 5
                        colorCode = colorCode,
                        price = 0.0,
                        available = true,
                        url = "https://bambulab.com/en/filament/${skuInfo.materialType.lowercase().replace(" ", "-")}",
                        manufacturer = "Bambu Lab",
                        materialType = skuInfo.materialType,
                        internalCode = materialCode,
                        lastUpdated = "2025-08-27T00:00:00Z",
                        filamentWeightGrams = 1000f, // Default 1kg
                        spoolType = SpoolPackaging.WITH_SPOOL
                    )
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
     * Convert ProductEntry to BambuProduct for legacy compatibility
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