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
    suspend fun handleIntentAsync(intent: Intent): NfcTagData? = withContext(Dispatchers.IO) {
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action || 
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            
            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            }
            return@withContext tag?.let { readTagData(it) }
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
    private fun readTagData(tag: Tag): NfcTagData? {
        debugCollector.reset() // Start fresh for each scan
        
        val uid = bytesToHex(tag.id)
        Log.d(TAG, "Reading tag with UID: $uid")
        
        try {
            // Check cache first for this UID
            tagDataCache.getCachedTagData(uid)?.let { cachedData ->
                Log.d(TAG, "Found cached tag data for UID: $uid - skipping physical read")
                debugCollector.recordCacheHit()
                return cachedData
            }
            
            Log.d(TAG, "No cached data for UID: $uid - performing physical read")
            val mifareClassic = MifareClassic.get(tag)
            val tagData = if (mifareClassic != null) {
                readMifareClassicTag(mifareClassic, tag)
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
    
    private fun readMifareClassicTag(mifareClassic: MifareClassic, tag: Tag): NfcTagData? {
        return try {
            mifareClassic.connect()
            
            val sectors = mifareClassic.sectorCount
            val allData = ByteArray(sectors * 48) // Each sector has 3 data blocks * 16 bytes
            
            Log.d(TAG, "Reading MIFARE Classic tag:")
            Log.d(TAG, "UID: ${bytesToHex(tag.id)}")
            Log.d(TAG, "Sectors: $sectors")
            Log.d(TAG, "Size: ${mifareClassic.size} bytes")
            
            // Record debug info
            debugCollector.recordTagInfo(mifareClassic.size, sectors)
            
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
                
                Log.d(TAG, "Reading sector $sector")
                
                // Try authentication with derived keys first, then fallback keys
                for ((keyIndex, key) in allKeys.withIndex()) {
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
                
                // Read blocks in sector (skip trailer block which contains keys)
                val blocksInSector = mifareClassic.getBlockCountInSector(sector)
                for (blockInSector in 0 until blocksInSector - 1) {
                    val absoluteBlock = mifareClassic.sectorToBlock(sector) + blockInSector
                    val dataOffset = sector * 48 + blockInSector * 16
                    
                    try {
                        val blockData = if (authenticated) {
                            mifareClassic.readBlock(absoluteBlock)
                        } else {
                            // Try to read without authentication for public blocks
                            mifareClassic.readBlock(absoluteBlock)
                        }
                        System.arraycopy(blockData, 0, allData, dataOffset, 16)
                        
                        // Log block data for key blocks (0-6) and Block 6 specifically for bed temperature debugging
                        if (sector <= 1 || absoluteBlock <= 6 || absoluteBlock == 6) {
                            val hexData = blockData.joinToString("") { "%02X".format(it) }
                            debugCollector.recordBlockData(absoluteBlock, hexData)
                            Log.d(TAG, "Block $absoluteBlock (sector $sector, block $blockInSector): $hexData")
                            
                            // Special logging for Block 6 (temperature data)
                            if (absoluteBlock == 6) {
                                Log.i(TAG, "*** BLOCK 6 DATA (Temperature Block): $hexData ***")
                                val isAllZeros = blockData.all { it == 0.toByte() }
                                if (isAllZeros) {
                                    Log.w(TAG, "*** WARNING: Block 6 contains only zeros - bed temperature will show as 0°C ***")
                                    debugCollector.recordError("Block 6 read as all zeros - authentication may have failed for sector containing temperature data")
                                }
                            }
                        }
                    } catch (e: IOException) {
                        val errorMsg = "Failed to read block $absoluteBlock: ${e.message}"
                        debugCollector.recordError(errorMsg)
                        Log.w(TAG, errorMsg)
                        // Fill with zeros if read fails
                        for (i in 0 until 16) {
                            allData[dataOffset + i] = 0
                        }
                    }
                }
            }
            
            mifareClassic.close()
            
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