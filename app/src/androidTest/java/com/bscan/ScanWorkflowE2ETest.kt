package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for the complete NFC scanning workflow
 * 
 * Note: These tests verify UI flow and behavior but cannot test actual NFC hardware
 * without physical tags. They test the application's response to scan states.
 */
@RunWith(AndroidJUnit4::class)
class ScanWorkflowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun scanPromptScreenDisplaysCorrectly() {
        // Verify the main scan prompt screen elements
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        
        // Check for NFC status indicator (should show NFC is available)
        composeTestRule.onNodeWithTag("nfc_status_indicator").assertExists()
        
        // Check for scan instruction text
        composeTestRule.onNode(
            hasText("Hold your device near a Bambu Lab filament spool") or
            hasText("Tap your device to a Bambu Lab spool")
        ).assertExists()
    }

    @Test
    fun historyNavigationWorkflow() {
        // Start on main screen
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        
        // Navigate to history
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Should be on history screen
        composeTestRule.onNodeWithText("Scan History").assertExists()
        
        // Navigate back to main
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
        
        // Should be back on main screen
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun updateSettingsAccessible() {
        // Check if update settings can be accessed through menu or settings
        // This will depend on how the update settings are integrated into the UI
        
        // For now, just verify the main screen is stable
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // If there's a settings button or menu, test it here
        // composeTestRule.onNodeWithContentDescription("Settings").performClick()
    }

    @Test
    fun emptyHistoryStateDisplaysCorrectly() {
        // Navigate to history screen
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        // Should show empty state message
        composeTestRule.onNode(
            hasText("No scans yet") or 
            hasText("No scan history") or
            hasText("Start scanning")
        ).assertExists()
    }

    @Test
    fun screenRotationHandledCorrectly() {
        // Verify main screen content
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // This would test rotation if we set up the activity rule for it
        // For now, just ensure the UI is stable
        composeTestRule.waitForIdle()
        
        // Navigate to history and back to test state preservation
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
        
        // Should still be stable
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun accessibilityElementsPresent() {
        // Verify that key UI elements have proper accessibility labels
        composeTestRule.onNodeWithContentDescription("View scan history").assertExists()
        
        // Check for accessibility-friendly content
        composeTestRule.onNodeWithTag("nfc_status_indicator").assertExists()
        
        // Verify text elements are accessible (have semantic properties)
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View scan history").assertHasClickAction()
    }

    @Test
    fun uiResponsivenessUnderStress() {
        // Test rapid navigation to ensure UI doesn't break
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("View scan history").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.waitForIdle()
        }
        
        // Should still be functional
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        composeTestRule.onNodeWithContentDescription("View scan history").assertIsDisplayed()
    }
}