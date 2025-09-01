package com.bscan.data.bambu

import com.bscan.data.bambu.rfid.BambuMaterialIdMapper
import com.bscan.util.ColorUtils
import com.bscan.model.*
import android.util.Log

/**
 * Runtime generator for complete Bambu Lab product catalog.
 *
 * Creates a comprehensive ManufacturerCatalog based on real RFID data extracted
 * from 428+ actual .bin files in test-data/rfid-library/, plus material and color
 * mappings from the bambu data directory.
 *
 * This replaces the static catalog_data.json asset with runtime generation from
 * verified real-world data.
 */
object BambuCatalogGenerator {
    private const val TAG = "BambuCatalogGenerator"

    /**
     * Generate complete Bambu Lab manufacturer catalog from runtime data
     */
    fun generateBambuCatalog(): ManufacturerCatalog {
        Log.i(TAG, "Generating Bambu Lab catalog from runtime data")

        val products = generateProductEntries()
        val materials = generateMaterialDefinitions(products)

        return ManufacturerCatalog(
            name = "bambu",
            displayName = "Bambu Lab",
            materials = materials,
            temperatureProfiles = NormalizedBambuData.temperatureProfiles,
            colorPalette = generateColorPalette(products),
            rfidMappings = generateRfidMappings(products),
            componentDefaults = generateComponentDefaults(),
            products = products,
            tagFormat = TagFormat.BAMBU_PROPRIETARY
        )
    }

    /**
     * Generate material definitions from product list
     */
    private fun generateMaterialDefinitions(products: List<ProductEntry>): Map<String, MaterialDefinition> {
        val materials = mutableMapOf<String, MaterialDefinition>()

        products.forEach { product ->
            val materialType = product.materialType
            if (!materials.containsKey(materialType)) {
                val temperatureProfile = getTemperatureProfileForMaterial(materialType)
                val properties = getMaterialProperties(materialType)

                materials[materialType] = MaterialDefinition(
                    displayName = materialType.replace("_", " "),
                    temperatureProfile = temperatureProfile,
                    properties = properties
                )
            }
        }

        Log.i(TAG, "Generated ${materials.size} material definitions")
        return materials
    }

    /**
     * Get temperature profile identifier for material type
     */
    private fun getTemperatureProfileForMaterial(materialType: String): String {
        return when {
            materialType.contains("PLA") -> "pla_standard"
            materialType.contains("ABS") -> "abs_standard"
            materialType.contains("ASA") -> "asa_standard"
            materialType.contains("PETG") -> "petg_standard"
            materialType.contains("PC") -> "pc_standard"
            materialType.contains("TPU") -> "tpu_standard"
            materialType.contains("PA") -> "nylon_standard"
            materialType.contains("PVA") -> "pva_standard"
            materialType.contains("SUPPORT") -> "support_standard"
            else -> "generic_standard"
        }
    }

    /**
     * Get material properties for material type using normalized data
     */
    private fun getMaterialProperties(materialType: String): MaterialCatalogProperties {
        // Extract base material name from material type
        val baseMaterialName = when {
            materialType.contains("PLA") -> "PLA"
            materialType.contains("PETG") -> "PETG" 
            materialType.contains("ABS") -> "ABS"
            materialType.contains("ASA") -> "ASA"
            materialType.contains("PC") -> "PC"
            materialType.contains("PA") -> "PA"
            materialType.contains("TPU") -> "TPU"
            materialType.contains("PVA") -> "PVA"
            materialType.contains("SUPPORT") -> "SUPPORT"
            else -> null // No default fallback - return null for unknown materials
        }
        
        if (baseMaterialName == null) {
            Log.w(TAG, "Unknown material type: $materialType")
            return MaterialCatalogProperties(category = MaterialCategory.UNKNOWN)
        }
        
        val baseMaterial = NormalizedBambuData.baseMaterials.find { it.name == baseMaterialName }
        if (baseMaterial == null) {
            Log.w(TAG, "Material not found in normalized data: $baseMaterialName")
            return MaterialCatalogProperties(category = MaterialCategory.UNKNOWN)
        }
        
        return baseMaterial.properties
    }

    /**
     * Generate color palette from product list
     */
    private fun generateColorPalette(products: List<ProductEntry>): Map<String, String> {
        val colorPalette = products.mapNotNull { product ->
            product.colorHex?.let { hex -> hex to product.colorName }
        }.toMap()
        Log.i(TAG, "Generated color palette with ${colorPalette.size} colors")
        return colorPalette
    }

    /**
     * Generate RFID mappings from product entries
     */
    private fun generateRfidMappings(products: List<ProductEntry>): Map<String, RfidMapping> {
        val mappings = mutableMapOf<String, RfidMapping>()

        products.forEach { product ->
            val rfidKey = "${product.internalCode}:${product.variantId}"
            val rfidMapping = RfidMapping(
                rfidCode = rfidKey,
                sku = product.variantId,
                material = product.materialType,
                color = product.colorName,
                hex = product.colorHex,
                sampleCount = 1
            )
            mappings[rfidKey] = rfidMapping
        }

        Log.i(TAG, "Generated ${mappings.size} RFID mappings from product database")
        return mappings
    }

    /**
     * Generate component defaults for Bambu Lab spools
     */
    private fun generateComponentDefaults(): Map<String, ComponentDefault> {
        return mapOf(
            "filament_1kg" to ComponentDefault(
                name = "1kg Filament",
                category = "filament",
                massGrams = 1000f,
                description = "Standard 1kg filament spool",
                manufacturer = "bambu"
            ),
            "spool_standard" to ComponentDefault(
                name = "Standard Spool",
                category = "spool",
                massGrams = 212f,
                description = "Standard Bambu Lab plastic spool",
                manufacturer = "bambu"
            ),
            "core_cardboard" to ComponentDefault(
                name = "Cardboard Core",
                category = "core",
                massGrams = 33f,
                description = "Standard cardboard core tube",
                manufacturer = "bambu"
            )
        )
    }

    /**
     * Generate product entries from verified BambuVariantSkuMapper data
     */
     private fun generateProductEntries(): List<ProductEntry> {
        val products = mutableListOf<ProductEntry>()

        BambuVariantSkuMapper.getAllKnownRfidKeys().forEach { rfidKey ->
            val skuInfo = BambuVariantSkuMapper.getSkuByRfidKey(rfidKey)
            if (skuInfo != null) {
                val parts = rfidKey.split(":")
                if (parts.size == 2) {
                    val materialId = parts[0]
                    val variantId = parts[1]

                    val productEntry = createProductEntryFromSkuInfo(
                        materialId = materialId,
                        variantId = variantId,
                        skuInfo = skuInfo
                    )
                    products.add(productEntry)
                }
            }
        }

        // Add component defaults as catalog entries
        products.addAll(generateComponentProductEntries())

        Log.i(TAG, "Generated ${products.size} product entries from BambuVariantSkuMapper and component defaults")
        return products
    }

    /**
     * Generate ProductEntry objects from component defaults for catalog display
     */
    private fun generateComponentProductEntries(): List<ProductEntry> {
        val componentDefaults = generateComponentDefaults()
        return componentDefaults.map { (key, componentDefault) ->
            ProductEntry(
                variantId = "component_$key",
                productHandle = "bambu-$key",
                productName = componentDefault.name,
                colorName = when(key) {
                    "spool_standard" -> "Natural"
                    "core_cardboard" -> "Brown"
                    else -> "Default"
                },
                colorHex = when(key) {
                    "spool_standard" -> "#F5F5DC" // Beige/natural plastic color
                    "core_cardboard" -> "#8B4513" // Brown cardboard color
                    else -> "#808080"
                },
                colorCode = when(key) {
                    "spool_standard" -> "NAT"
                    "core_cardboard" -> "BRN"
                    else -> "DEF"
                },
                price = 0.0, // Components are not sold separately
                available = true,
                url = "https://store.bambulab.com/",
                manufacturer = "Bambu Lab",
                materialType = componentDefault.category.replaceFirstChar { it.uppercase() }, // "spool" -> "Spool"
                internalCode = key,
                lastUpdated = "2025-01-16T00:00:00Z",
                filamentWeightGrams = componentDefault.massGrams,
                spoolType = when(key) {
                    "spool_standard" -> SpoolPackaging.WITH_SPOOL
                    "core_cardboard" -> SpoolPackaging.REFILL
                    else -> null
                },
                alternativeIds = emptySet()
            )
        }
    }

    /**
     * Create ProductEntry from SkuInfo using verified mapping data
     */
    private fun createProductEntryFromSkuInfo(
        materialId: String,
        variantId: String,
        skuInfo: SkuInfo
    ): ProductEntry {
        // Get enhanced product data for accurate color names and material types
        val product = BambuProductCatalog.getProductBySku(skuInfo.sku)
        val colorName = product?.colorName ?: skuInfo.colorName
        val materialType = product?.materialType ?: skuInfo.materialType
        val colorHex = product?.colorHex ?: ColorUtils.getHexColorForName(colorName)
        val colorCode = variantId.split("-").getOrNull(1) ?: ""

        // Create alternative identifiers set including the numeric SKU ID
        val alternativeIds = mutableSetOf<String>()
        
        // Add the numeric SKU ID as an alternative identifier
        alternativeIds.add(skuInfo.sku)
        
        // Add other potential identifiers
        product?.let { alternativeIds.add("${materialType}_${colorName}".replace(" ", "_")) }

        return ProductEntry(
            variantId = variantId,
            productHandle = "bambu-${materialType.lowercase().replace(" ", "-")}-${colorName.lowercase().replace(" ", "-")}",
            productName = "Bambu ${materialType} ${colorName}",
            colorName = colorName,
            colorHex = colorHex,
            colorCode = colorCode,
            price = 29.99,
            available = true,
            url = "https://bambulab.com/en/filament/${materialType.lowercase().replace(" ", "-")}",
            manufacturer = "Bambu Lab",
            materialType = materialType.replace(" ", "_").uppercase(),
            internalCode = materialId,
            lastUpdated = "2025-01-16T00:00:00Z",
            filamentWeightGrams = 1000f,
            spoolType = SpoolPackaging.WITH_SPOOL,
            alternativeIds = alternativeIds
        )
    }

    /**
     * Generate complete CatalogData with Bambu Lab manufacturer
     */
    fun generateCatalogData(): CatalogData {
        val bambuCatalog = generateBambuCatalog()
        return CatalogData(
            manufacturers = mapOf("bambu" to bambuCatalog)
        )
    }
}
