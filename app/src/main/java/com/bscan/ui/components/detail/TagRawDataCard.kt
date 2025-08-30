package com.bscan.ui.components.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanResult
import java.time.LocalDateTime

/**
 * Expandable card displaying raw data from RFID tag including decrypted blocks and derived keys.
 * Includes copy functionality for individual items and complete data export.
 */
@Composable
fun TagRawDataCard(
    tag: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAllBlocks by remember { mutableStateOf(false) }
    var showAllKeys by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (expanded) "Hide" else "Show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (expanded) {
                DetailInfoRow(
                    label = "Decrypted Blocks", 
                    value = "${tag.decryptedBlocks.size} blocks"
                )
                
                // Show decrypted blocks
                if (tag.decryptedBlocks.isNotEmpty()) {
                    DecryptedBlocksSection(
                        blocks = tag.decryptedBlocks,
                        showAllBlocks = showAllBlocks,
                        onToggleShowAll = { showAllBlocks = !showAllBlocks },
                        clipboardManager = clipboardManager
                    )
                }
                
                // Show derived keys (truncated for security)
                if (tag.derivedKeys.isNotEmpty()) {
                    DerivedKeysSection(
                        keys = tag.derivedKeys,
                        showAllKeys = showAllKeys,
                        onToggleShowAll = { showAllKeys = !showAllKeys },
                        clipboardManager = clipboardManager
                    )
                }
                
                // Copy all raw data button
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = {
                        val allRawData = buildAllRawDataString(tag)
                        clipboardManager.setText(AnnotatedString(allRawData))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy All Raw Data")
                }
            }
        }
    }
}

@Composable
private fun DecryptedBlocksSection(
    blocks: Map<Int, String>,
    showAllBlocks: Boolean,
    onToggleShowAll: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Decrypted Blocks:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(
            onClick = {
                val allBlocksText = blocks.entries
                    .sortedBy { it.key }
                    .joinToString("\n") { (blockNum, data) ->
                        "Block $blockNum: $data"
                    }
                clipboardManager.setText(AnnotatedString(allBlocksText))
            }
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy all blocks",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    val blocksToShow = if (showAllBlocks) {
        blocks.entries.sortedBy { it.key }
    } else {
        blocks.entries.sortedBy { it.key }.take(3)
    }
    
    blocksToShow.forEach { (blockNum, data) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Block $blockNum: ${data.take(32)}${if (data.length > 32) "..." else ""}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString("Block $blockNum: $data"))
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy block $blockNum",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (blocks.size > 3) {
        Text(
            text = if (showAllBlocks) {
                "Hide additional blocks"
            } else {
                "... and ${blocks.size - 3} more blocks"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleShowAll() }
        )
    }
}

@Composable
private fun DerivedKeysSection(
    keys: List<String>,
    showAllKeys: Boolean,
    onToggleShowAll: () -> Unit,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Derived Keys:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        IconButton(
            onClick = {
                val allKeysText = keys.joinToString("\n")
                clipboardManager.setText(AnnotatedString(allKeysText))
            }
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy all keys",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    val keysToShow = if (showAllKeys) keys else keys.take(2)
    
    keysToShow.forEach { key ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${key.take(8)}...${key.takeLast(8)}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(key))
                }
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy key",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (keys.size > 2) {
        Text(
            text = if (showAllKeys) {
                "Hide additional keys"
            } else {
                "... and ${keys.size - 2} more keys"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onToggleShowAll() }
        )
    }
}

private fun buildAllRawDataString(tag: DecryptedScanData): String {
    return buildString {
        appendLine("=== TAG RAW DATA ===")
        appendLine("Tag UID: ${tag.tagUid}")
        appendLine("Technology: ${tag.technology}")
        appendLine("Scan Result: ${tag.scanResult}")
        appendLine()
        
        appendLine("=== DECRYPTED BLOCKS ===")
        tag.decryptedBlocks.entries.sortedBy { it.key }.forEach { (blockNum, data) ->
            appendLine("Block $blockNum: $data")
        }
        appendLine()
        
        appendLine("=== AUTHENTICATION ===")
        appendLine("Authenticated Sectors: ${tag.authenticatedSectors.sorted().joinToString(", ")}")
        appendLine("Failed Sectors: ${tag.failedSectors.sorted().joinToString(", ")}")
        appendLine()
        
        appendLine("=== DERIVED KEYS ===")
        tag.derivedKeys.forEach { key ->
            appendLine(key)
        }
        
        if (tag.errors.isNotEmpty()) {
            appendLine()
            appendLine("=== ERRORS ===")
            tag.errors.forEach { error ->
                appendLine("• $error")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TagRawDataCardPreview() {
    MaterialTheme {
        TagRawDataCard(
            tag = createMockDecryptedScanDataWithData()
        )
    }
}

// Mock data with blocks and keys for preview
private fun createMockDecryptedScanDataWithData(): DecryptedScanData {
    return DecryptedScanData(
        timestamp = LocalDateTime.now(),
        tagUid = "A1B2C3D4",
        technology = "MIFARE Classic 1K",
        scanResult = ScanResult.SUCCESS,
        decryptedBlocks = mapOf(
            4 to "424D4C00474642303000000000000000",
            5 to "4B30000000000000000000000000000",
            6 to "010080230000000000000000000000000",
            7 to "5041534C61637465DABE656E20466961",
            8 to "6D656E7400000000000000000000000"
        ),
        authenticatedSectors = listOf(1, 2, 3, 4, 5, 6, 7, 8),
        failedSectors = listOf(9),
        usedKeys = mapOf(1 to "KeyA", 2 to "KeyB"),
        derivedKeys = listOf(
            "ABCD1234567890EF1234567890ABCDEF",
            "1234ABCDEF567890FEDCBA0987654321",
            "FEDCBA0987654321ABCD1234567890EF",
            "567890ABCDEF1234EFCDAB0987654321"
        ),
        errors = listOf("Failed to authenticate sector 9: Timeout"),
        keyDerivationTimeMs = 450L,
        authenticationTimeMs = 350L
    )
}