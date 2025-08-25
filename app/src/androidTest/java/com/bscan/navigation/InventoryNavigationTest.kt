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
 * Navigation tests for B-Scan inventory functionality.
 * Tests basic navigation flows and tab switching.
 */
@RunWith(AndroidJUnit4::class)
class InventoryNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Wait for app to fully load
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainScreenShowsExpectedElements() {
        // Wait for home screen to load
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()

        // Ensure main tabs are visible
        composeTestRule.onNodeWithText("Inventory").assertExists()
        composeTestRule.onNodeWithText("SKUs").assertExists()
        composeTestRule.onNodeWithText("Tags").assertExists()
        composeTestRule.onNodeWithText("Scans").assertExists()
    }

    @Test
    fun tabNavigationWorks() {
        // Test switching between tabs
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Tags").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Scans").performClick()
        composeTestRule.waitForIdle()
        
        // Return to Inventory
        composeTestRule.onNodeWithText("Inventory").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun topBarNavigationWorks() {
        // Test navigation to history screen
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Test navigation to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back on main screen
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun systemBackButtonWorks() {
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Use system back button
        device.pressBack()
        
        // Should return to home screen
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun controlButtonsAreVisible() {
        // Check that control buttons are available
        composeTestRule.onNode(
            hasContentDescription("Sort options") or 
            hasText("Filter", substring = true) or 
            hasText("Group", substring = true)
        ).assertExists()
    }
}