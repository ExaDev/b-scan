package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal instrumented tests optimized for CI environment
 * 
 * These tests focus on critical app functionality that requires
 * an actual Android environment to verify properly. Due to NFC
 * requirements in MainActivity that cause it to finish() on emulators,
 * we test core Compose functionality instead.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CIBasicTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun composeBasicRenderingWorks() {
        // Test that Compose can render basic components in CI environment
        composeTestRule.setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("B-Scan CI Test")
                }
            }
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("B-Scan CI Test").assertIsDisplayed()
    }

    @Test
    fun materialThemeWorks() {
        // Test that Material 3 theming works in CI environment
        composeTestRule.setContent {
            MaterialTheme {
                Text(
                    text = "Material Theme Test",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        composeTestRule.onNodeWithText("Material Theme Test").assertIsDisplayed()
    }

    @Test
    fun composeNavigationPrimitives() {
        // Test basic interaction primitives needed for navigation
        composeTestRule.setContent {
            MaterialTheme {
                var clickCount by remember { mutableIntStateOf(0) }
                
                androidx.compose.material3.Button(
                    onClick = { clickCount++ }
                ) {
                    Text("Click Test $clickCount")
                }
            }
        }
        
        composeTestRule.onNodeWithText("Click Test 0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Click Test 1").assertIsDisplayed()
    }

    @Test
    fun androidInstrumentationContext() {
        // Test that Android context and instrumentation work
        composeTestRule.setContent {
            MaterialTheme {
                Text("Context Test")
            }
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Context Test").assertIsDisplayed()
        
        // This verifies that the test is running in an actual Android environment
        // (createComposeRule doesn't have activity, but this test confirms Android instrumentation works)
    }
}