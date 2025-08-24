package com.bscan.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.model.ScanResult
import com.bscan.repository.UniqueSpool
import com.bscan.repository.InterpretedScan
import com.bscan.ui.components.ScanStateIndicator
import com.bscan.ui.components.FilamentColorBox
import java.time.format.DateTimeFormatter

data class SkuInfo(
    val skuKey: String,
    val filamentInfo: com.bscan.model.FilamentInfo,
    val spoolCount: Int,
    val totalScans: Int,
    val successfulScans: Int,
    val lastScanned: java.time.LocalDateTime,
    val successRate: Float,
    val isScannedOnly: Boolean = false // True if scanned from tag but no matching catalog product
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpoolCard(
    spool: UniqueSpool,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onClick?.invoke(spool.filamentInfo.trayUid) }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            FilamentColorBox(
                colorHex = spool.filamentInfo.colorHex,
                filamentType = spool.filamentInfo.filamentType,
                size = 48.dp
            )
            
            // Filament info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spool.filamentInfo.colorName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${spool.filamentInfo.filamentType} • ${spool.filamentInfo.trayUid}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "${spool.scanCount} scans • ${(spool.successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Success rate indicator
            when {
                spool.successRate >= 0.9f -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "High success rate",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                spool.successRate >= 0.7f -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Good success rate", 
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Low success rate",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagCard(
    uid: String,
    mostRecentScan: InterpretedScan,
    filamentInfo: com.bscan.model.FilamentInfo?,
    allScans: List<InterpretedScan>,
    modifier: Modifier = Modifier
) {
    val tagScans = allScans.filter { it.uid == uid }
    val successRate = tagScans.count { it.scanResult == ScanResult.SUCCESS }.toFloat() / tagScans.size
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tag icon with color if available
            if (filamentInfo != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FilamentColorBox(
                        colorHex = filamentInfo.colorHex,
                        filamentType = filamentInfo.filamentType,
                        size = 40.dp
                    )
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Tag info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = uid,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                
                if (filamentInfo != null) {
                    Text(
                        text = "${filamentInfo.colorName} • ${filamentInfo.trayUid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Unknown filament",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = "${tagScans.size} scans • ${(successRate * 100).toInt()}% success",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Result indicator
            Icon(
                imageVector = when (mostRecentScan.scanResult) {
                    ScanResult.SUCCESS -> Icons.Default.CheckCircle
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (mostRecentScan.scanResult) {
                    ScanResult.SUCCESS -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanCard(
    scan: InterpretedScan,
    modifier: Modifier = Modifier,
    onClick: ((InterpretedScan) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(scan) }
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Result icon
            Icon(
                imageVector = when (scan.scanResult) {
                    ScanResult.SUCCESS -> Icons.Default.CheckCircle
                    ScanResult.AUTHENTICATION_FAILED -> Icons.Default.Lock
                    ScanResult.INSUFFICIENT_DATA -> Icons.Default.Warning
                    ScanResult.PARSING_FAILED -> Icons.Default.Error
                    ScanResult.NO_NFC_TAG -> Icons.Default.SignalWifiOff
                    ScanResult.UNKNOWN_ERROR -> Icons.AutoMirrored.Filled.Help
                },
                contentDescription = null,
                tint = when (scan.scanResult) {
                    ScanResult.SUCCESS -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier.size(32.dp)
            )
            
            // Scan info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                if (scan.filamentInfo != null) {
                    Text(
                        text = scan.filamentInfo.colorName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${scan.filamentInfo.filamentType} • ${scan.uid}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = scan.uid,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = scan.scanResult.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Text(
                    text = scan.timestamp.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkuCard(
    sku: SkuInfo,
    modifier: Modifier = Modifier,
    onClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick(sku.skuKey) }
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            FilamentColorBox(
                colorHex = sku.filamentInfo.colorHex,
                filamentType = sku.filamentInfo.filamentType,
                size = 48.dp
            )
            
            // SKU info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = sku.filamentInfo.colorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show indicator for scanned-only products (not in catalog)
                    if (sku.isScannedOnly) {
                        Icon(
                            imageVector = Icons.Default.QuestionMark,
                            contentDescription = "Scanned product (not in catalog)",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Text(
                    text = sku.filamentInfo.filamentType,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = if (sku.spoolCount == 0 && sku.totalScans == 0) {
                        "Available • Not scanned"
                    } else {
                        "${sku.spoolCount} spool${if (sku.spoolCount != 1) "s" else ""} • ${sku.totalScans} scans • ${(sku.successRate * 100).toInt()}% success"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status indicator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    sku.spoolCount == 0 && sku.totalScans == 0 -> {
                        // Unscanned product - show shopping cart
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Available for purchase",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    sku.successRate >= 0.9f -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "High success rate",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    sku.successRate >= 0.7f -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Good success rate", 
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Low success rate",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Text(
                    text = if (sku.spoolCount == 0 && sku.totalScans == 0) "0" else "${sku.spoolCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CompactScanPrompt(
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onLongClick = onLongPress
            ) {},
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScanStateIndicator(
                    isIdle = scanState == ScanState.IDLE,
                    isDetected = scanState == ScanState.TAG_DETECTED,
                    isProcessing = scanState == ScanState.PROCESSING,
                    modifier = Modifier.size(48.dp)
                )
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val title = when (scanState) {
                        ScanState.IDLE -> "Scan a Spool"
                        ScanState.TAG_DETECTED -> "Tag Detected"
                        ScanState.PROCESSING -> "Scanning..."
                        ScanState.SUCCESS -> "Scan Complete"
                        ScanState.ERROR -> "Scan Failed"
                    }
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    
                    val description = when (scanState) {
                        ScanState.IDLE -> "Tap your device against a filament spool to read its information"
                        ScanState.TAG_DETECTED -> "Preparing to read tag data"
                        ScanState.PROCESSING -> scanProgress?.statusMessage ?: "Processing tag data"
                        ScanState.SUCCESS -> "Filament information successfully read"
                        ScanState.ERROR -> "Unable to read tag data"
                    }
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                val iconVector = when (scanState) {
                    ScanState.IDLE -> Icons.Default.NearMe
                    ScanState.TAG_DETECTED -> Icons.Default.CheckCircle
                    ScanState.PROCESSING -> Icons.Default.HourglassEmpty
                    ScanState.SUCCESS -> Icons.Default.CheckCircle
                    ScanState.ERROR -> Icons.Default.Error
                }
                
                val iconColor = when (scanState) {
                    ScanState.IDLE -> MaterialTheme.colorScheme.primary
                    ScanState.TAG_DETECTED -> MaterialTheme.colorScheme.secondary
                    ScanState.PROCESSING -> MaterialTheme.colorScheme.primary
                    ScanState.SUCCESS -> MaterialTheme.colorScheme.primary
                    ScanState.ERROR -> MaterialTheme.colorScheme.error
                }
                
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Show progress bar when scanning
            if (scanState == ScanState.PROCESSING && scanProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { scanProgress.percentage },
                    modifier = Modifier.fillMaxWidth()
                )
                if (scanProgress.currentSector > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sector ${scanProgress.currentSector}/${scanProgress.totalSectors}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}