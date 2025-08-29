package com.bscan.ui.components.scanning

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedNfcIcon(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1.0f,
        animationSpec = tween(300, easing = EaseInOutCubic),
        label = "scale_animation"
    )
    
    val color = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Icon(
        imageVector = Icons.Default.Nfc,
        contentDescription = null,
        modifier = modifier.scale(scale),
        tint = color
    )
}

@Composable
fun PulsingRings(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    if (!isActive) return
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    
    val pulsingRingData = createPulsingRingAnimations(infiniteTransition)
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2
        val primaryColor = Color(0xFF1976D2) // Blue
        val center = androidx.compose.ui.geometry.Offset(centerX, centerY)
        val strokeStyle = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        
        pulsingRingData.forEach { (alpha, scale) ->
            drawCircle(
                color = primaryColor,
                radius = maxRadius * scale,
                center = center,
                alpha = alpha,
                style = strokeStyle
            )
        }
    }
}

@Composable
private fun createPulsingRingAnimations(
    infiniteTransition: InfiniteTransition
): List<Pair<Float, Float>> {
    val alphaAnimationSpec = infiniteRepeatable<Float>(
        animation = tween(2000, easing = EaseInOutCubic),
        repeatMode = RepeatMode.Restart
    )
    
    val scaleAnimationSpec = infiniteRepeatable<Float>(
        animation = tween(2000, easing = EaseInOutCubic),
        repeatMode = RepeatMode.Restart
    )
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = alphaAnimationSpec,
        label = "alpha1_animation"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(2000, delayMillis = 500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2_animation"
    )
    
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(2000, delayMillis = 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha3_animation"
    )
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = scaleAnimationSpec,
        label = "scale1_animation"
    )
    
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(2000, delayMillis = 500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2_animation"
    )
    
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(2000, delayMillis = 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale3_animation"
    )
    
    return listOf(
        alpha1 to scale1,
        alpha2 to scale2,
        alpha3 to scale3
    )
}

@Composable
fun SpinningLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinning_transition")
    
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )
    
    Canvas(modifier = modifier) {
        rotate(rotationAngle) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
        }
    }
}

@Composable
fun DetectedSuccessRing(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val successColor = Color(0xFF4CAF50) // Green
        val radius = size.minDimension / 2
        
        drawCircle(
            color = successColor,
            radius = radius,
            alpha = 0.2f
        )
        drawCircle(
            color = successColor,
            radius = radius,
            style = Stroke(width = 3.dp.toPx()),
            alpha = 0.8f
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AnimatedNfcIconPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("NFC Icon States", style = MaterialTheme.typography.titleMedium)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNfcIcon(
                        modifier = Modifier.size(48.dp),
                        isActive = false
                    )
                    Text("Inactive", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedNfcIcon(
                        modifier = Modifier.size(48.dp),
                        isActive = true
                    )
                    Text("Active", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PulsingRingsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Pulsing Rings Animation", style = MaterialTheme.typography.titleMedium)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PulsingRings(
                            modifier = Modifier.fillMaxSize(),
                            isActive = false
                        )
                    }
                    Text("Inactive", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PulsingRings(
                            modifier = Modifier.fillMaxSize(),
                            isActive = true
                        )
                    }
                    Text("Active", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SpinningLoaderPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Spinning Loaders", style = MaterialTheme.typography.titleMedium)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningLoader(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("Primary", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningLoader(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("Secondary", style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningLoader(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text("Large", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetectedSuccessRingPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Success Ring", style = MaterialTheme.typography.titleMedium)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetectedSuccessRing(
                    modifier = Modifier.size(48.dp)
                )
                DetectedSuccessRing(
                    modifier = Modifier.size(64.dp)
                )
                DetectedSuccessRing(
                    modifier = Modifier.size(80.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AllAnimationComponentsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("All Animation Components", style = MaterialTheme.typography.titleLarge)
            
            // NFC Icons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedNfcIcon(
                    modifier = Modifier.size(40.dp),
                    isActive = false
                )
                AnimatedNfcIcon(
                    modifier = Modifier.size(40.dp),
                    isActive = true
                )
            }
            
            // Pulsing Rings
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                PulsingRings(
                    modifier = Modifier.fillMaxSize(),
                    isActive = true
                )
            }
            
            // Spinners
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpinningLoader(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SpinningLoader(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // Success Ring
            DetectedSuccessRing(
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

// Preview Functions
@Preview(showBackground = true)
@Composable
fun AnimatedNfcIconInactivePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedNfcIcon(
                modifier = Modifier.size(48.dp),
                isActive = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnimatedNfcIconActivePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedNfcIcon(
                modifier = Modifier.size(48.dp),
                isActive = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PulsingRingsActivePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingRings(
                modifier = Modifier.fillMaxSize(),
                isActive = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PulsingRingsInactivePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            PulsingRings(
                modifier = Modifier.fillMaxSize(),
                isActive = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpinningLoaderPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            SpinningLoader(
                modifier = Modifier.size(80.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetectedSuccessRingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            DetectedSuccessRing(
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanningAnimationsShowcasePreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Idle state with pulsing rings
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                PulsingRings(
                    modifier = Modifier.fillMaxSize(),
                    isActive = true
                )
                AnimatedNfcIcon(
                    modifier = Modifier.size(48.dp),
                    isActive = false
                )
            }
            
            // Processing state with spinning loader
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                SpinningLoader(
                    modifier = Modifier.size(100.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                AnimatedNfcIcon(
                    modifier = Modifier.size(48.dp),
                    isActive = true
                )
            }
            
            // Success state with success ring
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                DetectedSuccessRing(
                    modifier = Modifier.size(100.dp)
                )
                AnimatedNfcIcon(
                    modifier = Modifier.size(48.dp),
                    isActive = true
                )
            }
        }
    }
}