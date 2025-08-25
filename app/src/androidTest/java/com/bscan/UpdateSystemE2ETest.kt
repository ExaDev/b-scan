package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

/**
 * E2E tests for update system functionality.
 */
@RunWith(AndroidJUnit4::class)
class UpdateSystemE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunchesWithoutUpdateDialogs() {
        // Test that app launches without showing update dialogs by default
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Should not show update dialogs on normal launch
        composeTestRule.onNodeWithText("Update", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun basicAppFunctionalityWorksRegardlessOfUpdateSystem() {
        // Test that basic app functionality works regardless of update system state
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Navigate through tabs
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun settingsAccessibilityNotBlockedByUpdateSystem() {
        // Test that update system doesn't block access to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Should be able to access settings
        composeTestRule.onRoot().assertExists()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}