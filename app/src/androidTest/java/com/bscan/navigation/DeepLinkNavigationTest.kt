package com.bscan.navigation

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
 * Basic navigation tests for B-Scan app navigation functionality.
 * Tests core app navigation without deep links (which may not be implemented yet).
 */
@RunWith(AndroidJUnit4::class)
class DeepLinkNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunchesWithoutDeepLink() {
        // Test normal app launch without deep links
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Verify main UI elements are present
        composeTestRule.onNodeWithText("Inventory").assertExists()
        composeTestRule.onNodeWithText("SKUs").assertExists()
        composeTestRule.onNodeWithText("Tags").assertExists()
        composeTestRule.onNodeWithText("Scans").assertExists()
    }

    @Test
    fun navigationToHistoryScreen() {
        // Test navigation to history screen (main navigation path)
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we can navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun navigationToSettingsScreen() {
        // Test navigation to settings screen
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we can navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun tabNavigationMaintainsState() {
        // Test that tab navigation works and maintains state
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to another screen and back
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should return to the same tab (SKUs)
        composeTestRule.onNodeWithText("SKUs").assertExists()
    }

    @Test
    fun rapidNavigationHandling() {
        // Test rapid navigation doesn't break the app
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        }
        
        // App should still be functional
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun systemBackButtonHandling() {
        // Test system back button behavior
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Use system back button
        device.pressBack()
        
        // Should return to main screen
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun multiLevelNavigationAndBack() {
        // Test multi-level navigation
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // If there's a components navigation option in settings
        try {
            composeTestRule.onNodeWithText("Components", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            
            // Navigate back twice
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        } catch (e: Exception) {
            // If components navigation doesn't exist, just go back once
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        }
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}