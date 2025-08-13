package com.bscan.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.NfcA
import com.bscan.model.NfcTagData
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
            
            // Bambu Lab authentication keys from reference implementation
            val bambuKeys = arrayOf(
                byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
                byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
                byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),
                // Additional Bambu Lab keys
                byteArrayOf(0x48, 0x4D, 0x42, 0x48, 0x44, 0x49),
                byteArrayOf(0xF1.toByte(), 0xC4.toByte(), 0x42, 0x88.toByte(), 0x10, 0x01)
            )
            
            // Read data systematically like the reference implementation
            for (sector in 0 until minOf(sectors, 16)) {
                var authenticated = false
                
                // Try authentication with each key
                for (key in bambuKeys) {
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
                
                // Read blocks in sector (skip trailer block)
                val blocksInSector = mifareClassic.getBlockCountInSector(sector)
                for (blockInSector in 0 until blocksInSector - 1) {
                    val absoluteBlock = mifareClassic.sectorToBlock(sector) + blockInSector
                    val dataOffset = sector * 48 + blockInSector * 16
                    
                    try {
                        val blockData = mifareClassic.readBlock(absoluteBlock)
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