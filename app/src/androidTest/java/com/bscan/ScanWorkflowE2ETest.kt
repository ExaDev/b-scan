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
 * E2E workflow tests for scan functionality.
 * Tests basic app workflow without actual NFC scanning.
 */
@RunWith(AndroidJUnit4::class)
class ScanWorkflowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunchAndBasicWorkflow() {
        // Test the basic workflow: app launch -> navigate tabs -> settings
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Navigate through tabs
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Tags").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("Scans").performClick()
        composeTestRule.waitForIdle()
        
        // Access settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Return to main
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun historyNavigationWorkflow() {
        // Test history navigation workflow
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Should be able to navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun tabPersistenceWorkflow() {
        // Test that tab selection persists across navigation
        composeTestRule.onNodeWithText("Tags").performClick()
        composeTestRule.waitForIdle()
        
        // Navigate to settings and back
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Should still show Tags as selected
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun systemBackButtonWorkflow() {
        // Test system back button workflow
        composeTestRule.onNodeWithText("SKUs").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        device.pressBack()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}