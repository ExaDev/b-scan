package com.bscan.emulator

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bscan.TestActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SimpleComponentEmulatorTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun simpleEmulatorTest() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan").assertIsDisplayed()
    }
}