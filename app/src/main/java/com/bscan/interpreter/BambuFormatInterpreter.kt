package com.bscan.interpreter

import android.util.Log
import com.bscan.model.*
import com.bscan.data.BambuProductDatabase
import com.bscan.repository.MappingsRepository
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Interprets Bambu Lab's proprietary RFID format using current mappings.
 * This allows the same raw scan data to be re-interpreted as mappings improve,
 * without needing to rescan the NFC tags.
 */
class BambuFormatInterpreter(
    private val mappings: FilamentMappings,
    private val mappingsRepository: MappingsRepository
) : TagInterpreter {
    
    override val tagFormat = TagFormat.BAMBU_PROPRIETARY
    private val TAG = "BambuFormatInterpreter"
    
    override fun getDisplayName(): String = "Bambu Lab Proprietary Format"
    
    /**
     * Check if this interpreter can handle the given decrypted data.
     * Accepts both explicitly tagged BAMBU_PROPRIETARY data and UNKNOWN data that looks like Bambu format.
     */
    override fun canInterpret(decryptedData: DecryptedScanData): Boolean {
        // Accept explicitly tagged Bambu data
        if (decryptedData.tagFormat == TagFormat.BAMBU_PROPRIETARY) {
            return true
        }
        
        // For UNKNOWN format, check if it looks like Mifare Classic 1K with typical Bambu structure
        if (decryptedData.tagFormat == TagFormat.UNKNOWN) {
            return decryptedData.technology.contains("MifareClassic", ignoreCase = true) &&
                   decryptedData.sectorCount == 16 &&
                   decryptedData.tagSizeBytes == 1024 &&
                   decryptedData.decryptedBlocks.isNotEmpty()
        }
        
        return false
    }
    
    /**
     * Interpret decrypted scan data into FilamentInfo using current mappings
     */
    override fun interpret(decryptedData: DecryptedScanData): FilamentInfo? {
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
    
    private fun extractFilamentInfo(decryptedData: DecryptedScanData): FilamentInfo? {
        // Extract RFID codes from Block 1
        val materialId = extractString(decryptedData, 1, 8, 8) // Block 1, bytes 8-15
        val variantId = extractString(decryptedData, 1, 0, 8)  // Block 1, bytes 0-7
        
        if (materialId.isEmpty() || variantId.isEmpty()) {
            Log.w(TAG, "Missing material ID or variant ID from RFID block")
            return null
        }
        
        val rfidCode = "$materialId:$variantId"
        Log.d(TAG, "RFID Code extracted: $rfidCode")
        
        // Look up exact SKU mapping
        val rfidMapping = mappingsRepository.getRfidMappingByCode(materialId, variantId)
        if (rfidMapping == null) {
            Log.w(TAG, "No exact mapping found for RFID code: $rfidCode")
            return null  // Only return results for exact mappings
        }
        
        Log.i(TAG, "Exact SKU match found: ${rfidMapping.sku} for $rfidCode")
        
        // Extract remaining fields for completeness
        val trayUid = extractHexString(decryptedData, 9, 0, 16)
        val productionDate = extractString(decryptedData, 12, 0, 16)
        val spoolWeight = extractInt(decryptedData, 5, 4, 2)
        val filamentDiameter = extractFloat64(decryptedData, 5, 8)
        val filamentLength = extractInt(decryptedData, 14, 4, 2)
        
        // Temperature data from Block 6
        val dryingTemp = extractInt(decryptedData, 6, 0, 2)
        val dryingTime = extractInt(decryptedData, 6, 2, 2)
        val bedTempType = extractInt(decryptedData, 6, 4, 2)
        val bedTemp = extractInt(decryptedData, 6, 6, 2)
        val maxTemp = extractInt(decryptedData, 6, 8, 2)
        val minTemp = extractInt(decryptedData, 6, 10, 2)
        
        // Color from RFID mapping (authoritative) or extract from tag
        val colorHex = rfidMapping.hex ?: interpretColor(extractBytes(decryptedData, 5, 0, 4))
        
        return FilamentInfo(
            tagUid = decryptedData.tagUid,
            trayUid = trayUid,
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            manufacturerName = "Bambu Lab",
            filamentType = rfidMapping.material,
            detailedFilamentType = rfidMapping.material,
            colorHex = colorHex,
            colorName = rfidMapping.color,
            spoolWeight = spoolWeight,
            filamentDiameter = filamentDiameter,
            filamentLength = filamentLength,
            productionDate = productionDate,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            bedTemperature = bedTemp,
            dryingTemperature = dryingTemp,
            dryingTime = dryingTime,
            
            // RFID-specific fields
            exactSku = rfidMapping.sku,
            rfidCode = rfidCode,
            materialVariantId = variantId,
            materialId = materialId,
            nozzleDiameter = extractFloat32(decryptedData, 8, 12),
            spoolWidth = extractInt(decryptedData, 10, 4, 2).toFloat() / 100f,
            bedTemperatureType = bedTempType,
            shortProductionDate = extractString(decryptedData, 13, 0, 16),
            colorCount = extractInt(decryptedData, 16, 2, 2),
            shortProductionDateHex = extractHex(decryptedData, 13, 0, 16),
            unknownBlock17Hex = extractHex(decryptedData, 17, 0, 16)
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
    
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        }
        return data
    }
}