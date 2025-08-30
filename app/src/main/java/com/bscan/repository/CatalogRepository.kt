package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bscan.model.*
import com.bscan.data.bambu.BambuCatalogGenerator
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for accessing both build-time catalog data and user-created SKUs.
 * Build-time data is read-only and loaded from assets.
 * User-created SKUs are stored in SharedPreferences with full CRUD operations.
 * Contains manufacturer definitions, product catalogs, and default mappings.
 */
class CatalogRepository(private val context: Context) {
    
    private var cachedCatalog: CatalogData? = null
    private var cachedUserSkus: MutableMap<String, UserCatalogSku>? = null
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("user_catalog", Context.MODE_PRIVATE)
    
    // Custom LocalDateTime adapter for Gson
    private val localDateTimeAdapter = object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        override fun serialize(src: LocalDateTime?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src?.format(formatter))
        }
        
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LocalDateTime? {
            return try {
                json?.asString?.let { LocalDateTime.parse(it, formatter) }
            } catch (e: Exception) {
                LocalDateTime.now() // Fallback to current time
            }
        }
    }
    
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDateTime::class.java, localDateTimeAdapter)
        .setPrettyPrinting()
        .create()
    
    companion object {
        private const val TAG = "CatalogRepository"
        private const val USER_CATALOG_KEY = "user_catalog_skus_v1"
        private const val USER_CATALOG_STOCK_KEY = "user_catalog_stock_v1"
    }
    
    /**
     * Get the complete catalog data, generates runtime catalog from Bambu data
     */
    fun getCatalog(): CatalogData {
        if (cachedCatalog == null) {
            cachedCatalog = generateRuntimeCatalog()
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
     * Find a product by identifier across all manufacturers
     * Searches both primary variantId and alternative identifiers
     */
    fun findProductBySku(skuId: String): ProductEntry? {
        getCatalog().manufacturers.forEach { (_, catalog) ->
            val product = catalog.products.find { it.matchesIdentifier(skuId) }
            if (product != null) {
                return product
            }
        }
        return null
    }
    
    /**
     * Find a product by identifier for a specific manufacturer
     * Searches both primary variantId and alternative identifiers
     */
    fun findProductBySku(manufacturerId: String, skuId: String): ProductEntry? {
        return getProducts(manufacturerId).find { it.matchesIdentifier(skuId) }
    }

    /**
     * Force reload catalog (useful for testing)
     */
    fun reloadCatalog() {
        cachedCatalog = null
    }
    
    // === User-Created SKU Management ===
    
    /**
     * Create a new user-defined catalog SKU
     */
    suspend fun createUserSku(sku: UserCatalogSku): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userSkus = getUserSkusCache()
            
            if (userSkus.containsKey(sku.skuId)) {
                Result.failure(IllegalArgumentException("SKU ${sku.skuId} already exists"))
            } else {
                val timestampedSku = sku.copy(
                    createdAt = LocalDateTime.now(),
                    modifiedAt = LocalDateTime.now()
                )
                userSkus[sku.skuId] = timestampedSku
                saveUserSkusToPreferences(userSkus)
                
                Log.d(TAG, "Created user SKU: ${sku.skuId}")
                Result.success(sku.skuId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user SKU: ${sku.skuId}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update an existing user-defined catalog SKU
     */
    suspend fun updateUserSku(skuId: String, sku: UserCatalogSku): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userSkus = getUserSkusCache()
            
            if (!userSkus.containsKey(skuId)) {
                Result.failure(IllegalArgumentException("SKU $skuId not found"))
            } else {
                val existingSku = userSkus[skuId]!!
                val updatedSku = sku.copy(
                    skuId = skuId, // Preserve original ID
                    createdAt = existingSku.createdAt, // Preserve creation time
                    modifiedAt = LocalDateTime.now()
                )
                userSkus[skuId] = updatedSku
                saveUserSkusToPreferences(userSkus)
                
                Log.d(TAG, "Updated user SKU: $skuId")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user SKU: $skuId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a user-defined catalog SKU
     */
    suspend fun deleteUserSku(skuId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userSkus = getUserSkusCache()
            
            if (userSkus.remove(skuId) != null) {
                saveUserSkusToPreferences(userSkus)
                // Also remove any stock tracking for this SKU
                removeSkuStockTracking(skuId)
                
                Log.d(TAG, "Deleted user SKU: $skuId")
                Result.success(true)
            } else {
                Result.failure(IllegalArgumentException("SKU $skuId not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user SKU: $skuId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all user-created catalog SKUs
     */
    suspend fun getUserSkus(): List<UserCatalogSku> = withContext(Dispatchers.IO) {
        return@withContext getUserSkusCache().values.toList()
    }
    
    /**
     * Find a specific user-created catalog SKU
     */
    suspend fun findUserSku(skuId: String): UserCatalogSku? = withContext(Dispatchers.IO) {
        return@withContext getUserSkusCache()[skuId]
    }
    
    /**
     * Get all products (both build-time and user-created) for a manufacturer
     */
    suspend fun getAllProducts(manufacturerId: String): List<CombinedProductInfo> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CombinedProductInfo>()
        
        // Add build-time catalog products
        getProducts(manufacturerId).forEach { product ->
            results.add(CombinedProductInfo(
                productEntry = product,
                userSku = null,
                source = ProductSource.BUILD_TIME_CATALOG,
                stockInfo = getSkuStockInfo("catalog_${product.variantId}")
            ))
        }
        
        // Add user-created SKUs for this manufacturer
        getUserSkus().filter { it.manufacturerId == manufacturerId }.forEach { userSku ->
            results.add(CombinedProductInfo(
                productEntry = null,
                userSku = userSku,
                source = ProductSource.USER_CREATED,
                stockInfo = getSkuStockInfo(userSku.skuId)
            ))
        }
        
        return@withContext results.sortedBy { it.getDisplayName() }
    }
    
    /**
     * Enhanced search across both build-time and user catalogs with priority system
     */
    suspend fun searchProducts(
        manufacturerId: String? = null,
        hex: String? = null,
        materialType: String? = null,
        query: String? = null
    ): List<CombinedProductInfo> = withContext(Dispatchers.IO) {
        val userResults = mutableListOf<CombinedProductInfo>()
        val catalogResults = mutableListOf<CombinedProductInfo>()
        
        // Search user-created SKUs (higher priority)
        getUserSkus().forEach { userSku ->
            if (matchesCriteria(userSku, manufacturerId, hex, materialType, query)) {
                userResults.add(CombinedProductInfo(
                    productEntry = null,
                    userSku = userSku,
                    source = ProductSource.USER_CREATED,
                    stockInfo = getSkuStockInfo(userSku.skuId)
                ))
            }
        }
        
        // Search build-time catalog products
        if (manufacturerId != null) {
            findProducts(manufacturerId, hex, materialType).forEach { product ->
                if (query == null || matchesQuery(product, query)) {
                    catalogResults.add(CombinedProductInfo(
                        productEntry = product,
                        userSku = null,
                        source = ProductSource.BUILD_TIME_CATALOG,
                        stockInfo = getSkuStockInfo("catalog_${product.variantId}")
                    ))
                }
            }
        } else {
            // Search all manufacturers if none specified
            getCatalog().manufacturers.forEach { (mfgId, _) ->
                findProducts(mfgId, hex, materialType).forEach { product ->
                    if (query == null || matchesQuery(product, query)) {
                        catalogResults.add(CombinedProductInfo(
                            productEntry = product,
                            userSku = null,
                            source = ProductSource.BUILD_TIME_CATALOG,
                            stockInfo = getSkuStockInfo("catalog_${product.variantId}")
                        ))
                    }
                }
            }
        }
        
        // Return user results first (priority), then catalog results
        return@withContext userResults + catalogResults
    }
    
    // === Stock Tracking Integration ===
    
    /**
     * Update stock levels for a SKU based on component instances
     */
    suspend fun updateSkuStock(skuId: String, componentInstances: List<String>, totalQuantity: Int, consumedQuantity: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val stockMap = getSkuStockCache()
            stockMap[skuId] = SkuStockInfo(
                skuId = skuId,
                totalQuantity = totalQuantity,
                consumedQuantity = consumedQuantity,
                componentInstances = componentInstances.toSet(),
                lastUpdated = LocalDateTime.now()
            )
            saveSkuStockToPreferences(stockMap)
            
            Log.d(TAG, "Updated stock for SKU $skuId: total=$totalQuantity, consumed=$consumedQuantity")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update stock for SKU: $skuId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get stock information for a specific SKU
     */
    suspend fun getSkuStockInfo(skuId: String): SkuStockInfo? = withContext(Dispatchers.IO) {
        return@withContext getSkuStockCache()[skuId]
    }
    
    /**
     * Get stock information for all SKUs
     */
    suspend fun getAllSkuStock(): Map<String, SkuStockInfo> = withContext(Dispatchers.IO) {
        return@withContext getSkuStockCache().toMap()
    }
    
    /**
     * Link a component instance to a catalog SKU for stock tracking
     */
    suspend fun linkComponentToSku(componentId: String, skuId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val stockMap = getSkuStockCache()
            val existingStock = stockMap[skuId] ?: SkuStockInfo(
                skuId = skuId,
                totalQuantity = 0,
                consumedQuantity = 0,
                componentInstances = emptySet(),
                lastUpdated = LocalDateTime.now()
            )
            
            val updatedInstances = existingStock.componentInstances + componentId
            stockMap[skuId] = existingStock.copy(
                componentInstances = updatedInstances,
                totalQuantity = updatedInstances.size,
                lastUpdated = LocalDateTime.now()
            )
            saveSkuStockToPreferences(stockMap)
            
            Log.d(TAG, "Linked component $componentId to SKU $skuId")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to link component $componentId to SKU $skuId", e)
            Result.failure(e)
        }
    }
    
    // === Import/Export Support ===
    
    /**
     * Export user catalog to JSON format
     */
    suspend fun exportUserCatalog(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val userSkus = getUserSkus()
            val stockInfo = getAllSkuStock()
            
            val exportData = UserCatalogExport(
                version = 1,
                exportedAt = LocalDateTime.now(),
                userSkus = userSkus,
                stockInfo = stockInfo.values.toList()
            )
            
            val json = gson.toJson(exportData)
            Log.d(TAG, "Exported ${userSkus.size} user SKUs and ${stockInfo.size} stock entries")
            Result.success(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export user catalog", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import user catalog from JSON with validation and merge options
     */
    suspend fun importUserCatalog(json: String, mergeMode: CatalogMergeMode = CatalogMergeMode.MERGE_WITH_EXISTING): Result<CatalogImportResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val importData = gson.fromJson(json, UserCatalogExport::class.java)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid catalog format"))
            
            val existingSkus = getUserSkusCache()
            val existingStock = getSkuStockCache()
            var addedSkus = 0
            var updatedSkus = 0
            var skippedSkus = 0
            var addedStock = 0
            
            when (mergeMode) {
                CatalogMergeMode.MERGE_WITH_EXISTING -> {
                    // Merge user SKUs
                    importData.userSkus.forEach { importedSku ->
                        if (existingSkus.containsKey(importedSku.skuId)) {
                            updatedSkus++
                        } else {
                            addedSkus++
                        }
                        existingSkus[importedSku.skuId] = importedSku.copy(modifiedAt = LocalDateTime.now())
                    }
                    
                    // Merge stock info
                    importData.stockInfo.forEach { stockInfo ->
                        if (!existingStock.containsKey(stockInfo.skuId)) {
                            addedStock++
                        }
                        existingStock[stockInfo.skuId] = stockInfo.copy(lastUpdated = LocalDateTime.now())
                    }
                }
                
                CatalogMergeMode.REPLACE_EXISTING -> {
                    existingSkus.clear()
                    existingStock.clear()
                    
                    importData.userSkus.forEach { sku ->
                        existingSkus[sku.skuId] = sku
                        addedSkus++
                    }
                    
                    importData.stockInfo.forEach { stock ->
                        existingStock[stock.skuId] = stock
                        addedStock++
                    }
                }
                
                CatalogMergeMode.PREVIEW_ONLY -> {
                    // Count what would be changed without modifying
                    importData.userSkus.forEach { importedSku ->
                        if (existingSkus.containsKey(importedSku.skuId)) {
                            updatedSkus++
                        } else {
                            addedSkus++
                        }
                    }
                    addedStock = importData.stockInfo.count { !existingStock.containsKey(it.skuId) }
                    
                    // Don't save changes in preview mode
                    return@withContext Result.success(CatalogImportResult(
                        addedSkus = addedSkus,
                        updatedSkus = updatedSkus,
                        skippedSkus = skippedSkus,
                        addedStock = addedStock,
                        isPreview = true
                    ))
                }
            }
            
            saveUserSkusToPreferences(existingSkus)
            saveSkuStockToPreferences(existingStock)
            
            Log.i(TAG, "Imported user catalog: added=$addedSkus, updated=$updatedSkus, stock=$addedStock")
            Result.success(CatalogImportResult(
                addedSkus = addedSkus,
                updatedSkus = updatedSkus,
                skippedSkus = skippedSkus,
                addedStock = addedStock,
                isPreview = false
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import user catalog", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear user catalog cache (for testing or after imports)
     */
    fun clearUserCatalogCache() {
        cachedUserSkus = null
    }
    
    /**
     * Generate runtime catalog from bambu data structures
     */
    private fun generateRuntimeCatalog(): CatalogData {
        Log.i(TAG, "Generating runtime catalog from Bambu data")
        return BambuCatalogGenerator.generateCatalogData()
    }
    
    // === Private Helper Methods ===
    
    private fun getUserSkusCache(): MutableMap<String, UserCatalogSku> {
        if (cachedUserSkus == null) {
            cachedUserSkus = loadUserSkusFromPreferences()
        }
        return cachedUserSkus!!
    }
    
    private fun loadUserSkusFromPreferences(): MutableMap<String, UserCatalogSku> {
        val savedJson = sharedPreferences.getString(USER_CATALOG_KEY, null)
        return if (savedJson != null) {
            try {
                val type = object : TypeToken<Map<String, UserCatalogSku>>() {}.type
                gson.fromJson<Map<String, UserCatalogSku>>(savedJson, type)?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load user SKUs, using empty map", e)
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }
    
    private fun saveUserSkusToPreferences(userSkus: Map<String, UserCatalogSku>) {
        try {
            val json = gson.toJson(userSkus)
            sharedPreferences.edit()
                .putString(USER_CATALOG_KEY, json)
                .apply()
            Log.d(TAG, "Saved ${userSkus.size} user SKUs to preferences")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user SKUs", e)
        }
    }
    
    private fun getSkuStockCache(): MutableMap<String, SkuStockInfo> {
        val savedJson = sharedPreferences.getString(USER_CATALOG_STOCK_KEY, null)
        return if (savedJson != null) {
            try {
                val type = object : TypeToken<Map<String, SkuStockInfo>>() {}.type
                gson.fromJson<Map<String, SkuStockInfo>>(savedJson, type)?.toMutableMap() ?: mutableMapOf()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load SKU stock info, using empty map", e)
                mutableMapOf()
            }
        } else {
            mutableMapOf()
        }
    }
    
    private fun saveSkuStockToPreferences(stockMap: Map<String, SkuStockInfo>) {
        try {
            val json = gson.toJson(stockMap)
            sharedPreferences.edit()
                .putString(USER_CATALOG_STOCK_KEY, json)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save SKU stock info", e)
        }
    }
    
    private fun removeSkuStockTracking(skuId: String) {
        val stockMap = getSkuStockCache()
        if (stockMap.remove(skuId) != null) {
            saveSkuStockToPreferences(stockMap)
        }
    }
    
    private fun matchesCriteria(
        userSku: UserCatalogSku,
        manufacturerId: String?,
        hex: String?,
        materialType: String?,
        query: String?
    ): Boolean {
        val manufacturerMatches = manufacturerId?.let { userSku.manufacturerId == it } ?: true
        val hexMatches = hex?.let { userSku.colorHex?.equals(it, ignoreCase = true) == true } ?: true
        val materialMatches = materialType?.let { 
            userSku.materialType.contains(it, ignoreCase = true) ||
            userSku.materialType.split("_").first().equals(it, ignoreCase = true)
        } ?: true
        val queryMatches = query?.let { 
            userSku.name.contains(it, ignoreCase = true) ||
            userSku.colorName.contains(it, ignoreCase = true) ||
            userSku.materialType.contains(it, ignoreCase = true)
        } ?: true
        
        return manufacturerMatches && hexMatches && materialMatches && queryMatches
    }
    
    private fun matchesQuery(product: ProductEntry, query: String): Boolean {
        return product.productName.contains(query, ignoreCase = true) ||
               product.colorName.contains(query, ignoreCase = true) ||
               product.materialType.contains(query, ignoreCase = true)
    }
}