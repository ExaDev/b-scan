package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.ScanResult
import com.bscan.model.TagFormat
import java.time.LocalDateTime

/**
 * Primary card for displaying detailed tag information including basic info, technical details,
 * authentication results, raw data, and interpreted filament data if available.
 */
@Composable
fun PrimaryTagCard(
    tag: com.bscan.repository.InterpretedScan,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tag Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Basic Tag Information
        TagBasicInfoCard(tag = tag)
        
        // Scan Status and Technical Details
        TagTechnicalInfoCard(tag = tag)
        
        // Authentication Details
        TagAuthenticationCard(tag = tag)
        
        // Raw Data Section (expandable)
        TagRawDataCard(tag = tag)
        
        // Filament Information (if available)
        tag.filamentInfo?.let { filamentInfo ->
            InterpretedFilamentDataCard(filamentInfo = filamentInfo)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryTagCardWithFilamentPreview() {
    MaterialTheme {
        PrimaryTagCard(
            tag = createMockTagWithFilament()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryTagCardWithoutFilamentPreview() {
    MaterialTheme {
        PrimaryTagCard(
            tag = createMockTagWithoutFilament()
        )
    }
}

// Mock data with filament info for preview
private fun createMockTagWithFilament(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now().minusHours(1),
            tagUid = "A1B2C3D4E5F6G7H8",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(1024),
            scanDurationMs = 1150L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            timestamp = LocalDateTime.now().minusHours(1),
            tagUid = "A1B2C3D4E5F6G7H8",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.SUCCESS,
            decryptedBlocks = mapOf(
                4 to "424D4C00474642303000000000000000",
                5 to "4B30000000000000000000000000000",
                6 to "010080230000000000000000000000000",
                7 to "5041534C61637465DABE656E20466961"
            ),
            authenticatedSectors = (1..15).toList(),
            failedSectors = emptyList(),
            usedKeys = (1..15).associate { it to if (it % 2 == 0) "KeyB" else "KeyA" },
            derivedKeys = listOf(
                "ABCD1234567890EF1234567890ABCDEF",
                "1234ABCDEF567890FEDCBA0987654321",
                "FEDCBA0987654321ABCD1234567890EF"
            ),
            errors = emptyList(),
            keyDerivationTimeMs = 420L,
            authenticationTimeMs = 310L
        ),
        filamentInfo = com.bscan.model.FilamentInfo(
            tagUid = "A1B2C3D4E5F6G7H8",
            trayUid = "01008023456789AB",
            filamentType = "PETG",
            detailedFilamentType = "PETG Basic",
            colorName = "Jade White",
            colorHex = "#F5F5DC",
            spoolWeight = 220,
            filamentDiameter = 1.75f,
            filamentLength = 320,
            productionDate = "2024-02-20",
            minTemperature = 220,
            maxTemperature = 250,
            bedTemperature = 70,
            dryingTemperature = 55,
            dryingTime = 6
        )
    )
}

// Mock data without filament info for preview
private fun createMockTagWithoutFilament(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now().minusMinutes(45),
            tagUid = "1122334455667788",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(256),
            scanDurationMs = 3200L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            timestamp = LocalDateTime.now().minusMinutes(45),
            tagUid = "1122334455667788",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.AUTHENTICATION_FAILED,
            decryptedBlocks = mapOf(
                1 to "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
            ),
            authenticatedSectors = listOf(1),
            failedSectors = (2..15).toList(),
            usedKeys = mapOf(1 to "KeyA"),
            derivedKeys = listOf("1234567890ABCDEF"),
            errors = listOf(
                "Authentication failed for sector 2",
                "Authentication failed for sector 3",
                "Authentication timeout"
            ),
            keyDerivationTimeMs = 750L,
            authenticationTimeMs = 1200L
        ),
        filamentInfo = null
    )
}