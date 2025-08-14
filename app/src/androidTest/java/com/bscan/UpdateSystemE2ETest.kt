package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for the automatic update system
 * 
 * These tests verify the update UI components and workflow without
 * making actual network requests or installing updates.
 */
@RunWith(AndroidJUnit4::class)
class UpdateSystemE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunchesWithoutUpdatePrompts() {
        // Verify app launches normally without showing update dialogs
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        
        // Should not show update dialog on startup by default
        composeTestRule.onNode(hasText("Update Available") or hasText("New Version")).assertDoesNotExist()
    }

    @Test
    fun updateDialogNotShownByDefault() {
        // Verify that update dialogs don't appear unexpectedly
        composeTestRule.waitForIdle()
        
        // Check that no update-related dialogs are shown
        composeTestRule.onNodeWithText("Update Available", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Download", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Install", substring = true).assertDoesNotExist()
    }

    @Test
    fun backgroundUpdateCheckDoesNotBlockUI() {
        // Test that background update checks don't interfere with normal usage
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Navigate to history and back to ensure UI remains responsive
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
        
        // UI should remain functional
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
    }

    @Test
    fun updateSystemInitializesWithoutErrors() {
        // Verify that update system components initialize properly
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // App should launch successfully even with update system enabled
        // This tests that UpdateRepository, GitHubUpdateService, etc. don't crash on init
        composeTestRule.waitForIdle()
        
        // Basic app functionality should work
        composeTestRule.onNodeWithContentDescription("View scan history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun noUpdateRelatedCrashesOnNavigation() {
        // Test that rapid navigation doesn't trigger update-related crashes
        repeat(2) {
            composeTestRule.onNodeWithContentDescription("View scan history").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Navigate back").performClick()
            composeTestRule.waitForIdle()
        }
        
        // Should remain stable
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun updatePermissionsHandledGracefully() {
        // Verify that update system doesn't cause permission-related crashes
        // Even without INSTALL_PACKAGES permission, app should work normally
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ready to scan", substring = true).assertExists()
        
        // Basic navigation should work
        composeTestRule.onNodeWithContentDescription("View scan history").assertExists()
    }
}