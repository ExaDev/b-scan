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
    
    fun processTag(tagData: NfcTagData, debugCollector: DebugDataCollector) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            
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
                    isProcessing = false
                )
                is TagReadResult.InvalidTag -> BScanUiState(
                    error = "Invalid or unsupported tag",
                    isProcessing = false
                )
                is TagReadResult.ReadError -> BScanUiState(
                    error = "Error reading tag",
                    isProcessing = false
                )
                is TagReadResult.InsufficientData -> BScanUiState(
                    error = "Insufficient data on tag",
                    isProcessing = false
                )
                else -> BScanUiState(
                    error = "Unknown error occurred",
                    isProcessing = false
                )
            }
        }
    }
    
    fun setNfcError(error: String) {
        _uiState.value = _uiState.value.copy(error = error, isProcessing = false)
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
    val isProcessing: Boolean = false,
    val error: String? = null
)