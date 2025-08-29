package com.bscan.ui.components.detail

import androidx.compose.foundation.clickable
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
import com.bscan.ui.components.history.ScanHistoryCard
import com.bscan.ui.screens.DetailType
import com.bscan.ui.screens.home.*
import java.time.LocalDateTime

/**
 * Section displaying related filament reels for detail views.
 */
@Composable
fun RelatedFilamentReelsSection(
    filamentReels: List<com.bscan.repository.UniqueFilamentReel>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related Inventory Stock (${filamentReels.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        filamentReels.forEach { filamentReel ->
            FilamentReelCard(
                filamentReel = filamentReel,
                onClick = { trayUid ->
                    onNavigateToDetails?.invoke(DetailType.INVENTORY_STOCK, trayUid)
                }
            )
        }
    }
}

/**
 * Section displaying related RFID tags for detail views.
 */
@Composable
fun RelatedTagsSection(
    tagUids: List<String>,
    allScans: List<com.bscan.repository.InterpretedScan>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related Tags (${tagUids.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        tagUids.forEach { tagUid ->
            val mostRecentScan = allScans.filter { it.uid == tagUid }.maxByOrNull { it.timestamp }
            if (mostRecentScan != null) {
                TagCard(
                    uid = tagUid,
                    mostRecentScan = mostRecentScan,
                    filamentInfo = mostRecentScan.filamentInfo,
                    allScans = listOf(mostRecentScan),
                    modifier = Modifier.clickable {
                        onNavigateToDetails?.invoke(DetailType.TAG, tagUid)
                    }
                )
            }
        }
    }
}

/**
 * Section displaying associated SKUs for detail views.
 */
@Composable
fun AssociatedSkuSection(
    skus: List<com.bscan.ui.screens.home.SkuInfo>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Associated SKU",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        skus.forEach { sku ->
            SkuCard(
                sku = sku,
                modifier = Modifier.clickable {
                    onNavigateToDetails?.invoke(DetailType.SKU, sku.skuKey)
                }
            )
        }
    }
}

/**
 * Section displaying related scan history for detail views.
 */
@Composable
fun RelatedScansSection(
    scans: List<com.bscan.repository.InterpretedScan>,
    onNavigateToDetails: ((DetailType, String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Related Scans (${scans.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        scans.forEach { scan ->
            var expanded by remember { mutableStateOf(false) }
            ScanHistoryCard(
                scan = scan,
                isExpanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onScanClick = { detailType, scanId ->
                    onNavigateToDetails?.invoke(detailType, scanId)
                }
            )
        }
    }
}





// Mock data for previews
private fun createMockFilamentReels(): List<com.bscan.repository.UniqueFilamentReel> {
    return listOf(
        com.bscan.repository.UniqueFilamentReel(
            uid = "01008023456789AB",
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
            ),
            scanCount = 15,
            successCount = 14,
            lastScanned = LocalDateTime.now().minusDays(1),
            successRate = 0.93f
        ),
        com.bscan.repository.UniqueFilamentReel(
            uid = "01008023987654CD",
            filamentInfo = com.bscan.model.FilamentInfo(
                tagUid = "E5F6G7H8",
                trayUid = "01008023987654CD",
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
            ),
            scanCount = 8,
            successCount = 6,
            lastScanned = LocalDateTime.now().minusHours(3),
            successRate = 0.75f
        )
    )
}

private fun createMockScans(): List<com.bscan.repository.InterpretedScan> {
    return listOf(
        com.bscan.repository.InterpretedScan(
            encryptedData = com.bscan.model.EncryptedScanData(
                timestamp = LocalDateTime.now().minusHours(2),
                tagUid = "A1B2C3D4",
                technology = "MIFARE Classic 1K",
                encryptedData = ByteArray(0),
                scanDurationMs = 1250L
            ),
            decryptedData = com.bscan.model.DecryptedScanData(
                timestamp = LocalDateTime.now().minusHours(2),
                tagUid = "A1B2C3D4",
                technology = "MIFARE Classic 1K",
                scanResult = com.bscan.model.ScanResult.SUCCESS,
                decryptedBlocks = emptyMap(),
                authenticatedSectors = listOf(1, 2, 3),
                failedSectors = emptyList(),
                usedKeys = mapOf(1 to "KeyA", 2 to "KeyA", 3 to "KeyB"),
                derivedKeys = listOf("ABCD1234567890EF"),
                errors = emptyList(),
                keyDerivationTimeMs = 450L,
                authenticationTimeMs = 350L
            ),
            filamentInfo = null
        ),
        com.bscan.repository.InterpretedScan(
            encryptedData = com.bscan.model.EncryptedScanData(
                timestamp = LocalDateTime.now().minusDays(1),
                tagUid = "E5F6G7H8",
                technology = "MIFARE Classic 1K",
                encryptedData = ByteArray(0),
                scanDurationMs = 1800L
            ),
            decryptedData = com.bscan.model.DecryptedScanData(
                timestamp = LocalDateTime.now().minusDays(1),
                tagUid = "E5F6G7H8",
                technology = "MIFARE Classic 1K",
                scanResult = com.bscan.model.ScanResult.PARSING_FAILED,
                decryptedBlocks = emptyMap(),
                authenticatedSectors = listOf(1, 2),
                failedSectors = listOf(3, 4),
                usedKeys = mapOf(1 to "KeyA", 2 to "KeyB"),
                derivedKeys = listOf("1234ABCDEF567890"),
                errors = listOf("Failed to authenticate sector 3"),
                keyDerivationTimeMs = 650L,
                authenticationTimeMs = 580L
            ),
            filamentInfo = null
        )
    )
}

private fun createMockSkus(): List<com.bscan.ui.screens.home.SkuInfo> {
    return listOf(
        com.bscan.ui.screens.home.SkuInfo(
            skuKey = "PLA-Basic-Orange",
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
            ),
            filamentReelCount = 3,
            totalScans = 47,
            successfulScans = 44,
            successRate = 0.94f,
            lastScanned = LocalDateTime.now().minusDays(2)
        )
    )
}