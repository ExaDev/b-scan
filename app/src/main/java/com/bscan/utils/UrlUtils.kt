package com.bscan.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object UrlUtils {
    private const val TAG = "UrlUtils"
    
    /**
     * Open a URL in the default browser
     * @param context Android context
     * @param url The URL to open
     * @return true if the URL was opened successfully, false otherwise
     */
    fun openUrl(context: Context, url: String): Boolean {
        if (url.isBlank()) {
            Log.w(TAG, "Attempted to open blank URL")
            return false
        }
        
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                // Add FLAG_ACTIVITY_NEW_TASK to ensure the browser opens in a new task
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                Log.d(TAG, "Successfully opened URL: $url")
                true
            } else {
                Log.e(TAG, "No app found to handle URL: $url")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open URL: $url", e)
            false
        }
    }
    
    /**
     * Get the store URL for a filament product
     * Prioritises spool URL over refill URL
     */
    fun getPreferredStoreUrl(spoolUrl: String?, refillUrl: String?): String? {
        return when {
            !spoolUrl.isNullOrBlank() -> spoolUrl
            !refillUrl.isNullOrBlank() -> refillUrl
            else -> null
        }
    }
    
    /**
     * Check if a URL appears to be valid
     */
    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        
        return try {
            val uri = Uri.parse(url)
            uri.scheme != null && uri.host != null
        } catch (e: Exception) {
            false
        }
    }
}