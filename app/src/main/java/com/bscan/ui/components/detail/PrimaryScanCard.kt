package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.ScanResult
import com.bscan.model.TagFormat
import com.bscan.ui.components.ColorPreviewCard
import com.bscan.ui.components.InfoCard
import com.bscan.ui.components.SpecificationCard
import com.bscan.ui.components.TemperatureCard
import com.bscan.ui.components.filament.*
import com.bscan.ui.components.history.ScanHistoryCard
import java.time.LocalDateTime

/**
 * Primary card for displaying scan information including scan history and detailed filament data.
 */
@Composable
fun PrimaryScanCard(
    scan: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Scan Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        var expanded by remember { mutableStateOf(true) }
        ScanHistoryCard(
            scan = scan,
            isExpanded = expanded,
            onToggleExpanded = { expanded = !expanded },
            onScanClick = null
        )
        
        // Display basic scan information (no interpretation available at this level)
        if (scan.scanResult == ScanResult.SUCCESS) {
            InfoCard(
                title = "Scan Result",
                value = "Successfully decrypted ${scan.decryptedBlocks.size} blocks"
            )
            
            InfoCard(
                title = "Authentication",
                value = "${scan.authenticatedSectors.size} sectors authenticated"
            )
            
            if (scan.errors.isNotEmpty()) {
                InfoCard(
                    title = "Warnings",
                    value = scan.errors.joinToString(", ")
                )
            }
        } else {
            InfoCard(
                title = "Scan Result", 
                value = "Scan failed: ${scan.scanResult.name}"
            )
        }
    }
}



// Mock data for successful scan preview
private fun createMockSuccessfulScan(): DecryptedScanData {
    return com.bscan.model.DecryptedScanData(
        timestamp = LocalDateTime.now().minusHours(2),
        tagUid = "A1B2C3D4",
        technology = "MIFARE Classic 1K",
        scanResult = com.bscan.model.ScanResult.SUCCESS,
        decryptedBlocks = mapOf(
            4 to "474641303A413030D2DA4B30000000",
            5 to "000000000000000000000000000000",
            6 to "123456789ABCDEF0123456789ABCDEF"
        ),
        authenticatedSectors = listOf(1, 2, 3, 4, 5),
        failedSectors = emptyList(),
        usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA", 5 to "KeyA"),
        derivedKeys = listOf("ABCD1234567890EF"),
        errors = emptyList(),
        keyDerivationTimeMs = 450L,
        authenticationTimeMs = 350L
    )
}

// Mock data for failed scan preview
private fun createMockFailedScan(): DecryptedScanData {
    return com.bscan.model.DecryptedScanData(
        timestamp = LocalDateTime.now().minusMinutes(30),
        tagUid = "E1F2G3H4",
        technology = "MIFARE Classic 1K",
        scanResult = com.bscan.model.ScanResult.PARSING_FAILED,
        decryptedBlocks = emptyMap(),
        authenticatedSectors = listOf(1, 2),
        failedSectors = listOf(3, 4, 5),
        usedKeys = mapOf(1 to "KeyA", 2 to "KeyB"),
        derivedKeys = listOf("1234ABCDEF567890"),
        errors = listOf("Authentication failed for multiple sectors"),
        keyDerivationTimeMs = 680L,
        authenticationTimeMs = 890L
    )
}

@Preview(showBackground = true)
@Composable
private fun PrimaryScanCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Successful scan
            PrimaryScanCard(
                scan = createMockSuccessfulScan()
            )
            
            Divider()
            
            // Failed scan
            PrimaryScanCard(
                scan = createMockFailedScan()
            )
        }
    }
}