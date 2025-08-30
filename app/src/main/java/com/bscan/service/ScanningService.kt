package com.bscan.service

import com.bscan.ScanState
import com.bscan.model.Component
import com.bscan.model.DecryptedScanData
import com.bscan.model.EncryptedScanData
import com.bscan.model.ScanProgress
import kotlinx.coroutines.flow.StateFlow

/**
 * Decoupled scanning service that handles all NFC/RFID operations independently.
 * This service emits scan results as events without coupling to specific UI state.
 */
interface ScanningService {
    
    /**
     * Current scanning state (IDLE, PROCESSING, SUCCESS, ERROR, etc.)
     */
    val scanState: StateFlow<ScanState>
    
    /**
     * Current scan progress for operations that report progress
     */
    val scanProgress: StateFlow<ScanProgress?>
    
    /**
     * Latest scan result containing created components
     */
    val scanResult: StateFlow<ScanResult?>
    
    /**
     * Start scanning process with given scan data
     */
    suspend fun processScanData(
        encryptedData: EncryptedScanData,
        decryptedData: DecryptedScanData
    ): ScanResult
    
    /**
     * Set scanning state (called by NFC handlers)
     */
    fun setScanning()
    
    /**
     * Update scan progress (called during processing)
     */
    fun updateScanProgress(progress: ScanProgress)
    
    /**
     * Handle tag detection (immediate feedback)
     */
    fun onTagDetected()
    
    /**
     * Set error state
     */
    fun setError(message: String)
    
    /**
     * Clear current scan state
     */
    fun clearScanState()
}

/**
 * Result of a scanning operation
 */
data class ScanResult(
    val components: List<Component>,
    val encryptedData: EncryptedScanData,
    val decryptedData: DecryptedScanData,
    val scanDurationMs: Long? = null
)