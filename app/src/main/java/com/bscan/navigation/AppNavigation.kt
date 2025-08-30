package com.bscan.navigation

import android.util.Log
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
import com.bscan.model.UpdateStatus
import com.bscan.model.ComponentCreationResult
import com.bscan.nfc.NfcManager
import com.bscan.repository.ComponentRepository
import com.bscan.ui.ScanHistoryScreen
import com.bscan.ui.UpdateDialog
import com.bscan.ui.screens.*
import com.bscan.viewmodel.UpdateViewModel
import com.bscan.ble.BlePermissionHandler

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    nfcManager: NfcManager?,
    blePermissionHandler: BlePermissionHandler?,
    navController: NavHostController = rememberNavController()
) {
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val componentCreationResult by viewModel.componentCreationResult.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    
    // Handle navigation after successful component creation
    LaunchedEffect(componentCreationResult) {
        val result = componentCreationResult
        if (result is ComponentCreationResult.Success) {
            val scannedComponent = result.rootComponent
            
            // Find the inventory item that contains this component (could be the component itself or its parent)
            val inventoryItem = if (scannedComponent.isInventoryItem) {
                // Component is already an inventory item
                scannedComponent
            } else {
                // Find the root component (inventory item) that contains this component
                val componentRepository = ComponentRepository(viewModel.getApplication())
                componentRepository.getRootComponent(scannedComponent.id)
            }
            
            inventoryItem?.let { item ->
                // Navigate to the inventory item details
                navController.navigate("details/${DetailType.COMPONENT.name.lowercase()}/${item.id}") {
                    // Optional: clear the back stack to prevent going back to scanning state
                    // popUpTo("main") { inclusive = false }
                }
                
                // Reset the component creation result so future scans will trigger navigation
                viewModel.resetComponentState()
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
                onSimulateScan = { /* Development feature: simulate scan functionality */ },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") },
                onNavigateToDetails = { detailType, identifier ->
                    navController.navigate("details/${detailType.name.lowercase()}/$identifier")
                }
            )
        }
        
        composable("history") {
            ScanHistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { detailType, identifier ->
                    navController.navigate("details/${detailType.name.lowercase()}/$identifier")
                }
            )
        }
        
        composable("spoolList") {
            SpoolListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { detailType, identifier ->
                    navController.navigate("details/${detailType.name.lowercase()}/$identifier")
                }
            )
        }
        
        composable("inventory") {
            InventoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { detailType, identifier ->
                    navController.navigate("details/${detailType.name.lowercase()}/$identifier")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToComponents = { navController.navigate("components") },
                blePermissionHandler = blePermissionHandler
            )
        }
        
        composable("components") {
            ComponentsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("details/{type}/{identifier}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type")
            val identifier = backStackEntry.arguments?.getString("identifier")
            
            Log.d("AppNavigation", "Navigating to details with type: $typeStr, identifier: $identifier")
            
            // Validate parameters
            if (typeStr.isNullOrBlank()) {
                Log.e("AppNavigation", "Missing or empty type parameter in navigation")
                // Navigate back to main screen on invalid parameters
                LaunchedEffect(Unit) {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
                return@composable
            }
            
            if (identifier.isNullOrBlank()) {
                Log.e("AppNavigation", "Missing or empty identifier parameter in navigation")
                // Navigate back to main screen on invalid parameters
                LaunchedEffect(Unit) {
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
                return@composable
            }
            
            val detailType = when (typeStr.lowercase().trim()) {
                "scan" -> DetailType.SCAN
                "tag" -> DetailType.TAG
                "spool" -> DetailType.INVENTORY_STOCK
                "inventory_stock" -> DetailType.INVENTORY_STOCK
                "sku" -> DetailType.SKU
                "component" -> DetailType.COMPONENT
                else -> {
                    Log.e("AppNavigation", "Unknown detail type: $typeStr")
                    // Navigate back to main screen on unknown type
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                    return@composable
                }
            }
            
            Log.d("AppNavigation", "Valid navigation parameters, showing DetailScreen for $detailType")
            
            DetailScreen(
                detailType = detailType,
                identifier = identifier.trim(),
                onNavigateBack = { 
                    Log.d("AppNavigation", "Navigating back from DetailScreen")
                    navController.popBackStack() 
                },
                onNavigateToDetails = { newDetailType, newIdentifier ->
                    if (newIdentifier.isNotBlank()) {
                        Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                        navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                    } else {
                        Log.e("AppNavigation", "Attempted navigation with blank identifier")
                    }
                },
                onPurgeCache = { tagUid ->
                    Log.d("AppNavigation", "Cache purge requested for tagUid: $tagUid")
                    nfcManager?.let { manager ->
                        manager.invalidateTagCache(tagUid)
                        Log.d("AppNavigation", "Cache purged for tagUid: $tagUid")
                    } ?: run {
                        Log.w("AppNavigation", "NFC not available - cannot purge cache for tagUid: $tagUid")
                    }
                }
            )
        }
        }
    }
}