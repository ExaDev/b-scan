package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for MainActivity
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesSuccessfully() {
        // Test that the app launches without crashing
        // and displays the main UI elements
        
        // Check that the app bar is displayed
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Check that some scan prompt content is displayed
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun historyButtonIsDisplayed() {
        // Test that the history button is visible in the top bar
        composeTestRule.onNodeWithContentDescription("View scan history").assertIsDisplayed()
    }

    @Test
    fun historyButtonOpensHistoryScreen() {
        // Test tapping history button opens history screen
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        
        // Check that we're now on the history screen
        // (This might show "No scans yet" or similar)
        composeTestRule.onNodeWithText("Scan History", substring = true).assertExists()
    }

    @Test
    fun backNavigationFromHistoryWorks() {
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        
        // Navigate back (look for back button or arrow)
        composeTestRule.onNodeWithContentDescription("Navigate back", ignoreCase = true).performClick()
        
        // Should be back to main screen
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }
}