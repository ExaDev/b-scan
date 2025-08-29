package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.sectorCount
import com.bscan.model.tagSizeBytes
import com.bscan.model.tagFormat
import com.bscan.model.manufacturerName
import java.time.LocalDateTime

/**
 * Card displaying technical details about an RFID tag including format, size, timing information.
 */
@Composable
fun TagTechnicalInfoCard(
    tag: com.bscan.repository.InterpretedScan,
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
            
            DetailInfoRow(label = "Tag Format", value = tag.decryptedData.tagFormat.name)
            DetailInfoRow(label = "Tag Size", value = "${tag.decryptedData.tagSizeBytes} bytes")
            DetailInfoRow(label = "Sector Count", value = "${tag.decryptedData.sectorCount} sectors")
            DetailInfoRow(label = "Scan Duration", value = "${tag.encryptedData.scanDurationMs}ms")
            DetailInfoRow(label = "Key Derivation Time", value = "${tag.decryptedData.keyDerivationTimeMs}ms")
            DetailInfoRow(label = "Authentication Time", value = "${tag.decryptedData.authenticationTimeMs}ms")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagTechnicalInfoCardPreview() {
    MaterialTheme {
        TagTechnicalInfoCard(
            tag = createMockInterpretedScan()
        )
    }
}

// Mock data for preview
private fun createMockInterpretedScan(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            id = 1L,
            timestamp = LocalDateTime.now(),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(1024),
            scanDurationMs = 1250L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            id = 1L,
            timestamp = LocalDateTime.now(),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.SUCCESS,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = listOf(1, 2, 3, 4, 5),
            failedSectors = listOf(6, 7),
            usedKeys = emptyMap(),
            derivedKeys = listOf("ABCD1234567890EF", "1234ABCDEF567890"),
            errors = emptyList(),
            keyDerivationTimeMs = 450L,
            authenticationTimeMs = 350L
        ),
        filamentInfo = null
    )
}