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
 * E2E tests for visual indicators and UI consistency.
 */
@RunWith(AndroidJUnit4::class)
class VisualIndicatorsE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        composeTestRule.waitForIdle()
    }

    @Test
    fun mainUIElementsVisibleAndConsistent() {
        // Test that main UI elements are visible and consistent
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        
        // Check that navigation elements are present
        composeTestRule.onNodeWithContentDescription("Scan History").assertExists()
        composeTestRule.onNodeWithContentDescription("Settings").assertExists()
        
        // Check that tabs are visible
        composeTestRule.onNodeWithText("Inventory").assertExists()
        composeTestRule.onNodeWithText("SKUs").assertExists()
        composeTestRule.onNodeWithText("Tags").assertExists()
        composeTestRule.onNodeWithText("Scans").assertExists()
    }

    @Test
    fun visualConsistencyAcrossTabs() {
        // Test visual consistency across tabs
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            // App bar should always be present
            composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
            
            // Navigation buttons should be present
            composeTestRule.onNodeWithContentDescription("Scan History").assertExists()
            composeTestRule.onNodeWithContentDescription("Settings").assertExists()
        }
    }

    @Test
    fun visualIndicatorsInSecondaryScreens() {
        // Test visual indicators in secondary screens
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        // Should have back button indicator
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        // Test history screen
        composeTestRule.onNodeWithContentDescription("Scan History").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun noVisualGlitchesDuringNavigation() {
        // Test that there are no visual glitches during navigation
        repeat(3) {
            composeTestRule.onNodeWithText("SKUs").performClick()
            Thread.sleep(100) // Brief pause to catch visual issues
            composeTestRule.onNodeWithText("Tags").performClick()
            Thread.sleep(100)
            composeTestRule.onNodeWithText("Inventory").performClick()
            Thread.sleep(100)
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun visualElementsHandleEmptyStatesAppropriately() {
        // Test that visual elements handle empty states appropriately
        val tabs = listOf("Inventory", "SKUs", "Tags", "Scans")
        
        tabs.forEach { tab ->
            composeTestRule.onNodeWithText(tab).performClick()
            composeTestRule.waitForIdle()
            
            // Should not show error indicators for empty states
            composeTestRule.onNodeWithText("Error", ignoreCase = true).assertDoesNotExist()
            
            // Basic structure should still be present
            composeTestRule.onRoot().assertExists()
        }
    }
}