package com.bscan.model

/**
 * Represents a reel/coil of filament material identified by its tray UID.
 * 
 * This is the consumable component - the actual filament that gets printed.
 * Each FilamentReel has 2 RFID tags attached that share the same tray UID.
 * The tray UID uniquely identifies this specific reel of filament.
 * 
 * Note: This is distinct from SpoolHardware (the reusable plastic reel).
 */
data class FilamentReel(
    val trayUid: String,                        // Unique identifier for this filament reel
    val filamentInfo: FilamentInfo,             // Decoded filament properties
    val encryptedScans: List<EncryptedScanData>, // Raw RFID tag scans from this reel
    val decryptedScans: List<DecryptedScanData>, // Decrypted RFID tag scans from this reel
    val firstScannedAt: Long,                   // Timestamp of first scan
    val lastScannedAt: Long                     // Timestamp of most recent scan
) {
    /**
     * True if both RFID tags from this reel have been scanned
     */
    val hasCompleteScanSet: Boolean = decryptedScans.size >= 2
    
    /**
     * Get the most recent decrypted scan data
     */
    val latestScan: DecryptedScanData? = decryptedScans.maxByOrNull { it.timestamp }
}