package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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


