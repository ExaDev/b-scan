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
 * E2E tests for history functionality.
 */
@RunWith(AndroidJUnit4::class)
class HistoryE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun historyNavigationBasics() {
        // Test basic history navigation
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        // Should be able to navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun historyFromDifferentTabs() {
        // Test accessing history from different tabs
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithContentDescription("Scan History").performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        }
    }

    @Test
    fun multipleHistoryNavigations() {
        // Test multiple history navigations
        repeat(3) {
            composeTestRule.onNodeWithContentDescription("Scan History").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithContentDescription("Back").performClick()
            composeTestRule.waitForIdle()
        }
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}