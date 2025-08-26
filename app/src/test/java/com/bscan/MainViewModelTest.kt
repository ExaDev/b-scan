package com.bscan

import android.app.Application
import android.content.SharedPreferences
import com.bscan.debug.DebugDataCollector
import com.bscan.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

/**
 * Unit tests for MainViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class MainViewModelTest {

    @Mock
    private lateinit var mockApplication: Application
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Mock SharedPreferences for all repositories
        `when`(mockApplication.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.apply()).then { /* no-op */ }
        `when`(mockSharedPreferences.getString(anyString(), anyString())).thenReturn(null)
        
        viewModel = MainViewModel(mockApplication)
    }

    @Test
    fun `initial state should be idle`() {
        // Then
        val initialState = viewModel.uiState.value
        assertEquals("Should start in IDLE state", ScanState.IDLE, initialState.scanState)
        assertNull("Should have no filament info initially", initialState.filamentInfo)
        assertNull("Should have no error initially", initialState.error)
        assertNull("Should have no debug info initially", initialState.debugInfo)
        assertNull("Should have no scan progress initially", viewModel.scanProgress.value)
    }

    @Test
    fun `onTagDetected should update state to TAG_DETECTED`() = runTest {
        // When
        viewModel.onTagDetected()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in TAG_DETECTED state", ScanState.TAG_DETECTED, state.scanState)
        assertNull("Should clear any previous error", state.error)
        
        val progress = viewModel.scanProgress.value
        assertNotNull("Should have scan progress", progress)
        assertEquals("Should be at TAG_DETECTED stage", ScanStage.TAG_DETECTED, progress?.stage)
        assertEquals("Should start at 0% progress", 0.0f, progress?.percentage ?: 0.0f, 0.001f)
    }

    @Test
    fun `setScanning should update state to PROCESSING`() {
        // When
        viewModel.setScanning()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in PROCESSING state", ScanState.PROCESSING, state.scanState)
        assertNull("Should clear any previous error", state.error)
    }

    @Test
    fun `setNfcError should update state to ERROR with message`() {
        // Given
        val errorMessage = "NFC not available"
        
        // When
        viewModel.setNfcError(errorMessage)
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should be in ERROR state", ScanState.ERROR, state.scanState)
        assertEquals("Should set error message", errorMessage, state.error)
    }

    @Test
    fun `clearError should remove error message`() {
        // Given - set an error first
        viewModel.setNfcError("Some error")
        
        // When
        viewModel.clearError()
        
        // Then
        assertNull("Should clear error message", viewModel.uiState.value.error)
    }

    @Test
    fun `resetScan should return to initial state`() {
        // Given - set some state first
        viewModel.setNfcError("Some error")
        viewModel.updateScanProgress(ScanProgress(ScanStage.PARSING, 0.5f, 8, 16, "Parsing"))
        
        // When
        viewModel.resetScan()
        
        // Then
        val state = viewModel.uiState.value
        assertEquals("Should return to IDLE", ScanState.IDLE, state.scanState)
        assertNull("Should clear filament info", state.filamentInfo)
        assertNull("Should clear error", state.error)
        assertNull("Should clear debug info", state.debugInfo)
        assertNull("Should clear scan progress", viewModel.scanProgress.value)
    }

    @Test
    fun `updateScanProgress should update progress state`() {
        // Given
        val progress = ScanProgress(
            stage = ScanStage.AUTHENTICATING,
            percentage = 0.5f,
            statusMessage = "Authenticating sector 8/16",
            currentSector = 7
        )
        
        // When
        viewModel.updateScanProgress(progress)
        
        // Then
        val currentProgress = viewModel.scanProgress.value
        assertEquals("Should update stage", ScanStage.AUTHENTICATING, currentProgress?.stage)
        assertEquals("Should update percentage", 0.5f, currentProgress?.percentage ?: 0.0f, 0.001f)
        assertEquals("Should update status message", "Authenticating sector 8/16", currentProgress?.statusMessage)
        assertEquals("Should update current sector", 7, currentProgress?.currentSector)
    }

    @Test
    fun `simulateScan should start simulation without errors`() = runTest {
        // Given - initial state
        val initialState = viewModel.uiState.value
        assertEquals("Should start in IDLE state", ScanState.IDLE, initialState.scanState)
        
        // When
        viewModel.simulateScan()
        
        // Allow some time for initial simulation steps
        kotlinx.coroutines.delay(100)
        
        // Then - verify simulation started (should be in TAG_DETECTED or beyond)
        val updatedState = viewModel.uiState.value
        assertTrue(
            "Simulation should start (state should not be IDLE)", 
            updatedState.scanState != ScanState.IDLE
        )
        
        // Verify scan progress is updated
        val progress = viewModel.scanProgress.value
        assertNotNull("Scan progress should be set", progress)
        assertTrue(
            "Progress percentage should be non-negative", 
            (progress?.percentage ?: -1f) >= 0f
        )
    }

    @Test
    fun `getInventoryItems should return inventory items`() {
        // When
        val inventoryItems = viewModel.getInventoryItems()
        
        // Then
        assertNotNull("Should return inventory items map", inventoryItems)
        assertTrue("Inventory items should be a map", inventoryItems is Map<*, *>)
    }

    @Test
    fun `getManufacturers should return manufacturers`() {
        // When
        val manufacturers = viewModel.getManufacturers()
        
        // Then
        assertNotNull("Should return manufacturers map", manufacturers)
        assertTrue("Manufacturers should be a map", manufacturers is Map<*, *>)
    }

    @Test
    fun `refreshMappings should update interpreter factory`() {
        // When
        viewModel.refreshMappings()
        
        // Then - should not throw exception
        // This method refreshes internal state but has no observable effects to test
    }
}