package com.bscan.update

import android.content.Context
import com.bscan.model.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class GitHubUpdateService(private val context: Context) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    // GitHub repository information
    private val repoOwner = "Mearman"  
    private val repoName = "b-scan"
    private val apiUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
    
    suspend fun checkForUpdates(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "B-Scan-Android/1.0")
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    Exception("GitHub API request failed: ${response.code}")
                )
            }
            
            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val release = gson.fromJson(responseBody, GitHubRelease::class.java)
                ?: return@withContext Result.failure(Exception("Failed to parse GitHub release"))
            
            val updateInfo = parseReleaseToUpdateInfo(release)
            Result.success(updateInfo)
            
        } catch (e: JsonSyntaxException) {
            Result.failure(Exception("Failed to parse GitHub API response", e))
        } catch (e: Exception) {
            Result.failure(Exception("Error checking for updates", e))
        }
    }
    
    private fun parseReleaseToUpdateInfo(release: GitHubRelease): UpdateInfo {
        val currentVersion = "1.0" // TODO: Get from BuildConfig when available
        val latestVersion = release.tag_name.removePrefix("v")
        
        // Find APK asset
        val apkAsset = release.assets.find { 
            it.name.endsWith(".apk", ignoreCase = true) && 
            it.content_type == "application/vnd.android.package-archive"
        }
        
        val isUpdateAvailable = isVersionNewer(currentVersion, latestVersion)
        
        val publishedAt = try {
            LocalDateTime.parse(release.published_at, DateTimeFormatter.ISO_DATE_TIME)
        } catch (e: Exception) {
            LocalDateTime.now()
        }
        
        return UpdateInfo(
            isUpdateAvailable = isUpdateAvailable,
            latestVersion = latestVersion,
            currentVersion = currentVersion,
            releaseUrl = "https://github.com/$repoOwner/$repoName/releases/tag/${release.tag_name}",
            downloadUrl = apkAsset?.browser_download_url ?: "",
            releaseNotes = release.body,
            publishedAt = publishedAt,
            isPrerelease = release.prerelease,
            fileSize = apkAsset?.size ?: 0L
        )
    }
    
    private fun isVersionNewer(current: String, latest: String): Boolean {
        return try {
            // Parse version strings like "1.14", "1.14.1", etc.
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
            
            // Pad with zeros to make lengths equal
            val maxLength = maxOf(currentParts.size, latestParts.size)
            val currentNormalized = currentParts + List(maxLength - currentParts.size) { 0 }
            val latestNormalized = latestParts + List(maxLength - latestParts.size) { 0 }
            
            // Compare version parts
            for (i in currentNormalized.indices) {
                val currentPart = currentNormalized[i]
                val latestPart = latestNormalized[i]
                
                when {
                    latestPart > currentPart -> return true
                    latestPart < currentPart -> return false
                    // If equal, continue to next part
                }
            }
            
            false // Versions are equal
        } catch (e: Exception) {
            // If version parsing fails, assume no update
            false
        }
    }
}