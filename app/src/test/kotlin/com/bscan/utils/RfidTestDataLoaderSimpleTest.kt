package com.bscan.utils

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals

/**
 * Simple test to verify RfidTestDataLoader works with real submodule data
 */
class RfidTestDataLoaderSimpleTest {
    
    @Test
    fun testLoadAllDumpFiles() {
        val allFiles = RfidTestDataLoader.loadAllDumpFiles()
        
        assertTrue("Expected some dump files, got ${allFiles.size}", allFiles.isNotEmpty())
        println("Total dump files found: ${allFiles.size}")
        
        // Verify we have both BIN and JSON files
        val jsonFiles = allFiles.filter { it.name.endsWith("-dump.json") }
        val binFiles = allFiles.filter { it.name.endsWith(".bin") && !it.name.endsWith("-key.bin") }
        
        println("JSON files: ${jsonFiles.size}")
        println("BIN files: ${binFiles.size}")
        
        assertTrue("Expected JSON files", jsonFiles.isNotEmpty())
        assertTrue("Expected BIN files", binFiles.isNotEmpty())
        
        // Should have found ~775 total files (347 JSON + 428 BIN)
        assertTrue("Expected at least 700 files, got ${allFiles.size}", allFiles.size >= 700)
    }
    
    @Test
    fun testParseSampleJsonFile() {
        val jsonFiles = RfidTestDataLoader.loadJsonDumpFiles()
        assertTrue("No JSON files found", jsonFiles.isNotEmpty())
        
        val sampleFile = jsonFiles.first()
        println("Testing JSON file: ${sampleFile.name}")
        
        val dumpData = RfidTestDataLoader.parseDumpFile(sampleFile)
        
        assertNotNull("Failed to parse JSON dump", dumpData)
        assertNotNull("No card info", dumpData.Card)
        assertNotNull("No UID", dumpData.Card.UID)
        assertTrue("No blocks", dumpData.blocks.isNotEmpty())
        
        println("Parsed JSON - UID: ${dumpData.Card.UID}, Blocks: ${dumpData.blocks.size}")
        
        // Verify we have 64 blocks (Mifare Classic 1K)
        assertEquals("Expected 64 blocks", 64, dumpData.blocks.size)
        
        // Test block 2 (filament type)
        val block2 = dumpData.blocks["2"]
        assertNotNull("Block 2 missing", block2)
        if (block2 != null) {
            val blockBytes = hexStringToByteArray(block2)
            val filamentType = String(blockBytes.takeWhile { it != 0.toByte() }.toByteArray())
            println("Filament type from block 2: '$filamentType'")
            assertTrue("Invalid filament type", filamentType.isNotEmpty())
        }
    }
    
    @Test
    fun testParseSampleBinFile() {
        val binFiles = RfidTestDataLoader.loadBinDumpFiles()
        assertTrue("No BIN files found", binFiles.isNotEmpty())
        
        val sampleFile = binFiles.first()
        println("Testing BIN file: ${sampleFile.name}")
        
        val dumpData = RfidTestDataLoader.parseDumpFile(sampleFile)
        
        assertNotNull("Failed to parse BIN dump", dumpData)
        assertNotNull("No card info", dumpData.Card)
        assertNotNull("No UID", dumpData.Card.UID)
        assertTrue("No blocks", dumpData.blocks.isNotEmpty())
        
        println("Parsed BIN - UID: ${dumpData.Card.UID}, Blocks: ${dumpData.blocks.size}")
        
        // Verify we have 64 blocks (Mifare Classic 1K)
        assertEquals("Expected 64 blocks", 64, dumpData.blocks.size)
    }
    
    @Test
    fun testMaterialCounts() {
        val counts = RfidTestDataLoader.getMaterialCounts()
        
        assertTrue("No material counts", counts.isNotEmpty())
        
        println("Material counts:")
        counts.toSortedMap().forEach { (material, count) ->
            println("  $material: $count tags")
        }
        
        // Should have major material categories
        val totalFiles = counts.values.sum()
        assertTrue("Expected at least 700 total files", totalFiles >= 700)
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").uppercase()
        return cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}