package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumented tests optimized for CI environment
 * 
 * These tests focus on critical app functionality that requires
 * an actual Android environment to verify properly.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CIBasicTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithoutCrashing() {
        // Verify app launches and displays core UI
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun navigationToHistoryWorks() {
        // Test basic navigation functionality
        composeTestRule.onNodeWithContentDescription("View scan history")
            .assertExists()
            .performClick()
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Scan History").assertExists()
        
        // Navigate back
        composeTestRule.onNodeWithContentDescription("Navigate back")
            .performClick()
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun updateButtonIsAccessible() {
        // Verify update system UI is accessible
        composeTestRule.onNodeWithContentDescription("Check for updates")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun nfcStatusIndicatorIsPresent() {
        // Verify NFC indicator is shown (important for NFC-based app)
        composeTestRule.onNodeWithTag("nfc_status_indicator")
            .assertExists()
    }
}