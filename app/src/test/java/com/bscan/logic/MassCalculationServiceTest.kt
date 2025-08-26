package com.bscan.logic

import com.bscan.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive unit tests for MassCalculationService focusing on bidirectional mass updates.
 * Tests mathematical accuracy, edge cases, and validation for inventory mass management.
 */
class MassCalculationServiceTest {

    private lateinit var service: MassCalculationService
    private lateinit var testComponents: List<Component>

    @Before
    fun setUp() {
        service = MassCalculationService()
        
        // Create test components with realistic Bambu Lab setup
        testComponents = listOf(
            createFilamentComponent(id = "filament_pla", mass = 1000f, fullMass = 1000f), // Variable
            createCoreComponent(id = "bambu_core", mass = 30f), // Fixed
            createSpoolComponent(id = "bambu_spool", mass = 70f) // Fixed
        )
    }

    // === Test Mass Distribution to Variable Components ===

    @Test
    fun distributeToVariableComponents_proportionalDistribution_accurate() {
        // Setup: Multiple variable components with different masses
        val components = listOf(
            createFilamentComponent(id = "filament1", mass = 600f, fullMass = 1000f),
            createFilamentComponent(id = "filament2", mass = 400f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 100f)
        )
        val newTotalMass = 1200f // 100f fixed + 1100f variable
        
        val result = service.distributeToVariableComponents(components, newTotalMass)
        
        assertTrue("Distribution should succeed", result.success)
        assertNull("No error message expected", result.errorMessage)
        
        // Verify proportional distribution: 600:400 ratio should be maintained
        val variableComponents = result.updatedComponents.filter { it.variableMass }
        assertEquals("Should have 2 variable components", 2, variableComponents.size)
        
        val expectedFilament1 = 1100f * (600f / 1000f) // 660f
        val expectedFilament2 = 1100f * (400f / 1000f) // 440f
        
        assertEquals("Filament1 mass should be proportional", expectedFilament1, 
            variableComponents.find { it.id == "filament1" }?.massGrams ?: 0f, 0.01f)
        assertEquals("Filament2 mass should be proportional", expectedFilament2, 
            variableComponents.find { it.id == "filament2" }?.massGrams ?: 0f, 0.01f)
        
        // Verify fixed component unchanged
        val fixedComponent = result.updatedComponents.find { !it.variableMass }
        assertEquals("Fixed component should be unchanged", 100f, fixedComponent?.massGrams ?: 0f, 0.01f)
        
        // Verify total accuracy
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams.toDouble() }.toFloat()
        assertEquals("Total should match input", newTotalMass, calculatedTotal, 0.01f)
    }

    @Test
    fun distributeToVariableComponents_zeroVariableMass_equalDistribution() {
        // Test equal distribution when current variable mass is zero
        val components = listOf(
            createFilamentComponent(id = "filament1", mass = 0f, fullMass = 1000f),
            createFilamentComponent(id = "filament2", mass = 0f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 50f)
        )
        val newTotalMass = 550f
        
        val result = service.distributeToVariableComponents(components, newTotalMass)
        
        assertTrue("Distribution should succeed", result.success)
        
        val variableComponents = result.updatedComponents.filter { it.variableMass }
        val expectedPerComponent = (550f - 50f) / 2 // 250f each
        
        variableComponents.forEach { component ->
            assertEquals("Each variable component should get equal share", 
                expectedPerComponent, component.massGrams, 0.01f)
        }
    }

    @Test
    fun distributeToVariableComponents_insufficientMass_failure() {
        val newTotalMass = 50f // Less than fixed component mass (100f)
        
        val result = service.distributeToVariableComponents(testComponents, newTotalMass)
        
        assertFalse("Distribution should fail", result.success)
        assertNotNull("Should have error message", result.errorMessage)
        assertTrue("Error should mention fixed component mass", 
            result.errorMessage!!.contains("fixed component mass"))
        assertEquals("Components should be unchanged", testComponents, result.updatedComponents)
    }

    @Test
    fun distributeToVariableComponents_noVariableComponents_validation() {
        val fixedOnlyComponents = listOf(
            createCoreComponent(id = "core1", mass = 30f),
            createCoreComponent(id = "core2", mass = 70f)
        )
        
        // Test exact match
        val exactResult = service.distributeToVariableComponents(fixedOnlyComponents, 100f)
        assertTrue("Exact match should succeed", exactResult.success)
        assertNull("No error for exact match", exactResult.errorMessage)
        
        // Test mismatch
        val mismatchResult = service.distributeToVariableComponents(fixedOnlyComponents, 150f)
        assertFalse("Mismatch should fail", mismatchResult.success)
        assertNotNull("Should have error message", mismatchResult.errorMessage)
        assertTrue("Error should mention no variable components", 
            mismatchResult.errorMessage!!.contains("No variable components"))
    }

    // === Test Single Component Updates ===

    @Test
    fun updateSingleComponent_variableMass_bidirectionalUpdate() {
        val newMass = 750f
        val newFullMass = 1000f
        
        val result = service.updateSingleComponent(testComponents, "filament_pla", newMass, newFullMass)
        
        assertTrue("Update should succeed", result.success)
        assertNull("No error message expected", result.errorMessage)
        
        val updatedFilament = result.updatedComponents.find { it.id == "filament_pla" }
        assertNotNull("Filament component should exist", updatedFilament)
        assertEquals("Mass should be updated", newMass, updatedFilament!!.massGrams, 0.01f)
        assertEquals("Full mass should be updated", newFullMass, updatedFilament.fullMassGrams ?: 0f, 0.01f)
        
        // Verify total recalculation
        val expectedTotal = 750f + 30f + 70f // Updated filament + fixed components
        assertEquals("Total should be recalculated", expectedTotal, result.newTotalMass, 0.01f)
        
        // Verify other components unchanged
        val coreComponent = result.updatedComponents.find { it.id == "bambu_core" }
        assertEquals("Core mass should be unchanged", 30f, coreComponent?.massGrams ?: 0f, 0.01f)
    }

    @Test
    fun updateSingleComponent_fixedMass_noFullMassUpdate() {
        val newMass = 35f
        
        val result = service.updateSingleComponent(testComponents, "bambu_core", newMass)
        
        assertTrue("Update should succeed", result.success)
        
        val updatedCore = result.updatedComponents.find { it.id == "bambu_core" }
        assertEquals("Core mass should be updated", newMass, updatedCore?.massGrams ?: 0f, 0.01f)
        assertNull("Fixed component should not have full mass", updatedCore?.fullMassGrams)
        
        val expectedTotal = 1000f + 35f + 70f
        assertEquals("Total should reflect fixed component change", expectedTotal, result.newTotalMass, 0.01f)
    }

    @Test
    fun updateSingleComponent_invalidConstraints_validation() {
        // Test negative mass
        val negativeResult = service.updateSingleComponent(testComponents, "filament_pla", -100f)
        assertFalse("Negative mass should fail", negativeResult.success)
        assertTrue("Error should mention negative mass", 
            negativeResult.errorMessage!!.contains("cannot be negative"))
        
        // Test current mass exceeding full mass
        val excessResult = service.updateSingleComponent(testComponents, "filament_pla", 1200f, 1000f)
        assertFalse("Excess mass should fail", excessResult.success)
        assertTrue("Error should mention full mass constraint", 
            excessResult.errorMessage!!.contains("cannot exceed full mass"))
        
        // Test non-existent component
        val missingResult = service.updateSingleComponent(testComponents, "nonexistent", 100f)
        assertFalse("Missing component should fail", missingResult.success)
        assertTrue("Error should mention component not found", 
            missingResult.errorMessage!!.contains("not found"))
    }

    // === Test Mathematical Accuracy ===

    @Test
    fun mathematicalAccuracy_verySmallMasses_precision() {
        val components = listOf(
            createFilamentComponent(id = "filament", mass = 0.1f, fullMass = 100f),
            createCoreComponent(id = "core", mass = 0.05f)
        )
        val newTotalMass = 50.25f
        
        val result = service.distributeToVariableComponents(components, newTotalMass)
        assertTrue("Should handle small masses", result.success)
        
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams.toDouble() }.toFloat()
        assertEquals("Small mass precision should be maintained", newTotalMass, calculatedTotal, 0.001f)
    }

    @Test
    fun mathematicalAccuracy_largeMasses_noOverflow() {
        val components = listOf(
            createFilamentComponent(id = "filament", mass = 50000f, fullMass = 100000f),
            createCoreComponent(id = "core", mass = 1000f)
        )
        val newTotalMass = 75000f
        
        val result = service.distributeToVariableComponents(components, newTotalMass)
        assertTrue("Should handle large masses", result.success)
        
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams.toDouble() }.toFloat()
        assertEquals("Large mass accuracy should be maintained", newTotalMass, calculatedTotal, 0.1f)
    }

    @Test
    fun mathematicalAccuracy_repeatedOperations_consistentResults() {
        var currentComponents = testComponents
        val targetTotals = listOf(1200f, 900f, 1500f, 800f, 1100f)
        
        for (targetTotal in targetTotals) {
            val result = service.distributeToVariableComponents(currentComponents, targetTotal)
            assertTrue("Each operation should succeed for total $targetTotal", result.success)
            
            val calculatedTotal = result.updatedComponents.sumOf { it.massGrams.toDouble() }.toFloat()
            assertEquals("Total should match target for $targetTotal", targetTotal, calculatedTotal, 0.01f)
            
            currentComponents = result.updatedComponents
        }
        
        // Final verification
        val finalTotal = currentComponents.sumOf { it.massGrams.toDouble() }.toFloat()
        assertEquals("Final total should match last operation", 1100f, finalTotal, 0.01f)
    }

    // === Test Validation Methods ===

    @Test
    fun validateMassConstraints_comprehensiveValidation() {
        val invalidComponents = listOf(
            createFilamentComponent(id = "negative", mass = -10f, fullMass = 1000f),
            createFilamentComponent(id = "exceeds_full", mass = 1200f, fullMass = 1000f),
            createFilamentComponent(id = "negative_full", mass = 500f, fullMass = -100f),
            createCoreComponent(id = "valid", mass = 30f)
        )
        
        val validation = service.validateMassConstraints(invalidComponents)
        
        assertFalse("Validation should fail", validation.valid)
        assertEquals("Should have 4 errors", 4, validation.errors.size)
        
        assertTrue("Should detect negative mass", 
            validation.errors.any { it.contains("negative") && it.contains("Mass") })
        assertTrue("Should detect mass exceeding full mass", 
            validation.errors.any { it.contains("exceed full mass") })
        assertTrue("Should detect negative full mass", 
            validation.errors.any { it.contains("Full mass") && it.contains("negative") })
    }

    @Test
    fun validateComponents_componentCombination() {
        // Test valid setup
        val validComponents = testComponents
        val validResult = service.validateComponents(validComponents)
        assertTrue("Valid setup should pass", validResult.valid)
        
        // Test no components
        val emptyResult = service.validateComponents(emptyList())
        assertFalse("Empty list should fail", emptyResult.valid)
        assertTrue("Should mention no components", emptyResult.message.contains("No components"))
        
        // Test no variable components
        val fixedOnlyComponents = listOf(
            createCoreComponent(id = "core1", mass = 30f),
            createCoreComponent(id = "core2", mass = 70f)
        )
        val noVariableResult = service.validateComponents(fixedOnlyComponents)
        assertFalse("No variable components should fail", noVariableResult.valid)
        assertTrue("Should mention no filament component", 
            noVariableResult.message.contains("No filament component"))
        
        // Test multiple variable components
        val multiVariableComponents = listOf(
            createFilamentComponent(id = "filament1", mass = 500f, fullMass = 1000f),
            createFilamentComponent(id = "filament2", mass = 500f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 30f)
        )
        val multiVariableResult = service.validateComponents(multiVariableComponents)
        assertFalse("Multiple variable components should fail", multiVariableResult.valid)
        assertTrue("Should mention multiple filament components", 
            multiVariableResult.message.contains("Multiple filament components"))
    }

    // === Test Utility Methods ===

    @Test
    fun calculateTotalFromComponents_accuracy() {
        val total = service.calculateTotalFromComponents(testComponents)
        val expectedTotal = 1000f + 30f + 70f // 1100f
        assertEquals("Total calculation should be accurate", expectedTotal, total, 0.01f)
        
        // Test with empty list
        val emptyTotal = service.calculateTotalFromComponents(emptyList())
        assertEquals("Empty list should return 0", 0f, emptyTotal, 0.01f)
    }

    @Test
    fun formatWeight_unitConversions() {
        val testMass = 1000f // 1kg
        
        assertEquals("Grams formatting", "1000.0g", service.formatWeight(testMass, WeightUnit.GRAMS))
        assertEquals("Kilograms formatting", "1.0kg", service.formatWeight(testMass, WeightUnit.KILOGRAMS))
        assertEquals("Pounds formatting", "2.2lbs", service.formatWeight(testMass, WeightUnit.POUNDS, 1))
        assertEquals("Ounces formatting", "35.3oz", service.formatWeight(testMass, WeightUnit.OUNCES, 1))
        
        // Test decimal places
        assertEquals("Two decimal places", "1000.00g", 
            service.formatWeight(testMass, WeightUnit.GRAMS, 2))
        assertEquals("No decimal places", "1000g", 
            service.formatWeight(testMass, WeightUnit.GRAMS, 0))
    }

    // === Edge Cases and Error Handling ===

    @Test
    fun edgeCases_extremeValues() {
        // Test with Float.MAX_VALUE
        val extremeComponents = listOf(
            createFilamentComponent(id = "extreme", mass = Float.MAX_VALUE / 2, fullMass = Float.MAX_VALUE)
        )
        
        val result = service.calculateTotalFromComponents(extremeComponents)
        assertTrue("Should handle extreme values", result.isFinite())
        
        // Test with very small values
        val tinyComponents = listOf(
            createFilamentComponent(id = "tiny", mass = Float.MIN_VALUE, fullMass = 1f)
        )
        
        val tinyResult = service.calculateTotalFromComponents(tinyComponents)
        assertTrue("Should handle tiny values", tinyResult >= 0f)
    }

    @Test
    fun edgeCases_boundaryConditions() {
        // Test zero total mass
        val zeroResult = service.distributeToVariableComponents(testComponents, 0f)
        assertFalse("Zero total should fail (less than fixed mass)", zeroResult.success)
        
        // Test exact fixed mass - should succeed with zero variable mass (empty spool)
        val fixedMass = testComponents.filter { !it.variableMass }.sumOf { it.massGrams.toDouble() }.toFloat()
        val exactResult = service.distributeToVariableComponents(testComponents, fixedMass)
        assertTrue("Should succeed with zero variable mass (empty spool)", exactResult.success)
        
        val variableComponent = exactResult.updatedComponents.find { it.variableMass }
        assertEquals("Variable component should have zero mass", 0f, 
            variableComponent?.massGrams ?: -1f, 0.001f)
        
        // Test slightly above fixed mass
        val slightlyAboveResult = service.distributeToVariableComponents(testComponents, fixedMass + 0.01f)
        assertTrue("Should succeed with minimal variable mass", slightlyAboveResult.success)
        
        val slightlyAboveVariableComponent = slightlyAboveResult.updatedComponents.find { it.variableMass }
        assertEquals("Variable component should get remaining mass", 0.01f, 
            slightlyAboveVariableComponent?.massGrams ?: 0f, 0.001f)
    }

    // === Helper Methods ===

    private fun createFilamentComponent(id: String, mass: Float, fullMass: Float): Component {
        return Component(
            id = id,
            name = "Filament Component",
            category = "filament",
            massGrams = mass,
            variableMass = true,
            manufacturer = "Bambu Lab",
            description = "Test filament component",
            fullMassGrams = fullMass
        )
    }

    private fun createCoreComponent(id: String, mass: Float): Component {
        return Component(
            id = id,
            name = "Core Component",
            category = "core",
            massGrams = mass,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Test core component",
            fullMassGrams = null
        )
    }

    private fun Component.withMassUpdate(newMass: Float): Component {
        return if (variableMass) {
            withUpdatedMass(newMass)
        } else {
            copy(massGrams = newMass)
        }
    }

    private fun createSpoolComponent(id: String, mass: Float): Component {
        return Component(
            id = id,
            name = "Spool Component",
            category = "spool",
            massGrams = mass,
            variableMass = false,
            manufacturer = "Bambu Lab",
            description = "Test spool component",
            fullMassGrams = null
        )
    }
}