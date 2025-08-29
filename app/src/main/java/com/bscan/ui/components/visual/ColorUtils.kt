package com.bscan.ui.components.visual

import androidx.compose.ui.graphics.Color

/**
 * Parses color hex string preserving alpha channel
 */
fun parseColorWithAlpha(colorHex: String): Color {
    return try {
        val cleanHex = colorHex.removePrefix("#")
        
        val colorLong = when (cleanHex.length) {
            6 -> {
                // RGB format - add full alpha
                ("FF" + cleanHex).toLong(16)
            }
            8 -> {
                // Check if this is RGBA or AARRGGBB format
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
 * Determines if a color is light or dark for contrast purposes
 */
fun isColorLight(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.5f
}