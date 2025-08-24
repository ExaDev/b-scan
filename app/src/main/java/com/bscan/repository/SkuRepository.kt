package com.bscan.repository

import android.content.Context
import com.bscan.data.BambuProductDatabase
import com.bscan.model.BambuProduct
import com.bscan.model.FilamentInfo
import com.bscan.model.ScanResult
import com.bscan.ui.screens.home.SkuInfo
import java.time.LocalDateTime

class SkuRepository(private val context: Context) {
    
    private val scanHistoryRepository by lazy { ScanHistoryRepository(context) }
    
    /**
     * Get all SKUs from BambuProductDatabase merged with scan history data
     * - All Bambu Lab products exist as SKUs from the start
     * - Scanned products show actual spool count and scan statistics
     * - Unscanned products show spoolCount = 0 (available but not owned)
     */
    fun getAllSkus(): List<SkuInfo> {
        val allScans = scanHistoryRepository.getAllScans()
        
        // Group scans by SKU (detailed filament type + color combination)
        val scansBySkuKey = allScans
            .filter { it.filamentInfo != null }
            .groupBy { "${it.filamentInfo!!.detailedFilamentType}-${it.filamentInfo.colorName}" }
        
        // Get all products from BambuProductDatabase
        val allProducts = BambuProductDatabase.getAllProducts()
        val allSkus = mutableListOf<SkuInfo>()
        
        // Create SkuInfo for all products from database, merging with scan data where available
        val databaseSkus = allProducts.map { product ->
            val skuKey = "${product.productLine}-${product.colorName}"
            val scansForThisSku = scansBySkuKey[skuKey] ?: emptyList()
            
            if (scansForThisSku.isNotEmpty()) {
                // Has scan history - calculate statistics from actual scans
                val uniqueSpools = scansForThisSku.groupBy { it.filamentInfo!!.trayUid }.size
                val totalScans = scansForThisSku.size
                val successfulScans = scansForThisSku.count { it.scanResult == ScanResult.SUCCESS }
                val lastScanned = scansForThisSku.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
                val successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
                
                // Use FilamentInfo from the most recent successful scan or fallback to product data
                val mostRecentScan = scansForThisSku.maxByOrNull { it.timestamp }
                val filamentInfo = mostRecentScan?.filamentInfo ?: createFilamentInfoForUnscannedProduct(product)
                
                SkuInfo(
                    skuKey = skuKey,
                    filamentInfo = filamentInfo,
                    spoolCount = uniqueSpools,
                    totalScans = totalScans,
                    successfulScans = successfulScans,
                    lastScanned = lastScanned,
                    successRate = successRate,
                    isScannedOnly = false // Database products are not scanned-only
                )
            } else {
                // Unscanned product - create SKU entry with zero statistics
                val filamentInfo = createFilamentInfoForUnscannedProduct(product)
                
                SkuInfo(
                    skuKey = skuKey,
                    filamentInfo = filamentInfo,
                    spoolCount = 0, // Available but not owned
                    totalScans = 0,
                    successfulScans = 0,
                    lastScanned = LocalDateTime.of(1970, 1, 1, 0, 0), // Epoch time for unscanned
                    successRate = 0f,
                    isScannedOnly = false // Database products are not scanned-only
                )
            }
        }
        allSkus.addAll(databaseSkus)
        
        // Get all product database SKU keys to identify scanned-only SKUs
        val databaseSkuKeys = allProducts.map { "${it.productLine}-${it.colorName}" }.toSet()
        
        // Create SkuInfo entries for scanned products that don't exist in the database
        val scannedOnlySkus = scansBySkuKey
            .filterKeys { skuKey -> skuKey !in databaseSkuKeys } // Only scanned SKUs not in database
            .map { (skuKey, scans) ->
                val uniqueSpools = scans.groupBy { it.filamentInfo!!.trayUid }.size
                val totalScans = scans.size
                val successfulScans = scans.count { it.scanResult == ScanResult.SUCCESS }
                val lastScanned = scans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
                val successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
                
                // Use FilamentInfo from the most recent successful scan
                val mostRecentScan = scans.maxByOrNull { it.timestamp }
                val filamentInfo = mostRecentScan?.filamentInfo ?: return@map null
                
                SkuInfo(
                    skuKey = skuKey,
                    filamentInfo = filamentInfo,
                    spoolCount = uniqueSpools,
                    totalScans = totalScans,
                    successfulScans = successfulScans,
                    lastScanned = lastScanned,
                    successRate = successRate,
                    isScannedOnly = true // This is a scanned-only product (not in catalog)
                )
            }
            .filterNotNull()
        allSkus.addAll(scannedOnlySkus)
        
        return allSkus.sortedBy { it.filamentInfo.filamentType + it.filamentInfo.colorName }
    }
    
    /**
     * Get SKUs filtered by minimum spool count
     */
    fun getSkusFilteredBySpoolCount(minSpoolCount: Int = 1): List<SkuInfo> {
        return getAllSkus().filter { it.spoolCount >= minSpoolCount }
    }
    
    /**
     * Create FilamentInfo from BambuProduct for unscanned products
     */
    private fun createFilamentInfoForUnscannedProduct(product: BambuProduct): FilamentInfo {
        return FilamentInfo(
            tagUid = "", // No tag UID until scanned
            trayUid = "", // No tray UID until scanned
            filamentType = product.productLine,
            detailedFilamentType = product.productLine,
            colorHex = product.colorHex,
            colorName = product.colorName,
            spoolWeight = if (product.mass == "1kg") 1000 else if (product.mass == "0.5kg") 500 else 1000,
            filamentDiameter = 1.75f, // Standard filament diameter
            filamentLength = 0, // Unknown until scanned
            productionDate = "", // Unknown until scanned
            minTemperature = getDefaultMinTemp(product.productLine),
            maxTemperature = getDefaultMaxTemp(product.productLine),
            bedTemperature = getDefaultBedTemp(product.productLine),
            dryingTemperature = getDefaultDryingTemp(product.productLine),
            dryingTime = getDefaultDryingTime(product.productLine),
            bambuProduct = product
        )
    }
    
    /**
     * Get default printing temperatures based on material type
     */
    private fun getDefaultMinTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 190
        materialType.contains("ABS") -> 220
        materialType.contains("PETG") -> 220
        materialType.contains("TPU") -> 200
        else -> 190
    }
    
    private fun getDefaultMaxTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 220
        materialType.contains("ABS") -> 250
        materialType.contains("PETG") -> 250
        materialType.contains("TPU") -> 230
        else -> 220
    }
    
    private fun getDefaultBedTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 60
        materialType.contains("ABS") -> 80
        materialType.contains("PETG") -> 70
        materialType.contains("TPU") -> 50
        else -> 60
    }
    
    private fun getDefaultDryingTemp(materialType: String): Int = when {
        materialType.contains("PLA") -> 45
        materialType.contains("ABS") -> 60
        materialType.contains("PETG") -> 65
        materialType.contains("TPU") -> 40
        else -> 45
    }
    
    private fun getDefaultDryingTime(materialType: String): Int = when {
        materialType.contains("TPU") -> 12
        materialType.contains("PETG") -> 8
        materialType.contains("ABS") -> 4
        else -> 6
    }
}

