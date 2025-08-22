package com.bscan.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdateDownloadService(private val context: Context) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    
    fun downloadUpdate(downloadUrl: String, fileName: String): Flow<DownloadProgress> = callbackFlow {
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("B-Scan Update")
            setDescription("Downloading update...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        
        val downloadId = downloadManager.enqueue(request)
        var isDownloadComplete = false
        
        // Completion receiver for final status
        val completionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    isDownloadComplete = true
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                trySend(DownloadProgress.Completed(localUri ?: ""))
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val errorMessage = getFailureReason(reason)
                                trySend(DownloadProgress.Failed("Download failed: $errorMessage"))
                            }
                        }
                    }
                    cursor.close()
                    close()
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            completionReceiver, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Send initial progress
        trySend(DownloadProgress.Started)
        
        // Start periodic progress monitoring
        launch {
            try {
                while (!isDownloadComplete) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        
                        when (status) {
                            DownloadManager.STATUS_RUNNING -> {
                                val progress = if (bytesTotal > 0) {
                                    (bytesDownloaded * 100L / bytesTotal).toInt()
                                } else 0
                                trySend(DownloadProgress.InProgress(progress, bytesDownloaded, bytesTotal))
                            }
                            DownloadManager.STATUS_PENDING -> {
                                trySend(DownloadProgress.InProgress(0, 0, bytesTotal))
                            }
                            DownloadManager.STATUS_PAUSED -> {
                                val progress = if (bytesTotal > 0) {
                                    (bytesDownloaded * 100L / bytesTotal).toInt()
                                } else 0
                                trySend(DownloadProgress.InProgress(progress, bytesDownloaded, bytesTotal))
                            }
                            DownloadManager.STATUS_SUCCESSFUL, DownloadManager.STATUS_FAILED -> {
                                // Let the completion receiver handle these
                                break
                            }
                        }
                    }
                    cursor.close()
                    
                    // Check every second for progress updates
                    delay(1000)
                }
            } catch (e: Exception) {
                trySend(DownloadProgress.Failed("Error monitoring download progress: ${e.message}"))
                close()
            }
        }
        
        awaitClose {
            isDownloadComplete = true
            try {
                context.unregisterReceiver(completionReceiver)
            } catch (e: IllegalArgumentException) {
                // Receiver might not be registered
            }
        }
    }
    
    private fun getFailureReason(reason: Int): String = when (reason) {
        DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
        DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
        DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
        DownloadManager.ERROR_FILE_ERROR -> "File error"
        DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
        DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient storage space"
        DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
        DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP response code"
        DownloadManager.ERROR_UNKNOWN -> "Unknown error"
        else -> "Error code: $reason"
    }
    
    fun installUpdate(apkPath: String) {
        try {
            val file = File(Uri.parse(apkPath).path ?: return)
            if (!file.exists()) return
            
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(installIntent)
        } catch (e: Exception) {
            // Handle installation error
        }
    }
}

sealed class DownloadProgress {
    object Started : DownloadProgress()
    data class InProgress(val progress: Int, val bytesDownloaded: Long, val bytesTotal: Long) : DownloadProgress()
    data class Completed(val filePath: String) : DownloadProgress()
    data class Failed(val error: String) : DownloadProgress()
}