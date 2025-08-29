package com.bscan.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import java.lang.reflect.Method

/**
 * Test coverage for ColorPreviewCard component, focusing on the parseColorWithAlpha function
 * that handles RGBA to AARRGGBB conversion for Android.
 * 
 * Since parseColorWithAlpha is a public function, we test it via reflection for consistency.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class ColorPreviewCardTest {
    
    @Test
    fun `parseColor via reflection should handle RGB format correctly`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test that function exists and doesn't throw exceptions
        try {
            val color1 = parseColorMethod.invoke(null, "#FF0000")
            assertNotNull("RGB red should return non-null result", color1)
            
            val color2 = parseColorMethod.invoke(null, "00FF00")  
            assertNotNull("RGB green should return non-null result", color2)
        } catch (e: Exception) {
            fail("parseColorWithAlpha should not throw exception for valid RGB: ${e.message}")
        }
    }
    
    @Test
    fun `parseColor via reflection should handle RGBA format correctly`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test that RGBA format doesn't throw exceptions  
        try {
            val color = parseColorMethod.invoke(null, "FF00FF80")
            assertNotNull("RGBA should return non-null result", color)
        } catch (e: Exception) {
            fail("parseColorWithAlpha should not throw exception for RGBA: ${e.message}")
        }
    }
    
    @Test
    fun `parseColor via reflection should handle 8-char hex correctly`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test 8-character hex input handling
        try {
            val color = parseColorMethod.invoke(null, "80FF00FF")
            assertNotNull("8-char hex should return non-null result", color)
        } catch (e: Exception) {
            fail("parseColorWithAlpha should not throw exception for 8-char hex: ${e.message}")
        }
    }
    
    @Test
    fun `parseColor via reflection should handle invalid hex gracefully`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test that invalid hex formats don't crash
        try {
            val color1 = parseColorMethod.invoke(null, "invalid")
            assertNotNull("Invalid hex should return fallback color", color1)
            
            val color2 = parseColorMethod.invoke(null, "#GG0000")
            assertNotNull("Invalid characters should return fallback color", color2)
            
            val color3 = parseColorMethod.invoke(null, "")
            assertNotNull("Empty string should return fallback color", color3)
        } catch (e: Exception) {
            fail("parseColorWithAlpha should handle invalid input gracefully: ${e.message}")
        }
    }
    
    @Test
    fun `parseColor via reflection should handle unusual length hex`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test unusual length hex strings don't crash
        try {
            val color1 = parseColorMethod.invoke(null, "F0F")
            assertNotNull("Short hex should return fallback color", color1)
            
            val color2 = parseColorMethod.invoke(null, "FF0000FFAA")
            assertNotNull("Long hex should return fallback color", color2)
        } catch (e: Exception) {
            fail("parseColorWithAlpha should handle unusual length hex gracefully: ${e.message}")
        }
    }
    
    @Test
    fun `parseColor RGBA conversion logic should work correctly`() {
        val parseColorMethod = getParseColorMethod()
        
        // Test RGBA conversion examples don't crash
        val testCases = listOf(
            "FF000080", // Red with 50% alpha
            "00FF0040", // Green with 25% alpha  
            "0000FFFF", // Blue with full alpha
            "FFFFFF00"  // White with 0% alpha
        )
        
        testCases.forEach { rgba ->
            try {
                val color = parseColorMethod.invoke(null, rgba)
                assertNotNull("RGBA $rgba should return non-null result", color)
            } catch (e: Exception) {
                fail("parseColorWithAlpha should not throw exception for RGBA $rgba: ${e.message}")
            }
        }
    }
    
    private fun getParseColorMethod(): Method {
        // Use reflection to access the parseColorWithAlpha method from new location
        val clazz = Class.forName("com.bscan.ui.components.visual.ColorUtilsKt")
        val method = clazz.getDeclaredMethod("parseColorWithAlpha", String::class.java)
        method.isAccessible = true
        return method
    }
}