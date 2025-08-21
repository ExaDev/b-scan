package com.bscan.cache

import android.content.Context
import android.util.Log
import com.bscan.nfc.BambuKeyDerivation

/**
 * Cached wrapper for BambuKeyDerivation that provides transparent caching
 * of expensive HKDF operations. This class maintains backward compatibility
 * while adding caching capabilities.
 * 
 * Usage:
 * - Replace direct BambuKeyDerivation.deriveKeys() calls with this wrapper
 * - Cache is automatically managed with sensible defaults
 * - Provides additional methods for cache management and monitoring
 */
object CachedBambuKeyDerivation {
    private const val TAG = "CachedBambuKeyDerivation"
    
    @Volatile
    private var keyCache: DerivedKeyCache? = null
    
    /**
     * Initialize the cache with application context.
     * Should be called during application startup.
     */
    fun initialize(context: Context) {
        if (keyCache == null) {
            synchronized(this) {
                if (keyCache == null) {
                    keyCache = DerivedKeyCache.getInstance(context)
                    Log.i(TAG, "Initialized cached key derivation")
                }
            }
        }
    }
    
    /**
     * Derives authentication keys from UID with caching.
     * This is a drop-in replacement for BambuKeyDerivation.deriveKeys().
     * 
     * @param uid The NFC tag UID
     * @return Array of derived 6-byte authentication keys
     */
    fun deriveKeys(uid: ByteArray): Array<ByteArray> {
        val cache = keyCache
        
        return if (cache != null) {
            cache.getDerivedKeys(uid)
        } else {
            // Fallback to direct computation if cache not initialized
            Log.w(TAG, "Cache not initialized, falling back to direct computation")
            BambuKeyDerivation.deriveKeys(uid)
        }
    }
    
    /**
     * Preloads keys for a UID in the background.
     * Useful when you know a tag will be scanned soon.
     */
    fun preloadKeys(uid: ByteArray) {
        keyCache?.preloadKeys(uid)
    }
    
    /**
     * Invalidates cached keys for a specific UID.
     * Use this if you know the keys for a UID have changed.
     */
    fun invalidateUID(uid: ByteArray) {
        keyCache?.invalidateUID(uid)
    }
    
    /**
     * Clears all cached keys.
     * Use this for troubleshooting or privacy reasons.
     */
    fun clearCache() {
        keyCache?.clearAll()
        Log.i(TAG, "Cache cleared")
    }
    
    /**
     * Gets cache performance statistics for monitoring.
     */
    fun getCacheStatistics(): CacheStatistics? {
        return keyCache?.getStatistics()
    }
    
    /**
     * Gets current cache size information.
     */
    fun getCacheSizes(): CacheSizeInfo? {
        return keyCache?.getCacheSizes()
    }
    
    /**
     * Checks if the cache is initialized and ready.
     */
    fun isCacheInitialized(): Boolean {
        return keyCache != null
    }
    
    /**
     * Gets cache hit rate as a percentage (0.0 to 1.0).
     */
    fun getCacheHitRate(): Float {
        return keyCache?.getStatistics()?.getHitRate() ?: 0f
    }
    
    /**
     * Logs current cache statistics for debugging.
     */
    fun logCacheStatistics() {
        val stats = getCacheStatistics()
        val sizes = getCacheSizes()
        
        if (stats != null && sizes != null) {
            val hitRate = (stats.getHitRate() * 100).toInt()
            Log.i(TAG, "Cache Statistics:")
            Log.i(TAG, "  Hit Rate: $hitRate% (${stats.getTotalHits()}/${stats.getTotalRequests()})")
            Log.i(TAG, "  Memory Hits: ${stats.memoryHits}")
            Log.i(TAG, "  Persistent Hits: ${stats.persistentHits}")
            Log.i(TAG, "  Misses: ${stats.misses}")
            Log.i(TAG, "  Errors: ${stats.errors}")
            Log.i(TAG, "  Cache Sizes: Memory ${sizes.memorySize}/${sizes.memoryMaxSize}, Persistent ${sizes.persistentSize}/${sizes.persistentMaxSize}")
        } else {
            Log.w(TAG, "Cache statistics not available - cache may not be initialized")
        }
    }
}

/**
 * Extension functions for easier migration from existing code
 */

/**
 * Extension function for ByteArray to derive keys with caching
 */
fun ByteArray.deriveCachedKeys(): Array<ByteArray> {
    return CachedBambuKeyDerivation.deriveKeys(this)
}

/**
 * Extension function for ByteArray to preload keys
 */
fun ByteArray.preloadKeys() {
    CachedBambuKeyDerivation.preloadKeys(this)
}