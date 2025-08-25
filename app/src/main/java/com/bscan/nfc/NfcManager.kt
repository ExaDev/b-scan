package com.bscan.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import android.util.Log
import com.bscan.debug.DebugDataCollector
import com.bscan.model.NfcTagData
import com.bscan.model.EncryptedScanData
import com.bscan.model.DecryptedScanData
import com.bscan.model.ScanResult
import com.bscan.model.ScanProgress
import com.bscan.model.ScanStage
import com.bscan.nfc.BambuKeyDerivation
import com.bscan.cache.CachedBambuKeyDerivation
import com.bscan.cache.TagDataCache
import java.io.IOException
import kotlinx.coroutines.*

class NfcManager(private val activity: Activity) {
    private companion object {
        const val TAG = "NfcManager"
    }
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    val debugCollector = DebugDataCollector()
    private val tagDataCache = TagDataCache.getInstance(activity)
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val pendingIntent: PendingIntent = PendingIntent.getActivity(
        activity, 0,
        Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE
    )
    
    fun isNfcAvailable(): Boolean = nfcAdapter != null
    
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true
    
    fun enableForegroundDispatch() {
        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            null,
            arrayOf(
                arrayOf(MifareClassic::class.java.name),
                arrayOf(NfcA::class.java.name)
            )
        )
    }
    
    fun disableForegroundDispatch() {
        nfcAdapter?.disableForegroundDispatch(activity)
    }
    
    fun handleIntent(intent: Intent): NfcTagData? {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            
            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            }
            return tag?.let { readTagDataSync(it) }
        }
        return null
    }
    
    /**
     * Async version of tag reading that performs heavy operations on background thread
     */
    suspend fun handleIntentAsync(intent: Intent, progressCallback: ((ScanProgress) -> Unit)? = null): NfcTagData? = withContext(Dispatchers.IO) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            
            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            }
            return@withContext tag?.let { readTagData(it, progressCallback) }
        }
        return@withContext null
    }
    
    /**
     * New method that returns both encrypted and decrypted scan data
     */
    suspend fun handleIntentWithFullData(intent: Intent, progressCallback: ((ScanProgress) -> Unit)? = null): Pair<EncryptedScanData, DecryptedScanData>? = withContext(Dispatchers.IO) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            
            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            }
            return@withContext tag?.let { readTagDataFull(it, progressCallback) }
        }
        return@withContext null
    }
    
    /**
     * Synchronous version that only checks cache - for immediate response
     */
    private fun readTagDataSync(tag: Tag): NfcTagData? {
        debugCollector.reset() // Start fresh for each scan
        
        val uid = bytesToHex(tag.id)
        Log.d(TAG, "Quick check for cached data for UID: $uid")
        
        // Check cache first for instant response
        tagDataCache.getCachedTagData(uid)?.let { cachedData ->
            Log.d(TAG, "Found cached tag data for UID: $uid - returning immediately")
            debugCollector.recordCacheHit()
            return cachedData
        }
        
        Log.d(TAG, "No cached data for UID: $uid - will need background read")
        return null // Indicates background read needed
    }
    
    /**
     * Full tag reading with heavy operations - should be called on background thread
     */
    private fun readTagData(tag: Tag, progressCallback: ((ScanProgress) -> Unit)? = null): NfcTagData? {
        debugCollector.reset() // Start fresh for each scan
        
        val uid = bytesToHex(tag.id)
        Log.d(TAG, "Reading tag with UID: $uid")
        
        progressCallback?.invoke(ScanProgress(
            stage = ScanStage.CONNECTING,
            percentage = 0.05f,
            statusMessage = "Connecting to tag"
        ))
        
        try {
            // Check cache first for this UID
            tagDataCache.getCachedTagData(uid)?.let { cachedData ->
                Log.d(TAG, "Found cached tag data for UID: $uid - skipping physical read")
                debugCollector.recordCacheHit()
                progressCallback?.invoke(ScanProgress(
                    stage = ScanStage.COMPLETED,
                    percentage = 1.0f,
                    statusMessage = "Using cached data"
                ))
                return cachedData
            }
            
            Log.d(TAG, "No cached data for UID: $uid - performing physical read")
            val mifareClassic = MifareClassic.get(tag)
            val tagData = if (mifareClassic != null) {
                readMifareClassicTag(mifareClassic, tag, progressCallback)
            } else {
                debugCollector.recordError("Tag is not MIFARE Classic compatible")
                // Fallback to reading basic tag info
                NfcTagData(
                    uid = uid,
                    bytes = ByteArray(0), // Empty for now
                    technology = tag.techList.firstOrNull() ?: "Unknown"
                )
            }
            
            // Cache the tag data only if read was completely successful
            tagData?.let { data ->
                // Only cache if we have meaningful data and no critical errors
                if (data.bytes.isNotEmpty() && debugCollector.hasAuthenticatedSectors()) {
                    tagDataCache.cacheTagData(data)
                    Log.d(TAG, "Cached complete tag data for UID: $uid (${data.bytes.size} bytes)")
                } else {
                    Log.d(TAG, "Not caching incomplete/failed read for UID: $uid - missing data or authentication failures")
                }
            }
            
            return tagData
            
        } catch (e: IOException) {
            debugCollector.recordError("IOException reading tag: ${e.message}")
            e.printStackTrace()
            
            // If we had an IOException, invalidate any cached data for this UID
            // as it might be stale or corrupted
            tagDataCache.invalidateUID(uid)
            Log.d(TAG, "Invalidated cached data for UID: $uid due to IOException")
            
            return null
        }
    }
    
    private fun readMifareClassicTag(mifareClassic: MifareClassic, tag: Tag, progressCallback: ((ScanProgress) -> Unit)? = null): NfcTagData? {
        return try {
            mifareClassic.connect()
            
            val sectors = mifareClassic.sectorCount
            val allData = ByteArray(sectors * 48) // Each sector has 3 data blocks * 16 bytes (legacy format)
            val completeData = ByteArray(sectors * 64) // Each sector has 4 blocks * 16 bytes (including trailers)
            
            Log.d(TAG, "Reading MIFARE Classic tag:")
            Log.d(TAG, "UID: ${bytesToHex(tag.id)}")
            Log.d(TAG, "Sectors: $sectors")
            Log.d(TAG, "Size: ${mifareClassic.size} bytes")
            
            // Record debug info
            debugCollector.recordTagInfo(mifareClassic.size, sectors)
            
            // We'll capture raw data during the authentication process instead
            // to avoid interfering with authentication
            
            // Report key derivation progress
            progressCallback?.invoke(ScanProgress(
                stage = ScanStage.KEY_DERIVATION,
                percentage = 0.1f,
                statusMessage = "Deriving authentication keys"
            ))
            
            // Derive proper authentication keys from UID using cached KDF
            val derivedKeys = CachedBambuKeyDerivation.deriveKeys(tag.id)
            debugCollector.recordDerivedKeys(derivedKeys)
            Log.d(TAG, "Derived ${derivedKeys.size} keys from UID")
            
            // Fallback keys in case KDF fails - including common MIFARE Classic defaults
            val fallbackKeys = arrayOf(
                // Default factory keys
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                // Common MIFARE Classic keys
                byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
                byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),
                // Transport Configuration keys (NXP)
                byteArrayOf(0x4D.toByte(), 0x3A.toByte(), 0x99.toByte(), 0xC3.toByte(), 0x51.toByte(), 0xDD.toByte()),
                byteArrayOf(0x1A.toByte(), 0x98.toByte(), 0x2C.toByte(), 0x7E.toByte(), 0x45.toByte(), 0x9A.toByte())
            )
            
            // Combine derived keys with fallback keys
            // Enable fallback keys to help with Sector 1 authentication issues that cause bed temp to show 0°C
            val allKeys = derivedKeys + fallbackKeys
            Log.d(TAG, "Using ${derivedKeys.size} derived keys + ${fallbackKeys.size} fallback keys = ${allKeys.size} total keys")

            // Read data systematically according to RFID-Tag-Guide specification
            for (sector in 0 until minOf(sectors, 16)) {
                var authenticated = false
                var usedKey: ByteArray? = null
                
                // Report authentication progress for this sector
                progressCallback?.invoke(ScanProgress(
                    stage = ScanStage.AUTHENTICATING,
                    percentage = 0.15f + (sector * 0.04f), // 15% to 75% (16 sectors * 4% each)
                    currentSector = sector,
                    statusMessage = "Authenticating sector ${sector + 1}/16"
                ))
                
                Log.d(TAG, "Reading sector $sector")
                
                // Optimization: Try the key at the same index as the sector first
                // Pattern observed: sector N typically uses key index N
                val keyIndicesToTry = if (sector < allKeys.size) {
                    // Try sector-matching key first, then all others
                    listOf(sector) + (allKeys.indices - sector)
                } else {
                    // Fallback to trying all keys if sector >= key count
                    allKeys.indices.toList()
                }
                
                for (keyIndex in keyIndicesToTry) {
                    val key = allKeys[keyIndex]
                    val keyType = if (keyIndex < derivedKeys.size) "derived" else "fallback"
                    val keyHex = key.joinToString("") { "%02X".format(it) }
                    
                    try {
                        Log.v(TAG, "Trying sector $sector with $keyType key A (index $keyIndex): $keyHex")
                        if (mifareClassic.authenticateSectorWithKeyA(sector, key)) {
                            authenticated = true
                            usedKey = key
                            debugCollector.recordSectorAuthentication(sector, true, "KeyA")
                            Log.d(TAG, "Sector $sector authenticated with $keyType key A (index $keyIndex): $keyHex")
                            break
                        } else {
                            Log.v(TAG, "Sector $sector key A failed (index $keyIndex)")
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "Sector $sector key A IOException (index $keyIndex): ${e.message}")
                        debugCollector.recordError("Sector $sector KeyA authentication IOException: ${e.message}")
                        continue
                    }
                    
                    try {
                        Log.v(TAG, "Trying sector $sector with $keyType key B (index $keyIndex): $keyHex")
                        if (mifareClassic.authenticateSectorWithKeyB(sector, key)) {
                            authenticated = true
                            usedKey = key
                            debugCollector.recordSectorAuthentication(sector, true, "KeyB")
                            Log.d(TAG, "Sector $sector authenticated with $keyType key B (index $keyIndex): $keyHex")
                            break
                        } else {
                            Log.v(TAG, "Sector $sector key B failed (index $keyIndex)")
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "Sector $sector key B IOException (index $keyIndex): ${e.message}")
                        debugCollector.recordError("Sector $sector KeyB authentication IOException: ${e.message}")
                        continue
                    }
                }
                
                if (!authenticated) {
                    debugCollector.recordSectorAuthentication(sector, false)
                    debugCollector.recordError("Failed to authenticate sector $sector with any key")
                    Log.w(TAG, "Failed to authenticate sector $sector with any of ${allKeys.size} keys")
                    
                    // Special warning for Sector 1 which contains Block 6 (temperature data)
                    if (sector == 1) {
                        Log.e(TAG, "*** CRITICAL: Sector 1 authentication failed - Block 6 (bed temperature) will be zeros ***")
                        debugCollector.recordError("Sector 1 authentication failure will cause bed temperature to show as 0°C")
                    }
                }
                
                // Read ALL blocks in sector (including trailer block)
                val blocksInSector = mifareClassic.getBlockCountInSector(sector)
                for (blockInSector in 0 until blocksInSector) {
                    val absoluteBlock = mifareClassic.sectorToBlock(sector) + blockInSector
                    val isTrailerBlock = blockInSector == (blocksInSector - 1)
                    
                    // Calculate offsets for dual storage
                    val completeDataOffset = sector * 64 + blockInSector * 16
                    val legacyDataOffset = if (!isTrailerBlock) sector * 48 + blockInSector * 16 else -1
                    
                    // Report block reading progress
                    val blockProgress = 0.75f + (sector * 0.015f) + (blockInSector * 0.004f) // 75% to 95%
                    val blockType = if (isTrailerBlock) "trailer" else "data"
                    progressCallback?.invoke(ScanProgress(
                        stage = ScanStage.READING_BLOCKS,
                        percentage = blockProgress,
                        statusMessage = "Reading $blockType block ${absoluteBlock + 1}"
                    ))
                    
                    try {
                        val blockData = if (authenticated) {
                            mifareClassic.readBlock(absoluteBlock)
                        } else {
                            // Try to read without authentication for public blocks
                            mifareClassic.readBlock(absoluteBlock)
                        }
                        
                        // Store in complete data array (all blocks)
                        System.arraycopy(blockData, 0, completeData, completeDataOffset, 16)
                        
                        // Store in legacy data array (data blocks only, for compatibility)
                        if (!isTrailerBlock && legacyDataOffset >= 0) {
                            System.arraycopy(blockData, 0, allData, legacyDataOffset, 16)
                        }
                        
                        // Log all block data for complete raw data capture
                        val hexData = blockData.joinToString("") { "%02X".format(it) }
                        debugCollector.recordAllBlockData(absoluteBlock, hexData, isTrailerBlock)
                        
                        // Enhanced logging for key blocks and temperature data
                        if (sector <= 1 || absoluteBlock <= 6) {
                            Log.d(TAG, "Block $absoluteBlock (sector $sector, block $blockInSector): $hexData")
                        }
                        
                        // Special logging for Block 6 (temperature data)
                        if (absoluteBlock == 6) {
                            Log.i(TAG, "*** BLOCK 6 DATA (Temperature Block): $hexData ***")
                            val isAllZeros = blockData.all { it == 0.toByte() }
                            if (isAllZeros) {
                                Log.w(TAG, "*** WARNING: Block 6 contains only zeros - bed temperature will show as 0°C ***")
                                debugCollector.recordError("Block 6 read as all zeros - authentication may have failed for sector containing temperature data")
                            }
                        }
                    } catch (e: IOException) {
                        val errorMsg = "Failed to read block $absoluteBlock ($blockType): ${e.message}"
                        debugCollector.recordError(errorMsg)
                        Log.w(TAG, errorMsg)
                        
                        // Fill with zeros if read fails - update both arrays
                        val zeroBlock = ByteArray(16)
                        System.arraycopy(zeroBlock, 0, completeData, completeDataOffset, 16)
                        if (!isTrailerBlock && legacyDataOffset >= 0) {
                            System.arraycopy(zeroBlock, 0, allData, legacyDataOffset, 16)
                        }
                    }
                }
            }
            
            // Report data assembly progress
            progressCallback?.invoke(ScanProgress(
                stage = ScanStage.ASSEMBLING_DATA,
                percentage = 0.95f,
                statusMessage = "Assembling tag data"
            ))
            
            mifareClassic.close()
            
            // Record both legacy and complete raw data for debug analysis
            debugCollector.recordFullRawData(allData) // Legacy 768-byte format for compatibility
            debugCollector.recordCompleteTagData(completeData) // Complete 1024-byte format with trailers
            
            // Create decrypted data array (same as allData since blocks are read post-authentication)
            // In the future, this could be enhanced to store pre-authentication data separately
            debugCollector.recordDecryptedData(allData)
            
            // Check if we have any successful authentications
            if (!debugCollector.hasAuthenticatedSectors()) {
                debugCollector.recordError("Complete authentication failure - no sectors authenticated")
                Log.e(TAG, "Authentication failed for all sectors")
                
                // Invalidate any cached data for this UID since authentication failed
                val uid = bytesToHex(tag.id)
                tagDataCache.invalidateUID(uid)
                Log.d(TAG, "Invalidated cached data for UID: $uid due to authentication failure")
                
                return null // This will trigger authentication failure handling
            }
            
            NfcTagData(
                uid = bytesToHex(tag.id),
                bytes = allData,
                technology = "MifareClassic"
            )
        } catch (e: IOException) {
            e.printStackTrace()
            try {
                mifareClassic.close()
            } catch (closeException: IOException) {
                closeException.printStackTrace()
            }
            
            // Invalidate any cached data for this UID since the read failed
            val uid = bytesToHex(tag.id)
            tagDataCache.invalidateUID(uid)
            Log.d(TAG, "Invalidated cached data for UID: $uid due to MIFARE Classic read IOException")
            
            null
        }
    }
    
    /**
     * Read tag data and return both encrypted and decrypted data
     */
    private suspend fun readTagDataFull(tag: Tag, progressCallback: ((ScanProgress) -> Unit)? = null): Pair<EncryptedScanData, DecryptedScanData>? = withContext(Dispatchers.IO) {
        debugCollector.reset()
        
        val uid = bytesToHex(tag.id)
        val timestamp = java.time.LocalDateTime.now()
        val technology = "MifareClassic"
        val scanStartTime = System.currentTimeMillis()
        
        Log.d(TAG, "Starting full tag read for UID: $uid")
        
        // First read the existing way to get decrypted data
        val nfcTagData = readTagData(tag, progressCallback)
        if (nfcTagData == null) {
            Log.w(TAG, "Tag read returned null - may be authentication failure, continuing to create scan data")
            // Don't return null here - we still want to save the failed scan data
        } else {
            // If we got cached data, we need to populate the debugCollector with the block data
            // so that FilamentInterpreter can access it properly
            populateDebugCollectorFromCachedData(nfcTagData, uid)
        }
        
        val scanDuration = System.currentTimeMillis() - scanStartTime
        
        // Determine scan result
        val scanResult = if (nfcTagData != null) {
            // If readTagData returned actual data (either from physical read or cache), it's successful
            ScanResult.SUCCESS
        } else {
            // Only if readTagData returned null (complete failure) do we mark as failed
            ScanResult.AUTHENTICATION_FAILED
        }
        
        // Create scan data objects using DebugDataCollector factory methods
        val encryptedScanData = debugCollector.createEncryptedScanData(
            uid = uid,
            technology = technology,
            scanDurationMs = scanDuration
        )
        
        val decryptedScanData = debugCollector.createDecryptedScanData(
            uid = uid,
            technology = technology,
            result = scanResult,
            keyDerivationTimeMs = 0, // TODO: Add timing
            authenticationTimeMs = 0  // TODO: Add timing
        )
        
        Log.d(TAG, "Created scan data pair for UID: $uid")
        return@withContext Pair(encryptedScanData, decryptedScanData)
    }
    
    /**
     * Populate debugCollector with cached data so FilamentInterpreter can access block data
     */
    private fun populateDebugCollectorFromCachedData(nfcTagData: NfcTagData, uid: String) {
        Log.d(TAG, "Populating debugCollector with cached data for UID: $uid")
        
        // Convert raw bytes back to block structure to match the original reading logic
        val blockData = mutableMapOf<Int, String>()
        val bytes = nfcTagData.bytes
        
        // The cached bytes contain data from sectors read during authentication
        // Each sector contributes 48 bytes (3 data blocks * 16 bytes, trailer block excluded)
        var byteOffset = 0
        for (sector in 0 until minOf(16, bytes.size / 48)) { // 16 sectors max for MIFARE Classic 1K
            // Match the original loop: blocksInSector - 1 (skip trailer block)
            val blocksInSector = if (sector < 32) 4 else 16 // Standard MIFARE Classic 1K has 4 blocks per sector
            for (blockInSector in 0 until blocksInSector - 1) { // Skip trailer block
                if (byteOffset + 16 <= bytes.size) {
                    // Use the same calculation as original: sectorToBlock(sector) + blockInSector
                    val absoluteBlock = (sector * 4) + blockInSector // sectorToBlock equivalent for MIFARE Classic 1K
                    val blockBytes = bytes.sliceArray(byteOffset until byteOffset + 16)
                    val hexData = blockBytes.joinToString("") { "%02X".format(it) }
                    blockData[absoluteBlock] = hexData
                    byteOffset += 16
                    
                    // Log key blocks for debugging
                    if (sector <= 1 || absoluteBlock <= 6) {
                        Log.d(TAG, "Reconstructed Block $absoluteBlock (sector $sector, block $blockInSector): $hexData")
                    }
                }
            }
        }
        
        // Populate debugCollector with reconstructed data
        debugCollector.recordFullRawData(bytes)
        debugCollector.recordDecryptedData(bytes)
        
        // Record block data for FilamentInterpreter - this is crucial for interpretation
        blockData.forEach { (absoluteBlock, hexData) ->
            debugCollector.recordBlockData(absoluteBlock, hexData)
        }
        
        // Mark as cache hit for debugging
        debugCollector.recordCacheHit()
        
        Log.d(TAG, "Populated debugCollector with ${blockData.size} blocks from cached data")
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
    
    /**
     * Invalidates cached data for a specific UID
     */
    fun invalidateTagCache(uid: String) {
        tagDataCache.invalidateUID(uid)
        Log.d(TAG, "Invalidated tag cache for UID: $uid")
    }
    
    /**
     * Cleanup method to cancel background operations when NfcManager is no longer needed
     */
    fun cleanup() {
        backgroundScope.cancel()
        Log.d(TAG, "NfcManager cleanup completed")
    }
}