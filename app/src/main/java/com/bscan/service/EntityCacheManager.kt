package com.bscan.service

import android.util.Log
import com.bscan.model.graph.*
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import com.bscan.service.graph.GraphComponentFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Content-based caching system for ephemeral entities.
 * Uses result fingerprinting to detect when derived entities need regeneration,
 * regardless of what caused the change (parser updates, config changes, etc.).
 */
open class EntityCacheManager(
    private val graphRepository: GraphRepository
) {
    
    companion object {
        private const val TAG = "EntityCacheManager"
        
        // Default TTL values for different entity types
        private const val DEFAULT_TTL_MINUTES = 60
        private const val DECODED_ENCRYPTED_TTL = 90
        private const val ENCODED_DECRYPTED_TTL = 60
        private const val DECODED_DECRYPTED_TTL = 30
        private const val PHYSICAL_COMPONENT_TTL = 120
    }
    
    // Thread-safe cache storage
    private val cache = ConcurrentHashMap<String, FingerprintedCacheEntry<Entity>>()
    private val relationshipCache = ConcurrentHashMap<String, RelationshipCacheEntry>()
    
    // Cache statistics - use @Volatile for thread safety
    @Volatile private var cacheHits = 0L
    @Volatile private var cacheMisses = 0L
    @Volatile private var contentChanges = 0L
    @Volatile private var dependencyChanges = 0L
    @Volatile private var expirations = 0L
    @Volatile private var relationshipCacheHits = 0L
    @Volatile private var relationshipCacheMisses = 0L
    
    /**
     * Get or generate derived entity with content-based validation
     */
    suspend fun <T : Entity> getOrGenerate(
        sourceEntity: Entity,
        derivationType: String,
        generator: suspend (Entity) -> T
    ): T = withContext(Dispatchers.Default) {
        
        // Generate deterministic cache key from source
        val cacheKey = createCacheKey(sourceEntity.id, derivationType)
        
        // Check cached version first
        val cached = cache[cacheKey]
        
        if (cached != null && !cached.isExpired()) {
            // Validate cached content by generating minimal fingerprint
            val currentFingerprint = generateMinimalFingerprint(sourceEntity, derivationType)
            
            if (cached.contentFingerprint == currentFingerprint) {
                synchronized(this@EntityCacheManager) { cacheHits++ }
                Log.v(TAG, "Cache HIT for $derivationType from ${sourceEntity.id}")
                @Suppress("UNCHECKED_CAST")
                return@withContext cached.entity as T
            } else {
                synchronized(this@EntityCacheManager) { contentChanges++ }
                Log.d(TAG, "Content changed for $derivationType from ${sourceEntity.id}")
            }
        } else if (cached != null) {
            synchronized(this@EntityCacheManager) { expirations++ }
            Log.v(TAG, "Cache expired for $derivationType from ${sourceEntity.id}")
        }
        
        // Generate fresh entity
        synchronized(this@EntityCacheManager) { cacheMisses++ }
        Log.d(TAG, "Cache MISS - generating $derivationType from ${sourceEntity.id}")
        
        val freshEntity = generator(sourceEntity)
        
        // Use minimal fingerprint for consistency (not full entity fingerprint)
        val fingerprint = generateMinimalFingerprint(sourceEntity, derivationType)
        val dependencies = extractDependencies(freshEntity, sourceEntity)
        
        // Cache with fingerprint
        val cacheEntry = FingerprintedCacheEntry(
            entity = freshEntity,
            sourceEntityId = sourceEntity.id,
            derivationType = derivationType,
            contentFingerprint = fingerprint,
            dependencies = dependencies,
            timestamp = LocalDateTime.now(),
            ttlMinutes = getTTLForType(derivationType)
        )
        
        @Suppress("UNCHECKED_CAST")
        cache[cacheKey] = cacheEntry as FingerprintedCacheEntry<Entity>
        
        Log.v(TAG, "Cached $derivationType with fingerprint ${fingerprint.take(8)}...")
        
        freshEntity
    }
    
    /**
     * Get derived entities for a source entity (batch operation)
     */
    suspend fun getDerivedEntities(sourceEntity: Entity): Map<String, Entity> = withContext(Dispatchers.Default) {
        val results = mutableMapOf<String, Entity>()
        
        // Get all standard derived entity types
        val derivationTypes = listOf(
            "decoded_encrypted",
            "encoded_decrypted", 
            "decoded_decrypted"
        )
        
        derivationTypes.forEach { type ->
            try {
                val derived = getOrGenerate(sourceEntity, type) { source ->
                    generateDerivedEntity(source, type)
                }
                results[type] = derived
            } catch (e: Exception) {
                Log.w(TAG, "Failed to generate $type for ${sourceEntity.id}", e)
            }
        }
        
        return@withContext results
    }
    
    /**
     * Invalidate cache entries for a specific source entity
     */
    fun invalidateSource(sourceEntityId: String) {
        val invalidated = cache.entries.removeAll { (_, entry) ->
            entry.sourceEntityId == sourceEntityId
        }
        Log.d(TAG, "Invalidated $invalidated cache entries for source $sourceEntityId")
    }
    
    /**
     * Invalidate all cache entries of a specific derivation type
     */
    fun invalidateType(derivationType: String) {
        val invalidated = cache.entries.removeAll { (_, entry) ->
            entry.derivationType == derivationType
        }
        Log.d(TAG, "Invalidated $invalidated cache entries of type $derivationType")
    }
    
    /**
     * Clean up expired cache entries
     */
    fun cleanupExpired() {
        val initialSize = cache.size
        cache.entries.removeAll { (_, entry) -> entry.isExpired() }
        val removed = initialSize - cache.size
        
        if (removed > 0) {
            Log.d(TAG, "Cleaned up $removed expired cache entries")
        }
    }
    
    /**
     * Clear all cached entities
     */
    fun clearAll() {
        cache.clear()
        cacheHits = 0L
        cacheMisses = 0L
        contentChanges = 0L
        dependencyChanges = 0L
        expirations = 0L
        Log.i(TAG, "Cleared all cached entities")
    }
    
    /**
     * Get cache statistics
     */
    fun getStatistics(): EntityCacheStatistics {
        val totalRequests = cacheHits + cacheMisses
        val hitRate = if (totalRequests > 0) cacheHits.toFloat() / totalRequests else 0f
        
        return EntityCacheStatistics(
            totalEntries = cache.size,
            expiredEntries = cache.values.count { it.isExpired() },
            hitRate = hitRate,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            contentChanges = contentChanges,
            dependencyChanges = dependencyChanges,
            expirations = expirations,
            memoryUsageBytes = estimateMemoryUsage()
        )
    }
    
    // Private implementation methods
    
    private fun createCacheKey(sourceEntityId: String, derivationType: String): String {
        return GraphComponentFactory.createCompoundId(
            "source" to sourceEntityId,
            "type" to derivationType
        )
    }
    
    private suspend fun generateMinimalFingerprint(sourceEntity: Entity, derivationType: String): String {
        // Generate lightweight fingerprint for comparison without full entity generation
        val relevantData = buildString {
            append(sourceEntity.id)
            append(derivationType)
            
            // Include source entity properties that affect derivation
            sourceEntity.properties.entries
                .sortedBy { it.key }
                .forEach { (key, value) ->
                    append("$key:${value.asString()}")
                }
            
            // Include timestamp for raw scan data to detect changes
            if (sourceEntity is RawScanData) {
                append("rawData:${sourceEntity.rawData?.hashCode()}")
                append("contentHash:${sourceEntity.contentHash}")
            }
        }
        
        return hashString(relevantData).take(16)
    }
    
    private fun generateFullFingerprint(entity: Entity): String {
        val contentString = buildString {
            append(entity.entityType)
            append(entity.label)
            
            // Sort properties for deterministic ordering
            entity.properties.entries
                .sortedBy { it.key }
                .forEach { (key, value) ->
                    append("$key:${value.asString()}")
                }
        }
        
        return hashString(contentString).take(16)
    }
    
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
    }
    
    private fun extractDependencies(entity: Entity, sourceEntity: Entity): Set<String> {
        val dependencies = mutableSetOf<String>()
        
        // Track source entity as primary dependency
        dependencies.add("source:${sourceEntity.id}")
        
        // Track entity type-specific dependencies
        when (entity) {
            is DecodedDecrypted -> {
                // Depends on interpretation logic and external catalogs
                entity.getProperty<String>("catalogData")?.let {
                    dependencies.add("catalog:${it.hashCode()}")
                }
            }
            is PhysicalComponent -> {
                // Depends on component definitions and mass calculations
                dependencies.add("component_definitions:${entity.category}")
                entity.massGrams?.let { 
                    dependencies.add("mass_calculation:$it") 
                }
            }
        }
        
        return dependencies
    }
    
    protected open fun getTTLForType(derivationType: String): Int {
        return when (derivationType) {
            "decoded_encrypted" -> DECODED_ENCRYPTED_TTL
            "encoded_decrypted" -> ENCODED_DECRYPTED_TTL
            "decoded_decrypted" -> DECODED_DECRYPTED_TTL
            "physical_component" -> PHYSICAL_COMPONENT_TTL
            else -> DEFAULT_TTL_MINUTES
        }
    }
    
    private suspend fun generateDerivedEntity(sourceEntity: Entity, derivationType: String): Entity {
        // This would normally delegate to the appropriate generator
        // For now, create placeholder entities for testing
        return when (derivationType) {
            "decoded_encrypted" -> DecodedEncrypted(
                label = "Metadata from ${sourceEntity.label}"
            ).apply {
                tagType = "Mifare Classic 1K"
                authenticated = true
                keyDerivationTime = 10L
                cacheTimestamp = LocalDateTime.now()
            }
            
            "encoded_decrypted" -> EncodedDecrypted(
                label = "Decrypted ${sourceEntity.label}"
            ).apply {
                decryptedData = "placeholder_decrypted_data"
                keyInfo = "Derived from UID"
                decryptionTime = 5L
                cacheTimestamp = LocalDateTime.now()
            }
            
            "decoded_decrypted" -> DecodedDecrypted(
                label = "Interpreted ${sourceEntity.label}"
            ).apply {
                filamentProperties = """{"material":"PLA","color":"Black"}"""
                productInfo = """{"manufacturer":"Test"}"""
                interpretationVersion = "1.0.0"
                interpretationTime = 15L
                cacheTimestamp = LocalDateTime.now()
            }
            
            else -> throw IllegalArgumentException("Unknown derivation type: $derivationType")
        }
    }
    
    private fun estimateMemoryUsage(): Long {
        return cache.values.sumOf { entry ->
            estimateEntitySize(entry.entity) + 
            entry.contentFingerprint.length * 2L +
            entry.dependencies.sumOf { it.length * 2L } + 
            200L // overhead
        }
    }
    
    private fun estimateEntitySize(entity: Entity): Long {
        return entity.properties.values.sumOf { prop ->
            when (prop) {
                is PropertyValue.StringValue -> (prop.rawValue as String).length * 2L
                is PropertyValue.IntValue -> 8L
                is PropertyValue.LongValue -> 8L
                is PropertyValue.FloatValue -> 8L
                is PropertyValue.DoubleValue -> 8L
                is PropertyValue.BooleanValue -> 1L
                is PropertyValue.DateTimeValue -> 32L
                else -> 0L
            }
        }
    }
    
    /**
     * Get or generate dynamic relationships with caching
     */
    suspend fun getOrGenerateDynamicRelationships(
        entityId: String,
        generator: suspend (String) -> List<Edge>
    ): List<Edge> = withContext(Dispatchers.IO) {
        
        val cacheKey = "relationships:$entityId"
        val currentEntry = relationshipCache[cacheKey]
        
        // Check if we have a valid cached entry
        if (currentEntry != null && !currentEntry.isExpired()) {
            relationshipCacheHits++
            Log.d(TAG, "Relationship cache HIT for $entityId")
            return@withContext currentEntry.relationships
        }
        
        // Generate fresh relationships
        relationshipCacheMisses++
        Log.d(TAG, "Relationship cache MISS for $entityId - generating")
        
        val relationships = generator(entityId)
        
        // Create cache entry with TTL
        val entry = RelationshipCacheEntry(
            relationships = relationships,
            sourceEntityId = entityId,
            contentFingerprint = generateRelationshipFingerprint(relationships),
            dependencies = extractRelationshipDependencies(entityId, relationships),
            timestamp = LocalDateTime.now(),
            ttlMinutes = 30 // Shorter TTL for dynamic relationships
        )
        
        relationshipCache[cacheKey] = entry
        
        relationships
    }
    
    /**
     * Invalidate relationship cache for specific entity
     */
    fun invalidateRelationshipCache(entityId: String) {
        val cacheKey = "relationships:$entityId"
        relationshipCache.remove(cacheKey)
        Log.d(TAG, "Invalidated relationship cache for $entityId")
    }
    
    /**
     * Clear all relationship cache entries
     */
    fun clearRelationshipCache() {
        relationshipCache.clear()
        relationshipCacheHits = 0L
        relationshipCacheMisses = 0L
        Log.d(TAG, "Cleared all relationship cache entries")
    }
    
    /**
     * Generate fingerprint for relationship list
     */
    private fun generateRelationshipFingerprint(relationships: List<Edge>): String {
        val contentString = relationships
            .sortedBy { "${it.fromEntityId}-${it.toEntityId}-${it.relationshipType}" }
            .joinToString("|") { edge ->
                "${edge.fromEntityId}->${edge.toEntityId}:${edge.relationshipType}"
            }
        return hashString(contentString).take(16)
    }
    
    /**
     * Extract dependencies for relationship caching
     */
    private fun extractRelationshipDependencies(entityId: String, relationships: List<Edge>): Set<String> {
        val dependencies = mutableSetOf<String>()
        
        // Relationships depend on the source entity
        dependencies.add("entity:$entityId")
        
        // Relationships depend on target entities
        relationships.forEach { edge ->
            dependencies.add("entity:${edge.toEntityId}")
            dependencies.add("entity:${edge.fromEntityId}")
        }
        
        // Relationships depend on catalog data for DEFINED_BY relationships
        if (relationships.any { it.relationshipType == RelationshipTypes.DEFINED_BY }) {
            dependencies.add("catalog_data")
        }
        
        return dependencies
    }
}

/**
 * Cache entry with content fingerprinting and dependency tracking
 */
data class FingerprintedCacheEntry<T : Entity>(
    val entity: T,
    val sourceEntityId: String,
    val derivationType: String,
    val contentFingerprint: String,
    val dependencies: Set<String>,
    val timestamp: LocalDateTime,
    val ttlMinutes: Int
) {
    fun isExpired(): Boolean {
        // Handle immediate expiration (TTL = 0) as special case for testing
        if (ttlMinutes == 0) {
            return true
        }
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
 * Cache entry specifically for relationship lists
 */
data class RelationshipCacheEntry(
    val relationships: List<Edge>,
    val sourceEntityId: String,
    val contentFingerprint: String,
    val dependencies: Set<String>,
    val timestamp: LocalDateTime,
    val ttlMinutes: Int
) {
    fun isExpired(): Boolean {
        if (ttlMinutes == 0) {
            return true
        }
        return LocalDateTime.now().isAfter(timestamp.plusMinutes(ttlMinutes.toLong()))
    }
}

/**
 * Cache performance statistics
 */
data class EntityCacheStatistics(
    val totalEntries: Int,
    val expiredEntries: Int,
    val hitRate: Float,
    val cacheHits: Long,
    val cacheMisses: Long,
    val contentChanges: Long,
    val dependencyChanges: Long,
    val expirations: Long,
    val memoryUsageBytes: Long,
    val relationshipCacheHits: Long = 0L,
    val relationshipCacheMisses: Long = 0L
)