package com.bscan.decoder

import com.bscan.model.FilamentInfo
import com.bscan.model.NfcTagData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BambuTagDecoder {
    
    fun parseTagDetails(data: NfcTagData): FilamentInfo? {
        // Ensure there is enough data in the tag bytes to extract the necessary details
        if (data.bytes.size < 80) {
            return null
        }
        
        return try {
            val trayUid = hexstring(data.bytes, 9, 0, 16)
            
            // Extract the color bytes from block 5, offset 0, length 4
            val colorBytes = bytes(data.bytes, 5, 0, 4)
            val material = string(data.bytes, 2, 0, 16)
            val colorHex = hexstring(data.bytes, 5, 0, 4)
            val detailedFilamentType = string(data.bytes, 4, 0, 16)
            
            // Extract RGB color (first 6 characters of color hex)
            val rgb = colorHex.take(6)
            
            // Convert color bytes to RGB for display
            val colorRgb = if (colorBytes.size >= 3) {
                String.format("#%02X%02X%02X", 
                    colorBytes[0].toUByte().toInt(),
                    colorBytes[1].toUByte().toInt(), 
                    colorBytes[2].toUByte().toInt())
            } else "#000000"
            
            FilamentInfo(
                uid = data.uid,
                trayUID = trayUid,
                filamentType = material,
                detailedFilamentType = detailedFilamentType,
                colorHex = colorRgb,
                spoolWeight = int(data.bytes, 5, 4),
                filamentDiameter = float(data.bytes, 5, 8, 4) ?: 1.75f,
                filamentLength = int(data.bytes, 14, 4),
                productionDate = datetime(data.bytes, 12, 0)?.toString() ?: "Unknown"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Helper functions based on BambuTagDecodeHelpers
    private fun bytes(data: ByteArray, blockNumber: Int, offset: Int, len: Int): ByteArray {
        val startIndex = blockNumber * 16 + offset
        val endIndex = startIndex + len
        return if (startIndex < data.size && endIndex <= data.size) {
            data.copyOfRange(startIndex, endIndex)
        } else {
            ByteArray(len) // Return zeros if out of bounds
        }
    }
    
    private fun hexstring(data: ByteArray, blockNumber: Int, offset: Int, len: Int): String {
        return bytes(data, blockNumber, offset, len).joinToString("") { String.format("%02X", it) }
    }
    
    private fun string(data: ByteArray, blockNumber: Int, offset: Int, len: Int): String {
        return bytes(data, blockNumber, offset, len).toString(Charsets.UTF_8).replace("\u0000", "")
    }
    
    private fun int(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 2): Int {
        val byteArray = bytes(data, blockNumber, offset, len)
        return when (len) {
            4 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).int
            2 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            else -> 0
        }
    }
    
    private fun float(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 8): Float? {
        val byteArray = bytes(data, blockNumber, offset, len)
        return try {
            when (len) {
                8 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).double.toFloat()
                4 -> ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).float
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun datetime(data: ByteArray, blockNumber: Int, offset: Int, len: Int = 16): LocalDateTime? {
        return try {
            val dateString = string(data, blockNumber, offset, len)
            val formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm")
            LocalDateTime.parse(dateString, formatter)
        } catch (e: Exception) {
            null
        }
    }
}