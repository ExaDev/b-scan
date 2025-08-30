package com.bscan.utils

import com.bscan.model.*
import com.bscan.service.BambuComponentDefinitions
import java.time.LocalDateTime
import java.util.UUID

/**
 * Shared test utilities for component creation and testing.
 * Provides consistent test data and helper functions across all test suites.
 */
object ComponentTestUtils {
    
    // === Test Constants ===
    
    const val TEST_TRAY_UID = "01008023ABC123"
    const val TEST_TAG_UID = "A1B2C3D4"
    const val TEST_MANUFACTURER = "Test Manufacturer"
    const val DEFAULT_TEST_MASS = 1000f
    
    // === Component Creation Helpers ===
    
    /**
     * Create a test filament component with standard defaults
     */
    fun createTestFilamentComponent(
        id: String = "test-filament-${UUID.randomUUID().toString().take(8)}",
        filamentType: String = "PLA_BASIC",
        colorName: String = "Black",
        colorHex: String = "#000000",
        massGrams: Float = DEFAULT_TEST_MASS,
        fullMassGrams: Float = DEFAULT_TEST_MASS,
        trayUid: String = TEST_TRAY_UID,
        parentComponentId: String? = null
    ): Component {
        return Component(
            id = id,
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = trayUid,
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = BambuComponentDefinitions.Filament.getName(filamentType, colorName),
            category = BambuComponentDefinitions.Filament.CATEGORY,
            tags = BambuComponentDefinitions.Filament.TAGS,
            massGrams = massGrams,
            fullMassGrams = fullMassGrams,
            variableMass = BambuComponentDefinitions.Filament.VARIABLE_MASS,
            manufacturer = BambuComponentDefinitions.Filament.MANUFACTURER,
            description = BambuComponentDefinitions.Filament.getDescription(filamentType, colorName),
            metadata = BambuComponentDefinitions.Filament.getMetadata(filamentType, colorName, trayUid),
            parentComponentId = parentComponentId
        )
    }
    
    /**
     * Create a test core component with standard defaults
     */
    fun createTestCoreComponent(
        id: String = "test-core-${UUID.randomUUID().toString().take(8)}",
        parentComponentId: String? = null
    ): Component {
        return Component(
            id = id,
            name = BambuComponentDefinitions.Core.NAME,
            category = BambuComponentDefinitions.Core.CATEGORY,
            tags = BambuComponentDefinitions.Core.TAGS,
            massGrams = BambuComponentDefinitions.Core.MASS_GRAMS,
            variableMass = false,
            manufacturer = BambuComponentDefinitions.Core.MANUFACTURER,
            description = BambuComponentDefinitions.Core.DESCRIPTION,
            metadata = BambuComponentDefinitions.Core.METADATA,
            parentComponentId = parentComponentId
        )
    }
    
    /**
     * Create a test spool component with standard defaults
     */
    fun createTestSpoolComponent(
        id: String = "test-spool-${UUID.randomUUID().toString().take(8)}",
        parentComponentId: String? = null
    ): Component {
        return Component(
            id = id,
            name = BambuComponentDefinitions.Spool.NAME,
            category = BambuComponentDefinitions.Spool.CATEGORY,
            tags = BambuComponentDefinitions.Spool.TAGS,
            massGrams = BambuComponentDefinitions.Spool.MASS_GRAMS,
            variableMass = false,
            manufacturer = BambuComponentDefinitions.Spool.MANUFACTURER,
            description = BambuComponentDefinitions.Spool.DESCRIPTION,
            metadata = BambuComponentDefinitions.Spool.METADATA,
            parentComponentId = parentComponentId
        )
    }
    
    /**
     * Create a test RFID tag component with standard defaults
     */
    fun createTestRfidTagComponent(
        id: String = "test-tag-${UUID.randomUUID().toString().take(8)}",
        tagUid: String = TEST_TAG_UID,
        trayUid: String = TEST_TRAY_UID,
        parentComponentId: String? = null
    ): Component {
        return Component(
            id = id,
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.RFID_HARDWARE,
                    value = tagUid,
                    purpose = IdentifierPurpose.AUTHENTICATION
                ),
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = trayUid,
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = BambuComponentDefinitions.RfidTag.NAME,
            category = BambuComponentDefinitions.RfidTag.CATEGORY,
            tags = BambuComponentDefinitions.RfidTag.TAGS,
            massGrams = BambuComponentDefinitions.RfidTag.MASS_GRAMS,
            variableMass = false,
            manufacturer = BambuComponentDefinitions.RfidTag.MANUFACTURER,
            description = BambuComponentDefinitions.RfidTag.DESCRIPTION,
            metadata = BambuComponentDefinitions.RfidTag.getMetadata(tagUid),
            parentComponentId = parentComponentId
        )
    }
    
    /**
     * Create a test tray component (parent component containing other components)
     */
    fun createTestTrayComponent(
        id: String = "test-tray-${UUID.randomUUID().toString().take(8)}",
        trayUid: String = TEST_TRAY_UID,
        filamentType: String = "PLA_BASIC",
        colorName: String = "Black"
    ): Component {
        return Component(
            id = id,
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = trayUid,
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "Bambu $filamentType $colorName Tray",
            category = "tray",
            tags = listOf("bambu", "composite"),
            massGrams = null, // Will be calculated from children
            variableMass = true, // Contains variable-mass filament
            manufacturer = "Bambu Lab",
            description = "Complete Bambu filament tray with $filamentType $colorName filament",
            metadata = mapOf(
                "trayUid" to trayUid,
                "filamentType" to filamentType,
                "colorName" to colorName
            )
        )
    }
    
    /**
     * Create a complete test component hierarchy (tray with all child components)
     */
    fun createTestComponentHierarchy(
        trayUid: String = TEST_TRAY_UID,
        tagUid: String = TEST_TAG_UID,
        filamentType: String = "PLA_BASIC",
        colorName: String = "Black",
        colorHex: String = "#000000"
    ): ComponentHierarchy {
        val trayId = "tray-$trayUid"
        val filamentId = "filament-$trayUid"
        val coreId = "core-$trayUid"
        val spoolId = "spool-$trayUid"
        val tagId = "tag-$tagUid"
        
        val trayComponent = createTestTrayComponent(
            id = trayId,
            trayUid = trayUid,
            filamentType = filamentType,
            colorName = colorName
        ).copy(
            childComponents = listOf(filamentId, coreId, spoolId, tagId)
        )
        
        val filamentComponent = createTestFilamentComponent(
            id = filamentId,
            filamentType = filamentType,
            colorName = colorName,
            colorHex = colorHex,
            trayUid = trayUid,
            parentComponentId = trayId
        )
        
        val coreComponent = createTestCoreComponent(
            id = coreId,
            parentComponentId = trayId
        )
        
        val spoolComponent = createTestSpoolComponent(
            id = spoolId,
            parentComponentId = trayId
        )
        
        val tagComponent = createTestRfidTagComponent(
            id = tagId,
            tagUid = tagUid,
            trayUid = trayUid,
            parentComponentId = trayId
        )
        
        return ComponentHierarchy(
            trayComponent = trayComponent,
            childComponents = listOf(filamentComponent, coreComponent, spoolComponent, tagComponent),
            allComponents = listOf(trayComponent, filamentComponent, coreComponent, spoolComponent, tagComponent)
        )
    }
    
    // === Component Measurement Helpers ===
    
    /**
     * Create a test component measurement
     */
    fun createTestMeasurement(
        id: String = "test-measurement-${UUID.randomUUID().toString().take(8)}",
        componentId: String = TEST_TRAY_UID,
        measuredMassGrams: Float = DEFAULT_TEST_MASS,
        measurementType: MeasurementType = MeasurementType.TOTAL_MASS,
        notes: String = "Test measurement",
        isVerified: Boolean = true,
        daysAgo: Long = 0
    ): ComponentMeasurement {
        return ComponentMeasurement(
            id = id,
            componentId = componentId,
            measuredMassGrams = measuredMassGrams,
            measurementType = measurementType,
            measuredAt = LocalDateTime.now().minusDays(daysAgo),
            notes = notes,
            isVerified = isVerified
        )
    }
    
    /**
     * Create a series of test measurements showing consumption over time
     */
    fun createTestMeasurementSeries(
        componentId: String = TEST_TRAY_UID,
        initialMass: Float = 1245f,
        finalMass: Float = 850f,
        measurementCount: Int = 5
    ): List<ComponentMeasurement> {
        val measurements = mutableListOf<ComponentMeasurement>()
        val massStep = (initialMass - finalMass) / (measurementCount - 1)
        
        repeat(measurementCount) { index ->
            val currentMass = initialMass - (massStep * index)
            val daysAgo = (measurementCount - 1 - index).toLong()
            
            measurements.add(
                createTestMeasurement(
                    componentId = componentId,
                    measuredMassGrams = currentMass,
                    notes = "Measurement ${index + 1} of $measurementCount",
                    daysAgo = daysAgo
                )
            )
        }
        
        return measurements
    }
    
    // === FilamentInfo Test Data ===
    
    /**
     * Create test FilamentInfo for component factory testing
     */
    fun createTestFilamentInfo(
        tagUid: String = TEST_TAG_UID,
        trayUid: String = TEST_TRAY_UID,
        filamentType: String = "PLA_BASIC",
        colorName: String = "Black",
        colorHex: String = "#000000",
        manufacturerName: String = "Bambu Lab"
    ): FilamentInfo {
        return FilamentInfo(
            tagUid = tagUid,
            trayUid = trayUid,
            manufacturerName = manufacturerName,
            filamentType = filamentType,
            detailedFilamentType = filamentType,
            colorHex = colorHex,
            colorName = colorName,
            spoolWeight = DEFAULT_TEST_MASS.toInt(),
            filamentDiameter = 1.75f,
            filamentLength = 330000,
            productionDate = "2025-01-01",
            minTemperature = 190,
            maxTemperature = 220,
            bedTemperature = 60,
            dryingTemperature = 45,
            dryingTime = 8
        )
    }
    
    // === Test Data Variants ===
    
    /**
     * Get test data for different filament materials
     */
    fun getTestFilamentVariants(): List<TestFilamentVariant> {
        return listOf(
            TestFilamentVariant("PLA_BASIC", "Black", "#000000", 1000f),
            TestFilamentVariant("PLA_BASIC", "White", "#FFFFFF", 1000f),
            TestFilamentVariant("PETG", "Clear", "#FFFFFF", 1000f),
            TestFilamentVariant("ABS", "Black", "#000000", 1000f),
            TestFilamentVariant("TPU", "Red", "#FF0000", 500f),
            TestFilamentVariant("ASA", "Gray", "#808080", 1000f),
            TestFilamentVariant("PVA", "Natural", "#F5F5DC", 500f)
        )
    }
    
    // === Validation Helpers ===
    
    /**
     * Validate that a component has no material metadata in core/spool components
     */
    fun validateNoMaterialMetadata(component: Component): Boolean {
        return when (component.category) {
            "core", "spool" -> {
                val hasMaterialMetadata = component.metadata.keys.any { key ->
                    key in listOf("material", "materialType", "filamentType")
                }
                !hasMaterialMetadata
            }
            else -> true // Other components can have material metadata
        }
    }
    
    /**
     * Validate component hierarchy structure
     */
    fun validateComponentHierarchy(hierarchy: ComponentHierarchy): List<String> {
        val errors = mutableListOf<String>()
        
        // Check parent-child relationships
        hierarchy.childComponents.forEach { child ->
            if (child.parentComponentId != hierarchy.trayComponent.id) {
                errors.add("Child component ${child.id} has incorrect parent reference")
            }
            if (child.id !in hierarchy.trayComponent.childComponents) {
                errors.add("Child component ${child.id} not referenced in parent's childComponents list")
            }
        }
        
        // Check that all components have consistent tray UIDs
        val expectedTrayUid = hierarchy.trayComponent.getIdentifierByType(IdentifierType.CONSUMABLE_UNIT)?.value
        hierarchy.childComponents.forEach { child ->
            val childTrayUid = child.metadata["trayUid"]
            if (childTrayUid != null && childTrayUid != expectedTrayUid) {
                errors.add("Child component ${child.id} has inconsistent trayUid: $childTrayUid vs $expectedTrayUid")
            }
        }
        
        // Validate no material metadata in core/spool
        hierarchy.allComponents.forEach { component ->
            if (!validateNoMaterialMetadata(component)) {
                errors.add("Component ${component.id} (${component.category}) has invalid material metadata")
            }
        }
        
        return errors
    }
}

/**
 * Data class representing a test filament variant
 */
data class TestFilamentVariant(
    val filamentType: String,
    val colorName: String,
    val colorHex: String,
    val defaultMass: Float
)

/**
 * Data class representing a complete component hierarchy for testing
 */
data class ComponentHierarchy(
    val trayComponent: Component,
    val childComponents: List<Component>,
    val allComponents: List<Component>
) {
    fun getFilamentComponent(): Component? = childComponents.find { it.category == "filament" }
    fun getCoreComponent(): Component? = childComponents.find { it.category == "core" }
    fun getSpoolComponent(): Component? = childComponents.find { it.category == "spool" }
    fun getRfidTagComponent(): Component? = childComponents.find { it.category == "rfid-tag" }
}