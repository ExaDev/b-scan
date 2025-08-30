package com.bscan

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Parcel
import com.bscan.model.NfcTagData

/**
 * Helper utilities for NFC testing with stubbed intents and mock tags.
 * 
 * Provides utilities to create mock NFC intents for testing the complete
 * scan workflow without requiring physical NFC hardware.
 */
object NfcTestHelper {

    /**
     * Create a stubbed NFC intent with mock tag data for testing.
     * This simulates the Android system sending an NFC_TAG_DISCOVERED intent.
     */
    fun createStubbedNfcIntent(tagData: NfcTagData): Intent {
        val intent = Intent(NfcAdapter.ACTION_TAG_DISCOVERED)
        
        // Create mock Tag using the safest available method
        val mockTag = createMockTag(tagData.uid, tagData.bytes, tagData.technology)
        intent.putExtra(NfcAdapter.EXTRA_TAG, mockTag)
        
        return intent
    }

    /**
     * Create a mock NFC Tag for testing purposes.
     * Uses Parcel-based creation as it's the most reliable cross-Android-version method.
     */
    private fun createMockTag(uid: String, bytes: ByteArray, technology: String): Tag {
        // Convert UID hex string to byte array
        val uidBytes = uid.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        
        // Create tech list and extras
        val techList = arrayOf(technology)
        val techExtras = arrayOf(Bundle())
        
        return createTagViaParcel(uidBytes, techList, techExtras, bytes)
    }

    /**
     * Create Tag using Parcel serialization.
     * This method is more reliable than reflection as it uses the public API.
     */
    private fun createTagViaParcel(
        uidBytes: ByteArray, 
        techList: Array<String>, 
        techExtras: Array<Bundle>,
        tagBytes: ByteArray
    ): Tag {
        val parcel = Parcel.obtain()
        try {
            // Write Tag data to Parcel in the format expected by Tag.CREATOR
            parcel.writeByteArray(uidBytes)
            parcel.writeStringArray(techList)
            parcel.writeInt(techExtras.size)
            techExtras.forEach { bundle ->
                // For MifareClassic, add the tag data as EXTRA_DATA
                if (techList.contains("MifareClassic") && tagBytes.isNotEmpty()) {
                    bundle.putByteArray("EXTRA_DATA", tagBytes)
                }
                parcel.writeBundle(bundle)
            }
            parcel.writeInt(0) // serviceHandle
            parcel.writeInt(1) // isMock flag
            parcel.writeLong(0L) // nativePtr
            
            parcel.setDataPosition(0)
            return Tag.CREATOR.createFromParcel(parcel)
        } finally {
            parcel.recycle()
        }
    }

    /**
     * Create authentication failure scenario for testing error handling.
     */
    fun createAuthFailureIntent(uid: String): Intent {
        val tagData = NfcTagData(
            uid = uid,
            bytes = ByteArray(0), // Empty bytes simulate authentication failure
            technology = "MifareClassic"
        )
        return createStubbedNfcIntent(tagData)
    }

    /**
     * Create corrupted tag scenario for testing error handling.
     */
    fun createCorruptedTagIntent(): Intent {
        val tagData = NfcTagData(
            uid = "BADBEEF",
            bytes = ByteArray(10) { 0xFF.toByte() }, // Invalid data
            technology = "MifareClassic"
        )
        return createStubbedNfcIntent(tagData)
    }

    /**
     * Validate that an intent contains the expected NFC tag data.
     */
    fun validateNfcIntent(intent: Intent): Boolean {
        return intent.action == NfcAdapter.ACTION_TAG_DISCOVERED &&
               intent.hasExtra(NfcAdapter.EXTRA_TAG)
    }
}