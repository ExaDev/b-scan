package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

/**
 * Comprehensive unit tests for BambuTagDecoder based on RFID-Tag-Guide specification
 * Tests all block parsing functions with real example data
 * Uses gradle testOptions { unitTests { isReturnDefaultValues = true } } to mock Android Log
 */
class BambuTagDecoderComprehensiveTest {

    @Test
    fun `parseTagDetails with real example data extracts all fields correctly`() {
        // Test data from RFID-Tag-Guide exampleDump.json
        val testTagData = createExampleTagData()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(testTagData, debugCollector)
        
        assertNotNull("Should parse valid tag data", result)
        result!!
        
        // Block 0: UID
        assertEquals("UID should match", "75886B1D", result.uid)
        
        // Block 1: Material Variant ID and Material ID  
        assertEquals("Material variant ID should be parsed", "A00-A0", result.materialVariantId)
        assertEquals("Material ID should be parsed", "GFA00", result.materialId)
        
        // Block 2: Filament Type
        assertEquals("Filament type should be parsed", "PLA", result.filamentType)
        
        // Block 4: Detailed Filament Type
        assertEquals("Detailed filament type should be parsed", "PLA Basic", result.detailedFilamentType)
        
        // Block 5: Color, Spool Weight, Filament Diameter
        assertEquals("Color should be parsed correctly", "#FF6A13", result.colorHex)
        assertEquals("Spool weight should be 250g", 250, result.spoolWeight)
        assertEquals("Filament diameter should be 1.75mm", 1.75f, result.filamentDiameter, 0.01f)
        
        // Block 6: Temperature and Drying Info
        assertEquals("Drying temperature should be 55°C", 55, result.dryingTemperature)
        assertEquals("Drying time should be 8 hours", 8, result.dryingTime)
        assertEquals("Bed temperature type should be 1", 1, result.bedTemperatureType)
        assertEquals("Bed temperature should be 45°C", 45, result.bedTemperature)
        assertEquals("Max hotend temperature should be 220°C", 220, result.maxTemperature)
        assertEquals("Min hotend temperature should be 220°C", 220, result.minTemperature)
        
        // Block 8: X Cam Info and Nozzle Diameter
        assertEquals("Nozzle diameter should be 0.2mm", 0.2f, result.nozzleDiameter, 0.01f)
        
        // Block 9: Tray UID
        assertNotNull("Tray UID should be present", result.trayUid)
        
        // Block 10: Spool Width
        assertEquals("Spool width should be 66.25mm", 66.25f, result.spoolWidth, 0.01f)
        
        // Block 12: Production Date
        assertEquals("Production date should be parsed", "2022-10-15T08:26", result.productionDate)
        
        // Block 14: Filament Length (converted from meters to mm)
        assertEquals("Filament length should be 82000mm", 82000, result.filamentLength)
        
        // Block 16: Color Count (should be 1 for this example)
        assertEquals("Color count should be 1", 1, result.colorCount)
    }

    @Test
    fun `parseTagDetails handles dual color filament correctly`() {
        val testTagData = createDualColorTagData()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(testTagData, debugCollector)
        
        assertNotNull("Should parse dual color tag data", result)
        result!!
        
        assertEquals("Color count should be 2", 2, result.colorCount)
        assertTrue("Color hex should contain dual color format", result.colorHex.contains(" / "))
    }

    @Test
    fun `parseTagDetails handles temperature values correctly`() {
        val testTagData = createExampleTagData()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull(result)
        result!!
        
        // Validate temperature ranges
        assertTrue("Drying temperature should be reasonable", result.dryingTemperature in 40..100)
        assertTrue("Bed temperature should be reasonable", result.bedTemperature in 40..120)
        assertTrue("Min hotend temperature should be reasonable", result.minTemperature in 150..250)
        assertTrue("Max hotend temperature should be reasonable", result.maxTemperature in 150..300)
        assertTrue("Max should be >= min", result.maxTemperature >= result.minTemperature)
    }

    @Test
    fun `parseTagDetails handles production date parsing correctly`() {
        val testTagData = createExampleTagData()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull(result)
        result!!
        
        // Should parse production date from block 12: "323032325F31305F31355F30385F3236"
        // This is "2022_10_15_08_26" in ASCII
        assertTrue("Production date should contain 2022", result.productionDate.contains("2022"))
    }

    @Test
    fun `parseTagDetails handles filament diameter variations`() {
        val testTagData = createExampleTagData()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull(result)
        result!!
        
        // Common filament diameters
        assertTrue("Filament diameter should be common size", 
            result.filamentDiameter == 1.75f || result.filamentDiameter == 3.0f)
    }

    @Test
    fun `parseTagDetails handles material ID format correctly`() {
        val testTagData = createExampleTagData()
        val result = BambuTagDecoder.parseTagDetails(testTagData)
        
        assertNotNull(result)
        result!!
        
        // Material IDs should start with "GF" according to RFID-Tag-Guide
        assertTrue("Material ID should start with GF", result.materialId.startsWith("GF"))
        
        // Material variant ID should be related to material ID
        assertTrue("Material variant should be non-empty", result.materialVariantId.isNotEmpty())
    }

    @Test
    fun `block parsing helper functions work correctly`() {
        val testData = createExampleTagData()
        
        // Test individual block parsing using reflection to access private methods
        val decoderClass = BambuTagDecoder::class.java
        val bytesMethod = decoderClass.getDeclaredMethod("bytes", ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java)
        bytesMethod.isAccessible = true
        
        val stringMethod = decoderClass.getDeclaredMethod("string", ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java)
        stringMethod.isAccessible = true
        
        val intMethod = decoderClass.getDeclaredMethod("int", ByteArray::class.java, Int::class.java, Int::class.java, Int::class.java)
        intMethod.isAccessible = true
        
        // Test Block 2 (Filament Type) parsing
        val block2String = stringMethod.invoke(BambuTagDecoder, testData.bytes, 2, 0, 16) as String
        assertEquals("Block 2 should parse to PLA", "PLA", block2String)
        
        // Test Block 5 (Spool Weight) parsing - bytes 4-5, uint16 LE
        val spoolWeight = intMethod.invoke(BambuTagDecoder, testData.bytes, 5, 4, 2) as Int
        assertEquals("Spool weight should be 250g", 250, spoolWeight)
        
        // Test Block 6 (Temperatures) parsing
        val dryingTemp = intMethod.invoke(BambuTagDecoder, testData.bytes, 6, 0, 2) as Int
        val dryingTime = intMethod.invoke(BambuTagDecoder, testData.bytes, 6, 2, 2) as Int
        
        assertEquals("Drying temperature should be 55°C", 55, dryingTemp)
        assertEquals("Drying time should be 8 hours", 8, dryingTime)
    }

    // Helper method to create example tag data from RFID-Tag-Guide
    private fun createExampleTagData(): NfcTagData {
        // Convert hex blocks from exampleDump.json to byte array
        val blocks = mapOf(
            0 to "75886B1D8B080400034339DB5B5E0A90",
            1 to "4130302D413000004746413030000000", 
            2 to "504C4100000000000000000000000000",
            4 to "504C4120426173696300000000000000",
            5 to "FF6A13FFFA0000000000E03F00000000",
            6 to "3700080001002D00DC00DC0000000000",
            8 to "8813100EE803E8039A99193FCDCC4C3E",
            9 to "D7AC3B89A16B47C4B061728044E1F2D5",
            10 to "00000000E11900000000000000000000",
            12 to "323032325F31305F31355F30385F3236",
            13 to "36303932323032000000000000000000",
            14 to "00000000520000000000000000000000",
            16 to "00000000000000000000000000000000",
            17 to "00000000000000000000000000000000"
        )
        
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
    
    // Helper method to create dual color tag data
    private fun createDualColorTagData(): NfcTagData {
        val testData = createExampleTagData()
        val bytes = testData.bytes.toMutableList()
        
        // Modify block 16 to indicate dual color: format identifier = 0x0002, color count = 2
        // Calculate compressed byte offset for block 16: (16 - 16/4) * 16 = 12 * 16 = 192
        val block16Index = (16 - (16 / 4)) * 16 // Block 16 in compressed layout
        if (block16Index + 8 < bytes.size) {
            // Format identifier: 02 00 (little endian for 0x0002)
            bytes[block16Index] = 0x02
            bytes[block16Index + 1] = 0x00
            // Color count: 02 00 (little endian for 2)
            bytes[block16Index + 2] = 0x02
            bytes[block16Index + 3] = 0x00
            // Second color in ABGR format: FF00FF00 (magenta)
            bytes[block16Index + 4] = 0xFF.toByte()
            bytes[block16Index + 5] = 0x00
            bytes[block16Index + 6] = 0xFF.toByte()
            bytes[block16Index + 7] = 0x00
        }
        
        return NfcTagData(
            uid = testData.uid,
            bytes = bytes.toByteArray(),
            technology = testData.technology
        )
    }
}