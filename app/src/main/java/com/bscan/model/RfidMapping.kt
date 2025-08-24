package com.bscan.model

/**
 * RFID code to SKU mapping for exact product identification.
 * Based on reverse-engineered Bambu Lab RFID format from the Bambu-Lab-RFID-Library.
 */
data class RfidMapping(
    val rfidCode: String,     // Combined material ID and variant ID (e.g., "GFA00:A00-K0")
    val sku: String,          // 5-digit SKU number (e.g., "10101")
    val material: String,     // Material type (e.g., "PLA Basic")
    val color: String,        // Color name (e.g., "Black")
    val hex: String?,         // Hex color code if available
    val sampleCount: Int = 1  // Number of RFID dump samples for this mapping
) {
    /**
     * Get material ID from RFID code (e.g., "GFA00" from "GFA00:A00-K0")
     */
    fun getMaterialId(): String? {
        return rfidCode.split(":").getOrNull(0)
    }
    
    /**
     * Get variant ID from RFID code (e.g., "A00-K0" from "GFA00:A00-K0")
     */
    fun getVariantId(): String? {
        return rfidCode.split(":").getOrNull(1)
    }
}

/**
 * Collection of RFID mappings loaded from assets
 */
data class RfidMappings(
    val version: Int,
    val description: String,
    val rfidMappings: Map<String, RfidMapping>
) {
    /**
     * Look up SKU by RFID code
     */
    fun getSkuByRfidCode(rfidCode: String): String? {
        return rfidMappings[rfidCode]?.sku
    }
    
    /**
     * Look up SKU by material ID and variant ID
     */
    fun getSkuByMaterialAndVariant(materialId: String, variantId: String): String? {
        val rfidCode = "$materialId:$variantId"
        return rfidMappings[rfidCode]?.sku
    }
    
    /**
     * Get complete mapping by RFID code
     */
    fun getMappingByRfidCode(rfidCode: String): RfidMapping? {
        return rfidMappings[rfidCode]
    }
    
    /**
     * Check if exact mapping exists for given material and variant
     */
    fun hasExactMapping(materialId: String, variantId: String): Boolean {
        val rfidCode = "$materialId:$variantId"
        return rfidMappings.containsKey(rfidCode)
    }
    
    /**
     * Get all mappings for a specific material type
     */
    fun getMappingsForMaterial(materialType: String): List<RfidMapping> {
        return rfidMappings.values.filter { it.material.contains(materialType, ignoreCase = true) }
    }
    
    companion object {
        /**
         * Create empty RFID mappings
         */
        fun empty(): RfidMappings = RfidMappings(
            version = 0,
            description = "Empty RFID mappings",
            rfidMappings = emptyMap()
        )
    }
}