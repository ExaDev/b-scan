package com.bscan.navigation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Alignment
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
import com.bscan.model.IdentifierType
import com.bscan.model.ComponentNavigationPreference
import com.bscan.model.graph.entities.*
import com.bscan.model.graph.Entity
import com.bscan.repository.GraphEntityCreationResult
import com.bscan.nfc.NfcManager
import com.bscan.repository.UserDataRepository
import com.bscan.ui.ScanHistoryScreen
import com.bscan.ui.UpdateDialog
import com.bscan.ui.screens.*
import com.bscan.ui.screens.ScanDetailScreen
import com.bscan.ui.screens.TagDetailScreen
import com.bscan.ui.screens.ComponentDetailScreen
import com.bscan.ui.screens.DetailType
import com.bscan.viewmodel.UpdateViewModel
import com.bscan.ble.BlePermissionHandler
import kotlinx.coroutines.launch

/**
 * Helper functions to resolve entity IDs from legacy identifiers
 */
suspend fun resolveEntityIdFromLegacyIdentifier(
    context: android.content.Context,
    detailType: DetailType,
    identifier: String
): String? {
    val graphRepository = com.bscan.repository.GraphRepository(context)
    
    Log.d("AppNavigation", "Resolving $detailType with identifier: '$identifier'")
    
    return when (detailType) {
        DetailType.COMPONENT -> {
            // Try direct entity lookup first (identifier might be entity ID)
            Log.d("AppNavigation", "Trying direct entity lookup for: '$identifier'")
            val directEntity = graphRepository.getEntity(identifier)
            if (directEntity != null) {
                Log.d("AppNavigation", "Found entity directly: ${directEntity.entityType} - ${directEntity.label}")
                return directEntity.id
            }
            
            // Get all physical components and see if any match
            Log.d("AppNavigation", "Searching all physical_component entities")
            val physicalComponents = graphRepository.getEntitiesByType("physical_component")
            Log.d("AppNavigation", "Found ${physicalComponents.size} physical components")
            physicalComponents.firstOrNull()?.id
        }
        DetailType.TAG -> {
            // Get all identifier entities and find RFID ones
            Log.d("AppNavigation", "Searching all identifier entities for tag: '$identifier'")
            val identifiers = graphRepository.getEntitiesByType("identifier")
            Log.d("AppNavigation", "Found ${identifiers.size} identifier entities")
            
            for (entity in identifiers) {
                Log.d("AppNavigation", "Identifier entity: ${entity.label}, properties: ${entity.properties.keys}")
                if (entity.getProperty<String>("value") == identifier) {
                    Log.d("AppNavigation", "Found matching identifier entity: ${entity.id}")
                    return entity.id
                }
            }
            null
        }
        DetailType.SCAN -> {
            // Get all activity entities
            Log.d("AppNavigation", "Searching all activity entities for scan: '$identifier'")
            val activities = graphRepository.getEntitiesByType("activity")
            Log.d("AppNavigation", "Found ${activities.size} activity entities")
            activities.firstOrNull()?.id
        }
        DetailType.INVENTORY_STOCK -> {
            // Get all inventory items
            Log.d("AppNavigation", "Searching all inventory_item entities")
            val inventoryItems = graphRepository.getEntitiesByType("inventory_item")
            Log.d("AppNavigation", "Found ${inventoryItems.size} inventory items")
            inventoryItems.firstOrNull()?.id
        }
        DetailType.SKU -> {
            // Use GraphRepository to find StockDefinition entities (now persisted)
            Log.d("AppNavigation", "Resolving SKU with identifier: '$identifier'")
            
            // First try direct SKU lookup
            val directMatches = graphRepository.findEntitiesByProperties(
                mapOf("sku" to com.bscan.model.graph.PropertyValue.StringValue(identifier))
            ).filter { it.entityType == "stock_definition" }
            if (directMatches.isNotEmpty()) {
                Log.d("AppNavigation", "Found direct SKU match: ${directMatches.first().id}")
                return directMatches.first().id
            }
            
            // Try compound format: manufacturerId:sku
            val parts = identifier.split(":", limit = 2)
            if (parts.size == 2) {
                val manufacturerId = parts.getOrNull(0)
                val sku = parts.getOrNull(1)
                Log.d("AppNavigation", "Parsed identifier - ManufacturerID: '$manufacturerId', SKU: '$sku'")
                
                if (sku != null && manufacturerId != null) {
                    // Search for stock definitions matching both manufacturer and SKU
                    val allStockDefs = graphRepository.getEntitiesByType("stock_definition")
                    val compoundMatch = allStockDefs.find { entity ->
                        val entitySku = entity.getProperty<String>("sku")
                        val entityManufacturerId = entity.getProperty<String>("manufacturerId") ?: 
                                                  entity.getProperty<String>("manufacturer")
                        entitySku == sku && (entityManufacturerId == manufacturerId || 
                                           entityManufacturerId?.lowercase()?.contains(manufacturerId.lowercase()) == true)
                    }
                    
                    if (compoundMatch != null) {
                        Log.d("AppNavigation", "Found compound SKU match: ${compoundMatch.id}")
                        return compoundMatch.id
                    }
                }
            }
            
            Log.e("AppNavigation", "Failed to resolve sku identifier: $identifier")
            null
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    nfcManager: NfcManager?,
    blePermissionHandler: BlePermissionHandler?,
    navController: NavHostController = rememberNavController()
) {
    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val graphEntityCreationResult by viewModel.graphEntityCreationResult.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    
    /**
     * Extract identifier from graph entity
     */
    suspend fun findIdentifierInEntity(entity: Entity, identifierType: String): String? {
        return viewModel.getEntityIdentifiers(entity.id)
            .find { it.identifierType == identifierType }
            ?.value
    }
    
    // Handle navigation after successful graph entity creation
    LaunchedEffect(graphEntityCreationResult) {
        val result = graphEntityCreationResult
        if (result != null && result.success) {
            val rootEntity = result.rootEntity
            val scannedEntity = result.scannedEntity
            
            if (rootEntity == null || scannedEntity == null) {
                Log.e("AppNavigation", "Missing root or scanned entity in graph result")
                return@LaunchedEffect
            }
            
            // Get user's navigation preference
            val userDataRepository = UserDataRepository(context)
            val navigationPreference = userDataRepository.getUserData().preferences.componentNavigationPreference
            
            Log.d("AppNavigation", "Graph navigation preference: $navigationPreference")
            
            // Extract identifiers from graph entities
            val tagUid = findIdentifierInEntity(scannedEntity, IdentifierTypes.RFID_HARDWARE)
            val trayUid = findIdentifierInEntity(rootEntity, IdentifierTypes.CONSUMABLE_UNIT)
            
            Log.d("AppNavigation", "Graph identifiers - Tag UID: $tagUid, Tray UID: $trayUid")
            
            // Navigate based on user preference and available identifiers
            when (navigationPreference) {
                ComponentNavigationPreference.SCANNED_COMPONENT -> {
                    // Navigate to the specific scanned entity
                    navController.navigate("details/entity/${scannedEntity.id}")
                    Log.d("AppNavigation", "Graph: Navigating to scanned entity: ${scannedEntity.label} (ID: ${scannedEntity.id})")
                }
                
                ComponentNavigationPreference.ROOT_COMPONENT -> {
                    // Navigate to root/tray component (main inventory item)
                    navController.navigate("details/entity/${rootEntity.id}")
                    Log.d("AppNavigation", "Graph: Navigating to root entity: ${rootEntity.label} (ID: ${rootEntity.id})")
                }
            }
            
            // Reset the graph creation result so future scans will trigger navigation
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
                onNavigateToGraph = { navController.navigate("graph") },
                onNavigateToDetails = { detailType, identifier ->
                    // Handle entity navigation specially
                    if (identifier.startsWith("entity/")) {
                        navController.navigate("details/$identifier")
                    } else {
                        navController.navigate("details/${detailType.name.lowercase()}/$identifier")
                    }
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
        
        composable("graph") {
            GraphVisualizationScreen(
                navController = navController
            )
        }
        
        composable("details/{type}/{identifier}") { backStackEntry ->
            val typeStr = backStackEntry.arguments?.getString("type")
            val identifier = backStackEntry.arguments?.getString("identifier")
            
            Log.d("AppNavigation", "=== NAVIGATION DEBUG ===")
            Log.d("AppNavigation", "Navigating to details with type: $typeStr, identifier: $identifier")
            Log.d("AppNavigation", "Raw route: ${backStackEntry.destination.route}")
            Log.d("AppNavigation", "Arguments: ${backStackEntry.arguments}")
            Log.d("AppNavigation", "=========================")
            
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
                "scan", "tag", "component" -> {
                    // Resolve legacy identifier to entity ID and redirect to unified view
                    val legacyDetailType = when (typeStr.lowercase().trim()) {
                        "scan" -> DetailType.SCAN
                        "tag" -> DetailType.TAG
                        "component" -> DetailType.COMPONENT
                        else -> DetailType.COMPONENT
                    }
                    
                    var entityId by remember { mutableStateOf<String?>(null) }
                    var isResolving by remember { mutableStateOf(true) }
                    var resolveError by remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(identifier) {
                        try {
                            val resolvedId = resolveEntityIdFromLegacyIdentifier(
                                context = context,
                                detailType = legacyDetailType,
                                identifier = identifier.trim()
                            )
                            if (resolvedId != null) {
                                entityId = resolvedId
                                    Log.d("AppNavigation", "✅ Successfully resolved ${typeStr} '$identifier' to entity ID: $resolvedId")
                            } else {
                                resolveError = "Could not find entity for ${typeStr}: $identifier"
                                Log.e("AppNavigation", "Failed to resolve ${typeStr} identifier: $identifier")
                            }
                        } catch (e: Exception) {
                            resolveError = "Error resolving ${typeStr}: ${e.message}"
                            Log.e("AppNavigation", "Exception resolving ${typeStr} identifier", e)
                        } finally {
                            isResolving = false
                        }
                    }
                    
                    when {
                        isResolving -> {
                            // Show loading while resolving
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        resolveError != null -> {
                            // Show error if resolution failed
                            Log.e("AppNavigation", "Resolution error: $resolveError")
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        }
                        entityId != null -> {
                            // Show unified entity detail view
                            Log.d("AppNavigation", "Showing EntityDetailScreen for resolved entity ID: $entityId")
                            EntityDetailScreen(
                                entityId = entityId!!,
                                onNavigateBack = { 
                                    Log.d("AppNavigation", "Navigating back from EntityDetailScreen")
                                    navController.popBackStack() 
                                },
                                onNavigateToDetails = { newDetailType, newIdentifier ->
                                    if (newIdentifier.isNotBlank()) {
                                        Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                                        // Handle entity navigation specially
                                        if (newIdentifier.startsWith("entity/")) {
                                            navController.navigate("details/$newIdentifier")
                                        } else {
                                            navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                                        }
                                    } else {
                                        Log.e("AppNavigation", "Attempted navigation with blank identifier")
                                    }
                                }
                            )
                        }
                    }
                }
                "entity" -> {
                    Log.d("AppNavigation", "Showing EntityDetailScreen for entity ID: $identifier")
                    EntityDetailScreen(
                        entityId = identifier.trim(),
                        onNavigateBack = { 
                            Log.d("AppNavigation", "Navigating back from EntityDetailScreen")
                            navController.popBackStack() 
                        },
                        onNavigateToDetails = { newDetailType, newIdentifier ->
                            if (newIdentifier.isNotBlank()) {
                                Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                                // Handle entity navigation specially
                                if (newIdentifier.startsWith("entity/")) {
                                    navController.navigate("details/$newIdentifier")
                                } else {
                                    navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                                }
                            } else {
                                Log.e("AppNavigation", "Attempted navigation with blank identifier")
                            }
                        }
                    )
                }
                "spool", "inventory_stock", "sku" -> {
                    // Resolve legacy identifier to entity ID and redirect to unified view
                    val legacyDetailType = when (typeStr.lowercase().trim()) {
                        "spool", "inventory_stock" -> DetailType.INVENTORY_STOCK
                        "sku" -> DetailType.SKU
                        else -> DetailType.INVENTORY_STOCK
                    }
                    
                    var entityId by remember { mutableStateOf<String?>(null) }
                    var isResolving by remember { mutableStateOf(true) }
                    var resolveError by remember { mutableStateOf<String?>(null) }
                    
                    LaunchedEffect(identifier) {
                        try {
                            val resolvedId = resolveEntityIdFromLegacyIdentifier(
                                context = context,
                                detailType = legacyDetailType,
                                identifier = identifier.trim()
                            )
                            if (resolvedId != null) {
                                entityId = resolvedId
                                    Log.d("AppNavigation", "✅ Successfully resolved ${typeStr} '$identifier' to entity ID: $resolvedId")
                            } else {
                                resolveError = "Could not find entity for ${typeStr}: $identifier"
                                Log.e("AppNavigation", "Failed to resolve ${typeStr} identifier: $identifier")
                            }
                        } catch (e: Exception) {
                            resolveError = "Error resolving ${typeStr}: ${e.message}"
                            Log.e("AppNavigation", "Exception resolving ${typeStr} identifier", e)
                        } finally {
                            isResolving = false
                        }
                    }
                    
                    when {
                        isResolving -> {
                            // Show loading while resolving
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        resolveError != null -> {
                            // Show error if resolution failed
                            Log.e("AppNavigation", "Resolution error: $resolveError")
                            LaunchedEffect(Unit) {
                                navController.navigate("main") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        }
                        entityId != null -> {
                            // Show unified entity detail view
                            Log.d("AppNavigation", "Showing EntityDetailScreen for resolved entity ID: $entityId")
                            EntityDetailScreen(
                                entityId = entityId!!,
                                onNavigateBack = { 
                                    Log.d("AppNavigation", "Navigating back from EntityDetailScreen")
                                    navController.popBackStack() 
                                },
                                onNavigateToDetails = { newDetailType, newIdentifier ->
                                    if (newIdentifier.isNotBlank()) {
                                        Log.d("AppNavigation", "Navigating to new details: $newDetailType, $newIdentifier")
                                        // Handle entity navigation specially
                                        if (newIdentifier.startsWith("entity/")) {
                                            navController.navigate("details/$newIdentifier")
                                        } else {
                                            navController.navigate("details/${newDetailType.name.lowercase()}/$newIdentifier")
                                        }
                                    } else {
                                        Log.e("AppNavigation", "Attempted navigation with blank identifier")
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    Log.e("AppNavigation", "Unknown detail type: $typeStr")
                    // Navigate back to main screen on unknown type
                    LaunchedEffect(Unit) {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                }
            }
        }
        }
    }
}