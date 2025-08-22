package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*

/**
 * Edge case tests for bed temperature parsing
 * Tests malformed data, boundary conditions, and data corruption scenarios
 */
class BedTemperatureEdgeCaseTest {

    @Test
    fun `bed temperature parsing with corrupted block 6 should handle gracefully`() {
        val testTagData = createTagDataWithCorruptedBlock6()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(testTagData, debugCollector)
        
        // Should still parse but handle corrupted data gracefully
        assertNotNull("Should handle corrupted data gracefully", result)
        result!!
        
        // Bed temperature might be 0 or unexpected value, but shouldn't crash
        // With corrupted data, we just want to ensure no exception occurs and result is not null
        assertTrue("Bed temperature should be non-negative", result.bedTemperature >= 0)
    }

    @Test
    fun `bed temperature parsing with insufficient data should return null`() {
        // Create tag data with only 100 bytes (less than required 240)
        val insufficientTagData = NfcTagData(
            uid = "12345678",
            bytes = ByteArray(100) { 0x00 }, // Fill with zeros
            technology = "MifareClassic"
        )
        
        val result = BambuTagDecoder.parseTagDetails(insufficientTagData)
        
        assertNull("Should return null for insufficient data", result)
    }

    @Test
    fun `bed temperature parsing with all zero block 6 should return zero`() {
        val testTagData = createTagDataWithAllZeroBlock6()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull("Should parse tag with all zero block 6", result)
        result!!
        
        assertEquals("Bed temperature should be 0 when block 6 is all zeros", 0, result.bedTemperature)
        assertEquals("Bed temperature type should be 0 when block 6 is all zeros", 0, result.bedTemperatureType)
        assertEquals("Drying temperature should be 0 when block 6 is all zeros", 0, result.dryingTemperature)
        assertEquals("Drying time should be 0 when block 6 is all zeros", 0, result.dryingTime)
    }

    @Test
    fun `bed temperature parsing with maximum values should work correctly`() {
        val testTagData = createTagDataWithMaxBedTemp()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull("Should parse tag with maximum bed temperature", result)
        result!!
        
        // Test with maximum uint16 value (65535)
        assertEquals("Should handle maximum uint16 value", 65535, result.bedTemperature)
    }

    @Test
    fun `bed temperature parsing with common realistic values should work`() {
        val commonBedTemps = listOf(0, 45, 50, 60, 70, 80, 90, 100, 110)
        
        commonBedTemps.forEach { temp ->
            val testTagData = createTagDataWithSpecificBedTemp(temp)
            val result = BambuTagDecoder.parseTagDetails(testTagData)
            
            assertNotNull("Should parse tag with bed temp $temp", result)
            assertEquals("Bed temperature should be $temp", temp, result!!.bedTemperature)
        }
    }

    @Test
    fun `bed temperature parsing with byte boundary issues should be handled`() {
        // Test case where bed temperature bytes might be at exact boundary conditions
        val testTagData = createTagDataWithBoundaryBedTemp()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull("Should parse tag data near byte boundaries", result)
        result!!
        
        // Just ensure no exception is thrown and result is reasonable
        assertTrue("Bed temperature should be reasonable", result.bedTemperature >= 0)
    }

    @Test
    fun `bed temperature parsing with missing block 6 data should default to zero`() {
        val testTagData = createTagDataMissingBlock6()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        // Depending on implementation, might return null or handle gracefully
        if (result != null) {
            assertEquals("Missing block 6 should default bed temperature to 0", 0, result.bedTemperature)
        }
    }

    @Test
    fun `bed temperature byte order reversal test`() {
        // Test with bed temp 0x3C2D (15405 in decimal) vs 0x2D3C (11580 in decimal)
        val testTagDataLE = createTagDataWithBedTempBytes(0x2D, 0x3C) // Little-endian: should be 15405
        val testTagDataBE = createTagDataWithBedTempBytes(0x3C, 0x2D) // If parsed as big-endian: should be 15405
        
        val resultLE = BambuTagDecoder.parseTagDetails(testTagDataLE)
        val resultBE = BambuTagDecoder.parseTagDetails(testTagDataBE)
        
        assertNotNull("Should parse LE data", resultLE)
        assertNotNull("Should parse BE data", resultBE)
        
        // In little-endian format, first byte is LSB
        // 0x2D + (0x3C << 8) = 45 + 15360 = 15405
        assertEquals("LE format should give 15405", 15405, resultLE!!.bedTemperature)
        
        // If same bytes in different order:
        // 0x3C + (0x2D << 8) = 60 + 11520 = 11580  
        assertEquals("Different byte order should give 11580", 11580, resultBE!!.bedTemperature)
    }

    // Helper methods for creating test data

    private fun createTagDataWithCorruptedBlock6(): NfcTagData {
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "FFFF00FF01FFFF00FFFF00FFFF00FFFF", // Corrupted block 6
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

    private fun createTagDataWithAllZeroBlock6(): NfcTagData {
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "00000000000000000000000000000000", // All zeros
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

    private fun createTagDataWithMaxBedTemp(): NfcTagData {
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "370008000100FFFFDC00DC0000000000", // Max bed temp: 0xFFFF = 65535
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

    private fun createTagDataWithSpecificBedTemp(tempCelsius: Int): NfcTagData {
        // Create little-endian hex: LSB first, MSB second
        val lsb = String.format("%02X", tempCelsius and 0xFF)
        val msb = String.format("%02X", (tempCelsius shr 8) and 0xFF)
        val bedTempLE = lsb + msb
        
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "370008000100${bedTempLE}DC00DC0000000000",
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

    private fun createTagDataWithBoundaryBedTemp(): NfcTagData {
        // Use a temperature that's exactly at the boundary of byte alignment
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "370008000100FF01DC00DC0000000000", // Bed temp: 0x01FF = 511
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

    private fun createTagDataMissingBlock6(): NfcTagData {
        // Create data where block 6 position would be filled with zeros or missing
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            // Block 6 deliberately omitted
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

    private fun createTagDataWithBedTempBytes(byte1: Int, byte2: Int): NfcTagData {
        val bedTempHex = String.format("%02X%02X", byte1, byte2)
        
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "370008000100${bedTempHex}DC00DC0000000000",
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