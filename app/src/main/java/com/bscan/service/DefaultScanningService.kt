package com.bscan.service

import android.content.Context
import com.bscan.ScanState
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanProgress
import com.bscan.repository.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Default implementation of ScanningService that handles scan data persistence
 * without coupling to specific UI implementations. Components are generated on-demand
 * by ComponentGenerationService instead of being persisted during scanning.
 */
class DefaultScanningService(
    private val context: Context,
    private val scanHistoryRepository: ScanHistoryRepository
) : ScanningService {
    
    private val _scanState = MutableStateFlow<ScanState>(ScanState.IDLE)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    override val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()
    
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    override val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()
    
    override suspend fun processScanData(
        encryptedData: EncryptedScanData,
        decryptedData: DecryptedScanData
    ): ScanResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        try {
            // Only persist scan data - components will be generated on-demand
            scanHistoryRepository.saveScan(encryptedData, decryptedData)
            
            val scanDuration = System.currentTimeMillis() - startTime
            
            // Return result without pre-generated components
            // Components will be generated on-demand by ComponentGenerationService
            val result = ScanResult(
                components = emptyList(), // No pre-generated components
                encryptedData = encryptedData,
                decryptedData = decryptedData,
                scanDurationMs = scanDuration
            )
            
            // Update state flows
            _scanResult.value = result
            _scanState.value = ScanState.SUCCESS
            
            result
            
        } catch (e: Exception) {
            _scanState.value = ScanState.ERROR
            setError("Error processing scan data: ${e.message}")
            throw e
        }
    }
    
    override fun setScanning() {
        _scanState.value = ScanState.PROCESSING
        _scanProgress.value = null
    }
    
    override fun updateScanProgress(progress: ScanProgress) {
        _scanProgress.value = progress
    }
    
    override fun onTagDetected() {
        _scanState.value = ScanState.TAG_DETECTED
    }
    
    override fun setError(message: String) {
        _scanState.value = ScanState.ERROR
        // Error details could be stored separately if needed
    }
    
    override fun clearScanState() {
        _scanState.value = ScanState.IDLE
        _scanProgress.value = null
        _scanResult.value = null
    }
}