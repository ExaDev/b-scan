package com.bscan.nfc.handlers

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import androidx.lifecycle.LifecycleCoroutineScope
import com.bscan.MainViewModel
import com.bscan.model.NfcTagData
import com.bscan.nfc.NfcManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NfcIntentProcessor(
    private val nfcManager: NfcManager,
    private val viewModel: MainViewModel,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val hapticFeedbackProvider: HapticFeedbackProvider
) {
    
    fun handleIntent(intent: Intent) {
        if (intent.action == NfcAdapter.ACTION_TAG_DISCOVERED || 
            intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {
            
            // Provide immediate feedback that tag was detected
            viewModel.onTagDetected()
            
            // Provide haptic feedback
            hapticFeedbackProvider.provideFeedback()
            
            // Skip cache for now and go directly to full read
            // TODO: Implement caching for new data model
            processSlowTagRead(intent)
        }
    }
    
    private fun processSlowTagRead(intent: Intent) {
        // Show scanning state immediately
        viewModel.setScanning()
        
        lifecycleScope.launch {
            try {
                // Try the new full data method first
                val scanDataPair = nfcManager.handleIntentWithFullData(intent) { progress ->
                    viewModel.updateScanProgress(progress)
                }
                
                if (scanDataPair != null) {
                    val (encryptedData, decryptedData) = scanDataPair
                    // Use the new processing method
                    viewModel.processScanData(encryptedData, decryptedData)
                } else {
                    handleTagReadFailure(intent)
                }
            } catch (e: Exception) {
                // Handle any unexpected errors
                viewModel.setNfcError("Error reading tag: ${e.message}")
            }
        }
    }
    
    private fun handleTagReadFailure(intent: Intent) {
        // Check if authentication failure
        if (!nfcManager.debugCollector.hasAuthenticatedSectors()) {
            // Create a mock tag data for authentication failure case
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<android.nfc.Tag>(NfcAdapter.EXTRA_TAG)
            }
            
            tag?.let { 
                val failedTagData = NfcTagData(
                    uid = it.id.joinToString("") { byte -> "%02X".format(byte) },
                    bytes = ByteArray(0),
                    technology = it.techList.firstOrNull() ?: "Unknown"
                )
                viewModel.setAuthenticationFailed(failedTagData, nfcManager.debugCollector)
            } ?: viewModel.setNfcError("Error reading tag")
        } else {
            viewModel.setNfcError("Error reading tag")
        }
    }
}