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
            
            // First try quick cache check for immediate response
            val quickTagData = nfcManager.handleIntent(intent)
            if (quickTagData != null) {
                // Cache hit - immediate response
                lifecycleScope.launch {
                    delay(300) // Brief delay to show detection
                    viewModel.processTag(quickTagData, nfcManager.debugCollector)
                }
            } else {
                // Cache miss - need background read
                processSlowTagRead(intent)
            }
        }
    }
    
    private fun processSlowTagRead(intent: Intent) {
        // Show scanning state immediately
        viewModel.setScanning()
        
        lifecycleScope.launch {
            try {
                // Perform heavy read operation on background thread with progress callback
                val tagData = nfcManager.handleIntentAsync(intent) { progress ->
                    viewModel.updateScanProgress(progress)
                }
                
                if (tagData != null) {
                    // Successful read
                    viewModel.processTag(tagData, nfcManager.debugCollector)
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