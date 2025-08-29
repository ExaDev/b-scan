package com.bscan.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.DataExportManager
import com.bscan.repository.ExportScope
import com.bscan.repository.ImportMode
import com.bscan.ui.screens.settings.ExportImportCard
import com.bscan.ui.screens.settings.ExportPreviewData
import com.bscan.repository.UserPreferencesRepository
import com.bscan.ui.components.MaterialDisplaySettings
import com.bscan.ui.screens.home.CatalogDisplayMode
import com.bscan.repository.UserDataRepository
import com.bscan.repository.PhysicalComponentRepository
import com.bscan.model.AppTheme
import com.bscan.ble.BlePermissionHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
// Import all the extracted settings components
import com.bscan.ui.components.settings.*

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
                BleScalesSettingsCard()
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
                SampleDataGenerationCard(repository = repository)
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