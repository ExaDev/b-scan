package com.bscan.data.bambu

import android.util.Log
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
    
    private const val TAG = "NormalizedBambuData"

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
     * Based on actual product data from Bambu Lab catalog with descriptive names
     */
    val materialColors = listOf(
        // PLA Basic colors
        MaterialColor("K0", "PLA", "Basic", "Black", "#000000"),
        MaterialColor("W0", "PLA", "Basic", "Jade White", "#FFFFFF"),
        MaterialColor("R0", "PLA", "Basic", "Maroon Red", "#9D2235"),
        MaterialColor("G0", "PLA", "Basic", "Bambu Green", "#00AE42"),
        MaterialColor("B0", "PLA", "Basic", "Blue Grey", "#5B6579"),
        MaterialColor("Y0", "PLA", "Basic", "Yellow", "#F4EE2A"),
        MaterialColor("O0", "PLA", "Basic", "Orange", "#FF6A13"),
        MaterialColor("P0", "PLA", "Basic", "Purple", "#5E43B7"),
        MaterialColor("T0", "PLA", "Basic", "Cyan", "#0086D6"),
        MaterialColor("C0", "PLA", "Basic", "Clear", "#F0F8FF80"),
        MaterialColor("L0", "PLA", "Basic", "Mint Lime", "#B6FF43"),
        MaterialColor("M0", "PLA", "Basic", "Pink", "#F55A74"),
        MaterialColor("N0", "PLA", "Basic", "Brown", "#9D432C"),
        MaterialColor("S0", "PLA", "Basic", "Gray", "#8E9089"),
        MaterialColor("D0", "PLA", "Basic", "Gold", "#E4BD68"),
        
        // PLA Matte colors
        MaterialColor("K0", "PLA", "Matte", "Black", "#1C1C1C"),
        MaterialColor("W0", "PLA", "Matte", "Ivory White", "#FFFFFF"),
        MaterialColor("R0", "PLA", "Matte", "Scarlet Red", "#DE4343"),
        MaterialColor("G0", "PLA", "Matte", "Grass Green", "#61C680"),
        MaterialColor("B0", "PLA", "Matte", "Ice Blue", "#A3D8E1"),
        MaterialColor("Y0", "PLA", "Matte", "Lemon Yellow", "#F7D959"),
        MaterialColor("O0", "PLA", "Matte", "Mandarin Orange", "#F99963"),
        MaterialColor("P0", "PLA", "Matte", "Lilac Purple", "#AE96D4"),
        
        // PLA Metal colors
        MaterialColor("K0", "PLA", "Metal", "Black", "#2F2F2F"),
        MaterialColor("S0", "PLA", "Metal", "Iron Gray Metallic", "#43403D"),
        MaterialColor("D0", "PLA", "Metal", "Iridium Gold Metallic", "#DAA520"),
        MaterialColor("Z0", "PLA", "Metal", "Bronze", "#CD7F32"),
        MaterialColor("A0", "PLA", "Metal", "Copper Brown Metallic", "#AA6443"),
        MaterialColor("G0", "PLA", "Metal", "Oxide Green Metallic", "#1D7C6A"),
        MaterialColor("B0", "PLA", "Metal", "Cobalt Blue Metallic", "#39699E"),
        
        // PLA Silk colors
        MaterialColor("K0", "PLA", "Silk", "Black", "#1A1A1A"),
        MaterialColor("W0", "PLA", "Silk", "White", "#FFFFFF"),
        MaterialColor("R0", "PLA", "Silk", "Candy Red", "#D02727"),
        MaterialColor("G0", "PLA", "Silk", "Candy Green", "#018814"),
        MaterialColor("B0", "PLA", "Silk", "Blue", "#008BDA"),
        MaterialColor("Y0", "PLA", "Silk", "Yellow", "#DAA520"),
        MaterialColor("O0", "PLA", "Silk", "Orange", "#FF4500"),
        MaterialColor("P0", "PLA", "Silk", "Aurora Purple", "#8671CB"),
        MaterialColor("S0", "PLA", "Silk", "Silver", "#C8C8C8"),
        MaterialColor("D0", "PLA", "Silk", "Gold", "#BA9594"),
        MaterialColor("T0", "PLA", "Silk", "Teal", "#008B8B"),
        MaterialColor("M0", "PLA", "Silk", "Magenta", "#FF1493"),
        
        // PLA Marble colors
        MaterialColor("K0", "PLA", "Marble", "Black", "#36454F"),
        MaterialColor("W0", "PLA", "Marble", "White", "#F5F5DC"),
        MaterialColor("G0", "PLA", "Marble", "Green", "#228B22"),
        MaterialColor("R0", "PLA", "Marble", "Red Granite", "#AD4E38"),
        MaterialColor("B0", "PLA", "Marble", "Blue", "#4169E1"),
        
        // PLA Glow colors
        MaterialColor("G0", "PLA", "Glow", "Green", "#ADFF2F"),
        MaterialColor("B0", "PLA", "Glow", "Blue", "#7AC0E9"),
        MaterialColor("Y0", "PLA", "Glow", "Yellow", "#F8FF80"),
        MaterialColor("O0", "PLA", "Glow", "Orange", "#FF9D5B"),
        
        // PETG Basic colors
        MaterialColor("K0", "PETG", "Basic", "Black", "#000000"),
        MaterialColor("W0", "PETG", "Basic", "White", "#F8F8FF"),
        MaterialColor("C0", "PETG", "Basic", "Clear", "#F0F8FF80"),
        MaterialColor("B0", "PETG", "Basic", "Indigo Blue", "#324585"),
        MaterialColor("R0", "PETG", "Basic", "Brick Red", "#9F332A"),
        MaterialColor("G0", "PETG", "Basic", "Malachite Green", "#16B08E"),
        MaterialColor("Y0", "PETG", "Basic", "Yellow", "#FFD700"),
        MaterialColor("O0", "PETG", "Basic", "Translucent Orange", "#FF911A80"),
        MaterialColor("P0", "PETG", "Basic", "Violet Purple", "#583061"),
        MaterialColor("T0", "PETG", "Basic", "Translucent Teal", "#77EDD780"),
        MaterialColor("M0", "PETG", "Basic", "Translucent Pink", "#F9C1BD80"),
        MaterialColor("N0", "PETG", "Basic", "Translucent Brown", "#C9A38180"),
        MaterialColor("L0", "PETG", "Basic", "Translucent Light Blue", "#61B0FF80"),
        MaterialColor("U0", "PETG", "Basic", "Translucent Purple", "#D6ABFF80"),
        MaterialColor("V0", "PETG", "Basic", "Translucent Olive", "#748C4580"),
        MaterialColor("S0", "PETG", "Basic", "Translucent Gray", "#80808080"),
        
        // ABS Basic colors
        MaterialColor("K0", "ABS", "Basic", "Black", "#000000"),
        MaterialColor("W0", "ABS", "Basic", "White", "#FFFFFF"),
        MaterialColor("R0", "ABS", "Basic", "Red", "#D32941"),
        MaterialColor("G0", "ABS", "Basic", "Green", "#00AE42"),
        MaterialColor("B0", "ABS", "Basic", "Navy Blue", "#0C2340"),
        MaterialColor("Y0", "ABS", "Basic", "Yellow", "#FFC72C"),
        MaterialColor("O0", "ABS", "Basic", "Orange", "#FF6A13"),
        MaterialColor("S0", "ABS", "Basic", "Silver", "#87909A"),
        
        // ASA Basic colors
        MaterialColor("K0", "ASA", "Basic", "Black", "#000000"),
        MaterialColor("W0", "ASA", "Basic", "White", "#F5F5F5"),
        MaterialColor("R0", "ASA", "Basic", "Red", "#E02928"),
        MaterialColor("G0", "ASA", "Basic", "Green", "#00A6A0"),
        MaterialColor("B0", "ASA", "Basic", "Blue", "#2140B4"),
        MaterialColor("Y0", "ASA", "Basic", "Yellow", "#FFD700"),
        MaterialColor("S0", "ASA", "Basic", "Silver", "#C0C0C0"),
        
        // TPU 90A colors
        MaterialColor("K0", "TPU", "90A", "Black", "#101820"),
        MaterialColor("W0", "TPU", "90A", "White", "#FFFAF0"),
        MaterialColor("C0", "TPU", "90A", "Clear", "#F8F8FF80"),
        MaterialColor("R0", "TPU", "90A", "Red", "#C8102E"),
        MaterialColor("G0", "TPU", "90A", "Neon Green", "#90FF1A"),
        MaterialColor("B0", "TPU", "90A", "Blue", "#0072CE"),
        MaterialColor("Y0", "TPU", "90A", "Yellow", "#F3E600"),
        MaterialColor("O0", "TPU", "90A", "Neon Orange", "#F68B1B"),
        
        // PC Basic colors
        MaterialColor("K0", "PC", "Basic", "Black", "#000000"),
        MaterialColor("W0", "PC", "Basic", "White", "#F5F5F5"),
        MaterialColor("C0", "PC", "Basic", "Clear", "#F0F8FF80"),
        MaterialColor("R0", "PC", "Basic", "Red", "#DC143C"),
        MaterialColor("B0", "PC", "Basic", "Blue", "#4169E1"),
        MaterialColor("X0", "PC", "Basic", "Transparent", "#F0F8FF60"),
        
        // PA Basic colors
        MaterialColor("W0", "PA", "Basic", "White", "#F5F5F5"),
        MaterialColor("K0", "PA", "Basic", "Black Sparkle", "#2D2B28"),
        MaterialColor("G0", "PA", "Basic", "Green Sparkle", "#3F5443"),
        MaterialColor("R0", "PA", "Basic", "Red Sparkle", "#792B36"),
        
        // Support colors
        MaterialColor("W0", "PVA", "Basic", "White", "#F5F5F5"),
        MaterialColor("W0", "SUPPORT", "Basic", "White", "#F5F5F5")
    )

    /**
     * Normalized products table
     * Based on actual RFID keys from BambuVariantSkuMapper
     */
    val normalizedProducts = listOf(
        // PLA Basic products (GFA00 material ID)
        NormalizedProduct("10101", "40000001", "PLA", "Basic", "A00", "K0", "pla-basic"),
        NormalizedProduct("10102", "40000002", "PLA", "Basic", "A00", "W0", "pla-basic"),
        NormalizedProduct("10103", "40000003", "PLA", "Basic", "A00", "R0", "pla-basic"),
        NormalizedProduct("10104", "40000004", "PLA", "Basic", "A00", "G0", "pla-basic"),
        NormalizedProduct("10105", "40000005", "PLA", "Basic", "A00", "B0", "pla-basic"),
        NormalizedProduct("10106", "40000006", "PLA", "Basic", "A00", "Y0", "pla-basic"),
        NormalizedProduct("10107", "40000007", "PLA", "Basic", "A00", "O0", "pla-basic"),
        NormalizedProduct("10108", "40000008", "PLA", "Basic", "A00", "P0", "pla-basic"),
        NormalizedProduct("10109", "40000009", "PLA", "Basic", "A00", "T0", "pla-basic"),
        NormalizedProduct("10110", "40000010", "PLA", "Basic", "A00", "C0", "pla-basic"),
        NormalizedProduct("10111", "40000011", "PLA", "Basic", "A00", "L0", "pla-basic"),
        NormalizedProduct("10112", "40000012", "PLA", "Basic", "A00", "M0", "pla-basic"),
        NormalizedProduct("10113", "40000013", "PLA", "Basic", "A00", "N0", "pla-basic"),
        NormalizedProduct("10114", "40000014", "PLA", "Basic", "A00", "S0", "pla-basic"),
        NormalizedProduct("10115", "40000015", "PLA", "Basic", "A00", "D0", "pla-basic"),
        
        // PLA Matte products (GFA01 material ID)
        NormalizedProduct("11101", "40000101", "PLA", "Matte", "A00", "K0", "pla-matte"),
        NormalizedProduct("11102", "40000102", "PLA", "Matte", "A00", "W0", "pla-matte"),
        NormalizedProduct("11103", "40000103", "PLA", "Matte", "A00", "R0", "pla-matte"),
        NormalizedProduct("11104", "40000104", "PLA", "Matte", "A00", "G0", "pla-matte"),
        NormalizedProduct("11105", "40000105", "PLA", "Matte", "A00", "B0", "pla-matte"),
        NormalizedProduct("11106", "40000106", "PLA", "Matte", "A00", "Y0", "pla-matte"),
        NormalizedProduct("11107", "40000107", "PLA", "Matte", "A00", "O0", "pla-matte"),
        NormalizedProduct("11108", "40000108", "PLA", "Matte", "A00", "P0", "pla-matte"),
        
        // PLA Metal products (GFA02 material ID)
        NormalizedProduct("12101", "40000201", "PLA", "Metal", "A00", "K0", "pla-metal"),
        NormalizedProduct("12102", "40000202", "PLA", "Metal", "A00", "S0", "pla-metal"),
        NormalizedProduct("12103", "40000203", "PLA", "Metal", "A00", "D0", "pla-metal"),
        NormalizedProduct("12104", "40000204", "PLA", "Metal", "A00", "Z0", "pla-metal"),
        NormalizedProduct("12105", "40000205", "PLA", "Metal", "A00", "A0", "pla-metal"),
        
        // PLA Silk products (GFA05 material ID) 
        NormalizedProduct("15101", "40000501", "PLA", "Silk", "A00", "K0", "pla-silk"),
        NormalizedProduct("15102", "40000502", "PLA", "Silk", "A00", "W0", "pla-silk"),
        NormalizedProduct("15103", "40000503", "PLA", "Silk", "A00", "R0", "pla-silk"),
        NormalizedProduct("15104", "40000504", "PLA", "Silk", "A00", "G0", "pla-silk"),
        NormalizedProduct("15105", "40000505", "PLA", "Silk", "A00", "B0", "pla-silk"),
        NormalizedProduct("15106", "40000506", "PLA", "Silk", "A00", "Y0", "pla-silk"),
        NormalizedProduct("15107", "40000507", "PLA", "Silk", "A00", "O0", "pla-silk"),
        NormalizedProduct("15108", "40000508", "PLA", "Silk", "A00", "P0", "pla-silk"),
        NormalizedProduct("15109", "40000509", "PLA", "Silk", "A00", "S0", "pla-silk"),
        NormalizedProduct("15110", "40000510", "PLA", "Silk", "A00", "D0", "pla-silk"),
        NormalizedProduct("15111", "40000511", "PLA", "Silk", "A00", "T0", "pla-silk"),
        NormalizedProduct("15112", "40000512", "PLA", "Silk", "A00", "M0", "pla-silk"),
        
        // PLA Marble products (GFA07 material ID)
        NormalizedProduct("17101", "40000701", "PLA", "Marble", "A00", "K0", "pla-marble"),
        NormalizedProduct("17102", "40000702", "PLA", "Marble", "A00", "W0", "pla-marble"),
        NormalizedProduct("17103", "40000703", "PLA", "Marble", "A00", "G0", "pla-marble"),
        NormalizedProduct("17104", "40000704", "PLA", "Marble", "A00", "R0", "pla-marble"),
        NormalizedProduct("17105", "40000705", "PLA", "Marble", "A00", "B0", "pla-marble"),
        
        // PLA Glow products (GFA12 material ID)
        NormalizedProduct("1A101", "40001201", "PLA", "Glow", "A00", "G0", "pla-glow"),
        NormalizedProduct("1A102", "40001202", "PLA", "Glow", "A00", "B0", "pla-glow"),
        NormalizedProduct("1A103", "40001203", "PLA", "Glow", "A00", "Y0", "pla-glow"),
        NormalizedProduct("1A104", "40001204", "PLA", "Glow", "A00", "O0", "pla-glow"),
        
        // PETG Basic products (GFG00/GFG01 material ID)
        NormalizedProduct("20101", "40002001", "PETG", "Basic", "A00", "K0", "petg-basic"),
        NormalizedProduct("20102", "40002002", "PETG", "Basic", "A00", "W0", "petg-basic"),
        NormalizedProduct("20103", "40002003", "PETG", "Basic", "A00", "C0", "petg-basic"),
        NormalizedProduct("20104", "40002004", "PETG", "Basic", "A00", "B0", "petg-basic"),
        NormalizedProduct("20105", "40002005", "PETG", "Basic", "A00", "R0", "petg-basic"),
        NormalizedProduct("20106", "40002006", "PETG", "Basic", "A00", "G0", "petg-basic"),
        NormalizedProduct("20107", "40002007", "PETG", "Basic", "A00", "Y0", "petg-basic"),
        NormalizedProduct("20108", "40002008", "PETG", "Basic", "A00", "O0", "petg-basic"),
        NormalizedProduct("20109", "40002009", "PETG", "Basic", "A00", "P0", "petg-basic"),
        NormalizedProduct("20110", "40002010", "PETG", "Basic", "A00", "T0", "petg-basic"),
        
        // ABS Basic products (GFL01 material ID)
        NormalizedProduct("30101", "40003001", "ABS", "Basic", "A00", "K0", "abs-basic"),
        NormalizedProduct("30102", "40003002", "ABS", "Basic", "A00", "W0", "abs-basic"),
        NormalizedProduct("30103", "40003003", "ABS", "Basic", "A00", "R0", "abs-basic"),
        NormalizedProduct("30104", "40003004", "ABS", "Basic", "A00", "G0", "abs-basic"),
        NormalizedProduct("30105", "40003005", "ABS", "Basic", "A00", "B0", "abs-basic"),
        NormalizedProduct("30106", "40003006", "ABS", "Basic", "A00", "Y0", "abs-basic"),
        NormalizedProduct("30107", "40003007", "ABS", "Basic", "A00", "O0", "abs-basic"),
        NormalizedProduct("30108", "40003008", "ABS", "Basic", "A00", "S0", "abs-basic"),
        
        // ASA Basic products (GFL02 material ID)
        NormalizedProduct("31101", "40003101", "ASA", "Basic", "A00", "K0", "asa-basic"),
        NormalizedProduct("31102", "40003102", "ASA", "Basic", "A00", "W0", "asa-basic"),
        NormalizedProduct("31103", "40003103", "ASA", "Basic", "A00", "R0", "asa-basic"),
        NormalizedProduct("31104", "40003104", "ASA", "Basic", "A00", "G0", "asa-basic"),
        NormalizedProduct("31105", "40003105", "ASA", "Basic", "A00", "B0", "asa-basic"),
        NormalizedProduct("31106", "40003106", "ASA", "Basic", "A00", "Y0", "asa-basic"),
        NormalizedProduct("31107", "40003107", "ASA", "Basic", "A00", "S0", "asa-basic"),
        
        // TPU 90A products (GFL04 material ID)
        NormalizedProduct("40101", "40004001", "TPU", "90A", "A00", "K0", "tpu-90a"),
        NormalizedProduct("40102", "40004002", "TPU", "90A", "A00", "W0", "tpu-90a"),
        NormalizedProduct("40103", "40004003", "TPU", "90A", "A00", "C0", "tpu-90a"),
        NormalizedProduct("40104", "40004004", "TPU", "90A", "A00", "R0", "tpu-90a"),
        NormalizedProduct("40105", "40004005", "TPU", "90A", "A00", "G0", "tpu-90a"),
        NormalizedProduct("40106", "40004006", "TPU", "90A", "A00", "B0", "tpu-90a"),
        NormalizedProduct("40107", "40004007", "TPU", "90A", "A00", "Y0", "tpu-90a"),
        NormalizedProduct("40108", "40004008", "TPU", "90A", "A00", "O0", "tpu-90a"),
        
        // PC Basic products (GFC00 material ID)
        NormalizedProduct("50101", "40005001", "PC", "Basic", "A00", "K0", "pc-basic"),
        NormalizedProduct("50102", "40005002", "PC", "Basic", "A00", "W0", "pc-basic"),
        NormalizedProduct("50103", "40005003", "PC", "Basic", "A00", "C0", "pc-basic"),
        NormalizedProduct("50104", "40005004", "PC", "Basic", "A00", "R0", "pc-basic"),
        NormalizedProduct("50105", "40005005", "PC", "Basic", "A00", "B0", "pc-basic"),
        
        // PA Basic products (GFN04 material ID)
        NormalizedProduct("60101", "40006001", "PA", "Basic", "A00", "W0", "pa-nylon"),
        NormalizedProduct("60102", "40006002", "PA", "Basic", "A00", "K0", "pa-nylon"),
        NormalizedProduct("60103", "40006003", "PA", "Basic", "A00", "G0", "pa-nylon"),
        NormalizedProduct("60104", "40006004", "PA", "Basic", "A00", "R0", "pa-nylon"),
        
        // Support materials
        NormalizedProduct("70101", "40007001", "PVA", "Basic", "A00", "W0", "pva-basic"),
        NormalizedProduct("70201", "40007101", "SUPPORT", "Basic", "A00", "W0", "support-material")
    )

    /**
     * Get material colors for a specific material and variant combination
     */
    fun getMaterialColorsByMaterial(materialName: String, variantName: String): List<MaterialColor> {
        if (materialColors.isEmpty()) {
            Log.w(TAG, "getMaterialColorsByMaterial called but materialColors is empty")
            return emptyList()
        }
        return materialColors.filter { it.materialName == materialName && it.variantName == variantName }
    }
    
    fun getAllMaterialColors(): List<MaterialColor> {
        return materialColors
    }

    fun getNormalizedProductBySku(sku: String): NormalizedProduct? {
        if (normalizedProducts.isEmpty()) {
            Log.w(TAG, "getNormalizedProductBySku called but normalizedProducts is empty")
            return null
        }
        return normalizedProducts.find { it.sku == sku }
    }
    
    fun getAllNormalizedProducts(): List<NormalizedProduct> {
        if (normalizedProducts.isEmpty()) {
            Log.w(TAG, "getAllNormalizedProducts called but normalizedProducts is empty")
        }
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
         * Get material type string
         */
        val materialType: String
            get() = if (variant.name.equals("Basic", ignoreCase = true)) {
                material.name.uppercase()
            } else {
                "${material.name}_${variant.name}".uppercase()
            }

        /**
         * Convert to PurchaseLinks for UI display
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
        // Simple check to prevent issues when data is empty
        if (normalizedProducts.isEmpty() || materialColors.isEmpty()) {
            Log.w(TAG, "getCompleteProductView called but essential data is empty")
            return null
        }
        
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