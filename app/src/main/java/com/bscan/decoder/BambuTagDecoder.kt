package com.bscan.decoder

import android.util.Log
import com.bscan.debug.DebugDataCollector
import com.bscan.model.FilamentInfo
import com.bscan.model.NfcTagData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
            
            // Block 5: Color RGBA (bytes 0-3), Spool Weight (bytes 4-5), Filament Diameter (bytes 8-11)
            val colorBytes = bytes(data.bytes, 5, 0, 4) // RGBA format
            val spoolWeight = int(data.bytes, 5, 4, 2) // uint16 LE
            val filamentDiameter = float(data.bytes, 5, 8, 4) ?: 1.75f // float LE
            
            val rawColorHex = colorBytes.joinToString("") { "%02X".format(it) }
            debugCollector?.recordColorBytes(colorBytes)
            debugCollector?.recordParsingDetail("spoolWeight", spoolWeight)
            debugCollector?.recordParsingDetail("filamentDiameter", filamentDiameter)
            
            Log.d(TAG, "Raw color bytes: $rawColorHex")
            Log.d(TAG, "Spool weight: $spoolWeight g")
            Log.d(TAG, "Filament diameter: $filamentDiameter mm")
            
            // Block 6: Temperature and Drying Info
            val dryingTemperature = int(data.bytes, 6, 0, 2) // uint16 LE
            val dryingTime = int(data.bytes, 6, 2, 2) // uint16 LE  
            val bedTemperature = int(data.bytes, 6, 6, 2) // uint16 LE
            val maxTemperature = int(data.bytes, 6, 8, 2) // uint16 LE
            val minTemperature = int(data.bytes, 6, 10, 2) // uint16 LE
            
            // Block 9: Tray UID (16 bytes)
            val trayUid = string(data.bytes, 9, 0, 16)
            
            // Block 12: Production Date/Time (16 bytes)
            val productionDate = datetime(data.bytes, 12, 0)?.toString() ?: "Unknown"
            
            // Block 14: Filament Length (bytes 4-5) 
            val filamentLength = int(data.bytes, 14, 4, 2) // uint16 LE
            
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
            
            // Generate a basic color name from the hex value (could be enhanced)
            val colorName = getColorName(colorHex)
            
            FilamentInfo(
                uid = data.uid,
                trayUid = trayUid,
                filamentType = material,
                detailedFilamentType = detailedFilamentType,
                colorHex = colorHex,
                colorName = colorName,
                spoolWeight = spoolWeight,
                filamentDiameter = filamentDiameter,
                filamentLength = filamentLength,
                productionDate = productionDate,
                minTemperature = minTemperature,
                maxTemperature = maxTemperature,
                bedTemperature = bedTemperature,
                dryingTemperature = dryingTemperature,
                dryingTime = dryingTime
            )
        } catch (e: Exception) {
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
    private fun bytes(data: ByteArray, blockNumber: Int, offset: Int, len: Int): ByteArray {
        // Calculate actual offset based on MIFARE Classic layout
        // Each sector contributes 3 data blocks (48 bytes), skipping trailer blocks
        val sector = blockNumber / 4
        val blockInSector = blockNumber % 4
        
        // Skip if this is a trailer block (every 4th block contains keys)
        if (blockInSector == 3) {
            return ByteArray(len) // Return zeros for trailer blocks
        }
        
        val startIndex = sector * 48 + blockInSector * 16 + offset
        val endIndex = startIndex + len
        
        return if (startIndex < data.size && endIndex <= data.size) {
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