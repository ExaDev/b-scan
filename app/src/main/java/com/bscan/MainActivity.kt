package com.bscan

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bscan.model.UpdateStatus
import com.bscan.ScanState
import com.bscan.nfc.NfcManager
import com.bscan.cache.CachedBambuKeyDerivation
import kotlinx.coroutines.launch
import com.bscan.ui.ScanHistoryScreen
import com.bscan.ui.UpdateDialog
import com.bscan.ui.screens.FilamentDetailsScreen
import com.bscan.ui.screens.ScanPromptScreen
import com.bscan.ui.screens.ErrorScreen
import com.bscan.ui.screens.SpoolListScreen
import com.bscan.ui.screens.TrayTrackingScreen
import com.bscan.ui.theme.BScanTheme
import com.bscan.ui.components.ScanStateIndicator
import com.bscan.ui.components.NfcStatusIndicator
import com.bscan.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {
    
    private lateinit var nfcManager: NfcManager
    private val viewModel: MainViewModel by viewModels()
    private val updateViewModel: UpdateViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the key cache system
        CachedBambuKeyDerivation.initialize(this)
        
        nfcManager = NfcManager(this)
        
        if (!nfcManager.isNfcAvailable()) {
            Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        setContent {
            BScanTheme {
                val navController = rememberNavController()
                val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
                val uriHandler = LocalUriHandler.current
                
                // Update dialog
                if (updateUiState.status == UpdateStatus.AVAILABLE || 
                    updateUiState.status == UpdateStatus.DOWNLOADING ||
                    updateUiState.status == UpdateStatus.DOWNLOADED ||
                    updateUiState.status == UpdateStatus.INSTALLING ||
                    (updateUiState.status == UpdateStatus.ERROR && updateUiState.updateInfo != null)) {
                    
                    updateUiState.updateInfo?.let { updateInfo ->
                        UpdateDialog(
                            updateInfo = updateInfo,
                            status = updateUiState.status,
                            downloadProgress = updateUiState.downloadProgress,
                            error = updateUiState.error,
                            onDownload = { updateViewModel.downloadUpdate() },
                            onInstall = { updateViewModel.installUpdate() },
                            onDismiss = { updateViewModel.clearError() },
                            onViewOnGitHub = { 
                                uriHandler.openUri(updateInfo.releaseUrl)
                            },
                            onDismissVersion = { updateViewModel.dismissUpdate() }
                        )
                    }
                }
                
                NavHost(
                    navController = navController,
                    startDestination = "main"
                ) {
                    composable("main") {
                        MainScreen(
                            viewModel = viewModel,
                            updateViewModel = updateViewModel,
                            onResetScan = { viewModel.resetScan() },
                            onShowHistory = { navController.navigate("history") },
                            onShowSpoolList = { navController.navigate("spoolList") },
                            onShowTrayTracking = { navController.navigate("trayTracking") },
                            onPurgeCache = { uid -> nfcManager.invalidateTagCache(uid) }
                        )
                    }
                    
                    composable("history") {
                        ScanHistoryScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("spoolList") {
                        SpoolListScreen(
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    
                    composable("trayTracking") {
                        TrayTrackingScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
        
        // Process intent if launched via NFC
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        
        if (!nfcManager.isNfcEnabled()) {
            viewModel.setNfcError("NFC is disabled")
            return
        }
        
        nfcManager.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        nfcManager.cleanup()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || 
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            // Provide immediate feedback that tag was detected
            viewModel.onTagDetected()
            
            // Provide haptic feedback
            provideHapticFeedback()
            
            // First try quick cache check for immediate response
            val quickTagData = nfcManager.handleIntent(intent)
            if (quickTagData != null) {
                // Cache hit - immediate response
                lifecycleScope.launch {
                    kotlinx.coroutines.delay(300) // Brief delay to show detection
                    viewModel.processTag(quickTagData, nfcManager.debugCollector)
                }
            } else {
                // Cache miss - need background read
                // Show scanning state immediately
                viewModel.setScanning()
                
                lifecycleScope.launch {
                    try {
                        // Perform heavy read operation on background thread
                        val tagData = nfcManager.handleIntentAsync(intent)
                        
                        if (tagData != null) {
                            // Successful read
                            viewModel.processTag(tagData, nfcManager.debugCollector)
                        } else {
                            // Failed read - check if authentication failure
                            if (!nfcManager.debugCollector.hasAuthenticatedSectors()) {
                                // Create a mock tag data for authentication failure case
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
                                } else {
                                    @Suppress("DEPRECATION")
                                    intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
                                }?.let { tag ->
                                    val failedTagData = com.bscan.model.NfcTagData(
                                        uid = tag.id.joinToString("") { "%02X".format(it) },
                                        bytes = ByteArray(0),
                                        technology = tag.techList.firstOrNull() ?: "Unknown"
                                    )
                                    viewModel.setAuthenticationFailed(failedTagData, nfcManager.debugCollector)
                                } ?: viewModel.setNfcError("Error reading tag")
                            } else {
                                viewModel.setNfcError("Error reading tag")
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any unexpected errors
                        viewModel.setNfcError("Error reading tag: ${e.message}")
                    }
                }
            }
        }
    }
    
    private fun provideHapticFeedback() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService<VibratorManager>()
                vibratorManager?.defaultVibrator
            } else {
                getSystemService<Vibrator>()
            }
            
            vibrator?.let {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
                    it.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(100)
                }
            }
        } catch (e: Exception) {
            // Haptic feedback failed, continue silently
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    onResetScan: () -> Unit,
    onShowHistory: () -> Unit,
    onShowSpoolList: () -> Unit,
    onShowTrayTracking: () -> Unit,
    onPurgeCache: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "B-Scan")
                        NfcStatusIndicator(scanState = uiState.scanState)
                    }
                },
                actions = {
                    // Update button - show badge if update is available
                    IconButton(
                        onClick = { updateViewModel.checkForUpdates(force = true) }
                    ) {
                        BadgedBox(
                            badge = {
                                if (updateUiState.status == UpdateStatus.AVAILABLE) {
                                    Badge()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Check for updates"
                            )
                        }
                    }
                    
                    IconButton(onClick = onShowTrayTracking) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "View tray tracking"
                        )
                    }
                    
                    IconButton(onClick = onShowSpoolList) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = "View spool list"
                        )
                    }
                    
                    IconButton(onClick = onShowHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View scan history"
                        )
                    }
                    
                    if (uiState.filamentInfo != null) {
                        IconButton(onClick = onResetScan) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Scan new tag"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (uiState.scanState) {
            ScanState.IDLE -> {
                ScanPromptScreen(modifier = Modifier.padding(paddingValues))
            }
            ScanState.TAG_DETECTED -> {
                TagDetectedScreen(modifier = Modifier.padding(paddingValues))
            }
            ScanState.PROCESSING -> {
                ProcessingScreen(modifier = Modifier.padding(paddingValues))
            }
            ScanState.SUCCESS -> {
                uiState.filamentInfo?.let { filamentInfo ->
                    FilamentDetailsScreen(
                        filamentInfo = filamentInfo,
                        debugInfo = uiState.debugInfo,
                        onPurgeCache = onPurgeCache,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            ScanState.ERROR -> {
                uiState.error?.let { error ->
                    ErrorScreen(
                        error = error,
                        debugInfo = uiState.debugInfo,
                        onRetry = { viewModel.clearError(); viewModel.resetScan() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagDetectedScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScanStateIndicator(
                isDetected = true,
                modifier = androidx.compose.ui.Modifier.size(120.dp)
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
            
            Text(
                text = "Tag Detected!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            
            Text(
                text = "Reading tag data...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProcessingScreen(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ScanStateIndicator(
                isProcessing = true,
                modifier = androidx.compose.ui.Modifier.size(120.dp)
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
            
            Text(
                text = "Reading Tag Data",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            
            Text(
                text = "Keep your device on the tag until complete",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
            
            Text(
                text = "Authenticating and reading sectors...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorScreen(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Try Again")
            }
        }
    }
}