package com.bscan.detector

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.util.Log
import com.bscan.model.TagDetectionResult
import com.bscan.model.TagFormat
import com.bscan.model.TagTechnology

/**
 * Detects RFID tag format and technology based on tag properties and data patterns
 */
class TagDetector {
    companion object {
        private const val TAG = "TagDetector"
        
        // OpenTag signature - first 2 bytes should be "OT" (0x4F54)
        private val OPENTAG_SIGNATURE = byteArrayOf(0x4F, 0x54)
    }
    
    /**
     * Detect format and technology from NFC tag
     */
    fun detectTag(nfcTag: Tag): TagDetectionResult {
        try {
            // First, determine the technology
            val technology = determineTechnology(nfcTag)
            
            // Then try to determine format based on technology and data patterns
            return when (technology) {
                TagTechnology.MIFARE_CLASSIC -> detectMifareFormat(nfcTag)
                TagTechnology.NTAG -> detectNtagFormat(nfcTag)
                TagTechnology.UNKNOWN -> TagDetectionResult(
                    tagFormat = TagFormat.UNKNOWN,
                    technology = TagTechnology.UNKNOWN,
                    confidence = 0.0f,
                    detectionReason = "Unsupported tag technology: ${nfcTag.techList.joinToString()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting tag format", e)
            return TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.UNKNOWN,
                confidence = 0.0f,
                detectionReason = "Detection error: ${e.message}"
            )
        }
    }
    
    /**
     * Detect format from raw tag data (for cached tags)
     */
    fun detectFromData(technology: String, data: ByteArray): TagDetectionResult {
        try {
            return when {
                isMifareClassicTech(technology) -> detectMifareFromData(data)
                isNtagTech(technology) -> detectNtagFromData(data)
                else -> TagDetectionResult(
                    tagFormat = TagFormat.UNKNOWN,
                    technology = TagTechnology.UNKNOWN,
                    confidence = 0.0f,
                    detectionReason = "Unsupported technology: $technology"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting from data", e)
            return TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.UNKNOWN,
                confidence = 0.0f,
                detectionReason = "Detection error: ${e.message}"
            )
        }
    }
    
    private fun determineTechnology(nfcTag: Tag): TagTechnology {
        return when {
            nfcTag.techList.contains(MifareClassic::class.java.name) -> TagTechnology.MIFARE_CLASSIC
            nfcTag.techList.any { isNtagTech(it) } -> TagTechnology.NTAG
            else -> TagTechnology.UNKNOWN
        }
    }
    
    private fun detectMifareFormat(nfcTag: Tag): TagDetectionResult {
        try {
            val mifareClassic = MifareClassic.get(nfcTag)
            if (mifareClassic == null) {
                return TagDetectionResult(
                    tagFormat = TagFormat.UNKNOWN,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.0f,
                    detectionReason = "Could not access Mifare Classic interface"
                )
            }
            
            // Check tag size and structure
            val sectorCount = mifareClassic.sectorCount
            val blockCount = mifareClassic.blockCount
            
            return when {
                // Bambu format uses standard 1K (16 sectors, 64 blocks) with proprietary encryption
                sectorCount == 16 && blockCount == 64 -> TagDetectionResult(
                    tagFormat = TagFormat.BAMBU_PROPRIETARY,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.8f,
                    detectionReason = "Standard 1K Mifare Classic structure matches Bambu proprietary format",
                    manufacturerName = "Bambu Lab" // Default for Bambu format
                )
                // Creality might use different sizes or contain ASCII patterns
                else -> TagDetectionResult(
                    tagFormat = TagFormat.CREALITY_ASCII,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.6f,
                    detectionReason = "Non-standard Mifare structure, likely Creality ASCII format ($sectorCount sectors, $blockCount blocks)",
                    manufacturerName = "Creality" // Default for Creality format
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error accessing Mifare Classic tag", e)
            return TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.MIFARE_CLASSIC,
                confidence = 0.0f,
                detectionReason = "Error accessing tag: ${e.message}"
            )
        }
    }
    
    private fun detectNtagFormat(nfcTag: Tag): TagDetectionResult {
        try {
            val ndef = Ndef.get(nfcTag)
            if (ndef != null) {
                // Try to read NDEF data to check for OpenTag signature
                ndef.connect()
                val ndefMessage = ndef.ndefMessage
                ndef.close()
                
                ndefMessage?.records?.forEach { record ->
                    val payload = record.payload
                    if (payload.size >= 2 && payload[0] == OPENTAG_SIGNATURE[0] && payload[1] == OPENTAG_SIGNATURE[1]) {
                        // Extract manufacturer name from payload if possible (at offset 0x14 in OpenTag format)
                        val manufacturerName = if (payload.size >= 20) {
                            try {
                                String(payload.sliceArray(4..19), Charsets.UTF_8).trim('\u0000')
                            } catch (e: Exception) {
                                "Unknown"
                            }
                        } else "Unknown"
                        
                        return TagDetectionResult(
                            tagFormat = TagFormat.OPENTAG_V1,
                            technology = TagTechnology.NTAG,
                            confidence = 0.95f,
                            detectionReason = "OpenTag signature 'OT' found in NDEF data",
                            manufacturerName = manufacturerName
                        )
                    }
                }
            }
            
            // Try NfcA interface for raw data access
            val nfcA = NfcA.get(nfcTag)
            if (nfcA != null) {
                nfcA.connect()
                val atqa = nfcA.atqa
                val sak = nfcA.sak
                nfcA.close()
                
                // Check for OpenTag signature at standard address (0x10)
                // This would require reading specific pages, but we'll default to OpenTag for NTAG
                return TagDetectionResult(
                    tagFormat = TagFormat.OPENTAG_V1,
                    technology = TagTechnology.NTAG,
                    confidence = 0.7f,
                    detectionReason = "NTAG technology detected, defaulting to OpenTag standard (ATQA: ${atqa.joinToString { "%02X".format(it) }}, SAK: %02X".format(sak) + ")",
                    manufacturerName = "Unknown"
                )
            }
            
            return TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.NTAG,
                confidence = 0.0f,
                detectionReason = "NTAG detected but could not access data interfaces"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Error accessing NTAG", e)
            return TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.NTAG,
                confidence = 0.0f,
                detectionReason = "Error accessing NTAG: ${e.message}"
            )
        }
    }
    
    private fun detectMifareFromData(data: ByteArray): TagDetectionResult {
        Log.d(TAG, "detectMifareFromData called with ${data.size} bytes")
        return when {
            // Standard 1K size (complete or data-only) suggests Bambu proprietary format
            data.size == 1024 -> {
                Log.d(TAG, "Detected Bambu proprietary format (1024 bytes - complete)")
                TagDetectionResult(
                    tagFormat = TagFormat.BAMBU_PROPRIETARY,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.8f,
                    detectionReason = "1024-byte data matches Bambu proprietary format",
                    manufacturerName = "Bambu Lab"
                )
            }
            data.size == 768 -> {
                Log.d(TAG, "Detected Bambu proprietary format (768 bytes - data-only)")
                TagDetectionResult(
                    tagFormat = TagFormat.BAMBU_PROPRIETARY,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.8f,
                    detectionReason = "768-byte data matches Bambu proprietary format (data-only)",
                    manufacturerName = "Bambu Lab"
                )
            }
            // Check for Creality ASCII patterns in blocks 4-6 (64-96 bytes)
            data.size >= 96 && containsCrealityPattern(data.sliceArray(64..95)) -> {
                Log.d(TAG, "Detected Creality ASCII format")
                TagDetectionResult(
                    tagFormat = TagFormat.CREALITY_ASCII,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.9f,
                    detectionReason = "Creality ASCII pattern detected in blocks 4-6",
                    manufacturerName = "Creality"
                )
            }
            else -> {
                Log.w(TAG, "Unknown Mifare pattern detected: ${data.size} bytes")
                TagDetectionResult(
                    tagFormat = TagFormat.UNKNOWN,
                    technology = TagTechnology.MIFARE_CLASSIC,
                    confidence = 0.3f,
                    detectionReason = "Unknown Mifare pattern (${data.size} bytes)"
                )
            }
        }
    }
    
    private fun detectNtagFromData(data: ByteArray): TagDetectionResult {
        return when {
            // Check for OpenTag signature "OT" at the beginning
            data.size >= 2 && data[0] == OPENTAG_SIGNATURE[0] && data[1] == OPENTAG_SIGNATURE[1] -> {
                val manufacturerName = if (data.size >= 20) {
                    try {
                        String(data.sliceArray(4..19), Charsets.UTF_8).trim('\u0000')
                    } catch (e: Exception) {
                        "Unknown"
                    }
                } else "Unknown"
                
                TagDetectionResult(
                    tagFormat = TagFormat.OPENTAG_V1,
                    technology = TagTechnology.NTAG,
                    confidence = 0.95f,
                    detectionReason = "OpenTag 'OT' signature found at data start",
                    manufacturerName = manufacturerName
                )
            }
            // Check at offset 0x10 (16 bytes) where OpenTag data typically starts
            data.size >= 18 && data[16] == OPENTAG_SIGNATURE[0] && data[17] == OPENTAG_SIGNATURE[1] -> {
                val manufacturerName = if (data.size >= 36) {
                    try {
                        String(data.sliceArray(20..35), Charsets.UTF_8).trim('\u0000')
                    } catch (e: Exception) {
                        "Unknown"
                    }
                } else "Unknown"
                
                TagDetectionResult(
                    tagFormat = TagFormat.OPENTAG_V1,
                    technology = TagTechnology.NTAG,
                    confidence = 0.95f,
                    detectionReason = "OpenTag 'OT' signature found at offset 0x10",
                    manufacturerName = manufacturerName
                )
            }
            else -> TagDetectionResult(
                tagFormat = TagFormat.UNKNOWN,
                technology = TagTechnology.NTAG,
                confidence = 0.4f,
                detectionReason = "NTAG data without recognisable patterns (${data.size} bytes)"
            )
        }
    }
    
    private fun containsCrealityPattern(blockData: ByteArray): Boolean {
        try {
            // Creality data is ASCII-encoded, look for pattern: HEX DIGITS SPACE HEX SPACE etc
            val asciiString = String(blockData, Charsets.UTF_8).trim()
            
            // Expected pattern: "AAA BBBBB CCCC DDDDD #EEEEEE FFFFF GGGGG..."
            // Look for hex digits, spaces, and # symbol
            val pattern = Regex("[0-9A-Fa-f]{3}\\s+[0-9A-Fa-f]{5}\\s+[0-9A-Fa-f]{4}\\s+[0-9A-Fa-f]{5}\\s+#[0-9A-Fa-f]{6}")
            
            return pattern.containsMatchIn(asciiString)
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun isMifareClassicTech(technology: String): Boolean {
        return technology.contains("MifareClassic", ignoreCase = true)
    }
    
    private fun isNtagTech(technology: String): Boolean {
        return technology.contains("Ndef", ignoreCase = true) || 
               technology.contains("NfcA", ignoreCase = true) ||
               technology.contains("NTAG", ignoreCase = true)
    }
}