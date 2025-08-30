package com.bscan.ui.components.list

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import android.util.Log
import com.bscan.ScanState
import com.bscan.model.ScanProgress
import com.bscan.ui.screens.ScanPromptScreen
import com.bscan.ui.screens.home.CompactScanPrompt
import kotlin.math.max
import kotlin.math.min

/**
 * Reusable wrapper that provides overscroll-triggered scan prompt functionality for lists.
 * Supports discrete gesture levels: hidden -> compact prompt -> full prompt.
 */
@Composable
fun OverscrollListWrapper(
    lazyListState: LazyListState,
    scanState: ScanState = ScanState.IDLE,
    scanProgress: ScanProgress? = null,
    onSimulateScan: () -> Unit = {},
    compactPromptHeightDp: Dp = 100.dp,
    fullPromptHeightDp: Dp = 400.dp,
    modifier: Modifier = Modifier,
    content: @Composable (contentPadding: PaddingValues) -> Unit
) {
    // Overscroll-based scan prompt logic
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp
    val singleRowHeight = compactPromptHeightDp
    val fullPageHeight = kotlin.math.max(200.dp.value, kotlin.math.min(600.dp.value, (screenHeightDp - 200.dp).value)).dp
    
    // Track scan prompt state with discrete levels
    var promptState by remember { mutableStateOf(0) } // 0 = hidden, 1 = single row, 2 = full height
    var isUserDragging by remember { mutableStateOf(false) }
    var overscrollAccumulated by remember { mutableStateOf(0f) }
    var lastTransitionTime by remember { mutableStateOf(0L) } // Time of last state transition
    
    // Calculate scan prompt height based on discrete state
    val scanPromptHeight = when (promptState) {
        1 -> singleRowHeight // Single row
        2 -> fullPageHeight  // Full height
        else -> 0.dp         // Hidden
    }
    
    val animatedHeight by animateDpAsState(
        targetValue = scanPromptHeight,
        animationSpec = spring(),
        label = "scan_prompt_height"
    )
    
    // Overscroll connection for discrete gesture detection
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                
                // Track if user is actively dragging (not momentum)
                val wasDragging = isUserDragging
                isUserDragging = source == NestedScrollSource.UserInput
                
                // Only handle deliberate drag gestures at the top
                if (isAtTop && available.y > 0 && isUserDragging) {
                    overscrollAccumulated += available.y
                    
                    // Discrete state transitions based on accumulated drag with time delay
                    val threshold = singleRowHeight.value * 0.3f // Threshold for state change
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastTransition = currentTime - lastTransitionTime
                    val minimumDelay = 300L // 300ms minimum between transitions
                    
                    when (promptState) {
                        0 -> { // Hidden -> Single row
                            if (overscrollAccumulated > threshold) {
                                promptState = 1
                                overscrollAccumulated = 0f // Reset for next transition
                                lastTransitionTime = currentTime
                                Log.d("ScanPrompt", "Transition 0->1 (single row)")
                            }
                        }
                        1 -> { // Single row -> Full height  
                            if (overscrollAccumulated > threshold && timeSinceLastTransition > minimumDelay) {
                                promptState = 2
                                overscrollAccumulated = 0f
                                lastTransitionTime = currentTime
                                Log.d("ScanPrompt", "Transition 1->2 (full height)")
                            }
                        }
                        // State 2 (full height) stays at 2
                    }
                    
                    return Offset(0f, available.y) // Consume the drag
                }
                return Offset.Zero
            }
            
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val isAtTop = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
                
                // Reset states when actively scrolling down or significantly away from top
                // Use moderate tolerance to account for scan prompt height but still allow collapsing
                val tolerancePixels = 200 // Reduced tolerance to make collapsing more responsive
                if ((available.y < 0 && source == NestedScrollSource.UserInput) || 
                    (!isAtTop && lazyListState.firstVisibleItemScrollOffset > tolerancePixels)) {
                    promptState = 0
                    overscrollAccumulated = 0f
                    isUserDragging = false
                    lastTransitionTime = 0L // Reset transition timer
                }
                
                // Reset dragging flag when momentum stops
                if (source != NestedScrollSource.UserInput) {
                    isUserDragging = false
                    overscrollAccumulated = 0f
                }
                
                return Offset.Zero
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // Content with padding adjusted for scan prompt
        content(
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp + animatedHeight, // Push content down by scan prompt height
                bottom = 8.dp
            )
        )
        
        // Overlay scan prompt that appears on overscroll
        if (animatedHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(animatedHeight)
                    .padding(horizontal = 16.dp)
            ) {
                when (promptState) {
                    2 -> {
                        // Show full scan prompt
                        ScanPromptScreen()
                    }
                    1 -> {
                        // Show compact scan prompt
                        CompactScanPrompt(
                            scanState = scanState,
                            scanProgress = scanProgress,
                            onLongPress = onSimulateScan
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OverscrollListWrapperPreview() {
    MaterialTheme {
        OverscrollListWrapper(
            lazyListState = rememberLazyListState(),
            scanState = ScanState.IDLE,
            scanProgress = null,
            onSimulateScan = { },
            modifier = Modifier.height(200.dp)
        ) { contentPadding ->
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize()
            ) {
                item { Text("Sample Item 1", modifier = Modifier.padding(16.dp)) }
                item { Text("Sample Item 2", modifier = Modifier.padding(16.dp)) }
                item { Text("Sample Item 3", modifier = Modifier.padding(16.dp)) }
            }
        }
    }
}

