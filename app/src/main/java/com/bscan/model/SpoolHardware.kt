package com.bscan.model

/**
 * Represents the physical spool hardware - the reusable plastic reel/bobbin.
 * 
 * This is distinct from FilamentReel (the consumable filament material).
 * SpoolHardware can be empty, or have a FilamentReel mounted on it.
 * 
 * This class is prepared for future inventory management features where
 * we track the mounting/unmounting of filament reels onto spool hardware.
 */
data class SpoolHardware(
    val spoolId: String,              // Unique identifier for this physical spool
    val type: SpoolType,              // Type of spool hardware
    val manufacturer: String = "Bambu Lab",
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Types of spool hardware
 */
enum class SpoolType {
    BAMBU_ORIGINAL,    // Official Bambu Lab spool
    BAMBU_COMPATIBLE,  // Third-party Bambu-compatible spool
    CARDBOARD,         // Disposable cardboard spool
    GENERIC_PLASTIC,   // Generic plastic spool
    UNKNOWN
}

/**
 * Represents the relationship between filament and spool hardware.
 * This will be used for future inventory management features.
 */
data class MountedFilament(
    val filamentReel: FilamentReel,
    val spoolHardware: SpoolHardware?,  // null if filament is unmounted
    val mountedAt: Long? = null,        // When filament was mounted on spool
    val unmountedAt: Long? = null       // When filament was removed from spool
) {
    val isMounted: Boolean = spoolHardware != null && unmountedAt == null
}