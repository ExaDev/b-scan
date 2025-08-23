package com.bscan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bscan.model.UpdateInfo
import com.bscan.model.UpdateStatus
import com.bscan.ui.components.update.*

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    status: UpdateStatus,
    downloadProgress: Int,
    error: String?,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onViewOnGitHub: () -> Unit,
    onDismissVersion: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = status != UpdateStatus.DOWNLOADING,
            dismissOnClickOutside = status != UpdateStatus.DOWNLOADING
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                UpdateDialogHeader(
                    status = status,
                    onDismiss = onDismiss
                )
                
                // Version info
                UpdateVersionInfo(updateInfo = updateInfo)
                
                // Release info
                UpdateReleaseInfo(updateInfo = updateInfo)
                
                // Release notes
                UpdateReleaseNotes(releaseNotes = updateInfo.releaseNotes)
                
                // Error display
                UpdateErrorDisplay(error = error)
                
                // Progress indicator
                UpdateProgressIndicator(
                    status = status,
                    downloadProgress = downloadProgress
                )
                
                // Action buttons
                UpdateActionButtons(
                    status = status,
                    updateInfo = updateInfo,
                    onDownload = onDownload,
                    onInstall = onInstall,
                    onDismiss = onDismiss,
                    onViewOnGitHub = onViewOnGitHub,
                    onDismissVersion = onDismissVersion
                )
            }
        }
    }
}

