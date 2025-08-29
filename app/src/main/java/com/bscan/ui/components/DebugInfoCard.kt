package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.bscan.model.ScanDebugInfo
import com.bscan.ui.components.debug.*

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
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            DebugInfoHeader(
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded },
                onCopyToClipboard = {
                    clipboardManager.setText(AnnotatedString(debugText))
                    Toast.makeText(context, "Debug info copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )
            
            if (expanded) {
                // Tag Information
                TagInfoSection(debugInfo = debugInfo)
                
                // Authentication Status
                AuthenticationSection(debugInfo = debugInfo)
                
                // Derived Keys
                DerivedKeysSection(derivedKeys = debugInfo.derivedKeys)
                
                // Block Data
                BlockDataSection(blockData = debugInfo.blockData)
                
                // Error Messages
                ErrorMessagesSection(errorMessages = debugInfo.errorMessages)
                
                // Raw Color Data
                RawColorSection(rawColorBytes = debugInfo.rawColorBytes)
                
                // Full Raw Data
                RawDataSection(
                    data = debugInfo.fullRawHex,
                    title = "Complete Raw Data (768 bytes)",
                    description = "Complete encrypted data as read from tag"
                )
                
                // Decrypted Data
                RawDataSection(
                    data = debugInfo.decryptedHex,
                    title = "Complete Decrypted Data (768 bytes)",
                    description = "Data after successful sector authentication"
                )
                
                // Parsing Details
                ParsingDetailsSection(parsingDetails = debugInfo.parsingDetails)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DebugInfoCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            DebugInfoCard(
                debugInfo = ScanDebugInfo(
                    uid = "A1B2C3D4E5F6A7B8",
                    tagSizeBytes = 1024,
                    sectorCount = 16,
                    authenticatedSectors = listOf(0, 1, 2, 4, 5, 8, 9, 10),
                    failedSectors = listOf(3, 6, 7, 11, 12, 13, 14, 15),
                    usedKeyTypes = mapOf(
                        0 to "KeyA",
                        1 to "KeyA", 
                        2 to "KeyB",
                        4 to "KeyA",
                        5 to "KeyB"
                    ),
                    blockData = mapOf(
                        4 to "474641303A413030D2DA00000000000000000000000000",
                        5 to "4B30000000000000000000000000000000000000000000",
                        8 to "FF0000FFFFFFFFFF0000000000000000000000000000",
                        9 to "20240315000000000000000000000000000000000000"
                    ),
                    derivedKeys = listOf(
                        "A1B2C3D4E5F6",
                        "F6E5D4C3B2A1",
                        "123456789ABC",
                        "CBA987654321"
                    ),
                    rawColorBytes = "4B30",
                    errorMessages = listOf(
                        "Authentication failed for sector 3",
                        "Block 6 read timeout"
                    ),
                    parsingDetails = mapOf(
                        "material_id" to "GFA00",
                        "series_code" to "A00",
                        "color_code" to "K0",
                        "production_date" to "2024-03-15",
                        "temperatures" to mapOf(
                            "min" to 210,
                            "max" to 230,
                            "bed" to 60
                        )
                    ),
                    fullRawHex = "474641303A413030D2DA4B30000000000000FF0000FFFFFF2024031500000000".repeat(12),
                    decryptedHex = "474641303A413030D2DA4B30000000000000FF0000FFFFFF2024031500000000".repeat(10)
                )
            )
        }
    }
}


