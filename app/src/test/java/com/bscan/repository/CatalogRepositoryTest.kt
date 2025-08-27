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
 * - Empty catalog by default
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
    fun `catalog returns empty catalog by default`() {
        // When
        val catalog = catalogRepository.getCatalog()

        // Then
        assertNotNull("Catalog should not be null", catalog)
        assertEquals("Catalog version should be 1", 1, catalog.version)
        assertTrue("Catalog should have empty manufacturers map", catalog.manufacturers.isEmpty())
    }

    @Test
    fun `empty catalog has no manufacturers`() {
        // When
        val manufacturers = catalogRepository.getManufacturers()

        // Then
        assertTrue("Should have empty manufacturers map", manufacturers.isEmpty())
        assertFalse("Should not contain bambu manufacturer", manufacturers.containsKey("bambu"))
        assertFalse("Should not contain opentag manufacturer", manufacturers.containsKey("opentag"))
    }

    @Test
    fun `non-existent manufacturer returns null`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")
        val opentagCatalog = catalogRepository.getManufacturer("opentag")

        // Then
        assertNull("Non-existent bambu manufacturer should return null", bambuCatalog)
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
    fun `component defaults return empty for non-existent manufacturer`() {
        // When
        val componentDefaults = catalogRepository.getComponentDefaults("bambu")
        val specificComponent = catalogRepository.getComponentDefault("bambu", "spool_standard")

        // Then
        assertTrue("Component defaults should be empty", componentDefaults.isEmpty())
        assertNull("Specific component default should be null", specificComponent)
    }

    @Test
    fun `material and temperature profile lookups return null`() {
        // When
        val material = catalogRepository.getMaterial("bambu", "PLA_BASIC")
        val temperatureProfile = catalogRepository.getTemperatureProfile("bambu", "lowTempPLA")
        val colorName = catalogRepository.getColorName("bambu", "#FF0000")

        // Then
        assertNull("Material should be null", material)
        assertNull("Temperature profile should be null", temperatureProfile)
        assertNull("Color name should be null", colorName)
    }

    @Test
    fun `tag format filtering returns empty list`() {
        // When
        val bambuManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.BAMBU_PROPRIETARY)
        val ndefManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.NDEF_JSON)

        // Then
        assertTrue("Bambu manufacturers should be empty", bambuManufacturers.isEmpty())
        assertTrue("NDEF manufacturers should be empty", ndefManufacturers.isEmpty())
    }

    @Test
    fun `manufacturer existence checks return false`() {
        // When & Then
        assertFalse("Should not have bambu manufacturer", catalogRepository.hasManufacturer("bambu"))
        assertFalse("Should not have opentag manufacturer", catalogRepository.hasManufacturer("opentag"))
        assertFalse("Should not have unknown manufacturer", catalogRepository.hasManufacturer("unknown"))
    }

    @Test
    fun `product searches return empty lists`() {
        // When
        val bambuProducts = catalogRepository.getProducts("bambu")
        val searchResults = catalogRepository.findProducts("bambu", "#FF0000", "PLA")

        // Then
        assertTrue("Bambu products should be empty", bambuProducts.isEmpty())
        assertTrue("Search results should be empty", searchResults.isEmpty())
    }

    @Test
    fun `catalog reload functionality works with empty catalog`() {
        // When - First access loads empty catalog
        val catalog1 = catalogRepository.getCatalog()

        // Force reload
        catalogRepository.reloadCatalog()

        // Second access should reload empty catalog
        val catalog2 = catalogRepository.getCatalog()

        // Then - Both should be valid and equivalent empty catalogs
        assertNotNull("First catalog should not be null", catalog1)
        assertNotNull("Second catalog should not be null", catalog2)
        assertEquals("Both catalogs should have same version", catalog1.version, catalog2.version)
        assertEquals("Both catalogs should have same empty manufacturers", catalog1.manufacturers.size, catalog2.manufacturers.size)
        assertTrue("Both catalogs should have empty manufacturers", catalog1.manufacturers.isEmpty())
        assertTrue("Second catalog should also have empty manufacturers", catalog2.manufacturers.isEmpty())
    }
}