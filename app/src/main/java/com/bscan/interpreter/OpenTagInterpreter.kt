package com.bscan.interpreter

import android.util.Log
import com.bscan.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Interprets OpenTag standard RFID format.
 * OpenTag uses NTAG216 with a standardised memory map starting at 0x10.
 * Reference: OpenTag.md specification
 */
class OpenTagInterpreter : TagInterpreter {
    
    override val tagFormat = TagFormat.OPENTAG_V1
    private val TAG = "OpenTagInterpreter"
    
    override fun getDisplayName(): String = "OpenTag v1 Standard"
    
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
            Log.e(TAG, "Error interpreting OpenTag scan data: ${e.message}", e)
            return null
        }
    }
    
    private fun extractFilamentInfo(decryptedData: DecryptedScanData): FilamentInfo? {
        // OpenTag data typically starts at offset 0x10 (16 bytes) in NTAG memory
        // For simplicity, we'll assume the decrypted blocks represent the raw tag data
        val rawData = reconstructRawData(decryptedData)
        
        if (rawData.size < 144) {
            Log.w(TAG, "Insufficient data for OpenTag parsing: ${rawData.size} bytes")
            return null
        }
        
        // Verify OpenTag signature "OT" at offset 0x10
        val signatureOffset = 0x10
        if (rawData.size <= signatureOffset + 1 || 
            rawData[signatureOffset] != 0x4F.toByte() || 
            rawData[signatureOffset + 1] != 0x54.toByte()) {
            Log.w(TAG, "OpenTag signature not found at expected offset")
            return null
        }
        
        return parseOpenTagData(rawData, decryptedData)
    }
    
    private fun parseOpenTagData(data: ByteArray, decryptedData: DecryptedScanData): FilamentInfo {
        // Parse according to OpenTag memory map specification
        
        // Tag Format (0x10): Should be "OT"
        val tagFormat = String(data.sliceArray(0x10..0x11), Charsets.UTF_8)
        
        // Tag Version (0x12): 2 bytes, big endian
        val tagVersion = ByteBuffer.wrap(data.sliceArray(0x12..0x13)).order(ByteOrder.BIG_ENDIAN).short.toInt()
        
        // Filament Manufacturer (0x14): 16 bytes, UTF-8
        val manufacturerName = String(data.sliceArray(0x14..0x23), Charsets.UTF_8).trim('\u0000')
        
        // Base Material Name (0x24): 5 bytes, UTF-8
        val baseMaterial = String(data.sliceArray(0x24..0x28), Charsets.UTF_8).trim('\u0000')
        
        // Material Modifiers (0x29): 5 bytes, UTF-8
        val materialModifiers = String(data.sliceArray(0x29..0x2D), Charsets.UTF_8).trim('\u0000')
        
        // Color Name (0x2E): 32 bytes, UTF-8
        val colorName = String(data.sliceArray(0x2E..0x4D), Charsets.UTF_8).trim('\u0000')
        
        // Color RGB (0x4E): 3 bytes
        val colorR = data[0x4E].toInt() and 0xFF
        val colorG = data[0x4F].toInt() and 0xFF
        val colorB = data[0x50].toInt() and 0xFF
        val colorHex = String.format("#%02X%02X%02X", colorR, colorG, colorB)
        
        // Diameter (0x51): 2 bytes, big endian, in micrometers
        val diameter = ByteBuffer.wrap(data.sliceArray(0x51..0x52)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        val filamentDiameter = diameter / 1000f // Convert micrometers to mm
        
        // Weight (0x53): 2 bytes, big endian, in grams
        val weight = ByteBuffer.wrap(data.sliceArray(0x53..0x54)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        
        // Print Temp (0x55): 1 byte, multiply by 5 for actual temperature
        val printTemp = (data[0x55].toInt() and 0xFF) * 5
        
        // Bed Temp (0x56): 1 byte, multiply by 5 for actual temperature
        val bedTemp = (data[0x56].toInt() and 0xFF) * 5
        
        // Density (0x57): 2 bytes, big endian, in micrograms per cubic centimetre
        val density = ByteBuffer.wrap(data.sliceArray(0x57..0x58)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        
        // Combine base material and modifiers for filament type
        val filamentType = if (materialModifiers.isNotEmpty()) {
            "$baseMaterial $materialModifiers"
        } else {
            baseMaterial
        }
        
        // Try to extract extended data if available (starts at 0xA0)
        val extendedData = parseExtendedData(data)
        
        return FilamentInfo(
            tagUid = decryptedData.tagUid,
            trayUid = generateTrayUid(manufacturerName, baseMaterial, colorHex),
            tagFormat = TagFormat.OPENTAG_V1,
            manufacturerName = manufacturerName,
            filamentType = filamentType,
            detailedFilamentType = filamentType,
            colorHex = colorHex,
            colorName = colorName.ifEmpty { interpretColorName(colorHex) },
            spoolWeight = weight,
            filamentDiameter = filamentDiameter,
            filamentLength = extendedData?.filamentLength ?: estimateLength(weight, density),
            productionDate = extendedData?.productionDate ?: "Unknown",
            minTemperature = maxOf(printTemp - 20, 150),
            maxTemperature = printTemp,
            bedTemperature = bedTemp,
            dryingTemperature = extendedData?.dryingTemp ?: 45,
            dryingTime = extendedData?.dryingTime ?: 8,
            
            // Extended OpenTag fields
            materialVariantId = extendedData?.serialNumber ?: "",
            materialId = baseMaterial,
            nozzleDiameter = 0.4f, // Default
            spoolWidth = extendedData?.spoolCoreDiameter?.toFloat() ?: 200f,
            bedTemperatureType = 0,
            shortProductionDate = extendedData?.productionDate ?: "",
            colorCount = 1,
            
            // Store tag version and extended info
            shortProductionDateHex = "v$tagVersion",
            unknownBlock17Hex = extendedData?.toString() ?: ""
        )
    }
    
    private fun parseExtendedData(data: ByteArray): OpenTagExtendedData? {
        if (data.size < 0xC0) return null
        
        try {
            // Serial Number / Batch ID (0xA0): 16 bytes
            val serialNumber = String(data.sliceArray(0xA0..0xAF), Charsets.UTF_8).trim('\u0000')
            
            // Manufacture Date (0xB0): 4 bytes (Year: 2 bytes, Month: 1 byte, Day: 1 byte)
            val year = ByteBuffer.wrap(data.sliceArray(0xB0..0xB1)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
            val month = data[0xB2].toInt() and 0xFF
            val day = data[0xB3].toInt() and 0xFF
            val productionDate = String.format("%04d-%02d-%02d", year, month, day)
            
            // Spool Core Diameter (0xB7): 1 byte
            val spoolCoreDiameter = data[0xB7].toInt() and 0xFF
            
            // Filament Length (0xC0): 2 bytes, in meters
            val filamentLength = (ByteBuffer.wrap(data.sliceArray(0xC0..0xC1)).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF) * 1000 // Convert to mm
            
            // Max Dry Temp (0xC4): 1 byte
            val dryingTemp = data[0xC4].toInt() and 0xFF
            
            // Dry Time (0xC5): 1 byte, in hours
            val dryingTime = data[0xC5].toInt() and 0xFF
            
            return OpenTagExtendedData(
                serialNumber = serialNumber,
                productionDate = productionDate,
                spoolCoreDiameter = spoolCoreDiameter,
                filamentLength = filamentLength,
                dryingTemp = dryingTemp,
                dryingTime = dryingTime
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing extended data: ${e.message}")
            return null
        }
    }
    
    private fun reconstructRawData(decryptedData: DecryptedScanData): ByteArray {
        // For NTAG, blocks might be stored differently than Mifare Classic
        // Try to reconstruct the original tag memory layout
        val maxBlock = decryptedData.decryptedBlocks.keys.maxOrNull() ?: return ByteArray(0)
        val rawData = ByteArray((maxBlock + 1) * 16)
        
        decryptedData.decryptedBlocks.forEach { (blockNum, hexData) ->
            try {
                val bytes = hexStringToByteArray(hexData)
                val offset = blockNum * 16
                if (offset + bytes.size <= rawData.size) {
                    bytes.copyInto(rawData, offset)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reconstructing block $blockNum: ${e.message}")
            }
        }
        
        return rawData
    }
    
    private fun generateTrayUid(manufacturer: String, material: String, color: String): String {
        // Generate a consistent tray UID from manufacturer, material, and color
        return "${manufacturer.take(4).uppercase()}_${material.take(3).uppercase()}_${color.removePrefix("#").take(6)}"
    }
    
    private fun estimateLength(weightGrams: Int, densityMicrograms: Int): Int {
        if (weightGrams <= 0 || densityMicrograms <= 0) return 330000 // Default
        
        // Rough estimation: assume 1.75mm diameter
        val radius = 1.75f / 2f // mm
        val crossSectionArea = Math.PI * radius * radius // mm²
        val densityGramsMm3 = densityMicrograms / 1000f / 1000f // Convert to g/mm³
        val volume = weightGrams / densityGramsMm3 // mm³
        val length = volume / crossSectionArea // mm
        
        return length.toInt().coerceIn(10000, 2000000) // Reasonable bounds
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
    
    private data class OpenTagExtendedData(
        val serialNumber: String,
        val productionDate: String,
        val spoolCoreDiameter: Int,
        val filamentLength: Int,
        val dryingTemp: Int,
        val dryingTime: Int
    )
}