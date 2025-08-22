package com.bscan.debug

import android.util.Log
import com.bscan.model.NfcTagData

/**
 * Debug helper specifically for diagnosing bed temperature parsing issues with real RFID tags
 */
object BedTemperatureDebugHelper {
    private const val TAG = "BedTempDebug"
    
    /**
     * Analyzes tag data to identify why bed temperature might be showing as 0°C
     */
    fun analyzeBedTemperatureIssue(tagData: NfcTagData, debugCollector: DebugDataCollector) {
        Log.i(TAG, "=== BED TEMPERATURE DEBUG ANALYSIS ===")
        Log.i(TAG, "Tag UID: ${tagData.uid}")
        Log.i(TAG, "Tag data size: ${tagData.bytes.size} bytes")
        
        if (tagData.bytes.size < 240) {
            Log.e(TAG, "ISSUE: Insufficient tag data (${tagData.bytes.size} bytes, need 240+)")
            return
        }
        
        // Extract Block 6 using the same logic as BambuTagDecoder
        val block6Data = extractBlock(tagData.bytes, 6)
        val block6Hex = block6Data.joinToString("") { "%02X".format(it) }
        Log.i(TAG, "Block 6 raw data: $block6Hex")
        
        // Check if Block 6 is all zeros
        val isAllZeros = block6Data.all { it == 0.toByte() }
        if (isAllZeros) {
            Log.e(TAG, "ISSUE: Block 6 is all zeros - bed temperature will be 0°C")
            Log.e(TAG, "CAUSE: Most likely Sector 1 authentication failed")
            
            // Check other blocks in Sector 1 to confirm authentication failure
            val block4Data = extractBlock(tagData.bytes, 4)
            val block5Data = extractBlock(tagData.bytes, 5)
            val block4AllZeros = block4Data.all { it == 0.toByte() }
            val block5AllZeros = block5Data.all { it == 0.toByte() }
            
            if (block4AllZeros && block5AllZeros) {
                Log.e(TAG, "CONFIRMED: Entire Sector 1 (Blocks 4,5,6) is zeros - authentication failed")
                debugCollector.recordError("Sector 1 complete authentication failure - all temperature data lost")
            } else {
                Log.w(TAG, "PARTIAL: Only Block 6 is zeros - possible selective read failure")
                Log.i(TAG, "Block 4: ${block4Data.joinToString("") { "%02X".format(it) }}")
                Log.i(TAG, "Block 5: ${block5Data.joinToString("") { "%02X".format(it) }}")
            }
            
            // Suggest solutions
            Log.i(TAG, "SOLUTIONS:")
            Log.i(TAG, "1. Check if UID-based key derivation is working correctly")
            Log.i(TAG, "2. Try alternative MIFARE Classic keys")
            Log.i(TAG, "3. Verify the tag is a genuine Bambu Lab spool")
            return
        }
        
        // Block 6 has data - analyze the temperature values
        Log.i(TAG, "Block 6 contains data - analyzing temperature fields...")
        
        val dryingTemp = extractUInt16LE(block6Data, 0)
        val dryingTime = extractUInt16LE(block6Data, 2)
        val bedTempType = extractUInt16LE(block6Data, 4)
        val bedTemp = extractUInt16LE(block6Data, 6)
        val maxTemp = extractUInt16LE(block6Data, 8)
        val minTemp = extractUInt16LE(block6Data, 10)
        
        Log.i(TAG, "Temperature data extracted:")
        Log.i(TAG, "- Drying temp: ${dryingTemp}°C")
        Log.i(TAG, "- Drying time: ${dryingTime}h")
        Log.i(TAG, "- Bed temp type: $bedTempType")
        Log.i(TAG, "- Bed temperature: ${bedTemp}°C")
        Log.i(TAG, "- Max hotend temp: ${maxTemp}°C")
        Log.i(TAG, "- Min hotend temp: ${minTemp}°C")
        
        if (bedTemp == 0) {
            Log.w(TAG, "ISSUE: Bed temperature field contains 0 - this may be normal for some materials")
            if (dryingTemp == 0 && maxTemp == 0 && minTemp == 0) {
                Log.e(TAG, "SUSPICIOUS: All temperatures are 0 - possible data corruption")
            } else {
                Log.i(TAG, "INFO: Other temperatures are non-zero - bed temp 0 may be correct for this material")
            }
        } else {
            Log.i(TAG, "SUCCESS: Bed temperature parsed correctly as ${bedTemp}°C")
        }
        
        // Check for common data patterns that might indicate format issues
        checkForCommonPatterns(block6Data, block6Hex)
        
        Log.i(TAG, "=== END BED TEMPERATURE DEBUG ANALYSIS ===")
    }
    
    private fun checkForCommonPatterns(block6Data: ByteArray, block6Hex: String) {
        // Check for all same bytes (indicates possible format issue)
        val uniqueBytes = block6Data.distinct().size
        if (uniqueBytes == 1) {
            Log.w(TAG, "PATTERN: Block 6 contains only repeated byte 0x${"%02X".format(block6Data[0])}")
        }
        
        // Check for incremental pattern (might indicate test/debug tag)
        val isIncremental = (0..14).all { i -> block6Data[i + 1] == (block6Data[i] + 1).toByte() }
        if (isIncremental) {
            Log.w(TAG, "PATTERN: Block 6 contains incremental pattern - possible test tag")
        }
        
        // Check for known good pattern (from our test data)
        if (block6Hex == "3700080001002D00DC00DC0000000000") {
            Log.i(TAG, "PATTERN: Block 6 matches known good test data (PLA, 45°C bed temp)")
        }
        
        // Look for ASCII strings (might indicate wrong block)
        val containsAscii = block6Data.any { it in 32..126 }
        if (containsAscii) {
            val asciiAttempt = String(block6Data, charset = Charsets.US_ASCII).replace('\u0000', '.')
            Log.w(TAG, "PATTERN: Block 6 contains ASCII characters: '$asciiAttempt' - possible wrong block read")
        }
    }
    
    private fun extractBlock(data: ByteArray, blockNumber: Int): ByteArray {
        // Same logic as BambuTagDecoder.bytes() method
        val compressedBlockIndex = blockNumber - (blockNumber / 4)
        val startIndex = compressedBlockIndex * 16
        val endIndex = startIndex + 16
        
        return if (startIndex >= 0 && startIndex < data.size && endIndex <= data.size) {
            data.copyOfRange(startIndex, endIndex)
        } else {
            ByteArray(16) // Return zeros if out of bounds
        }
    }
    
    private fun extractUInt16LE(data: ByteArray, offset: Int): Int {
        return if (offset + 1 < data.size) {
            (data[offset].toUByte().toInt()) + (data[offset + 1].toUByte().toInt() shl 8)
        } else {
            0
        }
    }
}