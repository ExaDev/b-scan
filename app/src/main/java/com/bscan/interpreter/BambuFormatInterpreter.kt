package com.bscan.interpreter

import android.util.Log
import com.bscan.model.*
import com.bscan.model.TagFormat
import com.bscan.model.TagFormat.*
import com.bscan.repository.UnifiedDataAccess
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
    private val unifiedDataAccess: UnifiedDataAccess
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
            Log.d(TAG, "canInterpret: TRUE for ${decryptedData.tagUid} - explicitly tagged as BAMBU_PROPRIETARY")
            return true
        }
        
        // For UNKNOWN format, check if it looks like Mifare Classic 1K with typical Bambu structure
        if (decryptedData.tagFormat == TagFormat.UNKNOWN) {
            val canInterpret = decryptedData.technology.contains("MifareClassic", ignoreCase = true) &&
                               decryptedData.sectorCount == 16 &&
                               decryptedData.tagSizeBytes == 1024 &&
                               decryptedData.decryptedBlocks.isNotEmpty()
            return canInterpret
        }
        
        Log.d(TAG, "canInterpret: FALSE for ${decryptedData.tagUid} - unsupported format ${decryptedData.tagFormat}")
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
            Log.w(TAG, "Missing material ID or variant ID from RFID block for UID ${decryptedData.tagUid}: materialId='$materialId', variantId='$variantId'")
            Log.w(TAG, "Block 1 hex: ${decryptedData.decryptedBlocks[1]}")
            return null
        }
        
        val rfidCode = "$materialId:$variantId"
        Log.d(TAG, "RFID Code extracted: $rfidCode")
        
        // Extract all block data regardless of mapping availability
        val trayUid = extractTrayUid(decryptedData, 9)
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
        
        // Extract material types from blocks
        val baseFilamentType = cleanMaterialName(extractString(decryptedData, 2, 0, 16)) // Block 2: Base type (PLA, PETG, etc.)
        val detailedFilamentType = cleanMaterialName(extractString(decryptedData, 4, 0, 16)) // Block 4: Detailed type (PLA Matte, etc.)
        
        // Debug logging for extracted block data
        Log.d(TAG, "Extracted from blocks for UID ${decryptedData.tagUid} - Base type: '$baseFilamentType', Detailed type: '$detailedFilamentType'")
        Log.d(TAG, "Block 2 hex: ${decryptedData.decryptedBlocks[2]}, Block 4 hex: ${decryptedData.decryptedBlocks[4]}")
        
        // Extract color from tag
        val extractedColorHex = interpretColor(extractBytes(decryptedData, 5, 0, 4), baseFilamentType)
        
        // Look up exact SKU mapping for enrichment
        val rfidMapping = unifiedDataAccess.getRfidMappingByCode(materialId, variantId)
        
        // Determine final values: use mapping if available, otherwise use extracted values
        val finalFilamentType = rfidMapping?.material ?: baseFilamentType.ifEmpty { "Unknown Material" }
        val finalDetailedType = if (rfidMapping != null) {
            // When we have mapping, use more specific description if available
            val finishType = when {
                detailedFilamentType.contains("Matte", ignoreCase = true) -> " Matte"
                detailedFilamentType.contains("Silk", ignoreCase = true) -> " Silk"  
                detailedFilamentType.contains("Transparent", ignoreCase = true) -> " Transparent"
                detailedFilamentType.contains("Marble", ignoreCase = true) -> " Marble"
                detailedFilamentType.contains("Sparkle", ignoreCase = true) -> " Sparkle"
                else -> ""
            }
            rfidMapping.material + finishType
        } else {
            // No mapping - use extracted detailed type, but only if it's valid
            val validDetailedType = detailedFilamentType.takeIf { it.isNotEmpty() && it.length >= 2 }
            validDetailedType ?: baseFilamentType.ifEmpty { "Unknown Material" }
        }
        val finalColorHex = rfidMapping?.hex ?: extractedColorHex
        val finalColorName = rfidMapping?.color ?: getColorNameFromProducts(extractedColorHex, baseFilamentType)
        
        // Find matching BambuProduct for store links
        val matchingBambuProduct = findMatchingBambuProduct(finalColorHex, finalFilamentType, finalColorName)
        
        if (rfidMapping != null) {
            Log.i(TAG, "SKU enrichment applied: ${rfidMapping.sku} for $rfidCode")
        } else {
            Log.i(TAG, "Using block-extracted data for unmapped RFID code: $rfidCode")
            Log.d(TAG, "Final values: type='$finalFilamentType', detailedType='$finalDetailedType', colorHex='$finalColorHex', colorName='$finalColorName'")
        }
        
        return FilamentInfo(
            tagUid = decryptedData.tagUid,
            trayUid = trayUid,
            tagFormat = TagFormat.BAMBU_PROPRIETARY,
            manufacturerName = "Bambu Lab",
            filamentType = finalFilamentType,
            detailedFilamentType = finalDetailedType,
            colorHex = finalColorHex,
            colorName = finalColorName,
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
            exactSku = rfidMapping?.sku, // Only set if mapping available
            rfidCode = rfidCode,
            materialVariantId = variantId,
            materialId = materialId,
            nozzleDiameter = extractFloat32(decryptedData, 8, 12),
            spoolWidth = extractInt(decryptedData, 10, 4, 2).toFloat() / 100f,
            bedTemperatureType = bedTempType,
            shortProductionDate = extractString(decryptedData, 13, 0, 16),
            colorCount = extractInt(decryptedData, 16, 2, 2),
            shortProductionDateHex = extractHex(decryptedData, 13, 0, 16),
            unknownBlock17Hex = extractHex(decryptedData, 17, 0, 16),
            
            // Store information
            bambuProduct = matchingBambuProduct
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

    /**
     * Extract tray UID from block 9 with robust handling for different data formats
     */
    private fun extractTrayUid(decryptedData: DecryptedScanData, block: Int): String {
        val blockHex = decryptedData.decryptedBlocks[block] ?: return ""
        
        return try {
            // First, try to extract as UTF-8 text (for sample data and some real tags)
            val bytes = hexStringToByteArray(blockHex)
            val utf8String = String(bytes, Charsets.UTF_8).replace("\u0000", "").trim()
            
            // Check if the UTF-8 string looks like a valid tray UID (letters, numbers, common patterns)
            if (utf8String.isNotEmpty() && 
                utf8String.length >= 3 && 
                utf8String.all { it.isLetterOrDigit() || it in listOf('-', '_', ':') } &&
                !utf8String.contains('\uFFFD')) { // No replacement characters
                
                Log.d(TAG, "Extracted tray UID as UTF-8: '$utf8String'")
                return utf8String
            }
            
            // If UTF-8 extraction failed or produced garbage, use hex representation
            // But make it more compact than the raw 32-character hex string
            val compactHex = blockHex.take(16) // Use first 8 bytes (16 hex chars)
            Log.d(TAG, "Using compact hex tray UID: '$compactHex' (original: '$blockHex')")
            compactHex
            
        } catch (e: Exception) {
            Log.w(TAG, "Error extracting tray UID from block $block: ${e.message}")
            // Fallback to tag UID if tray UID extraction completely fails
            decryptedData.tagUid
        }
    }
    
    private fun interpretColor(colorBytes: ByteArray, materialType: String = ""): String {
        return if (colorBytes.size >= 4) {
            val r = colorBytes[0].toInt() and 0xFF
            val g = colorBytes[1].toInt() and 0xFF
            val b = colorBytes[2].toInt() and 0xFF
            
            // Check for valid color (not all zeros or invalid values)
            if (r == 0 && g == 0 && b == 0) {
                // Use material-specific default colors for all-zero values
                getDefaultColorForMaterial(materialType)
            } else {
                String.format("#%02X%02X%02X", r, g, b)
            }
        } else {
            getDefaultColorForMaterial(materialType)
        }
    }
    
    private fun getDefaultColorForMaterial(materialType: String): String {
        return when (materialType.uppercase()) {
            "PLA" -> "#4CAF50" // Green for PLA
            "PETG", "PET" -> "#2196F3" // Blue for PETG  
            "ABS" -> "#FF9800" // Orange for ABS
            "ASA" -> "#9C27B0" // Purple for ASA
            "TPU" -> "#E91E63" // Pink for TPU
            "PC" -> "#607D8B" // Blue grey for PC
            "PA", "NYLON" -> "#795548" // Brown for Nylon
            else -> "#808080" // Default grey for unknown
        }
    }
    
    
    /**
     * Get colour name by looking up products with matching hex and material type
     */
    private fun getColorNameFromProducts(hexColor: String, materialType: String): String {
        Log.d(TAG, "Looking up colour name for hex: $hexColor, material: $materialType")
        
        // Try exact hex match first
        val exactMatchingProducts = unifiedDataAccess.findProducts("bambu", hex = hexColor, materialType = materialType)
        if (exactMatchingProducts.isNotEmpty()) {
            val colorName = exactMatchingProducts.first().getDisplayColorName()
            Log.d(TAG, "Found exact colour match: '$colorName' for hex $hexColor")
            return colorName
        }
        
        // Try with base material type for exact match
        val baseMaterialType = materialType.split("_").firstOrNull() ?: materialType
        if (baseMaterialType != materialType) {
            val baseExactProducts = unifiedDataAccess.findProducts("bambu", hex = hexColor, materialType = baseMaterialType)
            if (baseExactProducts.isNotEmpty()) {
                val colorName = baseExactProducts.first().getDisplayColorName()
                Log.d(TAG, "Found exact base material match: '$colorName' for hex $hexColor")
                return colorName
            }
        }
        
        // If no exact match, try colour similarity matching
        Log.d(TAG, "No exact hex match found, trying colour similarity matching")
        val allProducts = unifiedDataAccess.getProducts("bambu").filter { product ->
            product.materialType.equals(materialType, ignoreCase = true) || 
            product.getBaseMaterialType().equals(baseMaterialType, ignoreCase = true)
        }
        
        val bestMatch = findBestColorMatch(hexColor, allProducts)
        if (bestMatch != null) {
            Log.d(TAG, "Found similar colour match: '${bestMatch.getDisplayColorName()}' (${bestMatch.colorHex}) for hex $hexColor")
            return bestMatch.getDisplayColorName()
        }
        
        Log.d(TAG, "No products found for hex $hexColor and material $materialType")
        return "Unknown Color ($hexColor)"
    }
    
    /**
     * Find the best colour match using colour distance
     */
    private fun findBestColorMatch(targetHex: String, products: List<ProductEntry>): ProductEntry? {
        if (products.isEmpty()) return null
        
        val targetRgb = hexToRgb(targetHex) ?: return null
        var bestMatch: ProductEntry? = null
        var bestDistance = Double.MAX_VALUE
        
        for (product in products) {
            product.colorHex?.let { productHex ->
                val productRgb = hexToRgb(productHex)
                if (productRgb != null) {
                    val distance = colorDistance(targetRgb, productRgb)
                    if (distance < bestDistance && distance < 100.0) { // Only accept reasonably close colours
                        bestDistance = distance
                        bestMatch = product
                    }
                }
            }
        }
        
        return bestMatch
    }
    
    /**
     * Find matching BambuProduct for store links
     */
    private fun findMatchingBambuProduct(colorHex: String, materialType: String, colorName: String): BambuProduct? {
        Log.d(TAG, "Finding BambuProduct for hex: $colorHex, material: $materialType, color: $colorName")
        
        // Try to find exact match by hex and material type
        val matchingProducts = unifiedDataAccess.findProducts("bambu", hex = colorHex, materialType = materialType)
        if (matchingProducts.isNotEmpty()) {
            val product = matchingProducts.first()
            Log.d(TAG, "Found exact product match: ${product.productName} - ${product.colorName}")
            return convertProductToBambuProduct(product)
        }
        
        // Try with base material type
        val baseMaterialType = materialType.split("_").firstOrNull() ?: materialType
        if (baseMaterialType != materialType) {
            val baseProducts = unifiedDataAccess.findProducts("bambu", hex = colorHex, materialType = baseMaterialType)
            if (baseProducts.isNotEmpty()) {
                val product = baseProducts.first()
                Log.d(TAG, "Found base material product match: ${product.productName} - ${product.colorName}")
                return convertProductToBambuProduct(product)
            }
        }
        
        // Try color name matching if hex matching failed
        val colorNameProducts = unifiedDataAccess.findProducts("bambu", materialType = materialType).filter { product ->
            product.getDisplayColorName().equals(colorName, ignoreCase = true)
        }
        if (colorNameProducts.isNotEmpty()) {
            val product = colorNameProducts.first()
            Log.d(TAG, "Found color name product match: ${product.productName} - ${product.colorName}")
            return convertProductToBambuProduct(product)
        }
        
        Log.d(TAG, "No matching BambuProduct found")
        return null
    }
    
    /**
     * Convert ProductEntry to BambuProduct
     */
    private fun convertProductToBambuProduct(product: ProductEntry): BambuProduct {
        return BambuProduct(
            productLine = product.productName,
            colorName = product.getDisplayColorName(),
            internalCode = product.colorCode ?: "",
            retailSku = product.variantId,
            colorHex = product.colorHex ?: "",
            spoolUrl = product.url,
            refillUrl = null, // For now, we only have the one URL from the catalog
            mass = "1kg" // Default, could be enhanced from product name parsing
        )
    }
    
    /**
     * Calculate Euclidean distance between two RGB colours
     */
    private fun colorDistance(rgb1: Triple<Int, Int, Int>, rgb2: Triple<Int, Int, Int>): Double {
        val (r1, g1, b1) = rgb1
        val (r2, g2, b2) = rgb2
        return kotlin.math.sqrt(
            ((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)).toDouble()
        )
    }
    
    /**
     * Convert hex colour to RGB triple
     */
    private fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
        val cleanHex = hex.removePrefix("#")
        if (cleanHex.length != 6) return null
        
        return try {
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16) 
            val b = cleanHex.substring(4, 6).toInt(16)
            Triple(r, g, b)
        } catch (e: NumberFormatException) {
            null
        }
    }
    
    /**
     * Clean and normalize material names extracted from RFID blocks
     */
    private fun cleanMaterialName(materialName: String): String {
        return materialName
            .replace("\u0000", "") // Remove null characters
            .replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "") // Remove control characters except CR/LF/Tab
            .replace(Regex("[\\uFFFD\\uFEFF]"), "") // Remove replacement characters and BOM
            .filter { char -> 
                // Only keep printable ASCII, common punctuation, and valid Unicode letters/digits
                char.isLetterOrDigit() || char.isWhitespace() || char in " +-_()[]{}.,;:"
            }
            .trim() // Remove leading/trailing whitespace
            .takeIf { it.isNotEmpty() && it.all { char -> !char.isISOControl() } } // Only return if non-empty and no control chars
            ?: ""
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