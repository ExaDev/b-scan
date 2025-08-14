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
import java.io.IOException

class NfcManager(private val activity: Activity) {
    private companion object {
        const val TAG = "NfcManager"
    }
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    val debugCollector = DebugDataCollector()
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
            
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            return tag?.let { readTagData(it) }
        }
        return null
    }
    
    private fun readTagData(tag: Tag): NfcTagData? {
        try {
            debugCollector.reset() // Start fresh for each scan
            
            val mifareClassic = MifareClassic.get(tag)
            return if (mifareClassic != null) {
                readMifareClassicTag(mifareClassic, tag)
            } else {
                debugCollector.recordError("Tag is not MIFARE Classic compatible")
                // Fallback to reading basic tag info
                NfcTagData(
                    uid = bytesToHex(tag.id),
                    bytes = ByteArray(0), // Empty for now
                    technology = tag.techList.firstOrNull() ?: "Unknown"
                )
            }
        } catch (e: IOException) {
            debugCollector.recordError("IOException reading tag: ${e.message}")
            e.printStackTrace()
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
            
            // Derive proper authentication keys from UID using KDF
            val derivedKeys = BambuKeyDerivation.deriveKeys(tag.id)
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
            val allKeys = derivedKeys + fallbackKeys
            
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
                        Log.v(TAG, "Sector $sector key A IOException (index $keyIndex): ${e.message}")
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
                        Log.v(TAG, "Sector $sector key B IOException (index $keyIndex): ${e.message}")
                        continue
                    }
                }
                
                if (!authenticated) {
                    debugCollector.recordSectorAuthentication(sector, false)
                    debugCollector.recordError("Failed to authenticate sector $sector with any key")
                    Log.w(TAG, "Failed to authenticate sector $sector with any of ${allKeys.size} keys")
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
                        
                        // Log block data for key blocks (0-6)
                        if (sector <= 1 || absoluteBlock <= 6) {
                            val hexData = blockData.joinToString("") { "%02X".format(it) }
                            debugCollector.recordBlockData(absoluteBlock, hexData)
                            Log.d(TAG, "Block $absoluteBlock (sector $sector, block $blockInSector): $hexData")
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
            null
        }
    }
    
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }
}