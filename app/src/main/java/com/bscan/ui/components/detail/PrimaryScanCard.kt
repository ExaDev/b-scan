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
    scan: com.bscan.repository.InterpretedScan,
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
        
        scan.filamentInfo?.let { filamentInfo ->
            ColorPreviewCard(
                colorHex = filamentInfo.colorHex,
                colorName = filamentInfo.colorName,
                filamentType = filamentInfo.filamentType
            )
            
            InfoCard(
                title = "Filament Type",
                value = filamentInfo.detailedFilamentType.ifEmpty { filamentInfo.filamentType }
            )
            
            SpecificationCard(filamentInfo = filamentInfo)
            TemperatureCard(filamentInfo = filamentInfo)
            ProductionInfoCard(filamentInfo = filamentInfo)
        }
    }
}



// Mock data with filament info for preview
private fun createMockInterpretedScanWithFilament(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now().minusHours(2),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(1024),
            scanDurationMs = 1250L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
            timestamp = LocalDateTime.now().minusHours(2),
            tagUid = "A1B2C3D4",
            technology = "MIFARE Classic 1K",
            scanResult = com.bscan.model.ScanResult.SUCCESS,
            decryptedBlocks = emptyMap(),
            authenticatedSectors = listOf(1, 2, 3, 4, 5),
            failedSectors = emptyList(),
            usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyB", 4 to "KeyA", 5 to "KeyA"),
            derivedKeys = listOf("ABCD1234567890EF"),
            errors = emptyList(),
            keyDerivationTimeMs = 450L,
            authenticationTimeMs = 350L
        ),
        filamentInfo = com.bscan.model.FilamentInfo(
            tagUid = "A1B2C3D4",
            trayUid = "01008023456789AB",
            filamentType = "PLA",
            detailedFilamentType = "PLA Basic",
            colorName = "Marble Orange",
            colorHex = "#FF8B42",
            spoolWeight = 245,
            filamentDiameter = 1.75f,
            filamentLength = 330,
            productionDate = "2024-01-15",
            minTemperature = 210,
            maxTemperature = 230,
            bedTemperature = 60,
            dryingTemperature = 45,
            dryingTime = 8
        )
    )
}

// Mock data without filament info for preview
private fun createMockInterpretedScanWithoutFilament(): com.bscan.repository.InterpretedScan {
    return com.bscan.repository.InterpretedScan(
        encryptedData = com.bscan.model.EncryptedScanData(
            timestamp = LocalDateTime.now().minusMinutes(30),
            tagUid = "E1F2G3H4",
            technology = "MIFARE Classic 1K",
            encryptedData = ByteArray(512),
            scanDurationMs = 2100L
        ),
        decryptedData = com.bscan.model.DecryptedScanData(
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
        ),
        filamentInfo = null
    )
}