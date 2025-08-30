package com.bscan.utils

import com.bscan.model.IdentifierType
import com.bscan.model.IdentifierPurpose
import com.bscan.model.MeasurementType
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for ComponentTestUtils to ensure shared test utilities work correctly
 */
class ComponentTestUtilsTest {
    
    @Test
    fun `createTestFilamentComponent creates valid component`() {
        val component = ComponentTestUtils.createTestFilamentComponent(
            filamentType = "PLA_BASIC",
            colorName = "Black",
            colorHex = "#000000"
        )
        
        assertEquals("filament", component.category)
        assertEquals("Bambu Lab", component.manufacturer)
        assertTrue(component.variableMass)
        assertEquals(1000f, component.massGrams)
        assertEquals(1000f, component.fullMassGrams)
        
        // Check metadata
        assertEquals("PLA_BASIC", component.metadata["material"])
        assertEquals("Black", component.metadata["color"])
        assertEquals(ComponentTestUtils.TEST_TRAY_UID, component.metadata["trayUid"])
        
        // Check identifier
        val trackingId = component.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)
        assertNotNull(trackingId)
        assertEquals(IdentifierPurpose.TRACKING, trackingId?.purpose)
        assertEquals(ComponentTestUtils.TEST_TRAY_UID, trackingId?.value)
    }
    
    @Test
    fun `createTestCoreComponent has no material metadata`() {
        val component = ComponentTestUtils.createTestCoreComponent()
        
        assertEquals("core", component.category)
        assertEquals(33f, component.massGrams)
        assertFalse(component.variableMass)
        
        // Validate no material metadata
        assertTrue("Core component should not have material metadata", 
            ComponentTestUtils.validateNoMaterialMetadata(component))
        
        // Check that it only has appropriate metadata
        assertTrue(component.metadata.containsKey("standardWeight"))
        assertFalse(component.metadata.containsKey("material"))
        assertFalse(component.metadata.containsKey("materialType"))
    }
    
    @Test
    fun `createTestSpoolComponent has no material metadata`() {
        val component = ComponentTestUtils.createTestSpoolComponent()
        
        assertEquals("spool", component.category)
        assertEquals(212f, component.massGrams)
        assertFalse(component.variableMass)
        
        // Validate no material metadata
        assertTrue("Spool component should not have material metadata",
            ComponentTestUtils.validateNoMaterialMetadata(component))
        
        // Check that it only has appropriate metadata
        assertTrue(component.metadata.containsKey("standardWeight"))
        assertTrue(component.metadata.containsKey("type"))
        assertEquals("refillable", component.metadata["type"])
        assertFalse(component.metadata.containsKey("material"))
        assertFalse(component.metadata.containsKey("materialType"))
    }
    
    @Test
    fun `createTestRfidTagComponent has dual identifiers`() {
        val component = ComponentTestUtils.createTestRfidTagComponent()
        
        assertEquals("rfid-tag", component.category)
        assertEquals(0.5f, component.massGrams)
        assertFalse(component.variableMass)
        
        // Check dual identifiers
        val hardwareId = component.getIdentifierByType(IdentifierType.RFID_HARDWARE)
        assertNotNull(hardwareId)
        assertEquals(IdentifierPurpose.AUTHENTICATION, hardwareId?.purpose)
        assertEquals(ComponentTestUtils.TEST_TAG_UID, hardwareId?.value)
        
        val trackingId = component.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)
        assertNotNull(trackingId)
        assertEquals(IdentifierPurpose.TRACKING, trackingId?.purpose)
        assertEquals(ComponentTestUtils.TEST_TRAY_UID, trackingId?.value)
    }
    
    @Test
    fun `createTestComponentHierarchy creates valid structure`() {
        val hierarchy = ComponentTestUtils.createTestComponentHierarchy(
            filamentType = "PETG",
            colorName = "Clear"
        )
        
        // Check tray component
        assertEquals("tray", hierarchy.trayComponent.category)
        assertEquals(4, hierarchy.trayComponent.childComponents.size)
        assertEquals(4, hierarchy.childComponents.size)
        assertEquals(5, hierarchy.allComponents.size)
        
        // Check child components
        val filament = hierarchy.getFilamentComponent()
        val core = hierarchy.getCoreComponent()
        val spool = hierarchy.getSpoolComponent()
        val tag = hierarchy.getRfidTagComponent()
        
        assertNotNull(filament)
        assertNotNull(core)
        assertNotNull(spool)
        assertNotNull(tag)
        
        // Check parent references
        assertEquals(hierarchy.trayComponent.id, filament?.parentComponentId)
        assertEquals(hierarchy.trayComponent.id, core?.parentComponentId)
        assertEquals(hierarchy.trayComponent.id, spool?.parentComponentId)
        assertEquals(hierarchy.trayComponent.id, tag?.parentComponentId)
        
        // Validate hierarchy structure
        val validationErrors = ComponentTestUtils.validateComponentHierarchy(hierarchy)
        assertTrue("Hierarchy validation failed: $validationErrors", validationErrors.isEmpty())
    }
    
    @Test
    fun `createTestMeasurement creates valid measurement`() {
        val measurement = ComponentTestUtils.createTestMeasurement(
            measuredMassGrams = 950f,
            measurementType = MeasurementType.TOTAL_MASS,
            notes = "Test measurement"
        )
        
        assertEquals(ComponentTestUtils.TEST_TRAY_UID, measurement.componentId)
        assertEquals(950f, measurement.measuredMassGrams, 0.01f)
        assertEquals(MeasurementType.TOTAL_MASS, measurement.measurementType)
        assertEquals("Test measurement", measurement.notes)
        assertTrue(measurement.isVerified)
    }
    
    @Test
    fun `createTestMeasurementSeries creates decreasing mass over time`() {
        val measurements = ComponentTestUtils.createTestMeasurementSeries(
            initialMass = 1000f,
            finalMass = 500f,
            measurementCount = 5
        )
        
        assertEquals(5, measurements.size)
        
        // Check masses are decreasing
        assertEquals(1000f, measurements[0].measuredMassGrams, 0.01f)
        assertEquals(875f, measurements[1].measuredMassGrams, 0.01f)
        assertEquals(750f, measurements[2].measuredMassGrams, 0.01f)
        assertEquals(625f, measurements[3].measuredMassGrams, 0.01f)
        assertEquals(500f, measurements[4].measuredMassGrams, 0.01f)
        
        // Check dates are in chronological order (most recent first)
        assertTrue(measurements[0].measuredAt.isBefore(measurements[1].measuredAt))
        assertTrue(measurements[1].measuredAt.isBefore(measurements[2].measuredAt))
    }
    
    @Test
    fun `createTestFilamentInfo creates valid FilamentInfo`() {
        val filamentInfo = ComponentTestUtils.createTestFilamentInfo(
            filamentType = "ABS",
            colorName = "Red",
            colorHex = "#FF0000"
        )
        
        assertEquals(ComponentTestUtils.TEST_TAG_UID, filamentInfo.tagUid)
        assertEquals(ComponentTestUtils.TEST_TRAY_UID, filamentInfo.trayUid)
        assertEquals("ABS", filamentInfo.filamentType)
        assertEquals("Red", filamentInfo.colorName)
        assertEquals("#FF0000", filamentInfo.colorHex)
        assertEquals("Bambu Lab", filamentInfo.manufacturerName)
        assertEquals(1000, filamentInfo.spoolWeight)
        assertEquals(1.75f, filamentInfo.filamentDiameter, 0.01f)
    }
    
    @Test
    fun `getTestFilamentVariants returns diverse materials`() {
        val variants = ComponentTestUtils.getTestFilamentVariants()
        
        assertTrue("Should have multiple variants", variants.size >= 5)
        
        val materialTypes = variants.map { it.filamentType }.distinct()
        assertTrue("Should have PLA_BASIC", materialTypes.contains("PLA_BASIC"))
        assertTrue("Should have PETG", materialTypes.contains("PETG"))
        assertTrue("Should have ABS", materialTypes.contains("ABS"))
        assertTrue("Should have TPU", materialTypes.contains("TPU"))
        
        // Check TPU has different mass
        val tpuVariant = variants.find { it.filamentType == "TPU" }
        assertNotNull(tpuVariant)
        assertEquals(500f, tpuVariant?.defaultMass)
    }
    
    @Test
    fun `validateNoMaterialMetadata correctly identifies invalid components`() {
        // Valid core component
        val validCore = ComponentTestUtils.createTestCoreComponent()
        assertTrue(ComponentTestUtils.validateNoMaterialMetadata(validCore))
        
        // Invalid core component with material metadata
        val invalidCore = validCore.copy(
            metadata = validCore.metadata + mapOf("material" to "cardboard")
        )
        assertFalse(ComponentTestUtils.validateNoMaterialMetadata(invalidCore))
        
        // Filament component with material metadata should be valid
        val filamentWithMaterial = ComponentTestUtils.createTestFilamentComponent()
        assertTrue(ComponentTestUtils.validateNoMaterialMetadata(filamentWithMaterial))
    }
}