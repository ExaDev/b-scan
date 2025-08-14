package com.bscan.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bscan.ScanState

@Composable
fun NfcStatusIndicator(
    scanState: ScanState,
    modifier: Modifier = Modifier
) {
    val (color, shouldAnimate) = when (scanState) {
        ScanState.IDLE -> MaterialTheme.colorScheme.outline to false
        ScanState.TAG_DETECTED -> Color(0xFF4CAF50) to true // Green
        ScanState.PROCESSING -> MaterialTheme.colorScheme.primary to true
        ScanState.SUCCESS -> Color(0xFF4CAF50) to false // Green
        ScanState.ERROR -> MaterialTheme.colorScheme.error to true
    }
    
    val alpha by if (shouldAnimate) {
        val infiniteTransition = rememberInfiniteTransition(label = "status_indicator")
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha_animation"
        )
    } else {
        remember { mutableStateOf(1f) }
    }
    
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}