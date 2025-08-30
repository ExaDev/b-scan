package com.bscan.ui.components.scanning

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

@Preview(showBackground = true)
@Composable
private fun ScanStateIndicatorPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Idle State", style = MaterialTheme.typography.titleMedium)
            ScanStateIndicator(
                isIdle = true,
                isDetected = false,
                isProcessing = false
            )
            
            Text("Detected State", style = MaterialTheme.typography.titleMedium)
            ScanStateIndicator(
                isIdle = false,
                isDetected = true,
                isProcessing = false
            )
            
            Text("Processing State", style = MaterialTheme.typography.titleMedium)
            ScanStateIndicator(
                isIdle = false,
                isDetected = false,
                isProcessing = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanStateIndicatorAllStatesPreview() {
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanStateIndicator(
                    isIdle = true,
                    modifier = Modifier.size(80.dp)
                )
                Text("Idle", style = MaterialTheme.typography.bodySmall)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanStateIndicator(
                    isDetected = true,
                    modifier = Modifier.size(80.dp)
                )
                Text("Detected", style = MaterialTheme.typography.bodySmall)
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ScanStateIndicator(
                    isProcessing = true,
                    modifier = Modifier.size(80.dp)
                )
                Text("Processing", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// Preview Functions
@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorIdlePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScanStateIndicator(
                isIdle = true,
                isDetected = false,
                isProcessing = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorDetectedPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScanStateIndicator(
                isIdle = false,
                isDetected = true,
                isProcessing = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorProcessingPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScanStateIndicator(
                isIdle = false,
                isDetected = false,
                isProcessing = true
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorInactivePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScanStateIndicator(
                isIdle = false,
                isDetected = false,
                isProcessing = false
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanStateIndicatorColumnPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanStateIndicator(
                isIdle = true,
                isDetected = false,
                isProcessing = false
            )
            
            ScanStateIndicator(
                isIdle = false,
                isDetected = false,
                isProcessing = true
            )
            
            ScanStateIndicator(
                isIdle = false,
                isDetected = true,
                isProcessing = false
            )
        }
    }
}