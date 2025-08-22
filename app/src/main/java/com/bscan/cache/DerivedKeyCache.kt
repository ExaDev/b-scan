package com.bscan.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import com.bscan.nfc.BambuKeyDerivation
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe cache for derived HKDF keys based on NFC tag UIDs.
 * Implements a two-tier caching strategy:
 * 1. In-memory LRU cache for fast access
 * 2. Persistent storage for cache survival across app restarts
 * 
 * Design goals:
 * - Avoid expensive HKDF computations for previously seen UIDs
 * - Thread-safe access for concurrent NFC operations
 * - Persistent storage with size limits and TTL
 * - Security considerations for key storage
 */
class DerivedKeyCache private constructor(
    context: Context,
    private val memorySize: Int = DEFAULT_MEMORY_SIZE,
    private val maxPersistentEntries: Int = DEFAULT_PERSISTENT_SIZE,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) {
    
    companion object {
        private const val TAG = "DerivedKeyCache"
        private const val PREFS_NAME = "derived_key_cache"
        private const val CACHE_KEY = "cached_keys"
        private const val CACHE_METADATA_KEY = "cache_metadata"
        
        // Default configuration
        private const val DEFAULT_MEMORY_SIZE = 5000 // Keep 5000 UIDs in memory
        private const val DEFAULT_PERSISTENT_SIZE = 20000 // Keep 20000 UIDs on disk
        private const val DEFAULT_TTL_MILLIS = 30 * 24 * 60 * 60 * 1000L // 30 days
        
        // Singleton instance with thread-safe initialization
        @Volatile
        private var INSTANCE: DerivedKeyCache? = null
        
        fun getInstance(context: Context): DerivedKeyCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DerivedKeyCache(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        @VisibleForTesting
        fun getTestInstance(
            context: Context,
            memorySize: Int = DEFAULT_MEMORY_SIZE,
            maxPersistentEntries: Int = DEFAULT_PERSISTENT_SIZE,
            ttlMillis: Long = DEFAULT_TTL_MILLIS
        ): DerivedKeyCache {
            return DerivedKeyCache(context, memorySize, maxPersistentEntries, ttlMillis)
        }
    }
    
    // Thread synchronization
    private val rwLock = ReentrantReadWriteLock()
    
    // In-memory LRU cache with custom eviction handling
    private val memoryCache = object : LruCache<String, CachedKeyEntry>(memorySize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: CachedKeyEntry,
            newValue: CachedKeyEntry?
        ) {
            if (evicted) {
                Log.v(TAG, "Evicted key from memory cache: $key")
                // Optionally promote to persistent storage if recently accessed
                if (System.currentTimeMillis() - oldValue.lastAccessTime < 60_000) { // 1 minute
                    rwLock.write {
                        saveToPersistentStorage(key, oldValue)
                    }
                }
            }
        }
    }
    
    // Persistent storage
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // JSON serialization with custom adapters
    private val gson = GsonBuilder()
        .registerTypeAdapter(ByteArray::class.java, KeyCacheByteArrayAdapter())
        .registerTypeAdapter(Array<ByteArray>::class.java, ByteArrayArrayAdapter())
        .create()
    
    // Cache statistics for monitoring
    private val stats = CacheStatistics()
    
    init {
        Log.i(TAG, "Initializing DerivedKeyCache with memory size: $memorySize, persistent size: $maxPersistentEntries, TTL: ${ttlMillis}ms")
        cleanupExpiredEntries()
    }
    
    /**
     * Retrieves derived keys for a given UID. Uses cache when possible,
     * computes and stores otherwise.
     */
    fun getDerivedKeys(uid: ByteArray): Array<ByteArray> {
        val uidHex = bytesToHex(uid)
        val startTime = System.currentTimeMillis()
        
        try {
            // Try memory cache first
            rwLock.read {
                memoryCache.get(uidHex)?.let { entry ->
                    if (!entry.isExpired(ttlMillis)) {
                        stats.recordHit(CacheLevel.MEMORY)
                        entry.updateAccessTime()
                        Log.d(TAG, "Cache HIT (memory) for UID: $uidHex")
                        return entry.keys
                    } else {
                        Log.v(TAG, "Memory cache entry expired for UID: $uidHex")
                    }
                }
            }
            
            // Try persistent storage
            rwLock.read {
                loadFromPersistentStorage(uidHex)?.let { entry ->
                    if (!entry.isExpired(ttlMillis)) {
                        stats.recordHit(CacheLevel.PERSISTENT)
                        entry.updateAccessTime()
                        
                        // Promote to memory cache
                        rwLock.write {
                            memoryCache.put(uidHex, entry)
                        }
                        
                        Log.d(TAG, "Cache HIT (persistent) for UID: $uidHex")
                        return entry.keys
                    } else {
                        Log.v(TAG, "Persistent cache entry expired for UID: $uidHex")
                    }
                }
            }
            
            // Cache miss - compute keys
            stats.recordMiss()
            Log.d(TAG, "Cache MISS for UID: $uidHex - computing keys")
            
            val derivedKeys = BambuKeyDerivation.deriveKeys(uid)
            val entry = CachedKeyEntry(
                keys = derivedKeys,
                creationTime = System.currentTimeMillis(),
                lastAccessTime = System.currentTimeMillis()
            )
            
            // Store in both caches
            rwLock.write {
                memoryCache.put(uidHex, entry)
                saveToPersistentStorage(uidHex, entry)
            }
            
            val computeTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "Computed and cached keys for UID: $uidHex in ${computeTime}ms")
            
            return derivedKeys
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving keys for UID: $uidHex", e)
            stats.recordError()
            // Fallback to direct computation
            return BambuKeyDerivation.deriveKeys(uid)
        }
    }
    
    /**
     * Preloads keys for a UID (useful for background preparation)
     */
    fun preloadKeys(uid: ByteArray) {
        val uidHex = bytesToHex(uid)
        Log.d(TAG, "Preloading keys for UID: $uidHex")
        
        // Check if already cached
        rwLock.read {
            memoryCache.get(uidHex)?.let { entry ->
                if (!entry.isExpired(ttlMillis)) {
                    Log.v(TAG, "Keys already cached for UID: $uidHex")
                    return
                }
            }
        }
        
        // Compute and cache in background thread
        Thread {
            try {
                getDerivedKeys(uid)
                Log.v(TAG, "Successfully preloaded keys for UID: $uidHex")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload keys for UID: $uidHex", e)
            }
        }.start()
    }
    
    /**
     * Invalidates cache entry for a specific UID
     */
    fun invalidateUID(uid: ByteArray) {
        val uidHex = bytesToHex(uid)
        rwLock.write {
            memoryCache.remove(uidHex)
            removeFromPersistentStorage(uidHex)
        }
        Log.d(TAG, "Invalidated cache for UID: $uidHex")
        stats.recordInvalidation()
    }
    
    /**
     * Clears all cached data
     */
    fun clearAll() {
        rwLock.write {
            memoryCache.evictAll()
            sharedPreferences.edit()
                .remove(CACHE_KEY)
                .remove(CACHE_METADATA_KEY)
                .apply()
        }
        Log.i(TAG, "Cleared all cached data")
        stats.reset()
    }
    
    /**
     * Returns cache statistics for monitoring
     */
    fun getStatistics(): CacheStatistics {
        return stats.copy()
    }
    
    /**
     * Returns current cache sizes
     */
    fun getCacheSizes(): CacheSizeInfo {
        rwLock.read {
            val persistentSize = loadPersistentCacheMap().size
            return CacheSizeInfo(
                memorySize = memoryCache.size(),
                persistentSize = persistentSize,
                memoryMaxSize = memoryCache.maxSize(),
                persistentMaxSize = maxPersistentEntries
            )
        }
    }
    
    // Private implementation methods
    
    private fun loadFromPersistentStorage(uidHex: String): CachedKeyEntry? {
        try {
            val cacheMap = loadPersistentCacheMap()
            return cacheMap[uidHex]
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from persistent storage for UID: $uidHex", e)
            return null
        }
    }
    
    private fun saveToPersistentStorage(uidHex: String, entry: CachedKeyEntry) {
        try {
            val cacheMap = loadPersistentCacheMap().toMutableMap()
            cacheMap[uidHex] = entry
            
            // Enforce size limit with LRU eviction
            if (cacheMap.size > maxPersistentEntries) {
                val sortedEntries = cacheMap.entries.sortedBy { it.value.lastAccessTime }
                val entriesToRemove = cacheMap.size - maxPersistentEntries
                repeat(entriesToRemove) {
                    if (sortedEntries.isNotEmpty()) {
                        cacheMap.remove(sortedEntries[it].key)
                    }
                }
                Log.v(TAG, "Evicted $entriesToRemove entries from persistent storage")
            }
            
            savePersistentCacheMap(cacheMap)
            Log.v(TAG, "Saved entry to persistent storage for UID: $uidHex")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to persistent storage for UID: $uidHex", e)
        }
    }
    
    private fun removeFromPersistentStorage(uidHex: String) {
        try {
            val cacheMap = loadPersistentCacheMap().toMutableMap()
            cacheMap.remove(uidHex)
            savePersistentCacheMap(cacheMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from persistent storage for UID: $uidHex", e)
        }
    }
    
    private fun loadPersistentCacheMap(): Map<String, CachedKeyEntry> {
        val cacheJson = sharedPreferences.getString(CACHE_KEY, null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, CachedKeyEntry>>() {}.type
            gson.fromJson(cacheJson, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing persistent cache, clearing corrupted data", e)
            sharedPreferences.edit().remove(CACHE_KEY).apply()
            emptyMap()
        }
    }
    
    private fun savePersistentCacheMap(cacheMap: Map<String, CachedKeyEntry>) {
        try {
            val cacheJson = gson.toJson(cacheMap)
            sharedPreferences.edit()
                .putString(CACHE_KEY, cacheJson)
                .putLong(CACHE_METADATA_KEY, System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing persistent cache", e)
        }
    }
    
    private fun cleanupExpiredEntries() {
        Thread {
            try {
                rwLock.write {
                    val cacheMap = loadPersistentCacheMap().toMutableMap()
                    val initialSize = cacheMap.size
                    
                    val iterator = cacheMap.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (entry.value.isExpired(ttlMillis)) {
                            iterator.remove()
                        }
                    }
                    
                    val removedCount = initialSize - cacheMap.size
                    if (removedCount > 0) {
                        savePersistentCacheMap(cacheMap)
                        Log.i(TAG, "Cleanup removed $removedCount expired entries from persistent storage")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during cache cleanup", e)
            }
        }.start()
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}

/**
 * Represents a cached key entry with metadata
 */
data class CachedKeyEntry(
    val keys: Array<ByteArray>,
    val creationTime: Long,
    var lastAccessTime: Long
) {
    fun isExpired(ttlMillis: Long): Boolean {
        return System.currentTimeMillis() - creationTime > ttlMillis
    }
    
    fun updateAccessTime() {
        lastAccessTime = System.currentTimeMillis()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as CachedKeyEntry
        
        if (!keys.contentDeepEquals(other.keys)) return false
        if (creationTime != other.creationTime) return false
        if (lastAccessTime != other.lastAccessTime) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = keys.contentDeepHashCode()
        result = 31 * result + creationTime.hashCode()
        result = 31 * result + lastAccessTime.hashCode()
        return result
    }
}

/**
 * Cache performance statistics
 */
data class CacheStatistics(
    var memoryHits: Long = 0,
    var persistentHits: Long = 0,
    var misses: Long = 0,
    var errors: Long = 0,
    var invalidations: Long = 0
) {
    fun recordHit(level: CacheLevel) {
        when (level) {
            CacheLevel.MEMORY -> memoryHits++
            CacheLevel.PERSISTENT -> persistentHits++
        }
    }
    
    fun recordMiss() { misses++ }
    fun recordError() { errors++ }
    fun recordInvalidation() { invalidations++ }
    
    fun getTotalHits(): Long = memoryHits + persistentHits
    fun getTotalRequests(): Long = getTotalHits() + misses
    
    fun getHitRate(): Float {
        val total = getTotalRequests()
        return if (total > 0) getTotalHits().toFloat() / total else 0f
    }
    
    fun reset() {
        memoryHits = 0
        persistentHits = 0
        misses = 0
        errors = 0
        invalidations = 0
    }
    
    fun copy(): CacheStatistics {
        return CacheStatistics(memoryHits, persistentHits, misses, errors, invalidations)
    }
}

enum class CacheLevel {
    MEMORY, PERSISTENT
}

data class CacheSizeInfo(
    val memorySize: Int,
    val persistentSize: Int,
    val memoryMaxSize: Int,
    val persistentMaxSize: Int
)

// Custom Gson adapters for ByteArray serialization
private class KeyCacheByteArrayAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
    override fun serialize(src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) })
    }
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ByteArray? {
        return json?.asString?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
    }
}

private class ByteArrayArrayAdapter : JsonSerializer<Array<ByteArray>>, JsonDeserializer<Array<ByteArray>> {
    override fun serialize(src: Array<ByteArray>?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return context?.serialize(src?.map { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) }) ?: JsonNull.INSTANCE
    }
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Array<ByteArray>? {
        val type = object : TypeToken<List<String>>() {}.type
        val stringList: List<String>? = context?.deserialize(json, type)
        return stringList?.map { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }?.toTypedArray()
    }
}