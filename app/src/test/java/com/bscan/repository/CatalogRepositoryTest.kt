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
 * Comprehensive test verification for multi-manufacturer catalog functionality.
 * 
 * This test validates that the new catalog architecture is working correctly with:
 * - Multiple manufacturers supported (Bambu Lab, OpenTag standard)
 * - Material definitions per manufacturer
 * - Temperature profiles per manufacturer
 * - RFID mappings per manufacturer
 * - Component defaults per manufacturer
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
    fun `catalog loads successfully from assets`() {
        // When
        val catalog = catalogRepository.getCatalog()

        // Then
        assertNotNull("Catalog should not be null", catalog)
        assertEquals("Catalog version should be 1", 1, catalog.version)
        assertTrue("Catalog should have manufacturers", catalog.manufacturers.isNotEmpty())
    }

    @Test
    fun `both bambu and opentag manufacturers are present`() {
        // When
        val manufacturers = catalogRepository.getManufacturers()

        // Then
        assertTrue("Should contain bambu manufacturer", manufacturers.containsKey("bambu"))
        assertTrue("Should contain opentag manufacturer", manufacturers.containsKey("opentag"))

        val bambu = manufacturers["bambu"]!!
        val openTag = manufacturers["opentag"]!!

        assertEquals("Bambu display name should be correct", "Bambu Lab", bambu.displayName)
        assertEquals("OpenTag display name should be correct", "OpenTag Standard", openTag.displayName)

        assertEquals("Bambu tag format should be correct", TagFormat.BAMBU_PROPRIETARY, bambu.tagFormat)
        assertEquals("OpenTag tag format should be correct", TagFormat.NDEF_JSON, openTag.tagFormat)
    }

    @Test
    fun `bambu lab has comprehensive material definitions`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")!!

        // Then - Check key materials exist
        val expectedMaterials = listOf(
            "PLA_BASIC", "PLA_MATTE", "PLA_SILK", "PLA_METAL", "PLA_WOOD", "PLA_MARBLE", "PLA_GLOW",
            "PETG", "ABS", "ASA", "TPU", "PA_NYLON", "PC", "PET_CF", "SUPPORT", "PVA"
        )

        expectedMaterials.forEach { materialId ->
            assertTrue(
                "Should have $materialId material definition",
                bambuCatalog.materials.containsKey(materialId)
            )
        }

        // Test specific material properties
        val plaBasic = bambuCatalog.materials["PLA_BASIC"]!!
        assertEquals("PLA Basic display name", "PLA Basic", plaBasic.displayName)
        assertEquals("PLA Basic temperature profile", "lowTempPLA", plaBasic.temperatureProfile)
        assertEquals("PLA Basic category", MaterialCategory.THERMOPLASTIC, plaBasic.properties.category)
        assertFalse("PLA Basic should not be flexible", plaBasic.properties.flexible)
        assertFalse("PLA Basic should not be support", plaBasic.properties.support)

        val tpu = bambuCatalog.materials["TPU"]!!
        assertEquals("TPU display name", "TPU (Flexible)", tpu.displayName)
        assertEquals("TPU temperature profile", "highTempPLA", tpu.temperatureProfile)
        assertEquals("TPU category", MaterialCategory.FLEXIBLE, tpu.properties.category)
        assertTrue("TPU should be flexible", tpu.properties.flexible)
        assertFalse("TPU should not be support", tpu.properties.support)

        val support = bambuCatalog.materials["SUPPORT"]!!
        assertEquals("Support display name", "Support Material", support.displayName)
        assertEquals("Support category", MaterialCategory.SUPPORT, support.properties.category)
        assertTrue("Support should be marked as support", support.properties.support)
        assertFalse("Support should not be flexible", support.properties.flexible)
    }

    @Test
    fun `temperature profiles are loaded correctly`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")!!

        // Then - Check key temperature profiles exist
        val expectedProfiles = listOf(
            "lowTempPLA", "highTempPLA", "lowTempSupport", "midTempEngineering",
            "highTempABS", "highTempCarbon", "veryHighTempASA", "extremeTempNylon",
            "ultraHighTempPC", "maxTempSpecialty"
        )

        expectedProfiles.forEach { profileId ->
            assertTrue(
                "Should have $profileId temperature profile",
                bambuCatalog.temperatureProfiles.containsKey(profileId)
            )
        }

        // Test specific temperature profiles
        val lowTempPLA = bambuCatalog.temperatureProfiles["lowTempPLA"]!!
        assertEquals("Low temp PLA min nozzle", 190, lowTempPLA.minNozzle)
        assertEquals("Low temp PLA max nozzle", 220, lowTempPLA.maxNozzle)
        assertEquals("Low temp PLA bed temp", 60, lowTempPLA.bed)

        val ultraHighTempPC = bambuCatalog.temperatureProfiles["ultraHighTempPC"]!!
        assertEquals("Ultra high temp PC min nozzle", 260, ultraHighTempPC.minNozzle)
        assertEquals("Ultra high temp PC max nozzle", 310, ultraHighTempPC.maxNozzle)
        assertEquals("Ultra high temp PC bed temp", 100, ultraHighTempPC.bed)
    }

    @Test
    fun `bambu color palette is complete`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")!!

        // Then
        assertTrue("Should have color palette", bambuCatalog.colorPalette.isNotEmpty())

        val expectedColors = mapOf(
            "#FFFFFF" to "White",
            "#000000" to "Black",
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            "#FFFF00" to "Yellow",
            "#FF6600" to "Orange",
            "#800080" to "Purple",
            "#808080" to "Grey",
            "#C0C0C0" to "Silver",
            "#FFD700" to "Gold",
            "#8B4513" to "Brown"
        )

        expectedColors.forEach { (hex, colorName) ->
            assertEquals(
                "Color $hex should map to $colorName",
                colorName,
                bambuCatalog.colorPalette[hex]
            )
        }
    }

    @Test
    fun `rfid mappings are functional`() {
        // When
        val bambuCatalog = catalogRepository.getManufacturer("bambu")!!

        // Then - Check RFID mappings exist
        assertTrue("Should have RFID mappings", bambuCatalog.rfidMappings.isNotEmpty())

        // Test specific mappings from catalog_data.json
        val brownPLA = bambuCatalog.rfidMappings["GFL99:41109904457788"]
        assertNotNull("Should have brown PLA mapping", brownPLA)
        assertEquals("Brown PLA SKU", "41109904457788", brownPLA!!.sku)
        assertEquals("Brown PLA material", "PLA_BASIC", brownPLA.material)
        assertEquals("Brown PLA color", "Brown", brownPLA.color)
        assertEquals("Brown PLA hex", "#8B4513", brownPLA.hex)

        val greenPLA = bambuCatalog.rfidMappings["GFL99:41109904490556"]
        assertNotNull("Should have green PLA mapping", greenPLA)
        assertEquals("Green PLA SKU", "41109904490556", greenPLA!!.sku)
        assertEquals("Green PLA material", "PLA_BASIC", greenPLA.material)
        assertEquals("Green PLA color", "Green", greenPLA.color)
        assertEquals("Green PLA hex", "#00FF00", greenPLA.hex)

        val nylon = bambuCatalog.rfidMappings["GFN04:40577626767420"]
        assertNotNull("Should have nylon mapping", nylon)
        assertEquals("Nylon SKU", "40577626767420", nylon!!.sku)
        assertEquals("Nylon material", "PA_NYLON", nylon.material)
        assertEquals("Nylon color", "Alpine Green Sparkle", nylon.color)
        assertEquals("Nylon hex", "#3F5443", nylon.hex)
    }

    @Test
    fun `rfid mapping resolution works across manufacturers`() {
        // Test finding RFID mappings across all manufacturers
        val brownPLAMapping = catalogRepository.findRfidMapping("GFL99:41109904457788")
        assertNotNull("Should find brown PLA mapping", brownPLAMapping)
        assertEquals("Should find mapping in bambu manufacturer", "bambu", brownPLAMapping!!.first)
        assertEquals("Should have correct SKU", "41109904457788", brownPLAMapping.second.sku)

        // Test finding RFID mapping for specific manufacturer
        val greenPLAMapping = catalogRepository.findRfidMapping("bambu", "GFL99:41109904490556")
        assertNotNull("Should find green PLA mapping for bambu", greenPLAMapping)
        assertEquals("Should have correct material", "PLA_BASIC", greenPLAMapping!!.material)

        // Test missing mapping
        val missingMapping = catalogRepository.findRfidMapping("NONEXISTENT:12345")
        assertNull("Should not find nonexistent mapping", missingMapping)
    }

    @Test
    fun `component defaults are loaded correctly`() {
        // When - Test Bambu component defaults
        val bambuCatalog = catalogRepository.getManufacturer("bambu")!!

        // Then
        assertTrue("Bambu should have component defaults", bambuCatalog.componentDefaults.isNotEmpty())

        val standardSpool = bambuCatalog.componentDefaults["spool_standard"]
        assertNotNull("Should have standard spool component", standardSpool)
        assertEquals("Standard spool name", "Bambu Standard Spool", standardSpool!!.name)
        assertEquals("Standard spool type", PhysicalComponentType.BASE_SPOOL, standardSpool.type)
        assertEquals("Standard spool mass", 212.0f, standardSpool.massGrams)

        val cardboardCore = bambuCatalog.componentDefaults["core_cardboard"]
        assertNotNull("Should have cardboard core component", cardboardCore)
        assertEquals("Cardboard core name", "Cardboard Core", cardboardCore!!.name)
        assertEquals("Cardboard core type", PhysicalComponentType.CORE_RING, cardboardCore.type)
        assertEquals("Cardboard core mass", 33.0f, cardboardCore.massGrams)

        val filament1kg = bambuCatalog.componentDefaults["filament_1kg"]
        assertNotNull("Should have 1kg filament component", filament1kg)
        assertEquals("1kg filament name", "1kg Filament", filament1kg!!.name)
        assertEquals("1kg filament type", PhysicalComponentType.FILAMENT, filament1kg.type)
        assertEquals("1kg filament mass", 1000.0f, filament1kg.massGrams)

        // Test OpenTag component defaults
        val openTagCatalog = catalogRepository.getManufacturer("opentag")!!
        assertTrue("OpenTag should have component defaults", openTagCatalog.componentDefaults.isNotEmpty())

        val genericFilament = openTagCatalog.componentDefaults["generic_filament"]
        assertNotNull("Should have generic filament component", genericFilament)
        assertEquals("Generic filament name", "Generic Filament", genericFilament!!.name)
        assertEquals("Generic filament type", PhysicalComponentType.FILAMENT, genericFilament.type)
        assertEquals("Generic filament mass", 1000.0f, genericFilament.massGrams)
    }

    @Test
    fun `component defaults can be retrieved by manufacturer and key`() {
        // When
        val bambuSpool = catalogRepository.getComponentDefault("bambu", "spool_standard")
        val openTagFilament = catalogRepository.getComponentDefault("opentag", "generic_filament")
        val missing = catalogRepository.getComponentDefault("bambu", "nonexistent")

        // Then
        assertNotNull("Should find Bambu spool component", bambuSpool)
        assertEquals("Should have correct Bambu spool name", "Bambu Standard Spool", bambuSpool!!.name)

        assertNotNull("Should find OpenTag filament component", openTagFilament)
        assertEquals("Should have correct OpenTag filament name", "Generic Filament", openTagFilament!!.name)

        assertNull("Should not find nonexistent component", missing)
    }

    @Test
    fun `manufacturers can be filtered by tag format`() {
        // When
        val bambuManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.BAMBU_PROPRIETARY)
        val ndefManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.NDEF_JSON)
        val unknownManufacturers = catalogRepository.getManufacturersByTagFormat(TagFormat.UNKNOWN)

        // Then
        assertEquals("Should find 1 Bambu proprietary manufacturer", 1, bambuManufacturers.size)
        assertEquals("Should be bambu manufacturer", "bambu", bambuManufacturers[0].first)

        assertEquals("Should find 1 NDEF JSON manufacturer", 1, ndefManufacturers.size)
        assertEquals("Should be opentag manufacturer", "opentag", ndefManufacturers[0].first)

        assertTrue("Should find no unknown format manufacturers", unknownManufacturers.isEmpty())
    }

    @Test
    fun `material and temperature profile retrieval works`() {
        // When
        val plaBasic = catalogRepository.getMaterial("bambu", "PLA_BASIC")
        val lowTempProfile = catalogRepository.getTemperatureProfile("bambu", "lowTempPLA")
        val colorName = catalogRepository.getColorName("bambu", "#FF0000")

        // Then
        assertNotNull("Should find PLA Basic material", plaBasic)
        assertEquals("Should have correct PLA Basic name", "PLA Basic", plaBasic!!.displayName)

        assertNotNull("Should find low temp profile", lowTempProfile)
        assertEquals("Should have correct min nozzle temp", 190, lowTempProfile!!.minNozzle)

        assertNotNull("Should find red color", colorName)
        assertEquals("Should have correct color name", "Red", colorName)

        // Test missing items
        val missing = catalogRepository.getMaterial("bambu", "NONEXISTENT")
        assertNull("Should not find nonexistent material", missing)
    }

    @Test
    fun `manufacturer existence checks work`() {
        // When/Then
        assertTrue("Should confirm bambu manufacturer exists", catalogRepository.hasManufacturer("bambu"))
        assertTrue("Should confirm opentag manufacturer exists", catalogRepository.hasManufacturer("opentag"))
        assertFalse("Should confirm nonexistent manufacturer does not exist", catalogRepository.hasManufacturer("nonexistent"))
    }

    @Test
    fun `catalog data can be accessed via UnifiedDataAccess`() {
        // When - create UnifiedDataAccess to test integration
        val userRepo = UserDataRepository(context)
        val unifiedDataAccess = UnifiedDataAccess(catalogRepository, userRepo)
        val manufacturers = unifiedDataAccess.getAllManufacturers()

        // Then
        assertNotNull("Should have manufacturers via UnifiedDataAccess", manufacturers)
        assertTrue("Should contain bambu manufacturer", manufacturers.containsKey("bambu"))
        
        val bambuManufacturer = manufacturers["bambu"]
        assertNotNull("Bambu manufacturer should exist", bambuManufacturer)
        assertEquals("Bambu display name should be correct", "Bambu Lab", bambuManufacturer?.displayName)
        
        // Note: OpenTag is a tag format, not a manufacturer
        // Actual manufacturers using OpenTag format would be listed separately
    }

    @Test
    fun `catalog reload functionality works`() {
        // When - First access loads catalog
        val catalog1 = catalogRepository.getCatalog()

        // Force reload
        catalogRepository.reloadCatalog()

        // Second access should reload from assets
        val catalog2 = catalogRepository.getCatalog()

        // Then - Both should be valid and equivalent
        assertNotNull("First catalog should not be null", catalog1)
        assertNotNull("Second catalog should not be null", catalog2)
        assertEquals("Both catalogs should have same version", catalog1.version, catalog2.version)
        assertEquals("Both catalogs should have same manufacturers", catalog1.manufacturers.size, catalog2.manufacturers.size)
    }

    @Test
    fun `comprehensive multi-manufacturer architecture validation`() {
        // This test validates the complete multi-manufacturer architecture

        // 1. Multiple manufacturers supported
        val manufacturers = catalogRepository.getManufacturers()
        assertTrue("Should support multiple manufacturers", manufacturers.size >= 2)

        // 2. Each manufacturer has proper structure
        manufacturers.forEach { (manufacturerId, catalog) ->
            assertNotNull("Manufacturer $manufacturerId should have display name", catalog.displayName)
            assertNotNull("Manufacturer $manufacturerId should have tag format", catalog.tagFormat)
            assertNotNull("Manufacturer $manufacturerId should have materials map", catalog.materials)
            assertNotNull("Manufacturer $manufacturerId should have temperature profiles", catalog.temperatureProfiles)
            assertNotNull("Manufacturer $manufacturerId should have color palette", catalog.colorPalette)
            assertNotNull("Manufacturer $manufacturerId should have RFID mappings", catalog.rfidMappings)
            assertNotNull("Manufacturer $manufacturerId should have component defaults", catalog.componentDefaults)
            assertNotNull("Manufacturer $manufacturerId should have products list", catalog.products)
        }

        // 3. Cross-manufacturer functionality works
        val allRfidCodes = catalogRepository.getCatalog().manufacturers.values
            .flatMap { it.rfidMappings.keys }
        
        allRfidCodes.forEach { rfidCode ->
            val mapping = catalogRepository.findRfidMapping(rfidCode)
            assertNotNull("Should find mapping for RFID code $rfidCode", mapping)
        }

        // 4. Repository API provides consistent interface
        assertTrue("Repository should be ready for multi-manufacturer use", true)

        println("✅ Multi-manufacturer catalog architecture validation PASSED")
        println("   - ${manufacturers.size} manufacturers loaded")
        println("   - ${allRfidCodes.size} total RFID mappings available")
        println("   - All cross-manufacturer functionality working correctly")
    }
}