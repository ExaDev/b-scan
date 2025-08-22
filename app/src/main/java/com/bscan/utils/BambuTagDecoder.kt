package com.bscan.utils

import com.bscan.model.FilamentInfo
import com.bscan.model.NfcTagData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

object BambuTagDecoder {
    
    fun parseTag(tagData: NfcTagData): FilamentInfo? {
        if (tagData.bytes.size < 80) {
            return null // Insufficient data
        }
        
        return try {
            val bytes = tagData.bytes
            
            FilamentInfo(
                uid = tagData.uid,
                trayUid = extractTrayUid(bytes),
                filamentType = extractFilamentType(bytes),
                detailedFilamentType = extractDetailedFilamentType(bytes),
                colorHex = extractColorHex(bytes),
                colorName = extractColorName(bytes),
                spoolWeight = extractSpoolWeight(bytes),
                filamentDiameter = extractFilamentDiameter(bytes),
                filamentLength = extractFilamentLength(bytes),
                productionDate = extractProductionDate(bytes),
                minTemperature = extractMinTemperature(bytes),
                maxTemperature = extractMaxTemperature(bytes),
                bedTemperature = extractBedTemperature(bytes),
                dryingTemperature = extractDryingTemperature(bytes),
                dryingTime = extractDryingTime(bytes)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun extractTrayUid(bytes: ByteArray): String {
        // Block 9: Tray UID (16 bytes hex)
        val blockOffset = 9 * 16 // Block 9
        return if (blockOffset + 16 <= bytes.size) {
            bytesToHex(bytes, blockOffset, 16)
        } else {
            "Unknown"
        }
    }
    
    private fun extractFilamentType(bytes: ByteArray): String {
        // Block 2: Filament Type (16 bytes string)
        val blockOffset = 2 * 16 // Block 2
        return extractString(bytes, blockOffset, 16)
    }
    
    private fun extractDetailedFilamentType(bytes: ByteArray): String {
        // Block 4: Detailed Filament Type (16 bytes string)
        val blockOffset = 4 * 16 // Block 4
        return extractString(bytes, blockOffset, 16)
    }
    
    private fun extractColorHex(bytes: ByteArray): String {
        // Block 5: Color (first 4 bytes RGBA)
        val blockOffset = 5 * 16 // Block 5
        return if (blockOffset + 4 <= bytes.size) {
            bytesToHex(bytes, blockOffset, 4)
        } else {
            "000000FF"
        }
    }
    
    private fun extractColorName(bytes: ByteArray): String {
        val colorHex = extractColorHex(bytes)
        
        // Check alpha channel (last 2 characters in RGBA format)
        if (colorHex.length >= 8) {
            val alpha = colorHex.takeLast(2).lowercase()
            if (alpha == "00") {
                return "Clear/Transparent"
            }
            // Warning: I do not have a nonclear transparent filament to test behaviour.
        }
        
        // Simple color name mapping based on RGB values (first 6 chars)
        return when (colorHex.take(6).lowercase()) {
            "ff0000" -> "Red"
            "00ff00" -> "Green"
            "0000ff" -> "Blue"
            "ffff00" -> "Yellow"
            "ff00ff" -> "Magenta"
            "00ffff" -> "Cyan"
            "000000" -> "Black"
            "ffffff" -> "White"
            else -> "Custom (#${colorHex.take(6)})"
        }
    }
    
    private fun extractSpoolWeight(bytes: ByteArray): Int {
        // Block 5: Spool Weight (bytes 4-5, little endian)
        val blockOffset = 5 * 16 + 4
        return if (blockOffset + 2 <= bytes.size) {
            ByteBuffer.wrap(bytes, blockOffset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .short
                .toInt()
        } else {
            1000 // Default 1000g
        }
    }
    
    private fun extractFilamentDiameter(bytes: ByteArray): Float {
        // Block 5: Filament Diameter (bytes 8-11, float little endian)
        val blockOffset = 5 * 16 + 8
        return if (blockOffset + 4 <= bytes.size) {
            ByteBuffer.wrap(bytes, blockOffset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .float
        } else {
            1.75f // Default diameter
        }
    }
    
    private fun extractFilamentLength(bytes: ByteArray): Int {
        // Block 14: Filament Length (4 bytes little endian)
        val blockOffset = 14 * 16
        return if (blockOffset + 4 <= bytes.size) {
            ByteBuffer.wrap(bytes, blockOffset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int
        } else {
            0
        }
    }
    
    private fun extractProductionDate(bytes: ByteArray): String {
        // Block 12: Production Date/Time
        val blockOffset = 12 * 16
        return if (blockOffset + 4 <= bytes.size) {
            try {
                val timestamp = ByteBuffer.wrap(bytes, blockOffset, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
                    .toLong()
                
                val date = Date(timestamp * 1000L)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                "Unknown"
            }
        } else {
            "Unknown"
        }
    }
    
    private fun extractMinTemperature(bytes: ByteArray): Int {
        // Block 6: Min Temperature (bytes 10-11, little endian)
        val blockOffset = 6 * 16 + 10
        return extractTemperature(bytes, blockOffset, 200) // Default 200°C
    }
    
    private fun extractMaxTemperature(bytes: ByteArray): Int {
        // Block 6: Max Temperature (bytes 8-9, little endian)
        val blockOffset = 6 * 16 + 8
        return extractTemperature(bytes, blockOffset, 220) // Default 220°C
    }
    
    private fun extractBedTemperature(bytes: ByteArray): Int {
        // Block 6: Bed Temperature (bytes 6-7, little endian)
        val blockOffset = 6 * 16 + 6
        android.util.Log.d("BambuTagDecoder", "Extracting bed temperature from block 6, offset $blockOffset (bytes ${blockOffset}-${blockOffset+1})")
        
        // Debug: Show entire block 6 contents
        val block6Start = 6 * 16
        if (block6Start + 16 <= bytes.size) {
            val block6Hex = bytes.sliceArray(block6Start until block6Start + 16)
                .joinToString(" ") { "%02X".format(it) }
            android.util.Log.d("BambuTagDecoder", "Block 6 contents: $block6Hex")
        }
        
        return extractTemperature(bytes, blockOffset, 60) // Default 60°C
    }
    
    private fun extractDryingTemperature(bytes: ByteArray): Int {
        // Block 6: Drying Temperature (bytes 0-1, little endian)
        val blockOffset = 6 * 16
        return extractTemperature(bytes, blockOffset, 45) // Default 45°C
    }
    
    private fun extractDryingTime(bytes: ByteArray): Int {
        // Block 6: Drying Time (bytes 2-3, little endian)
        val blockOffset = 6 * 16 + 2
        return if (blockOffset + 2 <= bytes.size) {
            ByteBuffer.wrap(bytes, blockOffset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .short
                .toInt()
        } else {
            8 // Default 8 hours
        }
    }
    
    private fun extractTemperature(bytes: ByteArray, offset: Int, default: Int): Int {
        return if (offset + 2 <= bytes.size) {
            val byte1 = bytes[offset].toUByte().toInt()
            val byte2 = bytes[offset + 1].toUByte().toInt()
            val rawValue = ByteBuffer.wrap(bytes, offset, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .short
                .toInt()
            
            // Debug logging for temperature extraction
            android.util.Log.d("BambuTagDecoder", "Temperature at offset $offset: byte1=$byte1, byte2=$byte2, rawValue=$rawValue, default=$default")
            
            // Check if the raw value seems reasonable for temperature
            // Note: Bed temperature can legitimately be 0 for some filaments
            if (rawValue >= 0 && rawValue < 500) {
                rawValue
            } else {
                android.util.Log.w("BambuTagDecoder", "Temperature value $rawValue seems unreasonable, using default $default")
                default
            }
        } else {
            android.util.Log.w("BambuTagDecoder", "Temperature offset $offset out of bounds, using default $default")
            default
        }
    }
    
    private fun extractString(bytes: ByteArray, offset: Int, length: Int): String {
        return if (offset + length <= bytes.size) {
            val stringBytes = bytes.copyOfRange(offset, offset + length)
            // Remove null terminators and decode as ASCII
            val nullIndex = stringBytes.indexOf(0)
            val actualBytes = if (nullIndex >= 0) {
                stringBytes.copyOfRange(0, nullIndex)
            } else {
                stringBytes
            }
            String(actualBytes, Charsets.US_ASCII).trim()
        } else {
            "Unknown"
        }
    }
    
    private fun bytesToHex(bytes: ByteArray, offset: Int, length: Int): String {
        return if (offset + length <= bytes.size) {
            bytes.copyOfRange(offset, offset + length)
                .joinToString("") { "%02X".format(it) }
        } else {
            ""
        }
    }
}