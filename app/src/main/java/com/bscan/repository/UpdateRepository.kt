package com.bscan.repository

import android.content.Context
import android.content.SharedPreferences
import com.bscan.model.UpdateInfo
import com.bscan.update.GitHubUpdateService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UpdateRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
    
    private val updateService = GitHubUpdateService(context)
    
    companion object {
        private const val KEY_LAST_CHECK = "last_update_check"
        private const val KEY_CHECK_INTERVAL_HOURS = "check_interval_hours"
        private const val KEY_AUTO_CHECK_ENABLED = "auto_check_enabled"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val DEFAULT_CHECK_INTERVAL = 24 // 24 hours
    }
    
    suspend fun checkForUpdates(force: Boolean = false): Result<UpdateInfo> {
        val shouldCheck = force || shouldCheckForUpdates()
        
        if (!shouldCheck) {
            return Result.failure(Exception("Update check skipped - too soon"))
        }
        
        val result = updateService.checkForUpdates()
        
        if (result.isSuccess) {
            updateLastCheckTime()
        }
        
        return result
    }
    
    private fun shouldCheckForUpdates(): Boolean {
        if (!isAutoCheckEnabled()) return false
        
        val lastCheckTime = getLastCheckTime()
        val currentTime = LocalDateTime.now()
        val intervalHours = getCheckIntervalHours()
        
        return lastCheckTime.plusHours(intervalHours.toLong()).isBefore(currentTime)
    }
    
    private fun updateLastCheckTime() {
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        sharedPreferences.edit()
            .putString(KEY_LAST_CHECK, currentTime)
            .apply()
    }
    
    private fun getLastCheckTime(): LocalDateTime {
        val lastCheckString = sharedPreferences.getString(KEY_LAST_CHECK, null)
        return if (lastCheckString != null) {
            try {
                LocalDateTime.parse(lastCheckString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            } catch (e: Exception) {
                LocalDateTime.MIN // Force check if parsing fails
            }
        } else {
            LocalDateTime.MIN // Force check if never checked
        }
    }
    
    fun isAutoCheckEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_AUTO_CHECK_ENABLED, true) // Default enabled
    }
    
    fun setAutoCheckEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_CHECK_ENABLED, enabled)
            .apply()
    }
    
    fun getCheckIntervalHours(): Int {
        return sharedPreferences.getInt(KEY_CHECK_INTERVAL_HOURS, DEFAULT_CHECK_INTERVAL)
    }
    
    fun setCheckIntervalHours(hours: Int) {
        val validHours = hours.coerceIn(1, 168) // Between 1 hour and 1 week
        sharedPreferences.edit()
            .putInt(KEY_CHECK_INTERVAL_HOURS, validHours)
            .apply()
    }
    
    fun dismissVersion(version: String) {
        sharedPreferences.edit()
            .putString(KEY_DISMISSED_VERSION, version)
            .apply()
    }
    
    fun isVersionDismissed(version: String): Boolean {
        val dismissedVersion = sharedPreferences.getString(KEY_DISMISSED_VERSION, null)
        return dismissedVersion == version
    }
    
    fun clearDismissedVersion() {
        sharedPreferences.edit()
            .remove(KEY_DISMISSED_VERSION)
            .apply()
    }
}