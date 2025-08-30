package com.bscan.ui.components.visual

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bscan.repository.UserPreferencesRepository
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.components.MaterialDisplayMode
import com.bscan.ui.components.rememberAccelerometerState
import com.bscan.ui.components.tiltToPositionOffset

/**
 * Universal material display component with visual effects
 * Supports any component category, not just filament
 */
@Composable
fun MaterialDisplayBox(
    colorHex: String,
    materialType: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    shape: Shape? = null, // Allow override, but default to material-based shape
    materialDisplaySettings: MaterialDisplaySettings? = null, // New granular settings
    category: String = "filament" // Component category for extension to non-filament types
) {
    val context = LocalContext.current
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    
    // Get display settings from parameter or user preferences
    val actualDisplaySettings = materialDisplaySettings ?: run {
        // Convert from user preferences
        when (userPrefsRepository.getMaterialDisplayMode()) {
            MaterialDisplayMode.SHAPES -> MaterialDisplaySettings.SHAPES_ONLY
            MaterialDisplayMode.TEXT_LABELS -> MaterialDisplaySettings.TEXT_LABELS
        }
    }
    
    // Get accelerometer effects preference and state
    val accelerometerEffectsEnabled = userPrefsRepository.isAccelerometerEffectsEnabled()
    val motionSensitivity = userPrefsRepository.getMotionSensitivity()
    val tiltAngles = rememberAccelerometerState(accelerometerEffectsEnabled)
    val (tiltOffsetX, tiltOffsetY) = tiltToPositionOffset(tiltAngles, sensitivity = motionSensitivity)
    
    val originalColor = parseColorWithAlpha(colorHex)
    val detectedMaterialType = detectMaterialType(materialType)
    val finish = detectFilamentFinish(colorHex, materialType)
    
    // Determine shape based on settings
    val actualShape = when {
        !actualDisplaySettings.showMaterialShapes -> shape ?: RoundedCornerShape(8.dp)
        else -> shape ?: getMaterialShape(detectedMaterialType)
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
        
        // Material-specific reflection effects (currently only for filament category)
        if (category == "filament") {
            val blurIntensity = getReflectionBlurIntensity(detectedMaterialType, finish, materialType)
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
                val materialName = getMaterialAbbreviation(detectedMaterialType)
                val variantName = getVariantFromFilamentType(materialType, actualDisplaySettings.showFullVariantNamesInShape)

                val showMaterialName = actualDisplaySettings.showMaterialNameInShape
                val showVariantName = actualDisplaySettings.showMaterialVariantInShape && variantName.isNotEmpty()

                val calculatedFontSize = if (showMaterialName && showVariantName) { (size.value / 2 * 0.8f).sp } else { (size.value * 0.20f).sp }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showMaterialName) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = materialName,
                                color = textColor,
                                fontSize = calculatedFontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = calculatedFontSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (showVariantName) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = variantName,
                                color = textColor,
                                fontSize = calculatedFontSize,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = calculatedFontSize,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialDisplayBoxPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Basic Materials", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialDisplayBox(
                    colorHex = "#FF0000",
                    materialType = "PLA Basic",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#00FF00",
                    materialType = "ABS Basic",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#0000FF",
                    materialType = "PETG Basic",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#FFD700",
                    materialType = "ASA Basic",
                    size = 48.dp
                )
            }

            Text("Special Finishes", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialDisplayBox(
                    colorHex = "#FFD700",
                    materialType = "PLA Silk",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#800080",
                    materialType = "PLA Matte",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#FFA50080",
                    materialType = "PLA Translucent",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#000000",
                    materialType = "PLA CF",
                    size = 48.dp
                )
            }

            Text("Non-Filament Materials", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaterialDisplayBox(
                    colorHex = "#C0C0C0",
                    materialType = "Aluminum",
                    size = 48.dp,
                    category = "metal"
                )
                MaterialDisplayBox(
                    colorHex = "#8B4513",
                    materialType = "Wood",
                    size = 48.dp,
                    category = "organic"
                )
                MaterialDisplayBox(
                    colorHex = "#4169E1",
                    materialType = "Resin",
                    size = 48.dp,
                    category = "liquid"
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MaterialDisplayBoxSizesPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Different Sizes", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialDisplayBox(
                    colorHex = "#FF6B35",
                    materialType = "PLA Basic",
                    size = 32.dp
                )
                MaterialDisplayBox(
                    colorHex = "#FF6B35",
                    materialType = "PLA Basic",
                    size = 48.dp
                )
                MaterialDisplayBox(
                    colorHex = "#FF6B35",
                    materialType = "PLA Basic",
                    size = 64.dp
                )
                MaterialDisplayBox(
                    colorHex = "#FF6B35",
                    materialType = "PLA Basic",
                    size = 80.dp
                )
            }
        }
    }
}




