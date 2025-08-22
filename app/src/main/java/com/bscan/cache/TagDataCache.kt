package com.bscan.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import com.bscan.model.NfcTagData
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Thread-safe cache for complete NFC tag data based on tag UIDs.
 * This cache stores the entire tag content, eliminating the need to 
 * re-authenticate and re-read sectors for previously seen tags.
 * 
 * Design goals:
 * - Avoid expensive sector authentication and block reading for known tags
 * - Store complete tag data for instant retrieval
 * - Thread-safe access for concurrent NFC operations
 * - Persistent storage with size limits and TTL
 */
class TagDataCache private constructor(
    context: Context,
    private val memorySize: Int = DEFAULT_MEMORY_SIZE,
    private val maxPersistentEntries: Int = DEFAULT_PERSISTENT_SIZE,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) {
    
    companion object {
        private const val TAG = "TagDataCache"
        private const val PREFS_NAME = "tag_data_cache"
        private const val CACHE_KEY = "cached_tag_data"
        private const val CACHE_METADATA_KEY = "cache_metadata"
        
        // Default configuration - much larger since we're caching full tag data
        private const val DEFAULT_MEMORY_SIZE = 1000 // Keep 1000 tags in memory
        private const val DEFAULT_PERSISTENT_SIZE = 10000 // Keep 10000 tags on disk
        private const val DEFAULT_TTL_MILLIS = 90 * 24 * 60 * 60 * 1000L // 90 days
        
        // Singleton instance with thread-safe initialization
        @Volatile
        private var INSTANCE: TagDataCache? = null
        
        fun getInstance(context: Context): TagDataCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TagDataCache(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        @VisibleForTesting
        fun getTestInstance(
            context: Context,
            memorySize: Int = DEFAULT_MEMORY_SIZE,
            maxPersistentEntries: Int = DEFAULT_PERSISTENT_SIZE,
            ttlMillis: Long = DEFAULT_TTL_MILLIS
        ): TagDataCache {
            return TagDataCache(context, memorySize, maxPersistentEntries, ttlMillis)
        }
    }
    
    // Thread synchronization
    private val rwLock = ReentrantReadWriteLock()
    
    // In-memory LRU cache with custom eviction handling
    private val memoryCache = object : LruCache<String, CachedTagData>(memorySize) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: CachedTagData,
            newValue: CachedTagData?
        ) {
            if (evicted) {
                Log.v(TAG, "Evicted tag data from memory cache: $key")
                // Promote to persistent storage if recently accessed
                if (System.currentTimeMillis() - oldValue.lastAccessTime < 300_000) { // 5 minutes
                    rwLock.write {
                        saveToPersistentStorage(key, oldValue)
                    }
                }
            }
        }
        
        override fun sizeOf(key: String, value: CachedTagData): Int {
            // Estimate size: UID string + tag data bytes + metadata
            return key.length + value.tagData.bytes.size + 100
        }
    }
    
    // Persistent storage
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // JSON serialization with custom adapters
    private val gson = GsonBuilder()
        .registerTypeAdapter(ByteArray::class.java, ByteArrayAdapter())
        .create()
    
    // Cache statistics for monitoring
    private val stats = TagCacheStatistics()
    
    init {
        Log.i(TAG, "Initializing TagDataCache with memory size: $memorySize, persistent size: $maxPersistentEntries, TTL: ${ttlMillis}ms")
        cleanupExpiredEntries()
    }
    
    /**
     * Retrieves cached tag data for a given UID.
     * Returns null if not cached or expired.
     */
    fun getCachedTagData(uid: String): NfcTagData? {
        val startTime = System.currentTimeMillis()
        
        try {
            // Try memory cache first
            rwLock.read {
                memoryCache.get(uid)?.let { entry ->
                    if (!entry.isExpired(ttlMillis)) {
                        stats.recordHit(CacheLevel.MEMORY)
                        entry.updateAccessTime()
                        Log.d(TAG, "Tag data cache HIT (memory) for UID: $uid")
                        return entry.tagData
                    } else {
                        Log.v(TAG, "Memory cache entry expired for UID: $uid")
                    }
                }
            }
            
            // Try persistent storage
            rwLock.read {
                loadFromPersistentStorage(uid)?.let { entry ->
                    if (!entry.isExpired(ttlMillis)) {
                        stats.recordHit(CacheLevel.PERSISTENT)
                        entry.updateAccessTime()
                        
                        // Promote to memory cache
                        rwLock.write {
                            memoryCache.put(uid, entry)
                        }
                        
                        Log.d(TAG, "Tag data cache HIT (persistent) for UID: $uid")
                        return entry.tagData
                    } else {
                        Log.v(TAG, "Persistent cache entry expired for UID: $uid")
                    }
                }
            }
            
            // Cache miss
            stats.recordMiss()
            Log.d(TAG, "Tag data cache MISS for UID: $uid")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving cached tag data for UID: $uid", e)
            stats.recordError()
            return null
        }
    }
    
    /**
     * Stores tag data in cache for future retrieval.
     */
    fun cacheTagData(tagData: NfcTagData) {
        val uid = tagData.uid
        val entry = CachedTagData(
            tagData = tagData,
            creationTime = System.currentTimeMillis(),
            lastAccessTime = System.currentTimeMillis()
        )
        
        try {
            // Store in both caches
            rwLock.write {
                memoryCache.put(uid, entry)
                saveToPersistentStorage(uid, entry)
            }
            
            Log.d(TAG, "Cached tag data for UID: $uid (${tagData.bytes.size} bytes)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching tag data for UID: $uid", e)
            stats.recordError()
        }
    }
    
    /**
     * Checks if tag data is cached for a given UID (without retrieving it).
     */
    fun isTagCached(uid: String): Boolean {
        rwLock.read {
            // Check memory cache
            memoryCache.get(uid)?.let { entry ->
                if (!entry.isExpired(ttlMillis)) {
                    return true
                }
            }
            
            // Check persistent storage
            loadFromPersistentStorage(uid)?.let { entry ->
                if (!entry.isExpired(ttlMillis)) {
                    return true
                }
            }
            
            return false
        }
    }
    
    /**
     * Invalidates cache entry for a specific UID
     */
    fun invalidateUID(uid: String) {
        rwLock.write {
            memoryCache.remove(uid)
            removeFromPersistentStorage(uid)
        }
        Log.d(TAG, "Invalidated tag data cache for UID: $uid")
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
        Log.i(TAG, "Cleared all cached tag data")
        stats.reset()
    }
    
    /**
     * Returns cache statistics for monitoring
     */
    fun getStatistics(): TagCacheStatistics {
        return stats.copy()
    }
    
    /**
     * Returns current cache sizes
     */
    fun getCacheSizes(): TagCacheSizeInfo {
        rwLock.read {
            val persistentSize = loadPersistentCacheMap().size
            return TagCacheSizeInfo(
                memorySize = memoryCache.size(),
                persistentSize = persistentSize,
                memoryMaxSize = memoryCache.maxSize(),
                persistentMaxSize = maxPersistentEntries,
                memoryUsageBytes = memoryCache.size() * 1000 // Rough estimate
            )
        }
    }
    
    // Private implementation methods
    
    private fun loadFromPersistentStorage(uid: String): CachedTagData? {
        try {
            val cacheMap = loadPersistentCacheMap()
            return cacheMap[uid]
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from persistent storage for UID: $uid", e)
            return null
        }
    }
    
    private fun saveToPersistentStorage(uid: String, entry: CachedTagData) {
        try {
            val cacheMap = loadPersistentCacheMap().toMutableMap()
            cacheMap[uid] = entry
            
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
            Log.v(TAG, "Saved tag data to persistent storage for UID: $uid")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to persistent storage for UID: $uid", e)
        }
    }
    
    private fun removeFromPersistentStorage(uid: String) {
        try {
            val cacheMap = loadPersistentCacheMap().toMutableMap()
            cacheMap.remove(uid)
            savePersistentCacheMap(cacheMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from persistent storage for UID: $uid", e)
        }
    }
    
    private fun loadPersistentCacheMap(): Map<String, CachedTagData> {
        val cacheJson = sharedPreferences.getString(CACHE_KEY, null) ?: return emptyMap()
        
        return try {
            val type = object : TypeToken<Map<String, CachedTagData>>() {}.type
            gson.fromJson(cacheJson, type) ?: emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing persistent cache, clearing corrupted data", e)
            sharedPreferences.edit().remove(CACHE_KEY).apply()
            emptyMap()
        }
    }
    
    private fun savePersistentCacheMap(cacheMap: Map<String, CachedTagData>) {
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
                        Log.i(TAG, "Cleanup removed $removedCount expired tag data entries from persistent storage")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during tag data cache cleanup", e)
            }
        }.start()
    }
}

/**
 * Represents cached tag data with metadata
 */
data class CachedTagData(
    val tagData: NfcTagData,
    val creationTime: Long,
    var lastAccessTime: Long
) {
    fun isExpired(ttlMillis: Long): Boolean {
        return System.currentTimeMillis() - creationTime > ttlMillis
    }
    
    fun updateAccessTime() {
        lastAccessTime = System.currentTimeMillis()
    }
}

/**
 * Cache performance statistics for tag data
 */
data class TagCacheStatistics(
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
    
    fun copy(): TagCacheStatistics {
        return TagCacheStatistics(memoryHits, persistentHits, misses, errors, invalidations)
    }
}

data class TagCacheSizeInfo(
    val memorySize: Int,
    val persistentSize: Int,
    val memoryMaxSize: Int,
    val persistentMaxSize: Int,
    val memoryUsageBytes: Int
)

// Custom Gson adapter for ByteArray serialization (reuse from existing cache)
private class ByteArrayAdapter : JsonSerializer<ByteArray>, JsonDeserializer<ByteArray> {
    override fun serialize(src: ByteArray?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.let { android.util.Base64.encodeToString(it, android.util.Base64.NO_WRAP) })
    }
    
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ByteArray? {
        return json?.asString?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
    }
}