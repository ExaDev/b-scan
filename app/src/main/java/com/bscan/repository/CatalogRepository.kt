package com.bscan.repository

import android.content.Context
import android.util.Log
import com.bscan.model.*
import com.google.gson.*
import java.io.IOException

/**
 * Repository for accessing build-time catalog data.
 * This data is read-only and loaded from assets.
 * Contains manufacturer definitions, product catalogs, and default mappings.
 */
class CatalogRepository(private val context: Context) {
    
    private var cachedCatalog: CatalogData? = null
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    companion object {
        private const val TAG = "CatalogRepository"
        private const val CATALOG_ASSET_PATH = "catalog_data.json"
    }
    
    /**
     * Get the complete catalog data, loading from assets on first access
     */
    fun getCatalog(): CatalogData {
        if (cachedCatalog == null) {
            cachedCatalog = loadFromAssets()
        }
        return cachedCatalog!!
    }
    
    /**
     * Get a specific manufacturer catalog by ID
     */
    fun getManufacturer(manufacturerId: String): ManufacturerCatalog? {
        return getCatalog().manufacturers[manufacturerId]
    }
    
    /**
     * Get all available manufacturers
     */
    fun getManufacturers(): Map<String, ManufacturerCatalog> {
        return getCatalog().manufacturers
    }
    
    /**
     * Find RFID mapping across all manufacturers
     */
    fun findRfidMapping(rfidCode: String): Pair<String, RfidMapping>? {
        getCatalog().manufacturers.forEach { (manufacturerId, catalog) ->
            catalog.rfidMappings[rfidCode]?.let { mapping ->
                return manufacturerId to mapping
            }
        }
        return null
    }
    
    /**
     * Find RFID mapping for a specific manufacturer
     */
    fun findRfidMapping(manufacturerId: String, rfidCode: String): RfidMapping? {
        return getManufacturer(manufacturerId)?.rfidMappings?.get(rfidCode)
    }
    
    /**
     * Get component default for a manufacturer
     */
    fun getComponentDefault(manufacturerId: String, componentKey: String): ComponentDefault? {
        return getManufacturer(manufacturerId)?.componentDefaults?.get(componentKey)
    }
    
    /**
     * Get all component defaults for a manufacturer
     */
    fun getComponentDefaults(manufacturerId: String): Map<String, ComponentDefault> {
        return getManufacturer(manufacturerId)?.componentDefaults ?: emptyMap()
    }
    
    /**
     * Get material definition for a manufacturer
     */
    fun getMaterial(manufacturerId: String, materialId: String): MaterialDefinition? {
        return getManufacturer(manufacturerId)?.materials?.get(materialId)
    }
    
    /**
     * Get temperature profile for a manufacturer
     */
    fun getTemperatureProfile(manufacturerId: String, profileId: String): TemperatureProfile? {
        return getManufacturer(manufacturerId)?.temperatureProfiles?.get(profileId)
    }
    
    /**
     * Get color name from manufacturer's color palette
     */
    fun getColorName(manufacturerId: String, hex: String): String? {
        return getManufacturer(manufacturerId)?.colorPalette?.get(hex)
    }
    
    /**
     * Find manufacturers that support a specific tag format
     */
    fun getManufacturersByTagFormat(tagFormat: TagFormat): List<Pair<String, ManufacturerCatalog>> {
        return getCatalog().manufacturers.filter { (_, catalog) ->
            catalog.tagFormat == tagFormat
        }.toList()
    }
    
    /**
     * Check if a manufacturer exists in the catalog
     */
    fun hasManufacturer(manufacturerId: String): Boolean {
        return getCatalog().manufacturers.containsKey(manufacturerId)
    }
    
    /**
     * Get all products for a manufacturer from catalog only
     */
    fun getProducts(manufacturerId: String): List<ProductEntry> {
        return getManufacturer(manufacturerId)?.products ?: emptyList()
    }
    
    /**
     * Find products by color and material for a manufacturer
     */
    fun findProducts(
        manufacturerId: String,
        hex: String? = null,
        materialType: String? = null
    ): List<ProductEntry> {
        val products = getProducts(manufacturerId)
        return products.filter { product ->
            val hexMatches = hex?.let { product.colorHex?.equals(it, ignoreCase = true) } ?: true
            val materialMatches = materialType?.let { product.materialType.equals(it, ignoreCase = true) } ?: true
            hexMatches && materialMatches
        }
    }
    
    /**
     * Find best product match by material type and color name (deprecated - use UnifiedDataAccess)
     * @deprecated Use UnifiedDataAccess.findBestProductMatch() instead
     */
    @Deprecated("Use UnifiedDataAccess.findBestProductMatch() instead")
    fun findBestProductMatch(filamentType: String, colorName: String): ProductEntry? {
        // This method is kept for backward compatibility but should not be used
        // Product data is now managed through UnifiedDataAccess
        return null
    }
    
    /**
     * Get current mappings in legacy format (deprecated - use UnifiedDataAccess)
     * @deprecated Use UnifiedDataAccess for product data and CatalogRepository for catalog metadata
     */
    @Deprecated("Use UnifiedDataAccess for product data")
    fun getCurrentMappings(): FilamentMappings {
        val catalog = getCatalog()
        
        // Convert to legacy format - this is a simplified conversion
        // Note: This no longer includes product data as that's handled by UnifiedDataAccess
        val materialMappings = mutableMapOf<String, String>()
        val brandMappings = mutableMapOf<String, String>()
        
        catalog.manufacturers.forEach { (manufacturerId, manufacturer) ->
            // Add brand mapping
            brandMappings[manufacturerId] = manufacturer.displayName
            
            // Add material mappings
            manufacturer.materials.forEach { (materialId, material) ->
                materialMappings[materialId] = material.displayName
            }
        }
        
        return FilamentMappings(
            productCatalog = emptyList(), // Products now come from UnifiedDataAccess
            materialMappings = materialMappings,
            brandMappings = brandMappings,
            version = catalog.version
        )
    }

    /**
     * Force reload catalog from assets (useful for testing)
     */
    fun reloadCatalog() {
        cachedCatalog = null
    }
    
    private fun loadFromAssets(): CatalogData {
        return try {
            Log.d(TAG, "Loading catalog from assets: $CATALOG_ASSET_PATH")
            
            val assetsInputStream = context.assets.open(CATALOG_ASSET_PATH)
            val jsonString = assetsInputStream.bufferedReader().use { it.readText() }
            
            val catalog = gson.fromJson(jsonString, CatalogData::class.java)
            if (catalog != null) {
                Log.i(TAG, "Loaded catalog with ${catalog.manufacturers.size} manufacturers")
                catalog
            } else {
                Log.w(TAG, "Catalog data is null, using empty catalog")
                createEmptyCatalog()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load catalog from assets", e)
            createEmptyCatalog()
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Invalid catalog JSON format", e)
            createEmptyCatalog()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading catalog", e)
            createEmptyCatalog()
        }
    }
    
    private fun createEmptyCatalog(): CatalogData {
        Log.i(TAG, "Creating empty catalog as fallback")
        return CatalogData(
            version = 1,
            manufacturers = emptyMap()
        )
    }
}