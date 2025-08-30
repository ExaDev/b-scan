package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Unit tests for the new hierarchical ComponentRepository
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ComponentRepositoryTest {

    private lateinit var context: Context
    
    @Before
    fun setup() {
        // Use real Android context from Robolectric instead of mocking
        context = RuntimeEnvironment.getApplication()
        
        // Clear any existing SharedPreferences data
        context.getSharedPreferences("component_data", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("component_measurement_data", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @Test
    fun testComponentHierarchy_parentChildRelationships() {
        val repository = ComponentRepository(context)
        
        // Create parent component (inventory item)
        val parentComponent = Component(
            id = "inventory_001",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = "TRAY_ABC123",
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "Bambu PLA Black",
            category = "filament-tray",
            tags = listOf("bambu", "inventory-item"),
            childComponents = listOf("filament_001", "core_001"),
            massGrams = null
        )
        
        // Create child components
        val filamentComponent = Component(
            id = "filament_001",
            name = "PLA Black Filament",
            category = "filament",
            tags = listOf("consumable", "variable-mass"),
            parentComponentId = "inventory_001",
            massGrams = 800f,
            fullMassGrams = 1000f,
            variableMass = true
        )
        
        val coreComponent = Component(
            id = "core_001",
            name = "Cardboard Core",
            category = "core",
            tags = listOf("reusable", "fixed-mass"),
            parentComponentId = "inventory_001",
            massGrams = 33f,
            variableMass = false
        )
        
        // Save components
        repository.saveComponent(parentComponent)
        repository.saveComponent(filamentComponent)
        repository.saveComponent(coreComponent)
        
        // Test inventory item query
        val inventoryItems = repository.getInventoryItems()
        assertTrue("Should find inventory items", inventoryItems.any { it.id == "inventory_001" })
        
        // Test finding by unique ID
        val foundInventory = repository.findInventoryByUniqueId("TRAY_ABC123")
        assertNotNull("Should find inventory by unique ID", foundInventory)
        assertEquals("inventory_001", foundInventory?.id)
        
        // Test child component queries
        val children = repository.getChildComponents("inventory_001")
        assertEquals("Should have 2 child components", 2, children.size)
        assertTrue("Should contain filament component", 
            children.any { it.id == "filament_001" })
        assertTrue("Should contain core component", 
            children.any { it.id == "core_001" })
    }
    
    @Test
    fun testMassCalculation_hierarchicalTotalMass() {
        val repository = ComponentRepository(context)
        
        // Create components with known masses
        val filament = Component(
            id = "filament_001",
            name = "PLA Filament",
            category = "filament",
            parentComponentId = "tray_001",
            massGrams = 750f,
            variableMass = true
        )
        
        val core = Component(
            id = "core_001", 
            name = "Cardboard Core",
            category = "core",
            parentComponentId = "tray_001",
            massGrams = 33f,
            variableMass = false
        )
        
        val spool = Component(
            id = "spool_001",
            name = "Refillable Spool",
            category = "spool",
            parentComponentId = "tray_001",
            massGrams = 212f,
            variableMass = false
        )
        
        val tray = Component(
            id = "tray_001",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = "TRAY_123",
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "Complete Tray",
            category = "filament-tray",
            childComponents = listOf("filament_001", "core_001", "spool_001"),
            massGrams = null // Should be calculated from children
        )
        
        // Save all components
        repository.saveComponent(filament)
        repository.saveComponent(core)
        repository.saveComponent(spool)
        repository.saveComponent(tray)
        
        // Test total mass calculation
        val totalMass = repository.getTotalMass("tray_001")
        val expectedMass = 750f + 33f + 212f // 995g
        assertEquals("Total mass should be sum of children", expectedMass, totalMass, 0.01f)
    }
    
    @Test
    fun testMassInference_calculateUnknownComponent() {
        val repository = ComponentRepository(context)
        
        // Create known components
        val knownComponent1 = Component(
            id = "known_001",
            name = "Known Component 1",
            category = "core",
            parentComponentId = "parent_001",
            massGrams = 100f
        )
        
        val knownComponent2 = Component(
            id = "known_002", 
            name = "Known Component 2",
            category = "spool",
            parentComponentId = "parent_001",
            massGrams = 200f
        )
        
        val unknownComponent = Component(
            id = "unknown_001",
            name = "Unknown Component",
            category = "packaging",
            parentComponentId = "parent_001",
            massGrams = null // Unknown mass
        )
        
        val parent = Component(
            id = "parent_001",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = "PARENT_123",
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "Parent Component",
            category = "composite",
            childComponents = listOf("known_001", "known_002", "unknown_001"),
            massGrams = null
        )
        
        // Save components
        repository.saveComponent(knownComponent1)
        repository.saveComponent(knownComponent2)
        repository.saveComponent(unknownComponent)
        repository.saveComponent(parent)
        
        // Test mass inference
        val totalMeasuredMass = 350f // Total measured mass
        val inferenceRequest = MassInferenceRequest(
            parentComponentId = "parent_001",
            totalMeasuredMass = totalMeasuredMass,
            unknownComponentId = "unknown_001",
            knownComponentIds = listOf("known_001", "known_002")
        )
        
        val inferredMass = repository.inferComponentMass(inferenceRequest)
        val expectedInferredMass = 350f - 100f - 200f // 50g
        assertEquals("Inferred mass should be total minus known", 
            expectedInferredMass, inferredMass ?: 0f, 0.01f)
    }
    
    @Test
    fun testCategoryAndTagQueries() {
        val repository = ComponentRepository(context)
        
        val component1 = Component(
            id = "comp_001",
            name = "PLA Filament",
            category = "filament",
            tags = listOf("consumable", "pla", "bambu"),
            massGrams = 800f
        )
        
        val component2 = Component(
            id = "comp_002", 
            name = "PETG Filament",
            category = "filament",
            tags = listOf("consumable", "petg", "bambu"),
            massGrams = 750f
        )
        
        val component3 = Component(
            id = "comp_003",
            name = "Cardboard Core",
            category = "core",
            tags = listOf("reusable", "bambu"),
            massGrams = 33f
        )
        
        // Save components
        repository.saveComponent(component1)
        repository.saveComponent(component2)
        repository.saveComponent(component3)
        
        // Test category queries
        val filamentComponents = repository.getComponentsByCategory("filament")
        assertEquals("Should find 2 filament components", 2, filamentComponents.size)
        
        val coreComponents = repository.getComponentsByCategory("core")
        assertEquals("Should find 1 core component", 1, coreComponents.size)
        
        // Test tag queries
        val bambuComponents = repository.getComponentsByTag("bambu")
        assertEquals("Should find 3 bambu components", 3, bambuComponents.size)
        
        val plaComponents = repository.getComponentsByTag("pla")
        assertEquals("Should find 1 PLA component", 1, plaComponents.size)
        
        // Test multiple tag query (AND logic)
        val consumableBambuComponents = repository.getComponentsByTags(listOf("consumable", "bambu"))
        assertEquals("Should find 2 consumable bambu components", 2, consumableBambuComponents.size)
    }
    
    @Test
    fun testVariableMassComponents() {
        val repository = ComponentRepository(context)
        
        val variableComponent = Component(
            id = "var_001",
            name = "Variable Component",
            category = "filament",
            massGrams = 800f,
            fullMassGrams = 1000f,
            variableMass = true
        )
        
        val fixedComponent = Component(
            id = "fix_001",
            name = "Fixed Component", 
            category = "core",
            massGrams = 33f,
            variableMass = false
        )
        
        repository.saveComponent(variableComponent)
        repository.saveComponent(fixedComponent)
        
        // Test variable mass queries
        val variableComponents = repository.getVariableMassComponents()
        assertEquals("Should find 1 variable mass component", 1, variableComponents.size)
        assertEquals("var_001", variableComponents[0].id)
        
        val fixedComponents = repository.getFixedMassComponents()
        assertEquals("Should find 1 fixed mass component", 1, fixedComponents.size)
        assertEquals("fix_001", fixedComponents[0].id)
        
        // Test percentage calculations
        val remainingPercentage = variableComponent.getRemainingPercentage()
        assertEquals("Should calculate correct remaining percentage", 0.8f, remainingPercentage ?: 0f, 0.01f)
        
        val consumedMass = variableComponent.getConsumedMass()
        assertEquals("Should calculate correct consumed mass", 200f, consumedMass ?: 0f, 0.01f)
        
        assertTrue("Should be running low when <20%", 
            variableComponent.copy(massGrams = 150f).isRunningLow)
        assertFalse("Should not be running low when >20%", variableComponent.isRunningLow)
    }
    
    @Test
    fun testComponentUpdates_withChildAndParent() {
        val repository = ComponentRepository(context)
        
        val parent = Component(
            id = "parent_001",
            identifiers = listOf(
                ComponentIdentifier(
                    type = IdentifierType.CONSUMABLE_UNIT,
                    value = "PARENT_ID",
                    purpose = IdentifierPurpose.TRACKING
                )
            ),
            name = "Parent Component",
            category = "composite",
            childComponents = emptyList(),
            massGrams = 0f
        )
        
        val child = Component(
            id = "child_001",
            name = "Child Component",
            category = "part",
            massGrams = 50f
        )
        
        repository.saveComponent(parent)
        repository.saveComponent(child)
        
        // Test adding child to parent
        repository.addChildComponent("parent_001", "child_001")
        
        val updatedParent = repository.getComponent("parent_001")
        val updatedChild = repository.getComponent("child_001")
        
        assertTrue("Parent should contain child", 
            updatedParent?.childComponents?.contains("child_001") == true)
        assertEquals("Child should reference parent", 
            "parent_001", updatedChild?.parentComponentId)
        
        // Test removing child from parent
        repository.removeChildComponent("parent_001", "child_001")
        
        val finalParent = repository.getComponent("parent_001")
        val finalChild = repository.getComponent("child_001")
        
        assertFalse("Parent should not contain child after removal",
            finalParent?.childComponents?.contains("child_001") == true)
        assertNull("Child should not reference parent after removal",
            finalChild?.parentComponentId)
    }
}