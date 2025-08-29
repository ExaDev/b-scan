package com.bscan.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.bscan.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

private val WhiteColorScheme = lightColorScheme(
    background = Color.White,
    surface = Color.White,
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val BlackColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun BScanTheme(
    theme: AppTheme = AppTheme.AUTO,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    
    val colorScheme = when (theme) {
        AppTheme.AUTO -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    if (systemInDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                }
                systemInDarkTheme -> DarkColorScheme
                else -> LightColorScheme
            }
        }
        AppTheme.LIGHT -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    dynamicLightColorScheme(context)
                }
                else -> LightColorScheme
            }
        }
        AppTheme.DARK -> {
            when {
                dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val context = LocalContext.current
                    dynamicDarkColorScheme(context)
                }
                else -> DarkColorScheme
            }
        }
        AppTheme.WHITE -> WhiteColorScheme
        AppTheme.BLACK -> BlackColorScheme
    }
    
    // Determine if status bars should be light (dark text) or dark (light text)
    val lightStatusBars = when (theme) {
        AppTheme.AUTO -> !systemInDarkTheme
        AppTheme.LIGHT -> true
        AppTheme.DARK -> false
        AppTheme.WHITE -> true  // Light status bars (dark text) for white theme
        AppTheme.BLACK -> false // Dark status bars (light text) for black theme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use modern WindowInsets approach instead of deprecated color properties
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = lightStatusBars
            insetsController.isAppearanceLightNavigationBars = lightStatusBars
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}