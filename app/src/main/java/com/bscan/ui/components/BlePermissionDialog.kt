package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.permissions.BlePermissionHandler
import com.bscan.permissions.BlePermissionState

@Composable
fun BlePermissionDialog(
    permissionHandler: BlePermissionHandler,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionState by permissionHandler.permissionState.collectAsStateWithLifecycle()
    
    when (permissionState) {
        BlePermissionState.DENIED -> {
            BlePermissionRequestDialog(
                permissionHandler = permissionHandler,
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
        BlePermissionState.REQUESTING -> {
            // Show loading state while requesting permissions
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Requesting Permissions") },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Please grant the requested permissions to enable BLE scales integration.")
                    }
                },
                confirmButton = { }
            )
        }
        BlePermissionState.GRANTED -> {
            // Permissions granted, dismiss dialog
            LaunchedEffect(Unit) {
                onDismiss()
            }
        }
        BlePermissionState.CHECKING -> {
            // Show loading while checking permissions
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Checking Permissions") },
                text = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Checking BLE permissions...")
                    }
                },
                confirmButton = { }
            )
        }
    }
}

@Composable
private fun BlePermissionRequestDialog(
    permissionHandler: BlePermissionHandler,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val missingPermissions = remember { permissionHandler.getMissingPermissions() }
    val permissionNames = remember { permissionHandler.getPermissionDisplayNames(missingPermissions) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { 
            Text(
                text = "BLE Permissions Required",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "To use BLE scales for weight tracking, this app needs the following permissions:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(permissionNames) { permissionName ->
                        PermissionItem(permissionName = permissionName)
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Location permission is required for BLE device discovery on Android.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { permissionHandler.requestPermissions() }
            ) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Skip")
            }
        }
    )
}

@Composable
private fun PermissionItem(
    permissionName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when {
            permissionName.contains("Bluetooth") -> Icons.Default.Bluetooth
            permissionName.contains("Location") -> Icons.Default.LocationOn
            else -> Icons.Default.Warning
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = permissionName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}