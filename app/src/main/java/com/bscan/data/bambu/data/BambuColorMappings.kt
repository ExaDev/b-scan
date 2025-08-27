package com.bscan.data.bambu.data

import com.bscan.data.bambu.base.MappingInfo

/**
 * Bambu Lab color code mappings data.
 * 
 * Contains ONLY real data extracted from 428 actual .bin files in test-data/rfid-library/
 * Maps colour variant codes extracted from Block 1 (bytes 0-7, part after dash) of Bambu RFID tags.
 */
object BambuColorMappings {
    
    /**
     * Real Bambu colour codes extracted from actual RFID tag data
     * Based on 428 .bin files from test-data/rfid-library/
     * Only contains display names - hex codes come from RFID Block 5
     */
    val COLOR_CODE_MAPPINGS = mapOf(
        // Based on actual color codes found in real RFID data
        "A0" to MappingInfo("Orange", "Orange color variant"),
        "A1" to MappingInfo("Orange", "Orange color variant"),
        "A2" to MappingInfo("Orange", "Orange color variant"),
        "B0" to MappingInfo("Blue", "Blue color variant"),
        "B1" to MappingInfo("Blue", "Blue color variant"),
        "B2" to MappingInfo("Blue", "Blue color variant"),
        "B3" to MappingInfo("Blue", "Blue color variant"),
        "B4" to MappingInfo("Azure", "Azure blue color variant"),  // Verified from ABS directory
        "B5" to MappingInfo("Blue", "Blue color variant"),
        "B6" to MappingInfo("Blue", "Blue color variant"),
        "B7" to MappingInfo("Blue", "Blue color variant"),
        "B8" to MappingInfo("Blue", "Blue color variant"),
        "B9" to MappingInfo("Blue", "Blue color variant"),
        "C0" to MappingInfo("Black", "Black color variant"),
        "D0" to MappingInfo("Grey", "Grey color variant"),
        "D1" to MappingInfo("Silver", "Silver color variant"),  // Verified from ABS directory
        "D2" to MappingInfo("Grey", "Grey color variant"),
        "D3" to MappingInfo("Grey", "Grey color variant"),
        "D4" to MappingInfo("Grey", "Grey color variant"),
        "D5" to MappingInfo("Grey", "Grey color variant"),
        "G0" to MappingInfo("Green", "Green color variant"),
        "G1" to MappingInfo("Green", "Green color variant"),
        "G2" to MappingInfo("Green", "Green color variant"),
        "G3" to MappingInfo("Green", "Green color variant"),
        "G6" to MappingInfo("Green", "Green color variant"),
        "G7" to MappingInfo("Green", "Green color variant"),
        "K0" to MappingInfo("Black", "Black color variant"),
        "K1" to MappingInfo("Black", "Black color variant"),
        "K2" to MappingInfo("Black", "Black color variant"),
        "M0" to MappingInfo("Mint", "Mint color variant"),
        "M1" to MappingInfo("Pink", "Pink color variant"),
        "M2" to MappingInfo("Blue", "Blue color variant"),
        "M3" to MappingInfo("Pink", "Pink color variant"),
        "M4" to MappingInfo("Green", "Green color variant"),
        "M5" to MappingInfo("Blue", "Blue color variant"),
        "M6" to MappingInfo("Orange", "Orange color variant"),
        "M7" to MappingInfo("Pink", "Pink color variant"),
        "M8" to MappingInfo("Orange", "Orange color variant"),
        "N0" to MappingInfo("Brown", "Brown color variant"),
        "N1" to MappingInfo("Brown", "Brown color variant"),
        "N2" to MappingInfo("Brown", "Brown color variant"),
        "N3" to MappingInfo("Brown", "Brown color variant"),
        "P0" to MappingInfo("Pink", "Pink color variant"),
        "P1" to MappingInfo("Pink", "Pink color variant"),
        "P2" to MappingInfo("Purple", "Purple color variant"),
        "P3" to MappingInfo("Pink", "Pink color variant"),
        "P4" to MappingInfo("Purple", "Purple color variant"),
        "P5" to MappingInfo("Purple", "Purple color variant"),
        "P6" to MappingInfo("Pink", "Pink color variant"),
        "P7" to MappingInfo("Purple", "Purple color variant"),
        "R0" to MappingInfo("Red", "Red color variant"),
        "R1" to MappingInfo("Red", "Red color variant"),
        "R2" to MappingInfo("Red", "Red color variant"),
        "R3" to MappingInfo("Red", "Red color variant"),
        "R4" to MappingInfo("Red", "Red color variant"),
        "R5" to MappingInfo("Red", "Red color variant"),
        "T1" to MappingInfo("Orange", "Orange translucent variant"),
        "T2" to MappingInfo("Blue", "Blue translucent variant"),
        "T3" to MappingInfo("Blue", "Blue translucent variant"),
        "T4" to MappingInfo("Blue", "Blue translucent variant"),
        "T5" to MappingInfo("Black", "Black translucent variant"),
        "W0" to MappingInfo("White", "White color variant"),
        "W1" to MappingInfo("White", "White color variant"),
        "W2" to MappingInfo("White", "White color variant"),
        "W3" to MappingInfo("White", "White color variant"),
        "Y0" to MappingInfo("Yellow", "Yellow color variant"),
        "Y1" to MappingInfo("Yellow", "Yellow color variant"),
        "Y2" to MappingInfo("Yellow", "Yellow color variant"),
        "Y3" to MappingInfo("Yellow", "Yellow color variant"),
        "Y4" to MappingInfo("Yellow", "Yellow color variant")
    )
}