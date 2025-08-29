package com.bscan.data.bambu

/**
 * Populates the normalized data structures from existing product catalog.
 * 
 * Extracts all unique color contexts and creates the complete normalized
 * product database with proper foreign key relationships.
 */
object NormalizedDataPopulator {

    /**
     * Populate all normalized tables from existing data
     */
    fun populateNormalizedData() {
        populateColorContexts()
        populateNormalizedProducts()
    }

    /**
     * Extract unique color contexts and populate MaterialColors table
     */
    private fun populateColorContexts() {
        val products = getAllProductsFromCatalog()
        val uniqueColorContexts = mutableSetOf<Triple<String, String, String>>()

        products.forEach { product ->
            val key = Triple(product.baseMaterial, product.variant, product.colorName)
            if (uniqueColorContexts.add(key)) {
                val materialColor = NormalizedBambuData.MaterialColor(
                    colorCode = generateColorCode(product.colorName),
                    materialName = product.baseMaterial,
                    variantName = product.variant,
                    colorName = product.colorName,
                    colorHex = product.colorHex
                )
                NormalizedBambuData.materialColors.add(materialColor)
            }
        }
    }

    /**
     * Get all products from catalog by accessing available methods
     */
    private fun getAllProductsFromCatalog(): List<BambuProductCatalog.BambuProduct> {
        // Get all unique base materials and variants, then get products for each combination
        val baseMaterials = listOf("PLA", "PETG", "ABS", "ASA", "TPU", "PC", "PA")
        val variants = listOf("Basic", "Silk", "Matte", "Glow", "Wood", "Marble", "Metal", "CF", "90A")
        
        val allProducts = mutableListOf<BambuProductCatalog.BambuProduct>()
        
        baseMaterials.forEach { baseMaterial ->
            allProducts.addAll(BambuProductCatalog.getProductsByBaseMaterial(baseMaterial))
        }
        
        return allProducts.distinctBy { it.sku }
    }

    /**
     * Create normalized products with real string identifiers
     */
    private fun populateNormalizedProducts() {
        val products = getAllProductsFromCatalog()

        products.forEach { product ->
            val colorCode = generateColorCode(product.colorName)
            val seriesCode = "A00" // Default to A00 series - this would need proper RFID mapping
            val shopifyVariantId = BambuShopifyVariants.getVariantIdsBySku(product.sku).firstOrNull()
            
            // Find matching product slug for this material+variant combination
            val productSlug = NormalizedBambuData.productSlugs.find { 
                it.materialName == product.baseMaterial && it.variantName == product.variant 
            }?.slug

            val normalizedProduct = NormalizedBambuData.NormalizedProduct(
                sku = product.sku,
                shopifyVariantId = shopifyVariantId,
                materialName = product.baseMaterial,
                variantName = product.variant,
                seriesCode = seriesCode,
                colorCode = colorCode,
                productSlug = productSlug
            )
            NormalizedBambuData.normalizedProducts.add(normalizedProduct)
        }
    }


    /**
     * Generate color code from color name
     */
    private fun generateColorCode(colorName: String): String {
        return when {
            colorName.contains("White", ignoreCase = true) -> "W0"
            colorName.contains("Black", ignoreCase = true) -> "K0"
            colorName.contains("Red", ignoreCase = true) -> "R0"
            colorName.contains("Blue", ignoreCase = true) -> "B0"
            colorName.contains("Green", ignoreCase = true) -> "G0"
            colorName.contains("Yellow", ignoreCase = true) -> "Y0"
            colorName.contains("Orange", ignoreCase = true) -> "O0"
            colorName.contains("Pink", ignoreCase = true) -> "P0"
            colorName.contains("Purple", ignoreCase = true) -> "V0"
            colorName.contains("Gray", ignoreCase = true) || colorName.contains("Grey", ignoreCase = true) -> "GR0"
            colorName.contains("Brown", ignoreCase = true) -> "BR0"
            colorName.contains("Clear", ignoreCase = true) -> "CL0"
            colorName.contains("Natural", ignoreCase = true) -> "N0"
            colorName.contains("Transparent", ignoreCase = true) -> "T0"
            else -> "X${colorName.take(2).uppercase()}"
        }
    }

    /**
     * Initialize the normalized data on first access
     */
    fun initialize() {
        if (NormalizedBambuData.materialColors.isEmpty()) {
            populateNormalizedData()
        }
    }
}