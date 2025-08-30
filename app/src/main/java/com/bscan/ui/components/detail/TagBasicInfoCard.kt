package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanResult
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Card displaying basic information about an RFID tag including UID, technology, and scan details.
 */
@Composable
fun TagBasicInfoCard(
    tag: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Basic Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DetailInfoRow(label = "Tag UID", value = tag.tagUid)
            DetailInfoRow(label = "Technology", value = tag.technology)
            DetailInfoRow(label = "Manufacturer", value = "Unknown")
            DetailInfoRow(
                label = "Last Scanned", 
                value = tag.timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            )
            DetailInfoRow(label = "Scan Result", value = tag.scanResult.name.replace('_', ' '))
        }
    }
}


// Mock data for preview
private fun createMockDecryptedScanData(): DecryptedScanData {
    return DecryptedScanData(
        timestamp = LocalDateTime.now(),
        tagUid = "A1B2C3D4",
        technology = "MIFARE Classic 1K",
        scanResult = ScanResult.SUCCESS,
        decryptedBlocks = emptyMap(),
        authenticatedSectors = listOf(1, 2, 3),
        failedSectors = emptyList(),
        usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyB"),
        derivedKeys = listOf("ABCD1234567890EF", "1234ABCDEF567890"),
        errors = emptyList(),
        keyDerivationTimeMs = 450L,
        authenticationTimeMs = 350L
    )
}

@Preview(showBackground = true)
@Composable
private fun TagBasicInfoCardPreview() {
    MaterialTheme {
        TagBasicInfoCard(
            tag = createMockDecryptedScanData(),
            modifier = Modifier.padding(16.dp)
        )
    }
}