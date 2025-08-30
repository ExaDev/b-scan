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
 * Display raw tag data in formatted hex dump (encoded data as received)
 */
@Composable
fun EncodedDataView(
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
 * Display combined decoded and decrypted data view (comprehensive view)
 */
@Composable
fun DecodedDecryptedDataView(
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
                text = "Complete Scan Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Basic tag information (decoded)
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
        
        // Scan performance and timing (decoded)
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
        
        // Authentication results (decoded + decrypted)
        item {
            AuthenticationResultsCard(
                authenticatedSectors = decryptedScanData.authenticatedSectors,
                failedSectors = decryptedScanData.failedSectors,
                usedKeys = decryptedScanData.usedKeys
            )
        }
        
        // Derived keys (decrypted)
        if (decryptedScanData.derivedKeys.isNotEmpty()) {
            item {
                DerivedKeysCard(derivedKeys = decryptedScanData.derivedKeys)
            }
        }
        
        // Decrypted blocks (decrypted)
        if (decryptedScanData.decryptedBlocks.isNotEmpty()) {
            item {
                DecryptedBlocksCard(blocks = decryptedScanData.decryptedBlocks)
            }
        }
        
        // Raw hex dump (encoded)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Raw Hex Dump",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SelectionContainer {
                        Text(
                            text = formatHexDump(encryptedScanData.encryptedData),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
        
        // Error messages (if any)
        if (decryptedScanData.errors.isNotEmpty()) {
            item {
                ErrorMessagesCard(errors = decryptedScanData.errors)
            }
        }
    }
}

/**
 * Display decrypted data as raw hex dump (encoded view of decrypted blocks)
 */
@Composable
fun DecryptedEncodedDataView(
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
                text = "Decrypted Block Data (Raw Hex)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (decryptedScanData.decryptedBlocks.isNotEmpty()) {
            items(decryptedScanData.decryptedBlocks.entries.sortedBy { it.key }.toList()) { (blockNumber, hexData) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Block $blockNumber",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        SelectionContainer {
                            Text(
                                text = formatDecryptedBlockHex(hexData),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "No decrypted blocks available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Show derived keys if available
        if (decryptedScanData.derivedKeys.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Derived Keys (Hex)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        decryptedScanData.derivedKeys.forEachIndexed { index, key ->
                            SelectionContainer {
                                Text(
                                    text = "Key $index: $key",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display meaningful interpreted Bambu filament data from decrypted scan data
 */
@Composable
fun DecryptedDecodedDataView(
    decryptedScanData: DecryptedScanData,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Get the interpreted filament data
    val filamentInfo = remember(decryptedScanData.tagUid) {
        try {
            val catalogRepository = com.bscan.repository.CatalogRepository(context)
            val userRepository = com.bscan.repository.UserDataRepository(context)
            val scanRepository = com.bscan.repository.ScanHistoryRepository(context)
            val unifiedDataAccess = com.bscan.repository.UnifiedDataAccess(catalogRepository, userRepository, scanRepository)
            val interpreterFactory = com.bscan.interpreter.InterpreterFactory(unifiedDataAccess)
            
            val interpreter = interpreterFactory.getInterpreter(decryptedScanData.tagFormat)
            interpreter?.interpret(decryptedScanData)
        } catch (e: Exception) {
            null
        }
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Interpreted Filament Data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (filamentInfo != null) {
            // Basic filament information
            item {
                MetadataCard(
                    title = "Filament Details",
                    items = listOf(
                        "Material Type" to filamentInfo.filamentType,
                        "Detailed Type" to filamentInfo.detailedFilamentType,
                        "Color" to filamentInfo.colorName,
                        "Color Hex" to filamentInfo.colorHex,
                        "Diameter" to "${filamentInfo.filamentDiameter}mm",
                        "Length" to "${filamentInfo.filamentLength}mm"
                    )
                )
            }
            
            // Temperature settings
            item {
                MetadataCard(
                    title = "Temperature Settings",
                    items = listOf(
                        "Nozzle Min" to "${filamentInfo.minTemperature}°C",
                        "Nozzle Max" to "${filamentInfo.maxTemperature}°C",
                        "Bed Temperature" to "${filamentInfo.bedTemperature}°C",
                        "Drying Temperature" to "${filamentInfo.dryingTemperature}°C",
                        "Drying Time" to "${filamentInfo.dryingTime}h"
                    )
                )
            }
            
            // Manufacturing info
            item {
                MetadataCard(
                    title = "Manufacturing Info",
                    items = buildList {
                        add("Manufacturer" to filamentInfo.manufacturerName)
                        add("Production Date" to filamentInfo.productionDate)
                        add("Spool Weight" to "${filamentInfo.spoolWeight}g")
                        if (filamentInfo.exactSku != null) {
                            add("Exact SKU" to filamentInfo.exactSku)
                        }
                        if (filamentInfo.rfidCode != null) {
                            add("RFID Code" to filamentInfo.rfidCode)
                        }
                    }
                )
            }
            
            // Advanced properties
            if (filamentInfo.materialId.isNotEmpty() || filamentInfo.materialVariantId.isNotEmpty()) {
                item {
                    MetadataCard(
                        title = "Advanced Properties",
                        items = buildList {
                            if (filamentInfo.materialId.isNotEmpty()) {
                                add("Material ID" to filamentInfo.materialId)
                            }
                            if (filamentInfo.materialVariantId.isNotEmpty()) {
                                add("Variant ID" to filamentInfo.materialVariantId)
                            }
                            add("Nozzle Diameter" to "${filamentInfo.nozzleDiameter}mm")
                            if (filamentInfo.spoolWidth > 0) {
                                add("Spool Width" to "${filamentInfo.spoolWidth}mm")
                            }
                            add("Color Count" to filamentInfo.colorCount.toString())
                        }
                    )
                }
            }
            
            // Tag identifiers
            item {
                MetadataCard(
                    title = "Tag Identifiers",
                    items = listOf(
                        "Tag UID" to filamentInfo.tagUid,
                        "Tray UID" to filamentInfo.trayUid,
                        "Tag Format" to filamentInfo.tagFormat.name
                    )
                )
            }
            
        } else {
            // Show interpretation status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Interpretation Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Unable to interpret decrypted data as meaningful filament information.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Tag Format: ${decryptedScanData.tagFormat.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Scan Result: ${decryptedScanData.scanResult.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Technology: ${decryptedScanData.technology}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Decrypted Blocks: ${decryptedScanData.decryptedBlocks.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Authenticated Sectors: ${decryptedScanData.authenticatedSectors.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        Text(
                            text = "Failed Sectors: ${decryptedScanData.failedSectors.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Format hex string as formatted hex dump with spacing
 */
private fun formatDecryptedBlockHex(hexString: String): String {
    // Remove any existing spaces and format as hex dump
    val cleanHex = hexString.replace(" ", "").uppercase()
    return cleanHex.chunked(32) { row ->
        row.chunked(2).joinToString(" ")
    }.joinToString("\n")
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