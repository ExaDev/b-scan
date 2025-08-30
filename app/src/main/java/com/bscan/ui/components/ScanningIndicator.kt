package com.bscan.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import com.bscan.ui.components.scanning.*

// Re-export main components for backward compatibility
@Composable
fun ScanStateIndicator(
    isIdle: Boolean = false,
    isDetected: Boolean = false,
    isProcessing: Boolean = false,
    modifier: Modifier = Modifier
) = com.bscan.ui.components.scanning.ScanStateIndicator(
    isIdle = isIdle,
    isDetected = isDetected,
    isProcessing = isProcessing,
    modifier = modifier
)

@Composable
fun AnimatedNfcIcon(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) = com.bscan.ui.components.scanning.AnimatedNfcIcon(
    modifier = modifier,
    isActive = isActive
)

@Composable
fun PulsingRings(
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) = com.bscan.ui.components.scanning.PulsingRings(
    modifier = modifier,
    isActive = isActive
)

@Composable
fun SpinningLoader(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) = com.bscan.ui.components.scanning.SpinningLoader(
    modifier = modifier,
    color = color
)

// Previews
@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorPreview() {
    MaterialTheme {
        ScanStateIndicator(
            isDetected = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AnimatedNfcIconPreview() {
    MaterialTheme {
        AnimatedNfcIcon(
            isActive = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PulsingRingsPreview() {
    MaterialTheme {
        PulsingRings(
            isActive = true
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SpinningLoaderPreview() {
    MaterialTheme {
        SpinningLoader()
    }
}