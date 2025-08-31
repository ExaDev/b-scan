package com.bscan.service

import com.bscan.model.graph.Edge
import com.bscan.model.graph.Graph
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing scan data with ephemeral entity caching.
 * Implements the pattern: persist only scan occurrences and unique raw data,
 * generate derived interpretations on-demand with TTL caching.
 */
class ScanDataService(
    private val graphRepository: GraphRepository
) {
    
    // In-memory cache for ephemeral entities
    private val decodedEncryptedCache = ConcurrentHashMap<String, CacheEntry<DecodedEncrypted>>()
    private val encodedDecryptedCache = ConcurrentHashMap<String, CacheEntry<EncodedDecrypted>>()
    private val decodedDecryptedCache = ConcurrentHashMap<String, CacheEntry<DecodedDecrypted>>()
    
    // Cache statistics
    private var cacheHits = 0L
    private var cacheMisses = 0L
    
    /**
     * Record a new scan occurrence with raw data
     * Deduplicates raw data based on content hash
     */
    suspend fun recordScan(
        rawData: String,
        scanFormat: String,
        deviceInfo: String? = null,
        scanLocation: String? = null,
        scanMethod: String? = null,
        userNotes: String? = null
    ): ScanRecordResult = withContext(Dispatchers.IO) {
        
        val contentHash = calculateContentHash(rawData)
        
        // Check if raw data already exists
        val existingRawData = findExistingRawData(contentHash, scanFormat)
        
        val rawScanData = existingRawData ?: run {
            // Create new raw data entity
            RawScanData(
                label = "Raw $scanFormat data",
                scanFormat = scanFormat
            ).apply {
                this.rawData = rawData
                this.dataSize = rawData.length
                this.contentHash = contentHash
                this.encoding = detectEncoding(rawData)
            }.also { newRawData ->
                graphRepository.addEntity(newRawData)
            }
        }
        
        // Always create new scan occurrence
        val scanOccurrence = ScanOccurrence(
            label = "Scan at ${LocalDateTime.now()}"
        ).apply {
            this.deviceInfo = deviceInfo
            this.scanLocation = scanLocation
            this.scanMethod = scanMethod
            this.userData = userNotes
        }
        
        graphRepository.addEntity(scanOccurrence)
        
        // Link scan occurrence to raw data
        val scanEdge = Edge(
            fromEntityId = scanOccurrence.id,
            toEntityId = rawScanData.id,
            relationshipType = ScanDataRelationshipTypes.SCANNED
        )
        graphRepository.addEdge(scanEdge)
        
        ScanRecordResult(
            success = true,
            scanOccurrence = scanOccurrence,
            rawScanData = rawScanData,
            wasRawDataDeduplicated = existingRawData != null
        )
    }
    
    /**
     * Get or generate decoded encrypted data (metadata extraction)
     * Returns cached version if available and not expired
     */
    suspend fun getDecodedEncrypted(rawScanData: RawScanData): DecodedEncrypted = withContext(Dispatchers.IO) {
        
        val cacheKey = rawScanData.id
        val cached = decodedEncryptedCache[cacheKey]
        
        if (cached != null && !cached.isExpired()) {
            cacheHits++
            return@withContext cached.entity
        }
        
        cacheMisses++
        
        // Generate new decoded encrypted entity
        val decodedEncrypted = generateDecodedEncrypted(rawScanData)
        
        // Cache with TTL
        decodedEncryptedCache[cacheKey] = CacheEntry(
            entity = decodedEncrypted,
            ttlMinutes = 60
        )
        
        // Note: We don't persist ephemeral entities - they exist only in cache
        
        decodedEncrypted
    }
    
    /**
     * Get or generate encoded decrypted data (decryption without interpretation)
     * Returns cached version if available and not expired
     */
    suspend fun getEncodedDecrypted(rawScanData: RawScanData): EncodedDecrypted = withContext(Dispatchers.IO) {
        
        val cacheKey = rawScanData.id
        val cached = encodedDecryptedCache[cacheKey]
        
        if (cached != null && !cached.isExpired()) {
            cacheHits++
            return@withContext cached.entity
        }
        
        cacheMisses++
        
        // Generate new encoded decrypted entity
        val encodedDecrypted = generateEncodedDecrypted(rawScanData)
        
        // Cache with TTL
        encodedDecryptedCache[cacheKey] = CacheEntry(
            entity = encodedDecrypted,
            ttlMinutes = 30
        )
        
        encodedDecrypted
    }
    
    /**
     * Get or generate decoded decrypted data (full interpretation)
     * Returns cached version if available and not expired
     */
    suspend fun getDecodedDecrypted(rawScanData: RawScanData): DecodedDecrypted = withContext(Dispatchers.IO) {
        
        val cacheKey = rawScanData.id
        val cached = decodedDecryptedCache[cacheKey]
        
        if (cached != null && !cached.isExpired()) {
            cacheHits++
            return@withContext cached.entity
        }
        
        cacheMisses++
        
        // Generate new decoded decrypted entity
        val decodedDecrypted = generateDecodedDecrypted(rawScanData)
        
        // Cache with shorter TTL (most expensive to compute)
        decodedDecryptedCache[cacheKey] = CacheEntry(
            entity = decodedDecrypted,
            ttlMinutes = 15
        )
        
        decodedDecrypted
    }
    
    /**
     * Clear expired cache entries
     */
    fun cleanupExpiredCache() {
        decodedEncryptedCache.entries.removeAll { it.value.isExpired() }
        encodedDecryptedCache.entries.removeAll { it.value.isExpired() }
        decodedDecryptedCache.entries.removeAll { it.value.isExpired() }
    }
    
    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        decodedEncryptedCache.clear()
        encodedDecryptedCache.clear()
        decodedDecryptedCache.clear()
        cacheHits = 0
        cacheMisses = 0
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): CacheStatistics {
        val totalEntries = decodedEncryptedCache.size + encodedDecryptedCache.size + decodedDecryptedCache.size
        val expiredEntries = decodedEncryptedCache.values.count { it.isExpired() } +
                           encodedDecryptedCache.values.count { it.isExpired() } +
                           decodedDecryptedCache.values.count { it.isExpired() }
        
        val totalRequests = cacheHits + cacheMisses
        val hitRate = if (totalRequests > 0) cacheHits.toFloat() / totalRequests else 0f
        
        return CacheStatistics(
            totalEntries = totalEntries,
            expiredEntries = expiredEntries,
            hitRate = hitRate,
            memoryUsageBytes = estimateMemoryUsage(),
            oldestEntryAge = getOldestEntryAge(),
            averageTtl = calculateAverageTtl()
        )
    }
    
    /**
     * Get all scan occurrences
     */
    suspend fun getAllScanOccurrences(): List<ScanOccurrence> = withContext(Dispatchers.IO) {
        graphRepository.getEntitiesByType("activity")
            .filterIsInstance<ScanOccurrence>()
            .sortedByDescending { it.timestamp }
    }
    
    /**
     * Get raw scan data linked to a scan occurrence
     */
    suspend fun getRawScanData(scanOccurrence: ScanOccurrence): RawScanData? = withContext(Dispatchers.IO) {
        val connected = graphRepository.getConnectedEntities(scanOccurrence.id, ScanDataRelationshipTypes.SCANNED)
        connected.filterIsInstance<RawScanData>().firstOrNull()
    }
    
    /**
     * Get all unique raw scan data (deduplicated)
     */
    suspend fun getAllUniqueRawScanData(): List<RawScanData> = withContext(Dispatchers.IO) {
        graphRepository.getEntitiesByType("information")
            .filterIsInstance<RawScanData>()
            .sortedByDescending { it.metadata.created }
    }
    
    // Private helper methods
    
    private fun calculateContentHash(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun detectEncoding(data: String): String {
        return when {
            data.matches(Regex("^[0-9A-Fa-f]+$")) -> "hex"
            data.matches(Regex("^[A-Za-z0-9+/]+={0,2}$")) -> "base64"
            else -> "utf8"
        }
    }
    
    private suspend fun findExistingRawData(contentHash: String, scanFormat: String): RawScanData? {
        return graphRepository.getEntitiesByType("information")
            .filterIsInstance<RawScanData>()
            .find { it.contentHash == contentHash && it.scanFormat == scanFormat }
    }
    
    private fun generateDecodedEncrypted(rawScanData: RawScanData): DecodedEncrypted {
        val startTime = System.currentTimeMillis()
        
        // TODO: Implement actual metadata extraction based on scanFormat
        val decodedEncrypted = DecodedEncrypted(
            label = "Metadata from ${rawScanData.label}"
        ).apply {
            when (rawScanData.scanFormat) {
                "bambu_rfid" -> {
                    tagType = "Mifare Classic 1K"
                    sectorCount = 16
                    dataBlocks = 64
                    authenticated = true
                }
                "creality_rfid" -> {
                    tagType = "NTAG213/215"
                    authenticated = false
                }
                "qr_code", "barcode" -> {
                    authenticated = false
                }
            }
            
            keyDerivationTime = System.currentTimeMillis() - startTime
            cacheTimestamp = LocalDateTime.now()
        }
        
        return decodedEncrypted
    }
    
    private fun generateEncodedDecrypted(rawScanData: RawScanData): EncodedDecrypted {
        val startTime = System.currentTimeMillis()
        
        // TODO: Implement actual decryption based on scanFormat
        val encodedDecrypted = EncodedDecrypted(
            label = "Decrypted ${rawScanData.label}"
        ).apply {
            when (rawScanData.scanFormat) {
                "bambu_rfid" -> {
                    // Simulate decryption process
                    decryptedData = rawScanData.rawData // Would be actual decrypted hex data
                    keyInfo = "Derived key from UID"
                }
                "creality_rfid" -> {
                    decryptedData = rawScanData.rawData // Already unencrypted
                }
                else -> {
                    decryptedData = rawScanData.rawData
                }
            }
            
            decryptionTime = System.currentTimeMillis() - startTime
            cacheTimestamp = LocalDateTime.now()
        }
        
        return encodedDecrypted
    }
    
    private fun generateDecodedDecrypted(rawScanData: RawScanData): DecodedDecrypted {
        val startTime = System.currentTimeMillis()
        
        // TODO: Implement actual interpretation based on scanFormat
        val decodedDecrypted = DecodedDecrypted(
            label = "Interpreted ${rawScanData.label}"
        ).apply {
            when (rawScanData.scanFormat) {
                "bambu_rfid" -> {
                    // Simulate filament interpretation
                    filamentProperties = """{"material":"PLA","color":"Black","weight":1000}"""
                    productInfo = """{"sku":"GFL00:A00-K0","manufacturer":"Bambu Lab"}"""
                    temperatureSettings = """{"hotend":220,"bed":65}"""
                    identifiers = """{"tagUid":"12345678","trayUid":"01008023..."}"""
                }
                "qr_code" -> {
                    interpretedData = rawScanData.rawData // QR content as-is
                }
            }
            
            interpretationVersion = "1.0.0"
            interpretationTime = System.currentTimeMillis() - startTime
            cacheTimestamp = LocalDateTime.now()
        }
        
        return decodedDecrypted
    }
    
    private fun estimateMemoryUsage(): Long {
        // Rough estimation
        var totalSize = 0L
        decodedEncryptedCache.values.forEach { totalSize += estimateEntitySize(it.entity) }
        encodedDecryptedCache.values.forEach { totalSize += estimateEntitySize(it.entity) }
        decodedDecryptedCache.values.forEach { totalSize += estimateEntitySize(it.entity) }
        return totalSize
    }
    
    private fun estimateEntitySize(entity: com.bscan.model.graph.Entity): Long {
        // Very rough estimation - could be more sophisticated
        return entity.properties.values.sumOf { prop ->
            when (prop) {
                is com.bscan.model.graph.PropertyValue.StringValue -> (prop.rawValue as String).length * 2L // UTF-16
                is com.bscan.model.graph.PropertyValue.IntValue -> 8L
                is com.bscan.model.graph.PropertyValue.LongValue -> 8L
                is com.bscan.model.graph.PropertyValue.FloatValue -> 8L
                is com.bscan.model.graph.PropertyValue.DoubleValue -> 8L
                is com.bscan.model.graph.PropertyValue.BooleanValue -> 1L
                is com.bscan.model.graph.PropertyValue.DateTimeValue -> 32L
                else -> 0L // Handle any other PropertyValue types
            }
        }
    }
    
    private fun getOldestEntryAge(): Long {
        val allEntries = decodedEncryptedCache.values + encodedDecryptedCache.values + decodedDecryptedCache.values
        val oldest = allEntries.minByOrNull { it.timestamp }
        return oldest?.let { 
            java.time.Duration.between(it.timestamp, LocalDateTime.now()).toMinutes() 
        } ?: 0L
    }
    
    private fun calculateAverageTtl(): Int {
        val allEntries = decodedEncryptedCache.values + encodedDecryptedCache.values + decodedDecryptedCache.values
        return if (allEntries.isNotEmpty()) {
            allEntries.sumOf { it.ttlMinutes } / allEntries.size
        } else {
            0
        }
    }
}

/**
 * Result of recording a scan
 */
data class ScanRecordResult(
    val success: Boolean,
    val scanOccurrence: ScanOccurrence? = null,
    val rawScanData: RawScanData? = null,
    val wasRawDataDeduplicated: Boolean = false,
    val error: String? = null
)