package com.bscan

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bscan.model.AppTheme
import com.bscan.repository.UserDataRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeSwitchingTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var userDataRepository: UserDataRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        userDataRepository = UserDataRepository(context)
    }

    @Test
    fun testThemeSwitchingFlow() {
        // First, let's test the repository flow directly
        runBlocking<Unit> {
            println("=== Testing UserDataRepository Flow ===")
            
            // Get initial theme
            val initialUserData = userDataRepository.getUserData()
            val initialTheme = initialUserData.preferences.theme
            println("Initial theme: $initialTheme")
            
            // Check if flow emits initial value
            val flowValue = userDataRepository.userDataFlow.first()
            println("Flow initial value: ${flowValue?.preferences?.theme}")
            Assert.assertEquals("Flow should emit initial theme", initialTheme, flowValue?.preferences?.theme)
            
            // Test theme change via updatePreferences
            println("Changing theme to DARK...")
            userDataRepository.updatePreferences { prefs ->
                prefs.copy(theme = AppTheme.DARK)
            }
            
            // Check if repository reflects change
            val updatedUserData = userDataRepository.getUserData()
            println("Updated theme in repository: ${updatedUserData.preferences.theme}")
            Assert.assertEquals("Repository should reflect theme change", AppTheme.DARK, updatedUserData.preferences.theme)
            
            // Check if flow emits updated value
            val updatedFlowValue = userDataRepository.userDataFlow.first()
            println("Flow updated value: ${updatedFlowValue?.preferences?.theme}")
            Assert.assertEquals("Flow should emit updated theme", AppTheme.DARK, updatedFlowValue?.preferences?.theme)
        }
    }

    @Test
    fun testThemeSwitchingUI() {
        composeTestRule.waitForIdle()
        
        // Navigate to settings screen
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        println("=== Testing UI Theme Switching ===")
        
        // Find theme section
        composeTestRule.onNodeWithText("Theme").assertExists()
        println("Found Theme section")
        
        // Look for theme options
        val themeOptions = listOf("Light", "Dark", "White", "Black", "Auto")
        themeOptions.forEach { option ->
            val exists = try {
                composeTestRule.onNodeWithText(option).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
            println("Theme option '$option' exists: $exists")
        }
        
        // Try to click Dark theme option
        try {
            println("Attempting to click Dark theme...")
            composeTestRule.onNodeWithText("Dark").performClick()
            composeTestRule.waitForIdle()
            println("Dark theme clicked successfully")
            
            // Check if the change persisted in repository
            runBlocking<Unit> {
                val currentTheme = userDataRepository.getUserData().preferences.theme
                println("Current theme in repository after UI click: $currentTheme")
                
                val flowTheme = userDataRepository.userDataFlow.first()?.preferences?.theme
                println("Current theme in flow after UI click: $flowTheme")
            }
            
        } catch (e: Exception) {
            println("Failed to click Dark theme: ${e.message}")
            e.printStackTrace()
        }
    }

    @Test
    fun testSettingsScreenThemeDisplay() {
        composeTestRule.waitForIdle()
        
        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        
        println("=== Testing Settings Screen Theme Display ===")
        
        // Check current theme display
        runBlocking<Unit> {
            val currentTheme = userDataRepository.getUserData().preferences.theme
            println("Current theme: $currentTheme")
            
            // Look for radio button selection
            val radioButtonSelected = try {
                // Check if any radio button is selected for current theme
                composeTestRule.onAllNodesWithText(currentTheme.name.lowercase().replaceFirstChar { it.uppercase() })
                    .onFirst()
                    .assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
            
            println("Radio button for current theme ($currentTheme) is visible: $radioButtonSelected")
        }
    }

    @Test
    fun testThemeChangeCallback() {
        println("=== Testing Theme Change Callback ===")
        
        var callbackInvoked = false
        var callbackTheme: AppTheme? = null
        
        // Set up a test callback similar to what SettingsScreen uses
        val testCallback = { theme: AppTheme ->
            println("Theme callback invoked with: $theme")
            callbackInvoked = true
            callbackTheme = theme
            
            // Simulate what SettingsScreen does
            runBlocking<Unit> {
                userDataRepository.updatePreferences { currentPrefs ->
                    println("Updating preferences from $${currentPrefs.theme} to $theme")
                    currentPrefs.copy(theme = theme)
                }
            }
        }
        
        // Test the callback
        testCallback(AppTheme.DARK)
        
        // Verify results
        assert(callbackInvoked) { "Callback should have been invoked" }
        Assert.assertEquals("Callback should receive correct theme", AppTheme.DARK, callbackTheme)
        
        // Verify repository was updated
        runBlocking<Unit> {
            val updatedTheme = userDataRepository.getUserData().preferences.theme
            println("Repository theme after callback: $updatedTheme")
            Assert.assertEquals("Repository should reflect callback change", AppTheme.DARK, updatedTheme)
        }
    }
}