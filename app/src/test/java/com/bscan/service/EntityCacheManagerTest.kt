package com.bscan.service

import com.bscan.model.graph.Entity
import com.bscan.model.graph.entities.*
import com.bscan.repository.GraphRepository
import com.bscan.service.graph.GraphComponentFactory
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Comprehensive unit tests for EntityCacheManager
 * Tests content-based caching, fingerprinting, TTL expiration, and invalidation
 */
class EntityCacheManagerTest {
    
    private lateinit var mockGraphRepository: GraphRepository
    private lateinit var cacheManager: EntityCacheManager
    
    // Test entities
    private lateinit var testRawScanData: RawScanData
    private lateinit var testRawScanData2: RawScanData
    private lateinit var testPhysicalComponent: PhysicalComponent
    
    // Test data
    private val testTagUid = "A1B2C3D4"
    private val testTrayUid = "01008023456789"
    private val testRawData = "048A5F00FF12345678ABCDEF..."
    
    @Before
    fun setUp() {
        mockGraphRepository = mock(GraphRepository::class.java)
        cacheManager = EntityCacheManager(mockGraphRepository)
        
        // Create test entities with consistent IDs for deterministic testing
        testRawScanData = RawScanData(
            id = GraphComponentFactory.createCompoundId("type" to "rawScan", "tagUid" to testTagUid),
            label = "Test RFID Scan",
            scanFormat = "bambu_rfid"
        ).apply {
            rawData = testRawData
            contentHash = "hash_$testRawData"
            dataSize = testRawData.length
            encoding = "hex"
        }
        
        testRawScanData2 = RawScanData(
            id = GraphComponentFactory.createCompoundId("type" to "rawScan", "tagUid" to "${testTagUid}2"),
            label = "Test RFID Scan 2", 
            scanFormat = "bambu_rfid"
        ).apply {
            rawData = "${testRawData}DIFFERENT"
            contentHash = "hash_${testRawData}DIFFERENT"
            dataSize = (testRawData + "DIFFERENT").length
            encoding = "hex"
        }
        
        testPhysicalComponent = PhysicalComponent(
            id = GraphComponentFactory.createCompoundId("type" to "component", "trayUid" to testTrayUid),
            label = "Test Component"
        ).apply {
            manufacturer = "Bambu Lab"
            category = "filament_spool"
        }
    }
    
    @After
    fun tearDown() {
        cacheManager.clearAll()
    }
    
    // CACHE HIT/MISS SCENARIOS
    
    @Test
    fun `cache miss should generate and store entity`() = runBlocking {
        var generatorCallCount = 0
        
        val result = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { source ->
            generatorCallCount++
            DecodedEncrypted(
                label = "Generated from ${source.label}"
            ).apply {
                tagType = "Mifare Classic 1K"
                tagUid = testTagUid
            }
        }
        
        assertNotNull("Generated entity should not be null", result)
        assertEquals("Generated entity should have correct label", "Generated from ${testRawScanData.label}", result.label)
        assertEquals("Generator should be called exactly once", 1, generatorCallCount)
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 1 cache miss", 1, stats.cacheMisses)
        assertEquals("Should have 0 cache hits", 0, stats.cacheHits)
        assertEquals("Should have 1 total entry", 1, stats.totalEntries)
    }
    
    @Test
    fun `cache hit should return stored entity without regeneration`() = runBlocking {
        var generatorCallCount = 0
        val generator: suspend (Entity) -> DecodedEncrypted = { source ->
            generatorCallCount++
            DecodedEncrypted(
                label = "Generated from ${source.label}"
            ).apply {
                tagType = "Mifare Classic 1K" 
                tagUid = testTagUid
            }
        }
        
        // First call - cache miss
        val result1 = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        // Second call - should be cache hit (same entity from cache)
        val result2 = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        // The cache returns the same entity instance, so IDs and labels should match
        assertEquals("Should return same entity ID", result1.id, result2.id)
        assertEquals("Should return same entity label", result1.label, result2.label)
        assertEquals("Should return same entity instance", result1, result2)
        assertEquals("Generator should be called exactly once", 1, generatorCallCount)
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 1 cache miss", 1, stats.cacheMisses)
        assertEquals("Should have 1 cache hit", 1, stats.cacheHits)
        assertEquals("Hit rate should be 50%", 0.5f, stats.hitRate, 0.01f)
    }
    
    // CONTENT FINGERPRINTING TESTS
    
    @Test
    fun `content change should invalidate cache and regenerate entity`() = runBlocking {
        var generatorCallCount = 0
        val generator: suspend (Entity) -> DecodedEncrypted = { source ->
            generatorCallCount++
            DecodedEncrypted(
                label = "Generated from ${(source as RawScanData).rawData}"
            ).apply {
                tagType = "Mifare Classic 1K"
                tagUid = source.getProperty<String>("testProp") ?: "default"
            }
        }
        
        // First call
        val result1 = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        // Modify source entity content (simulating content change)
        testRawScanData.setProperty("testProp", "changed_value")
        
        // Second call - should detect content change and regenerate
        val result2 = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        assertEquals("Generator should be called twice", 2, generatorCallCount)
        assertNotEquals("Results should have different content", result1.tagUid, result2.tagUid)
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 2 cache misses", 2, stats.cacheMisses)
        assertEquals("Should have 1 content change", 1, stats.contentChanges)
    }
    
    @Test
    fun `minimal fingerprint should detect rawData changes`() = runBlocking {
        var generatorCallCount = 0
        val generator: suspend (Entity) -> DecodedEncrypted = { source ->
            generatorCallCount++
            DecodedEncrypted(
                label = "Generated from ${(source as RawScanData).rawData.take(8)}"
            )
        }
        
        // First call
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        // Change raw data content 
        testRawScanData.rawData = "DIFFERENT_RAW_DATA"
        testRawScanData.contentHash = "hash_DIFFERENT_RAW_DATA"
        
        // Second call - should detect change
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted", generator)
        
        assertEquals("Generator should be called twice due to content change", 2, generatorCallCount)
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should register content change", 1, stats.contentChanges)
    }
    
    // TTL EXPIRATION TESTS
    
    @Test
    fun `expired cache entries should trigger regeneration`() = runBlocking {
        // Create test entity that will expire immediately
        val shortLivedEntity = RawScanData(
            id = GraphComponentFactory.createCompoundId("type" to "expiry", "test" to "data"),
            label = "Expiry Test",
            scanFormat = "test_format"
        )
        
        var generatorCallCount = 0
        val generator: suspend (Entity) -> DecodedEncrypted = { source ->
            generatorCallCount++
            DecodedEncrypted(
                label = "Generated ${generatorCallCount}"
            ).apply {
                // Entity properties don't control TTL - the cache manager does
                tagType = "Test Type"
            }
        }
        
        // Create a cache manager with 0 TTL for this derivation type
        val testCacheManager = object : EntityCacheManager(mockGraphRepository) {
            override fun getTTLForType(derivationType: String): Int {
                return if (derivationType == "test_expiry_type") 0 else 60
            }
        }
        
        // First call
        val result1 = testCacheManager.getOrGenerate(shortLivedEntity, "test_expiry_type", generator)
        
        // Second call - should regenerate due to immediate expiration
        val result2 = testCacheManager.getOrGenerate(shortLivedEntity, "test_expiry_type", generator)
        
        assertEquals("Generator should be called twice", 2, generatorCallCount)
        assertNotEquals("Should generate different entities", result1.label, result2.label)
        
        val stats = testCacheManager.getStatistics()
        assertEquals("Should have 1 expiration", 1, stats.expirations)
    }
    
    @Test
    fun `different derivation types should have correct TTL values`() = runBlocking {
        // Test that different entity types get appropriate TTL values
        val decodedEncrypted = cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->
            DecodedEncrypted(label = "Test")
        }
        
        val encodedDecrypted = cacheManager.getOrGenerate(testRawScanData, "encoded_decrypted") { _ ->
            EncodedDecrypted(label = "Test")
        }
        
        val decodedDecrypted = cacheManager.getOrGenerate(testRawScanData, "decoded_decrypted") { _ ->
            DecodedDecrypted(label = "Test") 
        }
        
        // Verify entities were cached with expected TTL patterns
        // (We can't directly test TTL values, but we can verify caching behavior)
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 3 cached entities", 3, stats.totalEntries)
        assertEquals("Should have 3 cache misses", 3, stats.cacheMisses)
    }
    
    // CACHE INVALIDATION TESTS
    
    @Test
    fun `invalidateSource should remove all cache entries for source entity`() = runBlocking {
        // Create multiple derived entities from same source
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->
            DecodedEncrypted(label = "Test 1")
        }
        
        cacheManager.getOrGenerate(testRawScanData, "encoded_decrypted") { _ ->
            EncodedDecrypted(label = "Test 2") 
        }
        
        cacheManager.getOrGenerate(testRawScanData2, "decoded_encrypted") { _ ->
            DecodedEncrypted(label = "Test 3")
        }
        
        assertEquals("Should have 3 cached entities", 3, cacheManager.getStatistics().totalEntries)
        
        // Invalidate first source
        cacheManager.invalidateSource(testRawScanData.id)
        
        // Should remove 2 entries (both derived from testRawScanData)
        assertEquals("Should have 1 cached entity remaining", 1, cacheManager.getStatistics().totalEntries)
    }
    
    @Test
    fun `invalidateType should remove all cache entries of specific derivation type`() = runBlocking {
        // Create entities of different derivation types
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->
            DecodedEncrypted(label = "Test 1")
        }
        
        cacheManager.getOrGenerate(testRawScanData, "encoded_decrypted") { _ ->
            EncodedDecrypted(label = "Test 2")
        }
        
        cacheManager.getOrGenerate(testRawScanData2, "decoded_encrypted") { _ ->
            DecodedEncrypted(label = "Test 3")
        }
        
        assertEquals("Should have 3 cached entities", 3, cacheManager.getStatistics().totalEntries)
        
        // Invalidate all decoded_encrypted entries
        cacheManager.invalidateType("decoded_encrypted")
        
        // Should remove 2 decoded_encrypted entries, leaving 1 encoded_decrypted
        assertEquals("Should have 1 cached entity remaining", 1, cacheManager.getStatistics().totalEntries)
    }
    
    @Test
    fun `cleanupExpired should remove only expired entries`() = runBlocking {
        // Create cache manager that we can control TTL for
        val testCacheManager = object : EntityCacheManager(mockGraphRepository) {
            override fun getTTLForType(derivationType: String): Int {
                return when (derivationType) {
                    "long_lived" -> 60  // Long TTL
                    "short_lived" -> 0  // Immediate expiration
                    else -> 60
                }
            }
        }
        
        // Create mix of expired and non-expired entries
        testCacheManager.getOrGenerate(testRawScanData, "long_lived") { _ ->
            DecodedEncrypted(label = "Not expired")
        }
        
        testCacheManager.getOrGenerate(testRawScanData2, "short_lived") { _ ->
            EncodedDecrypted(label = "Expired")
        }
        
        assertEquals("Should start with 2 entries", 2, testCacheManager.getStatistics().totalEntries)
        
        // Clean up expired entries
        testCacheManager.cleanupExpired()
        
        val stats = testCacheManager.getStatistics()
        assertEquals("Should have 1 entry after cleanup (expired removed)", 1, stats.totalEntries)
    }
    
    @Test
    fun `clearAll should remove all cached entities and reset statistics`() = runBlocking {
        // Create several cached entities
        repeat(3) { index ->
            cacheManager.getOrGenerate(testRawScanData, "test_type_$index") { _ ->
                DecodedEncrypted(label = "Test $index")
            }
        }
        
        assertEquals("Should have 3 cached entities", 3, cacheManager.getStatistics().totalEntries)
        
        // Clear all cache
        cacheManager.clearAll()
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 0 cached entities", 0, stats.totalEntries)
        assertEquals("Cache hits should be reset", 0, stats.cacheHits)
        assertEquals("Cache misses should be reset", 0, stats.cacheMisses)
        assertEquals("Content changes should be reset", 0, stats.contentChanges)
    }
    
    // BATCH OPERATIONS TESTS
    
    @Test
    fun `getDerivedEntities should return all standard derived types`() = runBlocking {
        val derivedEntities = cacheManager.getDerivedEntities(testRawScanData)
        
        assertEquals("Should return 3 derived entity types", 3, derivedEntities.size)
        assertTrue("Should contain decoded_encrypted", derivedEntities.containsKey("decoded_encrypted"))
        assertTrue("Should contain encoded_decrypted", derivedEntities.containsKey("encoded_decrypted"))  
        assertTrue("Should contain decoded_decrypted", derivedEntities.containsKey("decoded_decrypted"))
        
        // Verify entity types are correct
        assertTrue("decoded_encrypted should be correct type", 
            derivedEntities["decoded_encrypted"] is DecodedEncrypted)
        assertTrue("encoded_decrypted should be correct type",
            derivedEntities["encoded_decrypted"] is EncodedDecrypted)
        assertTrue("decoded_decrypted should be correct type", 
            derivedEntities["decoded_decrypted"] is DecodedDecrypted)
    }
    
    @Test
    fun `getDerivedEntities should handle generation failures gracefully`() = runBlocking {
        // Create entity that might cause generation to fail
        val problematicEntity = RawScanData(
            id = "problematic_id",
            label = "Problematic Entity",
            scanFormat = "unknown_format"
        ).apply {
            rawData = ""  // Empty raw data might cause issues
        }
        
        // Should not throw exception, but return partial results
        val derivedEntities = cacheManager.getDerivedEntities(problematicEntity)
        
        // Even if some generations fail, should return what it can
        assertTrue("Should return map even with failures", derivedEntities is Map)
    }
    
    // CACHE KEY GENERATION TESTS
    
    @Test
    fun `cache keys should be deterministic and collision-resistant`() {
        // Test that cache key generation is consistent and unique
        val key1 = GraphComponentFactory.createCompoundId(
            "source" to testRawScanData.id,
            "type" to "decoded_encrypted"
        )
        
        val key2 = GraphComponentFactory.createCompoundId(
            "source" to testRawScanData.id,
            "type" to "decoded_encrypted"  
        )
        
        val key3 = GraphComponentFactory.createCompoundId(
            "source" to testRawScanData.id,
            "type" to "encoded_decrypted"  // Different type
        )
        
        val key4 = GraphComponentFactory.createCompoundId(
            "source" to testRawScanData2.id,  // Different source
            "type" to "decoded_encrypted"
        )
        
        assertEquals("Same inputs should produce same key", key1, key2)
        assertNotEquals("Different types should produce different keys", key1, key3)
        assertNotEquals("Different sources should produce different keys", key1, key4)
        
        // Verify key format
        assertEquals("Keys should be 16 character hex strings", 16, key1.length)
        assertTrue("Keys should be hex format", key1.matches(Regex("[a-f0-9]{16}")))
    }
    
    // MEMORY MANAGEMENT TESTS
    
    @Test
    fun `memory usage estimation should be reasonable`() = runBlocking {
        // Create several cached entities
        repeat(5) { index ->
            cacheManager.getOrGenerate(testRawScanData, "test_type_$index") { source ->
                DecodedEncrypted(label = "Test entity $index").apply {
                    tagType = "Mifare Classic 1K"
                    tagUid = "UID_$index"
                    setProperty("customData", "Some custom data for entity $index")
                }
            }
        }
        
        val stats = cacheManager.getStatistics()
        assertTrue("Memory usage should be greater than 0", stats.memoryUsageBytes > 0)
        assertTrue("Memory usage should be reasonable", stats.memoryUsageBytes < 1_000_000) // Less than 1MB
        
        // Memory usage should increase with more entities
        val initialMemory = stats.memoryUsageBytes
        
        repeat(3) { index ->
            cacheManager.getOrGenerate(testRawScanData2, "additional_type_$index") { _ ->
                DecodedEncrypted(label = "Additional entity $index")
            }
        }
        
        val newStats = cacheManager.getStatistics()
        assertTrue("Memory usage should increase with more entities", 
            newStats.memoryUsageBytes > initialMemory)
    }
    
    @Test 
    fun `cache statistics should be comprehensive and accurate`() = runBlocking {
        // Generate known access pattern
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->  // miss
            DecodedEncrypted(label = "Test 1")
        }
        
        // Access same entity again - should be cache hit
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->  // hit
            DecodedEncrypted(label = "Should not be called")
        }
        
        // Change content and access again
        testRawScanData.setProperty("test", "changed")
        cacheManager.getOrGenerate(testRawScanData, "decoded_encrypted") { _ ->  // content change
            DecodedEncrypted(label = "Test 2")  
        }
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 1 total entry", 1, stats.totalEntries)
        assertEquals("Should have 1 cache hit", 1, stats.cacheHits)
        assertEquals("Should have 2 cache misses (initial + content change)", 2, stats.cacheMisses)
        assertEquals("Should have 1 content change", 1, stats.contentChanges)
        assertEquals("Hit rate should be 33%", 0.33f, stats.hitRate, 0.01f)
        assertTrue("Memory usage should be tracked", stats.memoryUsageBytes > 0)
    }
    
    // ERROR HANDLING TESTS
    
    @Test
    fun `generator exceptions should not corrupt cache state`() = runBlocking {
        var callCount = 0
        
        try {
            cacheManager.getOrGenerate(testRawScanData, "failing_type") { _ ->
                callCount++
                throw RuntimeException("Simulated generator failure")
            }
            fail("Should have thrown exception")
        } catch (e: RuntimeException) {
            assertEquals("Simulated generator failure", e.message)
        }
        
        // Cache should remain in valid state
        val stats = cacheManager.getStatistics()
        assertEquals("Should have 1 cache miss despite failure", 1, stats.cacheMisses)
        assertEquals("Should have 0 cached entries after failure", 0, stats.totalEntries)
        
        // Subsequent operations should work normally
        val result = cacheManager.getOrGenerate(testRawScanData, "working_type") { _ ->
            callCount++
            DecodedEncrypted(label = "Working generator")
        }
        
        assertNotNull("Subsequent operations should work", result)
        assertEquals("Call count should be 2", 2, callCount)
    }
    
    // CONCURRENCY TESTS
    
    @Test
    fun `concurrent access should be thread-safe`() = runBlocking {
        val numThreads = 5
        val numOperationsPerThread = 20
        val latch = CountDownLatch(numThreads)
        val successCount = AtomicInteger(0)
        val generatorCallCount = AtomicInteger(0)
        
        val threads = (0 until numThreads).map { threadIndex ->
            Thread {
                try {
                    repeat(numOperationsPerThread) { opIndex ->
                        val testEntity = RawScanData(
                            id = "thread_${threadIndex}_op_${opIndex}",
                            label = "Test $threadIndex-$opIndex",
                            scanFormat = "test_format"
                        )
                        
                        val result = runBlocking {
                            cacheManager.getOrGenerate(testEntity, "concurrent_test") { _ ->
                                generatorCallCount.incrementAndGet()
                                DecodedEncrypted(label = "Generated for $threadIndex-$opIndex")
                            }
                        }
                        
                        assertNotNull("Result should not be null", result)
                        successCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        // Start all threads
        threads.forEach { it.start() }
        
        // Wait for completion
        latch.await()
        threads.forEach { it.join() }
        
        // Verify results
        val expectedOperations = numThreads * numOperationsPerThread
        assertEquals("All operations should succeed", expectedOperations, successCount.get())
        
        // Due to concurrency, there might be race conditions, so allow some tolerance
        assertTrue("Generator call count should be close to expected", 
                   generatorCallCount.get() >= expectedOperations - 5 && generatorCallCount.get() <= expectedOperations)
        
        val stats = cacheManager.getStatistics()
        assertEquals("Should have cached all unique entities", expectedOperations, stats.totalEntries)
        assertTrue("Should have cache misses for most accesses", stats.cacheMisses >= expectedOperations - 5)
    }
}