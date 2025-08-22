package com.bscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.decoder.BambuTagDecoder
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.TrayTrackingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(BScanUiState())
    val uiState: StateFlow<BScanUiState> = _uiState.asStateFlow()
    
    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()
    
    private val scanHistoryRepository = ScanHistoryRepository(application)
    private val trayTrackingRepository = TrayTrackingRepository(application)
    
    fun onTagDetected() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.TAG_DETECTED,
            error = null
        )
        _scanProgress.value = ScanProgress(
            stage = ScanStage.TAG_DETECTED,
            percentage = 0.0f,
            statusMessage = "Tag detected"
        )
        
        // Show tag detected state for a brief moment before processing
        viewModelScope.launch {
            delay(300) // Show detection for 300ms
            if (_uiState.value.scanState == ScanState.TAG_DETECTED) {
                // Only proceed if still in TAG_DETECTED state (not cancelled)
                // The actual processTag call will update the state to PROCESSING
            }
        }
    }
    
    fun processTag(tagData: NfcTagData, debugCollector: DebugDataCollector) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.PROCESSING,
                error = null
            )
            _scanProgress.value = ScanProgress(
                stage = ScanStage.PARSING,
                percentage = 0.9f,
                statusMessage = "Parsing tag data"
            )
            
            val result = try {
                val filamentInfo = BambuTagDecoder.parseTagDetails(tagData, debugCollector)
                if (filamentInfo != null) {
                    TagReadResult.Success(filamentInfo)
                } else {
                    TagReadResult.InvalidTag
                }
            } catch (e: Exception) {
                debugCollector.recordError("Exception in parseTagDetails: ${e.message}")
                e.printStackTrace()
                TagReadResult.ReadError
            }
            
            // Save scan to history
            val scanResult = when (result) {
                is TagReadResult.Success -> ScanResult.SUCCESS
                is TagReadResult.InvalidTag -> ScanResult.PARSING_FAILED
                is TagReadResult.ReadError -> ScanResult.UNKNOWN_ERROR
                is TagReadResult.InsufficientData -> ScanResult.INSUFFICIENT_DATA
                else -> ScanResult.UNKNOWN_ERROR
            }
            
            val scanHistory = debugCollector.createScanHistory(
                uid = tagData.uid,
                technology = tagData.technology,
                result = scanResult,
                filamentInfo = if (result is TagReadResult.Success) result.filamentInfo else null
            )
            
            scanHistoryRepository.saveScan(scanHistory)
            trayTrackingRepository.recordScan(scanHistory)
            
            _uiState.value = when (result) {
                is TagReadResult.Success -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.COMPLETED,
                        percentage = 1.0f,
                        statusMessage = "Scan completed successfully"
                    )
                    BScanUiState(
                        filamentInfo = result.filamentInfo,
                        scanState = ScanState.SUCCESS,
                        debugInfo = scanHistory.debugInfo
                    )
                }
                is TagReadResult.InvalidTag -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Invalid or unsupported tag"
                    )
                    BScanUiState(
                        error = "Invalid or unsupported tag",
                        scanState = ScanState.ERROR,
                        debugInfo = scanHistory.debugInfo
                    )
                }
                is TagReadResult.ReadError -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Error reading tag"
                    )
                    BScanUiState(
                        error = "Error reading tag", 
                        scanState = ScanState.ERROR,
                        debugInfo = scanHistory.debugInfo
                    )
                }
                is TagReadResult.InsufficientData -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Insufficient data on tag"
                    )
                    BScanUiState(
                        error = "Insufficient data on tag",
                        scanState = ScanState.ERROR,
                        debugInfo = scanHistory.debugInfo
                    )
                }
                else -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Unknown error occurred"
                    )
                    BScanUiState(
                        error = "Unknown error occurred",
                        scanState = ScanState.ERROR,
                        debugInfo = scanHistory.debugInfo
                    )
                }
            }
        }
    }
    
    fun setScanning() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.PROCESSING,
            error = null
        )
    }
    
    fun updateScanProgress(progress: ScanProgress) {
        _scanProgress.value = progress
    }
    
    fun setNfcError(error: String) {
        _uiState.value = _uiState.value.copy(
            error = error,
            scanState = ScanState.ERROR
        )
    }
    
    fun setAuthenticationFailed(tagData: NfcTagData, debugCollector: DebugDataCollector) {
        // Create scan history for failed authentication
        val scanHistory = debugCollector.createScanHistory(
            uid = tagData.uid,
            technology = tagData.technology,
            result = ScanResult.AUTHENTICATION_FAILED,
            filamentInfo = null
        )
        
        // Save to history even for failed scans
        viewModelScope.launch {
            scanHistoryRepository.saveScan(scanHistory)
            trayTrackingRepository.recordScan(scanHistory)
        }
        
        _uiState.value = _uiState.value.copy(
            error = "Authentication failed - see debug info below",
            scanState = ScanState.ERROR,
            debugInfo = scanHistory.debugInfo
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetScan() {
        _uiState.value = BScanUiState()
        _scanProgress.value = null
    }
    
    fun simulateScan() {
        viewModelScope.launch {
            // Start simulation
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.TAG_DETECTED,
                error = null
            )
            
            val stages = listOf(
                ScanProgress(ScanStage.TAG_DETECTED, 0.0f, statusMessage = "Tag detected"),
                ScanProgress(ScanStage.CONNECTING, 0.05f, statusMessage = "Connecting to tag"),
                ScanProgress(ScanStage.KEY_DERIVATION, 0.1f, statusMessage = "Deriving keys"),
            )
            
            // Simulate authenticating sectors
            for (sector in 0..15) {
                _scanProgress.value = ScanProgress(
                    stage = ScanStage.AUTHENTICATING,
                    percentage = 0.15f + (sector * 0.04f), // 15% to 75%
                    currentSector = sector,
                    statusMessage = "Authenticating sector ${sector + 1}/16"
                )
                delay(80) // Short delay per sector
            }
            
            // Reading blocks
            _scanProgress.value = ScanProgress(
                stage = ScanStage.READING_BLOCKS,
                percentage = 0.8f,
                statusMessage = "Reading data blocks"
            )
            delay(200)
            
            // Assembling data
            _scanProgress.value = ScanProgress(
                stage = ScanStage.ASSEMBLING_DATA,
                percentage = 0.85f,
                statusMessage = "Assembling data"
            )
            delay(150)
            
            // Parsing
            _scanProgress.value = ScanProgress(
                stage = ScanStage.PARSING,
                percentage = 0.9f,
                statusMessage = "Parsing filament data"
            )
            delay(200)
            
            // Complete with mock data
            val mockFilamentInfo = FilamentInfo(
                tagUid = "MOCK${System.currentTimeMillis()}",
                trayUid = "MOCK_TRAY_001",
                filamentType = "PLA Basic",
                detailedFilamentType = "PLA Basic",
                colorHex = "FF0000",
                colorName = "Red",
                spoolWeight = 1000,
                filamentDiameter = 1.75f,
                filamentLength = 330000,
                productionDate = "2024-01",
                minTemperature = 190,
                maxTemperature = 220,
                bedTemperature = 60,
                dryingTemperature = 45,
                dryingTime = 8
            )
            
            _scanProgress.value = ScanProgress(
                stage = ScanStage.COMPLETED,
                percentage = 1.0f,
                statusMessage = "Scan completed successfully"
            )
            
            _uiState.value = BScanUiState(
                filamentInfo = mockFilamentInfo,
                scanState = ScanState.SUCCESS,
                debugInfo = null
            )
        }
    }
    
    // Expose tray tracking repository for UI access
    fun getTrayTrackingRepository(): TrayTrackingRepository = trayTrackingRepository
    
}

data class BScanUiState(
    val filamentInfo: FilamentInfo? = null,
    val scanState: ScanState = ScanState.IDLE,
    val error: String? = null,
    val debugInfo: ScanDebugInfo? = null
)

enum class ScanState {
    IDLE,           // Waiting for tag
    TAG_DETECTED,   // Tag detected but not yet processed
    PROCESSING,     // Processing tag data
    SUCCESS,        // Successfully processed
    ERROR          // Error occurred
}