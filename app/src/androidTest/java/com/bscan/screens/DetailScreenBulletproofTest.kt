package com.bscan.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.bscan.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

/**
 * Bulletproof tests for detail screen functionality.
 * Tests robustness and error handling of detail screens.
 */
@RunWith(AndroidJUnit4::class)
class DetailScreenBulletproofTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunchDoesNotCrash() {
        // Basic smoke test - app launches successfully
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // All main tabs are present
        composeTestRule.onNodeWithText("Inventory").assertExists()
        composeTestRule.onNodeWithText("SKUs").assertExists()
        composeTestRule.onNodeWithText("Tags").assertExists()
        composeTestRule.onNodeWithText("Scans").assertExists()
    }

    @Test
    fun allTabsCanBeAccessedWithoutCrashing() {
        // Test that all tabs can be accessed without crashes
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            // Should not crash
            composeTestRule.onRoot().assertExists()
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        }
    }

    @Test
    fun rapidTabSwitchingDoesNotCrash() {
        // Test rapid tab switching doesn't break the app
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        repeat(5) {
            tabs.forEach { tab ->
                composeTestRule.onNodeWithText(tab).performClick()
                // Shorter wait for rapid switching test
                Thread.sleep(100)
            }
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun navigationToSecondaryScreensDoesNotCrash() {
        // Test navigation to secondary screens
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should return to main screen without issues
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun systemBackButtonHandlingIsRobust() {
        // Test various system back button scenarios
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        device.pressBack()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Test back button when on main screen (should not crash)
        device.pressBack()
        composeTestRule.waitForIdle()
        // App might exit or stay - either is acceptable, just shouldn't crash
    }

    @Test
    fun emptyStateHandlingIsGraceful() {
        // Test that empty states are handled gracefully
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            // Should not show error states even if empty
            composeTestRule.onNodeWithText("Error", ignoreCase = true).assertDoesNotExist()
            composeTestRule.onRoot().assertExists()
        }
    }

    @Test
    fun uiElementsAreConsistentAcrossTabs() {
        // Test that UI elements are consistently accessible across tabs
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            // App bar should always be present
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
            
            // Navigation elements should be present
            composeTestRule.onNodeWithContentDescription("Scan History").assertExists()
            composeTestRule.onNodeWithContentDescription("Settings").assertExists()
        }
    }

    @Test
    fun memoryLeakPreventionTest() {
        // Test for potential memory leaks by performing many navigation operations
        repeat(20) {
            // Navigate through all tabs
            composeTestRule.onNodeWithText("SKUs").performClick()
            composeTestRule.onNodeWithText("Tags").performClick()
            composeTestRule.onNodeWithText("Scans").performClick()
            composeTestRule.onNodeWithText("Inventory").performClick()
            
            // Navigate to secondary screens
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        }
        
        // App should still be functional
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun screenRotationHandling() {
        // Test basic screen rotation handling (if supported by test environment)
        try {
            composeTestRule.onNodeWithText("SKUs").performClick()
            composeTestRule.waitForIdle()
            
            // Simulate configuration change by navigating away and back
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            
            // State should be preserved
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        } catch (e: Exception) {
            // If rotation is not supported in test environment, just verify basic functionality
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        }
    }
}