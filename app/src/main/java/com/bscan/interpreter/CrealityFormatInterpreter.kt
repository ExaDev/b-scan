package com.bscan.interpreter

import android.util.Log
import com.bscan.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Interprets Creality's ASCII-encoded RFID format.
 * Creality stores data in blocks 4-6 as ASCII text following the pattern:
 * "AAA BBBBB CCCC DDDDD #EEEEEE FFFFFF GGGGGGGGGGGGGGGGGG"
 */
class CrealityFormatInterpreter : TagInterpreter {
    
    override val tagFormat = TagFormat.CREALITY_ASCII
    private val TAG = "CrealityFormatInterpreter"
    
    override fun getDisplayName(): String = "Creality ASCII Format"
    
    /**
     * Interpret decrypted scan data into FilamentInfo
     */
    override fun interpret(decryptedData: DecryptedScanData): FilamentInfo? {
        if (decryptedData.scanResult != ScanResult.SUCCESS) {
            Log.d(TAG, "Skipping interpretation of failed scan: ${decryptedData.scanResult}")
            return null
        }
        
        try {
            return extractFilamentInfo(decryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error interpreting Creality scan data: ${e.message}", e)
            return null
        }
    }
    
    private fun extractFilamentInfo(decryptedData: DecryptedScanData): FilamentInfo? {
        // Extract ASCII data from blocks 4-6 (following CrealityRfid.md specification)
        val block4Data = extractString(decryptedData, 4)
        val block5Data = extractString(decryptedData, 5)
        val block6Data = extractString(decryptedData, 6)
        
        // Combine the blocks into a single string
        val combinedData = (block4Data + block5Data + block6Data).trim()
        
        if (combinedData.isEmpty()) {
            Log.w(TAG, "No ASCII data found in blocks 4-6")
            return null
        }
        
        Log.d(TAG, "Combined Creality data: '$combinedData'")
        
        // Parse the ASCII data following Creality pattern:
        // AAA BBBBB CCCC DDDDD #EEEEEE FFFFFF GGGGGGGGGGGGGGGGGG
        return parseCrealityData(combinedData, decryptedData)
    }
    
    private fun parseCrealityData(data: String, decryptedData: DecryptedScanData): FilamentInfo? {
        try {
            // Split on whitespace to get components
            val components = data.split("\\s+".toRegex())
            
            if (components.size < 6) {
                Log.w(TAG, "Insufficient data components: ${components.size}, expected at least 6")
                return null
            }
            
            // Parse according to Creality specification
            val batchNumber = components[0] // AAA (3 hex digits)
            val manufacturingDate = parseManufacturingDate(components[1]) // BBBBB (YYMDD)
            val supplierId = components[2] // CCCC (4 hex digits)
            val materialId = components[3] // DDDDD (5 hex digits)
            val colorHex = parseColorHex(components[4]) // #EEEEEE (RGB color)
            val spoolId = components[5] // FFFFFF (5 hex digits)
            // Components[6] and beyond are unknown data (GGGGGGGGGGGGGGGGGG)
            
            // Map material ID to filament type (this would need a lookup table in practice)
            val filamentType = mapMaterialIdToType(materialId)
            
            // Create color name from hex
            val colorName = interpretColorName(colorHex)
            
            return FilamentInfo(
                tagUid = decryptedData.tagUid,
                trayUid = spoolId, // Use spool ID as tray identifier
                tagFormat = TagFormat.CREALITY_ASCII,
                manufacturerName = decryptedData.manufacturerName,
                filamentType = filamentType,
                detailedFilamentType = filamentType, // Same as base type for Creality
                colorHex = colorHex,
                colorName = colorName,
                spoolWeight = 1000, // Default, not stored in Creality format
                filamentDiameter = 1.75f, // Default, not stored in Creality format
                filamentLength = 330000, // Default, not stored in Creality format
                productionDate = manufacturingDate,
                minTemperature = 190, // Default values for common materials
                maxTemperature = 220,
                bedTemperature = 60,
                dryingTemperature = 45,
                dryingTime = 8,
                
                // Creality-specific extended fields
                materialVariantId = batchNumber,
                materialId = materialId,
                nozzleDiameter = 0.4f,
                spoolWidth = 200f,
                bedTemperatureType = 0,
                shortProductionDate = manufacturingDate,
                colorCount = 1,
                
                // Store raw data for debugging
                shortProductionDateHex = supplierId,
                unknownBlock17Hex = if (components.size > 6) components.drop(6).joinToString(" ") else ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Creality data: ${e.message}", e)
            return null
        }
    }
    
    private fun extractString(decryptedData: DecryptedScanData, block: Int): String {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ""
        return try {
            val bytes = hexStringToByteArray(blockHex)
            String(bytes, Charsets.UTF_8).replace("\u0000", "").trim()
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting string from block $block: ${e.message}")
            ""
        }
    }
    
    private fun parseManufacturingDate(dateStr: String): String {
        try {
            if (dateStr.length != 5) return dateStr
            
            // Parse YYMDD format
            val year = 2000 + dateStr.substring(0, 2).toInt()
            val month = dateStr.substring(2, 3).toInt()
            val day = dateStr.substring(3, 5).toInt()
            
            return String.format("%04d-%02d-%02d", year, month, day)
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing manufacturing date: $dateStr, ${e.message}")
            return dateStr
        }
    }
    
    private fun parseColorHex(colorStr: String): String {
        // Remove # if present and ensure 6-character hex
        val cleanColor = colorStr.removePrefix("#").uppercase()
        return if (cleanColor.length == 6 && cleanColor.matches("[0-9A-F]+".toRegex())) {
            "#$cleanColor"
        } else {
            "#808080" // Default grey if invalid
        }
    }
    
    private fun mapMaterialIdToType(materialId: String): String {
        // This would ideally be a lookup table based on known Creality material IDs
        // For now, return a generic description
        return when {
            materialId.startsWith("01") -> "PLA"
            materialId.startsWith("02") -> "ABS"
            materialId.startsWith("03") -> "PETG"
            materialId.startsWith("04") -> "TPU"
            else -> "Unknown Material (ID: $materialId)"
        }
    }
    
    private fun interpretColorName(colorHex: String): String {
        try {
            val rgb = hexToRGB(colorHex)
            val hsv = rgbToHSV(rgb)
            
            return when {
                hsv[2] < 0.2 -> "Black"
                hsv[2] > 0.9 && hsv[1] < 0.1 -> "White"
                hsv[1] < 0.2 -> "Grey"
                hsv[0] < 30 || hsv[0] > 330 -> "Red"
                hsv[0] < 90 -> "Yellow"
                hsv[0] < 150 -> "Green"
                hsv[0] < 210 -> "Cyan"
                hsv[0] < 270 -> "Blue"
                hsv[0] < 330 -> "Magenta"
                else -> "Unknown Color"
            }
        } catch (e: Exception) {
            return "Unknown Color"
        }
    }
    
    private fun hexToRGB(hex: String): IntArray {
        val color = hex.removePrefix("#")
        return intArrayOf(
            color.substring(0, 2).toInt(16),
            color.substring(2, 4).toInt(16),
            color.substring(4, 6).toInt(16)
        )
    }
    
    private fun rgbToHSV(rgb: IntArray): FloatArray {
        val r = rgb[0] / 255f
        val g = rgb[1] / 255f  
        val b = rgb[2] / 255f
        
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        
        val h = when {
            delta == 0f -> 0f
            max == r -> ((g - b) / delta) % 6 * 60
            max == g -> ((b - r) / delta + 2) * 60
            else -> ((r - g) / delta + 4) * 60
        }
        
        val s = if (max == 0f) 0f else delta / max
        val v = max
        
        return floatArrayOf(if (h < 0) h + 360 else h, s, v)
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}