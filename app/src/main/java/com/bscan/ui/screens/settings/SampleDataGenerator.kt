package com.bscan.ui.screens.settings

import com.bscan.model.FilamentInfo
import com.bscan.model.ScanHistory
import com.bscan.model.ScanResult
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
                    
                    val scanHistory = ScanHistory(
                        uid = tagUid,
                        timestamp = scanTime,
                        technology = "MifareClassic",
                        scanResult = if (isSuccess) ScanResult.SUCCESS else ScanResult.AUTHENTICATION_FAILED,
                        filamentInfo = if (isSuccess) filamentInfo else null,
                        debugInfo = createSampleDebugInfo(tagUid, isSuccess)
                    )
                    
                    repository.saveScan(scanHistory)
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
    
    private fun createSampleDebugInfo(tagUid: String, isSuccess: Boolean): com.bscan.model.ScanDebugInfo {
        return com.bscan.model.ScanDebugInfo(
            uid = tagUid,
            tagSizeBytes = 1024,
            sectorCount = 16,
            authenticatedSectors = if (isSuccess) listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15) else listOf(0),
            failedSectors = if (isSuccess) emptyList() else listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            usedKeyTypes = if (isSuccess) {
                mapOf(
                    0 to "KeyA", 1 to "KeyB", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA",
                    5 to "KeyB", 6 to "KeyA", 7 to "KeyB", 8 to "KeyA", 9 to "KeyB",
                    10 to "KeyA", 11 to "KeyB", 12 to "KeyA", 13 to "KeyB", 14 to "KeyA", 15 to "KeyB"
                )
            } else {
                mapOf(0 to "KeyA")
            },
            blockData = if (isSuccess) {
                mapOf(
                    0 to tagUid,
                    1 to "00112233445566778899AABBCCDDEEFF",
                    2 to "FF00FF00FF00FF00FF00FF00FF00FF00"
                )
            } else {
                mapOf(0 to tagUid)
            },
            derivedKeys = listOf("A1B2C3D4E5F6", "123456789ABC"),
            rawColorBytes = "FF4444",
            errorMessages = if (isSuccess) emptyList() else listOf("Authentication failed for sectors 1-15"),
            parsingDetails = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "success" to isSuccess,
                "sampleData" to true
            )
        )
    }
}