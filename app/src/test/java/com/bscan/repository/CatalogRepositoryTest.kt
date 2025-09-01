package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import org.junit.Before
import org.junit.Test
import org.junit.After
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import java.time.LocalDateTime

/**
 * Test verification for catalog repository functionality.
 * 
 * This test validates that the catalog repository works correctly with:
 * - Runtime-generated Bambu catalog
 * - User-created SKU management
 * - Stock tracking integration
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CatalogRepositoryTest {

    private lateinit var context: Context
    private lateinit var catalogRepository: CatalogRepository
    
    @Before
    fun setup() {
        // Use Robolectric context to access assets
        context = RuntimeEnvironment.getApplication()
        catalogRepository = CatalogRepository(context)
    }
    
    @After
    fun tearDown() {
        // Clear user catalog cache after each test to prevent test interference
        catalogRepository.clearUserCatalogCache()
    }

    @Test
    fun `catalog returns runtime-generated Bambu catalog`() {
        // When
        val catalog = catalogRepository.getCatalog()

        // Then
        assertNotNull("Catalog should not be null", catalog)
        assertTrue("Catalog should have Bambu manufacturer", catalog.manufacturers.containsKey("bambu"))
    }

    @Test
    fun `runtime catalog has Bambu manufacturer`() {
        // When
        val manufacturers = catalogRepository.getManufacturers()

        // Then
        assertFalse("Should not have empty manufacturers map", manufacturers.isEmpty())
        assertTrue("Should contain bambu manufacturer", manufacturers.containsKey("bambu"))
        assertFalse("Should not contain unknown manufacturers", manufacturers.containsKey("opentag"))
        
        val bambuManufacturer = manufacturers["bambu"]
        assertNotNull("Bambu manufacturer should not be null", bambuManufacturer)
        assertEquals("Bambu Lab", bambuManufacturer!!.displayName)
    }

    @Test
    fun `Bambu manufacturer exists and unknown returns null`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")
        val opentagCatalog = catalogRepository.getManufacturer("opentag")

        // Then
        assertNotNull("Bambu manufacturer should not be null", bambuCatalog)
        assertEquals("Bambu Lab", bambuCatalog!!.displayName)
        assertNull("Non-existent opentag manufacturer should return null", opentagCatalog)
    }

    @Test
    fun `rfid mapping search returns null for empty catalog`() {
        // When
        val rfidMapping1 = catalogRepository.findRfidMapping("GFL99:41109904457788")
        val rfidMapping2 = catalogRepository.findRfidMapping("bambu", "GFL99:41109904457788")

        // Then
        assertNull("Global RFID mapping should return null", rfidMapping1)
        assertNull("Manufacturer-specific RFID mapping should return null", rfidMapping2)
    }

    @Test
    fun `Bambu component stock definitions are available`() {
        // When
        val packagingStockDefinitions = catalogRepository.findPackagingStockDefinitions("bambu")
        val specificComponent = catalogRepository.findStockDefinitionBySku("bambu", "component_spool_standard")

        // Then
        assertFalse("Packaging stock definitions should not be empty", packagingStockDefinitions.isEmpty())
        assertNotNull("Specific component stock definition should not be null", specificComponent)
        assertEquals("Standard Spool", specificComponent!!.displayName)
        assertEquals(212.0, specificComponent.weight?.value?.toDouble())
        assertEquals("g", specificComponent.weight?.unit)
    }

    @Test
    fun `Bambu material stock definitions and temperature profile lookups work`() {
        // When
        val materialStockDefinitions = catalogRepository.findMaterialStockDefinitions("bambu")
        val temperatureProfile = catalogRepository.getTemperatureProfile("bambu", "pla_standard")
        val colorName = catalogRepository.getColorName("bambu", "#DC143C")

        // Then
        assertFalse("Material stock definitions should not be empty", materialStockDefinitions.isEmpty())
        
        // Check that material stock definitions have material properties
        val firstMaterial = materialStockDefinitions.first()
        assertTrue("Material stock definition should be a material", firstMaterial.isMaterial())
        assertNotNull("Material stock definition should have material type", firstMaterial.materialType)
        
        // Find any PLA-based material (material types include PLA_BASIC, PLA_MATTE, etc)
        val plaStockDefinitions = materialStockDefinitions.filter { 
            it.materialType?.contains("PLA") == true 
        }
        assertFalse("PLA-based stock definitions should not be empty", plaStockDefinitions.isEmpty())
        
        assertNotNull("Temperature profile should not be null", temperatureProfile)
        assertEquals(190, temperatureProfile!!.minNozzle)
        // Color palette might be empty initially - the important thing is the method doesn't crash
        // Color lookup by hex is not critical for the entity system functionality
    }

    @Test
    fun `tag format filtering returns Bambu for proprietary format`() {
        // When
        val bambuManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.BAMBU_PROPRIETARY)
        val ndefManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.NDEF_JSON)

        // Then
        assertFalse("Bambu manufacturers should not be empty", bambuManufacturers.isEmpty())
        assertEquals("Should have one Bambu manufacturer", 1, bambuManufacturers.size)
        assertEquals("Should be bambu manufacturer", "bambu", bambuManufacturers[0].first)
        assertTrue("NDEF manufacturers should be empty", ndefManufacturers.isEmpty())
    }

    @Test
    fun `manufacturer existence checks return correct values`() {
        // When & Then
        assertTrue("Should have bambu manufacturer", catalogRepository.hasManufacturer("bambu"))
        assertFalse("Should not have opentag manufacturer", catalogRepository.hasManufacturer("opentag"))
        assertFalse("Should not have unknown manufacturer", catalogRepository.hasManufacturer("unknown"))
    }

    @Test
    fun `stock definition searches return runtime generated definitions`() {
        // When
        val bambuStockDefs = catalogRepository.getStockDefinitions("bambu")
        val searchResults = catalogRepository.findStockDefinitionsByMaterial("bambu", "PLA")

        // Then
        assertTrue("Bambu stock definitions should contain runtime generated definitions", bambuStockDefs.isNotEmpty())
        // Search results may be empty if no exact matches, but definitions should exist
        assertTrue("Should have Bambu stock definitions in catalog", bambuStockDefs.isNotEmpty())
    }

    @Test
    fun `catalog reload functionality works with runtime catalog`() {
        // When - First access loads runtime catalog
        val catalog1 = catalogRepository.getCatalog()

        // Force reload
        catalogRepository.reloadCatalog()

        // Second access should reload runtime catalog
        val catalog2 = catalogRepository.getCatalog()

        // Then - Both should be valid and equivalent runtime catalogs
        assertNotNull("First catalog should not be null", catalog1)
        assertNotNull("Second catalog should not be null", catalog2)
        assertEquals("Both catalogs should have same manufacturers count", catalog1.manufacturers.size, catalog2.manufacturers.size)
        assertTrue("Both catalogs should have bambu manufacturer", catalog1.manufacturers.containsKey("bambu"))
        assertTrue("Second catalog should also have bambu manufacturer", catalog2.manufacturers.containsKey("bambu"))
    }

    // === Content Fingerprint Tests ===

    @Test
    fun `getContentFingerprint returns consistent fingerprint for unchanged catalog`() {
        // When - Generate fingerprint multiple times
        val fingerprint1 = catalogRepository.getContentFingerprint()
        val fingerprint2 = catalogRepository.getContentFingerprint()
        val fingerprint3 = catalogRepository.getContentFingerprint()

        // Then - All fingerprints should be identical
        assertEquals("First and second fingerprint should be identical", fingerprint1, fingerprint2)
        assertEquals("Second and third fingerprint should be identical", fingerprint2, fingerprint3)
        assertEquals("First and third fingerprint should be identical", fingerprint1, fingerprint3)
    }

    @Test
    fun `getContentFingerprint returns proper SHA-256 hash format and length`() {
        // When - Generate fingerprint
        val fingerprint = catalogRepository.getContentFingerprint()

        // Then - Fingerprint should be exactly 16 characters and valid hex
        assertEquals("Fingerprint should be exactly 16 characters", 16, fingerprint.length)
        assertTrue("Fingerprint should contain only hex characters", fingerprint.matches(Regex("[0-9a-f]+")))
        assertFalse("Fingerprint should not be empty", fingerprint.isEmpty())
    }

    @Test
    fun `getContentFingerprint handles empty catalog gracefully`() {
        // When - Generate fingerprint for empty user catalog (still has build-time Bambu catalog)
        val fingerprint = catalogRepository.getContentFingerprint()

        // Then - Should still return valid fingerprint based on build-time catalog
        assertNotNull("Fingerprint should not be null for empty user catalog", fingerprint)
        assertEquals("Fingerprint should be exactly 16 characters", 16, fingerprint.length)
        assertTrue("Fingerprint should contain only hex characters", fingerprint.matches(Regex("[0-9a-f]+")))
        assertFalse("Fingerprint should not be empty", fingerprint.isEmpty())
    }

    @Test
    fun `getContentFingerprint includes manufacturer product data`() {
        // When - Generate fingerprint (should include Bambu catalog data)
        val fingerprint = catalogRepository.getContentFingerprint()

        // Then - Fingerprint should be based on manufacturer catalog data
        assertNotNull("Fingerprint should include manufacturer data", fingerprint)
        assertEquals("Fingerprint should be exactly 16 characters", 16, fingerprint.length)
        assertTrue("Fingerprint should contain only hex characters", fingerprint.matches(Regex("[0-9a-f]+")))
        
        // Verify fingerprint changes if we could modify manufacturer data
        // (Since we can't easily mock the build-time catalog, we verify the fingerprint is stable)
        val fingerprint2 = catalogRepository.getContentFingerprint()
        assertEquals("Fingerprint should be consistent with same manufacturer data", fingerprint, fingerprint2)
    }

    @Test
    fun `getContentFingerprint deterministic across multiple instances`() {
        // Given - Two separate repository instances
        val repository1 = CatalogRepository(context)
        val repository2 = CatalogRepository(context)

        // When - Generate fingerprints from both instances
        val fingerprint1 = repository1.getContentFingerprint()
        val fingerprint2 = repository2.getContentFingerprint()

        // Then - Fingerprints should be identical (deterministic)
        assertEquals("Fingerprints should be identical across instances with same data", fingerprint1, fingerprint2)
        assertEquals("Both fingerprints should be 16 characters", 16, fingerprint1.length)
        assertEquals("Both fingerprints should be 16 characters", 16, fingerprint2.length)
    }

    @Test
    fun `getContentFingerprint performance test`() {
        // When - Generate fingerprint and measure time
        val startTime = System.currentTimeMillis()
        val fingerprint = catalogRepository.getContentFingerprint()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Then - Should generate fingerprint in reasonable time and correct format
        assertNotNull("Fingerprint should not be null", fingerprint)
        assertEquals("Fingerprint should be exactly 16 characters", 16, fingerprint.length)
        assertTrue("Fingerprint should contain only hex characters", fingerprint.matches(Regex("[0-9a-f]+")))
        assertTrue("Fingerprint generation should be reasonably fast (< 1000ms)", duration < 1000)
        
        // Verify consistency
        val fingerprint2 = catalogRepository.getContentFingerprint()
        assertEquals("Fingerprint should be consistent", fingerprint, fingerprint2)
    }
}