package com.bscan.model

import java.time.LocalDateTime

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: LocalDateTime,
    val isPrerelease: Boolean,
    val fileSize: Long
)

data class GitHubRelease(
    val tag_name: String,
    val name: String,
    val body: String,
    val published_at: String,
    val prerelease: Boolean,
    val assets: List<GitHubAsset>
)

data class GitHubAsset(
    val name: String,
    val browser_download_url: String,
    val size: Long,
    val content_type: String
)

enum class UpdateStatus {
    CHECKING,
    AVAILABLE,
    NOT_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    INSTALLING,
    ERROR
}