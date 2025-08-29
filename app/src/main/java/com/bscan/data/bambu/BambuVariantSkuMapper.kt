package com.bscan.data.bambu

import com.bscan.model.SkuInfo

/**
 * Mapper for Bambu Lab RFID codes to filament SKUs.
 * 
 * Maps full RFID codes (e.g., "GFA00:A00-K0") to 5-digit filament codes/SKUs (e.g., "10101")
 * using the normalized data structure as the single source of truth.
 * 
 * Structure: MaterialID -> VariantID -> SkuInfo
 * RFID Key Format: "MaterialID:VariantID" (e.g., "GFA00:A00-K0")
 */
object BambuVariantSkuMapper {
    
    /**
     * Simple data class for color and SKU information without material type redundancy
     */
    data class ColorSku(
        val sku: String,
        val colorName: String,
        val materialOverride: String? = null
    )
    
    /**
     * Get SKU information for a given RFID key using normalized data
     */
    fun getSkuByRfidKey(rfidKey: String): SkuInfo? {
        // Parse RFID key format: "MaterialID:SeriesCode-ColorCode"
        val parts = rfidKey.split(":")
        if (parts.size != 2) return null
        
        val materialId = parts[0]
        val variantParts = parts[1].split("-")
        if (variantParts.size != 2) return null
        
        val seriesCode = variantParts[0]
        val colorCode = variantParts[1]
        
        // Find matching normalized product
        val normalizedProduct = NormalizedBambuData.getAllNormalizedProducts().find { product ->
            product.seriesCode == seriesCode && product.colorCode == colorCode
        } ?: return null
        
        // Get complete product view for additional information
        val completeView = NormalizedBambuData.getCompleteProductView(normalizedProduct.sku) ?: return null
        
        return SkuInfo(
            sku = normalizedProduct.sku,
            colorName = completeView.color.colorName,
            materialType = completeView.materialType
        )
    }
    
    /**
     * Get all known RFID keys by reconstructing from normalized data
     */
    fun getAllKnownRfidKeys(): Set<String> {
        // Map material+variant combinations to RFID material IDs (from BambuMaterialIdMapper)
        val materialIdMap = mapOf(
            ("PLA" to "Basic") to "GFA00",
            ("PLA" to "Matte") to "GFA01", 
            ("PLA" to "Metal") to "GFA02",
            ("PLA" to "Silk") to "GFA05",
            ("PLA" to "Marble") to "GFA07",
            ("PLA" to "Glow") to "GFA12",
            ("PETG" to "Basic") to "GFG00",
            ("ABS" to "Basic") to "GFL01",
            ("ASA" to "Basic") to "GFL02",
            ("TPU" to "90A") to "GFL04",
            ("PC" to "Basic") to "GFC00",
            ("PA" to "Basic") to "GFN04",
            ("PVA" to "Basic") to "GFS00",
            ("SUPPORT" to "Basic") to "GFS01"
        )
        
        return NormalizedBambuData.getAllNormalizedProducts().mapNotNull { product ->
            val materialId = materialIdMap[product.materialName to product.variantName]
            if (materialId != null) {
                // Reconstruct RFID key format: "MaterialID:SeriesCode-ColorCode"
                "$materialId:${product.seriesCode}-${product.colorCode}"
            } else {
                null // Skip unknown combinations
            }
        }.toSet()
    }
    
    /**
     * Check if an RFID key is known
     */
    fun hasRfidKey(rfidKey: String): Boolean {
        return getSkuByRfidKey(rfidKey) != null
    }
    
    /**
     * Get all products for a specific base material
     */
    fun getProductsByBaseMaterial(baseMaterial: String): List<SkuInfo> {
        return NormalizedBambuData.getAllNormalizedProducts()
            .filter { it.materialName.equals(baseMaterial, ignoreCase = true) }
            .mapNotNull { product -> 
                val completeView = NormalizedBambuData.getCompleteProductView(product.sku)
                completeView?.let {
                    SkuInfo(
                        sku = product.sku,
                        colorName = it.color.colorName,
                        materialType = it.materialType
                    )
                }
            }
    }
}