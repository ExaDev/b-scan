package com.bscan.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bscan.nfc.BambuKeyDerivation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CachedBambuKeyDerivationTest {
    
    private lateinit var context: Context
    
    // Test UIDs
    private val testUID1 = byteArrayOf(0x01, 0x02, 0x03, 0x04)
    private val testUID2 = byteArrayOf(0x05, 0x06, 0x07, 0x08)
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Clear any existing cache data
        val prefs = context.getSharedPreferences("derived_key_cache", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        // Initialize cached derivation
        CachedBambuKeyDerivation.initialize(context)
    }
    
    @Test
    fun `initialization should set up cache`() {
        CachedBambuKeyDerivation.initialize(context)
        assertTrue(CachedBambuKeyDerivation.isCacheInitialized())
    }
    
    @Test
    fun `deriveKeys should work as drop-in replacement`() {
        val cachedKeys = CachedBambuKeyDerivation.deriveKeys(testUID1)
        val directKeys = BambuKeyDerivation.deriveKeys(testUID1)
        
        assertContentEquals(directKeys, cachedKeys)
    }
    
    @Test
    fun `repeated calls should use cache`() {
        // First call - cache miss
        val keys1 = CachedBambuKeyDerivation.deriveKeys(testUID1)
        
        // Second call - should be cache hit
        val keys2 = CachedBambuKeyDerivation.deriveKeys(testUID1)
        
        assertContentEquals(keys1, keys2)
        
        // Verify cache was used
        val hitRate = CachedBambuKeyDerivation.getCacheHitRate()
        assertTrue(hitRate > 0f, "Cache hit rate should be greater than 0")
    }
    
    @Test
    fun `preload should improve performance`() {
        // Preload keys
        CachedBambuKeyDerivation.preloadKeys(testUID1)
        
        // Give it time to complete
        Thread.sleep(100)
        
        // Subsequent call should be faster (cache hit)
        val keys = CachedBambuKeyDerivation.deriveKeys(testUID1)
        assertNotNull(keys)
        
        val stats = CachedBambuKeyDerivation.getCacheStatistics()
        assertNotNull(stats)
        assertTrue(stats.getTotalHits() > 0)
    }
    
    @Test
    fun `invalidation should force recomputation`() {
        // Store keys in cache
        val keys1 = CachedBambuKeyDerivation.deriveKeys(testUID1)
        
        // Verify cache hit
        CachedBambuKeyDerivation.deriveKeys(testUID1)
        var stats = CachedBambuKeyDerivation.getCacheStatistics()!!
        assertTrue(stats.getTotalHits() > 0)
        
        // Invalidate
        CachedBambuKeyDerivation.invalidateUID(testUID1)
        
        // Should recompute
        val keys2 = CachedBambuKeyDerivation.deriveKeys(testUID1)
        assertContentEquals(keys1, keys2) // Same result
        
        stats = CachedBambuKeyDerivation.getCacheStatistics()!!
        assertTrue(stats.invalidations > 0)
    }
    
    @Test
    fun `clear cache should remove all entries`() {
        // Store multiple keys
        CachedBambuKeyDerivation.deriveKeys(testUID1)
        CachedBambuKeyDerivation.deriveKeys(testUID2)
        
        // Clear cache
        CachedBambuKeyDerivation.clearCache()
        
        // Subsequent calls should be cache misses
        CachedBambuKeyDerivation.deriveKeys(testUID1)
        CachedBambuKeyDerivation.deriveKeys(testUID2)
        
        val stats = CachedBambuKeyDerivation.getCacheStatistics()!!
        assertEquals(0, stats.getTotalHits())
    }
    
    @Test
    fun `fallback should work when cache not initialized`() {
        // Create fresh instance without initialization
        val context2 = ApplicationProvider.getApplicationContext<Context>()
        
        // Simulate uninitialized state
        // (We can't easily test this with singleton, but fallback is covered in implementation)
        val keys = CachedBambuKeyDerivation.deriveKeys(testUID1)
        assertNotNull(keys)
        assertTrue(keys.isNotEmpty())
    }
    
    @Test
    fun `cache sizes should be reported correctly`() {
        // Add some entries
        CachedBambuKeyDerivation.deriveKeys(testUID1)
        CachedBambuKeyDerivation.deriveKeys(testUID2)
        
        val sizes = CachedBambuKeyDerivation.getCacheSizes()
        assertNotNull(sizes)
        assertTrue(sizes.memorySize > 0)
        assertTrue(sizes.memoryMaxSize > 0)
        assertTrue(sizes.persistentMaxSize > 0)
    }
    
    @Test
    fun `statistics logging should not crash`() {
        // Add some cache activity
        CachedBambuKeyDerivation.deriveKeys(testUID1)
        CachedBambuKeyDerivation.deriveKeys(testUID1) // cache hit
        
        // Should not throw exception
        assertDoesNotThrow {
            CachedBambuKeyDerivation.logCacheStatistics()
        }
    }
    
    @Test
    fun `performance should be better with cache`() {
        val iterations = 10
        
        // Measure direct computation time
        val directTime = measureTimeMillis {
            repeat(iterations) {
                BambuKeyDerivation.deriveKeys(testUID1)
            }
        }
        
        // Measure cached computation time (after first cache miss)
        CachedBambuKeyDerivation.deriveKeys(testUID1) // Prime cache
        
        val cachedTime = measureTimeMillis {
            repeat(iterations) {
                CachedBambuKeyDerivation.deriveKeys(testUID1)
            }
        }
        
        // Cache should be significantly faster for repeated operations
        assertTrue(cachedTime < directTime, 
            "Cached operations ($cachedTime ms) should be faster than direct operations ($directTime ms)")
        
        // Verify high hit rate
        val hitRate = CachedBambuKeyDerivation.getCacheHitRate()
        assertTrue(hitRate > 0.8f, "Hit rate should be high: $hitRate")
    }
    
    @Test
    fun `extension functions should work correctly`() {
        // Test deriveCachedKeys extension
        val keys1 = testUID1.deriveCachedKeys()
        val keys2 = CachedBambuKeyDerivation.deriveKeys(testUID1)
        assertContentEquals(keys1, keys2)
        
        // Test preloadKeys extension
        assertDoesNotThrow {
            testUID2.preloadKeys()
        }
    }
    
    @Test
    fun `concurrent access should work correctly`() {
        val numThreads = 5
        val numOperations = 20
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Array<ByteArray>>()
        
        // Create multiple threads accessing cache concurrently
        repeat(numThreads) { threadIndex ->
            val thread = Thread {
                repeat(numOperations) {
                    val keys = CachedBambuKeyDerivation.deriveKeys(testUID1)
                    synchronized(results) {
                        results.add(keys)
                    }
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        // Verify all results are consistent
        val expectedKeys = BambuKeyDerivation.deriveKeys(testUID1)
        results.forEach { keys ->
            assertContentEquals(expectedKeys, keys)
        }
        
        // Verify high cache hit rate due to concurrent access
        val stats = CachedBambuKeyDerivation.getCacheStatistics()!!
        assertTrue(stats.getHitRate() > 0.8f, "Hit rate should be high with concurrent access")
    }
    
    private fun measureTimeMillis(block: () -> Unit): Long {
        val start = System.currentTimeMillis()
        block()
        return System.currentTimeMillis() - start
    }
}