package com.bscan.data.bambu.rfid

import com.bscan.model.SkuInfo
import com.bscan.data.bambu.base.MappingInfo
import com.bscan.data.bambu.BambuVariantSkuMapper

/**
 * Composite resolver for Bambu Lab RFID mapping components.
 * 
 * Combines MaterialIdMapper, SeriesCodeMapper, and ColorCodeMapper to provide
 * comprehensive RFID tag data interpretation for Bambu filament products.
 * 
 * Handles the complete "MaterialID:SeriesCode-ColorCode" format parsing and resolution.
 */
object BambuRfidMappingResolver {
    
    /**
     * Complete RFID mapping result containing all resolved components
     */
    data class RfidMappingResult(
        // Raw components from tag
        val materialId: String,
        val seriesCode: String,
        val colourCode: String,
        val fullRfidKey: String,  // "MaterialID:SeriesCode-ColorCode"
        
        // Resolved information
        val materialInfo: MappingInfo?,
        val seriesInfo: MappingInfo?,
        val colourInfo: MappingInfo?,
        val skuInfo: SkuInfo?,  // NEW: SKU information
        
        // Computed properties (hex colour comes from RFID Block 5, not mapping)
        val displayName: String,
        val fullDescription: String,
        val filamentCode: String?,  // NEW: 5-digit SKU/filament code
        val isKnownProduct: Boolean,
        val warnings: List<String> = emptyList()
    )
    
    /**
     * Parse and resolve a complete RFID key
     * @param rfidKey The full RFID key in format "MaterialID:SeriesCode-ColorCode" (e.g., "GFL00:A00-K0")
     * @return RfidMappingResult with all resolved components
     */
    fun resolveRfidKey(rfidKey: String): RfidMappingResult {
        val (materialId, seriesCode, colourCode) = parseRfidKey(rfidKey)
        
        return resolveComponents(materialId, seriesCode, colourCode, rfidKey)
    }
    
    /**
     * Parse and resolve individual components
     * @param materialId Material ID from Block 1 bytes 8-15
     * @param seriesCode Series code from Block 1 bytes 0-7 (before dash)
     * @param colourCode Colour code from Block 1 bytes 0-7 (after dash)
     * @return RfidMappingResult with all resolved components
     */
    fun resolveComponents(
        materialId: String,
        seriesCode: String,
        colourCode: String,
        originalKey: String? = null
    ): RfidMappingResult {
        val fullRfidKey = originalKey ?: "$materialId:$seriesCode-$colourCode"
        val warnings = mutableListOf<String>()
        
        // Resolve each component
        val materialInfo = BambuMaterialIdMapper.getMaterialInfo(materialId)
        val seriesInfo = BambuSeriesCodeMapper.getSeriesInfo(seriesCode)
        val colourInfo = BambuColorCodeMapper.getColorInfoForMaterial(colourCode, materialId)
        val variantId = "$seriesCode-$colourCode"
        val rfidKey = "$materialId:$variantId"
        val skuInfo = BambuVariantSkuMapper.getSkuByRfidKey(rfidKey)
        
        // Generate warnings for unknown components
        if (materialInfo == null) {
            warnings.add("Unknown material ID: $materialId")
        }
        if (seriesInfo == null) {
            warnings.add("Unknown series code: $seriesCode")
        }
        if (colourInfo == null) {
            warnings.add("Unknown colour code: $colourCode")
        }
        if (skuInfo == null) {
            warnings.add("Unknown variant ID: $variantId")
        }
        
        // Compute display properties
        val materialName = materialInfo?.displayName ?: "Unknown Material"
        val seriesName = seriesInfo?.displayName ?: "Unknown Series"
        val colourName = colourInfo?.displayName ?: "Unknown Colour"
        
        val displayName = "$materialName $colourName"
        val fullDescription = buildDescription(materialInfo, seriesInfo, colourInfo, materialName, seriesName, colourName)
        val filamentCode = skuInfo?.sku
        
        val isKnownProduct = materialInfo != null && seriesInfo != null && colourInfo != null
        
        return RfidMappingResult(
            materialId = materialId,
            seriesCode = seriesCode,
            colourCode = colourCode,
            fullRfidKey = fullRfidKey,
            materialInfo = materialInfo,
            seriesInfo = seriesInfo,
            colourInfo = colourInfo,
            skuInfo = skuInfo,
            displayName = displayName,
            fullDescription = fullDescription,
            filamentCode = filamentCode,
            isKnownProduct = isKnownProduct,
            warnings = warnings
        )
    }
    
    /**
     * Parse RFID key string into components
     * @param rfidKey The RFID key string (e.g., "GFL00:A00-K0")
     * @return Triple of (materialId, seriesCode, colourCode)
     * @throws IllegalArgumentException if format is invalid
     */
    fun parseRfidKey(rfidKey: String): Triple<String, String, String> {
        val colonIndex = rfidKey.indexOf(':')
        if (colonIndex == -1) {
            throw IllegalArgumentException("Invalid RFID key format: missing ':' separator. Expected 'MaterialID:SeriesCode-ColorCode'")
        }
        
        val materialId = rfidKey.substring(0, colonIndex)
        val variantPart = rfidKey.substring(colonIndex + 1)
        
        val dashIndex = variantPart.indexOf('-')
        if (dashIndex == -1) {
            throw IllegalArgumentException("Invalid RFID key format: missing '-' separator in variant part. Expected 'MaterialID:SeriesCode-ColorCode'")
        }
        
        val seriesCode = variantPart.substring(0, dashIndex)
        val colourCode = variantPart.substring(dashIndex + 1)
        
        if (materialId.isBlank() || seriesCode.isBlank() || colourCode.isBlank()) {
            throw IllegalArgumentException("Invalid RFID key format: empty components not allowed")
        }
        
        return Triple(materialId, seriesCode, colourCode)
    }
    
    /**
     * Validate RFID key format without full resolution
     * @param rfidKey The RFID key to validate
     * @return true if format is valid, false otherwise
     */
    fun isValidRfidKeyFormat(rfidKey: String): Boolean {
        return try {
            parseRfidKey(rfidKey)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * Check if all components of an RFID key are known
     * @param rfidKey The RFID key to check
     * @return true if all components are in the databases
     */
    fun isFullyKnownProduct(rfidKey: String): Boolean {
        return try {
            val result = resolveRfidKey(rfidKey)
            result.isKnownProduct
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * Get suggestions for similar known products
     * @param materialId The material ID to find alternatives for
     * @param maxSuggestions Maximum number of suggestions to return
     * @return List of similar RFID keys that are known
     */
    fun getSimilarKnownProducts(materialId: String, maxSuggestions: Int = 5): List<String> {
        val suggestions = mutableListOf<String>()
        
        // If material is known, suggest common variants
        if (BambuMaterialIdMapper.isKnownMaterial(materialId)) {
            val commonSeries = BambuSeriesCodeMapper.getAllKnownSeriesCodes().take(3).toList()
            val commonColours = BambuColorCodeMapper.getAllKnownColourCodes().take(4).toList()
            
            for (series in commonSeries) {
                for (colour in commonColours) {
                    val candidate = "$materialId:$series-$colour"
                    if (isFullyKnownProduct(candidate)) {
                        suggestions.add(candidate)
                        if (suggestions.size >= maxSuggestions) break
                    }
                }
                if (suggestions.size >= maxSuggestions) break
            }
        }
        
        return suggestions
    }
    
    /**
     * Build comprehensive description from resolved components
     */
    private fun buildDescription(
        materialInfo: MappingInfo?,
        seriesInfo: MappingInfo?,
        colourInfo: MappingInfo?,
        materialName: String,
        seriesName: String,
        colourName: String
    ): String {
        // Simple description combining display names
        return "$materialName ($seriesName) in $colourName"
    }
    
    /**
     * Extract variant ID from Block 1 raw data
     * @param block1Data The raw 16-byte Block 1 data from RFID tag
     * @return Triple of (materialId, seriesCode, colourCode) or null if parsing fails
     */
    fun extractFromBlock1Data(block1Data: ByteArray): Triple<String, String, String>? {
        if (block1Data.size < 16) return null
        
        return try {
            // Extract series-colour (bytes 0-7) and material ID (bytes 8-15)
            val variantBytes = block1Data.sliceArray(0..7)
            val materialBytes = block1Data.sliceArray(8..15)
            
            // Convert to strings, removing null terminators
            val variantString = String(variantBytes, Charsets.UTF_8).trimEnd('\u0000')
            val materialId = String(materialBytes, Charsets.UTF_8).trimEnd('\u0000')
            
            // Split variant into series and colour
            val dashIndex = variantString.indexOf('-')
            if (dashIndex == -1) return null
            
            val seriesCode = variantString.substring(0, dashIndex)
            val colourCode = variantString.substring(dashIndex + 1)
            
            Triple(materialId, seriesCode, colourCode)
        } catch (e: Exception) {
            null
        }
    }
}