package com.bscan.ui.screens.settings

import com.bscan.model.FilamentInfo
import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.TagFormat
import com.bscan.model.BambuProduct
import com.bscan.repository.ScanHistoryRepository
import com.bscan.data.BambuProductDatabase
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Statistics about generated sample data
 */
data class GenerationStats(
    val totalScans: Int,
    val successfulScans: Int,
    val skusCovered: Int,
    val totalSpools: Int
) {
    val failedScans: Int get() = totalScans - successfulScans
    val successRate: Float get() = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
}

/**
 * Internal stats for scan generation per tag
 */
private data class TagScanStats(
    val total: Int,
    val successful: Int
)

class SampleDataGenerator {
    
    /**
     * Generate sample data with complete SKU coverage.
     * Ensures every SKU in the database gets at least one scan, plus additional random data.
     */
    suspend fun generateWithCompleteSkuCoverage(
        repository: ScanHistoryRepository,
        additionalRandomSpools: Int = 50,
        minScans: Int = 1,
        maxScans: Int = 5
    ): GenerationStats {
        val allProducts = BambuProductDatabase.getAllProducts()
        var trayCounter = 1
        var totalScansGenerated = 0
        var successfulScans = 0
        
        // PHASE 1: Ensure every SKU has at least one scan of at least one tag
        allProducts.forEachIndexed { productIndex, product ->
            val trayUid = "TRAY${trayCounter.toString().padStart(3, '0')}"
            val baseTagId = String.format("%08X", productIndex * 2 + 1000)
            trayCounter++
            
            // Each spool has exactly 2 tags, but we MIGHT have scanned both (less common)
            val scanBothTags = Random.nextFloat() < 0.3f // 30% chance of scanning both tags
            val tagsToScan = if (scanBothTags) 2 else 1
            
            repeat(tagsToScan) { tagIndex ->
                val tagUid = String.format("%08X", baseTagId.toInt(16) + tagIndex)
                
                // Random number of scans for this tag (at least 1 to guarantee coverage)
                val scanCount = Random.nextInt(minScans, maxScans + 1)
                val scansGenerated = generateScansForTag(repository, product, tagUid, trayUid, scanCount)
                totalScansGenerated += scansGenerated.total
                successfulScans += scansGenerated.successful
            }
        }
        
        // PHASE 2: Add additional random spools for variety
        repeat(additionalRandomSpools) { spoolIndex ->
            val randomProduct = allProducts[Random.nextInt(allProducts.size)]
            val trayUid = "TRAY${trayCounter.toString().padStart(3, '0')}"
            val baseTagId = String.format("%08X", (allProducts.size + spoolIndex) * 2 + 1000)
            trayCounter++
            
            // Each additional spool also has exactly 2 tags, we MIGHT scan both (less common)
            val scanBothTags = Random.nextFloat() < 0.3f // 30% chance of scanning both tags
            val tagsToScan = if (scanBothTags) 2 else 1
            
            repeat(tagsToScan) { tagIndex ->
                val tagUid = String.format("%08X", baseTagId.toInt(16) + tagIndex)
                val scanCount = Random.nextInt(minScans, maxScans + 1)
                val scansGenerated = generateScansForTag(repository, randomProduct, tagUid, trayUid, scanCount)
                totalScansGenerated += scansGenerated.total
                successfulScans += scansGenerated.successful
            }
        }
        
        return GenerationStats(
            totalScans = totalScansGenerated,
            successfulScans = successfulScans,
            skusCovered = allProducts.size,
            totalSpools = allProducts.size + additionalRandomSpools
        )
    }
    
    /**
     * Generate minimal data with exactly one scan per SKU
     */
    suspend fun generateMinimalCoverage(
        repository: ScanHistoryRepository
    ): GenerationStats {
        val allProducts = BambuProductDatabase.getAllProducts()
        var totalScansGenerated = 0
        var successfulScans = 0
        
        allProducts.forEachIndexed { productIndex, product ->
            val trayUid = "TRAY${(productIndex + 1).toString().padStart(3, '0')}"
            val tagUid = String.format("%08X", productIndex + 1000)
            
            // Generate exactly one successful scan per SKU
            val scansGenerated = generateScansForTag(repository, product, tagUid, trayUid, scanCount = 1, forceSuccess = true)
            totalScansGenerated += scansGenerated.total
            successfulScans += scansGenerated.successful
        }
        
        return GenerationStats(
            totalScans = totalScansGenerated,
            successfulScans = successfulScans,
            skusCovered = allProducts.size,
            totalSpools = allProducts.size
        )
    }
    
    /**
     * Original random sample generation (renamed for clarity)
     */
    suspend fun generateRandomSample(
        repository: ScanHistoryRepository,
        spoolCount: Int = 10,
        minScans: Int = 1,
        maxScans: Int = 10
    ): GenerationStats {
        var totalScansGenerated = 0
        var successfulScans = 0
        val bambuProducts = BambuProductDatabase.getAllProducts()
        
        repeat(spoolCount) { spoolIndex ->
            val product = bambuProducts[spoolIndex % bambuProducts.size]
            val trayUid = "TRAY${(spoolIndex + 1).toString().padStart(3, '0')}"
            val baseTagId = String.format("%08X", spoolIndex * 2 + 1000)
            
            // Each spool has exactly 2 tags, but we MIGHT have scanned both (less common)
            val scanBothTags = Random.nextFloat() < 0.3f // 30% chance of scanning both tags
            val tagsToScan = if (scanBothTags) 2 else 1
            
            repeat(tagsToScan) { tagIndex ->
                val tagUid = String.format("%08X", baseTagId.toInt(16) + tagIndex)
                
                val scanCount = Random.nextInt(minScans, maxScans + 1)
                val scansGenerated = generateScansForTag(repository, product, tagUid, trayUid, scanCount)
                totalScansGenerated += scansGenerated.total
                successfulScans += scansGenerated.successful
            }
        }
        
        return GenerationStats(
            totalScans = totalScansGenerated,
            successfulScans = successfulScans,
            skusCovered = minOf(spoolCount, bambuProducts.size),
            totalSpools = spoolCount
        )
    }
    
    /**
     * Backward compatibility method that delegates to generateRandomSample
     */
    suspend fun generateSampleData(
        repository: ScanHistoryRepository,
        spoolCount: Int = 10,
        minScans: Int = 1,
        maxScans: Int = 10
    ) {
        generateRandomSample(repository, spoolCount, minScans, maxScans)
    }
    
    /**
     * Helper method to generate scans for a single tag
     */
    private suspend fun generateScansForTag(
        repository: ScanHistoryRepository,
        product: BambuProduct,
        tagUid: String,
        trayUid: String,
        scanCount: Int,
        forceSuccess: Boolean = false
    ): TagScanStats {
        val successRate = if (forceSuccess) 1.0f else Random.nextFloat() * 0.4f + 0.6f // 60-100% success rate
        val successCount = if (forceSuccess) scanCount else (scanCount * successRate).toInt().coerceAtLeast(1)
        var actualSuccessCount = 0
        
        repeat(scanCount) { scanIndex ->
            val isSuccess = scanIndex < successCount
            if (isSuccess) actualSuccessCount++
            
            val scanTime = LocalDateTime.now().minusDays(Random.nextLong(0, 30))
            
            val encryptedData = EncryptedScanData(
                timestamp = scanTime,
                tagUid = tagUid,
                technology = "MifareClassic",
                encryptedData = ByteArray(1024) { Random.nextInt(256).toByte() },
                tagSizeBytes = 1024,
                sectorCount = 16,
                scanDurationMs = Random.nextLong(1000, 5000)
            )
            
            val decryptedData = DecryptedScanData(
                timestamp = scanTime,
                tagUid = tagUid,
                technology = "MifareClassic",
                scanResult = if (isSuccess) ScanResult.SUCCESS else ScanResult.AUTHENTICATION_FAILED,
                decryptedBlocks = if (isSuccess) createSampleBlocks(
                    product = product,
                    spoolWeight = Random.nextInt(200, 1000),
                    trayUid = trayUid
                ) else emptyMap(),
                authenticatedSectors = if (isSuccess) (0..15).toList() else emptyList(),
                failedSectors = if (isSuccess) emptyList() else (0..15).toList(),
                usedKeys = createSampleUsedKeys(isSuccess),
                derivedKeys = listOf("KEY1", "KEY2", "KEY3"),
                tagSizeBytes = 1024,
                sectorCount = 16,
                errors = if (isSuccess) emptyList() else listOf("Authentication failed"),
                keyDerivationTimeMs = Random.nextLong(100, 500),
                authenticationTimeMs = Random.nextLong(500, 2000)
            )
            
            repository.saveScan(encryptedData, decryptedData)
        }
        
        return TagScanStats(total = scanCount, successful = actualSuccessCount)
    }
    
    private fun createSampleFilamentInfo(
        tagUid: String,
        trayUid: String,
        product: BambuProduct
    ): FilamentInfo {
        return FilamentInfo(
            tagUid = tagUid,
            trayUid = trayUid,
            filamentType = product.productLine,
            detailedFilamentType = product.productLine,
            colorHex = product.colorHex,
            colorName = product.colorName,
            spoolWeight = if (product.mass == "1kg") 1000 else if (product.mass == "0.5kg") 500 else 1000,
            filamentDiameter = 1.75f, // Standard Bambu Lab diameter
            filamentLength = Random.nextInt(100000, 500000),
            productionDate = "2024-${Random.nextInt(1, 13).toString().padStart(2, '0')}-${Random.nextInt(1, 29).toString().padStart(2, '0')}",
            minTemperature = getDefaultMinTemp(product.productLine),
            maxTemperature = getDefaultMaxTemp(product.productLine),
            bedTemperature = getDefaultBedTemp(product.productLine),
            dryingTemperature = getDefaultDryingTemp(product.productLine),
            dryingTime = getDefaultDryingTime(product.productLine),
            bambuProduct = product // Include the product for purchase links
        )
    }
    
    private fun createSampleBlocks(
        product: BambuProduct,
        spoolWeight: Int,
        trayUid: String
    ): Map<Int, String> {
        // Convert color hex to 4 bytes (RGBA format)
        val colorBytes = colorHexToBytes(product.colorHex)
        val spoolWeightBytes = String.format("%04X", spoolWeight)
        
        // Create realistic block data that BambuFormatInterpreter can decode
        return mapOf(
            // Block 0: UID and manufacturer data
            0 to "00112233445566778899AABBCCDDEEFF",
            // Block 1: Material variant and ID
            1 to "50544700504C4100000000000000FF00",
            // Block 2: Filament type (16 bytes, null-terminated)
            2 to stringToHexBlock(product.productLine, 16),
            // Block 4: Detailed filament type
            4 to stringToHexBlock(product.productLine, 16),
            // Block 5: Color (4 bytes) + spool weight (2 bytes) + diameter (8 bytes)
            5 to "${colorBytes}${spoolWeightBytes}AE47E17A14AE0940",
            // Block 6: Temperature data (drying temp, time, bed temp, etc)
            6 to "003C000C00500019000000000000FF00",
            // Block 9: Tray UID as hex
            9 to stringToHexBlock(trayUid, 16),
            // Block 12: Production date
            12 to stringToHexBlock("2024-${Random.nextInt(1, 13).toString().padStart(2, '0')}", 16),
            // Block 14: Filament length
            14 to "0000${String.format("%04X", Random.nextInt(100000, 500000))}000000000000FF00"
        )
    }
    
    private fun colorHexToBytes(colorHex: String): String {
        val hex = colorHex.removePrefix("#")
        return if (hex.length >= 6) {
            hex.substring(0, 6).padEnd(8, '0') // RGB + alpha padding
        } else {
            "FF0000FF" // Default to red
        }
    }
    
    private fun stringToHexBlock(text: String, blockSize: Int): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val paddedBytes = bytes.take(blockSize).toByteArray()
        val result = ByteArray(blockSize)
        System.arraycopy(paddedBytes, 0, result, 0, paddedBytes.size)
        return result.joinToString("") { "%02X".format(it) }
    }
    
    private fun createSampleUsedKeys(isSuccess: Boolean): Map<Int, String> {
        return if (isSuccess) {
            mapOf(
                0 to "KeyA", 1 to "KeyB", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA",
                5 to "KeyB", 6 to "KeyA", 7 to "KeyB", 8 to "KeyA", 9 to "KeyB",
                10 to "KeyA", 11 to "KeyB", 12 to "KeyA", 13 to "KeyB", 14 to "KeyA", 15 to "KeyB"
            )
        } else {
            mapOf(0 to "KeyA")
        }
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