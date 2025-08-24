package com.bscan.ui.screens.settings

import com.bscan.model.FilamentInfo
import com.bscan.model.ScanResult
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.TagFormat
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
                        tagFormat = TagFormat.BAMBU_PROPRIETARY,
                        manufacturerName = "Bambu Lab",
                        encryptedData = ByteArray(1024) { Random.nextInt(256).toByte() },
                        tagSizeBytes = 1024,
                        sectorCount = 16,
                        scanDurationMs = Random.nextLong(1000, 5000)
                    )
                    
                    val decryptedData = DecryptedScanData(
                        timestamp = scanTime,
                        tagUid = tagUid,
                        technology = "MifareClassic",
                        tagFormat = TagFormat.BAMBU_PROPRIETARY,
                        manufacturerName = "Bambu Lab",
                        scanResult = if (isSuccess) ScanResult.SUCCESS else ScanResult.AUTHENTICATION_FAILED,
                        decryptedBlocks = if (isSuccess) createSampleBlocks(
                            filamentType = spec.third.first,
                            colorHex = spec.second,
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
    
    private fun createSampleBlocks(
        filamentType: String,
        colorHex: String,
        spoolWeight: Int,
        trayUid: String
    ): Map<Int, String> {
        // Convert color hex to 4 bytes (RGBA format)
        val colorBytes = colorHexToBytes(colorHex)
        val spoolWeightBytes = String.format("%04X", spoolWeight)
        
        // Create realistic block data that BambuFormatInterpreter can decode
        return mapOf(
            // Block 0: UID and manufacturer data
            0 to "00112233445566778899AABBCCDDEEFF",
            // Block 1: Material variant and ID
            1 to "50544700504C4100000000000000FF00",
            // Block 2: Filament type (16 bytes, null-terminated)
            2 to stringToHexBlock(filamentType, 16),
            // Block 4: Detailed filament type
            4 to stringToHexBlock("$filamentType Basic", 16),
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
}