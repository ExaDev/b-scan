package com.bscan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Battery0Bar
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Battery3Bar
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bscan.model.BleScalesDevice
import com.bscan.model.SpoolWeight
import com.bscan.model.SpoolWeightStats
import com.bscan.permissions.BlePermissionState
import com.bscan.service.WeightTrackingService
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackingCard(
    weightTrackingService: WeightTrackingService,
    spoolId: String,
    permissionHandler: com.bscan.permissions.BlePermissionHandler? = null,
    modifier: Modifier = Modifier
) {
    val permissionState by (permissionHandler?.permissionState?.collectAsStateWithLifecycle() 
        ?: mutableStateOf(BlePermissionState.DENIED))
    
    val discoveredDevices by weightTrackingService.getDiscoveredDevices().collectAsStateWithLifecycle()
    val connectionStates by weightTrackingService.getConnectionStates().collectAsStateWithLifecycle()
    val batteryLevels by weightTrackingService.getBatteryLevels().collectAsStateWithLifecycle()
    val isServiceRunning by weightTrackingService.isServiceRunning.collectAsStateWithLifecycle()
    
    // Get current weight and stats for this spool
    val latestWeight = remember(spoolId) { 
        weightTrackingService.getLatestWeight(spoolId)
    }
    val spoolStats = remember(spoolId) {
        weightTrackingService.getSpoolStats(spoolId)
    }
    
    var isExpanded by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Weight Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Less" else "More")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Current weight display
            if (latestWeight != null) {
                WeightDisplay(
                    weight = latestWeight,
                    stats = spoolStats
                )
            } else {
                Text(
                    text = "No weight measurements recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Check permissions first
                if (permissionState != BlePermissionState.GRANTED) {
                    // Show permission prompt
                    PermissionPromptSection(
                        permissionState = permissionState,
                        onRequestPermissions = { showPermissionDialog = true }
                    )
                } else {
                    // Service status
                    ServiceStatusSection(
                        isRunning = isServiceRunning,
                        onStartService = { weightTrackingService.start() },
                        onStopService = { weightTrackingService.stop() },
                        onScanDevices = { weightTrackingService.scanForDevices() }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // BLE Devices section
                    if (discoveredDevices.isNotEmpty()) {
                        BleDevicesSection(
                            devices = discoveredDevices,
                            connectionStates = connectionStates,
                            batteryLevels = batteryLevels,
                            onConnectDevice = { deviceId -> weightTrackingService.connectToDevice(deviceId) },
                            onDisconnectDevice = { deviceId -> weightTrackingService.disconnectFromDevice(deviceId) }
                        )
                    } else {
                        Text(
                            text = "No BLE scales devices found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Permission dialog
    if (showPermissionDialog && permissionHandler != null) {
        BlePermissionDialog(
            permissionHandler = permissionHandler,
            onDismiss = { showPermissionDialog = false }
        )
    }
}

@Composable
private fun WeightDisplay(
    weight: SpoolWeight,
    stats: SpoolWeightStats?,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    
    Column(modifier = modifier) {
        // Current weight - large display
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "%.1f".format(weight.weightGrams),
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "g",
                modifier = Modifier.alignByBaseline(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Timestamp and device
        Text(
            text = "Measured ${weight.timestamp.format(formatter)} using ${weight.deviceName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Usage statistics if available
        stats?.let { statistics ->
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Used",
                    value = "%.1f g".format(statistics.totalUsage)
                )
                StatItem(
                    label = "Remaining",
                    value = "%.1f%%".format(statistics.remainingPercentage)
                )
                StatItem(
                    label = "Measurements",
                    value = statistics.measurementCount.toString()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ServiceStatusSection(
    isRunning: Boolean,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onScanDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Service Status",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isRunning,
                onClick = { if (isRunning) onStopService() else onStartService() },
                label = { Text(if (isRunning) "Running" else "Stopped") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            
            OutlinedButton(
                onClick = onScanDevices,
                modifier = Modifier.size(width = 120.dp, height = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun BleDevicesSection(
    devices: List<BleScalesDevice>,
    connectionStates: Map<String, Boolean>,
    batteryLevels: Map<String, Int>,
    onConnectDevice: (String) -> Unit,
    onDisconnectDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "BLE Scales Devices",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                BleDeviceChip(
                    device = device,
                    isConnected = connectionStates[device.deviceId] == true,
                    batteryLevel = batteryLevels[device.deviceId],
                    onConnect = { onConnectDevice(device.deviceId) },
                    onDisconnect = { onDisconnectDevice(device.deviceId) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BleDeviceChip(
    device: BleScalesDevice,
    isConnected: Boolean,
    batteryLevel: Int?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isConnected,
        onClick = { if (isConnected) onDisconnect() else onConnect() },
        modifier = modifier,
        label = { 
            Column {
                Text(
                    text = device.name.take(12) + if (device.name.length > 12) "..." else "",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (batteryLevel != null) {
                        Icon(
                            imageVector = getBatteryIcon(batteryLevel),
                            contentDescription = "Battery: $batteryLevel%",
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "$batteryLevel%",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    device.signalStrength?.let { rssi ->
                        Text(
                            text = "${rssi}dBm",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        },
        leadingIcon = {
            Icon(
                imageVector = when {
                    isConnected -> Icons.Default.BluetoothConnected
                    device.isConnected -> Icons.Default.BluetoothSearching // Assuming device.isConnected reflects the desired state
                    else -> Icons.Default.Bluetooth
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
private fun PermissionPromptSection(
    permissionState: BlePermissionState,
    onRequestPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "BLE Permissions Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Text(
                text = when (permissionState) {
                    BlePermissionState.CHECKING -> "Checking BLE permissions..."
                    BlePermissionState.REQUESTING -> "Please grant the requested permissions in the system dialog."
                    BlePermissionState.DENIED -> "BLE scales integration requires Bluetooth and location permissions to discover and connect to devices."
                    BlePermissionState.GRANTED -> "Permissions granted"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            if (permissionState == BlePermissionState.DENIED) {
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

private fun getBatteryIcon(batteryLevel: Int): ImageVector {
    return when {
        batteryLevel >= 95 -> Icons.Default.BatteryFull
        batteryLevel >= 80 -> Icons.Default.Battery6Bar
        batteryLevel >= 65 -> Icons.Default.Battery5Bar
        batteryLevel >= 50 -> Icons.Default.Battery4Bar
        batteryLevel >= 35 -> Icons.Default.Battery3Bar
        batteryLevel >= 20 -> Icons.Default.Battery2Bar
        batteryLevel >= 10 -> Icons.Default.Battery1Bar
        else -> Icons.Default.Battery0Bar
    }
}
