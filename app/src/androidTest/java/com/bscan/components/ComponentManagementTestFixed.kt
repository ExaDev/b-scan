package com.bscan.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bscan.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before

/**
 * Fixed component management tests that focus on accessible functionality.
 */
@RunWith(AndroidJUnit4::class)
class ComponentManagementTestFixed {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Before
    fun setUp() {
        composeTestRule.waitForIdle()
    }

    @Test
    fun appLaunchesSuccessfully() {
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }

    @Test
    fun settingsAccessible() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}