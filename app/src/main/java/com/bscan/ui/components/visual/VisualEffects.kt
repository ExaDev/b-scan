package com.bscan.ui.components.visual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Draws a checkerboard pattern for translucent backgrounds
 */
fun DrawScope.drawCheckerboard(
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
fun DrawScope.drawSilkShimmer(
    size: androidx.compose.ui.geometry.Size,
    offset: Float,
    shape: Shape,
    tiltOffsetX: Float = 0f, // -1.0 to 1.0 horizontal position offset
    tiltOffsetY: Float = 0f,  // -1.0 to 1.0 vertical position offset
    motionSensitivity: Float = 0.7f // User's motion sensitivity setting
) {
    val shimmerWidth = size.width * 0.3f
    // Apply tilt offset to shimmer position
    val baseShimmerCenter = size.width * 0.5f + offset * size.width * 0.5f
    // Scale shimmer movement based on sensitivity (max 30% at full sensitivity)
    val shimmerMovement = 0.3f * motionSensitivity
    val shimmerCenter = baseShimmerCenter + (tiltOffsetX * size.width * shimmerMovement)
    
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            Color.White.copy(alpha = 0.2f),
            Color.White.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.2f),
            Color.Transparent
        ),
        start = Offset(shimmerCenter - shimmerWidth, tiltOffsetY * size.height * 0.1f * motionSensitivity),
        end = Offset(shimmerCenter + shimmerWidth, size.height + (tiltOffsetY * size.height * 0.1f * motionSensitivity))
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
fun DrawScope.drawMatteStippling(
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

/**
 * Draws vertical stripes pattern for Support materials
 */
fun DrawScope.drawSupportStripes(
    size: androidx.compose.ui.geometry.Size,
    color: Color,
    density: androidx.compose.ui.unit.Density
) {
    val stripeWidth = with(density) { 3.dp.toPx() }
    val stripeSpacing = with(density) { 6.dp.toPx() }
    val darkerColor = color.copy(alpha = color.alpha * 0.6f)
    
    // Create circular clipping path
    val path = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(androidx.compose.ui.geometry.Offset.Zero, size))
    }
    
    clipPath(path) {
        var x = 0f
        while (x < size.width) {
            drawRect(
                color = darkerColor,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(stripeWidth, size.height)
            )
            x += stripeSpacing
        }
    }
}

/**
 * Draws reflection effect with configurable blur intensity
 */
fun DrawScope.drawReflection(
    size: androidx.compose.ui.geometry.Size,
    blurIntensity: Float, // 0.0 = sharp reflection, 1.0 = very blurred
    reflectionStrength: Float = 0.4f, // Opacity of the reflection
    shape: Shape,
    tiltOffsetX: Float = 0f, // -1.0 to 1.0 horizontal position offset
    tiltOffsetY: Float = 0f,  // -1.0 to 1.0 vertical position offset
    motionSensitivity: Float = 0.7f // User's motion sensitivity setting
) {
    val reflectionSize = size.minDimension * 0.5f
    // Base position at top-right, then apply tilt offset
    val baseX = size.width * 0.75f
    val baseY = size.height * 0.25f
    // Scale movement range based on sensitivity (max 40% at full sensitivity)
    val movementRange = 0.4f * motionSensitivity
    val centerX = baseX + (tiltOffsetX * size.width * movementRange)
    val centerY = baseY + (tiltOffsetY * size.height * movementRange)
    
    // Create gradient with blur effect - more blur means more gradient stops
    val gradientStops = if (blurIntensity < 0.3f) {
        // Sharp reflection (PETG)
        listOf(
            0.0f to Color.White.copy(alpha = reflectionStrength),
            0.3f to Color.White.copy(alpha = reflectionStrength * 0.7f),
            0.6f to Color.White.copy(alpha = reflectionStrength * 0.3f),
            1.0f to Color.Transparent
        )
    } else if (blurIntensity < 0.7f) {
        // Medium blur (Regular PLA)
        listOf(
            0.0f to Color.White.copy(alpha = reflectionStrength * 0.8f),
            0.4f to Color.White.copy(alpha = reflectionStrength * 0.5f),
            0.7f to Color.White.copy(alpha = reflectionStrength * 0.2f),
            1.0f to Color.Transparent
        )
    } else {
        // High blur (Matte PLA)
        listOf(
            0.0f to Color.White.copy(alpha = reflectionStrength * 0.6f),
            0.5f to Color.White.copy(alpha = reflectionStrength * 0.3f),
            0.8f to Color.White.copy(alpha = reflectionStrength * 0.1f),
            1.0f to Color.Transparent
        )
    }
    
    val reflectionBrush = Brush.radialGradient(
        colorStops = gradientStops.toTypedArray(),
        center = Offset(centerX, centerY),
        radius = reflectionSize * (1.0f + blurIntensity) // Larger radius for more blur
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
            brush = reflectionBrush,
            size = size
        )
    }
}