package com.bscan.interpreter

import com.bscan.model.*
import com.bscan.repository.MappingsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.junit.Before
import org.mockito.Mockito.*
import org.junit.Assert.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test BambuFormatInterpreter against real export data from user's JSON file
 */
class JsonExportInterpreterTest {
    
    private lateinit var interpreter: BambuFormatInterpreter
    private lateinit var mockMappingsRepository: MappingsRepository
    private lateinit var exportData: JsonExportData
    
    @Before
    fun setup() {
        // Load the actual JSON export file from resources
        val resourceStream = javaClass.classLoader.getResourceAsStream("bscan_export_20250824_184125.json")
        if (resourceStream == null) {
            println("JSON export file not found in resources - skipping test")
            return
        }
        
        val gson = Gson()
        val jsonContent = resourceStream.bufferedReader().use { it.readText() }
        exportData = gson.fromJson(jsonContent, JsonExportData::class.java)
        
        // Setup mock repository
        mockMappingsRepository = mock(MappingsRepository::class.java)
        
        // Mock empty mappings for fallback testing
        val emptyMappings = FilamentMappings.empty()
        `when`(mockMappingsRepository.getCurrentMappings()).thenReturn(emptyMappings)
        
        // Mock no RFID mappings found to test fallback behavior
        `when`(mockMappingsRepository.getRfidMappingByCode(anyString(), anyString())).thenReturn(null)
        
        interpreter = BambuFormatInterpreter(emptyMappings, mockMappingsRepository)
    }
    
    @Test
    fun testInterpretExportedScans() {
        if (!::exportData.isInitialized) {
            println("Export data not loaded - skipping test")
            return
        }
        
        var successfulInterpretations = 0
        var failedInterpretations = 0
        val interpretationResults = mutableListOf<InterpretationResult>()
        
        exportData.decryptedScans.forEachIndexed { index, scan ->
            println("\n=== Testing scan $index ===")
            
            // Convert JSON scan to DecryptedScanData
            val decryptedScanData = convertToDecryptedScanData(scan)
            
            // Test interpretation
            val result = interpreter.interpret(decryptedScanData)
            
            val interpretationResult = InterpretationResult(
                scanIndex = index,
                tagUid = decryptedScanData.tagUid,
                wasSuccessful = result != null,
                filamentType = result?.filamentType,
                detailedFilamentType = result?.detailedFilamentType,
                colorName = result?.colorName,
                colorHex = result?.colorHex,
                exactSku = result?.exactSku,
                rfidCode = result?.rfidCode,
                rawBlock2 = scan.decryptedBlocks["2"],
                rawBlock4 = scan.decryptedBlocks["4"]
            )
            
            interpretationResults.add(interpretationResult)
            
            if (result != null) {
                successfulInterpretations++
                println("✅ SUCCESS: ${result.filamentType} - ${result.detailedFilamentType} (${result.colorName})")
                println("   RFID Code: ${result.rfidCode}")
                println("   Exact SKU: ${result.exactSku ?: "None (will show question mark)"}")
            } else {
                failedInterpretations++
                println("❌ FAILED: Could not interpret scan")
                println("   Block 2 (material): ${scan.decryptedBlocks["2"]}")
                println("   Block 4 (detailed): ${scan.decryptedBlocks["4"]}")
            }
        }
        
        // Summary
        println("\n=== INTERPRETATION SUMMARY ===")
        println("Total scans: ${exportData.decryptedScans.size}")
        println("Successful interpretations: $successfulInterpretations")
        println("Failed interpretations: $failedInterpretations")
        println("Success rate: ${(successfulInterpretations * 100.0 / exportData.decryptedScans.size).toInt()}%")
        
        // Analyze failed interpretations
        val failedResults = interpretationResults.filter { !it.wasSuccessful }
        if (failedResults.isNotEmpty()) {
            println("\n=== FAILED INTERPRETATION ANALYSIS ===")
            failedResults.forEach { result ->
                println("Scan ${result.scanIndex}: Block2='${result.rawBlock2}', Block4='${result.rawBlock4}'")
            }
        }
        
        // Analyze tags that will show question marks (no exactSku)
        val questionMarkTags = interpretationResults.filter { it.wasSuccessful && it.exactSku == null }
        println("\n=== QUESTION MARK ANALYSIS ===")
        println("Tags that will show question marks: ${questionMarkTags.size}")
        questionMarkTags.take(10).forEach { result ->
            println("RFID Code: ${result.rfidCode} -> ${result.filamentType} ${result.detailedFilamentType}")
        }
        
        // Assert reasonable success rate
        val successRate = successfulInterpretations * 100.0 / exportData.decryptedScans.size
        assertTrue("Success rate should be at least 80%, got ${successRate.toInt()}%", successRate >= 80.0)
    }
    
    private fun convertToDecryptedScanData(scan: JsonDecryptedScan): DecryptedScanData {
        // Convert string keys to integer keys for blocks
        val intKeyBlocks = scan.decryptedBlocks.mapKeys { it.key.toInt() }
        
        // Convert string keys to integer keys for usedKeys
        val intKeyUsedKeys = scan.usedKeys?.mapKeys { it.key.toInt() } ?: emptyMap()
        
        return DecryptedScanData(
            timestamp = LocalDateTime.now(),
            tagUid = scan.tagUid ?: "UNKNOWN",
            technology = scan.technology ?: "MifareClassic",
            scanResult = ScanResult.SUCCESS,
            decryptedBlocks = intKeyBlocks,
            authenticatedSectors = scan.authenticatedSectors,
            failedSectors = scan.failedSectors ?: emptyList(),
            usedKeys = intKeyUsedKeys,
            derivedKeys = scan.derivedKeys ?: emptyList(),
            errors = scan.errors ?: emptyList(),
            keyDerivationTimeMs = scan.keyDerivationTimeMs ?: 0,
            authenticationTimeMs = scan.authenticationTimeMs ?: 0
        )
    }
}

/**
 * Data classes for JSON parsing
 */
data class JsonExportData(
    val decryptedScans: List<JsonDecryptedScan>
)

data class JsonDecryptedScan(
    val tagUid: String?,
    val technology: String?,
    val decryptedBlocks: Map<String, String>,
    val authenticatedSectors: List<Int>,
    val failedSectors: List<Int>?,
    val usedKeys: Map<String, String>?,
    val derivedKeys: List<String>?,
    val tagSizeBytes: Int?,
    val sectorCount: Int?,
    val errors: List<String>?,
    val keyDerivationTimeMs: Long?,
    val authenticationTimeMs: Long?
)

data class InterpretationResult(
    val scanIndex: Int,
    val tagUid: String,
    val wasSuccessful: Boolean,
    val filamentType: String?,
    val detailedFilamentType: String?,
    val colorName: String?,
    val colorHex: String?,
    val exactSku: String?,
    val rfidCode: String?,
    val rawBlock2: String?,
    val rawBlock4: String?
)