package com.bscan.data.bambu

import com.bscan.data.bambu.rfid.BambuMaterialIdMapper
import com.bscan.util.ColorUtils
import com.bscan.model.*
import com.bscan.model.graph.entities.StockDefinition
import com.bscan.model.graph.*
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

        val stockDefinitions = generateStockDefinitions()
        

        return ManufacturerCatalog(
            name = "bambu",
            displayName = "Bambu Lab",
            materials = emptyMap(), // Materials are now CatalogItems
            temperatureProfiles = NormalizedBambuData.temperatureProfiles,
            colorPalette = generateColorPalette(stockDefinitions),
            rfidMappings = generateRfidMappings(stockDefinitions),
            componentDefaults = emptyMap(), // Components are now CatalogItems
            stockDefinitions = stockDefinitions, // Add new unified stock definitions
            tagFormat = TagFormat.BAMBU_PROPRIETARY
        )
    }

    /**
     * Generate material definitions from stock definition list
     */
    private fun generateMaterialDefinitions(stockDefinitions: List<StockDefinition>): Map<String, MaterialDefinition> {
        val materials = mutableMapOf<String, MaterialDefinition>()

        stockDefinitions.forEach { stockDef ->
            val materialType = stockDef.getProperty<String>("materialType") ?: ""
            if (!materials.containsKey(materialType)) {
                val temperatureProfile = getTemperatureProfileForMaterial(materialType)
                val properties = getMaterialProperties(materialType)

                materials[materialType] = MaterialDefinition(
                    displayName = materialType.replace("_", " "),
                    temperatureProfile = temperatureProfile,
                    properties = properties
                )
            }
            
            // Also create base material definitions (PLA, PETG, etc.) for test compatibility
            val baseMaterial = when {
                materialType.contains("PLA") -> "PLA"
                materialType.contains("PETG") -> "PETG"
                materialType.contains("ABS") -> "ABS"
                materialType.contains("ASA") -> "ASA"
                materialType.contains("PC") -> "PC"
                materialType.contains("PA") -> "PA"
                materialType.contains("TPU") -> "TPU"
                materialType.contains("PVA") -> "PVA"
                else -> null
            }
            
            if (baseMaterial != null && !materials.containsKey(baseMaterial)) {
                val temperatureProfile = getTemperatureProfileForMaterial(baseMaterial)
                val properties = getMaterialProperties(baseMaterial)
                
                materials[baseMaterial] = MaterialDefinition(
                    displayName = baseMaterial,
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
     * Generate color palette from stock definitions list
     */
    private fun generateColorPalette(stockDefinitions: List<StockDefinition>): Map<String, String> {
        val colorPalette = stockDefinitions.mapNotNull { stockDef ->
            stockDef.getProperty<String>("colorHex")?.let { hex -> 
                val colorName = stockDef.getProperty<String>("colorName") ?: ""
                hex to colorName 
            }
        }.toMap()
        Log.i(TAG, "Generated color palette with ${colorPalette.size} colors")
        return colorPalette
    }

    /**
     * Generate RFID mappings only for stock definitions that have valid material ID mappings
     */
    private fun generateRfidMappings(stockDefinitions: List<StockDefinition>): Map<String, RfidMapping> {
        val mappings = mutableMapOf<String, RfidMapping>()
        
        // Material ID mappings from BambuVariantSkuMapper
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

        stockDefinitions.forEach { stockDef ->
            val sku = stockDef.getProperty<String>("sku") ?: ""
            // Skip component products (they don't have RFID mappings)
            if (sku.startsWith("component_")) return@forEach
            
            // Extract material and variant from internal code if available
            val internalCode = stockDef.getProperty<String>("internalCode") ?: ""
            val internalParts = internalCode.split("_")
            if (internalParts.size == 2) {
                val materialName = internalParts[0]
                val variantName = internalParts[1]
                val materialCombo = materialName to variantName
                
                // Only create RFID mapping if this material+variant combination has an RFID material ID
                materialIdMap[materialCombo]?.let { materialId ->
                    // Find alphanumeric variant ID from alternative identifiers if available
                    val alphanumericVariantId = stockDef.getProperty<String>("alternativeId")
                        ?.takeIf { it.matches(Regex("[A-Z]\\d{2}-[A-Z]\\d")) }
                    
                    if (alphanumericVariantId != null) {
                        val rfidKey = "$materialId:$alphanumericVariantId"
                        val rfidMapping = RfidMapping(
                            rfidCode = rfidKey,
                            sku = sku, // Use SKU as target
                            material = stockDef.getProperty<String>("materialType") ?: "",
                            color = stockDef.getProperty<String>("colorName") ?: "",
                            hex = stockDef.getProperty<String>("colorHex"),
                            sampleCount = 1
                        )
                        mappings[rfidKey] = rfidMapping
                    }
                }
            }
        }

        Log.i(TAG, "Generated ${mappings.size} RFID mappings from ${stockDefinitions.size} stock definitions")
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
     * Generate stock definitions from all normalized products and components
     */
    private fun generateStockDefinitions(): List<StockDefinition> {
        val stockDefinitions = mutableListOf<StockDefinition>()

        // Generate stock definitions from ALL normalized products, using numeric SKU as primary identifier
        NormalizedBambuData.getAllNormalizedProducts().forEach { normalizedProduct ->
            val stockDefinition = createStockDefinitionFromNormalizedProduct(normalizedProduct)
            stockDefinitions.add(stockDefinition)
        }

        // Add component defaults as stock definitions
        stockDefinitions.addAll(generateComponentStockDefinitions())

        Log.i(TAG, "Generated ${stockDefinitions.size} stock definitions from all normalized products and component defaults")
        return stockDefinitions
    }

    /**
     * Generate StockDefinition entities from component defaults
     */
    private fun generateComponentStockDefinitions(): List<StockDefinition> {
        val componentDefaults = generateComponentDefaults()
        return componentDefaults.map { (key, componentDefault) ->
            StockDefinition(
                id = "bambu_component_$key",
                label = componentDefault.name
            ).apply {
                sku = "component_$key"
                manufacturer = "Bambu Lab"
                displayName = componentDefault.name
                description = componentDefault.description
                category = componentDefault.category
                productUrl = "https://store.bambulab.com/"
                
                // Set weight using new Quantity system
                weight = ContinuousQuantity(componentDefault.massGrams.toDouble(), "g")
                
                // Usage characteristics - these are reusable packaging components
                consumable = false  // Spools and cores are reusable
                reusable = true
                recyclable = when(key) {
                    "spool_standard" -> true  // Plastic spool is recyclable
                    "core_cardboard" -> true  // Cardboard core is recyclable
                    else -> false
                }
                
                // Color properties
                colorName = when(key) {
                    "spool_standard" -> "Natural"
                    "core_cardboard" -> "Brown"
                    else -> "Default"
                }
                colorHex = when(key) {
                    "spool_standard" -> "#F5F5DC" // Beige/natural plastic color
                    "core_cardboard" -> "#8B4513" // Brown cardboard color
                    else -> "#808080"
                }
                colorCode = when(key) {
                    "spool_standard" -> "NAT"
                    "core_cardboard" -> "BRN"
                    else -> "DEF"
                }
                
                // No temperature properties for packaging components
                // price = 0.0 // Components are not sold separately (don't set if 0)
                available = true
                alternativeIds = emptySet()
            }
        }
    }


    /**
     * Create StockDefinition from normalized product using numeric SKU as primary identifier
     */
    private fun createStockDefinitionFromNormalizedProduct(
        normalizedProduct: NormalizedBambuData.NormalizedProduct
    ): StockDefinition {
        // Get complete product view for additional information
        val completeView = NormalizedBambuData.getCompleteProductView(normalizedProduct.sku)
            ?: throw IllegalStateException("No complete view found for SKU: ${normalizedProduct.sku}")
        
        val colorName = completeView.color.colorName
        val materialType = completeView.materialType
        val colorHex = ColorUtils.getHexColorForName(colorName)
        val colorCode = normalizedProduct.colorCode
        
        // Create alternative identifiers set including alphanumeric variant ID
        val alternativeIds = mutableSetOf<String>()
        
        // Add the alphanumeric variant ID as an alternative identifier
        val alphanumericVariantId = "${normalizedProduct.seriesCode}-${normalizedProduct.colorCode}"
        alternativeIds.add(alphanumericVariantId)
        
        // Add shopify variant ID if available
        normalizedProduct.shopifyVariantId?.let { alternativeIds.add(it) }
        
        // Add material+color identifier
        alternativeIds.add("${materialType}_${colorName}".replace(" ", "_"))
        
        return StockDefinition(
            id = "bambu_material_${normalizedProduct.sku}",
            label = "Bambu ${materialType} ${colorName}"
        ).apply {
            sku = normalizedProduct.sku // Use numeric SKU as primary identifier
            manufacturer = "Bambu Lab"
            displayName = "Bambu ${materialType} ${colorName}"
            description = "${materialType} filament in ${colorName}"
            category = "filament"
            productUrl = "https://bambulab.com/en/filament/${materialType.lowercase().replace(" ", "-")}"
            
            // Set weight using new Quantity system (1kg filament)
            weight = ContinuousQuantity(1000.0, "g")
            
            // Usage characteristics - materials are consumable
            consumable = true
            reusable = false
            recyclable = true
            
            // Material-specific properties
            this.materialType = materialType.replace(" ", "_").uppercase()
            this.colorName = colorName
            this.colorHex = colorHex
            this.colorCode = colorCode
            
            // Only set temperature properties if we have temperature profile data
            val temperatureProfile = getTemperatureProfileForMaterial(materialType.replace(" ", "_").uppercase())
            if (temperatureProfile != "generic_standard") {
                val tempData = NormalizedBambuData.temperatureProfiles[temperatureProfile]
                tempData?.let { temp ->
                    minNozzleTemp = temp.minNozzle
                    maxNozzleTemp = temp.maxNozzle
                    bedTemp = temp.bed
                    temp.enclosure?.let { enclosureTemp = it }
                }
            }
            // If no temperature data exists, don't set any temperature properties
            
            price = 29.99
            currency = "USD"
            available = true
            this.alternativeIds = alternativeIds
        }
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
