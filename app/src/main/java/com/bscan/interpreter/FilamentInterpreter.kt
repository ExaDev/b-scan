package com.bscan.interpreter

import android.util.Log
import com.bscan.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Interprets raw scan data into meaningful filament information using current mappings.
 * This allows the same raw scan data to be re-interpreted as mappings improve,
 * without needing to rescan the NFC tags.
 */
class FilamentInterpreter(
    private val mappings: FilamentMappings = FilamentMappings()
) {
    private val TAG = "FilamentInterpreter"
    
    /**
     * Interpret decrypted scan data into FilamentInfo using current mappings
     */
    fun interpret(decryptedData: DecryptedScanData): FilamentInfo? {
        if (decryptedData.scanResult != ScanResult.SUCCESS) {
            Log.d(TAG, "Skipping interpretation of failed scan: ${decryptedData.scanResult}")
            return null
        }
        
        if (decryptedData.decryptedBlocks.isEmpty()) {
            Log.w(TAG, "No decrypted blocks available for interpretation")
            return null
        }
        
        try {
            return extractFilamentInfo(decryptedData)
        } catch (e: Exception) {
            Log.e(TAG, "Error interpreting decrypted scan data: ${e.message}", e)
            return null
        }
    }
    
    private fun extractFilamentInfo(decryptedData: DecryptedScanData): FilamentInfo {
        // Extract basic string fields from blocks (following RFID-Tag-Guide)
        val trayUid = extractHexString(decryptedData, 9, 0, 16)  // Extract as hex, not UTF-8
        // Debug logging for tray ID extraction
        val block9Hex = decryptedData.decryptedBlocks[9] ?: "MISSING"
        Log.d(TAG, "*** TRAY ID DEBUG: Block 9 hex = $block9Hex")
        Log.d(TAG, "*** TRAY ID DEBUG: Extracted trayUid = '$trayUid'")
        val filamentType = extractString(decryptedData, 2, 0, 16)  
        val detailedFilamentType = extractString(decryptedData, 4, 0, 16)
        val productionDate = extractString(decryptedData, 12, 0, 16)
        
        // Extract numeric data
        val spoolWeight = extractInt(decryptedData, 5, 4, 2)
        val filamentDiameter = extractFloat64(decryptedData, 5, 8)
        val filamentLength = extractInt(decryptedData, 14, 4, 2)
        
        // Extract temperature data from Block 6
        val dryingTemp = extractInt(decryptedData, 6, 0, 2)
        val dryingTime = extractInt(decryptedData, 6, 2, 2)
        val bedTempType = extractInt(decryptedData, 6, 4, 2)
        val bedTemp = extractInt(decryptedData, 6, 6, 2)
        val maxTemp = extractInt(decryptedData, 6, 8, 2)
        val minTemp = extractInt(decryptedData, 6, 10, 2)
        
        // Extract color data
        val colorBytes = extractBytes(decryptedData, 5, 0, 4)
        val colorHex = interpretColor(colorBytes)
        val colorInterpretation = getColorInterpretation(colorHex)
        
        // Extract additional fields
        val materialVariantId = extractString(decryptedData, 1, 0, 8)
        val materialId = extractString(decryptedData, 1, 8, 8)
        val nozzleDiameter = extractFloat32(decryptedData, 8, 12)
        val spoolWidth = extractInt(decryptedData, 10, 4, 2).toFloat() / 100f // mm
        
        // Extract format and color count info
        val formatId = extractInt(decryptedData, 16, 0, 2)
        val colorCount = extractInt(decryptedData, 16, 2, 2)
        
        // Research fields for unknown blocks
        val shortProductionDateHex = extractHex(decryptedData, 13, 0, 16)
        val unknownBlock17Hex = extractHex(decryptedData, 17, 0, 16)
        
        return FilamentInfo(
            tagUid = decryptedData.tagUid,
            trayUid = trayUid,
            filamentType = filamentType,
            detailedFilamentType = detailedFilamentType,
            colorHex = colorHex,
            colorName = colorInterpretation.name,
            spoolWeight = spoolWeight,
            filamentDiameter = filamentDiameter,
            filamentLength = filamentLength,
            productionDate = productionDate,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            bedTemperature = bedTemp,
            dryingTemperature = dryingTemp,
            dryingTime = dryingTime,
            
            // Extended fields
            materialVariantId = materialVariantId,
            materialId = materialId,
            nozzleDiameter = nozzleDiameter,
            spoolWidth = spoolWidth,
            bedTemperatureType = bedTempType,
            shortProductionDate = extractString(decryptedData, 13, 0, 16),
            colorCount = colorCount,
            
            // Research fields
            shortProductionDateHex = shortProductionDateHex,
            unknownBlock17Hex = unknownBlock17Hex
        )
    }
    
    private fun extractString(decryptedData: DecryptedScanData, block: Int, offset: Int, length: Int): String {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ""
        return try {
            val bytes = hexStringToByteArray(blockHex.substring(offset * 2, (offset + length) * 2))
            String(bytes, Charsets.UTF_8).replace("\u0000", "").trim()
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting string from block $block: ${e.message}")
            ""
        }
    }
    
    private fun extractHexString(decryptedData: DecryptedScanData, block: Int, offset: Int, length: Int): String {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ""
        return try {
            // Extract the hex substring directly (no UTF-8 conversion)
            blockHex.substring(offset * 2, (offset + length) * 2)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting hex string from block $block: ${e.message}")
            ""
        }
    }
    
    private fun extractInt(decryptedData: DecryptedScanData, block: Int, offset: Int, length: Int): Int {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return 0
        return try {
            val bytes = hexStringToByteArray(blockHex.substring(offset * 2, (offset + length) * 2))
            when (length) {
                2 -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
                4 -> ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
                else -> 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting int from block $block: ${e.message}")
            0
        }
    }
    
    private fun extractFloat32(decryptedData: DecryptedScanData, block: Int, offset: Int): Float {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return 0f
        return try {
            val bytes = hexStringToByteArray(blockHex.substring(offset * 2, (offset + 4) * 2))
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting float32 from block $block: ${e.message}")
            0f
        }
    }
    
    private fun extractFloat64(decryptedData: DecryptedScanData, block: Int, offset: Int): Float {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return 1.75f
        return try {
            val bytes = hexStringToByteArray(blockHex.substring(offset * 2, (offset + 8) * 2))
            val double = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).double
            double.toFloat().takeIf { it > 0f } ?: 1.75f
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting float64 from block $block: ${e.message}")
            1.75f
        }
    }
    
    private fun extractBytes(decryptedData: DecryptedScanData, block: Int, offset: Int, length: Int): ByteArray {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ByteArray(length)
        return try {
            hexStringToByteArray(blockHex.substring(offset * 2, (offset + length) * 2))
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting bytes from block $block: ${e.message}")
            ByteArray(length)
        }
    }
    
    private fun extractHex(decryptedData: DecryptedScanData, block: Int, offset: Int, length: Int): String {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ""
        return try {
            blockHex.substring(offset * 2, (offset + length) * 2)
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting hex from block $block: ${e.message}")
            ""
        }
    }
    
    private fun interpretColor(colorBytes: ByteArray): String {
        return if (colorBytes.size >= 4) {
            // RGBA format
            String.format("#%02X%02X%02X", 
                colorBytes[0].toInt() and 0xFF,
                colorBytes[1].toInt() and 0xFF, 
                colorBytes[2].toInt() and 0xFF
            )
        } else {
            "#808080" // Default grey
        }
    }
    
    private fun getColorInterpretation(colorHex: String): ColorInterpretation {
        // Try exact match first
        mappings.colorMappings[colorHex.uppercase()]?.let { name ->
            return ColorInterpretation(name, colorHex, 1.0f, ColorSource.EXACT_MATCH)
        }
        
        // Try close match using colour distance
        val closestMatch = findClosestColor(colorHex)
        if (closestMatch.confidence > 0.8f) {
            return closestMatch
        }
        
        // Try HSV analysis
        val hsvName = analyzeColorByHSV(colorHex)
        if (hsvName != null) {
            return ColorInterpretation(hsvName, colorHex, 0.6f, ColorSource.HSV_ANALYSIS)
        }
        
        // Fallback to hex value
        return ColorInterpretation(colorHex, colorHex, 0.1f, ColorSource.FALLBACK)
    }
    
    private fun findClosestColor(targetHex: String): ColorInterpretation {
        val target = hexToRGB(targetHex)
        var bestMatch: Pair<String, String>? = null
        var minDistance = Double.MAX_VALUE
        
        for ((hex, name) in mappings.colorMappings) {
            val color = hexToRGB(hex)
            val distance = colorDistance(target, color)
            
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = hex to name
            }
        }
        
        return if (bestMatch != null && minDistance < 100) { // Threshold for "close"
            val confidence = (100 - minDistance).toFloat() / 100f
            ColorInterpretation(bestMatch.second, targetHex, confidence, ColorSource.CLOSE_MATCH)
        } else {
            ColorInterpretation(targetHex, targetHex, 0.1f, ColorSource.FALLBACK)
        }
    }
    
    private fun analyzeColorByHSV(colorHex: String): String? {
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
            else -> null
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
    
    private fun colorDistance(rgb1: IntArray, rgb2: IntArray): Double {
        val deltaR = rgb1[0] - rgb2[0]
        val deltaG = rgb1[1] - rgb2[1] 
        val deltaB = rgb1[2] - rgb2[2]
        return sqrt((deltaR * deltaR + deltaG * deltaG + deltaB * deltaB).toDouble())
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