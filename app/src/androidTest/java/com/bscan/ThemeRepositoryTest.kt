package com.bscan

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bscan.model.AppTheme
import com.bscan.repository.UserDataRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThemeRepositoryTest {

    private lateinit var userDataRepository: UserDataRepository

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        userDataRepository = UserDataRepository(context)
    }

    @Test
    fun testRepositoryFlowInitialization() = runBlocking<Unit> {
        println("=== Testing Repository Flow Initialization ===")
        
        // Get initial theme (should initialize the flow)
        val initialUserData = userDataRepository.getUserData()
        val initialTheme = initialUserData.preferences.theme
        println("Initial theme: $initialTheme")
        
        // The flow should now be initialized and contain the same data
        val flowValue = userDataRepository.userDataFlow.first()
        println("Flow value: ${flowValue?.preferences?.theme}")
        
        Assert.assertNotNull("Flow should not be null after getUserData()", flowValue)
        Assert.assertEquals("Flow should contain same theme as getUserData()", initialTheme, flowValue?.preferences?.theme)
    }

    @Test
    fun testThemeChangeViaUpdatePreferences() = runBlocking<Unit> {
        println("=== Testing Theme Change Via updatePreferences ===")
        
        // Initialize the flow
        val initialTheme = userDataRepository.getUserData().preferences.theme
        println("Starting with theme: $initialTheme")
        
        // Change theme using updatePreferences (same as SettingsScreen does)
        println("Changing theme to DARK...")
        userDataRepository.updatePreferences { prefs ->
            prefs.copy(theme = AppTheme.DARK)
        }
        
        // Check repository reflects change
        val updatedUserData = userDataRepository.getUserData()
        println("Repository theme after update: ${updatedUserData.preferences.theme}")
        Assert.assertEquals("Repository should reflect theme change", AppTheme.DARK, updatedUserData.preferences.theme)
        
        // Check flow reflects change  
        val updatedFlowValue = userDataRepository.userDataFlow.first()
        println("Flow theme after update: ${updatedFlowValue?.preferences?.theme}")
        Assert.assertEquals("Flow should reflect theme change", AppTheme.DARK, updatedFlowValue?.preferences?.theme)
        
        // Change to another theme
        println("Changing theme to WHITE...")
        userDataRepository.updatePreferences { prefs ->
            prefs.copy(theme = AppTheme.WHITE)
        }
        
        val finalFlowValue = userDataRepository.userDataFlow.first()
        println("Final flow theme: ${finalFlowValue?.preferences?.theme}")
        Assert.assertEquals("Flow should reflect second theme change", AppTheme.WHITE, finalFlowValue?.preferences?.theme)
    }

    @Test
    fun testMultipleFlowCollections() = runBlocking<Unit> {
        println("=== Testing Multiple Flow Collections ===")
        
        // Initialize
        userDataRepository.getUserData()
        
        // Collect from flow multiple times
        val collection1 = userDataRepository.userDataFlow.first()
        val collection2 = userDataRepository.userDataFlow.first()
        
        println("Collection 1: ${collection1?.preferences?.theme}")
        println("Collection 2: ${collection2?.preferences?.theme}")
        
        Assert.assertEquals("Multiple collections should return same value", 
            collection1?.preferences?.theme, collection2?.preferences?.theme)
    }
}