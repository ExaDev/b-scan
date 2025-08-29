package com.bscan.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.ui.components.ColorPreviewCard
import com.bscan.ui.components.InfoCard
import com.bscan.ui.components.SpecificationCard
import com.bscan.ui.components.TemperatureCard
import com.bscan.ui.components.filament.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Primary card for displaying SKU information including filament details, statistics, and specifications.
 */
@Composable
fun PrimarySkuCard(
    sku: com.bscan.ui.screens.home.SkuInfo,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "SKU Information",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        ColorPreviewCard(
            colorHex = sku.filamentInfo.colorHex,
            colorName = sku.filamentInfo.colorName,
            filamentType = sku.filamentInfo.filamentType
        )
        
        InfoCard(
            title = "Filament Type",
            value = sku.filamentInfo.detailedFilamentType.ifEmpty { sku.filamentInfo.filamentType }
        )
        
        InfoCard(
            title = "Unique Reels",
            value = "${sku.filamentReelCount} reels"
        )
        
        InfoCard(
            title = "Total Scans",
            value = "${sku.totalScans} scans"
        )
        
        InfoCard(
            title = "Success Rate",
            value = "${(sku.successRate * 100).toInt()}%"
        )
        
        InfoCard(
            title = "Last Scanned",
            value = sku.lastScanned.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        )
        
        SpecificationCard(filamentInfo = sku.filamentInfo)
        TemperatureCard(filamentInfo = sku.filamentInfo)
        ProductionInfoCard(filamentInfo = sku.filamentInfo)
    }
}



// Mock data for preview
private fun createMockSkuInfo(): com.bscan.ui.screens.home.SkuInfo {
    return com.bscan.ui.screens.home.SkuInfo(
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
}

// Mock data with low success rate for preview
private fun createMockSkuInfoLowSuccess(): com.bscan.ui.screens.home.SkuInfo {
    return com.bscan.ui.screens.home.SkuInfo(
        skuKey = "ABS-Carbon-Fiber-Black",
        filamentInfo = com.bscan.model.FilamentInfo(
            tagUid = "E1F2G3H4",
            trayUid = "01008023987654CD",
            filamentType = "ABS",
            detailedFilamentType = "ABS Carbon Fiber",
            colorName = "Carbon Black",
            colorHex = "#1C1C1C",
            spoolWeight = 280,
            filamentDiameter = 1.75f,
            filamentLength = 295,
            productionDate = "2024-03-20",
            minTemperature = 250,
            maxTemperature = 280,
            bedTemperature = 90,
            dryingTemperature = 60,
            dryingTime = 4
        ),
        filamentReelCount = 1,
        totalScans = 23,
        successfulScans = 15,
        successRate = 0.65f,
        lastScanned = LocalDateTime.now().minusHours(5)
    )
}