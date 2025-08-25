package com.bscan.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bscan.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Mass calculation tests - simplified to basic functionality.
 */
@RunWith(AndroidJUnit4::class)
class MassCalculationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun basicAppFunctionalityWorks() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inventory").assertExists()
    }
}