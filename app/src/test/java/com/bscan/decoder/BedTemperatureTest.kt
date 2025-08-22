package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Unit tests specifically focused on bed temperature parsing issues
 * Tests various scenarios to identify why bed temperature shows as 0
 */
class BedTemperatureTest {

    @Test
    fun `bed temperature parsing with known good data should return 45 degrees`() {
        val testTagData = createExampleTagDataWithKnownBedTemp()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(testTagData, debugCollector)
        
        assertNotNull("Should parse valid tag data", result)
        result!!
        
        assertEquals("Bed temperature should be 45°C", 45, result.bedTemperature)
        assertEquals("Bed temperature type should be 1", 1, result.bedTemperatureType)
    }

    @Test
    fun `bed temperature parsing with zero value should return 0 degrees`() {
        val testTagData = createTagDataWithZeroBedTemp()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(testTagData, debugCollector)
        
        assertNotNull("Should parse valid tag data", result)
        result!!
        
        assertEquals("Bed temperature should be 0°C when data is zero", 0, result.bedTemperature)
    }

    @Test
    fun `bed temperature parsing with various values should work correctly`() {
        val testCases = listOf(
            Pair(60, "PLA+"),
            Pair(80, "ABS"),
            Pair(100, "PETG"),
            Pair(45, "PLA"),
            Pair(0, "No bed heating")
        )
        
        testCases.forEach { (expectedTemp, material) ->
            val testTagData = createTagDataWithBedTemp(expectedTemp)
            val result = BambuTagDecoder.parseTagDetails(testTagData)
            
            assertNotNull("Should parse $material tag data", result)
            assertEquals("Bed temperature should be ${expectedTemp}°C for $material", 
                expectedTemp, result!!.bedTemperature)
        }
    }

    @Test
    fun `block 6 raw byte extraction should work correctly`() {
        val testTagData = createExampleTagDataWithKnownBedTemp()
        
        // Test the private bytes() method using reflection
        val decoderClass = BambuTagDecoder::class.java
        val bytesMethod = decoderClass.getDeclaredMethod("bytes", 
            ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java)
        bytesMethod.isAccessible = true
        
        val intMethod = decoderClass.getDeclaredMethod("int", 
            ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java)
        intMethod.isAccessible = true
        
        // Extract Block 6 raw bytes (16 bytes total)
        val block6Bytes = bytesMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 0, 16) as ByteArray
        
        // Log the raw bytes for debugging
        val hexString = block6Bytes.joinToString("") { "%02X".format(it) }
        println("Block 6 raw bytes: $hexString")
        
        // Test individual field extraction from Block 6
        val dryingTemp = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 0, 2) as Int
        val dryingTime = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 2, 2) as Int
        val bedTempType = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 4, 2) as Int
        val bedTemp = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 6, 2) as Int
        val maxTemp = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 8, 2) as Int
        val minTemp = intMethod.invoke(BambuTagDecoder, testTagData.bytes, 6, 10, 2) as Int
        
        println("Drying temp: $dryingTemp")
        println("Drying time: $dryingTime")
        println("Bed temp type: $bedTempType")
        println("Bed temp: $bedTemp")
        println("Max temp: $maxTemp")
        println("Min temp: $minTemp")
        
        // Verify expected values from example data
        assertEquals("Drying temperature should be 55°C", 55, dryingTemp)
        assertEquals("Drying time should be 8 hours", 8, dryingTime)
        assertEquals("Bed temperature type should be 1", 1, bedTempType)
        assertEquals("Bed temperature should be 45°C", 45, bedTemp)
        assertEquals("Max temperature should be 220°C", 220, maxTemp)
        assertEquals("Min temperature should be 220°C", 220, minTemp)
    }

    @Test
    fun `byte order conversion should work correctly for bed temperature`() {
        // Test little-endian conversion manually
        
        // Bed temp 45°C should be encoded as 2D00 in little-endian
        val bedTemp45Bytes = byteArrayOf(0x2D, 0x00)
        val bedTemp45 = ByteBuffer.wrap(bedTemp45Bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        assertEquals("45°C should decode correctly from little-endian", 45, bedTemp45)
        
        // Test other common bed temperatures
        val bedTemp60Bytes = byteArrayOf(0x3C, 0x00) // 60 in hex = 0x3C
        val bedTemp60 = ByteBuffer.wrap(bedTemp60Bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        assertEquals("60°C should decode correctly", 60, bedTemp60)
        
        val bedTemp80Bytes = byteArrayOf(0x50, 0x00) // 80 in hex = 0x50
        val bedTemp80 = ByteBuffer.wrap(bedTemp80Bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
        assertEquals("80°C should decode correctly", 80, bedTemp80)
    }

    @Test
    fun `compressed block index calculation should be correct for block 6`() {
        // Block 6 should map to byte index 80 in compressed format
        // Formula: (blockNumber - blockNumber/4) * 16
        val block6CompressedIndex = (6 - (6 / 4)) * 16
        assertEquals("Block 6 should start at byte 80", 80, block6CompressedIndex)
        
        // Bed temperature offset within block 6 is 6 bytes
        val bedTempByteIndex = block6CompressedIndex + 6
        assertEquals("Bed temperature should be at byte 86", 86, bedTempByteIndex)
    }

    // Helper method to create tag data with known bed temperature
    private fun createExampleTagDataWithKnownBedTemp(): NfcTagData {
        // Use the exact same data from BambuTagDecoderComprehensiveTest
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "3700080001002D00DC00DC0000000000", // This is where bed temp 45°C (0x2D) is located
            8 to "8813100EE803E8039A99193FCDCC4C3E",
            9 to "D7AC3B89A16B47C4B061728044E1F2D5",
            10 to "00000000E11900000000000000000000",
            12 to "323032325F31305F31355F30385F3236",
            13 to "36303932323032000000000000000000",
            14 to "00000000520000000000000000000000",
            16 to "00000000000000000000000000000000",
            17 to "00000000000000000000000000000000"
        )
        
        return createTagDataFromBlocks(blocks)
    }

    // Helper method to create tag data with zero bed temperature
    private fun createTagDataWithZeroBedTemp(): NfcTagData {
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "37000800010000000C00DC0000000000", // Modified: bed temp = 0x0000, bed type = 0x0000
            8 to "8813100EE803E8039A99193FCDCC4C3E",
            9 to "D7AC3B89A16B47C4B061728044E1F2D5",
            10 to "00000000E11900000000000000000000",
            12 to "323032325F31305F31355F30385F3236",
            13 to "36303932323032000000000000000000",
            14 to "00000000520000000000000000000000",
            16 to "00000000000000000000000000000000",
            17 to "00000000000000000000000000000000"
        )
        
        return createTagDataFromBlocks(blocks)
    }

    // Helper method to create tag data with specific bed temperature
    private fun createTagDataWithBedTemp(bedTempCelsius: Int): NfcTagData {
        // Create little-endian hex: LSB first, MSB second
        val lsb = String.format("%02X", bedTempCelsius and 0xFF)
        val msb = String.format("%02X", (bedTempCelsius shr 8) and 0xFF)
        val bedTempLE = lsb + msb
        
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "370008000100${bedTempLE}DC00DC0000000000", // Insert bed temp at correct position
            8 to "8813100EE803E8039A99193FCDCC4C3E",
            9 to "D7AC3B89A16B47C4B061728044E1F2D5",
            10 to "00000000E11900000000000000000000",
            12 to "323032325F31305F31355F30385F3236",
            13 to "36303932323032000000000000000000",
            14 to "00000000520000000000000000000000",
            16 to "00000000000000000000000000000000",
            17 to "00000000000000000000000000000000"
        )
        
        return createTagDataFromBlocks(blocks)
    }

    // Helper method to convert block map to NfcTagData
    private fun createTagDataFromBlocks(blocks: Map<Int, String>): NfcTagData {
        // Convert to sequential byte array (skip trailer blocks)
        val byteList = mutableListOf<Byte>()
        for (blockNum in 0..63) {
            if (blockNum % 4 != 3) { // Skip trailer blocks
                val hexData = blocks[blockNum] ?: "00000000000000000000000000000000"
                val blockBytes = hexData.chunked(2).map { it.toInt(16).toByte() }
                byteList.addAll(blockBytes)
            }
        }
        
        return NfcTagData(
            uid = "75886B1D",
            bytes = byteList.toByteArray(),
            technology = "MifareClassic"
        )
    }
}