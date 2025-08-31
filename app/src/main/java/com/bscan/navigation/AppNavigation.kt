package com.bscan.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bscan.MainViewModel
import com.bscan.model.UpdateStatus
import com.bscan.model.ComponentCreationResult
import com.bscan.model.IdentifierType
import com.bscan.model.ComponentNavigationPreference
import com.bscan.nfc.NfcManager
import com.bscan.repository.UserDataRepository
import com.bscan.repository.ComponentRepository
import com.bscan.ui.ScanHistoryScreen
import com.bscan.ui.UpdateDialog
import com.bscan.ui.screens.*
import com.bscan.ui.screens.ScanDetailScreen
import com.bscan.ui.screens.TagDetailScreen
import com.bscan.ui.screens.ComponentDetailScreen
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
    val context = LocalContext.current
    
    /**
     * Search for tag UID in component hierarchy (for child tag components)
     */
    fun findTagUidInComponentHierarchy(component: com.bscan.model.Component): String? {
        // Check the component itself
        val directTagUid = component.identifiers
            .find { it.type == IdentifierType.RFID_HARDWARE }
            ?.value
        if (directTagUid != null) return directTagUid
        
        // For Bambu components, the tag UID might be in child components
        // This is a simplified check - in a full implementation, you'd recursively search children
        return null
    }
    
    // Handle navigation after successful scan with sophisticated component navigation
    LaunchedEffect(componentCreationResult) {
        val result = componentCreationResult
        if (result is ComponentCreationResult.Success) {
            val scannedComponent = result.rootComponent
            
            // Get user's navigation preference
            val userDataRepository = UserDataRepository(context)
            val navigationPreference = userDataRepository.getUserData().preferences.componentNavigationPreference
            
            Log.d("AppNavigation", "Navigation preference: $navigationPreference")
            
            // Extract both identifiers for Bambu components
            // The tag UID comes from the scan metadata in the tray component
            val tagUid = scannedComponent.metadata["scannedTagUid"] ?: findTagUidInComponentHierarchy(scannedComponent)
            val trayUid = scannedComponent.identifiers
                .find { it.type == IdentifierType.CONSUMABLE_UNIT }
                ?.value
            
            Log.d("AppNavigation", "Found identifiers - Tag UID: $tagUid, Tray UID: $trayUid")
            
            // Navigate based on user preference and available identifiers
            when (navigationPreference) {
                ComponentNavigationPreference.SCANNED_COMPONENT -> {
                    // Try to navigate to the specific scanned component first
                    if (tagUid != null) {
                        navController.navigate("details/${DetailType.TAG.name.lowercase()}/$tagUid")
                        Log.d("AppNavigation", "Navigating to scanned tag details for UID: $tagUid")
                    } else if (trayUid != null) {
                        // Fallback to tray if no specific tag found
                        navController.navigate("details/${DetailType.INVENTORY_STOCK.name.lowercase()}/$trayUid")
                        Log.d("AppNavigation", "Fallback: Navigating to tray details for UID: $trayUid")
                    } else {
                        Log.e("AppNavigation", "No identifiers found for navigation")
                    }
                }
                
                ComponentNavigationPreference.ROOT_COMPONENT -> {
                    // Navigate to root/tray component (inventory view)
                    if (trayUid != null) {
                        navController.navigate("details/${DetailType.INVENTORY_STOCK.name.lowercase()}/$trayUid")
                        Log.d("AppNavigation", "Navigating to root component (tray) details for UID: $trayUid")
                    } else if (tagUid != null) {
                        // Fallback to tag if no tray found
                        navController.navigate("details/${DetailType.TAG.name.lowercase()}/$tagUid")
                        Log.d("AppNavigation", "Fallback: Navigating to tag details for UID: $tagUid")
                    } else {
                        Log.e("AppNavigation", "No identifiers found for navigation")
                    }
                }
            }
            
            // Reset the component creation result so future scans will trigger navigation
            viewModel.resetComponentState()
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
            
            when (typeStr.lowercase().trim()) {
                "scan" -> {
                    Log.d("AppNavigation", "Showing ScanDetailScreen for identifier: $identifier")
                    ScanDetailScreen(
                        scanId = identifier.trim(),
                        onNavigateBack = { 
                            Log.d("AppNavigation", "Navigating back from ScanDetailScreen")
                            navController.popBackStack() 
                        }
                    )
                }
                "tag" -> {
                    Log.d("AppNavigation", "Showing TagDetailScreen for tag UID: $identifier")
                    TagDetailScreen(
                        tagUid = identifier.trim(),
                        onNavigateBack = { 
                            Log.d("AppNavigation", "Navigating back from TagDetailScreen")
                            navController.popBackStack() 
                        },
                        onNavigateToDetails = { newDetailType, newIdentifier ->
                            if (newIdentifier.isNotBlank()) {
                                Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                                navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                            } else {
                                Log.e("AppNavigation", "Attempted navigation with blank identifier")
                            }
                        }
                    )
                }
                "component" -> {
                    Log.d("AppNavigation", "Showing ComponentDetailScreen for component ID: $identifier")
                    ComponentDetailScreen(
                        componentId = identifier.trim(),
                        onNavigateBack = { 
                            Log.d("AppNavigation", "Navigating back from ComponentDetailScreen")
                            navController.popBackStack() 
                        },
                        onNavigateToDetails = { newDetailType, newIdentifier ->
                            if (newIdentifier.isNotBlank()) {
                                Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                                navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                            } else {
                                Log.e("AppNavigation", "Attempted navigation with blank identifier")
                            }
                        }
                    )
                }
                else -> {
                    val detailType = when (typeStr.lowercase().trim()) {
                        "spool" -> DetailType.INVENTORY_STOCK
                        "inventory_stock" -> DetailType.INVENTORY_STOCK
                        "sku" -> DetailType.SKU
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
    }
}