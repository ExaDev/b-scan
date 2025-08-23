package com.bscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.repository.ScanHistoryRepository
import com.bscan.repository.TrayTrackingRepository
import com.bscan.repository.MappingsRepository
import com.bscan.interpreter.InterpreterFactory
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
    private val mappingsRepository = MappingsRepository(application)
    
    // InterpreterFactory for runtime interpretation
    private var interpreterFactory = InterpreterFactory(mappingsRepository)
    
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
    
    /**
     * New method to process scan data using the FilamentInterpreter
     */
    fun processScanData(encryptedData: EncryptedScanData, decryptedData: DecryptedScanData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                scanState = ScanState.PROCESSING,
                error = null
            )
            _scanProgress.value = ScanProgress(
                stage = ScanStage.PARSING,
                percentage = 0.9f,
                statusMessage = "Interpreting filament data"
            )
            
            // Store the raw scan data first
            scanHistoryRepository.saveScan(encryptedData, decryptedData)
            trayTrackingRepository.recordScan(decryptedData)
            
            val result = if (decryptedData.scanResult == ScanResult.SUCCESS) {
                try {
                    // Use FilamentInterpreter to convert decrypted data to FilamentInfo
                    val filamentInfo = interpreterFactory.interpret(decryptedData)
                    if (filamentInfo != null) {
                        TagReadResult.Success(filamentInfo)
                    } else {
                        TagReadResult.InvalidTag
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    TagReadResult.ReadError
                }
            } else {
                // Map scan result to TagReadResult
                when (decryptedData.scanResult) {
                    ScanResult.AUTHENTICATION_FAILED -> TagReadResult.ReadError
                    ScanResult.INSUFFICIENT_DATA -> TagReadResult.InsufficientData
                    ScanResult.PARSING_FAILED -> TagReadResult.InvalidTag
                    else -> TagReadResult.ReadError
                }
            }
            
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
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
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
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                }
                is TagReadResult.ReadError -> {
                    _scanProgress.value = ScanProgress(
                        stage = ScanStage.ERROR,
                        percentage = 0.0f,
                        statusMessage = "Error reading or authenticating tag"
                    )
                    BScanUiState(
                        error = "Error reading or authenticating tag", 
                        scanState = ScanState.ERROR,
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
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
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
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
                        debugInfo = createDebugInfoFromDecryptedData(decryptedData)
                    )
                }
            }
        }
    }
    
    /**
     * Helper method to create ScanDebugInfo from DecryptedScanData
     */
    private fun createDebugInfoFromDecryptedData(decryptedData: DecryptedScanData): ScanDebugInfo {
        return ScanDebugInfo(
            uid = decryptedData.tagUid,
            tagSizeBytes = decryptedData.tagSizeBytes,
            sectorCount = decryptedData.sectorCount,
            authenticatedSectors = decryptedData.authenticatedSectors,
            failedSectors = decryptedData.failedSectors,
            usedKeyTypes = decryptedData.usedKeys,
            blockData = decryptedData.decryptedBlocks,
            derivedKeys = decryptedData.derivedKeys,
            rawColorBytes = "", // TODO: Extract from block data if needed
            errorMessages = decryptedData.errors,
            parsingDetails = mapOf(), // Empty for now
            fullRawHex = "", // Not available in DecryptedScanData
            decryptedHex = "" // Could reconstruct from blocks if needed
        )
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
        // Create scan data for failed authentication
        val encryptedData = debugCollector.createEncryptedScanData(
            uid = tagData.uid,
            technology = tagData.technology,
            scanDurationMs = 0
        )
        
        val decryptedData = debugCollector.createDecryptedScanData(
            uid = tagData.uid,
            technology = tagData.technology,
            result = ScanResult.AUTHENTICATION_FAILED,
            keyDerivationTimeMs = 0,
            authenticationTimeMs = 0
        )
        
        // Save to history even for failed scans
        viewModelScope.launch {
            scanHistoryRepository.saveScan(encryptedData, decryptedData)
            trayTrackingRepository.recordScan(decryptedData)
        }
        
        _uiState.value = _uiState.value.copy(
            error = "Authentication failed - see debug info below",
            scanState = ScanState.ERROR,
            debugInfo = createDebugInfoFromDecryptedData(decryptedData)
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
    
    // Expose repositories for UI access
    fun getTrayTrackingRepository(): TrayTrackingRepository = trayTrackingRepository
    fun getMappingsRepository(): MappingsRepository = mappingsRepository
    
    /**
     * Refresh the FilamentInterpreter with updated mappings
     */
    fun refreshMappings() {
        interpreterFactory.refreshMappings()
    }
    
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