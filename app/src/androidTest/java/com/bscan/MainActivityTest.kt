package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for MainActivity functionality using TestActivity
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun appLaunchesSuccessfully() {
        // Test that the app launches without crashing and displays the main UI elements
        
        // Check that the app bar is displayed
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Check that the main tabs are displayed (Inventory is first tab)
        composeTestRule.onNodeWithText("Inventory").assertExists()
        
        // Wait for data to load
        composeTestRule.waitForIdle()
    }

    @Test
    fun historyButtonIsDisplayed() {
        // Test that the history button is visible in the top bar
        composeTestRule.onNodeWithContentDescription("Scan History").assertIsDisplayed()
    }

    @Test
    fun settingsButtonIsDisplayed() {
        // Test that the settings button is visible in the top bar
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }

    @Test
    fun historyButtonOpensHistoryScreen() {
        // Test tapping history button opens history screen
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        
        // Check that we're now on the history screen
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
        
        // Should show history content or back button
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun settingsButtonOpensSettingsScreen() {
        // Test tapping settings button opens settings screen
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        
        // Wait for navigation to complete
        composeTestRule.waitForIdle()
        
        // Should show settings content or back button
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
    }

    @Test
    fun tabNavigationWorks() {
        // Wait for app to load completely
        composeTestRule.waitForIdle()
        
        // Test navigation between different tabs
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Tags").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Scans").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate back to Inventory
        composeTestRule.onNodeWithText("Inventory").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun backNavigationFromHistoryWorks() {
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate back using back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back to main screen with app title
        composeTestRule.onNodeWithText("B-Scan").assertExists()
    }
}