package com.bscan.decoder

import android.util.Log
import com.bscan.debug.DebugDataCollector
import com.bscan.debug.BedTemperatureDebugHelper
import com.bscan.model.FilamentInfo
import com.bscan.model.NfcTagData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Bambu Lab RFID Tag Decoder
 * 
 * TERMINOLOGY CLARIFICATION:
 * - Tag UID: Unique identifier for each individual RFID tag (unique per tag)
 * - Tray UID: Identifier for the filament spool tray (shared between both tags on a spool)
 * - Spool: Physical filament spool containing 2 RFID tags with the same tray UID
 * 
 * Each Bambu Lab filament spool contains 2 RFID tags with:
 * - Different tag UIDs (unique per physical tag)
 * - Same tray UID (identifying the spool/tray)
 * - Same filament information encoded on both tags
 */
object BambuTagDecoder {
    private const val TAG = "BambuTagDecoder"
    
    fun parseTagDetails(data: NfcTagData, debugCollector: DebugDataCollector? = null): FilamentInfo? {
        Log.d(TAG, "Parsing tag UID: ${data.uid}")
        Log.d(TAG, "Tag data size: ${data.bytes.size} bytes")
        Log.d(TAG, "Tag technology: ${data.technology}")
        
        // Ensure there is enough data in the tag bytes to extract the necessary details
        // Need at least 15 blocks (240 bytes) to read all required data
        if (data.bytes.size < 240) {
            Log.w(TAG, "Insufficient data: ${data.bytes.size} bytes, need at least 240")
            return null
        }
        
        // Log first few blocks of raw data for debugging
        for (block in 0..6) {
            val blockData = bytes(data.bytes, block, 0, 16)
            val hexString = blockData.joinToString("") { "%02X".format(it) }
            Log.d(TAG, "Block $block: $hexString")
        }
        
        return try {
            // Extract data according to official RFID-Tag-Guide block structure
            
            // Block 2: Filament Type (16 bytes)
            val material = string(data.bytes, 2, 0, 16)
            
            // Block 4: Detailed Filament Type (16 bytes) 
            val detailedFilamentType = string(data.bytes, 4, 0, 16)
            
            // Block 5: Color RGBA (bytes 0-3), Spool Weight (bytes 4-5), Filament Diameter (bytes 8-15)
            val colorBytes = bytes(data.bytes, 5, 0, 4) // RGBA format
            val spoolWeight = int(data.bytes, 5, 4, 2) // uint16 LE
            val filamentDiameter = float(data.bytes, 5, 8, 8)?.let { if (it == 0.0f) 1.75f else it } ?: 1.75f // 8-byte float LE per official guide
            
            val rawColorHex = colorBytes.joinToString("") { "%02X".format(it) }
            debugCollector?.recordColorBytes(colorBytes)
            debugCollector?.recordParsingDetail("spoolWeight", spoolWeight)
            debugCollector?.recordParsingDetail("filamentDiameter", filamentDiameter)
            
            Log.d(TAG, "Raw color bytes: $rawColorHex")
            Log.d(TAG, "Spool weight: $spoolWeight g")
            Log.d(TAG, "Filament diameter: $filamentDiameter mm")
            
            // Block 1: Tray Info Index - Material Variant ID (0-7) and Material ID (8-15)
            val materialVariantId = string(data.bytes, 1, 0, 8)
            val materialId = string(data.bytes, 1, 8, 8)
            
            // Block 6: Temperature and Drying Info
            
            // Debug: Log raw Block 6 bytes for temperature analysis
            val block6RawBytes = bytes(data.bytes, 6, 0, 16)
            val block6Hex = block6RawBytes.joinToString("") { "%02X".format(it) }
            Log.d(TAG, "Block 6 raw bytes: $block6Hex")
            
            // Debug: Check if Block 6 is all zeros (common with authentication failures)
            val isBlock6AllZeros = block6RawBytes.all { it == 0.toByte() }
            if (isBlock6AllZeros) {
                Log.w(TAG, "WARNING: Block 6 is all zeros - possible authentication failure or empty tag")
                debugCollector?.recordError("Block 6 contains only zeros - bed temperature will be 0")
            }
            
            // Run detailed bed temperature debug analysis
            debugCollector?.let { 
                BedTemperatureDebugHelper.analyzeBedTemperatureIssue(data, it)
            }
            
            val dryingTemperature = int(data.bytes, 6, 0, 2) // uint16 LE
            val dryingTime = int(data.bytes, 6, 2, 2) // uint16 LE  
            val bedTemperatureType = int(data.bytes, 6, 4, 2) // uint16 LE (bed temp type)
            val bedTemperature = int(data.bytes, 6, 6, 2) // uint16 LE
            val maxTemperature = int(data.bytes, 6, 8, 2) // uint16 LE
            val minTemperature = int(data.bytes, 6, 10, 2) // uint16 LE
            
            // Debug: Log individual temperature values and raw bytes for each
            val bedTempBytes = bytes(data.bytes, 6, 6, 2)
            val bedTempHex = bedTempBytes.joinToString("") { "%02X".format(it) }
            Log.d(TAG, "Bed temperature raw bytes at offset 6: $bedTempHex -> $bedTemperature°C")
            Log.d(TAG, "Bed temperature type: $bedTemperatureType")
            Log.d(TAG, "Drying temperature: $dryingTemperature°C")
            Log.d(TAG, "Drying time: $dryingTime hours")
            Log.d(TAG, "Min temperature: $minTemperature°C")
            Log.d(TAG, "Max temperature: $maxTemperature°C")
            
            // Debug: Verify bed temperature calculation manually
            if (bedTempBytes.size >= 2) {
                val manualBedTemp = (bedTempBytes[0].toUByte().toInt()) + 
                                   (bedTempBytes[1].toUByte().toInt() shl 8)
                Log.d(TAG, "Manual bed temp calculation: ${bedTempBytes[0].toUByte()} + (${bedTempBytes[1].toUByte()} << 8) = $manualBedTemp")
                if (manualBedTemp != bedTemperature) {
                    Log.w(TAG, "BED TEMP MISMATCH: manual=$manualBedTemp, parsed=$bedTemperature")
                }
            }
            
            // Block 8: X Cam Info (0-11) and Nozzle Diameter (12-15)
            val xCamInfo = bytes(data.bytes, 8, 0, 12)
            val nozzleDiameter = float(data.bytes, 8, 12, 4) ?: 0.4f // float LE
            
            // Block 9: Tray UID (16 bytes) - extract as hex to avoid garbled display
            val trayUid = hexstring(data.bytes, 9, 0, 16)
            
            // Block 10: Spool Width (bytes 4-5 in mm*100)
            val spoolWidthRaw = int(data.bytes, 10, 4, 2) // uint16 LE
            val spoolWidth = spoolWidthRaw / 100.0f // Convert from mm*100 to mm
            
            // Block 12: Production Date/Time (16 bytes)
            val productionDate = datetime(data.bytes, 12, 0)?.toString() ?: "Unknown"
            
            // Block 13: Short Production Date/Time (unknown format - capture both string and hex for analysis)
            val shortProductionDate = string(data.bytes, 13, 0, 16)
            val shortProductionDateHex = hexstring(data.bytes, 13, 0, 16)
            Log.d(TAG, "Block 13 string: '$shortProductionDate'")
            Log.d(TAG, "Block 13 hex: $shortProductionDateHex")
            
            // Block 14: Filament Length (bytes 4-5) 
            val filamentLength = int(data.bytes, 14, 4, 2) // uint16 LE in meters
            
            // Block 16: Extra Color Info for dual-color filaments
            val formatIdentifier = int(data.bytes, 16, 0, 2) // uint16 LE
            val rawColorCount = int(data.bytes, 16, 2, 2) // uint16 LE
            // Default to 1 for single-color filaments when Block 16 is empty/zero
            val colorCount = if (rawColorCount == 0) 1 else rawColorCount
            val secondColorBytes = if (rawColorCount == 2) {
                bytes(data.bytes, 16, 4, 4) // Second color in reverse ABGR format
            } else null
            
            // Block 17: Unknown data (first 2 bytes)
            val unknownBlock17 = bytes(data.bytes, 17, 0, 2)
            
            // Convert RGBA color bytes to hex for display (ignore alpha channel)
            val colorHex = if (colorBytes.size >= 3) {
                val r = colorBytes[0].toUByte().toInt()
                val g = colorBytes[1].toUByte().toInt()
                val b = colorBytes[2].toUByte().toInt()
                val hex = String.format("#%02X%02X%02X", r, g, b)
                Log.d(TAG, "Color RGB: R=$r, G=$g, B=$b -> $hex")
                hex
            } else {
                Log.w(TAG, "Invalid color data, defaulting to black")
                "#000000"
            }
            
            // Handle dual color support
            val finalColorHex = if (rawColorCount == 2 && secondColorBytes != null && secondColorBytes.size >= 3) {
                // Second color is in reverse ABGR format, convert to RGB hex
                val a2 = secondColorBytes[0].toUByte().toInt()
                val b2 = secondColorBytes[1].toUByte().toInt()
                val g2 = secondColorBytes[2].toUByte().toInt()
                val r2 = if (secondColorBytes.size > 3) secondColorBytes[3].toUByte().toInt() else 0
                val secondColorHex = String.format("#%02X%02X%02X", r2, g2, b2)
                Log.d(TAG, "Second color ABGR: A=$a2, B=$b2, G=$g2, R=$r2 -> $secondColorHex")
                "$colorHex / $secondColorHex" // Dual color display
            } else {
                colorHex
            }
            
            debugCollector?.recordParsingDetail("materialVariantId", materialVariantId)
            debugCollector?.recordParsingDetail("materialId", materialId)
            debugCollector?.recordParsingDetail("nozzleDiameter", nozzleDiameter)
            debugCollector?.recordParsingDetail("spoolWidth", spoolWidth)
            debugCollector?.recordParsingDetail("colorCount", colorCount)
            debugCollector?.recordParsingDetail("formatIdentifier", formatIdentifier)
            debugCollector?.recordParsingDetail("shortProductionDate", shortProductionDate)
            debugCollector?.recordParsingDetail("shortProductionDateHex", shortProductionDateHex)
            
            // Debug: Record temperature parsing details
            debugCollector?.recordParsingDetail("bedTemperature", bedTemperature)
            debugCollector?.recordParsingDetail("bedTemperatureType", bedTemperatureType)
            debugCollector?.recordParsingDetail("dryingTemperature", dryingTemperature)
            debugCollector?.recordParsingDetail("dryingTime", dryingTime)
            debugCollector?.recordParsingDetail("minTemperature", minTemperature)
            debugCollector?.recordParsingDetail("maxTemperature", maxTemperature)
            debugCollector?.recordParsingDetail("block6Hex", block6Hex)
            debugCollector?.recordParsingDetail("bedTempHex", bedTempHex)
            
            Log.d(TAG, "Material Variant ID: $materialVariantId")
            Log.d(TAG, "Material ID: $materialId")
            Log.d(TAG, "Nozzle diameter: $nozzleDiameter mm")
            Log.d(TAG, "Spool width: $spoolWidth mm")
            Log.d(TAG, "Color count: $colorCount")
            Log.d(TAG, "Final color: $finalColorHex")
            
            // Generate a basic color name from the hex value (could be enhanced)
            val colorName = getColorName(finalColorHex)
            
            FilamentInfo(
                tagUid = data.uid, // Individual tag UID
                trayUid = trayUid, // Tray UID shared across spool tags
                filamentType = material,
                detailedFilamentType = detailedFilamentType,
                colorHex = finalColorHex,
                colorName = colorName,
                spoolWeight = spoolWeight,
                filamentDiameter = filamentDiameter,
                filamentLength = filamentLength * 1000, // Convert from meters to mm
                productionDate = productionDate,
                minTemperature = minTemperature,
                maxTemperature = maxTemperature,
                bedTemperature = bedTemperature,
                dryingTemperature = dryingTemperature,
                dryingTime = dryingTime,
                // Additional fields from extended parsing
                materialVariantId = materialVariantId,
                materialId = materialId,
                nozzleDiameter = nozzleDiameter,
                spoolWidth = spoolWidth,
                bedTemperatureType = bedTemperatureType,
                shortProductionDate = shortProductionDate,
                colorCount = colorCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tag data", e)
            debugCollector?.recordError("Parsing exception: ${e.message}")
            null
        }
    }
    
    private fun getColorName(colorHex: String): String {
        // Basic color name mapping - could be enhanced with more sophisticated color matching
        return when {
            colorHex.equals("#000000", ignoreCase = true) -> "Black"
            colorHex.equals("#FFFFFF", ignoreCase = true) -> "White"
            colorHex.equals("#FF0000", ignoreCase = true) -> "Red"
            colorHex.equals("#00FF00", ignoreCase = true) -> "Green"
            colorHex.equals("#0000FF", ignoreCase = true) -> "Blue"
            colorHex.equals("#FFFF00", ignoreCase = true) -> "Yellow"
            colorHex.equals("#FF00FF", ignoreCase = true) -> "Magenta"
            colorHex.equals("#00FFFF", ignoreCase = true) -> "Cyan"
            colorHex.startsWith("#FF", ignoreCase = true) -> "Red-ish"
            colorHex.substring(1, 3).equals("00", ignoreCase = true) && 
            colorHex.substring(3, 5) != "00" -> "Green-ish"
            colorHex.substring(3, 5).equals("00", ignoreCase = true) && 
            colorHex.substring(5, 7) != "00" -> "Blue-ish"
            else -> colorHex // Return hex if no match
        }
    }
    
    // Helper functions based on BambuTagDecodeHelpers  
    // Note: MIFARE Classic has sectors, each sector has blocks
    // Sectors 0-31 have 4 blocks each (blocks 0-3, 4-7, 8-11, etc.)
    // We read data sequentially: sector 0 blocks 0-2, sector 1 blocks 0-2, etc.
    // The data array is compressed - trailer blocks (every 4th block) are skipped
    private fun bytes(data: ByteArray, blockNumber: Int, offset: Int, len: Int): ByteArray {
        // Calculate compressed byte offset accounting for skipped trailer blocks
        // For block N, the actual byte offset is (N - N/4) * 16 because we skip every 4th block
        val compressedBlockIndex = blockNumber - (blockNumber / 4) // Number of trailer blocks skipped
        val startIndex = compressedBlockIndex * 16 + offset
        val endIndex = startIndex + len
        
        return if (startIndex >= 0 && startIndex < data.size && endIndex <= data.size) {
            data.copyOfRange(startIndex, endIndex)
        } else {
            ByteArray(len) // Return zeros if out of bounds
        }
    }
    
    private fun hexstring(data: ByteArray, blockNumber: Int, offset: Int, len: Int): String {
        return bytes(data, blockNumber, offset, len).joinToString("") { String.format("%02X", it) }
    }
    
    private fun string(data: ByteArray, blockNumber: Int, offset: Int, len: Int): String {
        return bytes(data, blockNumber, offset, len).toString(Charsets.UTF_8).replace("\u0000", "")
    }
    
    private fun int(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 2): Int {
        val byteArray = bytes(data, blockNumber, offset, len)
        return when (len) {
            4 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).int
            2 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            else -> 0
        }
    }
    
    private fun float(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 8): Float? {
        val byteArray = bytes(data, blockNumber, offset, len)
        return try {
            when (len) {
                8 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).double.toFloat()
                4 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).float
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun datetime(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 16): LocalDateTime? {
        return try {
            val dateString = string(data, blockNumber, offset, len)
            val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm")
            LocalDateTime.parse(dateString, formatter)
        } catch (e: Exception) {
            null
        }
    }
}