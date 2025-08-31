package com.bscan.model.graph.entities

import com.bscan.model.graph.Entity
import com.bscan.model.graph.PropertyValue
import com.bscan.model.graph.ValidationResult
import java.time.LocalDateTime

/**
 * Scan occurrence entity - represents each individual scan event
 * PERSISTENT: One created for each scan
 */
open class ScanOccurrence(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Activity(
    id = id,
    activityType = ActivityTypes.SCAN,
    label = label,
    properties = properties
) {
    
    var deviceInfo: String?
        get() = getProperty("deviceInfo")
        set(value) { value?.let { setProperty("deviceInfo", it) } }
    
    var scanLocation: String?
        get() = getProperty("scanLocation") 
        set(value) { value?.let { setProperty("scanLocation", it) } }
    
    var scanMethod: String?
        get() = getProperty("scanMethod")  // "nfc", "qr", "barcode", "manual"
        set(value) { value?.let { setProperty("scanMethod", it) } }
    
    var appVersion: String?
        get() = getProperty("appVersion")
        set(value) { value?.let { setProperty("appVersion", it) } }
    
    var userData: String?
        get() = getProperty("userData")  // User annotations, notes
        set(value) { value?.let { setProperty("userData", it) } }
}

/**
 * Raw scan data entity - stores unique encoded/encrypted data
 * PERSISTENT: Shared across multiple scan occurrences via relationships
 * Deduplicated based on content hash
 */
open class RawScanData(
    id: String = generateId(),
    label: String,
    val scanFormat: String,  // "bambu_rfid", "creality_rfid", "qr_code", "barcode"
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Information(
    id = id,
    informationType = "raw_scan_data",
    label = label,
    properties = properties
) {
    
    init {
        setProperty("scanFormat", scanFormat)
    }
    
    var rawData: String
        get() = getProperty("rawData") ?: ""
        set(value) { setProperty("rawData", value) }
    
    var dataSize: Int
        get() = getProperty("dataSize") ?: 0
        set(value) { setProperty("dataSize", value) }
    
    var contentHash: String?
        get() = getProperty("contentHash")
        set(value) { value?.let { setProperty("contentHash", it) } }
    
    var encoding: String?
        get() = getProperty("encoding")  // "hex", "base64", "utf8"
        set(value) { value?.let { setProperty("encoding", it) } }
    
    var checksumValid: Boolean?
        get() = getProperty("checksumValid")
        set(value) { value?.let { setProperty("checksumValid", it) } }
    
    override fun copy(newId: String): RawScanData {
        return RawScanData(
            id = newId,
            label = label,
            scanFormat = scanFormat,
            properties = properties.toMutableMap()
        )
    }
    
    override fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        if (scanFormat.isBlank()) errors.add("Scan format must be specified")
        if (rawData.isBlank()) errors.add("Raw data cannot be empty")
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(*errors.toTypedArray())
        }
    }
}

/**
 * Decoded encrypted entity - non-encrypted metadata from raw scan
 * EPHEMERAL: Generated on-demand with TTL caching
 */
open class DecodedEncrypted(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Information(
    id = id,
    informationType = "decoded_encrypted",
    label = label,
    properties = properties
) {
    
    var tagType: String?
        get() = getProperty("tagType")
        set(value) { value?.let { setProperty("tagType", it) } }
    
    var tagUid: String?
        get() = getProperty("tagUid")
        set(value) { value?.let { setProperty("tagUid", it) } }
    
    var dataBlocks: Int?
        get() = getProperty("dataBlocks")
        set(value) { value?.let { setProperty("dataBlocks", it) } }
    
    var sectorCount: Int?
        get() = getProperty("sectorCount")
        set(value) { value?.let { setProperty("sectorCount", it) } }
    
    var authenticated: Boolean?
        get() = getProperty("authenticated")
        set(value) { value?.let { setProperty("authenticated", it) } }
    
    var keyDerivationTime: Long?
        get() = getProperty<Long>("keyDerivationTime")
        set(value) { value?.let { setProperty("keyDerivationTime", it) } }
    
    var cacheTimestamp: LocalDateTime
        get() = getProperty("cacheTimestamp") ?: LocalDateTime.now()
        set(value) { setProperty("cacheTimestamp", value) }
    
    var cacheTtlMinutes: Int
        get() = getProperty("cacheTtlMinutes") ?: 60
        set(value) { setProperty("cacheTtlMinutes", value) }
    
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(cacheTimestamp.plusMinutes(cacheTtlMinutes.toLong()))
    }
}

/**
 * Encoded decrypted entity - decrypted but still hex-encoded data  
 * EPHEMERAL: Generated on-demand with TTL caching
 */
open class EncodedDecrypted(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Information(
    id = id,
    informationType = "encoded_decrypted", 
    label = label,
    properties = properties
) {
    
    var decryptedData: String
        get() = getProperty("decryptedData") ?: ""
        set(value) { setProperty("decryptedData", value) }
    
    var blockStructure: String?
        get() = getProperty("blockStructure")  // JSON representation of block layout
        set(value) { value?.let { setProperty("blockStructure", it) } }
    
    var keyInfo: String?
        get() = getProperty("keyInfo")  // Which keys were used for decryption
        set(value) { value?.let { setProperty("keyInfo", it) } }
    
    var decryptionTime: Long?
        get() = getProperty<Long>("decryptionTime")
        set(value) { value?.let { setProperty("decryptionTime", it) } }
    
    var cacheTimestamp: LocalDateTime
        get() = getProperty("cacheTimestamp") ?: LocalDateTime.now()
        set(value) { setProperty("cacheTimestamp", value) }
    
    var cacheTtlMinutes: Int
        get() = getProperty("cacheTtlMinutes") ?: 30
        set(value) { setProperty("cacheTtlMinutes", value) }
    
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(cacheTimestamp.plusMinutes(cacheTtlMinutes.toLong()))
    }
}

/**
 * Decoded decrypted entity - fully interpreted structured data
 * EPHEMERAL: Generated on-demand with TTL caching
 */
open class DecodedDecrypted(
    id: String = generateId(),
    label: String,
    properties: MutableMap<String, PropertyValue> = mutableMapOf()
) : Information(
    id = id,
    informationType = "decoded_decrypted",
    label = label,
    properties = properties
) {
    
    var interpretedData: String?
        get() = getProperty("interpretedData")  // JSON representation of structured data
        set(value) { value?.let { setProperty("interpretedData", it) } }
    
    var filamentProperties: String?
        get() = getProperty("filamentProperties")  // Bambu-specific: material, color, etc.
        set(value) { value?.let { setProperty("filamentProperties", it) } }
    
    var productInfo: String?
        get() = getProperty("productInfo")  // SKU, manufacturer, model
        set(value) { value?.let { setProperty("productInfo", it) } }
    
    var temperatureSettings: String?
        get() = getProperty("temperatureSettings")  // Print/bed temperatures
        set(value) { value?.let { setProperty("temperatureSettings", it) } }
    
    var physicalProperties: String?
        get() = getProperty("physicalProperties")  // Mass, dimensions, etc.
        set(value) { value?.let { setProperty("physicalProperties", it) } }
    
    var identifiers: String?
        get() = getProperty("identifiers")  // UID, tray ID, consumable ID
        set(value) { value?.let { setProperty("identifiers", it) } }
    
    var interpretationVersion: String?
        get() = getProperty("interpretationVersion")  // Version of interpretation logic
        set(value) { value?.let { setProperty("interpretationVersion", it) } }
    
    var interpretationTime: Long?
        get() = getProperty<Long>("interpretationTime")
        set(value) { value?.let { setProperty("interpretationTime", it) } }
    
    var cacheTimestamp: LocalDateTime
        get() = getProperty("cacheTimestamp") ?: LocalDateTime.now()
        set(value) { setProperty("cacheTimestamp", value) }
    
    var cacheTtlMinutes: Int
        get() = getProperty("cacheTtlMinutes") ?: 15  // Shorter TTL for complex interpretations
        set(value) { setProperty("cacheTtlMinutes", value) }
    
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(cacheTimestamp.plusMinutes(cacheTtlMinutes.toLong()))
    }
}

/**
 * Common scan data relationship types
 */
object ScanDataRelationshipTypes {
    const val SCANNED = "scanned"                    // ScanOccurrence -> RawScanData
    const val DECODED_TO = "decoded_to"              // RawScanData -> DecodedEncrypted
    const val DECRYPTED_TO = "decrypted_to"          // RawScanData -> EncodedDecrypted
    const val INTERPRETED_AS = "interpreted_as"      // RawScanData -> DecodedDecrypted
    const val DERIVED_FROM = "derived_from"          // Any ephemeral -> source entity
    const val CREATED_ENTITIES = "created_entities"  // DecodedDecrypted -> PhysicalComponent entities
}

/**
 * Cache management for ephemeral entities
 */
data class CacheEntry<T : Entity>(
    val entity: T,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val ttlMinutes: Int
) {
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(timestamp.plusMinutes(ttlMinutes.toLong()))
    }
    
    fun getRemainingTtl(): Long {
        val expiryTime = timestamp.plusMinutes(ttlMinutes.toLong())
        val now = LocalDateTime.now()
        return if (now.isBefore(expiryTime)) {
            java.time.Duration.between(now, expiryTime).toMinutes()
        } else {
            0
        }
    }
}

/**
 * Cache statistics for monitoring
 */
data class CacheStatistics(
    val totalEntries: Int,
    val expiredEntries: Int,
    val hitRate: Float,
    val memoryUsageBytes: Long,
    val oldestEntryAge: Long,
    val averageTtl: Int
)