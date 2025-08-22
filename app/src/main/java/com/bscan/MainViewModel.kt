package com.bscan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import com.bscan.decoder.BambuTagDecoder
import com.bscan.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(BScanUiState())
    val uiState: StateFlow<BScanUiState> = _uiState.asStateFlow()
    
    private val scanHistoryRepository = ScanHistoryRepository(application)
    
    fun onTagDetected() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.TAG_DETECTED,
            error = null
        )
        
        // Show tag detected state for a brief moment before processing
        viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Show detection for 300ms
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
            
            _uiState.value = when (result) {
                is TagReadResult.Success -> BScanUiState(
                    filamentInfo = result.filamentInfo,
                    scanState = ScanState.SUCCESS,
                    debugInfo = scanHistory.debugInfo
                )
                is TagReadResult.InvalidTag -> BScanUiState(
                    error = "Invalid or unsupported tag",
                    scanState = ScanState.ERROR,
                    debugInfo = scanHistory.debugInfo
                )
                is TagReadResult.ReadError -> BScanUiState(
                    error = "Error reading tag", 
                    scanState = ScanState.ERROR,
                    debugInfo = scanHistory.debugInfo
                )
                is TagReadResult.InsufficientData -> BScanUiState(
                    error = "Insufficient data on tag",
                    scanState = ScanState.ERROR,
                    debugInfo = scanHistory.debugInfo
                )
                else -> BScanUiState(
                    error = "Unknown error occurred",
                    scanState = ScanState.ERROR,
                    debugInfo = scanHistory.debugInfo
                )
            }
        }
    }
    
    fun setScanning() {
        _uiState.value = _uiState.value.copy(
            scanState = ScanState.PROCESSING,
            error = null
        )
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