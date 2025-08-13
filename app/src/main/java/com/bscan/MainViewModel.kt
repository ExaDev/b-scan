package com.bscan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bscan.model.*
import com.bscan.utils.BambuTagDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(BScanUiState())
    val uiState: StateFlow<BScanUiState> = _uiState.asStateFlow()
    
    fun processTag(tagData: NfcTagData) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true, error = null)
            
            val result = try {
                val filamentInfo = BambuTagDecoder.parseTag(tagData)
                if (filamentInfo != null) {
                    TagReadResult.Success(filamentInfo)
                } else {
                    TagReadResult.InvalidTag
                }
            } catch (e: Exception) {
                e.printStackTrace()
                TagReadResult.ReadError
            }
            
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