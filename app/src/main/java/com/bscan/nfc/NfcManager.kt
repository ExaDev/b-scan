package com.bscan.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import com.bscan.model.NfcTagData
import com.bscan.nfc.BambuKeyDerivation
import java.io.IOException

class NfcManager(private val activity: Activity) {
    
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
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
            val mifareClassic = MifareClassic.get(tag)
            return if (mifareClassic != null) {
                readMifareClassicTag(mifareClassic, tag)
            } else {
                // Fallback to reading basic tag info
                NfcTagData(
                    uid = bytesToHex(tag.id),
                    bytes = ByteArray(0), // Empty for now
                    technology = tag.techList.firstOrNull() ?: "Unknown"
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }
    
    private fun readMifareClassicTag(mifareClassic: MifareClassic, tag: Tag): NfcTagData? {
        return try {
            mifareClassic.connect()
            
            val sectors = mifareClassic.sectorCount
            val allData = ByteArray(sectors * 48) // Each sector has 3 data blocks * 16 bytes
            
            // Derive proper authentication keys from UID using KDF
            val derivedKeys = BambuKeyDerivation.deriveKeys(tag.id)
            
            // Fallback keys in case KDF fails
            val fallbackKeys = arrayOf(
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
            )
            
            // Combine derived keys with fallback keys
            val allKeys = derivedKeys + fallbackKeys
            
            // Read data systematically according to RFID-Tag-Guide specification
            for (sector in 0 until minOf(sectors, 16)) {
                var authenticated = false
                
                // Try authentication with derived keys first, then fallback keys
                for (key in allKeys) {
                    try {
                        if (mifareClassic.authenticateSectorWithKeyA(sector, key)) {
                            authenticated = true
                            break
                        }
                    } catch (e: IOException) {
                        continue
                    }
                    
                    try {
                        if (mifareClassic.authenticateSectorWithKeyB(sector, key)) {
                            authenticated = true
                            break
                        }
                    } catch (e: IOException) {
                        continue
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
                    } catch (e: IOException) {
                        // Fill with zeros if read fails
                        for (i in 0 until 16) {
                            allData[dataOffset + i] = 0
                        }
                    }
                }
            }
            
            mifareClassic.close()
            
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