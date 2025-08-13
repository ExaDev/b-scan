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
        // Need at least 15 blocks (240 bytes) to read all required data
        if (data.bytes.size < 240) {
            return null
        }
        
        return try {
            // Extract data according to official RFID-Tag-Guide block structure
            
            // Block 2: Filament Type (16 bytes)
            val material = string(data.bytes, 2, 0, 16)
            
            // Block 4: Detailed Filament Type (16 bytes) 
            val detailedFilamentType = string(data.bytes, 4, 0, 16)
            
            // Block 5: Color RGBA (bytes 0-3), Spool Weight (bytes 4-5), Filament Diameter (bytes 8-11)
            val colorBytes = bytes(data.bytes, 5, 0, 4) // RGBA format
            val spoolWeight = int(data.bytes, 5, 4, 2) // uint16 LE
            val filamentDiameter = float(data.bytes, 5, 8, 4) ?: 1.75f // float LE
            
            // Block 9: Tray UID (16 bytes)
            val trayUid = string(data.bytes, 9, 0, 16)
            
            // Block 12: Production Date/Time (16 bytes)
            val productionDate = datetime(data.bytes, 12, 0)?.toString() ?: "Unknown"
            
            // Block 14: Filament Length (bytes 4-5) 
            val filamentLength = int(data.bytes, 14, 4, 2) // uint16 LE
            
            // Convert RGBA color bytes to hex for display (ignore alpha channel)
            val colorHex = if (colorBytes.size >= 3) {
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
                colorHex = colorHex,
                spoolWeight = spoolWeight,
                filamentDiameter = filamentDiameter,
                filamentLength = filamentLength,
                productionDate = productionDate
            )
        } catch (e: Exception) {
            null
        }
    }
    
    // Helper functions based on BambuTagDecodeHelpers  
    // Note: MIFARE Classic has sectors, each sector has blocks
    // Sectors 0-31 have 4 blocks each (blocks 0-3, 4-7, 8-11, etc.)
    // We read data sequentially: sector 0 blocks 0-2, sector 1 blocks 0-2, etc.
    private fun bytes(data: ByteArray, blockNumber: Int, offset: Int, len: Int): ByteArray {
        // Calculate actual offset based on MIFARE Classic layout
        // Each sector contributes 3 data blocks (48 bytes), skipping trailer blocks
        val sector = blockNumber / 4
        val blockInSector = blockNumber % 4
        
        // Skip if this is a trailer block (every 4th block contains keys)
        if (blockInSector == 3) {
            return ByteArray(len) // Return zeros for trailer blocks
        }
        
        val startIndex = sector * 48 + blockInSector * 16 + offset
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