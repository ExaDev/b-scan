package com.bscan.data.bambu

import com.bscan.data.bambu.data.BambuColorMappings
import com.bscan.data.bambu.data.BambuMaterialMappings
import com.bscan.data.bambu.data.BambuSeriesMappings
import com.bscan.data.bambu.BambuVariantSkuMapper
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
        
        return ManufacturerCatalog(
            name = "bambu",
            displayName = "Bambu Lab",
            materials = generateMaterialDefinitions(),
            temperatureProfiles = generateTemperatureProfiles(),
            colorPalette = generateColorPalette(),
            rfidMappings = generateRfidMappings(),
            componentDefaults = generateComponentDefaults(),
            products = generateProductEntries(),
            tagFormat = TagFormat.BAMBU_PROPRIETARY
        )
    }
    
    /**
     * Generate material definitions from BambuMaterialMappings
     */
    private fun generateMaterialDefinitions(): Map<String, MaterialDefinition> {
        val materials = mutableMapOf<String, MaterialDefinition>()
        
        BambuMaterialMappings.MATERIAL_ID_MAPPINGS.forEach { (materialId, mappingInfo) ->
            val materialType = mappingInfo.displayName
            val temperatureProfile = getTemperatureProfileForMaterial(materialType)
            val properties = getMaterialProperties(materialType)
            
            materials[materialType] = MaterialDefinition(
                displayName = mappingInfo.displayName,
                temperatureProfile = temperatureProfile,
                properties = properties
            )
        }
        
        Log.i(TAG, "Generated ${materials.size} material definitions")
        return materials
    }
    
    /**
     * Get temperature profile identifier for material type
     */
    private fun getTemperatureProfileForMaterial(materialType: String): String {
        return when {
            materialType.contains("PLA", ignoreCase = true) -> "pla_standard"
            materialType.contains("ABS", ignoreCase = true) -> "abs_standard"
            materialType.contains("ASA", ignoreCase = true) -> "asa_standard"
            materialType.contains("PETG", ignoreCase = true) -> "petg_standard"
            materialType.contains("PC", ignoreCase = true) -> "pc_standard"
            materialType.contains("TPU", ignoreCase = true) -> "tpu_standard"
            materialType.contains("PA", ignoreCase = true) -> "nylon_standard"
            materialType.contains("PVA", ignoreCase = true) -> "pva_standard"
            materialType.contains("S", ignoreCase = true) -> "support_standard"
            else -> "generic_standard"
        }
    }
    
    /**
     * Get material properties for material type
     */
    private fun getMaterialProperties(materialType: String): MaterialCatalogProperties {
        return when {
            materialType.contains("PLA", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.24f,
                shrinkage = 0.3f,
                biodegradable = true,
                foodSafe = true,
                flexible = false,
                support = false,
                category = MaterialCategory.THERMOPLASTIC
            )
            materialType.contains("ABS", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.04f,
                shrinkage = 0.8f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.THERMOPLASTIC
            )
            materialType.contains("ASA", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.05f,
                shrinkage = 0.7f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.THERMOPLASTIC
            )
            materialType.contains("PETG", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.27f,
                shrinkage = 0.2f,
                biodegradable = false,
                foodSafe = true,
                flexible = false,
                support = false,
                category = MaterialCategory.THERMOPLASTIC
            )
            materialType.contains("PC", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.20f,
                shrinkage = 0.6f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.ENGINEERING
            )
            materialType.contains("TPU", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.20f,
                shrinkage = 1.0f,
                biodegradable = false,
                foodSafe = false,
                flexible = true,
                support = false,
                category = MaterialCategory.FLEXIBLE
            )
            materialType.contains("PA", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.14f,
                shrinkage = 1.5f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.ENGINEERING
            )
            materialType.contains("CF", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.30f,
                shrinkage = 0.1f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.COMPOSITE
            )
            materialType.contains("GF", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.40f,
                shrinkage = 0.2f,
                biodegradable = false,
                foodSafe = false,
                flexible = false,
                support = false,
                category = MaterialCategory.COMPOSITE
            )
            materialType.contains("PVA", ignoreCase = true) || materialType.contains("S", ignoreCase = true) -> MaterialCatalogProperties(
                density = 1.23f,
                shrinkage = 0.5f,
                biodegradable = true,
                foodSafe = false,
                flexible = false,
                support = true,
                category = MaterialCategory.SUPPORT
            )
            else -> MaterialCatalogProperties(
                category = MaterialCategory.SPECIALTY
            )
        }
    }
    
    /**
     * Generate temperature profiles for all materials
     */
    private fun generateTemperatureProfiles(): Map<String, TemperatureProfile> {
        return mapOf(
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
    }
    
    /**
     * Generate color palette from BambuColorMappings
     */
    private fun generateColorPalette(): Map<String, String> {
        val colorPalette = mutableMapOf<String, String>()
        
        BambuColorMappings.COLOR_CODE_MAPPINGS.forEach { (colorCode, mappingInfo) ->
            // Use color code as hex fallback (will be overridden by actual hex from RFID Block 5)
            val hex = getHexColorForName(mappingInfo.displayName)
            colorPalette[hex] = mappingInfo.displayName
        }
        
        Log.i(TAG, "Generated color palette with ${colorPalette.size} colors")
        return colorPalette
    }
    
    /**
     * Get approximate hex color for color name
     */
    private fun getHexColorForName(colorName: String): String {
        return when (colorName.lowercase()) {
            "black" -> "#000000"
            "white" -> "#FFFFFF"
            "red" -> "#FF0000"
            "green" -> "#00FF00"
            "blue" -> "#0000FF"
            "yellow" -> "#FFFF00"
            "orange" -> "#FFA500"
            "purple" -> "#800080"
            "pink" -> "#FFC0CB"
            "brown" -> "#A52A2A"
            "grey", "gray" -> "#808080"
            "silver" -> "#C0C0C0"
            "azure" -> "#007FFF"
            "mint" -> "#98FF98"
            else -> "#808080" // Default to gray for unknown colors
        }
    }
    
    /**
     * Generate RFID mappings from product entries and material mappings
     */
    private fun generateRfidMappings(): Map<String, RfidMapping> {
        val mappings = mutableMapOf<String, RfidMapping>()
        
        // Get all product entries to build RFID mappings
        val allProducts = generateProductEntries()
        
        allProducts.forEach { product ->
            // Create RFID key format: "MaterialID:SeriesCode-ColorCode" 
            // e.g., "GFA00:A00-K0" for PLA Basic Black
            val rfidKey = "${product.internalCode}:${product.variantId}"
            
            // Extract material type from the product
            val materialType = when {
                product.materialType.contains("PLA") -> product.materialType
                product.materialType.contains("PETG") -> "PETG"
                product.materialType.contains("ABS") -> "ABS"
                product.materialType.contains("ASA") -> "ASA"
                product.materialType.contains("PC") -> "PC"
                product.materialType.contains("TPU") -> "TPU"
                product.materialType.contains("PA") -> "PA-CF"
                else -> product.materialType
            }
            
            val rfidMapping = RfidMapping(
                rfidCode = rfidKey,
                sku = product.variantId, // Use variantId as SKU (e.g., "A00-K0")
                material = materialType,
                color = product.colorName,
                hex = product.colorHex,
                sampleCount = 1 // Default sample count
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
                type = PhysicalComponentType.FILAMENT,
                massGrams = 1000f,
                description = "Standard 1kg filament spool",
                manufacturer = "bambu"
            ),
            "filament_750g" to ComponentDefault(
                name = "750g Filament",
                type = PhysicalComponentType.FILAMENT,
                massGrams = 750f,
                description = "Standard 750g filament spool",
                manufacturer = "bambu"
            ),
            "filament_500g" to ComponentDefault(
                name = "500g Filament",
                type = PhysicalComponentType.FILAMENT,
                massGrams = 500f,
                description = "Standard 500g filament spool",
                manufacturer = "bambu"
            ),
            "spool_standard" to ComponentDefault(
                name = "Standard Spool",
                type = PhysicalComponentType.BASE_SPOOL,
                massGrams = 212f,
                description = "Standard Bambu Lab plastic spool",
                manufacturer = "bambu"
            ),
            "core_cardboard" to ComponentDefault(
                name = "Cardboard Core",
                type = PhysicalComponentType.CORE_RING,
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
        
        // Iterate through all verified RFID key mappings from BambuVariantSkuMapper
        BambuVariantSkuMapper.getAllKnownRfidKeys().forEach { rfidKey ->
            val skuInfo = BambuVariantSkuMapper.getSkuByRfidKey(rfidKey)
            if (skuInfo != null) {
                val parts = rfidKey.split(":")
                if (parts.size == 2) {
                    val materialId = parts[0] // e.g., "GFA00"
                    val variantId = parts[1] // e.g., "A00-K0"
                    
                    val productEntry = createProductEntryFromSkuInfo(
                        rfidKey = rfidKey,
                        materialId = materialId,
                        variantId = variantId,
                        skuInfo = skuInfo
                    )
                    
                    products.add(productEntry)
                }
            }
        }
        
        Log.i(TAG, "Generated ${products.size} product entries from BambuVariantSkuMapper")
        return products
    }
    
    /**
     * Create ProductEntry from SkuInfo using verified mapping data
     */
    private fun createProductEntryFromSkuInfo(
        rfidKey: String,
        materialId: String,
        variantId: String,
        skuInfo: SkuInfo
    ): ProductEntry {
        val colorHex = getHexColorForName(skuInfo.colorName)
        val colorCode = variantId.split("-").getOrNull(1) ?: ""
        
        return ProductEntry(
            variantId = variantId,
            productHandle = "bambu-${skuInfo.materialType.lowercase().replace(" ", "-")}-${skuInfo.colorName.lowercase().replace(" ", "-")}",
            productName = "Bambu ${skuInfo.materialType} ${skuInfo.colorName}",
            colorName = skuInfo.colorName,
            colorHex = colorHex,
            colorCode = colorCode,
            price = 29.99, // Standard Bambu filament price
            available = true,
            url = "https://bambulab.com/en/filament/${skuInfo.materialType.lowercase().replace(" ", "-")}",
            manufacturer = "Bambu Lab",
            materialType = skuInfo.materialType.replace(" ", "_").uppercase(),
            internalCode = materialId,
            lastUpdated = "2025-01-16T00:00:00Z",
            filamentWeightGrams = 1000f,
            spoolType = SpoolPackaging.WITH_SPOOL
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