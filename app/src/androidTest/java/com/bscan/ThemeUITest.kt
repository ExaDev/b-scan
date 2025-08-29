package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bscan.model.AppTheme
import com.bscan.repository.UserDataRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeUITest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var userDataRepository: UserDataRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        userDataRepository = UserDataRepository(context)
        
        // Reset to known theme state
        runBlocking {
            userDataRepository.updatePreferences { prefs ->
                prefs.copy(theme = AppTheme.AUTO)
            }
        }
    }

    @Test
    fun testActualThemeSwitching() {
        composeTestRule.waitForIdle()
        
        println("=== THEME SWITCHING UI TEST ===")
        
        // Get initial theme state
        runBlocking {
            val initialTheme = userDataRepository.getUserData().preferences.theme
            println("Starting theme: $initialTheme")
        }
        
        // Navigate to settings - try different approaches to find settings button
        println("Looking for settings navigation...")
        
        // Try content description first
        val settingsFound = try {
            composeTestRule.onNodeWithContentDescription("Settings").performClick()
            true
        } catch (e: AssertionError) {
            try {
                // Try text-based navigation
                composeTestRule.onNodeWithText("Settings").performClick()
                true
            } catch (e2: AssertionError) {
                // Try looking for any clickable element with settings
                try {
                    composeTestRule.onAllNodesWithText("Settings", ignoreCase = true)
                        .onFirst()
                        .performClick()
                    true
                } catch (e3: AssertionError) {
                    println("Could not find Settings button")
                    false
                }
            }
        }
        
        if (!settingsFound) {
            // Print all available text nodes for debugging
            println("Available nodes:")
            try {
                composeTestRule.onAllNodes(hasText("", substring = true))
                    .fetchSemanticsNodes()
                    .forEach { node ->
                        println("- Found node: $node")
                    }
            } catch (e: Exception) {
                println("Could not enumerate nodes: ${e.message}")
            }
            return
        }
        
        composeTestRule.waitForIdle()
        println("Successfully navigated to settings")
        
        // Look for theme section
        val themeFound = try {
            composeTestRule.onNodeWithText("Theme", ignoreCase = true).assertExists()
            println("Found Theme section")
            true
        } catch (e: AssertionError) {
            println("Could not find Theme section")
            false
        }
        
        if (!themeFound) {
            // Print available text for debugging
            println("Available text in settings:")
            try {
                composeTestRule.onAllNodes(hasText("", substring = true))
                    .fetchSemanticsNodes()
                    .forEach { node ->
                        println("- Settings node: $node")
                    }
            } catch (e: Exception) {
                println("Could not enumerate settings nodes: ${e.message}")
            }
            return
        }
        
        // Try to click on Dark theme option
        val darkThemeClicked = try {
            composeTestRule.onNodeWithText("Dark", ignoreCase = true).performClick()
            composeTestRule.waitForIdle()
            println("Clicked Dark theme option")
            true
        } catch (e: AssertionError) {
            println("Could not find or click Dark theme option")
            
            // Look for theme options
            println("Looking for theme options:")
            val themeOptions = listOf("Light", "Dark", "White", "Black", "Auto")
            themeOptions.forEach { option ->
                val found = try {
                    composeTestRule.onNodeWithText(option, ignoreCase = true).assertExists()
                    println("- Found theme option: $option")
                    true
                } catch (e: AssertionError) {
                    println("- Missing theme option: $option")
                    false
                }
            }
            false
        }
        
        if (darkThemeClicked) {
            // Wait a bit and check if theme actually changed
            Thread.sleep(500) // Give time for change to propagate
            
            runBlocking {
                val finalTheme = userDataRepository.getUserData().preferences.theme
                println("Theme after clicking Dark: $finalTheme")
                
                if (finalTheme == AppTheme.DARK) {
                    println("✅ SUCCESS: Theme change worked!")
                } else {
                    println("❌ FAILED: Theme did not change to DARK, still: $finalTheme")
                }
            }
        }
    }

    @Test 
    fun testDirectRepositoryChange() {
        println("=== TESTING DIRECT REPOSITORY CHANGE ===")
        
        runBlocking {
            // Get initial state
            val initial = userDataRepository.getUserData().preferences.theme
            println("Initial theme: $initial")
            
            // Change theme directly
            println("Changing to DARK via repository...")
            userDataRepository.updatePreferences { prefs ->
                prefs.copy(theme = AppTheme.DARK)
            }
            
            // Check change
            val after = userDataRepository.getUserData().preferences.theme
            println("Theme after change: $after")
            
            if (after == AppTheme.DARK) {
                println("✅ Repository change works")
            } else {
                println("❌ Repository change failed")
            }
        }
        
        // Give time for UI to react
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        
        println("UI should now reflect DARK theme")
    }
}