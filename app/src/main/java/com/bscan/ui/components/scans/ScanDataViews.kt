package com.bscan.ui.components.scans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData

/**
 * Display raw tag data in formatted hex dump
 */
@Composable
fun RawDataView(
    encryptedScanData: EncryptedScanData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Raw Tag Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Text(
                text = "Technology: ${encryptedScanData.technology}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        item {
            Text(
                text = "Tag UID: ${encryptedScanData.tagUid}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
        
        item {
            Text(
                text = "Data Size: ${encryptedScanData.encryptedData.size} bytes",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hex Dump:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                SelectionContainer {
                    Text(
                        text = formatHexDump(encryptedScanData.encryptedData),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Display decoded tag information (readable metadata and non-encrypted data)
 */
@Composable
fun DecodedDataView(
    encryptedScanData: EncryptedScanData,
    decryptedScanData: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Decoded Tag Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Basic tag information (readable without decryption)
        item {
            MetadataCard(
                title = "Tag Information",
                items = listOf(
                    "Tag UID" to encryptedScanData.tagUid,
                    "Technology" to encryptedScanData.technology,
                    "Format" to decryptedScanData.tagFormat.name,
                    "Data Size" to "${encryptedScanData.encryptedData.size} bytes"
                )
            )
        }
        
        // Scan timing information  
        item {
            MetadataCard(
                title = "Scan Performance",
                items = buildList {
                    add("Timestamp" to encryptedScanData.timestamp.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    add("Scan Duration" to "${encryptedScanData.scanDurationMs}ms")
                    if (decryptedScanData.keyDerivationTimeMs > 0) {
                        add("Key Derivation" to "${decryptedScanData.keyDerivationTimeMs}ms")
                    }
                    if (decryptedScanData.authenticationTimeMs > 0) {
                        add("Authentication" to "${decryptedScanData.authenticationTimeMs}ms")
                    }
                }
            )
        }
        
        // Scan result and status
        item {
            MetadataCard(
                title = "Scan Results",
                items = listOf(
                    "Result" to decryptedScanData.scanResult.name,
                    "Authenticated Sectors" to if (decryptedScanData.authenticatedSectors.isNotEmpty()) 
                        decryptedScanData.authenticatedSectors.joinToString(", ") else "None",
                    "Failed Sectors" to if (decryptedScanData.failedSectors.isNotEmpty()) 
                        decryptedScanData.failedSectors.joinToString(", ") else "None"
                )
            )
        }
        
        // Key usage information (metadata about authentication)
        if (decryptedScanData.usedKeys.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Authentication Method",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        decryptedScanData.usedKeys.forEach { (sector, keyType) ->
                            SelectionContainer {
                                Text(
                                    text = "Sector $sector: $keyType",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Error information (non-encrypted diagnostic data)
        if (decryptedScanData.errors.isNotEmpty()) {
            item {
                ErrorMessagesCard(errors = decryptedScanData.errors)
            }
        }
    }
}

/**
 * Display decrypted scan data with authentication results and blocks
 */
@Composable
fun DecryptedDataView(
    decryptedScanData: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Decrypted Scan Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        item {
            MetadataCard(
                title = "Scan Results",
                items = listOf(
                    "Scan Result" to decryptedScanData.scanResult.name,
                    "Tag Format" to decryptedScanData.tagFormat.name,
                    "Technology" to decryptedScanData.technology,
                    "Timestamp" to decryptedScanData.timestamp.toString()
                )
            )
        }
        
        // Authentication Results
        item {
            AuthenticationResultsCard(
                authenticatedSectors = decryptedScanData.authenticatedSectors,
                failedSectors = decryptedScanData.failedSectors,
                usedKeys = decryptedScanData.usedKeys
            )
        }
        
        // Derived Keys
        if (decryptedScanData.derivedKeys.isNotEmpty()) {
            item {
                DerivedKeysCard(derivedKeys = decryptedScanData.derivedKeys)
            }
        }
        
        // Decrypted Blocks
        if (decryptedScanData.decryptedBlocks.isNotEmpty()) {
            item {
                DecryptedBlocksCard(blocks = decryptedScanData.decryptedBlocks)
            }
        }
        
        // Error Messages
        if (decryptedScanData.errors.isNotEmpty()) {
            item {
                ErrorMessagesCard(errors = decryptedScanData.errors)
            }
        }
    }
}

@Composable
private fun MetadataCard(
    title: String,
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$label:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    SelectionContainer {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticationResultsCard(
    authenticatedSectors: List<Int>,
    failedSectors: List<Int>,
    usedKeys: Map<Int, String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Authentication Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (authenticatedSectors.isNotEmpty()) {
                Text(
                    text = "Successful Sectors:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                SelectionContainer {
                    Text(
                        text = authenticatedSectors.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            
            if (failedSectors.isNotEmpty()) {
                Text(
                    text = "Failed Sectors:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error
                )
                SelectionContainer {
                    Text(
                        text = failedSectors.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
            
            if (usedKeys.isNotEmpty()) {
                Text(
                    text = "Used Keys:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                usedKeys.forEach { (sector, keyType) ->
                    SelectionContainer {
                        Text(
                            text = "Sector $sector: $keyType",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DerivedKeysCard(
    derivedKeys: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Derived Keys",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            derivedKeys.forEachIndexed { index, key ->
                SelectionContainer {
                    Text(
                        text = "Key $index: $key",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun DecryptedBlocksCard(
    blocks: Map<Int, String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Decrypted Blocks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            blocks.entries.sortedBy { it.key }.forEach { (block, data) ->
                SelectionContainer {
                    Text(
                        text = "Block $block: $data",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorMessagesCard(
    errors: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Error Messages",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            errors.forEach { error ->
                Text(
                    text = "• $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Format byte array as hex dump with ASCII representation
 */
private fun formatHexDump(data: ByteArray): String {
    return data.toList().chunked(16) { row ->
        val hex = row.joinToString(" ") { "%02X".format(it) }
        val ascii = row.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
        "${hex.padEnd(48)} |$ascii|"
    }.joinToString("\n")
}