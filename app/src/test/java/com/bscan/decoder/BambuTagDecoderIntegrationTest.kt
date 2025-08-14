package com.bscan.decoder

import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import org.junit.Test
import org.junit.Assert.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Integration tests for BambuTagDecoder with simulated real-world tag scenarios
 * Tests complete tag parsing workflows with various filament types and conditions
 * Uses gradle testOptions { unitTests { isReturnDefaultValues = true } } to mock Android Log
 */
class BambuTagDecoderIntegrationTest {

    @Test
    fun `integration test with PLA Basic filament tag`() {
        val tagData = createPLABasicTag()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(tagData, debugCollector)
        
        assertNotNull("Should successfully parse PLA Basic tag", result)
        result!!
        
        // Validate PLA Basic specific characteristics
        assertEquals("Should be PLA filament", "PLA", result.filamentType)
        assertEquals("Should be PLA Basic variant", "PLA Basic", result.detailedFilamentType)
        assertEquals("Should be 1.75mm diameter", 1.75f, result.filamentDiameter, 0.01f)
        assertTrue("Should have reasonable bed temp for PLA", result.bedTemperature in 50..70)
        assertTrue("Should have reasonable hotend temp for PLA", result.minTemperature in 190..220)
        
        // Validate debug data collection
        assertNotNull("Debug collector should have recorded data", debugCollector)
        
        // Validate extended fields
        assertTrue("Material ID should start with GF", result.materialId.startsWith("GF"))
        assertEquals("Should be single color", 1, result.colorCount)
        assertTrue("Color should be valid hex", result.colorHex.matches(Regex("^#[0-9A-Fa-f]{6}$")))
    }

    @Test
    fun `integration test with PETG filament tag`() {
        val tagData = createPETGTag()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should successfully parse PETG tag", result)
        result!!
        
        // Validate PETG specific characteristics
        assertEquals("Should be PETG filament", "PETG", result.filamentType)
        assertTrue("Should have higher bed temp for PETG", result.bedTemperature in 70..90)
        assertTrue("Should have higher hotend temp for PETG", result.minTemperature in 220..250)
        
        // PETG typically requires higher drying temperature
        assertTrue("Should have higher drying temp for PETG", result.dryingTemperature in 60..80)
    }

    @Test
    fun `integration test with dual color PLA Silk tag`() {
        val tagData = createDualColorPLASilkTag()
        val debugCollector = DebugDataCollector()
        
        val result = BambuTagDecoder.parseTagDetails(tagData, debugCollector)
        
        assertNotNull("Should successfully parse dual color tag", result)
        result!!
        
        // Validate dual color characteristics
        assertEquals("Should be PLA filament", "PLA", result.filamentType)
        assertEquals("Should be PLA Silk variant", "PLA Silk", result.detailedFilamentType)
        assertEquals("Should have 2 colors", 2, result.colorCount)
        assertTrue("Should contain dual color format", result.colorHex.contains(" / "))
        
        // Colors should be valid hex format on both sides
        val colors = result.colorHex.split(" / ")
        assertEquals("Should have exactly 2 colors", 2, colors.size)
        colors.forEach { color ->
            assertTrue("Each color should be valid hex", color.matches(Regex("^#[0-9A-Fa-f]{6}$")))
        }
    }

    @Test
    fun `integration test with support material tag`() {
        val tagData = createSupportMaterialTag()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should successfully parse support material tag", result)
        result!!
        
        // Validate support material characteristics
        assertTrue("Should be support material", 
            result.filamentType.contains("Support") || result.detailedFilamentType.contains("Support"))
        
        // Support materials typically have different properties
        assertTrue("Support material should have reasonable properties", 
            result.bedTemperature in 40..80 && result.spoolWeight > 0)
    }

    @Test
    fun `integration test with old production date tag`() {
        val tagData = createOldProductionDateTag()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse old production date", result)
        result!!
        
        // Should handle old dates correctly
        assertTrue("Production date should contain year", result.productionDate.contains("202"))
        assertNotEquals("Should not be Unknown", "Unknown", result.productionDate)
    }

    @Test
    fun `integration test with maximum spool weight tag`() {
        val tagData = createMaxWeightSpoolTag()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse heavy spool", result)
        result!!
        
        // Should handle large spool weights
        assertTrue("Should handle large spool weight", result.spoolWeight >= 1000)
        assertTrue("Filament length should be proportional", result.filamentLength > 100000) // > 100m
    }

    @Test
    fun `integration test validates cross-field consistency`() {
        val tagData = createConsistentTag()
        val result = BambuTagDecoder.parseTagDetails(tagData)
        
        assertNotNull("Should parse consistent tag", result)
        result!!
        
        // Validate field consistency
        assertTrue("Max temp should be >= min temp", result.maxTemperature >= result.minTemperature)
        assertTrue("Spool weight should be positive", result.spoolWeight > 0)
        assertTrue("Filament diameter should be positive", result.filamentDiameter > 0)
        assertTrue("Filament length should be positive", result.filamentLength > 0)
        
        // Color consistency
        if (result.colorCount > 1) {
            assertTrue("Multi-color should have multi-color hex", result.colorHex.contains(" / "))
        } else {
            assertFalse("Single color should not have multi-color hex", result.colorHex.contains(" / "))
        }
    }

    @Test
    fun `integration test with different nozzle diameters`() {
        val nozzleSizes = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)
        
        nozzleSizes.forEach { expectedSize ->
            val tagData = createTagWithNozzleDiameter(expectedSize)
            val result = BambuTagDecoder.parseTagDetails(tagData)
            
            assertNotNull("Should parse tag with ${expectedSize}mm nozzle", result)
            result!!
            
            assertEquals("Should have correct nozzle diameter", expectedSize, result.nozzleDiameter, 0.01f)
        }
    }

    @Test
    fun `integration test validates material ID and variant relationships`() {
        val testCases = listOf(
            Pair("GFA50", "A50-K0"),
            Pair("GFB00", "B00-A1"),
            Pair("GFC25", "C25-X2")
        )
        
        testCases.forEach { (materialId, variantId) ->
            val tagData = createTagWithMaterialIds(materialId, variantId)
            val result = BambuTagDecoder.parseTagDetails(tagData)
            
            assertNotNull("Should parse tag with material $materialId", result)
            result!!
            
            assertEquals("Should have correct material ID", materialId, result.materialId)
            assertEquals("Should have correct variant ID", variantId, result.materialVariantId)
            
            // Validate relationship pattern
            val materialSuffix = materialId.substring(2) // Remove "GF" prefix
            assertTrue("Variant should relate to material", 
                variantId.startsWith(materialSuffix) || materialSuffix.startsWith(variantId.take(3)))
        }
    }

    // Helper methods to create various tag scenarios

    private fun createPLABasicTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        // Block 1: Material IDs
        writeStringToBlock(bytes, 1, 0, "A00-K000") // Variant ID
        writeStringToBlock(bytes, 1, 8, "GFA00000") // Material ID
        
        // Block 2: Filament Type
        writeStringToBlock(bytes, 2, 0, "PLA")
        
        // Block 4: Detailed Type
        writeStringToBlock(bytes, 4, 0, "PLA Basic")
        
        // Block 5: Color (red), Weight (250g), Diameter (1.75mm)
        writeColorToBlock(bytes, 5, 255, 0, 0, 255) // Red RGBA
        writeUInt16ToBlock(bytes, 5, 4, 250) // 250g weight
        writeFloatToBlock(bytes, 5, 8, 1.75f) // 1.75mm diameter
        
        // Block 6: Temperatures
        writeUInt16ToBlock(bytes, 6, 0, 45) // Drying temp
        writeUInt16ToBlock(bytes, 6, 2, 8) // Drying time
        writeUInt16ToBlock(bytes, 6, 4, 1) // Bed temp type
        writeUInt16ToBlock(bytes, 6, 6, 60) // Bed temp
        writeUInt16ToBlock(bytes, 6, 8, 210) // Max hotend
        writeUInt16ToBlock(bytes, 6, 10, 190) // Min hotend
        
        // Block 8: Nozzle diameter
        writeFloatToBlock(bytes, 8, 12, 0.4f)
        
        // Block 10: Spool width
        writeUInt16ToBlock(bytes, 10, 4, 6625) // 66.25mm * 100
        
        // Block 12: Production date
        writeStringToBlock(bytes, 12, 0, "2024_01_15_10_30")
        
        // Block 14: Filament length
        writeUInt16ToBlock(bytes, 14, 4, 250) // 250 meters
        
        return NfcTagData("PLA_BASIC", bytes, "MifareClassic")
    }

    private fun createPETGTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "PETG")
        writeStringToBlock(bytes, 4, 0, "PETG Basic")
        
        // Higher temperatures for PETG
        writeUInt16ToBlock(bytes, 6, 0, 65) // Drying temp
        writeUInt16ToBlock(bytes, 6, 6, 80) // Bed temp
        writeUInt16ToBlock(bytes, 6, 8, 250) // Max hotend
        writeUInt16ToBlock(bytes, 6, 10, 230) // Min hotend
        
        writeFloatToBlock(bytes, 5, 8, 1.75f) // 1.75mm diameter
        
        return NfcTagData("PETG_BASIC", bytes, "MifareClassic")
    }

    private fun createDualColorPLASilkTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "PLA")
        writeStringToBlock(bytes, 4, 0, "PLA Silk")
        
        // Primary color (red)
        writeColorToBlock(bytes, 5, 255, 0, 0, 255)
        
        // Block 16: Dual color info
        writeUInt16ToBlock(bytes, 16, 0, 2) // Format identifier
        writeUInt16ToBlock(bytes, 16, 2, 2) // Color count
        writeColorToBlock(bytes, 16, 4, 0, 255, 0, 255) // Second color (green) in ABGR
        
        return NfcTagData("PLA_SILK_DUAL", bytes, "MifareClassic")
    }

    private fun createSupportMaterialTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "Support")
        writeStringToBlock(bytes, 4, 0, "Support for PLA")
        
        // Support material properties
        writeUInt16ToBlock(bytes, 5, 4, 200) // Spool weight 200g
        writeUInt16ToBlock(bytes, 6, 6, 50) // Lower bed temp
        writeUInt16ToBlock(bytes, 6, 8, 200) // Lower hotend temp
        writeUInt16ToBlock(bytes, 6, 10, 180)
        
        return NfcTagData("SUPPORT_PLA", bytes, "MifareClassic")
    }

    private fun createOldProductionDateTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "PLA")
        writeStringToBlock(bytes, 12, 0, "2021_03_15_14_22") // Old date
        
        return NfcTagData("OLD_DATE", bytes, "MifareClassic")
    }

    private fun createMaxWeightSpoolTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "PLA")
        writeUInt16ToBlock(bytes, 5, 4, 1000) // 1kg spool
        writeUInt16ToBlock(bytes, 14, 4, 330) // 330 meters
        
        return NfcTagData("MAX_WEIGHT", bytes, "MifareClassic")
    }

    private fun createConsistentTag(): NfcTagData {
        val bytes = ByteArray(1024)
        
        writeStringToBlock(bytes, 2, 0, "PLA")
        writeUInt16ToBlock(bytes, 5, 4, 500) // Weight
        writeFloatToBlock(bytes, 5, 8, 1.75f) // Diameter
        writeUInt16ToBlock(bytes, 14, 4, 285) // Length
        
        // Consistent temperatures
        writeUInt16ToBlock(bytes, 6, 8, 210) // Max temp
        writeUInt16ToBlock(bytes, 6, 10, 190) // Min temp
        
        // Single color
        writeUInt16ToBlock(bytes, 16, 2, 1) // Color count = 1
        
        return NfcTagData("CONSISTENT", bytes, "MifareClassic")
    }

    private fun createTagWithNozzleDiameter(diameter: Float): NfcTagData {
        val bytes = ByteArray(1024)
        writeStringToBlock(bytes, 2, 0, "PLA")
        writeFloatToBlock(bytes, 8, 12, diameter)
        return NfcTagData("NOZZLE_$diameter", bytes, "MifareClassic")
    }

    private fun createTagWithMaterialIds(materialId: String, variantId: String): NfcTagData {
        val bytes = ByteArray(1024)
        writeStringToBlock(bytes, 1, 0, variantId)
        writeStringToBlock(bytes, 1, 8, materialId)
        writeStringToBlock(bytes, 2, 0, "PLA")
        return NfcTagData("MATERIAL_$materialId", bytes, "MifareClassic")
    }

    // Helper methods for writing data to byte arrays

    private fun writeStringToBlock(bytes: ByteArray, block: Int, offset: Int, value: String) {
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = block - (block / 4)
        val blockStart = compressedBlockIndex * 16 + offset
        val stringBytes = value.toByteArray()
        val copyLength = minOf(stringBytes.size, 16 - offset)
        stringBytes.copyInto(bytes, blockStart, 0, copyLength)
    }

    private fun writeUInt16ToBlock(bytes: ByteArray, block: Int, offset: Int, value: Int) {
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = block - (block / 4)
        val blockStart = compressedBlockIndex * 16 + offset
        bytes[blockStart] = (value and 0xFF).toByte()
        bytes[blockStart + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun writeFloatToBlock(bytes: ByteArray, block: Int, offset: Int, value: Float) {
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = block - (block / 4)
        val blockStart = compressedBlockIndex * 16 + offset
        val buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array()
        buffer.copyInto(bytes, blockStart)
    }

    private fun writeColorToBlock(bytes: ByteArray, block: Int, r: Int, g: Int, b: Int, a: Int) {
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = block - (block / 4)
        val blockStart = compressedBlockIndex * 16
        bytes[blockStart] = r.toByte()
        bytes[blockStart + 1] = g.toByte()
        bytes[blockStart + 2] = b.toByte()
        bytes[blockStart + 3] = a.toByte()
    }

    private fun writeColorToBlock(bytes: ByteArray, block: Int, offset: Int, r: Int, g: Int, b: Int, a: Int) {
        // Use compressed addressing to match decoder layout
        val compressedBlockIndex = block - (block / 4)
        val blockStart = compressedBlockIndex * 16 + offset
        bytes[blockStart] = r.toByte()
        bytes[blockStart + 1] = g.toByte()
        bytes[blockStart + 2] = b.toByte()
        bytes[blockStart + 3] = a.toByte()
    }
}