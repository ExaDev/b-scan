package com.bscan.ui.screens.settings

import com.bscan.model.FilamentInfo
import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.repository.ScanHistoryRepository
import java.time.LocalDateTime
import kotlin.random.Random

class SampleDataGenerator {
    
    suspend fun generateSampleData(
        repository: ScanHistoryRepository,
        spoolCount: Int = 10,
        minScans: Int = 1,
        maxScans: Int = 10
    ) {
        val filamentSpecs = FilamentSpecsProvider.getFilamentSpecs()
        
        repeat(spoolCount) { spoolIndex ->
            val spec = filamentSpecs[spoolIndex % filamentSpecs.size]
            val trayUid = "TRAY${(spoolIndex + 1).toString().padStart(3, '0')}"
            val baseTagId = String.format("%08X", spoolIndex * 2 + 1000)
            
            // Randomly choose 1 or both tags for this spool
            val useBothTags = Random.nextBoolean()
            val tagsToGenerate = if (useBothTags) 2 else 1
            
            repeat(tagsToGenerate) { tagIndex ->
                val tagUid = String.format("%08X", baseTagId.toInt(16) + tagIndex)
                
                val filamentInfo = createSampleFilamentInfo(
                    tagUid = tagUid,
                    trayUid = trayUid,
                    colorName = spec.first,
                    colorHex = spec.second,
                    filamentType = spec.third.first,
                    detailedType = spec.third.second
                )
                
                // Generate scan history for this tag
                val scanCount = Random.nextInt(minScans, maxScans + 1)
                val successRate = Random.nextFloat() * 0.4f + 0.6f // 60-100% success rate
                val successCount = (scanCount * successRate).toInt().coerceAtLeast(1)
                
                repeat(scanCount) { scanIndex ->
                    val isSuccess = scanIndex < successCount
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
                        decryptedBlocks = if (isSuccess) createSampleBlocks() else emptyMap(),
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
            }
        }
    }
    
    private fun createSampleFilamentInfo(
        tagUid: String,
        trayUid: String,
        colorName: String,
        colorHex: String,
        filamentType: String,
        detailedType: String
    ): FilamentInfo {
        return FilamentInfo(
            tagUid = tagUid,
            trayUid = trayUid,
            filamentType = filamentType,
            detailedFilamentType = detailedType,
            colorHex = colorHex,
            colorName = colorName,
            spoolWeight = Random.nextInt(200, 1000),
            filamentDiameter = if (Random.nextBoolean()) 1.75f else 2.85f,
            filamentLength = Random.nextInt(100000, 500000),
            productionDate = "2024-${Random.nextInt(1, 13).toString().padStart(2, '0')}-${Random.nextInt(1, 29).toString().padStart(2, '0')}",
            minTemperature = Random.nextInt(180, 220),
            maxTemperature = Random.nextInt(220, 280),
            bedTemperature = Random.nextInt(50, 80),
            dryingTemperature = Random.nextInt(40, 70),
            dryingTime = Random.nextInt(4, 24)
        )
    }
    
    private fun createSampleBlocks(): Map<Int, String> {
        return mapOf(
            0 to "00112233445566778899AABBCCDDEEFF",
            1 to "FF00FF00FF00FF00FF00FF00FF00FF00",
            2 to "AA55AA55AA55AA55AA55AA55AA55AA55",
            4 to "1234567890ABCDEF1234567890ABCDEF"
        )
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
}