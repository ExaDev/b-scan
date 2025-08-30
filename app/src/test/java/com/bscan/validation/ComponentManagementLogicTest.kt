package com.bscan.validation

import com.bscan.logic.MassCalculationService
import com.bscan.logic.WeightUnit
import com.bscan.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for component management business logic validation.
 * 
 * Focuses on:
 * - MassCalculationService algorithms
 * - Component validation logic
 * - Edge cases and error conditions
 * - Mathematical calculations accuracy
 * - Business rule enforcement
 * 
 * These tests run fast and don't require Android components.
 */
class ComponentManagementLogicTest {

    private lateinit var massCalculationService: MassCalculationService

    @Before
    fun setUp() {
        massCalculationService = MassCalculationService()
    }

    // ===== MASS CALCULATION VALIDATION =====

    @Test
    fun calculateFilamentMassFromTotal_validInput_calculatesCorrectly() {
        // Arrange
        val components = listOf(
            createFilamentComponent("filament", 800f),
            createFixedComponent("core", "core", 33f),
            createFixedComponent("spool", "spool", 212f)
        )
        val totalMeasuredMass = 1045f
        
        // Act
        val result = massCalculationService.calculateFilamentMassFromTotal(
            totalMeasuredMass = totalMeasuredMass,
            components = components,
            measurementType = MeasurementType.TOTAL_MASS
        )
        
        // Assert
        assertTrue("Calculation should succeed", result.success)
        assertEquals("Filament mass should be total - fixed", 800f, result.componentMass)
        assertEquals("Total mass should match input", totalMeasuredMass, result.totalMass)
    }

    @Test
    fun calculateFilamentMassFromTotal_emptyWeight_calculatesAsEmpty() {
        // Arrange
        val components = listOf(
            createFilamentComponent("filament", 0f),
            createFixedComponent("core", "core", 33f)
        )
        val emptyMass = 33f
        
        // Act
        val result = massCalculationService.calculateFilamentMassFromTotal(
            totalMeasuredMass = emptyMass,
            components = components,
            measurementType = MeasurementType.COMPONENT_ONLY
        )
        
        // Assert
        assertTrue("Empty weight calculation should succeed", result.success)
        assertEquals("Filament mass should be 0 for empty measurement", 0f, result.componentMass)
        assertEquals("Total mass should match input for empty measurement", emptyMass, result.totalMass)
    }

    @Test
    fun calculateFilamentMassFromTotal_negativeMass_returnsError() {
        // Arrange
        val components = listOf(createFixedComponent("core", "core", 33f))
        
        // Act
        val result = massCalculationService.calculateFilamentMassFromTotal(
            totalMeasuredMass = -10f,
            components = components
        )
        
        // Assert
        assertFalse("Should fail with negative mass", result.success)
        assertNotNull("Should have error message", result.errorMessage)
        assertTrue("Error should mention positive mass", 
            result.errorMessage!!.contains("greater than 0"))
    }

    @Test
    fun calculateFilamentMassFromTotal_massLessThanFixed_returnsError() {
        // Arrange
        val components = listOf(
            createFixedComponent("core", "core", 33f),
            createFixedComponent("spool", "spool", 212f)
        )
        val insufficientMass = 200f // Less than fixed component total (245f)
        
        // Act
        val result = massCalculationService.calculateFilamentMassFromTotal(
            totalMeasuredMass = insufficientMass,
            components = components
        )
        
        // Assert
        assertFalse("Should fail when total < fixed components", result.success)
        assertNotNull("Should have error message", result.errorMessage)
        assertTrue("Error should mention negative filament mass", 
            result.errorMessage!!.contains("negative"))
    }

    // ===== COMPONENT UPDATE ALGORITHMS =====

    @Test
    fun updateVariableComponents_proportionalDistribution_maintainsRatios() {
        // Arrange: Two filament components with different masses
        val components = listOf(
            createFilamentComponent("f1", 600f), // 60% of variable mass
            createFilamentComponent("f2", 400f), // 40% of variable mass
            createFixedComponent("core", "core", 33f)
        )
        val newTotalMass = 1133f // Should give 800f to variables (600 + 200)
        
        // Act
        val updatedComponents = massCalculationService.updateVariableComponents(components, newTotalMass)
        
        // Assert
        val updatedF1 = updatedComponents.find { it.id == "f1" }!!
        val updatedF2 = updatedComponents.find { it.id == "f2" }!!
        val fixedComponent = updatedComponents.find { it.id == "core" }!!
        
        // Fixed component should not change
        assertEquals("Fixed component should not change", 33f, fixedComponent.massGrams ?: 0f)
        
        // Variable components should maintain proportions
        val availableForVariable = newTotalMass - 33f // 1100f
        assertEquals("F1 should get 60% of available", 660f, updatedF1.massGrams ?: 0f, 0.01f)
        assertEquals("F2 should get 40% of available", 440f, updatedF2.massGrams ?: 0f, 0.01f)
        
        // Total should match
        val calculatedTotal = updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Total should match new mass", newTotalMass, calculatedTotal, 0.01f)
    }

    @Test
    fun updateVariableComponents_zeroVariable_equalDistribution() {
        // Arrange: Variable components with zero mass (equal distribution scenario)
        val components = listOf(
            createFilamentComponent("f1", 0f),
            createFilamentComponent("f2", 0f),
            createFixedComponent("core", "core", 33f)
        )
        val newTotalMass = 1033f
        
        // Act
        val updatedComponents = massCalculationService.updateVariableComponents(components, newTotalMass)
        
        // Assert
        val variableComponents = updatedComponents.filter { it.variableMass }
        val expectedPerVariable = (newTotalMass - 33f) / 2f // 500f each
        
        variableComponents.forEach { component ->
            assertEquals("Each variable component should get equal share", 
                expectedPerVariable, component.massGrams ?: 0f)
        }
    }

    @Test
    fun updateVariableComponents_negativeAvailable_setsZero() {
        // Arrange: Total mass less than fixed components
        val components = listOf(
            createFilamentComponent("f1", 100f),
            createFixedComponent("core", "core", 200f)
        )
        val insufficientTotal = 150f
        
        // Act
        val updatedComponents = massCalculationService.updateVariableComponents(components, insufficientTotal)
        
        // Assert
        val variableComponent = updatedComponents.find { it.variableMass }!!
        assertEquals("Variable component should be set to zero", 0f, variableComponent.massGrams ?: 0f)
    }

    // ===== DISTRIBUTION ALGORITHMS =====

    @Test
    fun distributeToVariableComponents_validDistribution_success() {
        // Arrange
        val components = listOf(
            createFilamentComponent("f1", 300f), // 75% of current variable
            createFilamentComponent("f2", 100f), // 25% of current variable
            createFixedComponent("core", "core", 50f)
        )
        val newTotalMass = 850f // 800f available for variables
        
        // Act
        val result = massCalculationService.distributeToVariableComponents(components, newTotalMass)
        
        // Assert
        assertTrue("Distribution should succeed", result.success)
        assertNull("Should not have error message", result.errorMessage)
        
        val updatedF1 = result.updatedComponents.find { it.id == "f1" }!!
        val updatedF2 = result.updatedComponents.find { it.id == "f2" }!!
        
        // Should maintain 75%/25% ratio
        assertEquals("F1 should get 75% of 800f", 600f, updatedF1.massGrams ?: 0f, 0.01f)
        assertEquals("F2 should get 25% of 800f", 200f, updatedF2.massGrams ?: 0f, 0.01f)
    }

    @Test
    fun distributeToVariableComponents_totalLessThanFixed_fails() {
        // Arrange
        val components = listOf(
            createFilamentComponent("f1", 100f),
            createFixedComponent("core", "core", 200f)
        )
        val insufficientTotal = 150f
        
        // Act
        val result = massCalculationService.distributeToVariableComponents(components, insufficientTotal)
        
        // Assert
        assertFalse("Distribution should fail", result.success)
        assertNotNull("Should have error message", result.errorMessage)
        assertTrue("Error should mention fixed component mass", 
            result.errorMessage!!.contains("200"))
    }

    @Test
    fun distributeToVariableComponents_noVariableComponents_success() {
        // Arrange: Only fixed components
        val components = listOf(
            createFixedComponent("core", "core", 33f),
            createFixedComponent("spool", "spool", 212f)
        )
        val exactTotal = 245f
        
        // Act
        val result = massCalculationService.distributeToVariableComponents(components, exactTotal)
        
        // Assert
        assertTrue("Should succeed with exact match", result.success)
        assertNull("Should not have error", result.errorMessage)
        
        // Act with different total
        val wrongTotal = 300f
        val result2 = massCalculationService.distributeToVariableComponents(components, wrongTotal)
        
        // Assert
        assertFalse("Should fail with wrong total", result2.success)
        assertNotNull("Should have error about no variable components", result2.errorMessage)
    }

    // ===== SINGLE COMPONENT UPDATE =====

    @Test
    fun updateSingleComponent_validUpdate_success() {
        // Arrange
        val components = listOf(
            createFilamentComponent("f1", 500f),
            createFixedComponent("core", "core", 33f)
        )
        val newMass = 750f
        
        // Act
        val result = massCalculationService.updateSingleComponent(
            components = components,
            componentId = "f1",
            newMass = newMass
        )
        
        // Assert
        assertTrue("Update should succeed", result.success)
        assertNull("Should not have error", result.errorMessage)
        
        val updatedComponent = result.updatedComponents.find { it.id == "f1" }!!
        assertEquals("Component mass should be updated", newMass, updatedComponent.massGrams ?: 0f)
        assertEquals("Total should be recalculated", 783f, result.newTotalMass)
    }

    @Test
    fun updateSingleComponent_withFullMass_updatesFullMass() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 500f, 1000f))
        val newMass = 600f
        val newFullMass = 1200f
        
        // Act
        val result = massCalculationService.updateSingleComponent(
            components = components,
            componentId = "f1",
            newMass = newMass,
            newFullMass = newFullMass
        )
        
        // Assert
        assertTrue("Update should succeed", result.success)
        
        val updatedComponent = result.updatedComponents.find { it.id == "f1" }!!
        assertEquals("Mass should be updated", newMass, updatedComponent.massGrams ?: 0f)
        assertEquals("Full mass should be updated", newFullMass, updatedComponent.fullMassGrams ?: 0f)
    }

    @Test
    fun updateSingleComponent_currentExceedsFullMass_fails() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 500f))
        val newMass = 1200f
        val newFullMass = 1000f
        
        // Act
        val result = massCalculationService.updateSingleComponent(
            components = components,
            componentId = "f1",
            newMass = newMass,
            newFullMass = newFullMass
        )
        
        // Assert
        assertFalse("Should fail when current > full", result.success)
        assertNotNull("Should have error message", result.errorMessage)
        assertTrue("Error should mention exceeds full mass", 
            result.errorMessage!!.contains("exceed full mass"))
    }

    @Test
    fun updateSingleComponent_negativeMass_fails() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 500f))
        
        // Act
        val result = massCalculationService.updateSingleComponent(
            components = components,
            componentId = "f1",
            newMass = -100f
        )
        
        // Assert
        assertFalse("Should fail with negative mass", result.success)
        assertTrue("Error should mention negative mass", 
            result.errorMessage!!.contains("negative"))
    }

    @Test
    fun updateSingleComponent_nonExistentComponent_fails() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 500f))
        
        // Act
        val result = massCalculationService.updateSingleComponent(
            components = components,
            componentId = "nonexistent",
            newMass = 100f
        )
        
        // Assert
        assertFalse("Should fail for non-existent component", result.success)
        assertTrue("Error should mention component not found", 
            result.errorMessage!!.contains("not found"))
    }

    // ===== COMPONENT VALIDATION =====

    @Test
    fun validateComponents_validCombination_passes() {
        // Arrange: One filament + fixed components
        val components = listOf(
            createFilamentComponent("f1", 500f),
            createFixedComponent("core", "core", 33f),
            createFixedComponent("spool", "spool", 212f)
        )
        
        // Act
        val result = massCalculationService.validateComponents(components)
        
        // Assert
        assertTrue("Valid combination should pass", result.valid)
        assertTrue("Should indicate valid", result.message.contains("valid"))
    }

    @Test
    fun validateComponents_emptyList_fails() {
        // Act
        val result = massCalculationService.validateComponents(emptyList())
        
        // Assert
        assertFalse("Empty list should fail", result.valid)
        assertTrue("Should mention no components", result.message.contains("No components"))
    }

    @Test
    fun validateComponents_noFilamentComponent_fails() {
        // Arrange: Only fixed components
        val components = listOf(
            createFixedComponent("core", "core", 33f),
            createFixedComponent("spool", "spool", 212f)
        )
        
        // Act
        val result = massCalculationService.validateComponents(components)
        
        // Assert
        assertFalse("Should fail without filament component", result.valid)
        assertTrue("Should mention no filament component", 
            result.message.contains("No filament component"))
    }

    @Test
    fun validateComponents_multipleFilamentComponents_fails() {
        // Arrange: Multiple filament components
        val components = listOf(
            createFilamentComponent("f1", 500f),
            createFilamentComponent("f2", 300f),
            createFixedComponent("core", "core", 33f)
        )
        
        // Act
        val result = massCalculationService.validateComponents(components)
        
        // Assert
        assertFalse("Should fail with multiple filament components", result.valid)
        assertTrue("Should mention multiple filament components", 
            result.message.contains("Multiple filament"))
    }

    // ===== MASS CONSTRAINT VALIDATION =====

    @Test
    fun validateMassConstraints_validComponents_passes() {
        // Arrange
        val components = listOf(
            createFilamentComponent("f1", 500f, 1000f),
            createFixedComponent("core", "core", 33f)
        )
        
        // Act
        val result = massCalculationService.validateMassConstraints(components)
        
        // Assert
        assertTrue("Valid constraints should pass", result.valid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }

    @Test
    fun validateMassConstraints_negativeMass_fails() {
        // Arrange
        val components = listOf(
            createFilamentComponent("f1", -100f),
            createFixedComponent("core", "core", 33f)
        )
        
        // Act
        val result = massCalculationService.validateMassConstraints(components)
        
        // Assert
        assertFalse("Should fail with negative mass", result.valid)
        assertTrue("Should have error for negative mass", 
            result.errors.any { it.contains("negative") })
    }

    @Test
    fun validateMassConstraints_negativeFullMass_fails() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 100f, -500f))
        
        // Act
        val result = massCalculationService.validateMassConstraints(components)
        
        // Assert
        assertFalse("Should fail with negative full mass", result.valid)
        assertTrue("Should have error for negative full mass", 
            result.errors.any { it.contains("Full mass") && it.contains("negative") })
    }

    @Test
    fun validateMassConstraints_currentExceedsFullMass_fails() {
        // Arrange
        val components = listOf(createFilamentComponent("f1", 1200f, 1000f))
        
        // Act
        val result = massCalculationService.validateMassConstraints(components)
        
        // Assert
        assertFalse("Should fail when current > full", result.valid)
        assertTrue("Should have error for exceeding full mass", 
            result.errors.any { it.contains("exceed full mass") })
    }

    // ===== WEIGHT FORMATTING =====

    @Test
    fun formatWeight_grams_correctFormat() {
        // Test various gram values
        assertEquals("500.0g", massCalculationService.formatWeight(500f, WeightUnit.GRAMS))
        assertEquals("1000.5g", massCalculationService.formatWeight(1000.5f, WeightUnit.GRAMS))
        assertEquals("0.0g", massCalculationService.formatWeight(0f, WeightUnit.GRAMS))
    }

    @Test
    fun formatWeight_otherUnits_correctConversion() {
        val gramsValue = 1000f
        
        // Test kilogram conversion (1000g = 1kg)
        assertEquals("1.0kg", massCalculationService.formatWeight(gramsValue, WeightUnit.KILOGRAMS))
        
        // Test ounce conversion (1000g ≈ 35.27 oz)
        val ouncesResult = massCalculationService.formatWeight(gramsValue, WeightUnit.OUNCES)
        assertTrue("Should convert to ounces", ouncesResult.endsWith("oz"))
        assertTrue("Should be around 35 oz", ouncesResult.startsWith("35."))
        
        // Test pound conversion (1000g ≈ 2.2 lbs)
        val poundsResult = massCalculationService.formatWeight(gramsValue, WeightUnit.POUNDS)
        assertTrue("Should convert to pounds", poundsResult.endsWith("lbs"))
        assertTrue("Should be around 2.2 lbs", poundsResult.startsWith("2."))
    }

    @Test
    fun formatWeight_decimals_correctPrecision() {
        // Test different decimal precision
        assertEquals("500.00g", massCalculationService.formatWeight(500f, WeightUnit.GRAMS, decimals = 2))
        assertEquals("500g", massCalculationService.formatWeight(500f, WeightUnit.GRAMS, decimals = 0))
    }

    // ===== COMPONENT CREATION HELPERS =====

    @Test
    fun createBambuComponentSetup_standardSetup_correctIds() {
        // Arrange
        val filamentComponent = createFilamentComponent("test-filament", 1000f)
        
        // Act
        val componentIds = massCalculationService.createBambuComponentSetup(filamentComponent, includeRefillableSpool = false)
        
        // Assert
        assertEquals("Should have filament + core", 2, componentIds.size)
        assertTrue("Should include filament component", componentIds.contains("test-filament"))
        assertTrue("Should include Bambu core", componentIds.contains("bambu_cardboard_core"))
        assertFalse("Should not include spool", componentIds.contains("bambu_refillable_spool"))
    }

    @Test
    fun createBambuComponentSetup_withRefillableSpool_includesSpool() {
        // Arrange
        val filamentComponent = createFilamentComponent("test-filament", 1000f)
        
        // Act
        val componentIds = massCalculationService.createBambuComponentSetup(filamentComponent, includeRefillableSpool = true)
        
        // Assert
        assertEquals("Should have filament + core + spool", 3, componentIds.size)
        assertTrue("Should include filament component", componentIds.contains("test-filament"))
        assertTrue("Should include Bambu core", componentIds.contains("bambu_cardboard_core"))
        assertTrue("Should include Bambu spool", componentIds.contains("bambu_refillable_spool"))
    }

    // ===== HELPER METHODS =====

    private fun createFilamentComponent(id: String, mass: Float, fullMass: Float? = null): Component {
        return Component(
            id = id,
            name = "Test Filament",
            category = "filament",
            massGrams = mass,
            fullMassGrams = fullMass,
            variableMass = true,
            manufacturer = "Test Manufacturer"
        )
    }

    private fun createFixedComponent(id: String, category: String, mass: Float): Component {
        return Component(
            id = id,
            name = "Test $category",
            category = category,
            massGrams = mass,
            variableMass = false,
            manufacturer = "Test Manufacturer"
        )
    }
}