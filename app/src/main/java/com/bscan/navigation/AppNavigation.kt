package com.bscan.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bscan.MainViewModel
import com.bscan.ScanState
import com.bscan.model.UpdateStatus
import com.bscan.nfc.NfcManager
import com.bscan.ui.ScanHistoryScreen
import com.bscan.ui.UpdateDialog
import com.bscan.ui.screens.*
import com.bscan.viewmodel.UpdateViewModel

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    nfcManager: NfcManager,
    navController: NavHostController = rememberNavController()
) {
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val scanUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    
    // Handle navigation after successful scan
    LaunchedEffect(scanUiState.scanState, scanUiState.filamentInfo) {
        if (scanUiState.scanState == ScanState.SUCCESS) {
            scanUiState.filamentInfo?.let { filamentInfo ->
                val trayUid = filamentInfo.trayUid
                // Navigate to details page after successful scan
                navController.navigate("details/$trayUid") {
                    // Optional: clear the back stack to prevent going back to scanning state
                    // popUpTo("main") { inclusive = false }
                }
            }
        }
    }
    
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHost(
            navController = navController,
            startDestination = "main"
        ) {
        composable("main") {
            HomeScreen(
                scanState = viewModel.uiState.collectAsStateWithLifecycle().value.scanState,
                scanProgress = viewModel.scanProgress.collectAsStateWithLifecycle().value,
                onSimulateScan = { /* TODO: Add simulate scan functionality if needed */ },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToDetails = { trayUid ->
                    navController.navigate("details/$trayUid")
                }
            )
        }
        
        composable("history") {
            ScanHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { trayUid ->
                    navController.navigate("details/$trayUid")
                }
            )
        }
        
        composable("spoolList") {
            SpoolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { trayUid ->
                    navController.navigate("details/$trayUid")
                }
            )
        }
        
        composable("trayTracking") {
            TrayTrackingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("details/{trayUid}") { backStackEntry ->
            val trayUid = backStackEntry.arguments?.getString("trayUid") ?: return@composable
            SpoolDetailsScreen(
                trayUid = trayUid,
                onNavigateBack = { navController.popBackStack() },
                onPurgeCache = { tagUid ->
                    // Handle cache purging if needed
                    // This could call the existing cache management functionality
                }
            )
        }
        }
    }
}