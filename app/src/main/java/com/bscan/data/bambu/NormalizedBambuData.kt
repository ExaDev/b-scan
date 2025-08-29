package com.bscan.data.bambu

import com.bscan.model.MaterialCatalogProperties
import com.bscan.model.PurchaseLinks
import com.bscan.model.TemperatureProfile

/**
 * Fully normalized data structures for Bambu Lab product catalog.
 * 
 * Handles contextual color naming where the same color code or hex value
 * may have different names depending on material type and variant.
 * 
 * Eliminates data redundancy while preserving all contextual information
 * from the original Bambu product catalog.
 */
object NormalizedBambuData {

    /**
     * Base material definitions with core properties
     */
    data class BaseMaterial(
        val name: String,           // Primary key: "PLA", "PETG", "ABS", etc.
        val displayName: String,
        val properties: MaterialCatalogProperties,
        val temperatureProfileName: String
    )

    /**
     * Material variants with specific modifications
     */
    data class MaterialVariant(
        val name: String,           // Primary key: "Basic", "Silk", "CF", etc.
        val displayName: String,
        val propertyModifications: Map<String, Any>? = null
    )

    /**
     * Series codes used in RFID system
     */
    data class Series(
        val code: String, // A00, B00, C00, etc. - this IS the primary key
        val displayName: String = code
    )

    /**
     * Product slugs for store URLs
     */
    data class ProductSlug(
        val slug: String,         // Primary key: "pla-basic", "pla-wood", etc.
        val materialName: String, // FK to BaseMaterial: "PLA", "PETG", etc.
        val variantName: String,  // FK to MaterialVariant: "Basic", "Silk", etc.
        val displayName: String   // "PLA Basic", "PLA Wood", etc.
    )

    /**
     * Store regions for URL generation
     */
    data class StoreRegion(
        val regionCode: String,   // Primary key: "uk", "us", "eu"
        val baseUrl: String,      // "https://uk.store.bambulab.com"
        val displayName: String   // "United Kingdom", "United States", etc.
    )

    /**
     * Material-contextual color definitions
     * Handles the fact that color names are specific to material+variant combinations
     */
    data class MaterialColor(
        val colorCode: String,      // Primary key: "K0", "R0", "W0", etc.
        val materialName: String,   // FK to BaseMaterial: "PLA", "PETG", etc.
        val variantName: String,    // FK to MaterialVariant: "Basic", "Silk", etc.
        val colorName: String,      // Material-specific name: "Black", "TPU 90A / Black", "Clear Black"
        val colorHex: String?       // Can be null or material-specific hex
    )

    /**
     * Normalized product table with foreign keys only
     */
    data class NormalizedProduct(
        val sku: String,            // Primary key: "10101", "40200", etc.
        val shopifyVariantId: String?,
        val materialName: String,   // FK to BaseMaterial: "PLA", "PETG", etc.
        val variantName: String,    // FK to MaterialVariant: "Basic", "Silk", etc.
        val seriesCode: String,     // FK to Series: "A00", "B00", etc.
        val colorCode: String,      // FK to MaterialColor: "K0", "R0", etc.
        val productSlug: String?    // FK to ProductSlug: "pla-basic", "pla-wood", etc. (nullable)
    )

    /**
     * Base materials with properties
     */
    val baseMaterials = listOf(
        BaseMaterial(
            name = "PLA",
            displayName = "PLA",
            properties = MaterialCatalogProperties(
                density = 1.24f,
                shrinkage = 0.3f,
                biodegradable = true,
                foodSafe = true,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.THERMOPLASTIC
            ),
            temperatureProfileName = "pla_standard"
        ),
        BaseMaterial(
            name = "PETG",
            displayName = "PETG",
            properties = MaterialCatalogProperties(
                density = 1.27f,
                shrinkage = 0.2f,
                biodegradable = false,
                foodSafe = true,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.THERMOPLASTIC
            ),
            temperatureProfileName = "petg_standard"
        ),
        BaseMaterial(
            name = "ABS",
            displayName = "ABS",
            properties = MaterialCatalogProperties(
                density = 1.04f,
                shrinkage = 0.8f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.THERMOPLASTIC
            ),
            temperatureProfileName = "abs_standard"
        ),
        BaseMaterial(
            name = "ASA",
            displayName = "ASA",
            properties = MaterialCatalogProperties(
                density = 1.05f,
                shrinkage = 0.7f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.THERMOPLASTIC
            ),
            temperatureProfileName = "asa_standard"
        ),
        BaseMaterial(
            name = "TPU",
            displayName = "TPU",
            properties = MaterialCatalogProperties(
                density = 1.20f,
                shrinkage = 1.0f,
                biodegradable = false,
                foodSafe = false,
                flexible = true,
                support = false,
                category = com.bscan.model.MaterialCategory.FLEXIBLE
            ),
            temperatureProfileName = "tpu_standard"
        ),
        BaseMaterial(
            name = "PC",
            displayName = "PC",
            properties = MaterialCatalogProperties(
                density = 1.20f,
                shrinkage = 0.6f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.ENGINEERING
            ),
            temperatureProfileName = "pc_standard"
        ),
        BaseMaterial(
            name = "PA",
            displayName = "PA (Nylon)",
            properties = MaterialCatalogProperties(
                density = 1.14f,
                shrinkage = 1.5f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = com.bscan.model.MaterialCategory.ENGINEERING
            ),
            temperatureProfileName = "nylon_standard"
        ),
        BaseMaterial(
            name = "PVA",
            displayName = "PVA",
            properties = MaterialCatalogProperties(
                density = 1.23f,
                shrinkage = 0.5f,
                biodegradable = true,
                foodSafe = false,
                flexible = false,
                support = true,
                category = com.bscan.model.MaterialCategory.SUPPORT
            ),
            temperatureProfileName = "pva_standard"
        ),
        BaseMaterial(
            name = "SUPPORT",
            displayName = "Support Material",
            properties = MaterialCatalogProperties(
                density = 1.23f,
                shrinkage = 0.5f,
                biodegradable = true,
                foodSafe = false,
                flexible = false,
                support = true,
                category = com.bscan.model.MaterialCategory.SUPPORT
            ),
            temperatureProfileName = "support_standard"
        )
    )

    /**
     * Temperature profiles from existing system
     */
    val temperatureProfiles = mapOf(
        "pla_standard" to TemperatureProfile(minNozzle = 190, maxNozzle = 230, bed = 60, enclosure = null),
        "abs_standard" to TemperatureProfile(minNozzle = 220, maxNozzle = 280, bed = 90, enclosure = 60),
        "asa_standard" to TemperatureProfile(minNozzle = 220, maxNozzle = 280, bed = 90, enclosure = 60),
        "petg_standard" to TemperatureProfile(minNozzle = 220, maxNozzle = 260, bed = 70, enclosure = null),
        "pc_standard" to TemperatureProfile(minNozzle = 260, maxNozzle = 310, bed = 100, enclosure = 80),
        "tpu_standard" to TemperatureProfile(minNozzle = 210, maxNozzle = 250, bed = 50, enclosure = null),
        "nylon_standard" to TemperatureProfile(minNozzle = 250, maxNozzle = 300, bed = 90, enclosure = 80),
        "pva_standard" to TemperatureProfile(minNozzle = 190, maxNozzle = 220, bed = 60, enclosure = null),
        "support_standard" to TemperatureProfile(minNozzle = 190, maxNozzle = 230, bed = 60, enclosure = null),
        "generic_standard" to TemperatureProfile(minNozzle = 200, maxNozzle = 250, bed = 60, enclosure = null)
    )

    /**
     * Material variants with modifications to base properties
     */
    val materialVariants = listOf(
        MaterialVariant(
            name = "Basic",
            displayName = "Basic"
        ),
        MaterialVariant(
            name = "Silk",
            displayName = "Silk"
        ),
        MaterialVariant(
            name = "Matte",
            displayName = "Matte"
        ),
        MaterialVariant(
            name = "Glow",
            displayName = "Glow"
        ),
        MaterialVariant(
            name = "Wood",
            displayName = "Wood"
        ),
        MaterialVariant(
            name = "Marble",
            displayName = "Marble"
        ),
        MaterialVariant(
            name = "Metal",
            displayName = "Metal"
        ),
        MaterialVariant(
            name = "CF",
            displayName = "Carbon Fiber",
            propertyModifications = mapOf(
                "density" to 1.30f,
                "shrinkage" to 0.1f,
                "category" to com.bscan.model.MaterialCategory.COMPOSITE
            )
        ),
        MaterialVariant(
            name = "90A",
            displayName = "90A Shore Hardness"
        )
    )

    /**
     * Series codes from RFID system
     */
    val series = listOf(
        Series(code = "A00"),
        Series(code = "B00"),
        Series(code = "C00"),
        Series(code = "G50"),
        Series(code = "S04")
    )

    /**
     * Store regions available for URLs
     */
    val storeRegions = listOf(
        StoreRegion(
            regionCode = "uk",
            baseUrl = "https://uk.store.bambulab.com",
            displayName = "United Kingdom"
        ),
        StoreRegion(
            regionCode = "us",
            baseUrl = "https://us.store.bambulab.com",
            displayName = "United States"
        ),
        StoreRegion(
            regionCode = "eu",
            baseUrl = "https://eu.store.bambulab.com", 
            displayName = "Europe"
        )
    )

    /**
     * Product slugs for material+variant combinations
     */
    val productSlugs = listOf(
        // PLA variants
        ProductSlug("pla-basic", "PLA", "Basic", "PLA Basic"),
        ProductSlug("pla-silk", "PLA", "Silk", "PLA Silk"),
        ProductSlug("pla-matte", "PLA", "Matte", "PLA Matte"),
        ProductSlug("pla-glow", "PLA", "Glow", "PLA Glow"),
        ProductSlug("pla-wood", "PLA", "Wood", "PLA Wood"),
        ProductSlug("pla-marble", "PLA", "Marble", "PLA Marble"),
        ProductSlug("pla-metal", "PLA", "Metal", "PLA Metal"),
        
        // PETG variants
        ProductSlug("petg-basic", "PETG", "Basic", "PETG Basic"),
        ProductSlug("pet-cf", "PETG", "CF", "PET-CF"),
        
        // ABS variants
        ProductSlug("abs-basic", "ABS", "Basic", "ABS Basic"),
        
        // ASA variants
        ProductSlug("asa-basic", "ASA", "Basic", "ASA Basic"),
        
        // TPU variants
        ProductSlug("tpu-basic", "TPU", "Basic", "TPU Basic"),
        ProductSlug("tpu-90a", "TPU", "90A", "TPU 90A"),
        
        // PC variants
        ProductSlug("pc-basic", "PC", "Basic", "PC Basic"),
        
        // PA variants
        ProductSlug("pa-nylon", "PA", "Basic", "PA Nylon"),
        
        // PVA variants
        ProductSlug("pva-basic", "PVA", "Basic", "PVA Basic"),
        
        // Support variants
        ProductSlug("support-material", "SUPPORT", "Basic", "Support Material")
    )

    /**
     * Lookup functions
     */
    fun getBaseMaterialByName(name: String): BaseMaterial? = baseMaterials.find { it.name == name }
    fun getAllBaseMaterials(): List<BaseMaterial> = baseMaterials
    
    fun getMaterialVariantByName(name: String): MaterialVariant? = materialVariants.find { it.name == name }
    fun getAllMaterialVariants(): List<MaterialVariant> = materialVariants
    
    fun getSeriesByCode(code: String): Series? = series.find { it.code == code }
    fun getAllSeries(): List<Series> = series
    
    fun getProductSlugBySlug(slug: String): ProductSlug? = productSlugs.find { it.slug == slug }
    fun getProductSlugByMaterialAndVariant(materialName: String, variantName: String): ProductSlug? = 
        productSlugs.find { it.materialName == materialName && it.variantName == variantName }
    fun getAllProductSlugs(): List<ProductSlug> = productSlugs
    
    fun getStoreRegionByCode(code: String): StoreRegion? = storeRegions.find { it.regionCode == code }
    fun getAllStoreRegions(): List<StoreRegion> = storeRegions

    /**
     * Material-specific color definitions
     * Generated from existing BambuProductCatalog to preserve contextual color naming
     * This will be populated separately to avoid circular dependencies
     */
    val materialColors = mutableListOf<MaterialColor>()

    /**
     * Normalized products table (will be populated separately)
     */
    val normalizedProducts = mutableListOf<NormalizedProduct>()

    /**
     * Get material colors for a specific material and variant combination
     */
    fun getMaterialColorsByMaterial(materialName: String, variantName: String): List<MaterialColor> {
        NormalizedDataPopulator.initialize()
        return materialColors.filter { it.materialName == materialName && it.variantName == variantName }
    }
    
    fun getAllMaterialColors(): List<MaterialColor> {
        NormalizedDataPopulator.initialize()
        return materialColors
    }

    fun getNormalizedProductBySku(sku: String): NormalizedProduct? {
        NormalizedDataPopulator.initialize()
        return normalizedProducts.find { it.sku == sku }
    }
    
    fun getAllNormalizedProducts(): List<NormalizedProduct> {
        NormalizedDataPopulator.initialize()
        return normalizedProducts
    }

    /**
     * Get a complete view of a product with all related data
     */
    data class CompleteProductView(
        val sku: String,
        val shopifyVariantId: String?,
        val material: BaseMaterial,
        val variant: MaterialVariant,
        val series: Series,
        val color: MaterialColor,
        val productSlug: ProductSlug?,
        val temperatureProfile: TemperatureProfile,
        val productUrl: String?
    ) {
        /**
         * Get material type string for backwards compatibility
         */
        val materialType: String
            get() = if (variant.name.equals("Basic", ignoreCase = true)) {
                material.name.uppercase()
            } else {
                "${material.name}_${variant.name}".uppercase()
            }

        /**
         * Convert to PurchaseLinks for UI compatibility
         */
        fun toPurchaseLinks(): PurchaseLinks {
            return PurchaseLinks(
                spoolAvailable = productUrl != null,
                refillAvailable = false, // Bambu doesn't distinguish refill vs spool in current URLs
                spoolUrl = productUrl,
                refillUrl = null
            )
        }
    }


    /**
     * Get complete product information with all normalized data
     */
    fun getCompleteProductView(sku: String): CompleteProductView? {
        NormalizedDataPopulator.initialize()
        
        val product = getNormalizedProductBySku(sku) ?: return null
        val material = baseMaterials.find { it.name == product.materialName } ?: return null
        val variant = materialVariants.find { it.name == product.variantName } ?: return null
        val series = series.find { it.code == product.seriesCode } ?: return null
        val color = materialColors.find { 
            it.colorCode == product.colorCode && 
            it.materialName == product.materialName && 
            it.variantName == product.variantName 
        } ?: return null
        val temperatureProfile = temperatureProfiles[material.temperatureProfileName] ?: return null
        
        // Find matching product slug
        val productSlug = productSlugs.find { 
            it.materialName == material.name && it.variantName == variant.name 
        }
        
        val productUrl = if (product.shopifyVariantId != null && productSlug != null) {
            "https://uk.store.bambulab.com/en/products/${productSlug.slug}?id=${product.shopifyVariantId}"
        } else null

        return CompleteProductView(
            sku = product.sku,
            shopifyVariantId = product.shopifyVariantId,
            material = material,
            variant = variant,
            series = series,
            color = color,
            productSlug = productSlug,
            temperatureProfile = temperatureProfile,
            productUrl = productUrl
        )
    }
}