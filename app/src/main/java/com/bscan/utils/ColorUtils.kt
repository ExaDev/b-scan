package com.bscan.utils

import androidx.compose.ui.graphics.Color

/**
 * Utility functions for color parsing and formatting.
 * Extracted from UI components to enable testing.
 */
object ColorUtils {
    
    /**
     * Formats a color hex string for display, handling # prefix correctly.
     * This addresses the issue fixed in commit 4c21797.
     */
    fun formatColorHexForDisplay(colorHex: String): String {
        val cleaned = colorHex.trim().removePrefix("#")
        return if (cleaned.isEmpty()) "#" else "#${cleaned.take(6)}"
    }
    
    /**
     * Parses a color hex string into a Compose Color, handling RGBA to AARRGGBB conversion.
     * This addresses the issue fixed in commit a261484.
     */
    fun parseColorHex(colorHex: String): Color {
        return try {
            val cleanHex = colorHex.removePrefix("#")
            
            // Handle different hex formats
            val colorLong = when (cleanHex.length) {
                6 -> {
                    // RGB format - add full alpha
                    ("FF" + cleanHex).toLong(16)
                }
                8 -> {
                    // Check if this is RGBA or AARRGGBB format
                    // If it looks like RGBA (common from tag data), convert to AARRGGBB
                    val r = cleanHex.substring(0, 2)
                    val g = cleanHex.substring(2, 4)
                    val b = cleanHex.substring(4, 6)
                    val a = cleanHex.substring(6, 8)
                    // Convert RGBA to AARRGGBB for Android
                    (a + r + g + b).toLong(16)
                }
                else -> {
                    // Default to gray
                    0xFFAAAAAA
                }
            }
            
            Color(colorLong)
        } catch (e: Exception) {
            Color.Gray
        }
    }
    
    /**
     * Validates if a color hex string is properly formatted.
     */
    fun isValidColorHex(colorHex: String): Boolean {
        val cleanHex = colorHex.removePrefix("#")
        return when (cleanHex.length) {
            6, 8 -> cleanHex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
            else -> false
        }
    }
    
    /**
     * Converts RGBA format color string to AARRGGBB format for Android.
     */
    fun convertRGBAtoAARRGGBB(rgbaHex: String): String {
        val cleanHex = rgbaHex.removePrefix("#")
        
        return if (cleanHex.length == 8) {
            val r = cleanHex.substring(0, 2)
            val g = cleanHex.substring(2, 4)
            val b = cleanHex.substring(4, 6)
            val a = cleanHex.substring(6, 8)
            a + r + g + b
        } else {
            cleanHex
        }
    }
}