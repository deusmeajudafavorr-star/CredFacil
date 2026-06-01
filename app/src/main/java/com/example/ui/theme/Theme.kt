package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DarkSleekPrimary,
    onPrimary = Color(0xFF00325B),
    primaryContainer = DarkSleekPrimaryContainer,
    onPrimaryContainer = DarkSleekOnPrimaryContainer,
    secondary = DarkSleekSecondary,
    onSecondary = Color(0xFF00325B),
    background = DarkSleekBackground,
    onBackground = DarkSleekTextPrimary,
    surface = DarkSleekSurface,
    onSurface = DarkSleekTextPrimary,
    surfaceVariant = DarkSleekSurfaceVariant,
    onSurfaceVariant = DarkSleekTextSecondary,
    error = DarkSleekError,
    onError = Color(0xFF690005),
    errorContainer = DarkSleekErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimary,
    onPrimary = Color.White,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = SleekSecondary,
    onSecondary = Color.White,
    background = SleekBackground,
    onBackground = SleekOnSurface,
    surface = SleekSurface,
    onSurface = SleekOnSurface,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant,
    error = SleekError,
    onError = Color.White,
    errorContainer = SleekErrorContainer,
    onErrorContainer = SleekOnErrorContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Allow dynamic color on Android 12+ if desired, but default to our brand colors for maximum punch
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
