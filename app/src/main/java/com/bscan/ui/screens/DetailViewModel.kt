package com.bscan.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.repository.UnifiedDataAccess
import com.bscan.repository.FilamentReelDetails
import com.bscan.repository.UniqueFilamentReel
import com.bscan.repository.InterpretedScan
import com.bscan.ui.screens.home.SkuInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

enum class DetailType {
    SCAN, TAG, INVENTORY_STOCK, SKU
}

data class DetailUiState(
    val isLoading: Boolean = true,
    val detailType: DetailType = DetailType.INVENTORY_STOCK,
    val identifier: String = "",
    val error: String? = null,
    // Primary entity data
    val primaryScan: InterpretedScan? = null,
    val primaryTag: InterpretedScan? = null, // Most recent scan for this tag UID
    val primarySpool: FilamentReelDetails? = null,
    val primarySku: SkuInfo? = null,
    // Related data
    val relatedScans: List<InterpretedScan> = emptyList(),
    val relatedTags: List<String> = emptyList(), // Tag UIDs
    val relatedFilamentReels: List<UniqueFilamentReel> = emptyList(),
    val relatedSkus: List<SkuInfo> = emptyList()
)

class DetailViewModel(private val unifiedDataAccess: UnifiedDataAccess) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    
    fun loadDetails(detailType: DetailType, identifier: String) {
        Log.d("DetailViewModel", "Loading details for type: $detailType, identifier: $identifier")
        
        // Validate inputs before proceeding
        if (identifier.isBlank()) {
            Log.e("DetailViewModel", "Empty identifier provided for detail type: $detailType")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid identifier provided"
            )
            return
        }
        
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            detailType = detailType,
            identifier = identifier,
            error = null
        )
        
        viewModelScope.launch {
            try {
                Log.d("DetailViewModel", "Starting load operation for $detailType")
                
                withContext(Dispatchers.IO) {
                    when (detailType) {
                        DetailType.SCAN -> loadScanDetails(identifier)
                        DetailType.TAG -> loadTagDetails(identifier)
                        DetailType.INVENTORY_STOCK -> loadSpoolDetails(identifier)
                        DetailType.SKU -> loadSkuDetails(identifier)
                    }
                }
                
                Log.d("DetailViewModel", "Successfully loaded details for $detailType")
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to load details for $detailType", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load details: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    private suspend fun loadScanDetails(scanIdentifier: String) {
        // Scan identifier format: "formatted_timestamp_uid"
        val parts = scanIdentifier.split("_")
        if (parts.size < 2) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid scan identifier"
            )
            return
        }
        
        val uid = parts.last()
        val timestampStr = parts.dropLast(1).joinToString("_")
        
        val allScans = unifiedDataAccess.getAllScans()
        
        // Try to find scan by generating the same ID format and comparing
        val primaryScan = allScans.find { scan ->
            val generatedId = "${scan.timestamp.toString().replace(":", "-").replace(".", "-")}_${scan.uid}"
            generatedId == scanIdentifier
        } ?: allScans.find { scan ->
            // Fallback: match by UID only if exact match fails
            scan.uid == uid
        }
        
        if (primaryScan == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Scan not found: $scanIdentifier"
            )
            return
        }
        
        // Get related data
        val trayUid = primaryScan.filamentInfo?.trayUid
        val relatedSpools = if (trayUid != null) {
            unifiedDataAccess.getFilamentReelDetails(trayUid)?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        
        val relatedTags = if (trayUid != null) {
            allScans
                .filter { it.filamentInfo?.trayUid == trayUid }
                .map { it.uid }
                .distinct()
        } else {
            listOf(uid)
        }
        
        val relatedScans = allScans.filter { it.uid == uid }
        val associatedSku = getAssociatedSku(primaryScan.filamentInfo)
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primaryScan = primaryScan,
            relatedScans = relatedScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = associatedSku
        )
    }
    
    private suspend fun loadTagDetails(tagUid: String) {
        val allScans = unifiedDataAccess.getAllScans()
        val tagScans = unifiedDataAccess.getScansByTagUid(tagUid)
        val primaryTag = tagScans.maxByOrNull { it.timestamp }
        
        if (primaryTag == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Tag not found"
            )
            return
        }
        
        // Get related data
        val trayUid = primaryTag.filamentInfo?.trayUid
        val relatedSpools = if (trayUid != null) {
            unifiedDataAccess.getFilamentReelDetails(trayUid)?.let { listOf(it) } ?: emptyList()
        } else {
            emptyList()
        }
        
        val relatedTags = if (trayUid != null) {
            allScans
                .filter { it.filamentInfo?.trayUid == trayUid }
                .map { it.uid }
                .distinct()
        } else {
            listOf(tagUid)
        }
        
        val associatedSku = getAssociatedSku(primaryTag.filamentInfo)
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primaryTag = primaryTag,
            relatedScans = tagScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = associatedSku
        )
    }
    
    private suspend fun loadSpoolDetails(trayUid: String) {
        Log.d("DetailViewModel", "Loading spool details for trayUid: '$trayUid'")
        
        // Debug: List all available data
        val allScans = unifiedDataAccess.getAllScans()
        Log.d("DetailViewModel", "Available scan count: ${allScans.size}")
        allScans.take(5).forEach { scan ->
            Log.d("DetailViewModel", "Available scan - uid: '${scan.uid}', trayUid: '${scan.filamentInfo?.trayUid}', colorName: '${scan.filamentInfo?.colorName}'")
        }
        
        // Debug: Show scan data patterns to understand the identifier mismatch
        Log.d("DetailViewModel", "Looking for data matching identifier: '$trayUid'")
        
        try {
            val filamentReelDetails = unifiedDataAccess.getFilamentReelDetails(trayUid)
            
            if (filamentReelDetails == null) {
                Log.w("DetailViewModel", "No filament reel found for trayUid: '$trayUid'")
                
                // Try component system approach - maybe the ID is for an inventory item
                val inventoryItem = unifiedDataAccess.getInventoryItem(trayUid)
                if (inventoryItem != null) {
                    Log.d("DetailViewModel", "Found inventory item for ID: $trayUid, redirecting to component view")
                    // Handle inventory item display
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Component-based inventory items not yet supported in detail view. Found inventory item with ${inventoryItem.components.size} components."
                    )
                    return
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Inventory item not found for ID: $trayUid"
                )
                return
            }
            
            Log.d("DetailViewModel", "Found filament reel details, loading associated data")
            
            // Safely get associated SKU with error handling
            val associatedSku = try {
                getAssociatedSku(filamentReelDetails.filamentInfo)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to get associated SKU for filament reel", e)
                emptyList()
            }
            
            // Safely convert to unique component representation
            val uniqueComponent = try {
                filamentReelDetailsToUniqueComponent(filamentReelDetails)
            } catch (e: Exception) {
                Log.e("DetailViewModel", "Failed to convert filament reel details to unique component", e)
                null
            }
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                primarySpool = filamentReelDetails,
                relatedScans = filamentReelDetails.allScans,
                relatedTags = filamentReelDetails.tagUids,
                relatedFilamentReels = if (uniqueComponent != null) listOf(uniqueComponent) else emptyList(),
                relatedSkus = associatedSku
            )
            
            Log.d("DetailViewModel", "Successfully loaded filament reel details with ${filamentReelDetails.allScans.size} related scans")
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Exception loading filament reel details", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Failed to load filament reel details: ${e.message}"
            )
        }
    }
    
    private suspend fun loadSkuDetails(skuKey: String) {
        val allScans = unifiedDataAccess.getAllScans()
        
        // Parse the SKU key to understand what we're looking for
        val parts = skuKey.split("-")
        val filamentType = parts.getOrNull(0) ?: ""
        val colorName = parts.getOrNull(1) ?: ""
        
        // Find scans that match this SKU pattern - include ALL scans, not just successful ones
        // This ensures we show actual scanned data even for incomplete/failed scans
        var skuScans = allScans.filter { scan ->
            scan.filamentInfo?.let { info ->
                val scanKey = "${info.filamentType}-${info.colorName}"
                scanKey == skuKey
            } ?: false
        }
        
        // If still no exact matches, try more flexible matching for incomplete data
        if (skuScans.isEmpty()) {
            skuScans = allScans.filter { scan ->
                scan.filamentInfo?.let { info ->
                    // Match if filament type matches and color is empty/null (for cases like "PLA-")
                    if (colorName.isEmpty() || colorName.isBlank()) {
                        info.filamentType == filamentType && (info.colorName.isEmpty() || info.colorName.isBlank())
                    } else {
                        // Try partial matching
                        info.filamentType.contains(filamentType, ignoreCase = true) ||
                        info.colorName.contains(colorName, ignoreCase = true)
                    }
                } ?: false
            }
        }
        
        // Even broader search - try to find any scan with similar filament type
        if (skuScans.isEmpty() && filamentType.isNotEmpty()) {
            skuScans = allScans.filter { scan ->
                scan.filamentInfo?.filamentType?.contains(filamentType, ignoreCase = true) == true
            }
        }
        
        // Final fallback - if we still have nothing, create a synthetic entry from the SKU key itself
        if (skuScans.isEmpty()) {
            // We'll create a minimal SkuInfo based on the parsed key information
            val syntheticFilamentInfo = createSyntheticFilamentInfo(filamentType, colorName)
            val primarySku = SkuInfo(
                skuKey = skuKey,
                filamentInfo = syntheticFilamentInfo,
                filamentReelCount = 0,
                totalScans = 0,
                successfulScans = 0,
                lastScanned = LocalDateTime.now(),
                successRate = 0f
            )
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                primarySku = primarySku,
                relatedScans = emptyList(),
                relatedTags = emptyList(),
                relatedFilamentReels = emptyList(),
                relatedSkus = listOf(primarySku)
            )
            return
        }
        
        val mostRecentScan = skuScans.maxByOrNull { it.timestamp }!!
        val filamentInfo = mostRecentScan.filamentInfo!!
        
        val uniqueFilamentReels = skuScans.groupBy { it.filamentInfo!!.trayUid }.size
        val totalScans = skuScans.size
        val successfulScans = skuScans.count { it.scanResult == com.bscan.model.ScanResult.SUCCESS }
        val lastScanned = skuScans.maxByOrNull { it.timestamp }?.timestamp ?: LocalDateTime.now()
        
        val primarySku = SkuInfo(
            skuKey = skuKey,
            filamentInfo = filamentInfo,
            filamentReelCount = uniqueFilamentReels,
            totalScans = totalScans,
            successfulScans = successfulScans,
            lastScanned = lastScanned,
            successRate = if (totalScans > 0) successfulScans.toFloat() / totalScans else 0f
        )
        
        // Get related spools and tags
        val relatedSpools = skuScans
            .groupBy { it.filamentInfo!!.trayUid }
            .mapNotNull { (trayUid, scans) ->
                unifiedDataAccess.getFilamentReelDetails(trayUid)
            }
        
        val relatedTags = skuScans.map { it.uid }.distinct()
        
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            primarySku = primarySku,
            relatedScans = skuScans,
            relatedTags = relatedTags,
            relatedFilamentReels = relatedSpools.map { filamentReelDetailsToUniqueFilamentReel(it) },
            relatedSkus = listOf(primarySku)
        )
    }
    
    private fun getAssociatedSku(filamentInfo: com.bscan.model.FilamentInfo?): List<SkuInfo> {
        if (filamentInfo == null) {
            Log.d("DetailViewModel", "No filament info provided for SKU association")
            return emptyList()
        }
        
        try {
            val allScans = unifiedDataAccess.getAllScans()
            
            // Find scans that match BOTH filament type AND color for this specific SKU
            val skuScans = allScans.filter { scan ->
                try {
                    scan.filamentInfo?.filamentType == filamentInfo.filamentType &&
                    scan.filamentInfo?.colorName == filamentInfo.colorName
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error filtering scan for SKU association", e)
                    false
                }
            }
            
            if (skuScans.isEmpty()) {
                Log.d("DetailViewModel", "No matching scans found for SKU: ${filamentInfo.filamentType}-${filamentInfo.colorName}")
                return emptyList()
            }
            
            // Create the single associated SKU
            val skuKey = "${filamentInfo.filamentType}-${filamentInfo.colorName}"
            val successfulScans = skuScans.filter { 
                try {
                    it.scanResult == com.bscan.model.ScanResult.SUCCESS 
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error checking scan result", e)
                    false
                }
            }
            
            if (successfulScans.isNotEmpty()) {
                val mostRecentScan = successfulScans.maxByOrNull { it.timestamp }
                val info = mostRecentScan?.filamentInfo ?: filamentInfo
                
                val uniqueFilamentReels = try {
                    skuScans.groupBy { it.filamentInfo?.trayUid ?: "unknown" }.size
                } catch (e: Exception) {
                    Log.w("DetailViewModel", "Error calculating unique filament reels", e)
                    1
                }
                
                val totalScans = skuScans.size
                val successfulCount = successfulScans.size
                val lastScanned = skuScans.maxByOrNull { it.timestamp }?.timestamp
                
                val sku = SkuInfo(
                    skuKey = skuKey,
                    filamentInfo = info,
                    filamentReelCount = uniqueFilamentReels,
                    totalScans = totalScans,
                    successfulScans = successfulCount,
                    lastScanned = lastScanned ?: LocalDateTime.now(),
                    successRate = if (totalScans > 0) successfulCount.toFloat() / totalScans else 0f
                )
                
                Log.d("DetailViewModel", "Created SKU info: $skuKey with ${totalScans} scans")
                return listOf(sku)
            }
            
            Log.d("DetailViewModel", "No successful scans found for SKU: $skuKey")
            return emptyList()
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error getting associated SKU", e)
            return emptyList()
        }
    }
    
    private fun filamentReelDetailsToUniqueComponent(filamentReelDetails: FilamentReelDetails): UniqueFilamentReel {
        return try {
            UniqueFilamentReel(
                uid = filamentReelDetails.trayUid, // Use tray UID as the identifier
                filamentInfo = filamentReelDetails.filamentInfo,
                scanCount = filamentReelDetails.totalScans,
                successCount = filamentReelDetails.successfulScans,
                lastScanned = filamentReelDetails.lastScanned,
                successRate = if (filamentReelDetails.totalScans > 0) 
                    filamentReelDetails.successfulScans.toFloat() / filamentReelDetails.totalScans else 0f
            )
        } catch (e: Exception) {
            Log.e("DetailViewModel", "Error converting filament reel details to unique component", e)
            // Return a minimal valid component if conversion fails
            UniqueFilamentReel(
                uid = filamentReelDetails.trayUid,
                filamentInfo = filamentReelDetails.filamentInfo,
                scanCount = 0,
                successCount = 0,
                lastScanned = LocalDateTime.now(),
                successRate = 0f
            )
        }
    }
    
    private fun filamentReelDetailsToUniqueFilamentReel(filamentReelDetails: FilamentReelDetails): UniqueFilamentReel {
        return filamentReelDetailsToUniqueComponent(filamentReelDetails)
    }
    
    private fun createSyntheticFilamentInfo(filamentType: String, colorName: String): com.bscan.model.FilamentInfo {
        return com.bscan.model.FilamentInfo(
            tagUid = "unknown",
            trayUid = "unknown",
            filamentType = filamentType.ifEmpty { "Unknown Material" },
            detailedFilamentType = filamentType.ifEmpty { "Unknown Material" },
            colorHex = "#808080", // Grey for unknown
            colorName = colorName.ifEmpty { "Unknown Colour" },
            spoolWeight = 0,
            filamentDiameter = 1.75f,
            filamentLength = 0,
            productionDate = "",
            minTemperature = 0,
            maxTemperature = 0,
            bedTemperature = 0,
            dryingTemperature = 0,
            dryingTime = 0
        )
    }
}