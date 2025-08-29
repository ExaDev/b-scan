package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.sectorCount
import java.time.LocalDateTime

/**
 * Card displaying authentication results and sector information for an RFID tag.
 */
@Composable
fun TagAuthenticationCard(
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
                text = "Authentication Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            DetailInfoRow(
                label = "Authenticated Sectors", 
                value = "${tag.decryptedData.authenticatedSectors.size}/${tag.decryptedData.sectorCount}"
            )
            
            if (tag.decryptedData.authenticatedSectors.isNotEmpty()) {
                DetailInfoRow(
                    label = "Success Sectors", 
                    value = tag.decryptedData.authenticatedSectors.sorted().joinToString(", ")
                )
            }
            
            if (tag.decryptedData.failedSectors.isNotEmpty()) {
                DetailInfoRow(
                    label = "Failed Sectors", 
                    value = tag.decryptedData.failedSectors.sorted().joinToString(", ")
                )
            }
            
            DetailInfoRow(
                label = "Derived Keys", 
                value = "${tag.decryptedData.derivedKeys.size} keys generated"
            )
            
            if (tag.decryptedData.errors.isNotEmpty()) {
                Text(
                    text = "Errors:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                tag.decryptedData.errors.take(3).forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (tag.decryptedData.errors.size > 3) {
                    Text(
                        text = "... and ${tag.decryptedData.errors.size - 3} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}



// Mock data with errors for preview
private fun createMockInterpretedScanWithErrors(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now(),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(0),
            scanDurationMs = 1250L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            timestamp = LocalDateTime.now(),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.SUCCESS,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = listOf(1, 2, 3, 4, 5),
            failedSectors = listOf(6, 7, 8),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA", 5 to "KeyA"),
            derivedKeys = listOf("ABCD1234567890EF", "1234ABCDEF567890", "FEDCBA0987654321"),
            errors = listOf(
                "Failed to authenticate sector 6: Invalid key",
                "Failed to authenticate sector 7: Timeout",
                "Failed to authenticate sector 8: Authentication error",
                "Block read failed for sector 9",
                "Memory allocation error"
            ),
            keyDerivationTimeMs = 450L,
            authenticationTimeMs = 350L
        ),
        filamentInfo = null
    )
}

// Mock data success case for preview
private fun createMockInterpretedScanSuccess(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now(),
            tagUid = "E1F2G3H4",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(0),
            scanDurationMs = 890L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            timestamp = LocalDateTime.now(),
            tagUid = "E1F2G3H4",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.SUCCESS,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = (1..15).toList(),
            failedSectors = emptyList(),
            usedKeys = (1..15).associate { it to if (it % 2 == 0) "KeyB" else "KeyA" },
            derivedKeys = listOf("ABCD1234567890EF", "1234ABCDEF567890", "FEDCBA0987654321", "567890ABCDEF1234"),
            errors = emptyList(),
            keyDerivationTimeMs = 320L,
            authenticationTimeMs = 280L
        ),
        filamentInfo = null
    )
}

@Preview(showBackground = true)
@Composable
private fun TagAuthenticationCardPreview() {
    MaterialTheme {
        TagAuthenticationCard(
            tag = createMockInterpretedScan(),
            modifier = Modifier.padding(16.dp)
        )
    }
}