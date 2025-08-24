package com.bscan.data

import com.bscan.model.BambuProduct
import org.junit.Test
import org.junit.Assert.*

class BambuProductDatabaseTest {
    
    @Test
    fun `findProduct returns correct product for known RFID code and color`() {
        // Given
        val internalCode = "GFL00"
        val colorHex = "#FFFFFF"
        
        // When
        val product = BambuProductDatabase.findProduct(internalCode, colorHex)
        
        // Then
        assertNotNull("Product should be found", product)
        assertEquals("PLA Basic", product?.productLine)
        assertEquals("Jade White", product?.colorName)
        assertEquals("#FFFFFF", product?.colorHex)
        assertEquals("1kg", product?.mass)
        assertNotNull("Spool URL should be available", product?.spoolUrl)
        assertNotNull("Refill URL should be available", product?.refillUrl)
    }
    
    @Test
    fun `findProduct returns null for unknown RFID code`() {
        // Given
        val internalCode = "UNKNOWN"
        val colorHex = "#FFFFFF"
        
        // When
        val product = BambuProductDatabase.findProduct(internalCode, colorHex)
        
        // Then
        assertNull("Product should not be found for unknown code", product)
    }
    
    @Test
    fun `findProduct returns null for unknown color`() {
        // Given
        val internalCode = "GFL00"
        val colorHex = "#999999" // Unknown color
        
        // When
        val product = BambuProductDatabase.findProduct(internalCode, colorHex)
        
        // Then
        assertNull("Product should not be found for unknown color", product)
    }
    
    @Test
    fun `support material has correct properties`() {
        // Given
        val internalCode = "GFS00"
        val colorHex = "#F0F8FF"
        
        // When
        val product = BambuProductDatabase.findProduct(internalCode, colorHex)
        
        // Then
        assertNotNull("Support material should be found", product)
        assertEquals("Support for PLA/PETG", product?.productLine)
        assertEquals("Nature", product?.colorName)
        assertEquals("0.5kg", product?.mass)
        assertNotNull("Spool URL should be available", product?.spoolUrl)
        assertNull("Refill URL should not be available for support materials", product?.refillUrl)
    }
    
    @Test
    fun `silk material has spool only availability`() {
        // Given
        val internalCode = "GFL02"
        val colorHex = "#F4A925"
        
        // When
        val product = BambuProductDatabase.findProduct(internalCode, colorHex)
        
        // Then
        assertNotNull("Silk material should be found", product)
        assertEquals("PLA Silk+", product?.productLine)
        assertEquals("Gold", product?.colorName)
        assertNotNull("Spool URL should be available", product?.spoolUrl)
        assertNull("Refill URL should not be available for silk materials", product?.refillUrl)
    }
    
    @Test
    fun `getProductCount returns expected number`() {
        // When
        val count = BambuProductDatabase.getProductCount()
        
        // Then
        assertTrue("Should have products in database", count > 0)
        // Note: Exact count will change as we add more products
    }
    
    @Test
    fun `getAllProductLines returns expected lines`() {
        // When
        val productLines = BambuProductDatabase.getAllProductLines()
        
        // Then
        assertTrue("Should contain PLA Basic", productLines.contains("PLA Basic"))
        assertTrue("Should contain ABS", productLines.contains("ABS"))
        assertTrue("Should contain PLA Silk+", productLines.contains("PLA Silk+"))
        assertTrue("Should contain Support for PLA/PETG", productLines.contains("Support for PLA/PETG"))
    }
    
    @Test
    fun `hasProduct works correctly`() {
        // Test existing product
        assertTrue("Should have known product", 
            BambuProductDatabase.hasProduct("GFL00", "#FFFFFF"))
        
        // Test non-existing product
        assertFalse("Should not have unknown product", 
            BambuProductDatabase.hasProduct("UNKNOWN", "#FFFFFF"))
    }
}