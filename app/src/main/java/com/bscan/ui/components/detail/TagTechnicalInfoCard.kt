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

/**
 * Card displaying technical details about an RFID tag including format, size, timing information.
 */
@Composable
fun TagTechnicalInfoCard(
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
                text = "Technical Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DetailInfoRow(label = "Technology", value = tag.technology)
            DetailInfoRow(label = "Sector Count", value = "16 sectors")
            DetailInfoRow(label = "Key Derivation Time", value = "${tag.keyDerivationTimeMs}ms")
            DetailInfoRow(label = "Authentication Time", value = "${tag.authenticationTimeMs}ms")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagTechnicalInfoCardPreview() {
    MaterialTheme {
        TagTechnicalInfoCard(
            tag = createMockDecryptedScanData()
        )
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
        authenticatedSectors = listOf(1, 2, 3, 4, 5),
        failedSectors = listOf(6, 7),
        usedKeys = emptyMap(),
        derivedKeys = listOf("ABCD1234567890EF", "1234ABCDEF567890"),
        errors = emptyList(),
        keyDerivationTimeMs = 450L,
        authenticationTimeMs = 350L
    )
}