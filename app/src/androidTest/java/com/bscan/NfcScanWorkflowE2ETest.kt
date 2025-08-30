package com.bscan

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Parcel
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bscan.utils.RfidTestDataLoader
import com.bscan.model.NfcTagData
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end test for complete NFC scan workflow:
 * Stubbed NFC Intent → Decryption → Parsing → Component Creation → UI Display
 * 
 * Uses real RFID tag dump data from test-data/rfid-library/ submodule
 * to verify the complete scan workflow displays parsed data correctly.
 */
@RunWith(AndroidJUnit4::class)
class NfcScanWorkflowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var testActivity: MainActivity

    @Before
    fun setUp() {
        testActivity = composeTestRule.activity
        composeTestRule.waitForIdle()
        
        // Set system property for test data path
        System.setProperty("test.data.path", 
            "${InstrumentationRegistry.getInstrumentation().targetContext.externalCacheDir?.parent}/test-data/rfid-library")
    }

    @Test
    fun testCompletePLABasicScanWorkflow() = runBlocking {
        // Load real PLA Basic Black tag data
        val testFile = findFirstPLABasicTag()
        val tagData = loadRealTagData(testFile)
        
        // Create stubbed NFC intent with real tag data
        val nfcIntent = createStubbedNfcIntent(tagData)
        
        // Inject intent to trigger scan workflow
        testActivity.onNewIntent(nfcIntent)
        composeTestRule.waitForIdle()
        
        // Verify scanning progress UI appears
        composeTestRule.onNodeWithText("Tag Detected!")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Reading tag data...")
            .assertIsDisplayed()
        
        // Wait for processing state
        delay(1000)
        composeTestRule.waitForIdle()
        
        // Verify processing state
        composeTestRule.onNodeWithText("Scanning...")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Reading sector data from tag")
            .assertIsDisplayed()
        
        // Wait for processing to complete
        delay(4000)
        composeTestRule.waitForIdle()
        
        // Verify scan completed successfully (should show results)
        // Look for material type information in UI
        composeTestRule.onNodeWithText("PLA", substring = true)
            .assertExists()
        
        // Verify temperature information is displayed
        composeTestRule.onNodeWithText("°C", substring = true)
            .assertExists()
        
        // Verify tray UID or component ID appears
        composeTestRule.onRoot().printToLog("UI_TREE_AFTER_SCAN")
        
        // Look for any indication that parsing succeeded
        // Could be material name, temperature, or component details
        val hasValidScanResult = try {
            composeTestRule.onNodeWithText("Basic", substring = true).assertExists()
            true
        } catch (e: AssertionError) {
            try {
                composeTestRule.onNodeWithText("Black", substring = true).assertExists()
                true
            } catch (e2: AssertionError) {
                false
            }
        }
        
        assert(hasValidScanResult) { "No valid scan result found in UI" }
    }

    @Test
    fun testPETGCarbonFiberScanWorkflow() = runBlocking {
        // Load real PETG CF tag data
        val testFile = findFirstPETGCFTag()
        val tagData = loadRealTagData(testFile)
        
        // Create stubbed NFC intent
        val nfcIntent = createStubbedNfcIntent(tagData)
        
        // Inject intent
        testActivity.onNewIntent(nfcIntent)
        composeTestRule.waitForIdle()
        
        // Wait for processing
        delay(3000)
        composeTestRule.waitForIdle()
        
        // Verify PETG CF specific data
        composeTestRule.onNodeWithText("PETG", substring = true)
            .assertExists()
        
        composeTestRule.onNodeWithText("CF", substring = true)
            .assertExists()
            
        // PETG typically prints at higher temperatures
        composeTestRule.onNodeWithText("2", substring = true) // Temperature likely starts with 2 (260°C+)
            .assertExists()
    }

    @Test
    fun testSupportMaterialScanWorkflow() = runBlocking {
        // Load real Support Material tag data
        val testFile = findFirstSupportMaterialTag()
        val tagData = loadRealTagData(testFile)
        
        // Create stubbed NFC intent
        val nfcIntent = createStubbedNfcIntent(tagData)
        
        // Inject intent
        testActivity.onNewIntent(nfcIntent)
        composeTestRule.waitForIdle()
        
        // Wait for processing
        delay(3000)
        composeTestRule.waitForIdle()
        
        // Verify support material identification
        composeTestRule.onNodeWithText("Support", substring = true)
            .assertExists()
    }

    @Test
    fun testCorruptedTagErrorHandling() = runBlocking {
        // Create intentionally corrupted tag data
        val corruptedTagData = NfcTagData(
            uid = "BADBEEF",
            bytes = ByteArray(10) { 0xFF.toByte() }, // Too small and filled with 0xFF
            technology = "MifareClassic"
        )
        
        // Create corrupted tag intent
        val nfcIntent = NfcTestHelper.createCorruptedTagIntent()
        
        // Inject intent
        testActivity.onNewIntent(nfcIntent)
        composeTestRule.waitForIdle()
        
        // Wait for processing
        delay(2000)
        composeTestRule.waitForIdle()
        
        // Verify error handling
        composeTestRule.onNodeWithText("Error", substring = true)
            .assertExists()
    }

    @Test
    fun testAuthenticationFailureHandling() = runBlocking {
        // Load real tag data but simulate authentication failure
        val testFile = findFirstPLABasicTag()
        val realTagData = loadRealTagData(testFile)
        
        // Create tag data that simulates authentication failure (empty bytes but valid UID)
        val authFailedTagData = NfcTagData(
            uid = realTagData.uid,
            bytes = ByteArray(0), // Empty bytes simulate auth failure
            technology = realTagData.technology
        )
        
        // Create authentication failure intent
        val nfcIntent = NfcTestHelper.createAuthFailureIntent(realTagData.uid)
        
        // Inject intent
        testActivity.onNewIntent(nfcIntent)
        composeTestRule.waitForIdle()
        
        // Wait for processing
        delay(2000)
        composeTestRule.waitForIdle()
        
        // Verify authentication failure is handled gracefully
        composeTestRule.onNodeWithText("Authentication", substring = true)
            .assertExists()
    }

    @Test
    fun testMultipleSequentialScans() = runBlocking {
        // Test scanning multiple different tags in sequence
        val testFiles = listOf(
            findFirstPLABasicTag(),
            findFirstPETGCFTag() ?: findFirstPLABasicTag(), // Fallback if PETG CF not available
            findFirstSupportMaterialTag() ?: findFirstPLABasicTag() // Fallback if Support not available
        )
        
        testFiles.forEach { testFile ->
            val tagData = loadRealTagData(testFile)
            val nfcIntent = createStubbedNfcIntent(tagData)
            
            // Clear previous state
            composeTestRule.waitForIdle()
            
            // Inject new intent
            testActivity.onNewIntent(nfcIntent)
            composeTestRule.waitForIdle()
            
            // Wait for processing
            delay(3000)
            composeTestRule.waitForIdle()
            
            // Verify each scan is processed
            composeTestRule.onNodeWithText("°C", substring = true)
                .assertExists()
        }
    }

    // Helper methods for creating stubbed NFC intents

    private fun createStubbedNfcIntent(tagData: NfcTagData): Intent {
        return NfcTestHelper.createStubbedNfcIntent(tagData)
    }

    // Helper methods for loading real tag data

    private fun loadRealTagData(testFile: File): NfcTagData {
        val dumpData = RfidTestDataLoader.parseDumpFile(testFile)
        val tagBytes = RfidTestDataLoader.convertDumpToByteArray(dumpData)
        
        return NfcTagData(
            uid = dumpData.Card.UID,
            bytes = tagBytes,
            technology = "MifareClassic"
        )
    }

    private fun findFirstPLABasicTag(): File {
        return RfidTestDataLoader.findFirstTagOfType("PLA", "PLA Basic")
            ?: throw IllegalStateException("No PLA Basic test data available. Run: git submodule update --init --recursive")
    }

    private fun findFirstPETGCFTag(): File? {
        return RfidTestDataLoader.findFirstTagOfType("PETG", "PETG CF")
    }

    private fun findFirstSupportMaterialTag(): File? {
        return RfidTestDataLoader.findFirstTagOfType("Support Material", "Support for PLA-PETG")
    }
}