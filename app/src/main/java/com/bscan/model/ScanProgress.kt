package com.bscan.model

data class ScanProgress(
    val stage: ScanStage,
    val percentage: Float, // 0.0 to 1.0
    val currentSector: Int = 0, // 0-15 for Mifare Classic
    val totalSectors: Int = 16,
    val statusMessage: String
)

enum class ScanStage {
    IDLE,
    TAG_DETECTED,
    CONNECTING,
    KEY_DERIVATION,
    AUTHENTICATING,
    READING_BLOCKS,
    ASSEMBLING_DATA,
    PARSING,
    COMPLETED,
    ERROR
}