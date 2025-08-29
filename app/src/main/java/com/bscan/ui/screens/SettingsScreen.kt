package com.bscan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import android.util.Log
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.DataExportManager
import com.bscan.repository.ExportScope
import com.bscan.repository.ImportMode
import com.bscan.ui.screens.settings.SampleDataGenerator
import com.bscan.ui.screens.settings.ExportImportCard
import com.bscan.ui.screens.settings.ExportPreviewData
import com.bscan.ui.screens.settings.DataGenerationMode
import com.bscan.repository.UserPreferencesRepository
import com.bscan.ui.components.MaterialDisplayMode
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.components.FilamentColorBox
import com.bscan.ui.screens.home.CatalogDisplayMode
import com.bscan.repository.UserDataRepository
import com.bscan.data.bambu.BambuVariantSkuMapper
import com.bscan.repository.PhysicalComponentRepository
import com.bscan.model.AppTheme
import com.bscan.ble.BlePermissionHandler
import com.bscan.ble.BlePermissionState
import com.bscan.ble.BleScalesManager
import com.bscan.ble.ScaleConnectionState
import com.bscan.ble.ScaleCommandResult
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: com.bscan.MainViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToComponents: () -> Unit = {},
    blePermissionHandler: BlePermissionHandler? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val exportManager = remember { DataExportManager(context) }
    val userPrefsRepository = remember { UserPreferencesRepository(context) }
    val userDataRepository = viewModel.getUserDataRepository()
    val physicalComponentRepository = remember { PhysicalComponentRepository(context) }
    val scope = rememberCoroutineScope()
    
    // Physical component stats
    val allComponents = remember { physicalComponentRepository.getComponents() }
    val userDefinedComponents = remember { allComponents.filter { it.isUserDefined } }
    val builtInComponents = remember { allComponents.filter { !it.isUserDefined } }
    val totalComponents = allComponents.size
    
    // Initialize user data flow and observe reactively
    LaunchedEffect(userDataRepository) {
        userDataRepository.getUserData() // Initialize the flow
    }
    
    // Observe user data flow for reactive updates
    val userData by userDataRepository.userDataFlow.collectAsStateWithLifecycle()
    
    // Material display settings state
    val materialDisplaySettings = remember { 
        userData?.preferences?.materialDisplaySettings ?: MaterialDisplaySettings.DEFAULT 
    }
    var currentMaterialDisplaySettings by remember { mutableStateOf(materialDisplaySettings) }
    var catalogDisplayMode by remember { 
        mutableStateOf(userData?.preferences?.catalogDisplayMode ?: CatalogDisplayMode.COMPLETE_TITLE) 
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            item {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Material Shape Style Settings
            item {
                MaterialShapeStyleCard(
                    currentSettings = currentMaterialDisplaySettings,
                    onSettingsChanged = { newSettings ->
                        currentMaterialDisplaySettings = newSettings
                        scope.launch {
                            userDataRepository.updatePreferences { currentPrefs ->
                                currentPrefs.copy(materialDisplaySettings = newSettings)
                            }
                        }
                    }
                )
            }
            
            // Material Text Overlay Settings
            item {
                MaterialTextOverlayCard(
                    currentSettings = currentMaterialDisplaySettings,
                    onSettingsChanged = { newSettings ->
                        currentMaterialDisplaySettings = newSettings
                        scope.launch {
                            userDataRepository.updatePreferences { currentPrefs ->
                                currentPrefs.copy(materialDisplaySettings = newSettings)
                            }
                        }
                    }
                )
            }
            
            // Material Variant Name Settings
            item {
                MaterialVariantNameCard(
                    currentSettings = currentMaterialDisplaySettings,
                    onSettingsChanged = { newSettings ->
                        currentMaterialDisplaySettings = newSettings
                        scope.launch {
                            userDataRepository.updatePreferences { currentPrefs ->
                                currentPrefs.copy(materialDisplaySettings = newSettings)
                            }
                        }
                    }
                )
            }
            
            item {
                CatalogDisplayModeCard(
                    currentMode = catalogDisplayMode,
                    currentMaterialSettings = currentMaterialDisplaySettings,
                    onModeChanged = { mode ->
                        catalogDisplayMode = mode
                        scope.launch {
                            userDataRepository.updatePreferences { currentPrefs ->
                                currentPrefs.copy(catalogDisplayMode = mode)
                            }
                        }
                    }
                )
            }
            
            item {
                ThemeSelectionCard(
                    currentTheme = userData?.preferences?.theme ?: AppTheme.AUTO,
                    onThemeChanged = { theme ->
                        scope.launch {
                            userDataRepository.updatePreferences { currentPrefs ->
                                currentPrefs.copy(theme = theme)
                            }
                        }
                    }
                )
            }
            
            item {
                AccelerometerEffectsCard(
                    userPrefsRepository = userPrefsRepository
                )
            }
            
            item {
                Text(
                    text = "Physical Components",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                PhysicalComponentsManagementCard(
                    totalComponents = totalComponents,
                    userDefinedComponents = userDefinedComponents.size,
                    builtInComponents = builtInComponents.size,
                    onManageComponents = onNavigateToComponents
                )
            }
            
            item {
                Text(
                    text = "BLE Scales",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                BleScalesPreferenceCard(
                    userPrefsRepository = userPrefsRepository,
                    blePermissionHandler = blePermissionHandler
                )
            }
            
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                val scanCount = remember { repository.getAllScans().size }
                val spoolCount = remember { repository.getUniqueFilamentReelsByTray().size }
                var isExporting by remember { mutableStateOf(false) }
                var isImporting by remember { mutableStateOf(false) }
                var exportScope by remember { mutableStateOf(ExportScope.ALL_DATA) }
                var importMode by remember { mutableStateOf(ImportMode.MERGE_WITH_EXISTING) }
                var fromDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
                var toDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
                var previewData by remember { mutableStateOf<ExportPreviewData?>(null) }
                var showPreview by remember { mutableStateOf(false) }
                
                ExportImportCard(
                    scanCount = scanCount,
                    spoolCount = spoolCount,
                    isExporting = isExporting,
                    isImporting = isImporting,
                    exportScope = exportScope,
                    importMode = importMode,
                    fromDate = fromDate,
                    toDate = toDate,
                    previewData = previewData,
                    showPreview = showPreview,
                    onExportScopeChange = { exportScope = it },
                    onImportModeChange = { importMode = it },
                    onFromDateChange = { fromDate = it },
                    onToDateChange = { toDate = it },
                    onExportClick = {
                        scope.launch {
                            isExporting = true
                            try {
                                // DataExportManager integration: needs file picker and scope selection
                                kotlinx.coroutines.delay(1000) // Simulate export
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    onImportClick = {
                        // Launch file picker for import
                    },
                    onConfirmImport = {
                        scope.launch {
                            isImporting = true
                            try {
                                previewData?.let {
                                    // Perform import based on importMode
                                }
                            } finally {
                                isImporting = false
                                showPreview = false
                                previewData = null
                            }
                        }
                    },
                    onCancelImport = {
                        showPreview = false
                        previewData = null
                    }
                )
            }
            
            item {
                Text(
                    text = "Sample Data",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            item {
                SampleDataCard(repository = repository)
            }
        }
    }
}

/**
 * Card for material display preferences
 */
@Composable
private fun MaterialDisplayCard(
    currentMode: MaterialDisplayMode,
    onModeChanged: (MaterialDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Material Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "How filament materials are visually distinguished",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Display mode options
            MaterialDisplayMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeChanged(mode) }
                    )
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentMode == mode) FontWeight.Medium else FontWeight.Normal
                        )
                        
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Sample display
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilamentColorBox(
                            colorHex = "#FF6B35",
                            filamentType = "PLA",
                            size = 24.dp,
                            displayMode = mode
                        )
                        
                        FilamentColorBox(
                            colorHex = "#4A90E2",
                            filamentType = "PETG", 
                            size = 24.dp,
                            displayMode = mode
                        )
                        
                        FilamentColorBox(
                            colorHex = "#7B68EE",
                            filamentType = "ABS",
                            size = 24.dp,
                            displayMode = mode
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for managing physical components
 */
@Composable
private fun PhysicalComponentsManagementCard(
    totalComponents: Int,
    userDefinedComponents: Int,
    builtInComponents: Int,
    onManageComponents: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Component Inventory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Manage physical components used in inventory calculations",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComponentStatistic(
                    label = "Total Components",
                    value = totalComponents.toString()
                )
                
                ComponentStatistic(
                    label = "User-Defined",
                    value = userDefinedComponents.toString()
                )
                
                ComponentStatistic(
                    label = "Built-In",
                    value = builtInComponents.toString()
                )
            }
            
            Button(
                onClick = onManageComponents,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Components")
            }
        }
    }
}

@Composable
private fun BleScalesPreferenceCard(
    userPrefsRepository: UserPreferencesRepository,
    blePermissionHandler: BlePermissionHandler?
) {
    val context = LocalContext.current
    
    var isScalesEnabled by remember { mutableStateOf(userPrefsRepository.isBleScalesEnabled()) }
    var preferredScaleName by remember { mutableStateOf(userPrefsRepository.getPreferredScaleName()) }
    var autoConnectEnabled by remember { mutableStateOf(userPrefsRepository.isBleScalesAutoConnectEnabled()) }
    
    // BLE components - only create if BLE is supported
    val bleScalesManager = remember { 
        if (blePermissionHandler != null) {
            BleScalesManager(context)
        } else null 
    }
    
    // BLE state
    val isScanning by (bleScalesManager?.isScanning ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    val discoveredDevices by (bleScalesManager?.discoveredDevices ?: MutableStateFlow(emptyList())).collectAsStateWithLifecycle()
    val permissionState by (blePermissionHandler?.permissionState ?: MutableStateFlow(BlePermissionState.UNKNOWN)).collectAsStateWithLifecycle()
    val currentReading by (bleScalesManager?.currentReading ?: MutableStateFlow(null)).collectAsStateWithLifecycle()
    val connectionState by (bleScalesManager?.connectionState ?: MutableStateFlow(ScaleConnectionState.DISCONNECTED)).collectAsStateWithLifecycle()
    val isReading by (bleScalesManager?.isReading ?: MutableStateFlow(false)).collectAsStateWithLifecycle()
    
    var showDeviceSelection by remember { mutableStateOf(false) }
    var isTaring by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    // Auto-connect to stored scale on first load
    LaunchedEffect(bleScalesManager, preferredScaleName) {
        // Only attempt reconnect if BLE is available
        if (bleScalesManager != null && blePermissionHandler != null) {
            // Always attempt to reconnect if we have stored scale info and aren't connected
            val storedAddress = userPrefsRepository.getPreferredScaleAddress()
            val storedName = userPrefsRepository.getPreferredScaleName()
            
            if (storedAddress != null && storedName != null && !bleScalesManager.isConnectedToScale()) {
                Log.i("BleScalesSettings", "Attempting auto-reconnect to stored scale: $storedName ($storedAddress)")
                
                val result = bleScalesManager.reconnectToStoredScale(
                    storedAddress, 
                    storedName, 
                    blePermissionHandler
                )
                
                when (result) {
                    is ScaleCommandResult.Success -> {
                        Log.i("BleScalesSettings", "Auto-reconnect successful")
                    }
                    is ScaleCommandResult.Error -> {
                        Log.w("BleScalesSettings", "Auto-reconnect failed: ${result.message}")
                    }
                    else -> {
                        Log.w("BleScalesSettings", "Auto-reconnect failed: $result")
                    }
                }
            }
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Weight Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Connect a Bluetooth scale to automatically track filament weight after NFC scans",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Current scale status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val isActuallyConnected = bleScalesManager?.isConnectedToScale() == true
                    val hasStoredScale = preferredScaleName != null
                    
                    Text(
                        text = when {
                            isActuallyConnected -> "Connected Scale"
                            hasStoredScale -> "Configured Scale (Not Connected)"
                            else -> "No Scale Connected"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (hasStoredScale) {
                        Text(
                            text = preferredScaleName!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActuallyConnected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        
                        if (!isActuallyConnected) {
                            Text(
                                text = "Tap Connect to establish BLE connection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        Text(
                            text = "Tap to connect a BLE scale",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                val isActuallyConnected = bleScalesManager?.isConnectedToScale() == true
                val hasStoredScale = preferredScaleName != null
                
                if (hasStoredScale) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isActuallyConnected) {
                            // Connect button (reconnect to stored scale)
                            Button(
                                onClick = {
                                    scope.launch {
                                        val storedAddress = userPrefsRepository.getPreferredScaleAddress()
                                        if (storedAddress != null && bleScalesManager != null && blePermissionHandler != null) {
                                            bleScalesManager.reconnectToStoredScale(
                                                storedAddress, 
                                                preferredScaleName!!, 
                                                blePermissionHandler
                                            )
                                        }
                                    }
                                },
                                enabled = connectionState != ScaleConnectionState.CONNECTING
                            ) {
                                Text(if (connectionState == ScaleConnectionState.CONNECTING) "Connecting..." else "Connect")
                            }
                        }
                        
                        // Disconnect/Remove button
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // Actual BLE disconnection if connected
                                    if (isActuallyConnected && bleScalesManager != null) {
                                        bleScalesManager.disconnectFromScale()
                                    }
                                    
                                    // Clear preferences
                                    userPrefsRepository.clearBleScalesConfiguration()
                                    isScalesEnabled = false
                                    preferredScaleName = null
                                }
                            }
                        ) {
                            Text(if (isActuallyConnected) "Disconnect" else "Remove")
                        }
                    }
                } else {
                    // Connect button
                    Button(
                        onClick = {
                            blePermissionHandler?.let { handler ->
                                if (handler.hasAllPermissions() && bleScalesManager != null) {
                                    showDeviceSelection = true
                                    bleScalesManager.startScanning(handler)
                                } else if (!handler.hasAllPermissions()) {
                                    handler.requestPermissions()
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        if (isScanning) {
                            Text("Scanning...")
                        } else {
                            Text("Connect Scale")
                        }
                    }
                }
            }
            
            // Auto-connect setting (only shown when scale is configured)
            if (preferredScaleName != null) {
                HorizontalDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-connect",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatically connect to scale when app starts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoConnectEnabled,
                        onCheckedChange = { enabled ->
                            autoConnectEnabled = enabled
                            scope.launch {
                                userPrefsRepository.setBleScalesAutoConnectEnabled(enabled)
                            }
                        }
                    )
                }
                
                // Debug information panel (when connected)
                if (bleScalesManager?.isConnectedToScale() == true) {
                    HorizontalDivider()
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Scale Debug Information",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Connection status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Connection Status:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            val statusColor = when (connectionState) {
                                ScaleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                ScaleConnectionState.READING -> MaterialTheme.colorScheme.tertiary
                                ScaleConnectionState.CONNECTING -> MaterialTheme.colorScheme.secondary
                                ScaleConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Text(
                                text = connectionState.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = statusColor
                            )
                        }
                        
                        // Current weight reading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Weight:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            currentReading?.let { reading ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = reading.getDisplayWeightWithValidation(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (reading.isUnitValid) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Text(
                                        text = reading.getStabilityIcon(),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } ?: Text(
                                text = "No reading",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Reading status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Reading Status:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isReading) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isReading) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Raw data display
                        currentReading?.let { reading ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Raw Data:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = reading.getRawDataHex(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Method: ${reading.parsingMethod}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Unit: ${reading.unit.displayName} ${if (reading.isUnitValid) "✓" else "⚠"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (reading.isUnitValid) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                        Text(
                                            text = "Age: ${reading.getAgeString()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Control buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start/Stop reading button
                            Button(
                                onClick = {
                                    scope.launch {
                                        if (isReading && bleScalesManager != null) {
                                            bleScalesManager.stopWeightReading()
                                        } else if (!isReading && bleScalesManager != null) {
                                            bleScalesManager.startWeightReading()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = connectionState == ScaleConnectionState.CONNECTED || connectionState == ScaleConnectionState.READING
                            ) {
                                Text(if (isReading) "Stop Reading" else "Start Reading")
                            }
                            
                            // Tare button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        if (bleScalesManager != null) {
                                            isTaring = true
                                            val result = bleScalesManager.tareScale()
                                            isTaring = false
                                            
                                            // Could add toast/snackbar feedback here
                                        }
                                    }
                                },
                                enabled = !isTaring && connectionState in listOf(ScaleConnectionState.CONNECTED, ScaleConnectionState.READING)
                            ) {
                                if (isTaring) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Tare")
                                }
                            }
                        }
                        
                        // Unit detection tools
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Enable unit detection monitoring button
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        if (bleScalesManager != null) {
                                            val result = bleScalesManager.enableUnitDetectionMonitoring()
                                            Log.i("BleScalesSettings", "Unit detection monitoring result: $result")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = connectionState in listOf(ScaleConnectionState.CONNECTED, ScaleConnectionState.READING)
                            ) {
                                Text("🔍 Monitor All")
                            }
                            
                            // Instructions text
                            Text(
                                text = "Enable monitoring, then change units",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            // Device discovery and selection
            if (isScanning || discoveredDevices.isNotEmpty()) {
                HorizontalDivider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Scanning indicator
                    if (isScanning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Scanning for BLE scales...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Discovered devices
                    if (discoveredDevices.isNotEmpty()) {
                        Text(
                            text = "Found ${discoveredDevices.size} device${if (discoveredDevices.size == 1) "" else "s"}:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        discoveredDevices.forEach { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (device.isKnownScale) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = device.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${device.address} • ${device.rssi} dBm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (device.isKnownScale) {
                                            Text(
                                                text = "✓ Compatible scale",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                if (bleScalesManager != null) {
                                                    // Stop scanning first
                                                    bleScalesManager.stopScanning()
                                                    
                                                    // Attempt actual BLE connection
                                                    val result = bleScalesManager.connectToScale(device)
                                                    
                                                    if (result is ScaleCommandResult.Success) {
                                                        // Save preferences only on successful connection
                                                        userPrefsRepository.setPreferredScaleAddress(device.address)
                                                        userPrefsRepository.setPreferredScaleName(device.displayName)
                                                        userPrefsRepository.setBleScalesEnabled(true)
                                                        
                                                        // Update local state
                                                        isScalesEnabled = true
                                                        preferredScaleName = device.displayName
                                                        showDeviceSelection = false
                                                    } else {
                                                        // Handle connection failure - could show toast/snackbar here
                                                        // For now, just continue showing device selection
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Connect")
                                    }
                                }
                            }
                        }
                        
                        // Cancel scanning button
                        OutlinedButton(
                            onClick = {
                                bleScalesManager?.stopScanning()
                                showDeviceSelection = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                    
                    // Permission state feedback
                    when (permissionState) {
                        com.bscan.ble.BlePermissionState.DENIED -> {
                            Text(
                                text = "⚠ BLE permissions required to scan for scales",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        com.bscan.ble.BlePermissionState.REQUESTING -> {
                            Text(
                                text = "Requesting BLE permissions...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> { /* No feedback needed */ }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentStatistic(
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
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Card for generating sample data
 */
@Composable
private fun SampleDataCard(
    repository: ScanHistoryRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var isGenerating by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(DataGenerationMode.COMPLETE_COVERAGE) }
    var sampleCount by remember { mutableStateOf(25) }
    var generateFromDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var generateToDate by remember { mutableStateOf(LocalDate.now()) }
    var showAdvancedOptions by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Sample Data Generator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Generate realistic scan data for testing and demonstration",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Generation mode selection
            Text(
                text = "Generation Mode",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            DataGenerationMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode }
                    )
                    
                    Column(
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Text(
                            text = mode.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = mode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Advanced options toggle
            TextButton(
                onClick = { showAdvancedOptions = !showAdvancedOptions }
            ) {
                Text(
                    if (showAdvancedOptions) "Hide Advanced Options" else "Show Advanced Options"
                )
                Icon(
                    if (showAdvancedOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            if (showAdvancedOptions) {
                // Sample count (only for non-complete coverage modes)
                if (selectedMode != DataGenerationMode.COMPLETE_COVERAGE) {
                    OutlinedTextField(
                        value = sampleCount.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { count ->
                                sampleCount = count.coerceIn(1, 500)
                            }
                        },
                        label = { Text("Sample Count") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Date range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = generateFromDate.format(dateFormatter),
                        onValueChange = { /* Read-only for now */ },
                        label = { Text("From Date") },
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = generateToDate.format(dateFormatter),
                        onValueChange = { /* Read-only for now */ },
                        label = { Text("To Date") },
                        readOnly = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Generation info
            when (selectedMode) {
                DataGenerationMode.COMPLETE_COVERAGE -> {
                    val totalSkus = BambuVariantSkuMapper.getAllKnownRfidKeys().size
                    Text(
                        text = "Will generate samples for all $totalSkus known Bambu Lab products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DataGenerationMode.RANDOM_SAMPLE -> {
                    Text(
                        text = "Will generate $sampleCount random samples from available products",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DataGenerationMode.MINIMAL_COVERAGE -> {
                    Text(
                        text = "Will generate $sampleCount samples focusing on most common materials",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Generate button
            Button(
                onClick = {
                    scope.launch {
                        isGenerating = true
                        try {
                            val generator = SampleDataGenerator()
                            when (selectedMode) {
                                DataGenerationMode.COMPLETE_COVERAGE -> {
                                    generator.generateWithCompleteSkuCoverage(
                                        repository = repository,
                                        additionalRandomSpools = 50
                                    )
                                }
                                DataGenerationMode.RANDOM_SAMPLE -> {
                                    generator.generateRandomSample(
                                        repository = repository,
                                        spoolCount = sampleCount
                                    )
                                }
                                DataGenerationMode.MINIMAL_COVERAGE -> {
                                    generator.generateMinimalCoverage(
                                        repository = repository
                                    )
                                }
                            }
                        } finally {
                            isGenerating = false
                        }
                    }
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating...")
                } else {
                    Icon(Icons.Default.DataArray, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Sample Data")
                }
            }
        }
    }
}

@Composable
fun AccelerometerEffectsCard(
    userPrefsRepository: UserPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accelerometerManager = remember { com.bscan.sensor.AccelerometerManager(context) }
    val deviceCapabilities = remember { com.bscan.utils.DeviceCapabilities(context) }
    
    var accelerometerEffectsEnabled by remember { 
        mutableStateOf(userPrefsRepository.isAccelerometerEffectsEnabled()) 
    }
    var motionSensitivity by remember {
        mutableStateOf(userPrefsRepository.getMotionSensitivity())
    }
    val scope = rememberCoroutineScope()
    
    val isAccelerometerAvailable = accelerometerManager.isAvailable()
    val deviceSupportsEffects = remember { !deviceCapabilities.shouldDisableAccelerometerEffects() }
    val wasAutoDisabled = userPrefsRepository.wasAccelerometerEffectsAutoDisabled()
    val deviceInfo = userPrefsRepository.getAccelerometerEffectsDeviceInfo()
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main toggle row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Motion Effects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show performance indicator
                    if (wasAutoDisabled) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Auto-disabled for performance",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                
                Text(
                    text = when {
                        !isAccelerometerAvailable -> "Accelerometer not available on this device"
                        !deviceSupportsEffects -> deviceInfo
                        wasAutoDisabled -> "$deviceInfo (can be manually enabled)"
                        else -> "Reflections and shimmer follow device tilt"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        !isAccelerometerAvailable -> MaterialTheme.colorScheme.error
                        !deviceSupportsEffects -> MaterialTheme.colorScheme.secondary
                        wasAutoDisabled -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Switch(
                checked = accelerometerEffectsEnabled && isAccelerometerAvailable,
                enabled = isAccelerometerAvailable,
                onCheckedChange = { enabled ->
                    if (isAccelerometerAvailable) {
                        accelerometerEffectsEnabled = enabled
                        scope.launch {
                            userPrefsRepository.setAccelerometerEffectsEnabled(enabled)
                        }
                    }
                }
                )
            }
            
            // Motion sensitivity slider (only show when effects are enabled)
            if (accelerometerEffectsEnabled && isAccelerometerAvailable) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Motion Sensitivity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when {
                                motionSensitivity <= 0.3f -> "Subtle"
                                motionSensitivity <= 0.7f -> "Balanced"
                                else -> "Dynamic"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Slider(
                        value = motionSensitivity,
                        onValueChange = { sensitivity ->
                            motionSensitivity = sensitivity
                        },
                        onValueChangeFinished = {
                            scope.launch {
                                userPrefsRepository.setMotionSensitivity(motionSensitivity)
                            }
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 17, // 0.1 to 1.0 in steps of 0.05
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Subtle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Dynamic",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Preview box to show current sensitivity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Small preview box with PETG reflection effect
                        FilamentColorBox(
                            colorHex = "#1E88E5", // Blue PETG color
                            filamentType = "PETG Basic",
                            size = 32.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card for catalog display preferences
 */
@Composable
private fun CatalogDisplayModeCard(
    currentMode: CatalogDisplayMode,
    currentMaterialSettings: MaterialDisplaySettings,
    onModeChanged: (CatalogDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Catalog Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "How product information is displayed in the catalog browser",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Display mode options using consistent full-width style
            CatalogDisplayMode.entries.forEach { mode ->
                CatalogDisplayOption(
                    title = mode.displayName,
                    description = mode.description,
                    isSelected = currentMode == mode,
                    onClick = { onModeChanged(mode) },
                    displayMode = mode,
                    materialSettings = currentMaterialSettings
                )
            }
        }
    }
}

/**
 * Preview card showing how catalog items will look in each display mode
 */
@Composable
private fun CatalogPreviewCard(
    displayMode: CatalogDisplayMode,
    materialSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview using current material display settings
            FilamentColorBox(
                colorHex = "#00BCD4", // Cyan color
                filamentType = "PLA Basic",
                materialDisplaySettings = materialSettings,
                modifier = Modifier.size(40.dp)
            )
            
            // Product information based on display mode
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title changes based on display mode
                Text(
                    text = when (displayMode) {
                        CatalogDisplayMode.COMPLETE_TITLE -> "Basic Cyan PLA"
                        CatalogDisplayMode.COLOR_FOCUSED -> "Cyan"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Properties change based on display mode
                when (displayMode) {
                    CatalogDisplayMode.COMPLETE_TITLE -> {
                        Text(
                            text = "SKU: GFL00A00K0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    CatalogDisplayMode.COLOR_FOCUSED -> {
                        Text(
                            text = "PLA Basic • SKU: GFL00A00K0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Temperature info (same for both modes)
                Text(
                    text = "210-230°C • Bed: 60°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status indicators (same for both modes)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Has RFID mapping",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "Thermoplastic",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * Catalog display option with radio button and complete catalog item preview
 */
@Composable
private fun CatalogDisplayOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    displayMode: CatalogDisplayMode,
    materialSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Complete catalog item preview showing display mode effect
        CatalogPreviewCard(
            displayMode = displayMode,
            materialSettings = materialSettings,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Card for material shape style settings
 */
@Composable
private fun MaterialShapeStyleCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Material Shape Style",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose the shape style for material color boxes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Option 1: Material-based shapes
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSettingsChanged(currentSettings.copy(showMaterialShapes = true)) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = currentSettings.showMaterialShapes,
                        onClick = { onSettingsChanged(currentSettings.copy(showMaterialShapes = true)) }
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Material-based shapes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (currentSettings.showMaterialShapes) FontWeight.Medium else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Different shapes for each material type (PLA=circle, PETG=octagon, etc.)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                MaterialShapesPreview()
            }

            // Option 2: Simple rounded rectangles
            MaterialDisplayOption(
                title = "Simple rounded rectangles",
                description = "Consistent rectangular shape for all materials",
                isSelected = !currentSettings.showMaterialShapes,
                onClick = { onSettingsChanged(currentSettings.copy(showMaterialShapes = false)) },
                previewSettings = currentSettings.copy(showMaterialShapes = false)
            )
        }
    }
}

/**
 * Card for material text overlay settings
 */
@Composable
private fun MaterialTextOverlayCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Material Text Overlays",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose what text information appears over material colors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            MaterialDisplayOption(
                title = "No text overlays",
                description = "Pure color display without text",
                isSelected = !currentSettings.showMaterialNameInShape && !currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = false,
                        showMaterialVariantInShape = false
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = false,
                    showMaterialVariantInShape = false
                )
            )
            
            MaterialDisplayOption(
                title = "Material name only",
                description = "Show material abbreviations (PLA, PETG, etc.)",
                isSelected = currentSettings.showMaterialNameInShape && !currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = true,
                        showMaterialVariantInShape = false
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = true,
                    showMaterialVariantInShape = false
                )
            )
            
            MaterialDisplayOption(
                title = "Material + variant",
                description = "Show both material and variant (PLA B, PETG S, etc.)",
                isSelected = currentSettings.showMaterialNameInShape && currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = true,
                        showMaterialVariantInShape = true
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = true,
                    showMaterialVariantInShape = true
                )
            )
            
            MaterialDisplayOption(
                title = "Variant only",
                description = "Show only variant information (Basic, Silk, Matte)",
                isSelected = !currentSettings.showMaterialNameInShape && currentSettings.showMaterialVariantInShape,
                onClick = { 
                    onSettingsChanged(currentSettings.copy(
                        showMaterialNameInShape = false,
                        showMaterialVariantInShape = true
                    ))
                },
                previewSettings = currentSettings.copy(
                    showMaterialNameInShape = false,
                    showMaterialVariantInShape = true
                )
            )
        }
    }
}

/**
 * Card for material variant name format settings
 */
@Composable
private fun MaterialVariantNameCard(
    currentSettings: MaterialDisplaySettings,
    onSettingsChanged: (MaterialDisplaySettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val variantOptionsEnabled = currentSettings.showMaterialVariantInShape
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Icon Variant Name Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (variantOptionsEnabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            
            Text(
                text = if (variantOptionsEnabled) {
                    "Choose how variant names are displayed in material icons"
                } else {
                    "Enable variant text overlays above to use these options"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (variantOptionsEnabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
            
            MaterialDisplayOption(
                title = "Abbreviated in icons",
                description = "Short forms in shapes (B, S, M, CF, HF, etc.)",
                isSelected = !currentSettings.showFullVariantNamesInShape,
                onClick = { onSettingsChanged(currentSettings.copy(showFullVariantNamesInShape = false)) },
                previewSettings = currentSettings.copy(showFullVariantNamesInShape = false),
                enabled = variantOptionsEnabled
            )
            
            MaterialDisplayOption(
                title = "Full names in icons",
                description = "Complete names in shapes (Basic, Silk, Matte, etc.)",
                isSelected = currentSettings.showFullVariantNamesInShape,
                onClick = { onSettingsChanged(currentSettings.copy(showFullVariantNamesInShape = true)) },
                previewSettings = currentSettings.copy(showFullVariantNamesInShape = true),
                enabled = variantOptionsEnabled
            )
        }
    }
}





/**
 * Material display option with radio button and complete catalog item preview
 */
@Composable
private fun MaterialDisplayOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    previewSettings: MaterialDisplaySettings,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                enabled = enabled
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
        
        // Complete catalog item preview
        CatalogPreviewCard(
            displayMode = CatalogDisplayMode.COMPLETE_TITLE,
            materialSettings = previewSettings,
            modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.38f)
        )
    }
}




/**
 * Reusable toggle row for settings
 */
@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun MaterialShapesPreview(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilamentColorBox(
            colorHex = "#FF6B35",
            filamentType = "PLA",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#4A90E2",
            filamentType = "PETG",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#7B68EE",
            filamentType = "ABS",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#F44336",
            filamentType = "ASA",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
        FilamentColorBox(
            colorHex = "#4CAF50",
            filamentType = "TPU",
            size = 32.dp,
            materialDisplaySettings = MaterialDisplaySettings(showMaterialShapes = true)
        )
    }
}

/**
 * Card for theme selection
 */
@Composable
private fun ThemeSelectionCard(
    currentTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Choose the app appearance theme",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Theme options
            AppTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onThemeChanged(theme) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentTheme == theme,
                        onClick = { onThemeChanged(theme) }
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = when (theme) {
                                AppTheme.AUTO -> "Auto (System)"
                                AppTheme.LIGHT -> "Light"
                                AppTheme.DARK -> "Dark"
                                AppTheme.WHITE -> "White"
                                AppTheme.BLACK -> "Black"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (currentTheme == theme) FontWeight.Medium else FontWeight.Normal
                        )
                        
                        Text(
                            text = when (theme) {
                                AppTheme.AUTO -> "Follow system dark/light mode setting"
                                AppTheme.LIGHT -> "Light theme with standard colours"
                                AppTheme.DARK -> "Dark theme for low-light environments"
                                AppTheme.WHITE -> "High contrast white theme"
                                AppTheme.BLACK -> "Pure black theme for OLED displays"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Visual indicator for theme
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when (theme) {
                                    AppTheme.AUTO -> Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface,
                                            MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    AppTheme.LIGHT -> SolidColor(Color(0xFFFFFFFF))
                                    AppTheme.DARK -> SolidColor(Color(0xFF121212))
                                    AppTheme.WHITE -> SolidColor(Color(0xFFFFFFFF))
                                    AppTheme.BLACK -> SolidColor(Color(0xFF000000))
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        if (theme == AppTheme.AUTO) {
                            // Show a small icon for Auto theme
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = "Auto theme",
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.Center),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Note: Preview temporarily disabled due to BlePermissionHandler dependency
// @Preview(showBackground = true)
// @Composable
// fun SettingsScreenPreview() {
//     MaterialTheme {
//         SettingsScreen(
//             onNavigateBack = {},
//             onNavigateToComponents = {},
//             blePermissionHandler = MockBlePermissionHandler()
//         )
//     }
// }