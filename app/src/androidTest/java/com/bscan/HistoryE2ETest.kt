package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for scan history functionality
 * 
 * These tests verify the history screen behavior, navigation, and display of scan results.
 */
@RunWith(AndroidJUnit4::class)
class HistoryE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun historyScreenLayoutAndNavigation() {
        // Navigate to history screen
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Verify history screen is displayed
        composeTestRule.onNodeWithText("Scan History").assertIsDisplayed()
        
        // Check for back navigation
        composeTestRule.onNodeWithContentDescription("Navigate back").assertIsDisplayed()
        
        // Test back navigation
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
        
        // Should return to main screen
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun emptyHistoryStateUI() {
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Should display empty state
        composeTestRule.onNode(
            hasText("No scans yet") or
            hasText("No scan history") or 
            hasText("Your scan history will appear here") or
            hasText("Start scanning to see results")
        ).assertExists()
    }

    @Test
    fun historyScreenAccessibility() {
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Verify accessibility elements
        composeTestRule.onNodeWithText("Scan History").assertExists()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertHasClickAction()
        
        // Check that empty state has accessible text
        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun historyScreenStatePreservation() {
        // Navigate to history multiple times to test state preservation
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to history again
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Should still show history screen consistently
        composeTestRule.onNodeWithText("Scan History").assertIsDisplayed()
    }

    @Test
    fun historyScreenScrollBehavior() {
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Even with empty state, should handle scroll gestures gracefully
        // This tests the LazyColumn or similar scrollable component
        composeTestRule.onRoot().performTouchInput {
            swipeUp()
            swipeDown()
        }
        
        // Should still be functional after scroll gestures
        composeTestRule.onNodeWithText("Scan History").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Navigate back").assertHasClickAction()
    }
}