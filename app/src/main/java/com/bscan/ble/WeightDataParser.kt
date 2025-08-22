package com.bscan.ble

import android.util.Log
import com.bscan.model.WeightParser
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object WeightDataParser {
    
    private const val TAG = "WeightDataParser"
    
    fun parseWeight(data: ByteArray, parser: WeightParser): Float {
        if (data.isEmpty()) {
            Log.w(TAG, "Empty weight data received")
            return 0f
        }
        
        // Comprehensive raw data logging for offline analysis
        val hexString = data.joinToString(" ") { "%02X".format(it) }
        val decimalString = data.joinToString(" ") { it.toInt().and(0xFF).toString() }
        val binaryString = data.joinToString(" ") { it.toInt().and(0xFF).toString(2).padStart(8, '0') }
        
        Log.i(TAG, "=== RAW WEIGHT DATA ANALYSIS ===")
        Log.i(TAG, "Data Length: ${data.size} bytes")
        Log.i(TAG, "Hex:     $hexString")
        Log.i(TAG, "Decimal: $decimalString") 
        Log.i(TAG, "Binary:  $binaryString")
        Log.i(TAG, "Parser:  $parser")
        
        // Show individual byte interpretations
        if (data.size >= 7) {
            Log.i(TAG, "Individual byte analysis:")
            for (i in data.indices) {
                val byteVal = data[i].toInt() and 0xFF
                Log.i(TAG, "  Byte $i: 0x${byteVal.toString(16).padStart(2, '0').uppercase()} = $byteVal decimal")
            }
            
            // Show all possible 16-bit little-endian combinations
            Log.i(TAG, "16-bit little-endian combinations:")
            for (i in 0 until data.size - 1) {
                val low = data[i].toInt() and 0xFF
                val high = data[i + 1].toInt() and 0xFF
                val combined = low or (high shl 8)
                Log.i(TAG, "  Bytes $i-${i+1}: ${low} | (${high} << 8) = $combined")
            }
        }
        Log.i(TAG, "==================================")
        
        return try {
            when (parser) {
                WeightParser.STANDARD_16BIT_GRAMS -> parseStandardWeightScale(data)
                WeightParser.GENERIC_FFE0_RAW -> parseGenericFFE0(data)
                WeightParser.CUSTOM_LITTLE_ENDIAN_16BIT -> parseLittleEndian16Bit(data)
                WeightParser.CUSTOM_BIG_ENDIAN_16BIT -> parseBigEndian16Bit(data)
                WeightParser.CUSTOM_32BIT_FLOAT -> parse32BitFloat(data)
                WeightParser.ASCII_STRING -> parseAsciiString(data)
                WeightParser.FFE0_BYTES_5_6_LITTLE_ENDIAN -> parseFFE0Bytes5_6LittleEndian(data)
                WeightParser.FFE0_BYTES_4_5_PAGED -> parseFFE0Bytes4_5Paged(data)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing weight data with parser $parser: ${e.message}")
            0f
        }
    }
    
    private fun parseStandardWeightScale(data: ByteArray): Float {
        // Standard BLE Weight Scale Service format
        // Reference: https://www.bluetooth.com/specifications/specs/weight-scale-service-1-0/
        
        if (data.size < 3) {
            Log.w(TAG, "Insufficient data for standard weight scale format")
            return 0f
        }
        
        // Flags byte
        val flags = data[0].toInt() and 0xFF
        
        // Weight measurement unit
        val isImperial = (flags and 0x01) != 0
        val hasTimestamp = (flags and 0x02) != 0
        val hasUserID = (flags and 0x04) != 0
        val hasBMI = (flags and 0x08) != 0
        
        var offset = 1
        
        // Weight value (16-bit, resolution 0.005 kg or 0.01 lb)
        if (data.size < offset + 2) return 0f
        
        val weightRaw = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
        val weight = if (isImperial) {
            weightRaw * 0.01f * 453.592f // Convert pounds to grams
        } else {
            weightRaw * 0.005f * 1000f // Convert kg to grams
        }
        
        Log.d(TAG, "Standard weight scale: ${weight}g (imperial: $isImperial, raw: $weightRaw)")
        return weight
    }
    
    private fun parseGenericFFE0(data: ByteArray): Float {
        // Generic FFE0 service - we'll need to determine the format
        // Based on user's data: 08 07 03 01 00 5C 00 (7 bytes)
        
        Log.d(TAG, "Attempting to parse FFE0 data: ${data.size} bytes")
        
        // Try different parsing methods and return the most reasonable result
        val candidates = mutableListOf<Pair<String, Float>>()
        
        // For this specific scale, try different interpretations
        if (data.size >= 7) {
            // Try weight at different byte positions with different interpretations
            
            // Try bytes 5-6 (0x5C 0x00) as little-endian weight in grams
            val weightPos5 = (data[5].toInt() and 0xFF) or ((data[6].toInt() and 0xFF) shl 8)
            if (weightPos5 > 0 && weightPos5 < 5000) {
                candidates.add("Bytes 5-6 little-endian" to weightPos5.toFloat())
            }
            
            // Try bytes 5-6 as weight in decisgrams (divide by 10)
            if (weightPos5 > 0 && weightPos5 < 50000) {
                candidates.add("Bytes 5-6 decisgrams" to (weightPos5 / 10.0f))
            }
            
            // Try bytes 0-1 (0x08 0x07) 
            val weightPos0 = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            if (weightPos0 > 0 && weightPos0 < 5000) {
                candidates.add("Bytes 0-1 little-endian" to weightPos0.toFloat())
            }
            
            // Try bytes 1-2 (0x07 0x03)
            val weightPos1 = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            if (weightPos1 > 0 && weightPos1 < 5000) {
                candidates.add("Bytes 1-2 little-endian" to weightPos1.toFloat())
            }
        }
        
        // Try standard 16-bit little-endian from start
        if (data.size >= 2) {
            val weightLE = parseLittleEndian16Bit(data)
            if (weightLE > 0 && weightLE < 50000) {
                candidates.add("Little-endian 16-bit" to weightLE)
            }
        }
        
        // Try 16-bit big-endian from start
        if (data.size >= 2) {
            val weightBE = parseBigEndian16Bit(data)
            if (weightBE > 0 && weightBE < 50000) {
                candidates.add("Big-endian 16-bit" to weightBE)
            }
        }
        
        // Try ASCII string
        val weightASCII = parseAsciiString(data)
        if (weightASCII > 0 && weightASCII < 50000) {
            candidates.add("ASCII string" to weightASCII)
        }
        
        // Try 32-bit float
        if (data.size >= 4) {
            val weightFloat = parse32BitFloat(data)
            if (weightFloat > 0 && weightFloat < 50000) {
                candidates.add("32-bit float" to weightFloat)
            }
        }
        
        Log.d(TAG, "FFE0 parsing candidates: ${candidates.joinToString(", ") { "${it.first}: ${it.second}g" }}")
        
        // Return the most reasonable candidate (prefer smaller, realistic values)
        return candidates.filter { it.second < 2000 }.minByOrNull { it.second }?.second 
            ?: candidates.firstOrNull()?.second 
            ?: 0f
    }
    
    private fun parseLittleEndian16Bit(data: ByteArray): Float {
        if (data.size < 2) return 0f
        
        val weight = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        return weight.toFloat()
    }
    
    private fun parseBigEndian16Bit(data: ByteArray): Float {
        if (data.size < 2) return 0f
        
        val weight = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        return weight.toFloat()
    }
    
    private fun parse32BitFloat(data: ByteArray): Float {
        if (data.size < 4) return 0f
        
        val buffer = ByteBuffer.wrap(data, 0, 4)
        buffer.order(ByteOrder.LITTLE_ENDIAN) // Try little-endian first
        val weightLE = buffer.float
        
        // If result is reasonable, return it
        if (weightLE > 0 && weightLE < 50000) {
            return weightLE
        }
        
        // Try big-endian
        buffer.rewind()
        buffer.order(ByteOrder.BIG_ENDIAN)
        val weightBE = buffer.float
        
        return if (weightBE > 0 && weightBE < 50000) weightBE else 0f
    }
    
    private fun parseAsciiString(data: ByteArray): Float {
        return try {
            val str = String(data, StandardCharsets.UTF_8).trim()
            Log.d(TAG, "Attempting to parse ASCII string: '$str'")
            
            // Look for numeric values in the string
            val numberRegex = """(\d+\.?\d*)""".toRegex()
            val match = numberRegex.find(str)
            
            match?.value?.toFloat() ?: 0f
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse ASCII string: ${e.message}")
            0f
        }
    }
    
    private fun parseFFE0Bytes5_6LittleEndian(data: ByteArray): Float {
        // Specific parser for user's scale: weight data in bytes 5-6 little-endian
        // Data format: XX XX XX XX XX WW WW where WW WW is weight in grams
        // Scale capacity: 10kg = 10000g, so we need full 16-bit range
        
        if (data.size < 7) {
            Log.w(TAG, "Insufficient data for FFE0 bytes 5-6 parsing (need 7 bytes, got ${data.size})")
            return 0f
        }
        
        // Extract weight from bytes 5-6 (little-endian) - full 16-bit range
        val byte5 = data[5].toInt() and 0xFF  // Low byte
        val byte6 = data[6].toInt() and 0xFF  // High byte
        val weightRaw = byte5 or (byte6 shl 8) // Combine as 16-bit little-endian
        
        // Test different interpretations of the raw value
        val weightGrams = weightRaw.toFloat()                    // Direct grams
        val weightDeciGrams = weightRaw.toFloat() / 10.0f       // Decigrams (0.1g units)
        val weightCentiGrams = weightRaw.toFloat() / 100.0f     // Centigrams (0.01g units)
        val weightOunces = weightRaw.toFloat() * 0.035274f      // Ounces to grams
        val weightMilliOunces = weightRaw.toFloat() * 0.000035274f // Milliounces to grams
        
        Log.i(TAG, "=== BYTES 5-6 ANALYSIS ===")
        Log.i(TAG, "Raw bytes: 5=0x${byte5.toString(16).padStart(2, '0')} (${byte5}), 6=0x${byte6.toString(16).padStart(2, '0')} (${byte6})")
        Log.i(TAG, "Combined raw: ${weightRaw}")
        Log.i(TAG, "Interpretations:")
        Log.i(TAG, "  Direct grams:     ${weightGrams}g")
        Log.i(TAG, "  Decigrams ÷10:    ${weightDeciGrams}g")
        Log.i(TAG, "  Centigrams ÷100:  ${weightCentiGrams}g")
        Log.i(TAG, "  Ounces to grams:  ${weightOunces}g")
        Log.i(TAG, "  Milliounces:      ${weightMilliOunces}g")
        
        // Check for potential overflow/precision issues
        if (byte6 > 0) {
            Log.i(TAG, "  High byte non-zero: potential high weight or different encoding")
        }
        
        Log.i(TAG, "========================")
        
        // For now, return direct grams but log all possibilities
        val weight = weightGrams
        
        // Expand the range significantly to capture any encoding
        return if (weight >= 0 && weight <= 65535) weight else {
            Log.w(TAG, "Weight ${weight}g outside expected range (0-65535g), returning 0")
            0f
        }
    }
}