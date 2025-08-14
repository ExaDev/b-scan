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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        
                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                trySend(DownloadProgress.Completed(localUri ?: ""))
                                close()
                            }
                            DownloadManager.STATUS_FAILED -> {
                                trySend(DownloadProgress.Failed("Download failed"))
                                close()
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val progress = if (bytesTotal > 0) {
                                    (bytesDownloaded * 100L / bytesTotal).toInt()
                                } else 0
                                trySend(DownloadProgress.InProgress(progress, bytesDownloaded, bytesTotal))
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            receiver, 
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        // Send initial progress
        trySend(DownloadProgress.Started)
        
        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Receiver might not be registered
            }
        }
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