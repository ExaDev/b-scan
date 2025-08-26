package com.bscan.repository

import com.bscan.model.InventoryItem
import com.bscan.model.Component
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for component addition logic
 */
class ComponentAdditionUnitTest {

    @Test
    fun testInventoryItem_withComponent_addsNewComponent() {
        val initialItem = InventoryItem(
            trayUid = "TEST_001",
            components = listOf("component1", "component2"),
            totalMeasuredMass = 1000f,
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now()
        )

        val updatedItem = initialItem.withComponent("component3")

        assert(updatedItem.components.size == 3) { "Should have 3 components" }
        assert("component3" in updatedItem.components) { "Should contain new component" }
        assert(updatedItem.components.containsAll(listOf("component1", "component2", "component3"))) { 
            "Should contain all components" 
        }
    }

    @Test
    fun testInventoryItem_withComponent_ignoresDuplicates() {
        val initialItem = InventoryItem(
            trayUid = "TEST_001",
            components = listOf("component1", "component2"),
            totalMeasuredMass = 1000f,
            measurements = emptyList(),
            lastUpdated = LocalDateTime.now()
        )

        val updatedItem = initialItem.withComponent("component1")

        assert(updatedItem.components.size == 2) { "Should still have 2 components" }
        assert(updatedItem.components.count { it == "component1" } == 1) { "Should not duplicate component1" }
    }

    @Test
    fun testComponent_createFilamentComponent_setsCorrectProperties() {
        val component = Component(
            id = "test_filament",
            name = "PLA_BASIC - Black",
            category = "filament",
            massGrams = 1000f,
            fullMassGrams = 1000f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component"
        )

        assert(component.variableMass) { "Filament components should be variable mass" }
        assert(component.category == "filament") { "Should have correct category" }
        assert(component.massGrams == 1000f) { "Should have correct mass" }
        assert(component.fullMassGrams == 1000f) { "Should have correct full mass" }
        assert(component.getRemainingPercentage() == 1.0f) { "Should be 100% remaining when mass equals full mass" }
    }

    @Test
    fun testComponent_createFixedComponent_setsCorrectProperties() {
        val component = Component(
            id = "bambu_cardboard_core",
            name = "Bambu Cardboard Core",
            category = "core",
            massGrams = 33f,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Standard Bambu Lab cardboard core (33g)"
        )

        assert(!component.variableMass) { "Core components should be fixed mass" }
        assert(component.category == "core") { "Should have correct category" }
        assert(component.massGrams == 33f) { "Should have correct mass" }
        assert(component.fullMassGrams == null) { "Fixed components don't need full mass" }
    }

    @Test
    fun testComponent_withUpdatedMass_updatesCorrectly() {
        val originalComponent = Component(
            id = "test_filament",
            name = "PLA_BASIC - Black",
            category = "filament",
            massGrams = 1000f,
            fullMassGrams = 1000f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component"
        )

        val updatedComponent = originalComponent.withUpdatedMass(500f)

        assert(updatedComponent.massGrams == 500f) { "Should have updated mass" }
        assert(updatedComponent.fullMassGrams == 1000f) { "Should preserve full mass" }
        assert(updatedComponent.getRemainingPercentage() == 0.5f) { "Should calculate 50% remaining" }
        assert(updatedComponent.getConsumedMass() == 500f) { "Should calculate 500g consumed" }
    }

    @Test
    fun testComponent_consumptionCalculations() {
        val component = Component(
            id = "test_filament",
            name = "PLA_BASIC - Black",
            category = "filament",
            massGrams = 750f,
            fullMassGrams = 1000f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component"
        )

        assert(component.getRemainingPercentage() == 0.75f) { "Should calculate 75% remaining" }
        assert(component.getConsumedMass() == 250f) { "Should calculate 250g consumed" }
        assert(!component.isRunningLow) { "Should not be running low at 75%" }
        assert(!component.isNearlyEmpty) { "Should not be nearly empty at 75%" }
    }

    @Test
    fun testComponent_runningLowCalculations() {
        val runningLowComponent = Component(
            id = "test_filament",
            name = "PLA_BASIC - Black",
            category = "filament",
            massGrams = 150f,
            fullMassGrams = 1000f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component"
        )

        assert(runningLowComponent.getRemainingPercentage() == 0.15f) { "Should calculate 15% remaining" }
        assert(runningLowComponent.isRunningLow) { "Should be running low at 15%" }
        assert(!runningLowComponent.isNearlyEmpty) { "Should not be nearly empty at 15%" }
    }

    @Test
    fun testComponent_nearlyEmptyCalculations() {
        val nearlyEmptyComponent = Component(
            id = "test_filament", 
            name = "PLA_BASIC - Black",
            category = "filament",
            massGrams = 30f,
            fullMassGrams = 1000f,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component"
        )

        assert(nearlyEmptyComponent.getRemainingPercentage() == 0.03f) { "Should calculate 3% remaining" }
        assert(nearlyEmptyComponent.isRunningLow) { "Should be running low at 3%" }
        assert(nearlyEmptyComponent.isNearlyEmpty) { "Should be nearly empty at 3%" }
    }
}