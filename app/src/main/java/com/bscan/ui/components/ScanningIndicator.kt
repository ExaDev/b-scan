package com.bscan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha1_animation"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha2_animation"
    )
    
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha3_animation"
    )
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale1_animation"
    )
    
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale2_animation"
    )
    
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale3_animation"
    )
    
    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val maxRadius = size.minDimension / 2
        
        val primaryColor = Color(0xFF1976D2) // Blue
        
        // Draw three pulsing rings
        drawCircle(
            color = primaryColor,
            radius = maxRadius * scale1,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            alpha = alpha1,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        drawCircle(
            color = primaryColor,
            radius = maxRadius * scale2,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            alpha = alpha2,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        
        drawCircle(
            color = primaryColor,
            radius = maxRadius * scale3,
            center = androidx.compose.ui.geometry.Offset(centerX, centerY),
            alpha = alpha3,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
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
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )
    
    Canvas(modifier = modifier) {
        rotate(rotationAngle) {
            val strokeWidth = 4.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Draw spinning arc
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
fun ScanStateIndicator(
    isIdle: Boolean = false,
    isDetected: Boolean = false,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background pulsing rings for idle state
        PulsingRings(
            isActive = isIdle,
            modifier = Modifier.fillMaxSize()
        )
        
        // Spinning loader for processing
        if (isProcessing) {
            SpinningLoader(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        // Success ring for detected state
        if (isDetected) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawCircle(
                    color = Color(0xFF4CAF50), // Green
                    radius = size.minDimension / 2,
                    alpha = 0.2f
                )
                drawCircle(
                    color = Color(0xFF4CAF50),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 3.dp.toPx()),
                    alpha = 0.8f
                )
            }
        }
        
        // Central NFC icon
        AnimatedNfcIcon(
            modifier = Modifier.size(48.dp),
            isActive = isDetected || isProcessing
        )
    }
}