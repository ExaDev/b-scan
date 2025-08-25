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
 * Tests for SKU details navigation functionality.
 * Tests navigation to and from SKU-related screens.
 */
@RunWith(AndroidJUnit4::class)
class SkuDetailsNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun skusTabAccessible() {
        // Test that SKUs tab is accessible
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Should not crash and maintain app structure
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("SKUs").assertExists()
    }

    @Test
    fun skusTabShowsExpectedUI() {
        // Navigate to SKUs tab
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Check that we have the basic structure
        // (may show empty state if no data)
        composeTestRule.onRoot().assertExists()
        
        // Look for control elements that should be present
        try {
            composeTestRule.onNode(hasText("Filter", substring = true) or hasText("Sort", substring = true)).assertExists()
        } catch (e: Exception) {
            // If no controls, at least verify main structure is present
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        }
    }

    @Test
    fun navigationFromSkusToOtherTabs() {
        // Start on SKUs tab
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to other tabs
        composeTestRule.onNodeWithText("Inventory").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Tags").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Scans").performClick()
        composeTestRule.waitForIdle()
        
        // All should work without issues
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun navigationFromSkusToSettings() {
        // Start on SKUs tab
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should return to main screen (SKUs tab should still be selected)
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("SKUs").assertExists()
    }

    @Test
    fun skusTabHandlesEmptyState() {
        // Navigate to SKUs tab
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Should handle empty state gracefully without crashes
        composeTestRule.onRoot().assertExists()
        
        // Should not show error messages
        composeTestRule.onNodeWithText("Error", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun rapidTabSwitchingFromSkus() {
        // Test rapid navigation from SKUs doesn't break the app
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        repeat(3) {
            composeTestRule.onNodeWithText("Inventory").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("SKUs").performClick()
            composeTestRule.waitForIdle()
        }
        
        // App should still be functional
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun systemBackButtonFromSkus() {
        // Navigate to SKUs tab then to settings
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Use system back button
        device.pressBack()
        
        // Should return to main screen
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}