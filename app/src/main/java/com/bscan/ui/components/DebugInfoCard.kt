package com.bscan.ui.components

import androidx.compose.foundation.background
import com.bscan.BuildConfig
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.os.Build
import android.content.Context
import android.os.Environment
import com.bscan.model.ScanDebugInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugInfoCard(
    debugInfo: ScanDebugInfo,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val debugText = formatDebugInfoAsText(debugInfo)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = "Debug Info",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Debug Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap copy to share with developers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Row {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(debugText))
                            Toast.makeText(context, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy debug info"
                        )
                    }
                    
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tag Information
                DebugSection(title = "Tag Information") {
                    DebugRow("Size", "${debugInfo.tagSizeBytes} bytes")
                    DebugRow("Sectors", debugInfo.sectorCount.toString())
                }
                
                // Authentication Status
                DebugSection(title = "Authentication Status") {
                    DebugRow("Authenticated Sectors", debugInfo.authenticatedSectors.joinToString(", "))
                    DebugRow("Failed Sectors", debugInfo.failedSectors.joinToString(", "))
                    
                    if (debugInfo.usedKeyTypes.isNotEmpty()) {
                        Text(
                            text = "Key Types Used:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        debugInfo.usedKeyTypes.forEach { (sector, keyType) ->
                            Text(
                                text = "Sector $sector: $keyType",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
                
                // Derived Keys
                if (debugInfo.derivedKeys.isNotEmpty()) {
                    DebugSection(title = "Derived Keys (HKDF-SHA256)") {
                        debugInfo.derivedKeys.take(8).forEachIndexed { index, key ->
                            DebugRow("Key $index", key, monospace = true)
                        }
                        if (debugInfo.derivedKeys.size > 8) {
                            Text(
                                text = "... and ${debugInfo.derivedKeys.size - 8} more keys",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Block Data
                if (debugInfo.blockData.isNotEmpty()) {
                    DebugSection(title = "Block Data") {
                        debugInfo.blockData.entries.sortedBy { it.key }.forEach { (block, data) ->
                            DebugRow("Block $block", data, monospace = true)
                        }
                    }
                }
                
                // Error Messages
                if (debugInfo.errorMessages.isNotEmpty()) {
                    DebugSection(title = "Errors") {
                        debugInfo.errorMessages.forEach { error ->
                            Text(
                                text = "• $error",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                
                // Raw Color Data
                if (debugInfo.rawColorBytes.isNotEmpty()) {
                    DebugSection(title = "Raw Color Data") {
                        DebugRow("Color Bytes", debugInfo.rawColorBytes, monospace = true)
                    }
                }
                
                // Full Raw Data
                if (debugInfo.fullRawHex.isNotEmpty()) {
                    DebugSection(title = "Complete Raw Data (768 bytes)") {
                        Text(
                            text = "Complete encrypted data as read from tag",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RawDataDisplay(
                            data = debugInfo.fullRawHex,
                            label = "Raw Encrypted"
                        )
                    }
                }
                
                // Decrypted Data
                if (debugInfo.decryptedHex.isNotEmpty()) {
                    DebugSection(title = "Complete Decrypted Data (768 bytes)") {
                        Text(
                            text = "Data after successful sector authentication",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RawDataDisplay(
                            data = debugInfo.decryptedHex,
                            label = "Decrypted"
                        )
                    }
                }
                
                // Parsing Details
                if (debugInfo.parsingDetails.isNotEmpty()) {
                    DebugSection(title = "Parsing Details") {
                        debugInfo.parsingDetails.forEach { (key, value) ->
                            DebugRow(key, value.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun DebugRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.ifEmpty { "N/A" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Format debug info as copyable text for sharing/analysis
 */
private fun formatDebugInfoAsText(debugInfo: ScanDebugInfo): String {
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    
    return buildString {
        appendLine("=== B-Scan Debug Information ===")
        appendLine("Generated: $timestamp")
        appendLine()
        
        // App & Device Info
        appendLine("APP & DEVICE INFO:")
        appendLine("- App version: B-Scan ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("- Build type: ${BuildConfig.BUILD_TYPE}")
        appendLine("- Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("- Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("- Device ID: ${Build.DEVICE}")
        appendLine()
        
        // Tag Information
        appendLine("TAG INFORMATION:")
        appendLine("- UID: ${debugInfo.uid}")
        appendLine("- Size: ${debugInfo.tagSizeBytes} bytes")
        appendLine("- Sectors: ${debugInfo.sectorCount}")
        appendLine()
        
        // Authentication Status
        appendLine("AUTHENTICATION STATUS:")
        appendLine("- Authenticated sectors: ${debugInfo.authenticatedSectors.joinToString(", ").ifEmpty { "None" }}")
        appendLine("- Failed sectors: ${debugInfo.failedSectors.joinToString(", ").ifEmpty { "None" }}")
        
        if (debugInfo.usedKeyTypes.isNotEmpty()) {
            appendLine("- Key types used:")
            debugInfo.usedKeyTypes.forEach { (sector, keyType) ->
                appendLine("  * Sector $sector: $keyType")
            }
        }
        appendLine()
        
        // Derived Keys (all keys for debugging)
        if (debugInfo.derivedKeys.isNotEmpty()) {
            appendLine("DERIVED KEYS (HKDF-SHA256):")
            debugInfo.derivedKeys.forEachIndexed { index, key ->
                appendLine("- Key $index: $key")
            }
            appendLine()
        }
        
        // Block Data
        if (debugInfo.blockData.isNotEmpty()) {
            appendLine("BLOCK DATA:")
            debugInfo.blockData.entries.sortedBy { it.key }.forEach { (block, data) ->
                appendLine("- Block $block: $data")
            }
            appendLine()
        }
        
        // Raw Color Data
        if (debugInfo.rawColorBytes.isNotEmpty()) {
            appendLine("RAW COLOR DATA:")
            appendLine("- Color bytes: ${debugInfo.rawColorBytes}")
            appendLine()
        }
        
        // Parsing Details
        if (debugInfo.parsingDetails.isNotEmpty()) {
            appendLine("PARSING DETAILS:")
            debugInfo.parsingDetails.forEach { (key, value) ->
                appendLine("- $key: $value")
            }
            appendLine()
        }
        
        // Full Raw Data
        if (debugInfo.fullRawHex.isNotEmpty()) {
            appendLine("COMPLETE RAW DATA (768 bytes):")
            appendLine("- Raw encrypted: ${debugInfo.fullRawHex}")
            appendLine()
        }
        
        // Decrypted Data
        if (debugInfo.decryptedHex.isNotEmpty()) {
            appendLine("COMPLETE DECRYPTED DATA (768 bytes):")
            appendLine("- Decrypted: ${debugInfo.decryptedHex}")
            appendLine()
        }
        
        // Error Messages
        if (debugInfo.errorMessages.isNotEmpty()) {
            appendLine("ERRORS:")
            debugInfo.errorMessages.forEach { error ->
                appendLine("- $error")
            }
            appendLine()
        }
        
        appendLine("=== End Debug Information ===")
    }
}

@Composable
private fun RawDataDisplay(
    data: String,
    label: String,
    modifier: Modifier = Modifier
) {
    var showFormatted by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    Column(modifier = modifier) {
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${data.length / 2} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Row {
                TextButton(
                    onClick = { showFormatted = !showFormatted }
                ) {
                    Text(
                        text = if (showFormatted) "Raw" else "Formatted",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(data))
                        Toast.makeText(context, "$label data copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy $label data",
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                IconButton(
                    onClick = {
                        exportRawData(context, data, label)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.SaveAlt,
                        contentDescription = "Export $label data",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        // Data display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = if (showFormatted) formatHexData(data) else data,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 16.sp
            )
        }
    }
}

private fun formatHexData(hexString: String): String {
    if (hexString.isEmpty()) return ""
    
    return buildString {
        hexString.chunked(32).forEachIndexed { lineIndex, line ->
            val address = String.format("%04X", lineIndex * 16)
            append("$address: ")
            
            // Add hex bytes with spaces
            line.chunked(2).forEachIndexed { byteIndex, byte ->
                append(byte)
                if (byteIndex < 15) append(" ")
                if (byteIndex == 7) append(" ") // Extra space in middle
            }
            
            // Pad to consistent width
            val currentLength = line.length / 2
            if (currentLength < 16) {
                repeat(16 - currentLength) { 
                    append("   ")
                    if (it == 7) append(" ")
                }
            }
            
            // Add ASCII representation
            append("  |")
            line.chunked(2).forEach { byte ->
                val byteValue = byte.toIntOrNull(16) ?: 0
                val char = if (byteValue in 32..126) byteValue.toChar() else '.'
                append(char)
            }
            append("|")
            
            if (lineIndex < hexString.length / 32 - 1) appendLine()
        }
    }
}

private fun exportRawData(context: Context, hexData: String, label: String) {
    try {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "bscan_${label.lowercase().replace(" ", "_")}_${timestamp}.bin"
        
        // Convert hex string to bytes
        val bytes = hexData.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        // Save to Downloads directory
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, filename)
        
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
        
        Toast.makeText(context, "Exported to Downloads/$filename", Toast.LENGTH_LONG).show()
        
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
    }
}