package com.bscan.model

/**
 * Build-time catalog data that ships with the app.
 * Contains manufacturer definitions, product catalogs, and default mappings.
 * This data is read-only and loaded from assets.
 */
data class CatalogData(
    val manufacturers: Map<String, ManufacturerCatalog>
)

/**
 * Complete catalog information for a single manufacturer
 */
data class ManufacturerCatalog(
    val name: String,
    val displayName: String,
    val materials: Map<String, MaterialDefinition>,
    val temperatureProfiles: Map<String, TemperatureProfile>,
    val colorPalette: Map<String, String>,
    val rfidMappings: Map<String, RfidMapping>,
    val componentDefaults: Map<String, ComponentDefault>,
    val products: List<ProductEntry>,
    val tagFormat: TagFormat = TagFormat.PROPRIETARY
)

/**
 * Material definition with display name and properties
 */
data class MaterialDefinition(
    val displayName: String,
    val temperatureProfile: String,
    val properties: MaterialCatalogProperties = MaterialCatalogProperties()
)

/**
 * Physical and chemical properties of a material
 */
data class MaterialCatalogProperties(
    val density: Float? = null,           // g/cm³
    val shrinkage: Float? = null,         // percentage
    val biodegradable: Boolean = false,
    val foodSafe: Boolean = false,
    val flexible: Boolean = false,
    val support: Boolean = false,
    val category: MaterialCategory = MaterialCategory.THERMOPLASTIC
)

/**
 * Categories of 3D printing materials
 */
enum class MaterialCategory {
    THERMOPLASTIC,      // PLA, ABS, PETG
    ENGINEERING,        // PC, PA, PEI
    FLEXIBLE,           // TPU, TPE
    COMPOSITE,          // Carbon fiber, wood fill
    SUPPORT,            // PVA, HIPS breakaway
    SPECIALTY,          // Conductive, ceramic
    UNKNOWN             // Unrecognized materials
}

/**
 * Temperature profile for a material
 */
data class TemperatureProfile(
    val minNozzle: Int,
    val maxNozzle: Int,
    val bed: Int,
    val enclosure: Int? = null
)

/**
 * Default component definition for automatic setup
 */
data class ComponentDefault(
    val name: String,
    val category: String,
    val massGrams: Float,
    val description: String,
    val manufacturer: String? = null
)

