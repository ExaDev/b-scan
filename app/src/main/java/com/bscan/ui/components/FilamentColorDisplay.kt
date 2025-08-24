package com.bscan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class FilamentFinish {
    BASIC,
    SILK,
    MATTE,
    TRANSLUCENT
}

/**
 * Detects the finish type of filament based on color alpha channel and filament type
 */
fun detectFilamentFinish(colorHex: String, filamentType: String): FilamentFinish {
    // Parse alpha from hex colour
    val cleanHex = colorHex.removePrefix("#")
    val alpha = when (cleanHex.length) {
        8 -> {
            // Check if this is RGBA format (common from tag data)
            val alphaHex = cleanHex.substring(6, 8)
            alphaHex.toIntOrNull(16) ?: 255
        }
        else -> 255
    }
    
    // Check for translucent materials
    if (alpha < 255 || filamentType.contains("Translucent", ignoreCase = true)) {
        return FilamentFinish.TRANSLUCENT
    }
    
    // Otherwise check product line for finish type
    return when {
        filamentType.contains("Silk", ignoreCase = true) -> FilamentFinish.SILK
        filamentType.contains("Matte", ignoreCase = true) -> FilamentFinish.MATTE
        else -> FilamentFinish.BASIC
    }
}

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

/**
 * Enhanced filament color display box with finish-specific visual effects
 */
@Composable
fun FilamentColorBox(
    colorHex: String,
    filamentType: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape = CircleShape
) {
    val originalColor = parseColorWithAlpha(colorHex)
    val finish = detectFilamentFinish(colorHex, filamentType)
    
    // Apply automatic alpha to translucent materials that don't have alpha in their hex
    val color = if (finish == FilamentFinish.TRANSLUCENT && originalColor.alpha == 1f) {
        originalColor.copy(alpha = 0.5f) // 50% opacity for translucent materials
    } else {
        originalColor
    }
    val density = LocalDensity.current
    
    // Animation for silk shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_offset"
    )
    
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
    ) {
        // Background effects based on finish type
        when (finish) {
            FilamentFinish.TRANSLUCENT -> {
                // Checkerboard background for translucent colours
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    drawCheckerboard(
                        size = this.size,
                        checkSize = with(density) { 4.dp.toPx() }
                    )
                }
            }
            else -> {
                // Solid background for non-translucent colours
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(MaterialTheme.colorScheme.surface, shape)
                )
            }
        }
        
        // Main colour layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color, shape)
        )
        
        // Finish-specific overlay effects
        when (finish) {
            FilamentFinish.SILK -> {
                // Silk shimmer effect
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    drawSilkShimmer(
                        size = this.size,
                        offset = shimmerOffset,
                        shape = shape
                    )
                }
            }
            FilamentFinish.MATTE -> {
                // Matte stippling effect
                Canvas(
                    modifier = Modifier.matchParentSize()
                ) {
                    drawMatteStippling(
                        size = this.size,
                        isColorLight = isColorLight(color),
                        shape = shape,
                        density = density
                    )
                }
            }
            else -> {
                // No additional effects for basic or translucent
            }
        }
    }
}

/**
 * Draws a checkerboard pattern for translucent backgrounds
 */
private fun DrawScope.drawCheckerboard(
    size: androidx.compose.ui.geometry.Size,
    checkSize: Float
) {
    val lightColor = Color(0xFFFFFFFF)
    val darkColor = Color(0xFFE0E0E0)
    
    val checksX = (size.width / checkSize).toInt() + 1
    val checksY = (size.height / checkSize).toInt() + 1
    
    for (x in 0 until checksX) {
        for (y in 0 until checksY) {
            val color = if ((x + y) % 2 == 0) lightColor else darkColor
            drawRect(
                color = color,
                topLeft = Offset(x * checkSize, y * checkSize),
                size = androidx.compose.ui.geometry.Size(checkSize, checkSize)
            )
        }
    }
}

/**
 * Draws silk shimmer effect
 */
private fun DrawScope.drawSilkShimmer(
    size: androidx.compose.ui.geometry.Size,
    offset: Float,
    shape: Shape
) {
    val shimmerWidth = size.width * 0.3f
    val shimmerCenter = size.width * 0.5f + offset * size.width * 0.5f
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.2f),
            Color.Transparent
        ),
        start = Offset(shimmerCenter - shimmerWidth, 0f),
        end = Offset(shimmerCenter + shimmerWidth, size.height)
    )
    
    // Create a path based on the shape for clipping
    val path = Path().apply {
        when (shape) {
            CircleShape -> {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset.Zero,
                        size
                    )
                )
            }
            is RoundedCornerShape -> {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        androidx.compose.ui.geometry.Rect(Offset.Zero, size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.minDimension * 0.1f
                        )
                    )
                )
            }
            else -> {
                addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
            }
        }
    }
    
    clipPath(path) {
        drawRect(
            brush = shimmerBrush,
            size = size
        )
    }
}

/**
 * Draws matte stippling effect with regular grid pattern
 */
private fun DrawScope.drawMatteStippling(
    size: androidx.compose.ui.geometry.Size,
    isColorLight: Boolean,
    shape: Shape,
    density: androidx.compose.ui.unit.Density
) {
    val dotColor = if (isColorLight) {
        Color.Black.copy(alpha = 0.25f)
    } else {
        Color.White.copy(alpha = 0.25f)
    }
    
    val spacing = with(density) { 3.dp.toPx() }
    val dotRadius = with(density) { 0.5.dp.toPx() }
    
    val dotsX = (size.width / spacing).toInt()
    val dotsY = (size.height / spacing).toInt()
    
    // Create a path based on the shape for clipping
    val path = Path().apply {
        when (shape) {
            CircleShape -> {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        Offset.Zero,
                        size
                    )
                )
            }
            is RoundedCornerShape -> {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        androidx.compose.ui.geometry.Rect(Offset.Zero, size),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            size.minDimension * 0.1f
                        )
                    )
                )
            }
            else -> {
                addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size))
            }
        }
    }
    
    clipPath(path) {
        for (x in 0 until dotsX) {
            for (y in 0 until dotsY) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = Offset(
                        x * spacing + spacing / 2,
                        y * spacing + spacing / 2
                    )
                )
            }
        }
    }
}