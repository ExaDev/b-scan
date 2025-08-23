package com.bscan.ui.components.scanning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            DetectedSuccessRing(
                modifier = Modifier.size(100.dp)
            )
        }
        
        // Central NFC icon
        AnimatedNfcIcon(
            modifier = Modifier.size(48.dp),
            isActive = isDetected || isProcessing
        )
    }
}