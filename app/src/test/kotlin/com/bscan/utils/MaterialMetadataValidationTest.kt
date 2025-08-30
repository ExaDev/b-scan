package com.bscan.utils

import com.bscan.service.BambuComponentDefinitions
import org.junit.Test
import org.junit.Assert.*

/**
 * Test to verify that core and spool components don't have material metadata
 */
class MaterialMetadataValidationTest {
    
    @Test
    fun `BambuComponentDefinitions Core has no material metadata`() {
        val coreMetadata = BambuComponentDefinitions.Core.METADATA
        
        // Verify no material-related keys
        assertFalse("Core should not have 'material' metadata", 
            coreMetadata.containsKey("material"))
        assertFalse("Core should not have 'materialType' metadata", 
            coreMetadata.containsKey("materialType"))
        assertFalse("Core should not have 'filamentType' metadata", 
            coreMetadata.containsKey("filamentType"))
        
        // Verify it has appropriate metadata
        assertTrue("Core should have standardWeight metadata", 
            coreMetadata.containsKey("standardWeight"))
        assertEquals("33g", coreMetadata["standardWeight"])
    }
    
    @Test
    fun `BambuComponentDefinitions Spool has no material metadata`() {
        val spoolMetadata = BambuComponentDefinitions.Spool.METADATA
        
        // Verify no material-related keys
        assertFalse("Spool should not have 'material' metadata", 
            spoolMetadata.containsKey("material"))
        assertFalse("Spool should not have 'materialType' metadata", 
            spoolMetadata.containsKey("materialType"))
        assertFalse("Spool should not have 'filamentType' metadata", 
            spoolMetadata.containsKey("filamentType"))
        
        // Verify it has appropriate metadata
        assertTrue("Spool should have standardWeight metadata", 
            spoolMetadata.containsKey("standardWeight"))
        assertTrue("Spool should have type metadata", 
            spoolMetadata.containsKey("type"))
        assertEquals("212g", spoolMetadata["standardWeight"])
        assertEquals("refillable", spoolMetadata["type"])
    }
    
    @Test
    fun `created core components have no material metadata`() {
        val coreComponent = ComponentTestUtils.createTestCoreComponent()
        
        assertTrue("Core component should pass material metadata validation",
            ComponentTestUtils.validateNoMaterialMetadata(coreComponent))
        
        // Check metadata doesn't contain material keys
        assertFalse(coreComponent.metadata.containsKey("material"))
        assertFalse(coreComponent.metadata.containsKey("materialType"))
        assertFalse(coreComponent.metadata.containsKey("filamentType"))
    }
    
    @Test
    fun `created spool components have no material metadata`() {
        val spoolComponent = ComponentTestUtils.createTestSpoolComponent()
        
        assertTrue("Spool component should pass material metadata validation",
            ComponentTestUtils.validateNoMaterialMetadata(spoolComponent))
        
        // Check metadata doesn't contain material keys
        assertFalse(spoolComponent.metadata.containsKey("material"))
        assertFalse(spoolComponent.metadata.containsKey("materialType"))
        assertFalse(spoolComponent.metadata.containsKey("filamentType"))
    }
    
    @Test
    fun `filament components can have material metadata`() {
        val filamentComponent = ComponentTestUtils.createTestFilamentComponent(
            filamentType = "PLA_BASIC",
            colorName = "Black"
        )
        
        assertTrue("Filament component should pass material metadata validation",
            ComponentTestUtils.validateNoMaterialMetadata(filamentComponent))
        
        // Filament components SHOULD have material metadata
        assertTrue("Filament should have material metadata",
            filamentComponent.metadata.containsKey("material"))
        assertEquals("PLA_BASIC", filamentComponent.metadata["material"])
    }
    
    @Test
    fun `complete component hierarchy has clean core and spool`() {
        val hierarchy = ComponentTestUtils.createTestComponentHierarchy()
        
        val validationErrors = ComponentTestUtils.validateComponentHierarchy(hierarchy)
        assertTrue("Component hierarchy should have no validation errors: $validationErrors",
            validationErrors.isEmpty())
        
        // Specifically check core and spool
        val coreComponent = hierarchy.getCoreComponent()
        val spoolComponent = hierarchy.getSpoolComponent()
        
        assertNotNull(coreComponent)
        assertNotNull(spoolComponent)
        
        assertTrue("Core should have no material metadata",
            ComponentTestUtils.validateNoMaterialMetadata(coreComponent!!))
        assertTrue("Spool should have no material metadata",
            ComponentTestUtils.validateNoMaterialMetadata(spoolComponent!!))
    }
}