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
        // Give the activity time to launch and set content
        composeTestRule.waitForIdle()
        
        // Wait up to 5 seconds for the compose hierarchy to be established
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("B-Scan").fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Verify app launches and displays core UI
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun navigationToHistoryWorks() {
        // Wait for compose hierarchy
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription("View scan history").fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }
        
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
        // Wait for compose hierarchy
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithContentDescription("Check for updates").fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Verify update system UI is accessible
        composeTestRule.onNodeWithContentDescription("Check for updates")
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun nfcStatusIndicatorIsPresent() {
        // Wait for compose hierarchy
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithTag("nfc_status_indicator").fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Verify NFC indicator is shown (important for NFC-based app)
        composeTestRule.onNodeWithTag("nfc_status_indicator")
            .assertExists()
    }
}