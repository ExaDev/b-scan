package com.bscan.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class UnitConversionServiceTest {

    private lateinit var unitConversionService: UnitConversionService

    @Before
    fun setUp() {
        unitConversionService = UnitConversionService()
    }

    // Weight Conversion Tests

    @Test
    fun `convertWeight should convert grams to kilograms correctly`() {
        val result = unitConversionService.convertWeight(1500f, WeightUnit.GRAMS, WeightUnit.KILOGRAMS)
        assertEquals(1.5f, result, 0.001f)
    }

    @Test
    fun `convertWeight should convert kilograms to grams correctly`() {
        val result = unitConversionService.convertWeight(2.5f, WeightUnit.KILOGRAMS, WeightUnit.GRAMS)
        assertEquals(2500f, result, 0.001f)
    }

    @Test
    fun `convertWeight should convert grams to ounces correctly`() {
        val result = unitConversionService.convertWeight(100f, WeightUnit.GRAMS, WeightUnit.OUNCES)
        assertEquals(3.527f, result, 0.01f)
    }

    @Test
    fun `convertWeight should convert pounds to grams correctly`() {
        val result = unitConversionService.convertWeight(2.2f, WeightUnit.POUNDS, WeightUnit.GRAMS)
        assertEquals(997.9f, result, 1f)
    }

    @Test
    fun `convertWeight should handle same unit conversion`() {
        val result = unitConversionService.convertWeight(100f, WeightUnit.GRAMS, WeightUnit.GRAMS)
        assertEquals(100f, result, 0.001f)
    }

    // Volume Conversion Tests

    @Test
    fun `convertVolume should convert millilitres to litres correctly`() {
        val result = unitConversionService.convertVolume(2500f, VolumeUnit.MILLILITRES, VolumeUnit.LITRES)
        assertEquals(2.5f, result, 0.001f)
    }

    @Test
    fun `convertVolume should convert cubic centimetres to cubic metres correctly`() {
        val result = unitConversionService.convertVolume(1000000f, VolumeUnit.CUBIC_CENTIMETRES, VolumeUnit.CUBIC_METRES)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `convertVolume should handle same unit conversion`() {
        val result = unitConversionService.convertVolume(100f, VolumeUnit.LITRES, VolumeUnit.LITRES)
        assertEquals(100f, result, 0.001f)
    }

    // Length Conversion Tests

    @Test
    fun `convertLength should convert millimetres to metres correctly`() {
        val result = unitConversionService.convertLength(1750f, LengthUnit.MILLIMETRES, LengthUnit.METRES)
        assertEquals(1.75f, result, 0.001f)
    }

    @Test
    fun `convertLength should convert inches to centimetres correctly`() {
        val result = unitConversionService.convertLength(4f, LengthUnit.INCHES, LengthUnit.CENTIMETRES)
        assertEquals(10.16f, result, 0.01f)
    }

    @Test
    fun `convertLength should convert feet to metres correctly`() {
        val result = unitConversionService.convertLength(6.56f, LengthUnit.FEET, LengthUnit.METRES)
        assertEquals(2f, result, 0.01f)
    }

    // Edge Case Tests

    @Test
    fun `conversion should handle very small values`() {
        val result = unitConversionService.convertWeight(0.001f, WeightUnit.GRAMS, WeightUnit.KILOGRAMS)
        assertEquals(0.000001f, result, 0.0000001f)
    }

    @Test
    fun `conversion should handle very large values`() {
        val result = unitConversionService.convertWeight(1000000f, WeightUnit.GRAMS, WeightUnit.KILOGRAMS)
        assertEquals(1000f, result, 0.001f)
    }

    @Test
    fun `conversion should maintain precision for round-trip conversions`() {
        val original = 1.234f
        
        // Convert grams -> kg -> grams
        val toKg = unitConversionService.convertWeight(original, WeightUnit.GRAMS, WeightUnit.KILOGRAMS)
        val backToGrams = unitConversionService.convertWeight(toKg, WeightUnit.KILOGRAMS, WeightUnit.GRAMS)
        
        assertEquals(original, backToGrams, 0.001f)
    }

    // Enum Symbol Tests

    @Test
    fun `weight unit symbols should be correct`() {
        assertEquals("g", WeightUnit.GRAMS.symbol)
        assertEquals("kg", WeightUnit.KILOGRAMS.symbol)
        assertEquals("oz", WeightUnit.OUNCES.symbol)
        assertEquals("lb", WeightUnit.POUNDS.symbol)
        assertEquals("mg", WeightUnit.MILLIGRAMS.symbol)
    }

    @Test
    fun `volume unit symbols should be correct`() {
        assertEquals("ml", VolumeUnit.MILLILITRES.symbol)
        assertEquals("l", VolumeUnit.LITRES.symbol)
        assertEquals("cm³", VolumeUnit.CUBIC_CENTIMETRES.symbol)
        assertEquals("m³", VolumeUnit.CUBIC_METRES.symbol)
    }

    @Test
    fun `length unit symbols should be correct`() {
        assertEquals("m", LengthUnit.METRES.symbol)
        assertEquals("cm", LengthUnit.CENTIMETRES.symbol)
        assertEquals("mm", LengthUnit.MILLIMETRES.symbol)
        assertEquals("in", LengthUnit.INCHES.symbol)
        assertEquals("ft", LengthUnit.FEET.symbol)
    }

    // Helper method for floating point comparison
    private fun Float.assertEquals(expected: Float, tolerance: Float) {
        assertTrue("Expected $expected but was $this", abs(this - expected) < tolerance)
    }
}