package com.example.asm3.ui.theme

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

private val LightColors = lightColorScheme(
    primary = SolarGreen,
    onPrimary = Color.White,
    secondary = SolarTeal,
    onSecondary = Color.White,
    tertiary = SolarYellow,
    background = SolarGray,
    surface = Color.White,
    onSurface = Color(0xFF1A1C1A)
)

private val DarkColors = darkColorScheme(
    primary = SolarTeal,
    onPrimary = Color.Black,
    secondary = SolarGreen,
    onSecondary = Color.Black,
    tertiary = SolarYellow,
    background = Color(0xFF101010),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White
)

@Composable
fun Asm3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            try {
                // Try to use dynamic colors, fallback to static if GMS fonts not available
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                // Fallback to static colors if dynamic colors fail (e.g., no GMS fonts)
                if (darkTheme) DarkColors else LightColors
            }
        }

        darkTheme -> DarkColors
        else -> LightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            window.statusBarColor = colorScheme.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

