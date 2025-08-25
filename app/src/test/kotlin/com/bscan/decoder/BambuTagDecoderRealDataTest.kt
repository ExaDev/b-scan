package com.bscan.decoder

import com.bscan.utils.RfidTestDataLoader
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull

/**
 * Test BambuTagDecoder against real RFID tag data from submodule.
 * 
 * Tests decoder against 775+ real Bambu Lab RFID tag dumps to ensure
 * comprehensive compatibility with all material types and formats.
 */
@RunWith(Parameterized::class)
class BambuTagDecoderRealDataTest(
    private val testFile: java.io.File,
    private val fileFormat: String,
    private val materialInfo: com.bscan.utils.MaterialInfo
) {
    
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} - {index}")
        fun data(): Collection<Array<Any>> {
            return loadTestData()
        }
        
        private fun loadTestData(): List<Array<Any>> {
            val testData = mutableListOf<Array<Any>>()
            
            // Load ALL dump files for comprehensive testing
            val allFiles = RfidTestDataLoader.loadAllDumpFiles()
            allFiles.forEach { dumpFile ->
                try {
                    val materialInfo = RfidTestDataLoader.extractMaterialInfo(dumpFile)
                    val format = when {
                        dumpFile.name.endsWith("-dump.json") -> "JSON"
                        dumpFile.name.endsWith(".bin") -> "BIN"
                        else -> "UNKNOWN"
                    }
                    testData.add(arrayOf(dumpFile, format, materialInfo))
                } catch (e: Exception) {
                    println("Skipping file: ${dumpFile.path} - ${e.message}")
                }
            }
            
            if (testData.isEmpty()) {
                throw IllegalStateException("No test data loaded! Check RFID library submodule.")
            }
            
            println("Loaded ${testData.size} real RFID tag test cases:")
            testData.groupBy { it[1] as String }.forEach { (format, files) ->
                println("  - $format: ${files.size} files")
            }
            testData.groupBy { (it[2] as com.bscan.utils.MaterialInfo).category }.forEach { (category, files) ->
                println("  - $category: ${files.size} tags")
            }
            
            return testData
        }
    }
    
    @Test
    fun testRealTagDecoding() {
        println("Testing $fileFormat: ${materialInfo.category}/${materialInfo.material}/${materialInfo.color}")
        
        try {
            // Parse dump file
            val dumpData = RfidTestDataLoader.parseDumpFile(testFile)
            val tagData = RfidTestDataLoader.convertDumpToByteArray(dumpData)
            
            // Create NfcTagData for decoder
            val nfcTagData = NfcTagData(
                uid = dumpData.Card.UID,
                bytes = tagData,
                technology = "MifareClassic"
            )
            
            // Test decoder
            val filamentInfo = BambuTagDecoder.parseTagDetails(nfcTagData)
            
            // Basic validation
            assertNotNull("Failed to decode $fileFormat file ${testFile.name}", filamentInfo)
            
            if (filamentInfo != null) {
                // Validate essential fields
                assertTrue("Missing tray UID in ${testFile.name}", filamentInfo.trayUid.isNotEmpty())
                assertTrue("Missing filament type in ${testFile.name}", filamentInfo.filamentType.isNotEmpty())
                assertTrue("Missing color name in ${testFile.name}", filamentInfo.colorName.isNotEmpty())
                
                // Material category validation
                when (materialInfo.category.uppercase()) {
                    "PLA" -> assertTrue("Expected PLA material, got: ${filamentInfo.filamentType}", 
                        filamentInfo.filamentType.contains("PLA", ignoreCase = true))
                    "PETG" -> assertTrue("Expected PETG material, got: ${filamentInfo.filamentType}", 
                        filamentInfo.filamentType.contains("PETG", ignoreCase = true))
                    "ABS" -> assertTrue("Expected ABS material, got: ${filamentInfo.filamentType}", 
                        filamentInfo.filamentType.contains("ABS", ignoreCase = true))
                    "ASA" -> assertTrue("Expected ASA material, got: ${filamentInfo.filamentType}", 
                        filamentInfo.filamentType.contains("ASA", ignoreCase = true))
                    "SUPPORT MATERIAL" -> {
                        val supportTypes = listOf("SUPPORT", "PVA", "HIPS", "PLA-S", "ABS-S", "PETG-S")
                        val hasValidSupportType = supportTypes.any { 
                            filamentInfo.filamentType.contains(it, ignoreCase = true) 
                        }
                        assertTrue("Expected Support material, got: ${filamentInfo.filamentType}", hasValidSupportType)
                    }
                    "TPU", "PC", "PA" -> assertTrue("Expected ${materialInfo.category} material, got: ${filamentInfo.filamentType}", 
                        filamentInfo.filamentType.contains(materialInfo.category, ignoreCase = true))
                }
                
                // Temperature validation for printable materials
                if (materialInfo.category.uppercase() != "SUPPORT MATERIAL") {
                    assertTrue("Invalid temperature for ${materialInfo.category}: ${filamentInfo.minTemperature}°C", 
                        filamentInfo.minTemperature > 0)
                }
                
                println("✓ $fileFormat: Decoded as ${filamentInfo.filamentType} @ ${filamentInfo.minTemperature}°C")
            }
            
        } catch (e: Exception) {
            println("ERROR testing ${testFile.name}: ${e.message}")
            throw AssertionError("Failed to decode $fileFormat file ${testFile.name}: ${e.message}")
        }
    }
}