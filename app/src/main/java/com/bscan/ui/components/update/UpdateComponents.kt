package com.bscan.ui.components.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bscan.model.UpdateInfo
import com.bscan.model.UpdateStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun UpdateDialogHeader(
    status: UpdateStatus,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.SystemUpdate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        
        if (status != UpdateStatus.DOWNLOADING && status != UpdateStatus.INSTALLING) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

@Composable
fun UpdateVersionInfo(
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Update Available",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Version ${updateInfo.latestVersion} is available",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = "Current version: ${updateInfo.currentVersion}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UpdateReleaseInfo(
    updateInfo: UpdateInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Released ${updateInfo.publishedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
                    style = MaterialTheme.typography.bodySmall
                )
                
                if (updateInfo.isPrerelease) {
                    Badge {
                        Text(
                            text = "PRE-RELEASE",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
            
            if (updateInfo.fileSize > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Size: ${formatFileSize(updateInfo.fileSize)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateReleaseNotes(
    releaseNotes: String,
    modifier: Modifier = Modifier
) {
    if (releaseNotes.isNotBlank()) {
        Column(modifier = modifier) {
            Text(
                text = "What's New:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Text(
                    text = releaseNotes,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun UpdateErrorDisplay(
    error: String?,
    modifier: Modifier = Modifier
) {
    if (error != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun UpdateProgressIndicator(
    status: UpdateStatus,
    downloadProgress: Int,
    modifier: Modifier = Modifier
) {
    when (status) {
        UpdateStatus.DOWNLOADING -> {
            Column(modifier = modifier) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Downloading...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$downloadProgress%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        UpdateStatus.INSTALLING -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = "Installing update...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        else -> {
            // No progress indicator for other states
        }
    }
}

@Composable
fun UpdateActionButtons(
    status: UpdateStatus,
    updateInfo: UpdateInfo,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
    onViewOnGitHub: () -> Unit,
    onDismissVersion: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (status) {
        UpdateStatus.AVAILABLE, UpdateStatus.ERROR -> {
            Column(modifier = modifier) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewOnGitHub,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
                    
                    Button(
                        onClick = onDownload,
                        modifier = Modifier.weight(1f),
                        enabled = updateInfo.downloadUrl.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismissVersion,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip this version")
                    }
                    
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Later")
                    }
                }
            }
        }
        
        UpdateStatus.DOWNLOADED -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Later")
                }
                
                Button(
                    onClick = onInstall,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install")
                }
            }
        }
        
        else -> {
            // No buttons for downloading/installing states
        }
    }
}

fun formatFileSize(sizeInBytes: Long): String {
    val kb = sizeInBytes / 1024.0
    val mb = kb / 1024.0
    
    return when {
        mb >= 1 -> "%.1f MB".format(mb)
        kb >= 1 -> "%.1f KB".format(kb)
        else -> "$sizeInBytes B"
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateDialogHeaderPreview() {
    MaterialTheme {
        UpdateDialogHeader(
            status = UpdateStatus.AVAILABLE,
            onDismiss = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateVersionInfoPreview() {
    MaterialTheme {
        val mockUpdateInfo = UpdateInfo(
            latestVersion = "1.5.2",
            currentVersion = "1.5.0",
            releaseNotes = "Bug fixes and performance improvements",
            downloadUrl = "https://github.com/example/releases/1.5.2.apk",
            publishedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
            isPrerelease = false,
            fileSize = 15728640L
        )
        
        UpdateVersionInfo(updateInfo = mockUpdateInfo)
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateReleaseInfoPreview() {
    MaterialTheme {
        val mockUpdateInfo = UpdateInfo(
            latestVersion = "1.5.2-beta",
            currentVersion = "1.5.0",
            releaseNotes = "Beta release with new features",
            downloadUrl = "https://github.com/example/releases/1.5.2-beta.apk",
            publishedAt = LocalDateTime.of(2024, 3, 15, 14, 45),
            isPrerelease = true,
            fileSize = 16777216L
        )
        
        UpdateReleaseInfo(updateInfo = mockUpdateInfo)
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateProgressIndicatorPreview() {
    MaterialTheme {
        androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
            UpdateProgressIndicator(
                status = UpdateStatus.DOWNLOADING,
                downloadProgress = 65
            )
            
            UpdateProgressIndicator(
                status = UpdateStatus.INSTALLING,
                downloadProgress = 100
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateActionButtonsPreview() {
    MaterialTheme {
        val mockUpdateInfo = UpdateInfo(
            latestVersion = "1.5.2",
            currentVersion = "1.5.0",
            releaseNotes = "Update available",
            downloadUrl = "https://github.com/example/releases/1.5.2.apk",
            publishedAt = LocalDateTime.of(2024, 3, 15, 10, 30),
            isPrerelease = false,
            fileSize = 15728640L
        )
        
        UpdateActionButtons(
            status = UpdateStatus.AVAILABLE,
            updateInfo = mockUpdateInfo,
            onDownload = { },
            onInstall = { },
            onDismiss = { },
            onViewOnGitHub = { },
            onDismissVersion = { }
        )
    }
}