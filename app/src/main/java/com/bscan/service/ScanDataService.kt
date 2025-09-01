package com.bscan.service

import android.content.Context
import com.bscan.model.graph.Edge
import com.bscan.model.graph.Entity
import com.bscan.model.graph.Graph
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import com.bscan.repository.CatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing scan data with ephemeral entity caching.
 * Implements the pattern: persist only scan occurrences and unique raw data,
 * generate derived interpretations on-demand with content-based caching.
 */
class ScanDataService(
    private val context: Context,
    private val graphRepository: GraphRepository
) {
    
    // Enhanced caching system with content-based validation
    private val entityCacheManager by lazy { 
        EntityCacheManager(graphRepository) 
    }
    
    private val dependencyTracker by lazy {
        DependencyTracker(context, CatalogRepository(context))
    }
    
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
     * Uses content-based validation to ensure cache consistency
     */
    suspend fun getDecodedEncrypted(rawScanData: RawScanData): DecodedEncrypted = withContext(Dispatchers.IO) {
        return@withContext entityCacheManager.getOrGenerate(
            sourceEntity = rawScanData,
            derivationType = "decoded_encrypted"
        ) { source ->
            generateDecodedEncrypted(source as RawScanData)
        }
    }
    
    /**
     * Get or generate encoded decrypted data (decryption without interpretation)
     * Uses content-based validation to ensure cache consistency
     */
    suspend fun getEncodedDecrypted(rawScanData: RawScanData): EncodedDecrypted = withContext(Dispatchers.IO) {
        return@withContext entityCacheManager.getOrGenerate(
            sourceEntity = rawScanData,
            derivationType = "encoded_decrypted"
        ) { source ->
            generateEncodedDecrypted(source as RawScanData)
        }
    }
    
    /**
     * Get or generate decoded decrypted data (full interpretation)
     * Uses content-based validation with dependency tracking
     */
    suspend fun getDecodedDecrypted(rawScanData: RawScanData): DecodedDecrypted = withContext(Dispatchers.IO) {
        return@withContext entityCacheManager.getOrGenerate(
            sourceEntity = rawScanData,
            derivationType = "decoded_decrypted"
        ) { source ->
            generateDecodedDecrypted(source as RawScanData)
        }
    }
    
    /**
     * Get all derived entities for a source (batch operation)
     */
    suspend fun getAllDerivedEntities(rawScanData: RawScanData): Map<String, Entity> {
        return entityCacheManager.getDerivedEntities(rawScanData)
    }
    
    /**
     * Invalidate cached entities for specific source
     */
    fun invalidateCacheForSource(sourceEntityId: String) {
        entityCacheManager.invalidateSource(sourceEntityId)
    }
    
    /**
     * Invalidate all cached entities of specific type
     */
    fun invalidateCacheForType(derivationType: String) {
        entityCacheManager.invalidateType(derivationType)
    }
    
    /**
     * Clear expired cache entries
     */
    fun cleanupExpiredCache() {
        entityCacheManager.cleanupExpired()
    }
    
    /**
     * Clear all caches
     */
    fun clearAllCaches() {
        entityCacheManager.clearAll()
    }
    
    /**
     * Get enhanced cache statistics with content change tracking
     */
    fun getCacheStatistics(): EntityCacheStatistics {
        return entityCacheManager.getStatistics()
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