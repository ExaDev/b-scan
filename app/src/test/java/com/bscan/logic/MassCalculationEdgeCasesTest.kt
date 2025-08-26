package com.bscan.logic

import com.bscan.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Comprehensive edge case and mathematical accuracy tests for mass calculations.
 * Tests boundary conditions, floating-point precision, extreme values, and error scenarios.
 */
class MassCalculationEdgeCasesTest {

    private lateinit var service: MassCalculationService
    
    companion object {
        private const val FLOAT_TOLERANCE = 0.01f
        private const val PRECISION_TOLERANCE = 0.001f
    }

    @Before
    fun setUp() {
        service = MassCalculationService()
    }

    // === Test Floating-Point Precision ===

    @Test
    fun floatingPointPrecision_repeatedDivisionMultiplication_accuracy() {
        val components = listOf(
            createFilamentComponent(id = "fil1", mass = 333.33f, fullMass = 1000f),
            createFilamentComponent(id = "fil2", mass = 333.33f, fullMass = 1000f),
            createFilamentComponent(id = "fil3", mass = 333.34f, fullMass = 1000f) // Slightly different to sum to 1000
        )
        
        // Test repeated operations that might accumulate floating-point errors
        var currentComponents = components
        val operations = listOf(1500f, 1000f, 750f, 1250f, 1000f)
        
        operations.forEach { targetTotal ->
            val result = service.distributeToVariableComponents(currentComponents, targetTotal)
            assertTrue("Operation should succeed for $targetTotal", result.success)
            
            val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
            assertEquals("Total should be accurate for $targetTotal", 
                targetTotal, calculatedTotal, PRECISION_TOLERANCE)
            
            currentComponents = result.updatedComponents
        }
        
        // Final verification - should still be mathematically consistent
        val finalSum = currentComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Final sum should match last operation", operations.last(), finalSum, PRECISION_TOLERANCE)
    }

    @Test
    fun floatingPointPrecision_verySmallIncrements_maintained() {
        val components = listOf(
            createFilamentComponent(id = "fil", mass = 100.001f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 0.999f)
        )
        
        // Test very small increment
        val newTotal = 101.0005f
        val result = service.distributeToVariableComponents(components, newTotal)
        
        assertTrue("Should handle small increments", result.success)
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Small increment precision should be maintained", 
            newTotal, calculatedTotal, 0.0001f)
    }

    @Test
    fun floatingPointPrecision_exactDecimalRepresentation_preserved() {
        // Test values that have exact decimal representation
        val exactComponents = listOf(
            createFilamentComponent(id = "exact", mass = 250.0f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 50.0f)
        )
        
        val exactTotal = 400.0f
        val result = service.distributeToVariableComponents(exactComponents, exactTotal)
        
        assertTrue("Exact decimal should succeed", result.success)
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Exact decimal should be preserved", exactTotal, calculatedTotal, 0.0f)
    }

    // === Test Extreme Values ===

    @Test
    fun extremeValues_veryLargeMasses_handled() {
        val largeComponents = listOf(
            createFilamentComponent(id = "large", mass = Float.MAX_VALUE / 4, fullMass = Float.MAX_VALUE / 2),
            createCoreComponent(id = "core", mass = 1000f)
        )
        
        val largeTotal = Float.MAX_VALUE / 3
        val result = service.distributeToVariableComponents(largeComponents, largeTotal)
        
        // Should either succeed or fail gracefully
        if (result.success) {
            val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
            assertTrue("Large total should be finite", calculatedTotal.isFinite())
            
            // Allow larger tolerance for extreme values
            val relativeTolerance = largeTotal * 0.001f
            assertEquals("Large mass should be approximately correct", 
                largeTotal, calculatedTotal, relativeTolerance)
        } else {
            assertNotNull("Should have error message for large values", result.errorMessage)
        }
    }

    @Test
    fun extremeValues_verySmallMasses_precision() {
        val tinyComponents = listOf(
            createFilamentComponent(id = "tiny", mass = Float.MIN_VALUE, fullMass = 1f),
            createCoreComponent(id = "core", mass = Float.MIN_VALUE)
        )
        
        val tinyTotal = Float.MIN_VALUE * 3
        val result = service.distributeToVariableComponents(tinyComponents, tinyTotal)
        
        assertTrue("Should handle tiny masses", result.success)
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        
        assertTrue("Tiny total should be positive", calculatedTotal >= 0f)
        assertTrue("Tiny total should be finite", calculatedTotal.isFinite())
    }

    @Test
    fun extremeValues_zeroMasses_boundary() {
        val zeroComponents = listOf(
            createFilamentComponent(id = "zero", mass = 0f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 0f)
        )
        
        // Test various zero scenarios - when total equals fixed (both 0), it should succeed
        val zeroResult = service.distributeToVariableComponents(zeroComponents, 0f)
        assertTrue("Zero total should succeed when fixed components also zero", zeroResult.success)
        
        val zeroVariableComponent = zeroResult.updatedComponents.find { it.variableMass }
        assertEquals("Variable component should remain zero", 
            0f, zeroVariableComponent?.massGrams ?: -1f, PRECISION_TOLERANCE)
        
        val positiveResult = service.distributeToVariableComponents(zeroComponents, 100f)
        assertTrue("Positive total with zero components should succeed", positiveResult.success)
        
        val variableComponent = positiveResult.updatedComponents.find { it.variableMass }
        assertEquals("All mass should go to variable component", 
            100f, variableComponent?.massGrams ?: 0f, PRECISION_TOLERANCE)
    }

    // === Test Boundary Conditions ===

    @Test
    fun boundaryConditions_exactFixedMass_edge() {
        val components = listOf(
            createFilamentComponent(id = "fil", mass = 500f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 200f),
            createCoreComponent(id = "spool", mass = 150f)
        )
        
        val fixedMass = 200f + 150f // Exactly the fixed component mass
        
        // Should succeed with zero mass for variable components (empty spool scenario)
        val exactResult = service.distributeToVariableComponents(components, fixedMass)
        assertTrue("Exact fixed mass should succeed (empty spool)", exactResult.success)
        
        val exactVariableComponent = exactResult.updatedComponents.find { it.variableMass }
        assertEquals("Variable component should have zero mass", 
            0f, exactVariableComponent?.massGrams ?: -1f, PRECISION_TOLERANCE)
        
        // Should succeed with minimal additional mass
        val slightlyMoreResult = service.distributeToVariableComponents(components, fixedMass + 0.01f)
        assertTrue("Slightly more than fixed should succeed", slightlyMoreResult.success)
        
        val variableComponent = slightlyMoreResult.updatedComponents.find { it.variableMass }
        assertEquals("Variable component should get minimal mass", 
            0.01f, variableComponent?.massGrams ?: 0f, PRECISION_TOLERANCE)
    }

    @Test
    fun boundaryConditions_singleComponentUpdate_extremes() {
        val components = listOf(
            createFilamentComponent(id = "fil", mass = 500f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 100f)
        )
        
        // Test updating to exactly zero
        val zeroResult = service.updateSingleComponent(components, "fil", 0f, 1000f)
        assertTrue("Zero mass update should succeed", zeroResult.success)
        assertEquals("Total should reflect zero filament", 100f, zeroResult.newTotalMass, PRECISION_TOLERANCE)
        
        // Test updating to exactly full mass
        val fullResult = service.updateSingleComponent(components, "fil", 1000f, 1000f)
        assertTrue("Full mass update should succeed", fullResult.success)
        assertEquals("Total should reflect full filament", 1100f, fullResult.newTotalMass, PRECISION_TOLERANCE)
        
        // Test updating beyond full mass (should fail)
        val excessResult = service.updateSingleComponent(components, "fil", 1001f, 1000f)
        assertFalse("Excess mass should fail", excessResult.success)
        assertTrue("Should mention full mass constraint", 
            excessResult.errorMessage!!.contains("exceed full mass"))
    }

    // === Test Proportional Distribution Accuracy ===

    @Test
    fun proportionalDistribution_complexRatios_accuracy() {
        val components = listOf(
            createFilamentComponent(id = "fil1", mass = 123.456f, fullMass = 1000f),
            createFilamentComponent(id = "fil2", mass = 654.321f, fullMass = 1000f),
            createFilamentComponent(id = "fil3", mass = 222.223f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 50f)
        )
        
        val newTotal = 1500f
        val availableForVariable = newTotal - 50f // 1450f
        
        val result = service.distributeToVariableComponents(components, newTotal)
        assertTrue("Complex ratio distribution should succeed", result.success)
        
        // Verify proportions are maintained
        val originalVariableTotal = 123.456f + 654.321f + 222.223f
        val variableComponents = result.updatedComponents.filter { it.variableMass }
        
        variableComponents.forEach { component ->
            val originalComponent = components.find { it.id == component.id }!!
            val expectedRatio = (originalComponent.massGrams ?: 0f) / originalVariableTotal
            val expectedNewMass = availableForVariable * expectedRatio
            
            assertEquals("Component ${component.id} should maintain proportion", 
                expectedNewMass, component.massGrams ?: 0f, 0.01f)
        }
        
        // Verify total is exact
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Total should be exact with complex ratios", newTotal, calculatedTotal, PRECISION_TOLERANCE)
    }

    @Test
    fun proportionalDistribution_repeatedOperations_consistency() {
        val initialComponents = listOf(
            createFilamentComponent(id = "fil", mass = 600f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 100f)
        )
        
        // Perform scale up and scale down operations
        var currentComponents = initialComponents
        val operations = listOf(1400f, 700f, 1750f, 1050f, 1200f, 800f)
        
        operations.forEach { targetTotal ->
            val result = service.distributeToVariableComponents(currentComponents, targetTotal)
            assertTrue("Each operation should succeed for $targetTotal", result.success)
            
            currentComponents = result.updatedComponents
            val calculatedTotal = currentComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
            assertEquals("Total should be accurate for $targetTotal", 
                targetTotal, calculatedTotal, PRECISION_TOLERANCE)
        }
        
        // Final state should be mathematically sound
        val finalTotal = currentComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Final total should match last operation", 800f, finalTotal, PRECISION_TOLERANCE)
        
        // Component masses should be reasonable
        currentComponents.forEach { component ->
            assertTrue("Component ${component.id} should have non-negative mass", 
                (component.massGrams ?: 0f) >= 0f)
            assertTrue("Component ${component.id} should have finite mass", 
                (component.massGrams ?: 0f).isFinite())
        }
    }

    // === Test Rounding and Precision Edge Cases ===

    @Test
    fun roundingPrecision_manySmallComponents_accuracy() {
        // Create many components with small masses that might cause rounding errors
        val manyComponents = (1..100).map { index ->
            createFilamentComponent(id = "fil_$index", mass = 1.01f, fullMass = 10f)
        } + listOf(createCoreComponent(id = "core", mass = 5f))
        
        val newTotal = 500f
        val result = service.distributeToVariableComponents(manyComponents, newTotal)
        
        assertTrue("Many small components should succeed", result.success)
        
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Total should be accurate with many components", 
            newTotal, calculatedTotal, PRECISION_TOLERANCE)
        
        // Verify each component got a reasonable share
        val variableComponents = result.updatedComponents.filter { it.variableMass }
        val averageMass = (newTotal - 5f) / variableComponents.size
        
        variableComponents.forEach { component ->
            assertTrue("Component should have positive mass", (component.massGrams ?: 0f) > 0f)
            assertTrue("Component mass should be reasonable", 
                abs((component.massGrams ?: 0f) - averageMass) < averageMass)
        }
    }

    @Test
    fun roundingPrecision_fractionalDistribution_handled() {
        // Test case where exact division is impossible
        val components = listOf(
            createFilamentComponent(id = "fil1", mass = 1f, fullMass = 100f),
            createFilamentComponent(id = "fil2", mass = 1f, fullMass = 100f),
            createFilamentComponent(id = "fil3", mass = 1f, fullMass = 100f),
            createCoreComponent(id = "core", mass = 1f)
        )
        
        val indivisibleTotal = 11f // 10f available for variable, not evenly divisible by 3
        val result = service.distributeToVariableComponents(components, indivisibleTotal)
        
        assertTrue("Fractional distribution should succeed", result.success)
        
        val calculatedTotal = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Total should be maintained despite fractional distribution", 
            indivisibleTotal, calculatedTotal, PRECISION_TOLERANCE)
        
        // Verify the sum of variable components is correct
        val variableMass = result.updatedComponents.filter { it.variableMass }
            .sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
        assertEquals("Variable mass should total 10f", 10f, variableMass, PRECISION_TOLERANCE)
    }

    // === Test Error Accumulation ===

    @Test
    fun errorAccumulation_manySequentialOperations_bounds() {
        var components = listOf(
            createFilamentComponent(id = "fil", mass = 1000f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 100f)
        )
        
        // Perform many sequential operations to test error accumulation
        val baseTotal = 1100f
        repeat(1000) { iteration ->
            val variance = (iteration % 100) - 50 // ±50 variation
            val targetTotal = baseTotal + variance
            
            val result = service.distributeToVariableComponents(components, targetTotal)
            assertTrue("Operation $iteration should succeed", result.success)
            
            components = result.updatedComponents
            val calculatedTotal = components.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
            
            assertEquals("Total should be accurate after $iteration operations", 
                targetTotal, calculatedTotal, PRECISION_TOLERANCE * 2) // Allow slightly more tolerance for accumulated operations
        }
        
        // Final verification
        val finalComponent = components.find { it.variableMass }
        assertNotNull("Variable component should exist", finalComponent)
        assertTrue("Final component mass should be reasonable", 
            (finalComponent!!.massGrams ?: 0f) in 950f..1150f) // Should be close to 1050f ± variance
    }

    // === Test Mathematical Invariants ===

    @Test
    fun mathematicalInvariants_massConservation_maintained() {
        val components = listOf(
            createFilamentComponent(id = "fil1", mass = 300f, fullMass = 1000f),
            createFilamentComponent(id = "fil2", mass = 700f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 150f)
        )
        
        val operations = listOf(2000f, 500f, 1750f, 825f, 1200f)
        
        operations.forEach { targetTotal ->
            val result = service.distributeToVariableComponents(components, targetTotal)
            assertTrue("Operation should succeed for total $targetTotal", result.success)
            
            // Test mass conservation
            val totalMass = result.updatedComponents.sumOf { it.massGrams?.toDouble() ?: 0.0 }.toFloat()
            assertEquals("Mass should be conserved for $targetTotal", 
                targetTotal, totalMass, PRECISION_TOLERANCE)
            
            // Test component type preservation
            val variableCount = result.updatedComponents.count { it.variableMass }
            val fixedCount = result.updatedComponents.count { !it.variableMass }
            assertEquals("Variable component count should be preserved", 2, variableCount)
            assertEquals("Fixed component count should be preserved", 1, fixedCount)
            
            // Test non-negativity
            result.updatedComponents.forEach { component ->
                assertTrue("Component ${component.id} mass should be non-negative", 
                    (component.massGrams ?: 0f) >= 0f)
            }
        }
    }

    @Test
    fun mathematicalInvariants_idempotency_singleComponentUpdate() {
        val components = listOf(
            createFilamentComponent(id = "fil", mass = 600f, fullMass = 1000f),
            createCoreComponent(id = "core", mass = 50f)
        )
        
        // Update component to same value - should be idempotent
        val result1 = service.updateSingleComponent(components, "fil", 600f, 1000f)
        assertTrue("First update should succeed", result1.success)
        
        val result2 = service.updateSingleComponent(result1.updatedComponents, "fil", 600f, 1000f)
        assertTrue("Second update should succeed", result2.success)
        
        // Results should be identical
        assertEquals("Total should be same", result1.newTotalMass, result2.newTotalMass, PRECISION_TOLERANCE)
        
        val component1 = result1.updatedComponents.find { it.id == "fil" }!!
        val component2 = result2.updatedComponents.find { it.id == "fil" }!!
        
        assertEquals("Component mass should be same", 
            component1.massGrams ?: 0f, component2.massGrams ?: 0f, PRECISION_TOLERANCE)
        assertEquals("Component full mass should be same", 
            component1.fullMassGrams ?: 0f, component2.fullMassGrams ?: 0f, PRECISION_TOLERANCE)
    }

    // === Helper Methods ===

    private fun createFilamentComponent(id: String, mass: Float, fullMass: Float): Component {
        return Component(
            id = id,
            name = "Test Filament $id",
            category = "filament",
            massGrams = mass,
            variableMass = true,
            manufacturer = "Test Lab",
            description = "Test filament component",
            fullMassGrams = fullMass
        )
    }

    private fun createCoreComponent(id: String, mass: Float): Component {
        return Component(
            id = id,
            name = "Test Core $id",
            category = "core",
            massGrams = mass,
            variableMass = false,
            manufacturer = "Test Lab",
            description = "Test core component",
            fullMassGrams = null
        )
    }
}