package com.bscan.service

import android.content.Context
import com.bscan.ScanState
import com.bscan.model.Component
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanProgress
import com.bscan.repository.ComponentRepository
import com.bscan.repository.ScanHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Default implementation of ScanningService that handles component creation
 * and persistence without coupling to specific UI implementations.
 */
class DefaultScanningService(
    private val context: Context,
    private val componentRepository: ComponentRepository,
    private val scanHistoryRepository: ScanHistoryRepository,
    private val componentFactory: ComponentFactory
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
            // Create components using appropriate factory
            val rootComponent = componentFactory.processScan(encryptedData, decryptedData)
            val components = if (rootComponent != null) listOf(rootComponent) else emptyList()
            
            // Persist components to repository
            components.forEach { component ->
                componentRepository.saveComponent(component)
            }
            
            // Persist scan history with synchronized timestamps
            scanHistoryRepository.saveScan(encryptedData, decryptedData)
            
            val scanDuration = System.currentTimeMillis() - startTime
            
            val result = ScanResult(
                components = components,
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