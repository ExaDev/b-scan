package com.bscan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bscan.repository.UserPreferencesRepository

enum class FilamentFinish {
    BASIC,
    SILK,
    MATTE,
    TRANSLUCENT
}

/**
 * Determines reflection blur intensity based on material type and finish
 */
fun getReflectionBlurIntensity(materialType: MaterialType, finish: FilamentFinish, filamentType: String): Float {
    return when {
        // PETG HF gets same reflection as regular PLA (medium blur)
        materialType == MaterialType.PETG && filamentType.contains("HF", ignoreCase = true) -> 0.5f
        // Regular PETG gets strong reflection
        materialType == MaterialType.PETG -> 0.1f
        // Matte PLA gets very blurred reflection
        materialType == MaterialType.PLA && finish == FilamentFinish.MATTE -> 0.9f
        // Regular PLA gets medium blur
        materialType == MaterialType.PLA && finish == FilamentFinish.BASIC -> 0.5f
        // Other combinations get no reflection
        else -> -1f // Use -1 to indicate no reflection should be applied
    }
}

enum class MaterialType {
    PLA,      // Circle
    ABS,      // Triangle  
    ASA,      // Inverted Triangle
    PETG,     // Hexagon
    TPU,      // Rounded Square
    PC,       // Octagon
    PA,       // Diamond/Rhombus
    PVA,      // Teardrop
    SUPPORT,  // Vertical Lines
    UNKNOWN   // Dodecagon (12-sided)
}

/**
 * Detects the base material type from filament type string
 */
fun detectMaterialType(filamentType: String): MaterialType {
    return when {
        filamentType.contains("Support", ignoreCase = true) -> MaterialType.SUPPORT
        filamentType.contains("PVA", ignoreCase = true) -> MaterialType.PVA
        filamentType.contains("PLA", ignoreCase = true) -> MaterialType.PLA
        filamentType.contains("ASA", ignoreCase = true) -> MaterialType.ASA
        filamentType.contains("ABS", ignoreCase = true) -> MaterialType.ABS
        filamentType.contains("PETG", ignoreCase = true) -> MaterialType.PETG
        filamentType.contains("TPU", ignoreCase = true) -> MaterialType.TPU
        filamentType.contains("PC", ignoreCase = true) -> MaterialType.PC
        filamentType.contains("PA", ignoreCase = true) || 
        filamentType.contains("Nylon", ignoreCase = true) -> MaterialType.PA
        else -> MaterialType.UNKNOWN
    }
}

/**
 * Gets the material abbreviation for text overlay display
 */
fun getMaterialAbbreviation(materialType: MaterialType): String {
    return when (materialType) {
        MaterialType.PLA -> "PLA"
        MaterialType.ABS -> "ABS"
        MaterialType.ASA -> "ASA"
        MaterialType.PETG -> "PETG"
        MaterialType.TPU -> "TPU"
        MaterialType.PC -> "PC"
        MaterialType.PA -> "PA"
        MaterialType.PVA -> "PVA"
        MaterialType.SUPPORT -> "SUP"
        MaterialType.UNKNOWN -> "?"
    }
}

/**
 * Extracts variant information from filament type string
 */
fun getVariantFromFilamentType(filamentType: String, showFullVariantNames: Boolean): String {
    // Common variant patterns
    val variants = listOf(
        "Basic", "Silk", "Matte", "Translucent", "Carbon Fiber", "CF", 
        "Support", "Water Soluble", "High Flow", "HF", "Tough", "Impact"
    )
    
    for (variant in variants) {
        if (filamentType.contains(variant, ignoreCase = true)) {
            return if (showFullVariantNames) {
                when (variant.uppercase()) {
                    "CF" -> "Carbon Fiber"
                    "HF" -> "High Flow"
                    else -> variant
                }
            } else {
                when (variant.uppercase()) {
                    "BASIC" -> "B"
                    "SILK" -> "S"
                    "MATTE" -> "M"
                    "TRANSLUCENT" -> "T"
                    "CARBON FIBER", "CF" -> "CF"
                    "SUPPORT" -> "SUP"
                    "HIGH FLOW", "HF" -> "HF"
                    "TOUGH" -> "TGH"
                    "IMPACT" -> "IMP"
                    else -> variant.take(3).uppercase()
                }
            }
        }
    }
    
    return ""
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
    shape: Shape? = null, // Allow override, but default to material-based shape
    displayMode: MaterialDisplayMode? = null, // Allow override of display mode (deprecated)
    materialDisplaySettings: MaterialDisplaySettings? = null // New granular settings
) {
    val context = LocalContext.current
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    
    // Get display settings from parameter or user preferences
    val actualDisplaySettings = materialDisplaySettings ?: run {
        // For backward compatibility, check if legacy displayMode is provided
        if (displayMode != null) {
            when (displayMode) {
                MaterialDisplayMode.SHAPES -> MaterialDisplaySettings.SHAPES_ONLY
                MaterialDisplayMode.TEXT_LABELS -> MaterialDisplaySettings.TEXT_LABELS
            }
        } else {
            // Convert from user preferences
            when (userPrefsRepository.getMaterialDisplayMode()) {
                MaterialDisplayMode.SHAPES -> MaterialDisplaySettings.SHAPES_ONLY
                MaterialDisplayMode.TEXT_LABELS -> MaterialDisplaySettings.TEXT_LABELS
            }
        }
    }
    
    // Get accelerometer effects preference and state
    val accelerometerEffectsEnabled = userPrefsRepository.isAccelerometerEffectsEnabled()
    val motionSensitivity = userPrefsRepository.getMotionSensitivity()
    val tiltAngles = rememberAccelerometerState(accelerometerEffectsEnabled)
    val (tiltOffsetX, tiltOffsetY) = tiltToPositionOffset(tiltAngles, sensitivity = motionSensitivity)
    
    val originalColor = parseColorWithAlpha(colorHex)
    val finish = detectFilamentFinish(colorHex, filamentType)
    val materialType = detectMaterialType(filamentType)
    
    // Determine shape based on settings
    val actualShape = when {
        !actualDisplaySettings.showMaterialShapes -> shape ?: RoundedCornerShape(8.dp)
        else -> shape ?: getMaterialShape(materialType)
    }
    
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
            .clip(actualShape)
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
                        .background(MaterialTheme.colorScheme.surface, actualShape)
                )
            }
        }
        
        // Main colour layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color, actualShape)
        )
        
        // Material-specific patterns (Support materials)
        // Support materials use StripedShape which handles the pattern in the shape itself
        
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
                        shape = actualShape,
                        tiltOffsetX = tiltOffsetX,
                        tiltOffsetY = tiltOffsetY,
                        motionSensitivity = motionSensitivity
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
                        shape = actualShape,
                        density = density
                    )
                }
            }
            else -> {
                // No additional finish effects for basic or translucent
            }
        }
        
        // Material-specific reflection effects
        val blurIntensity = getReflectionBlurIntensity(materialType, finish, filamentType)
        if (blurIntensity >= 0f) {
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                drawReflection(
                    size = this.size,
                    blurIntensity = blurIntensity,
                    shape = actualShape,
                    tiltOffsetX = tiltOffsetX,
                    tiltOffsetY = tiltOffsetY,
                    motionSensitivity = motionSensitivity
                )
            }
        }
        
        // Text overlay based on granular settings
        val showAnyText = actualDisplaySettings.showMaterialNameInShape || 
                         actualDisplaySettings.showMaterialVariantInShape
        
        if (showAnyText) {
            Box(
                modifier = Modifier
                    .matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                val textColor = if (isColorLight(color)) Color.Black else Color.White
                val materialName = getMaterialAbbreviation(materialType)
                val variantName = getVariantFromFilamentType(filamentType, actualDisplaySettings.showFullVariantNames)
                
                val displayText = buildString {
                    if (actualDisplaySettings.showMaterialNameInShape) {
                        append(materialName)
                    }
                    if (actualDisplaySettings.showMaterialVariantInShape && variantName.isNotEmpty()) {
                        if (isNotEmpty()) append(" ")
                        append(variantName)
                    }
                }
                
                if (displayText.isNotEmpty()) {
                    Text(
                        text = displayText,
                        color = textColor,
                        fontSize = (size.value * 0.20f).sp, // Slightly smaller to fit more text
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(2.dp),
                        maxLines = 2 // Allow wrapping for longer text
                    )
                }
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

/**
 * Draws vertical stripes pattern for Support materials
 */
private fun DrawScope.drawSupportStripes(
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
private fun DrawScope.drawReflection(
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