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
            val data = mutableListOf<Byte>()
            
            // Read first few sectors that contain Bambu Lab data
            for (sector in 0 until minOf(sectors, 16)) {
                val blocks = mifareClassic.getBlockCountInSector(sector)
                
                for (block in 0 until blocks - 1) { // Skip trailer block
                    val blockIndex = mifareClassic.sectorToBlock(sector) + block
                    
                    try {
                        // Try to read without authentication first
                        val blockData = mifareClassic.readBlock(blockIndex)
                        data.addAll(blockData.toList())
                    } catch (e: IOException) {
                        // If read fails, add zeros as placeholder
                        data.addAll(ByteArray(16).toList())
                    }
                }
            }
            
            mifareClassic.close()
            
            NfcTagData(
                uid = bytesToHex(tag.id),
                bytes = data.toByteArray(),
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