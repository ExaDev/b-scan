package com.bscan.utils

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

/**
 * Tests for ColorUtils functions covering the fixes in commits:
 * - 4c21797: Color hex name formatting fix 
 * - a261484: RGBA to AARRGGBB conversion fix
 */
class ColorUtilsTest {

    @Test
    fun `formatColorHexForDisplay handles hash prefix correctly`() {
        // Test cases for commit 4c21797 fix
        val testCases = mapOf(
            "FF0000" to "#FF0000",
            "#FF0000" to "#FF0000", 
            "##FF0000" to "#FF0000", // Multiple hashes should be cleaned
            "#FF0000FF" to "#FF0000", // Should take only first 6 chars
            "ff0000" to "#ff0000", // Preserve case
            "#" to "#",
            "" to "#",
            "   " to "#" // Whitespace only
        )
        
        testCases.forEach { (input, expected) ->
            val result = ColorUtils.formatColorHexForDisplay(input)
            assertEquals("Input '$input' should format to '$expected'", expected, result)
        }
    }

    @Test
    fun `parseColorHex handles 6-character RGB format correctly`() {
        val testCases = listOf(
            "FF0000" to Triple(1.0f, 0.0f, 0.0f), // Red
            "#00FF00" to Triple(0.0f, 1.0f, 0.0f), // Green  
            "0000FF" to Triple(0.0f, 0.0f, 1.0f), // Blue
            "#FFFFFF" to Triple(1.0f, 1.0f, 1.0f), // White
            "000000" to Triple(0.0f, 0.0f, 0.0f) // Black
        )
        
        testCases.forEach { (input, expected) ->
            val color = ColorUtils.parseColorHex(input)
            assertEquals("$input red component", expected.first, color.red, 0.001f)
            assertEquals("$input green component", expected.second, color.green, 0.001f) 
            assertEquals("$input blue component", expected.third, color.blue, 0.001f)
            assertEquals("$input should have full alpha", 1.0f, color.alpha, 0.001f)
        }
    }

    @Test
    fun `parseColorHex converts RGBA to AARRGGBB correctly`() {
        // Test the core fix from commit a261484
        val testCases = listOf(
            // RGBA input -> Expected RGBA components
            "FF000080" to Quad(1.0f, 0.0f, 0.0f, 0.502f), // Red 50% alpha
            "00FF0040" to Quad(0.0f, 1.0f, 0.0f, 0.251f), // Green 25% alpha
            "0000FFFF" to Quad(0.0f, 0.0f, 1.0f, 1.0f), // Blue full alpha
            "FFFFFF00" to Quad(1.0f, 1.0f, 1.0f, 0.0f), // White transparent
            "FF6A1380" to Quad(1.0f, 0.416f, 0.075f, 0.502f) // Orange 50% alpha
        )
        
        testCases.forEach { (rgba, expected) ->
            val color = ColorUtils.parseColorHex(rgba)
            assertEquals("RGBA $rgba red", expected.r, color.red, 0.01f)
            assertEquals("RGBA $rgba green", expected.g, color.green, 0.01f)
            assertEquals("RGBA $rgba blue", expected.b, color.blue, 0.01f)
            assertEquals("RGBA $rgba alpha", expected.a, color.alpha, 0.01f)
        }
    }

    @Test
    fun `parseColorHex handles invalid input gracefully`() {
        val invalidInputs = listOf(
            "", "ZZZ", "12345", "123456789", "#GGG", "INVALID"
        )
        
        invalidInputs.forEach { input ->
            val result = ColorUtils.parseColorHex(input)
            assertEquals("Invalid input '$input' should return Gray", Color.Gray, result)
        }
    }

    @Test
    fun `isValidColorHex validates color strings correctly`() {
        val validCases = listOf(
            "FF0000", "#FF0000", "ff0000", "#ff0000", 
            "123456", "#123456", "ABCDEF", "#ABCDEF",
            "FF0000AA", "#FF0000AA", "12345678", "#12345678"
        )
        
        val invalidCases = listOf(
            "", "#", "ZZZ", "#GGG", "12345", "123456789", 
            "GGGGGG", "#GGGGGG", "FF00", "#FF00FF0"
        )
        
        validCases.forEach { input ->
            assertTrue("'$input' should be valid", ColorUtils.isValidColorHex(input))
        }
        
        invalidCases.forEach { input ->
            assertFalse("'$input' should be invalid", ColorUtils.isValidColorHex(input))
        }
    }

    @Test
    fun `convertRGBAtoAARRGGBB transforms format correctly`() {
        // Test the specific transformation logic from commit a261484
        val testCases = mapOf(
            "FF6A1380" to "80FF6A13", // Orange RGBA -> AARRGGBB
            "#FF000080" to "80FF0000", // Red RGBA -> AARRGGBB  
            "00FF0040" to "4000FF00", // Green RGBA -> AARRGGBB
            "0000FFFF" to "FF0000FF", // Blue RGBA -> AARRGGBB
            "FFFFFF00" to "00FFFFFF", // White transparent -> AARRGGBB
            "FF0000" to "FF0000", // 6-char should pass through
            "#123456" to "123456" // 6-char with hash should remove hash
        )
        
        testCases.forEach { (input, expected) ->
            val result = ColorUtils.convertRGBAtoAARRGGBB(input)
            assertEquals("RGBA '$input' should convert to AARRGGBB '$expected'", expected, result)
        }
    }

    @Test
    fun `formatColorHexForDisplay handles edge cases from real usage`() {
        // Test edge cases that could occur in real ColorPreviewCard usage
        val edgeCases = mapOf(
            // Cases that caused the original bug (commit 4c21797)
            "#FF6A13" to "#FF6A13", // Normal case
            "##FF6A13" to "#FF6A13", // Double hash
            "#FF6A13EXTRA" to "#FF6A13", // Extra characters
            "#ff6a13" to "#ff6a13", // Lowercase
            "FF6A13" to "#FF6A13", // Missing hash
            "" to "#", // Empty string
            "   " to "#", // Whitespace only
            "#" to "#" // Just hash
        )
        
        edgeCases.forEach { (input, expected) ->
            val result = ColorUtils.formatColorHexForDisplay(input)
            assertEquals("Edge case '$input' should format correctly", expected, result)
        }
    }

    @Test
    fun `color conversion preserves precision for Bambu Lab colors`() {
        // Test with real Bambu Lab colors to ensure conversion accuracy
        val bambuColors = mapOf(
            "FF6A13FF" to "Bambu Orange", // Full alpha orange
            "1E3A8AFF" to "Bambu Blue", // Full alpha blue
            "DC143CFF" to "Bambu Red", // Full alpha red
            "FF6A1380" to "Semi-transparent Orange", // 50% alpha orange
            "FFFFFF00" to "Transparent White" // Fully transparent
        )
        
        bambuColors.forEach { (rgba, description) ->
            val color = ColorUtils.parseColorHex(rgba)
            
            // Verify the color can be parsed without exceptions
            assertNotNull("$description should parse successfully", color)
            
            // Verify components are in valid range
            assertTrue("$description red in range", color.red in 0.0f..1.0f)
            assertTrue("$description green in range", color.green in 0.0f..1.0f)
            assertTrue("$description blue in range", color.blue in 0.0f..1.0f)
            assertTrue("$description alpha in range", color.alpha in 0.0f..1.0f)
            
            // Test round-trip formatting
            val formatted = ColorUtils.formatColorHexForDisplay(rgba)
            assertTrue("$description should format with # prefix", formatted.startsWith("#"))
            assertEquals("$description should format to 7 chars", 7, formatted.length)
        }
    }

    @Test
    fun `RGBA conversion handles transparency correctly for UI rendering`() {
        // Ensure transparency is handled correctly for Compose UI
        val transparencyTestCases = listOf(
            "FF000000" to 0.0f, // Fully transparent red
            "FF000080" to 0.502f, // Semi-transparent red  
            "FF0000FF" to 1.0f, // Fully opaque red
            "00000000" to 0.0f, // Fully transparent black
            "FFFFFFFF" to 1.0f // Fully opaque white
        )
        
        transparencyTestCases.forEach { (rgba, expectedAlpha) ->
            val color = ColorUtils.parseColorHex(rgba)
            assertEquals("RGBA $rgba should have correct alpha", 
                expectedAlpha, color.alpha, 0.01f)
            
            // Verify the alpha affects visibility correctly
            if (expectedAlpha == 0.0f) {
                assertTrue("Transparent color should not be visible in UI", 
                    color.alpha < 0.01f)
            } else if (expectedAlpha == 1.0f) {
                assertTrue("Opaque color should be fully visible in UI",
                    color.alpha > 0.99f)
            }
        }
    }

    // Helper data class for RGBA components
    private data class Quad(val r: Float, val g: Float, val b: Float, val a: Float)
}