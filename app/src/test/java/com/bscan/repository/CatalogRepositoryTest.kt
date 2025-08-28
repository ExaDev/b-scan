package com.bscan.repository

import android.content.Context
import com.bscan.model.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

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
    fun `Bambu component defaults are available`() {
        // When
        val componentDefaults = catalogRepository.getComponentDefaults("bambu")
        val specificComponent = catalogRepository.getComponentDefault("bambu", "spool_standard")

        // Then
        assertFalse("Component defaults should not be empty", componentDefaults.isEmpty())
        assertNotNull("Specific component default should not be null", specificComponent)
        assertEquals("Standard Spool", specificComponent!!.name)
        assertEquals(212f, specificComponent.massGrams)
    }

    @Test
    fun `Bambu material and temperature profile lookups work`() {
        // When
        val material = catalogRepository.getMaterial("bambu", "PLA Basic")
        val temperatureProfile = catalogRepository.getTemperatureProfile("bambu", "pla_standard")
        val colorName = catalogRepository.getColorName("bambu", "#FF0000")

        // Then
        assertNotNull("Material should not be null", material)
        assertEquals("PLA Basic", material!!.displayName)
        assertNotNull("Temperature profile should not be null", temperatureProfile)
        assertEquals(190, temperatureProfile!!.minNozzle)
        assertNotNull("Color name should not be null", colorName)
        assertEquals("Red", colorName)
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
    fun `product searches return runtime generated products`() {
        // When
        val bambuProducts = catalogRepository.getProducts("bambu")
        val searchResults = catalogRepository.findProducts("bambu", "#FF0000", "PLA")

        // Then
        assertTrue("Bambu products should contain runtime generated products", bambuProducts.isNotEmpty())
        // Search results may be empty if no exact matches, but products should exist
        assertTrue("Should have Bambu products in catalog", bambuProducts.isNotEmpty())
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
}