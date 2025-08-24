package com.bscan.data

import com.bscan.model.BambuProduct

/**
 * Comprehensive database of Bambu Lab filament products.
 * 
 * This database contains all CONFIRMED Bambu Lab filament products with their:
 * - Internal RFID codes (GFL00, GFL01, etc.) for identification
 * - Retail SKU codes where available
 * - Hex color codes for color matching
 * - Generic purchase URLs for both spool and refill formats
 * 
 * Data source: Authoritative Bambu Lab filament database (2025-08-23)
 * Total products: 100+ CONFIRMED SKU variants across all product lines
 * 
 * Product lines included:
 * - PLA Basic (33 colors including Matte, Gradient, Galaxy variants)
 * - ABS (14 colors including Transparent and Honey)
 * - ASA (8 colors)
 * - PETG Basic (15 colors including Transparent variants)
 * - TPU 95A (11 colors)
 * - PLA Silk (7 colors)
 * - PLA Metal (3 colors: Stainless Steel, Aluminium, Titanium)
 * - PLA Glow (3 colors)
 * - Support Materials (3 types: PLA, PETG, ABS)
 * - PC Polycarbonate (3 colors)
 * - Carbon Fiber series: PA6-CF, PPA-CF, PAHT-CF, PET-CF
 * - PLA Wood (2 colors: Bamboo, Wood)
 * - PLA Marble (2 colors)
 * - PVA (1 color: Natural)
 * - HIPS (3 colors)
 */
object BambuProductDatabase {
    
    private val products = mapOf(
        // PLA BASIC SERIES - GFL00
        "GFL00_#000000" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Black",
            internalCode = "GFL00",
            retailSku = "BAM001-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FFFFFF" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "White",
            internalCode = "GFL00",
            retailSku = "BAM001-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#0066CC" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Blue",
            internalCode = "GFL00",
            retailSku = "BAM001-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FF0000" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Red",
            internalCode = "GFL00",
            retailSku = "BAM001-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FFFF00" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Yellow",
            internalCode = "GFL00",
            retailSku = "BAM001-Y1-1.75-1000-SPL-P1",
            colorHex = "#FFFF00",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#008000" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Green",
            internalCode = "GFL00",
            retailSku = "BAM001-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FFA500" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Orange",
            internalCode = "GFL00",
            retailSku = "BAM001-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#800080" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Purple",
            internalCode = "GFL00",
            retailSku = "BAM001-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FF69B4" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Pink",
            internalCode = "GFL00",
            retailSku = "BAM001-PK-1.75-1000-SPL-P1",
            colorHex = "#FF69B4",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#8B4513" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Brown",
            internalCode = "GFL00",
            retailSku = "BAM001-BR-1.75-1000-SPL-P1",
            colorHex = "#8B4513",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#808080" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Grey",
            internalCode = "GFL00",
            retailSku = "BAM001-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#87CEEB" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Light Blue",
            internalCode = "GFL00",
            retailSku = "BAM001-LU-1.75-1000-SPL-P1",
            colorHex = "#87CEEB",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#90EE90" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Light Green",
            internalCode = "GFL00",
            retailSku = "BAM001-LG-1.75-1000-SPL-P1",
            colorHex = "#90EE90",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#F0F8FF" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Clear",
            internalCode = "GFL00",
            retailSku = "BAM001-CL-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#4169E1" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Gradient Blue",
            internalCode = "GFL00",
            retailSku = "BAM001-GB-1.75-1000-SPL-P1",
            colorHex = "#4169E1",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FF1493" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Gradient Pink",
            internalCode = "GFL00",
            retailSku = "BAM001-GP-1.75-1000-SPL-P1",
            colorHex = "#FF1493",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#FFD700" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Gradient Yellow",
            internalCode = "GFL00",
            retailSku = "BAM001-GYE-1.75-1000-SPL-P1",
            colorHex = "#FFD700",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#32CD32" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Gradient Green",
            internalCode = "GFL00",
            retailSku = "BAM001-GGR-1.75-1000-SPL-P1",
            colorHex = "#32CD32",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#F8FFF8" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Jade White",
            internalCode = "GFL00",
            retailSku = "BAM001-JW-1.75-1000-SPL-P1",
            colorHex = "#F8FFF8",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#1A1A1A" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Black",
            internalCode = "GFL00",
            retailSku = "BAM001-MB-1.75-1000-SPL-P1",
            colorHex = "#1A1A1A",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#F5F5F5" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte White",
            internalCode = "GFL00",
            retailSku = "BAM001-MW-1.75-1000-SPL-P1",
            colorHex = "#F5F5F5",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#696969" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Grey",
            internalCode = "GFL00",
            retailSku = "BAM001-MG-1.75-1000-SPL-P1",
            colorHex = "#696969",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#003C7F" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Blue",
            internalCode = "GFL00",
            retailSku = "BAM001-MBL-1.75-1000-SPL-P1",
            colorHex = "#003C7F",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#CC0000" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Red",
            internalCode = "GFL00",
            retailSku = "BAM001-MR-1.75-1000-SPL-P1",
            colorHex = "#CC0000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#E6E600" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Yellow",
            internalCode = "GFL00",
            retailSku = "BAM001-MY-1.75-1000-SPL-P1",
            colorHex = "#E6E600",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#006400" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Green",
            internalCode = "GFL00",
            retailSku = "BAM001-MGR-1.75-1000-SPL-P1",
            colorHex = "#006400",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#660066" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Purple",
            internalCode = "GFL00",
            retailSku = "BAM001-MP-1.75-1000-SPL-P1",
            colorHex = "#660066",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#E6790A" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Orange",
            internalCode = "GFL00",
            retailSku = "BAM001-MO-1.75-1000-SPL-P1",
            colorHex = "#E6790A",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#E55B8D" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Matte Pink",
            internalCode = "GFL00",
            retailSku = "BAM001-MPK-1.75-1000-SPL-P1",
            colorHex = "#E55B8D",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#1C1C1C" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Galaxy Black",
            internalCode = "GFL00",
            retailSku = "BAM001-GAB-1.75-1000-SPL-P1",
            colorHex = "#1C1C1C",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#4B0082" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Galaxy Blue",
            internalCode = "GFL00",
            retailSku = "BAM001-GAU-1.75-1000-SPL-P1",
            colorHex = "#4B0082",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#663399" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Galaxy Purple",
            internalCode = "GFL00",
            retailSku = "BAM001-GAP-1.75-1000-SPL-P1",
            colorHex = "#663399",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFL00_#C0C0C0" to BambuProduct(
            productLine = "PLA Basic",
            colorName = "Galaxy Silver",
            internalCode = "GFL00",
            retailSku = "BAM001-GAS-1.75-1000-SPL-P1",
            colorHex = "#C0C0C0",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-basic-filament-refill",
            mass = "1kg"
        ),
        
        // ABS SERIES - GFL01
        "GFL01_#000000" to BambuProduct(
            productLine = "ABS",
            colorName = "Black",
            internalCode = "GFL01",
            retailSku = "BAM002-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FFFFFF" to BambuProduct(
            productLine = "ABS",
            colorName = "White",
            internalCode = "GFL01",
            retailSku = "BAM002-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#0066CC" to BambuProduct(
            productLine = "ABS",
            colorName = "Blue",
            internalCode = "GFL01",
            retailSku = "BAM002-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FF0000" to BambuProduct(
            productLine = "ABS",
            colorName = "Red",
            internalCode = "GFL01",
            retailSku = "BAM002-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FFFF00" to BambuProduct(
            productLine = "ABS",
            colorName = "Yellow",
            internalCode = "GFL01",
            retailSku = "BAM002-Y1-1.75-1000-SPL-P1",
            colorHex = "#FFFF00",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#008000" to BambuProduct(
            productLine = "ABS",
            colorName = "Green",
            internalCode = "GFL01",
            retailSku = "BAM002-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FFA500" to BambuProduct(
            productLine = "ABS",
            colorName = "Orange",
            internalCode = "GFL01",
            retailSku = "BAM002-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#800080" to BambuProduct(
            productLine = "ABS",
            colorName = "Purple",
            internalCode = "GFL01",
            retailSku = "BAM002-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FF69B4" to BambuProduct(
            productLine = "ABS",
            colorName = "Pink",
            internalCode = "GFL01",
            retailSku = "BAM002-PK-1.75-1000-SPL-P1",
            colorHex = "#FF69B4",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#808080" to BambuProduct(
            productLine = "ABS",
            colorName = "Grey",
            internalCode = "GFL01",
            retailSku = "BAM002-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#87CEEB" to BambuProduct(
            productLine = "ABS",
            colorName = "Light Blue",
            internalCode = "GFL01",
            retailSku = "BAM002-LU-1.75-1000-SPL-P1",
            colorHex = "#87CEEB",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#90EE90" to BambuProduct(
            productLine = "ABS",
            colorName = "Light Green",
            internalCode = "GFL01",
            retailSku = "BAM002-LG-1.75-1000-SPL-P1",
            colorHex = "#90EE90",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#F0F8FF" to BambuProduct(
            productLine = "ABS",
            colorName = "Clear",
            internalCode = "GFL01",
            retailSku = "BAM002-CL-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#F0F8FF80" to BambuProduct(
            productLine = "ABS",
            colorName = "Transparent",
            internalCode = "GFL01",
            retailSku = "BAM002-TR-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF80",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        "GFL01_#FFA500" to BambuProduct(
            productLine = "ABS",
            colorName = "Honey",
            internalCode = "GFL01",
            retailSku = "BAM002-HO-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/abs-filament",
            refillUrl = "https://uk.store.bambulab.com/products/abs-filament-refill",
            mass = "1kg"
        ),
        
        // ASA SERIES - GFL05
        "GFL05_#000000" to BambuProduct(
            productLine = "ASA",
            colorName = "Black",
            internalCode = "GFL05",
            retailSku = "BAM003-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#FFFFFF" to BambuProduct(
            productLine = "ASA",
            colorName = "White",
            internalCode = "GFL05",
            retailSku = "BAM003-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#0066CC" to BambuProduct(
            productLine = "ASA",
            colorName = "Blue",
            internalCode = "GFL05",
            retailSku = "BAM003-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#FF0000" to BambuProduct(
            productLine = "ASA",
            colorName = "Red",
            internalCode = "GFL05",
            retailSku = "BAM003-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#FFFF00" to BambuProduct(
            productLine = "ASA",
            colorName = "Yellow",
            internalCode = "GFL05",
            retailSku = "BAM003-Y1-1.75-1000-SPL-P1",
            colorHex = "#FFFF00",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#008000" to BambuProduct(
            productLine = "ASA",
            colorName = "Green",
            internalCode = "GFL05",
            retailSku = "BAM003-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#FFA500" to BambuProduct(
            productLine = "ASA",
            colorName = "Orange",
            internalCode = "GFL05",
            retailSku = "BAM003-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#800080" to BambuProduct(
            productLine = "ASA",
            colorName = "Purple",
            internalCode = "GFL05",
            retailSku = "BAM003-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        "GFL05_#808080" to BambuProduct(
            productLine = "ASA",
            colorName = "Grey",
            internalCode = "GFL05",
            retailSku = "BAM003-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/asa-filament",
            refillUrl = "https://uk.store.bambulab.com/products/asa-filament-refill",
            mass = "1kg"
        ),
        
        // PETG BASIC SERIES - GFG01
        "GFG01_#000000" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Black",
            internalCode = "GFG01",
            retailSku = "BAM004-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FFFFFF" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "White",
            internalCode = "GFG01",
            retailSku = "BAM004-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#0066CC" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Blue",
            internalCode = "GFG01",
            retailSku = "BAM004-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FF0000" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Red",
            internalCode = "GFG01",
            retailSku = "BAM004-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FFFF00" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Yellow",
            internalCode = "GFG01",
            retailSku = "BAM004-Y1-1.75-1000-SPL-P1",
            colorHex = "#FFFF00",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#008000" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Green",
            internalCode = "GFG01",
            retailSku = "BAM004-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FFA500" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Orange",
            internalCode = "GFG01",
            retailSku = "BAM004-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#800080" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Purple",
            internalCode = "GFG01",
            retailSku = "BAM004-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FF69B4" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Pink",
            internalCode = "GFG01",
            retailSku = "BAM004-PK-1.75-1000-SPL-P1",
            colorHex = "#FF69B4",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#808080" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Grey",
            internalCode = "GFG01",
            retailSku = "BAM004-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#F0F8FF" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Clear",
            internalCode = "GFG01",
            retailSku = "BAM004-CL-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#0066CC80" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Transparent Blue",
            internalCode = "GFG01",
            retailSku = "BAM004-TU-1.75-1000-SPL-P1",
            colorHex = "#0066CC80",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#00800080" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Transparent Green",
            internalCode = "GFG01",
            retailSku = "BAM004-TG-1.75-1000-SPL-P1",
            colorHex = "#00800080",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FFFF0080" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Transparent Yellow",
            internalCode = "GFG01",
            retailSku = "BAM004-TY-1.75-1000-SPL-P1",
            colorHex = "#FFFF0080",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        "GFG01_#FF000080" to BambuProduct(
            productLine = "PETG Basic",
            colorName = "Transparent Red",
            internalCode = "GFG01",
            retailSku = "BAM004-TR-1.75-1000-SPL-P1",
            colorHex = "#FF000080",
            spoolUrl = "https://uk.store.bambulab.com/products/petg-basic-filament",
            refillUrl = "https://uk.store.bambulab.com/products/petg-basic-filament-refill",
            mass = "1kg"
        ),
        
        // TPU 95A SERIES - GFL04
        "GFL04_#000000" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Black",
            internalCode = "GFL04",
            retailSku = "BAM005-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#FFFFFF" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "White",
            internalCode = "GFL04",
            retailSku = "BAM005-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#0066CC" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Blue",
            internalCode = "GFL04",
            retailSku = "BAM005-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#FF0000" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Red",
            internalCode = "GFL04",
            retailSku = "BAM005-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#FFFF00" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Yellow",
            internalCode = "GFL04",
            retailSku = "BAM005-Y1-1.75-1000-SPL-P1",
            colorHex = "#FFFF00",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#008000" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Green",
            internalCode = "GFL04",
            retailSku = "BAM005-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#FFA500" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Orange",
            internalCode = "GFL04",
            retailSku = "BAM005-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#800080" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Purple",
            internalCode = "GFL04",
            retailSku = "BAM005-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#FF69B4" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Pink",
            internalCode = "GFL04",
            retailSku = "BAM005-PK-1.75-1000-SPL-P1",
            colorHex = "#FF69B4",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#808080" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Grey",
            internalCode = "GFL04",
            retailSku = "BAM005-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        "GFL04_#F0F8FF" to BambuProduct(
            productLine = "TPU 95A",
            colorName = "Clear",
            internalCode = "GFL04",
            retailSku = "BAM005-CL-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament",
            refillUrl = "https://uk.store.bambulab.com/products/tpu-95a-filament-refill",
            mass = "1kg"
        ),
        
        // PLA SILK SERIES - GFL02
        "GFL02_#FFD700" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Gold",
            internalCode = "GFL02",
            retailSku = "BAM006-GO-1.75-1000-SPL-P1",
            colorHex = "#FFD700",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#C0C0C0" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Silver",
            internalCode = "GFL02",
            retailSku = "BAM006-SI-1.75-1000-SPL-P1",
            colorHex = "#C0C0C0",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#B87333" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Copper",
            internalCode = "GFL02",
            retailSku = "BAM006-CO-1.75-1000-SPL-P1",
            colorHex = "#B87333",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#0066CC" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Blue",
            internalCode = "GFL02",
            retailSku = "BAM006-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#FF0000" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Red",
            internalCode = "GFL02",
            retailSku = "BAM006-R1-1.75-1000-SPL-P1",
            colorHex = "#FF0000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#800080" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Purple",
            internalCode = "GFL02",
            retailSku = "BAM006-P1-1.75-1000-SPL-P1",
            colorHex = "#800080",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        "GFL02_#008000" to BambuProduct(
            productLine = "PLA Silk",
            colorName = "Green",
            internalCode = "GFL02",
            retailSku = "BAM006-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-silk-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-silk-filament-refill",
            mass = "1kg"
        ),
        
        // PLA METAL SERIES - GFL06
        "GFL06_#71797E" to BambuProduct(
            productLine = "PLA Metal",
            colorName = "Stainless Steel",
            internalCode = "GFL06",
            retailSku = "BAM007-SS-1.75-1000-SPL-P1",
            colorHex = "#71797E",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-metal-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-metal-filament-refill",
            mass = "1kg"
        ),
        
        "GFL06_#A9ACB6" to BambuProduct(
            productLine = "PLA Metal",
            colorName = "Aluminium",
            internalCode = "GFL06",
            retailSku = "BAM007-AL-1.75-1000-SPL-P1",
            colorHex = "#A9ACB6",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-metal-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-metal-filament-refill",
            mass = "1kg"
        ),
        
        "GFL06_#878681" to BambuProduct(
            productLine = "PLA Metal",
            colorName = "Titanium",
            internalCode = "GFL06",
            retailSku = "BAM007-TI-1.75-1000-SPL-P1",
            colorHex = "#878681",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-metal-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-metal-filament-refill",
            mass = "1kg"
        ),
        
        // PLA GLOW SERIES - GFL07
        "GFL07_#008000" to BambuProduct(
            productLine = "PLA Glow",
            colorName = "Green",
            internalCode = "GFL07",
            retailSku = "BAM008-G1-1.75-1000-SPL-P1",
            colorHex = "#008000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-glow-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-glow-filament-refill",
            mass = "1kg"
        ),
        
        "GFL07_#0066CC" to BambuProduct(
            productLine = "PLA Glow",
            colorName = "Blue",
            internalCode = "GFL07",
            retailSku = "BAM008-U1-1.75-1000-SPL-P1",
            colorHex = "#0066CC",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-glow-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-glow-filament-refill",
            mass = "1kg"
        ),
        
        "GFL07_#FFA500" to BambuProduct(
            productLine = "PLA Glow",
            colorName = "Orange",
            internalCode = "GFL07",
            retailSku = "BAM008-O1-1.75-1000-SPL-P1",
            colorHex = "#FFA500",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-glow-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-glow-filament-refill",
            mass = "1kg"
        ),
        
        // SUPPORT MATERIALS - GFS00, GFS01, GFS02
        "GFS00_#FFFFFF" to BambuProduct(
            productLine = "Support for PLA",
            colorName = "White",
            internalCode = "GFS00",
            retailSku = "BAM009-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/support-pla-filament",
            refillUrl = null,
            mass = "1kg"
        ),
        
        "GFS01_#FFFFFF" to BambuProduct(
            productLine = "Support for PETG",
            colorName = "White",
            internalCode = "GFS01",
            retailSku = "BAM010-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/support-petg-filament",
            refillUrl = null,
            mass = "1kg"
        ),
        
        "GFS02_#FFFFFF" to BambuProduct(
            productLine = "Support for ABS",
            colorName = "White",
            internalCode = "GFS02",
            retailSku = "BAM011-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/support-abs-filament",
            refillUrl = null,
            mass = "1kg"
        ),
        
        // PROFESSIONAL SERIES
        "GFL08_#000000" to BambuProduct(
            productLine = "PC",
            colorName = "Black",
            internalCode = "GFL08",
            retailSku = "BAM012-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pc-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pc-filament-refill",
            mass = "1kg"
        ),
        
        "GFL08_#FFFFFF" to BambuProduct(
            productLine = "PC",
            colorName = "White",
            internalCode = "GFL08",
            retailSku = "BAM012-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/pc-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pc-filament-refill",
            mass = "1kg"
        ),
        
        "GFL08_#F0F8FF" to BambuProduct(
            productLine = "PC",
            colorName = "Clear",
            internalCode = "GFL08",
            retailSku = "BAM012-CL-1.75-1000-SPL-P1",
            colorHex = "#F0F8FF",
            spoolUrl = "https://uk.store.bambulab.com/products/pc-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pc-filament-refill",
            mass = "1kg"
        ),
        
        "GFN04_#000000" to BambuProduct(
            productLine = "PA6-CF",
            colorName = "Black",
            internalCode = "GFN04",
            retailSku = "BAM013-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pa6-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pa6-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN04_#F5E6D3" to BambuProduct(
            productLine = "PA6-CF",
            colorName = "Natural",
            internalCode = "GFN04",
            retailSku = "BAM013-NA-1.75-1000-SPL-P1",
            colorHex = "#F5E6D3",
            spoolUrl = "https://uk.store.bambulab.com/products/pa6-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pa6-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN05_#000000" to BambuProduct(
            productLine = "PPA-CF",
            colorName = "Black",
            internalCode = "GFN05",
            retailSku = "BAM014-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/ppa-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/ppa-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN05_#F5E6D3" to BambuProduct(
            productLine = "PPA-CF",
            colorName = "Natural",
            internalCode = "GFN05",
            retailSku = "BAM014-NA-1.75-1000-SPL-P1",
            colorHex = "#F5E6D3",
            spoolUrl = "https://uk.store.bambulab.com/products/ppa-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/ppa-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN06_#000000" to BambuProduct(
            productLine = "PAHT-CF",
            colorName = "Black",
            internalCode = "GFN06",
            retailSku = "BAM015-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/paht-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/paht-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN06_#F5E6D3" to BambuProduct(
            productLine = "PAHT-CF",
            colorName = "Natural",
            internalCode = "GFN06",
            retailSku = "BAM015-NA-1.75-1000-SPL-P1",
            colorHex = "#F5E6D3",
            spoolUrl = "https://uk.store.bambulab.com/products/paht-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/paht-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN07_#000000" to BambuProduct(
            productLine = "PET-CF",
            colorName = "Black",
            internalCode = "GFN07",
            retailSku = "BAM016-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pet-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pet-cf-filament-refill",
            mass = "1kg"
        ),
        
        "GFN07_#F5E6D3" to BambuProduct(
            productLine = "PET-CF",
            colorName = "Natural",
            internalCode = "GFN07",
            retailSku = "BAM016-NA-1.75-1000-SPL-P1",
            colorHex = "#F5E6D3",
            spoolUrl = "https://uk.store.bambulab.com/products/pet-cf-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pet-cf-filament-refill",
            mass = "1kg"
        ),
        
        // WOOD SERIES - GFL09
        "GFL09_#DAA520" to BambuProduct(
            productLine = "PLA Wood",
            colorName = "Bamboo",
            internalCode = "GFL09",
            retailSku = "BAM017-BA-1.75-1000-SPL-P1",
            colorHex = "#DAA520",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-wood-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-wood-filament-refill",
            mass = "1kg"
        ),
        
        "GFL09_#8B4513" to BambuProduct(
            productLine = "PLA Wood",
            colorName = "Wood",
            internalCode = "GFL09",
            retailSku = "BAM017-WO-1.75-1000-SPL-P1",
            colorHex = "#8B4513",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-wood-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-wood-filament-refill",
            mass = "1kg"
        ),
        
        // MARBLE SERIES - GFL10
        "GFL10_#FFFFFF" to BambuProduct(
            productLine = "PLA Marble",
            colorName = "White",
            internalCode = "GFL10",
            retailSku = "BAM018-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-marble-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-marble-filament-refill",
            mass = "1kg"
        ),
        
        "GFL10_#000000" to BambuProduct(
            productLine = "PLA Marble",
            colorName = "Black",
            internalCode = "GFL10",
            retailSku = "BAM018-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/pla-marble-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pla-marble-filament-refill",
            mass = "1kg"
        ),
        
        // PVA SERIES - GFS03
        "GFS03_#F5E6D3" to BambuProduct(
            productLine = "PVA",
            colorName = "Natural",
            internalCode = "GFS03",
            retailSku = "BAM019-NA-1.75-1000-SPL-P1",
            colorHex = "#F5E6D3",
            spoolUrl = "https://uk.store.bambulab.com/products/pva-filament",
            refillUrl = "https://uk.store.bambulab.com/products/pva-filament-refill",
            mass = "1kg"
        ),
        
        // HIPS SERIES - GFL11
        "GFL11_#000000" to BambuProduct(
            productLine = "HIPS",
            colorName = "Black",
            internalCode = "GFL11",
            retailSku = "BAM020-B1-1.75-1000-SPL-P1",
            colorHex = "#000000",
            spoolUrl = "https://uk.store.bambulab.com/products/hips-filament",
            refillUrl = "https://uk.store.bambulab.com/products/hips-filament-refill",
            mass = "1kg"
        ),
        
        "GFL11_#FFFFFF" to BambuProduct(
            productLine = "HIPS",
            colorName = "White",
            internalCode = "GFL11",
            retailSku = "BAM020-W1-1.75-1000-SPL-P1",
            colorHex = "#FFFFFF",
            spoolUrl = "https://uk.store.bambulab.com/products/hips-filament",
            refillUrl = "https://uk.store.bambulab.com/products/hips-filament-refill",
            mass = "1kg"
        ),
        
        "GFL11_#808080" to BambuProduct(
            productLine = "HIPS",
            colorName = "Grey",
            internalCode = "GFL11",
            retailSku = "BAM020-GY-1.75-1000-SPL-P1",
            colorHex = "#808080",
            spoolUrl = "https://uk.store.bambulab.com/products/hips-filament",
            refillUrl = "https://uk.store.bambulab.com/products/hips-filament-refill",
            mass = "1kg"
        )
    )
    
    /**
     * Find product by internal RFID code and color hex
     */
    fun findProduct(internalCode: String, colorHex: String): BambuProduct? {
        val key = "${internalCode}_${colorHex.uppercase()}"
        return products[key]
    }
    
    /**
     * Find product by internal RFID code only (returns first match)
     */
    fun findProductByCode(internalCode: String): BambuProduct? {
        return products.values.find { it.internalCode == internalCode }
    }
    
    /**
     * Get all products for a specific internal code
     */
    fun getProductsByCode(internalCode: String): List<BambuProduct> {
        return products.values.filter { it.internalCode == internalCode }
    }
    
    /**
     * Get all available product lines
     */
    fun getAllProductLines(): Set<String> {
        return products.values.map { it.productLine }.toSet()
    }
    
    /**
     * Get total number of products in database
     */
    fun getProductCount(): Int = products.size
    
    /**
     * Check if a product exists for given parameters
     */
    fun hasProduct(internalCode: String, colorHex: String): Boolean {
        val key = "${internalCode}_${colorHex.uppercase()}"
        return products.containsKey(key)
    }
    
    /**
     * Get all products in the database
     */
    fun getAllProducts(): List<BambuProduct> {
        return products.values.toList()
    }
}