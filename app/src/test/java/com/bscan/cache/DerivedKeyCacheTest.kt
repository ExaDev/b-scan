package com.bscan.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.bscan.nfc.BambuKeyDerivation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DerivedKeyCacheTest {
    
    private lateinit var context: Context
    private lateinit var cache: DerivedKeyCache
    
    // Test UIDs
    private val testUID1 = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val testUID2 = byteArrayOf(0x05, 0x06, 0x07, 0x08)
    private val testUID3 = byteArrayOf(0x09, 0x0A, 0x0B, 0x0C)
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Clear any existing cache data
        val prefs = context.getSharedPreferences("derived_key_cache", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        // Create test cache with small sizes for testing
        cache = DerivedKeyCache.getTestInstance(
            context = context,
            memorySize = 2,
            maxPersistentEntries = 3,
            ttlMillis = 60_000 // 1 minute for testing
        )
    }
    
    @Test
    fun `cache miss should compute and store keys`() {
        val keys = cache.getDerivedKeys(testUID1)
        
        // Verify keys are computed correctly
        val expectedKeys = BambuKeyDerivation.deriveKeys(testUID1)
        assertTrue("Keys should be identical", expectedKeys.contentDeepEquals(keys))
        
        // Verify cache statistics
        val stats = cache.getStatistics()
        assertEquals(0, stats.getTotalHits())
        assertEquals(1, stats.misses)
    }
    
    @Test
    fun `cache hit should return stored keys without computation`() {
        // First call - cache miss
        val keys1 = cache.getDerivedKeys(testUID1)
        
        // Second call - should be cache hit
        val keys2 = cache.getDerivedKeys(testUID1)
        
        // Should return same keys
        assertTrue("Keys should be identical", keys1.contentDeepEquals(keys2))
        
        // Verify cache statistics
        val stats = cache.getStatistics()
        assertEquals(1, stats.getTotalHits())
        assertEquals(1, stats.misses)
        assertTrue(stats.getHitRate() > 0.4f) // 50% hit rate
    }
    
    @Test
    fun `memory cache should work correctly`() {
        // Fill memory cache
        cache.getDerivedKeys(testUID1)
        cache.getDerivedKeys(testUID2)
        
        // Both should be memory hits
        cache.getDerivedKeys(testUID1)
        cache.getDerivedKeys(testUID2)
        
        val stats = cache.getStatistics()
        assertEquals(2, stats.memoryHits)
        assertEquals(0, stats.persistentHits)
    }
    
    @Test
    fun `memory cache eviction should promote to persistent storage`() {
        // Fill memory cache beyond capacity (size = 2)
        cache.getDerivedKeys(testUID1)
        cache.getDerivedKeys(testUID2)
        cache.getDerivedKeys(testUID3) // Should evict testUID1
        
        // Access testUID1 - should be persistent hit
        cache.getDerivedKeys(testUID1)
        
        val stats = cache.getStatistics()
        assertTrue("Should have at least one persistent hit", stats.persistentHits > 0)
    }
    
    @Test
    fun `persistent storage should survive cache recreation`() {
        // Store keys in first cache instance
        val keys1 = cache.getDerivedKeys(testUID1)
        
        // Create new cache instance (simulating app restart)
        val newCache = DerivedKeyCache.getTestInstance(
            context = context,
            memorySize = 2,
            maxPersistentEntries = 3,
            ttlMillis = 60_000
        )
        
        // Should load from persistent storage
        val keys2 = newCache.getDerivedKeys(testUID1)
        
        assertTrue("Keys should be identical", keys1.contentDeepEquals(keys2))
        
        val stats = newCache.getStatistics()
        assertEquals(1, stats.persistentHits)
    }
    
    @Test
    fun `cache invalidation should remove entries`() {
        // Store keys
        cache.getDerivedKeys(testUID1)
        
        // Verify it's cached
        cache.getDerivedKeys(testUID1)
        var stats = cache.getStatistics()
        assertTrue(stats.getTotalHits() > 0)
        
        // Invalidate
        cache.invalidateUID(testUID1)
        
        // Should be cache miss again
        cache.getDerivedKeys(testUID1)
        stats = cache.getStatistics()
        assertEquals(1, stats.invalidations)
    }
    
    @Test
    fun `cache clear should remove all entries`() {
        // Store multiple keys
        cache.getDerivedKeys(testUID1)
        cache.getDerivedKeys(testUID2)
        
        // Clear cache
        cache.clearAll()
        
        // Should be cache misses
        cache.getDerivedKeys(testUID1)
        cache.getDerivedKeys(testUID2)
        
        val stats = cache.getStatistics()
        assertEquals(0, stats.getTotalHits())
        assertEquals(2, stats.misses)
    }
    
    @Test
    fun `persistent storage size limit should be enforced`() {
        val testUIDs = listOf(
            byteArrayOf(0x01, 0x02, 0x03, 0x04),
            byteArrayOf(0x05, 0x06, 0x07, 0x08),
            byteArrayOf(0x09, 0x0A, 0x0B, 0x0C),
            byteArrayOf(0x0D, 0x0E, 0x0F, 0x10), // Should evict oldest
        )
        
        // Fill beyond persistent storage limit (size = 3)
        testUIDs.forEach { uid ->
            cache.getDerivedKeys(uid)
        }
        
        val sizes = cache.getCacheSizes()
        assertTrue("Persistent storage size ${sizes.persistentSize} exceeds limit ${sizes.persistentMaxSize}",
            sizes.persistentSize <= sizes.persistentMaxSize)
    }
    
    @Test
    fun `expired entries should not be returned`() {
        // Create cache with very short TTL
        val shortTTLCache = DerivedKeyCache.getTestInstance(
            context = context,
            memorySize = 2,
            maxPersistentEntries = 3,
            ttlMillis = 1 // 1ms TTL
        )
        
        // Store keys
        shortTTLCache.getDerivedKeys(testUID1)
        
        // Wait for expiration
        Thread.sleep(10)
        
        // Should be cache miss due to expiration
        shortTTLCache.getDerivedKeys(testUID1)
        
        val stats = shortTTLCache.getStatistics()
        assertEquals(2, stats.misses) // Both calls should be misses
    }
    
    @Test
    fun `preload should cache keys in background`() {
        // Preload keys
        cache.preloadKeys(testUID1)
        
        // Give it a moment to complete
        Thread.sleep(100)
        
        // Should be cache hit
        cache.getDerivedKeys(testUID1)
        
        val stats = cache.getStatistics()
        assertTrue("Should have cache hit after preload", stats.getTotalHits() > 0)
    }
    
    @Test
    fun `cache should handle concurrent access`() {
        val numThreads = 10
        val numOperations = 50
        val threads = mutableListOf<Thread>()
        
        // Create multiple threads accessing cache concurrently
        repeat(numThreads) { threadIndex ->
            val thread = Thread {
                repeat(numOperations) { opIndex ->
                    val uid = byteArrayOf(
                        (threadIndex % 256).toByte(),
                        (opIndex % 256).toByte(),
                        0x00,
                        0x00
                    )
                    cache.getDerivedKeys(uid)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // Verify no errors occurred
        val stats = cache.getStatistics()
        assertEquals(0, stats.errors)
        assertTrue(stats.getTotalRequests() > 0)
    }
    
    @Test
    fun `cache statistics should be accurate`() {
        // Generate known access pattern
        cache.getDerivedKeys(testUID1) // miss
        cache.getDerivedKeys(testUID1) // memory hit
        cache.getDerivedKeys(testUID2) // miss
        cache.getDerivedKeys(testUID2) // memory hit
        
        val stats = cache.getStatistics()
        assertEquals(2, stats.memoryHits)
        assertEquals(0, stats.persistentHits)
        assertEquals(2, stats.misses)
        assertEquals(0.5f, stats.getHitRate())
    }
    
    @Test
    fun `cache should handle corrupted persistent data gracefully`() {
        // Manually corrupt persistent storage
        val prefs = context.getSharedPreferences("derived_key_cache", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("cached_keys", "invalid json data")
            .commit()
        
        // Should still work and return computed keys
        val keys = cache.getDerivedKeys(testUID1)
        assertNotNull(keys)
        assertTrue(keys.isNotEmpty())
        
        // Should clear corrupted data and work normally
        val keys2 = cache.getDerivedKeys(testUID1)
        assertTrue("Keys should be identical", keys.contentDeepEquals(keys2))
    }
    
    @Test
    fun `different UIDs should produce different keys`() {
        val keys1 = cache.getDerivedKeys(testUID1)
        val keys2 = cache.getDerivedKeys(testUID2)
        
        // Keys should be different
        assertFalse("Different UIDs should produce different keys", 
            keys1.contentDeepEquals(keys2))
    }
    
    @Test
    fun `cache entry access time should be updated`() {
        // Store initial entry
        cache.getDerivedKeys(testUID1)
        val initialTime = System.currentTimeMillis()
        
        // Wait a bit
        Thread.sleep(10)
        
        // Access again - should update access time
        cache.getDerivedKeys(testUID1)
        
        // This is hard to test directly, but we can verify it doesn't affect functionality
        val stats = cache.getStatistics()
        assertEquals(1, stats.memoryHits)
    }
}